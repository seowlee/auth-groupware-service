import {PaginationManager} from "./pagination.js";
import {navigateTo} from './core/router.js';
import {formatLeaveDays, mapLeaveType} from './leave-common.js';
import {apiFetch, showLoading, showMessage} from "./list-form-common.js";

let _leaveListManager = null;

export function initLeaveListManager(onRowClick = (leaveId) => {
    navigateTo(`/leaves/${leaveId}`);
}) {
    if (!_leaveListManager) {
        _leaveListManager = new LeaveListManager(onRowClick);
    }
    _leaveListManager.init();
}

class LeaveListManager {
    constructor(onRowClick) {
        this.onRowClick = onRowClick;
        this.currentUser = null;
        this.filters = {keyword: '', teamId: '', type: '', status: '', myOnly: true, tenureYear: ''};
        this.sortField = 'appliedAt';
        this.sortDir = 'desc';
    }

    async init() {
        this.readActorFromPage();
        this.setupRoleBasedUI();
        this.setupTenureYearUI();
        this.setupApplicantUI();
        this.pagination = new PaginationManager('paginationContainer', {
            onPageChange: page => this.loadLeaves(page),
            onPageSizeChange: () => this.loadLeaves(0),
        });
        this.pagination.init();
        this.bindEvents();
        await this.loadLeaves();
    }

    readActorFromPage() {
        const v = window.__LEAVE_FILTER__ || {};
        this.currentUser = {
            username: v.username || '',
            role: String(v.role || 'MEMBER').toUpperCase(),
            isSuper: !!v.superAdmin,
            isLeader: !!v.teamLeader,
            teamId: v.teamId ?? null,
            yearNumber: v.yearNumber ?? 1,
            joinedDate: v.joinedDate || null,
        };
    }

    bindEvents() {
        document.getElementById('leaveSearchBtn').addEventListener('click', () => this.handleSearch());
        document.getElementById('leaveResetBtn').addEventListener('click', () => this.resetFilters());
        document.getElementById('refreshLeavesBtn').addEventListener('click', () => this.loadLeaves());
        document.querySelectorAll('.sortable').forEach(th =>
            th.addEventListener('click', e => this.handleSort(e.currentTarget.dataset.sort))
        );

    }

    setupRoleBasedUI() {
        const u = this.currentUser || {};
        const myOnlyEl = document.getElementById('leaveMyOnly');
        const teamSel = document.getElementById('leaveTeamId');
        const keyword = document.getElementById('leaveKeyword');

        myOnlyEl.checked = true; // 기본 ON

        if (u.isSuper) {
            // 전체 조회 가능
            teamSel.disabled = false;
            keyword.disabled = false;
            myOnlyEl.disabled = false;
        } else if (u.isLeader) {
            // 팀장: 팀 고정(표시는 하되 변경 불가), 전체 보기 가능(팀 한정)
            if (u.teamId) teamSel.value = String(u.teamId);
            teamSel.disabled = true;
            keyword.disabled = false;
            myOnlyEl.disabled = false;
        } else {
            // 일반 멤버: 내 것만 보기 강제
            myOnlyEl.checked = true;
            myOnlyEl.disabled = true;
            if (u.teamId) teamSel.value = String(u.teamId);
            teamSel.disabled = true;
            keyword.disabled = true;
        }
    }

    setupTenureYearUI() {
        const myOnlyEl = document.getElementById('leaveMyOnly');
        const wrap = document.getElementById('tenureYearWrap');
        const select = document.getElementById('tenureYear');
        const n = Number(this.currentUser?.yearNumber || 1);
        const joinedIso = this.currentUser?.joinedDate; // LocalDate 문자열 "YYYY-MM-DD"

        const parseLocalDate = (s) => {
            if (!s) return null;
            const [y, m, d] = s.split('-').map(Number);
            return new Date(y, m - 1, d); // 로컬 Date
        };
        const addYears = (date, years) => {
            const d = new Date(date.getTime());
            d.setFullYear(d.getFullYear() + years);
            return d;
        };
        const minusDays = (date, days) => new Date(date.getTime() - days * 86400000);
        const fmt = (date) => {
            const y = date.getFullYear();
            const m = String(date.getMonth() + 1).padStart(2, '0');
            const d = String(date.getDate()).padStart(2, '0');
            return `${y}.${m}.${d}`;
        };

        const buildLabel = (k) => {
            if (!joinedIso) return (k === n ? `근속 ${k}년차` : `${k}년차`);
            const joined = parseLocalDate(joinedIso); // ← 안전 파싱
            const start = addYears(joined, k - 1);
            const endExclusive = addYears(joined, k);
            const end = minusDays(endExclusive, 1); // 포함 끝일
            const head = (k === n ? `근속 ${k}년차` : `${k}년차`);
            return `${head} (${fmt(start)} ~ ${fmt(end)})`;
        };

        const fill = () => {
            select.innerHTML = '';
            for (let k = n; k >= 1; k--) {
                const label = buildLabel(k);
                select.insertAdjacentHTML('beforeend', `<option value="${k}">${label}</option>`);
            }
            select.value = String(n);
        };

        const toggle = () => {
            if (myOnlyEl.checked) {
                wrap.classList.remove('hidden');
                fill();
            } else {
                wrap.classList.add('hidden');
                select.value = '';
            }
        };

        toggle();
        myOnlyEl.addEventListener('change', () => {
            toggle();
            this.handleSearch();
        });
        select.addEventListener('change', () => this.handleSearch());
    }

    setupApplicantUI() {
        const u = this.currentUser || {};
        const myOnlyEl = document.getElementById('leaveMyOnly');
        const wrap = document.getElementById('applicantWrap');
        // const teamSel = document.getElementById('leaveTeamId');

        const toggle = () => {
            if (!myOnlyEl.checked && (u.isLeader || u.isSuper)) {
                wrap.classList.remove('hidden');
                // 팀장: 팀 고정 표시(값은 setupRoleBasedUI에서 이미 disabled)
            } else {
                wrap.classList.add('hidden');
                const appSel = document.getElementById('applicantId');
                if (appSel) appSel.value = '';
            }
            // 입력 컨트롤 enable/disable
            this.applyRoleBasedEnabling();
        };
        toggle();
        myOnlyEl.addEventListener('change', () => {
            toggle();
            this.handleSearch();
        });

        // 팀장 모드에서 팀 셀렉트는 비활성화 되어 있음(변경 불가).
    }

    applyRoleBasedEnabling() {
        const u = this.currentUser || {};
        const myOnly = document.getElementById('leaveMyOnly').checked;

        const kw = document.getElementById('leaveKeyword');   // 신청자명 검색
        const teamSel = document.getElementById('leaveTeamId');

        // 신청자명 검색: myOnly=ON이면 모두 비활성, OFF일 때만 리더/슈퍼 활성
        const canSearchOthers = !myOnly && (u.isLeader || u.isSuper);
        kw.disabled = !canSearchOthers;
        if (kw.disabled) kw.value = '';

        if (u.isSuper) {
            // 슈퍼관리자: myOnly OFF일 때 팀 선택 허용
            teamSel.disabled = !!myOnly;
            if (myOnly) teamSel.value = ''; // 전체
        } else if (u.isLeader) {
            // 팀장: 팀 선택은 항상 불가(자기 팀 고정)
            if (u.teamId != null) teamSel.value = String(u.teamId);
            teamSel.disabled = true;
        } else {
            // 일반 멤버: 팀 의미 없음
            teamSel.value = u.teamId ? String(u.teamId) : '';
            teamSel.disabled = true;
        }
    }


    async loadLeaves(page = 0) {
        showLoading(true);
        this.collectFilters();
        const params = new URLSearchParams({
            page, size: this.pagination.getPageSize(),
            sort: `${this.sortField},${this.sortDir}`,
            ...this.filters,
            myOnly: String(this.filters.myOnly)
            // keyword: this.filters.keyword || '',
            // teamId: this.filters.teamId || '',
            // leaveType: this.filters.type || '',
            // status: this.filters.status || '',
            // // 필요 시 userUuid 추가 가능: userUuid: '...'
        });

        try {
            const res = await apiFetch(`/api/leaves?${params.toString()}`);
            if (!res.ok) {
                const msg = await readErrorMessage(res);
                throw new Error(msg);
            }
            const data = await res.json();
            this.renderLeaves(data.content || []);
            this.pagination.updatePagination(data);
        } catch (e) {
            // showMessage('연차 목록 로딩 실패', 'error');
            showMessage('연차 목록 로딩 실패' + (e?.message ? `: ${e.message}` : ''), 'error');
        } finally {
            showLoading(false);
        }
    }

    collectFilters() {
        const myOnly = document.getElementById('leaveMyOnly').checked;

        // 기본값 수집
        this.filters.keyword = document.getElementById('leaveKeyword').value.trim();
        this.filters.teamId = document.getElementById('leaveTeamId').value;
        this.filters.type = document.getElementById('leaveType').value;
        this.filters.status = document.getElementById('leaveStatus').value;

        this.filters.myOnly = !!myOnly;

        const ty = document.getElementById('tenureYear');
        this.filters.tenureYear = (myOnly && ty && ty.value) ? ty.value : '';

        // 신청자 선택(관리자/팀장 + myOnly=false)
        const appSel = document.getElementById('applicantId');
        this.filters.userId = (!myOnly && appSel && appSel.value) ? appSel.value : '';

        // myOnly = true일 땐 서버 혼선 방지용으로 강제 초기화
        if (myOnly) {
            this.filters.keyword = '';
            this.filters.teamId = '';     // 팀강제는 서버에서 처리(팀장/멤버), 전송은 공백
            this.filters.userId = '';
        }
    }

    renderLeaves(rows) {
        const body = document.getElementById('leaveTableBody');
        const table = document.getElementById('leaveTable');

        if (!rows.length) {
            body.innerHTML = '<tr><td colspan="8" style="text-align:center; color:#718096; font-weight:500;">검색 결과가 없습니다.</td></tr>';
            table.style.display = 'table';
            return;
        }

        body.innerHTML = rows.map(r => {
            const start = this.formatDate(r.startDt);
            const end = this.formatDate(r.endDt);
            const days = r.usedDays;
            return `
        <tr class="leave-row" data-id="${r.id}">
          <td>${this.escape(r.userName || '-')}</td>
          <td>${mapLeaveType(r.leaveType)}</td>
          <td>${start}</td>
          <td>${end}</td>
          <td>${formatLeaveDays(days)}</td>
          <td>
            <span class="status-badge ${this.mapStatusClass(r.status)}">
              ${this.mapStatus(r.status)}
            </span>
          </td>
          <td>${this.formatDate(r.appliedAt)}</td>
        </tr>
      `;
        }).join('');

        // 행 클릭 → 상세로 이동(원하면 비활성화 가능)
        body.querySelectorAll('.leave-row').forEach(row =>
            row.addEventListener('click', () => this.onRowClick(row.dataset.id))
        );

        table.style.display = 'table';
    }

    handleSearch() {
        this.pagination.reset();
        this.loadLeaves(0);
    }

    resetFilters() {
        const u = this.currentUser || {};
        const myOnlyEl = document.getElementById('leaveMyOnly');
        const tenureWrap = document.getElementById('tenureYearWrap');
        const tenureSel = document.getElementById('tenureYear');
        const applicantWrap = document.getElementById('applicantWrap');
        const applicantSel = document.getElementById('applicantId');
        const teamSel = document.getElementById('leaveTeamId');

        // 기본값
        myOnlyEl.checked = true;

        // 근속연차 영역 보이기 + 최상위 연차로 셀렉트
        tenureWrap.classList.remove('hidden');
        if (tenureSel && tenureSel.options.length > 0) {
            // 첫 로드 때 우리가 n..1로 채워두니 최상단(=현재 연차)로
            tenureSel.selectedIndex = 0;
        }

        // 신청자 영역 숨기고 값 제거
        if (applicantWrap) applicantWrap.classList.add('hidden');
        if (applicantSel) applicantSel.value = '';

        // 키워드/유형/상태 초기화
        document.getElementById('leaveKeyword').value = '';
        document.getElementById('leaveType').value = '';
        document.getElementById('leaveStatus').value = '';

        // 팀/권한 반영
        this.applyRoleBasedEnabling();

        // 내부 필터 객체도 싱크
        this.collectFilters();
        this.pagination.reset();    // 1페이지로
        this.loadLeaves(0);
    }

    handleSort(field) {
        if (this.sortField === field) {
            this.sortDir = this.sortDir === 'asc' ? 'desc' : 'asc';
        } else {
            this.sortField = field;
            this.sortDir = 'asc';
        }
        document.querySelectorAll('.sortable').forEach(th => th.classList.remove('asc', 'desc'));
        const current = document.querySelector(`th[data-sort="${field}"]`);
        if (current) current.classList.add(this.sortDir);
        this.loadLeaves(0);
    }

    // utils

    escape(s) {
        const d = document.createElement('div');
        d.textContent = s ?? '';
        return d.innerHTML;
    }

    calculateDays(startIso, endIso) {
        if (!startIso || !endIso) return 0;
        const s = new Date(startIso), e = new Date(endIso);
        return Math.max(1, Math.ceil((e - s) / (1000 * 60 * 60 * 24)));
        // (종료 포함 계산: +1)
    }

    formatDate(iso) {
        if (!iso) return '';
        try {
            return new Date(iso).toLocaleString('ko-KR');
        } catch {
            return iso;
        }
    }

    mapType(t) {
        return ({ANNUAL: '연차', BIRTHDAY: "생일", SICK: '병가', CUSTOM: '기타휴가'}[t] || t);
    }

    mapStatus(s) {
        return ({APPROVED: '승인', CANCELED: '취소', REJECTED: '반려'}[s] || s);
    }

    mapStatusClass(s) {
        return ({
            PENDING: 'status-pending',
            APPROVED: 'status-approved',
            CANCELED: 'status-canceled',
            REJECTED: 'status-rejected'
        }[s] || '');
    }

    validateDateRange() {
        const startDate = document.getElementById('startDate').value;
        const endDate = document.getElementById('endDate').value;

        if (startDate && endDate && new Date(endDate) < new Date(startDate)) {
            document.getElementById('endDate').value = startDate;
        }
    }


    clearMessage() {
        document.getElementById('messageArea').innerHTML = '';
    }

}
