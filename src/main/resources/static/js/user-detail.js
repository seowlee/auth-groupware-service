import {navigateTo} from './core/router.js';
import {
    ensureEnums,
    fmt3,
    formatLeaveDays,
    leaveTypeOptionsHtml,
    mapLeaveClass,
    yearNumberLabel
} from './leave-common.js';

/**
 * 사용자 상세 정보 관리자
 */
class UserDetailManager {

    constructor(userId) {
        this.userId = userId;
        this.userData = null;
        this.editing = false;
        this.balEditing = false;
        this.apiPrefix = '/api';
        this.activeTab = 'profile'; // 'profile' | 'leave'
        this.selectedYear = null;
    }

    /**
     * 초기화: 사용자/팀 데이터 로드 후 이벤트 바인딩
     */
    async init() {
        ensureEnums();
        await this.loadUser();
        await this.loadTeams();
        this.bindEvents();
        this.bindTabEvents();
        this.showTab('profile');
    }

    /**
     * 사용자 정보 조회 및 렌더링
     */
    async loadUser(yearNumber = null) {
        const q = new URLSearchParams({includeBalances: 'true'});
        if (yearNumber != null) q.set('yearNumber', String(yearNumber));
        const res = await fetch(`${this.apiPrefix}/team/users/${this.userId}?${q.toString()}`);
        if (!res.ok) throw new Error('사용자 조회 실패');
        this.userData = await res.json();
        // 최초 진입 시 selectedYear가 없으면 “현재 근속연차”로 설정
        if (this.selectedYear == null) this.selectedYear = this.userData.yearNumber;

        this.render(this.userData);

        // 연차 드롭다운 동기화(leave 탭 DOM이 이미 있으면 옵션/값 반영)
        this.syncYearSelect();
    }

    /**
     * 화면에 사용자 정보 반영
     */
    render(user) {
        const ENUMS = ensureEnums();
        document.getElementById('detailUsername').textContent = user.username;
        document.getElementById('detailEmail').textContent = user.email;
        document.getElementById('detailPhoneNumber').value = user.phoneNumber;
        document.getElementById('detailFirstName').value = user.firstName;
        document.getElementById('detailLastName').value = user.lastName;
        document.getElementById('detailJoinedDate').value = user.joinedDate;
        // 상태
        const userStatus = document.getElementById('detailStatus');
        // userStatus.innerHTML = [
        //     {v: 'ACTIVE', t: '활성'},
        //     {v: 'INACTIVE', t: '비활성'},
        //     {v: 'PENDING', t: '승인대기'}
        // ].map(o => `<option value="${o.v}">${o.t}</option>`).join('');
        userStatus.innerHTML = (ENUMS.statuses || [])
            .map(s => `<option value="${s.name}">${s.description}</option>`)
            .join('');
        userStatus.value = user.status;
        // 역할
        const roleSelect = document.getElementById('detailRole');
        // roleSelect.innerHTML = [
        //     {v: 'TEAM_MEMBER', t: '팀원'},
        //     {v: 'TEAM_LEADER', t: '팀장'},
        //     {v: 'SUPER_ADMIN', t: '최고관리자'}
        // ].map(o => `<option value="${o.v}">${o.t}</option>`).join('');
        roleSelect.innerHTML = (ENUMS.roles || [])
            .map(r => `<option value="${r.name}">${r.description}</option>`)
            .join('');
        roleSelect.value = user.role;

        // 연차 테이블
        this.renderLeaveBalances(user.leaveBalances || [], false, this.selectedYear);
    }

    /**
     * 연차 잔여 일수 목록
     * @param balances
     * @param editable : true면 편집용, false면 읽기용
     * @param yearNumber
     */
    renderLeaveBalances(balances, editable, yearNumber) {
        const el = document.getElementById('leaveBalances');
        const rows = balances || [];
        // parent 기반 정렬 시퀀스 만들기 (ENUMS.leaveTypes의 정의 순서 기준)
        const {leaveTypes = []} = ensureEnums();
        const parents = leaveTypes.filter(t => !t.parent).map(t => t.name);
        const childMap = new Map(); // parent -> [childName...]
        leaveTypes.forEach(t => {
            if (t.parent) {
                if (!childMap.has(t.parent)) childMap.set(t.parent, []);
                childMap.get(t.parent).push(t.name);
            }
        });
        const seq = [];
        parents.forEach(p => {
            seq.push(p);
            (childMap.get(p) || []).forEach(c => seq.push(c));
        });

        // balances를 seq 순서로 정렬 (seq에 없는 타입은 뒤로)
        const idx = (code) => {
            const i = seq.indexOf(code);
            return i === -1 ? Number.MAX_SAFE_INTEGER : i;
        };
        const rowsSorted = rows.slice().sort((a, b) => idx(a.leaveType) - idx(b.leaveType));

        // 합계
        const sum = rowsSorted.reduce((a, r) => {
            const t = Number(r.totalAllocated ?? 0);
            const u = Number(r.used ?? 0);
            return {total: a.total + t, used: a.used + u};
        }, {total: 0, used: 0});
        const sumRemain = Math.max(0, sum.total - sum.used);

        if (!editable) {
            // 보기 모드: 표 + 배지 + 진행바 + 합계
            el.innerHTML = `
              <table class="lb-table">
                <thead>
                  <tr>
                    <th style="width:28%">종류</th>
                    <th class="lb-num" style="width:14%">총 부여</th>
                    <th class="lb-num" style="width:14%">사용</th>
                    <th class="lb-num" style="width:14%">잔여</th>
                    <th class="lb-num" style="width:12%">근속 구분</th>
                    <th style="width:18%">진행</th>
                  </tr>
                </thead>
                <tbody>
${rowsSorted.map(r => {
                const code = r.leaveType;
                const {leaveTypes = []} = ensureEnums();
                const meta = leaveTypes.find(t => t.name === code);
                const isChild = !!meta?.parent;
                const label = meta?.krName || code;
                const total = Number(r.totalAllocated ?? 0);
                const used = Number(r.used ?? 0);
                const remain = Math.max(0, total - used);
                const pct = total > 0 ? Math.min(100, Math.round((used / total) * 100)) : 0;
                const yr = r.yearNumber ?? yearNumber ?? '';
                const yrLabel = yearNumberLabel(yr, this.userData?.yearNumber)
                // 들여쓰기 라벨 처리 (연차 하위)
                // const label = mapLeaveType(code);
                return `
                    <tr class="${isChild ? 'lb-child-row' : 'lb-parent-row'}">
                      <td class="lb-type-cell">
                        <span class="lb-type-badge ${mapLeaveClass(code)}">${label}</span>
                       </td>
                        <td class="lb-num">${formatLeaveDays(total)}</td>
                        <td class="lb-num">${formatLeaveDays(used)}</td>
                        <td class="lb-num">${formatLeaveDays(remain)}</td>
                        <td>${yrLabel}</td>
                        <td>
                          <div class="lb-bar"><span style="width:${pct}%"></span></div>
                          <div class="lb-small"><span class="lb-dim">사용</span><span>${pct}%</span></div>
                        </td>
                      </tr>`;
            }).join('')}
                  <tr class="lb-total-row">
                    <td>합계</td>
                    <td class="lb-num">${formatLeaveDays(sum.total)}</td>
                    <td class="lb-num">${formatLeaveDays(sum.used)}</td>
                    <td class="lb-num">${formatLeaveDays(sumRemain)}</td>
                    <td class="lb-num">—</td>
                    <td></td>
                  </tr>
                </tbody>
              </table>`;
            return;
        }

        // 편집 모드 (기존과 동일하되 step=0.01 유지)
        el.innerHTML = `
        <table class="lb-table">
          <thead>
            <tr>
              <th>종류</th>
              <th>총 부여</th>
              <th>사용</th>
              <th>잔여</th>
              <th>근속 구분</th>
            </tr>
          </thead>
          <tbody>
            ${rowsSorted.map(r => {
            const code = r.leaveType;
            const total = Number(r.totalAllocated ?? 0);
            const used = Number(r.used ?? 0);
            const remain = Math.max(0, total - used);
            const yr = r.yearNumber ?? yearNumber ?? '';
            const yrLabel = yearNumberLabel(yr, this.userData?.yearNumber);
            return `
                <tr>
                  <td class="lb-type-cell"><select class="lb-type">${leaveTypeOptionsHtml(code)}</select></td>
                      <td><input class="lb-total" type="number" step="0.001" min="0" value="${Number.isFinite(total) ? total : ''}"></td>
                  <td class="lb-num lb-unit">${fmt3(used)}</td>
                  <td class="lb-num lb-unit">${fmt3(remain)}</td>
                  <td> <div class="lb-small lb-dim">${yrLabel}</div></td>
                </tr>`;
        }).join('')}
          </tbody>
        </table>`;
    }


    /**
     * 팀 목록 조회 및 옵션 렌더링
     */
    async loadTeams() {
        const res = await fetch(`${this.apiPrefix}/teams`);
        if (!res.ok) throw new Error('팀 목록 로딩 실패');
        const teams = await res.json();
        this.renderTeamOptions(teams);
    }

    renderTeamOptions(teams) {
        const sel = document.getElementById('detailTeam');
        sel.innerHTML = teams
            .map(t => `<option value="${t.id}">${t.name}</option>`)
            .join('');
        if (this.userData?.teamId) sel.value = this.userData.teamId;
    }


    /**
     * 수정/삭제/뒤로 버튼 이벤트 바인딩
     */
    bindEvents() {
        const editBtn = document.getElementById('editProfileBtn');
        const editLeaveBtn = document.getElementById('editLeaveBtn')
        // const deactivateUserBtn = document.getElementById('deactivateUserBtn');
        const backBtn = document.getElementById('backBtn');

        if (editBtn) editBtn.addEventListener('click', this.onEditProfileToggle.bind(this));
        if (editLeaveBtn) editLeaveBtn.addEventListener('click', this.onEditBalancesToggle.bind(this))
        // if (deactivateUserBtn) deactivateUserBtn.addEventListener('click', this.onInActiveBtnClick.bind(this));
        if (backBtn) backBtn.addEventListener('click', this.onBackBtnClick.bind(this));

        // 연차 드롭다운 변경 시 데이터 재로드
        const yearSel = document.getElementById('lbYearSelect');
        if (yearSel) {
            yearSel.addEventListener('change', async (e) => {
                if (this.balEditing) return; // 편집 중에는 연차 변경 잠금
                this.selectedYear = parseInt(e.target.value, 10);
                await this.loadUser(this.selectedYear); // 해당 연차로 재조회(서버)
                // loadUser 내부에서 render + syncYearSelect 수행
            });
        }
    }

    // 탭 이벤트 바인딩
    bindTabEvents() {
        const tabs = document.querySelectorAll('.tab-nav li');
        tabs.forEach(tab => {
            tab.addEventListener('click', () => {
                const target = tab.dataset.tab; // 'profile' | 'leave'
                this.showTab(target);
            });
        });
    }

    // 탭 전환 로직
    showTab(target) {
        if (target === this.activeTab) return;

        // 편집 중이면 안전 처리 (자동 저장 X, 토글 종료)
        if (this.editing) {
            // 프로필 편집 중 탭 이동 시 편집 모드 해제
            this.editing = false;
            this.toggleProfileEditMode(false);
            const editBtn = document.getElementById('editProfileBtn');
            if (editBtn) editBtn.textContent = '프로필 수정';
        }
        if (this.balEditing) {
            // 연차 편집 중 탭 이동 시 편집 모드 해제(보기 모드로 렌더)
            this.balEditing = false;
            const editLeaveBtn = document.getElementById('editLeaveBtn');
            if (editLeaveBtn) editLeaveBtn.textContent = '연차 수정';
            this.renderLeaveBalances(this.userData?.leaveBalances || [], false, this.userData?.yearNumber);
        }

        // 섹션 토글
        document.getElementById('profileSection')?.classList.remove('active');
        document.getElementById('leaveSection')?.classList.remove('active');
        document.querySelectorAll('.tab-nav li').forEach(li => li.classList.remove('active'));

        if (target === 'profile') {
            document.getElementById('profileSection')?.classList.add('active');
            document.querySelector('.tab-nav li[data-tab="profile"]')?.classList.add('active');
        } else {
            // 탭 전환 직전에 최신 ENUM 보장
            ensureEnums();
            document.getElementById('leaveSection')?.classList.add('active');
            document.querySelector('.tab-nav li[data-tab="leave"]')?.classList.add('active');
            this.syncYearSelect();
            if (!this.balEditing) {
                this.renderLeaveBalances(this.userData?.leaveBalances || [], false, this.userData?.yearNumber);
            }
        }

        this.activeTab = target;
    }

    /**
     * [프로필 수정/저장] 토글
     */
    async onEditProfileToggle() {
        const btn = document.getElementById('editProfileBtn');
        if (!this.editing) {
            this.editing = true;
            this.toggleProfileEditMode(true);
            btn.textContent = '프로필 저장';
            return;
        }

        // 저장 로직
        const payload = {
            phoneNumber: document.getElementById('detailPhoneNumber').value,
            firstName: document.getElementById('detailFirstName').value,
            lastName: document.getElementById('detailLastName').value,
            role: document.getElementById('detailRole').value,
            status: document.getElementById('detailStatus').value,
            joinedDate: document.getElementById('detailJoinedDate').value,
            teamId: document.getElementById('detailTeam').value
        };
        try {
            const res = await fetch(`${this.apiPrefix}/admin/users/${this.userId}/update`, {
                method: 'POST',
                headers: {'Content-Type': 'application/json'},
                body: JSON.stringify(payload)
            });
            if (!res.ok) {
                const msg = await res.text(); // 서버가 JSON이면 res.json()
                throw new Error(msg || '수정 실패');
            }

            alert('사용자 정보가 저장되었습니다.');
            this.editing = false;
            this.toggleProfileEditMode(false);
            btn.textContent = '프로필 수정';

            // 상세→목록 복귀
            navigateTo('/team/users');
        } catch (error) {
            console.error(error);
            alert("수정 중 오류: " + (error.message || error));
        }
    }

    async onEditBalancesToggle(e) {
        const btn = e.currentTarget;
        if (!this.balEditing) {
            this.balEditing = true;
            this.setYearSelectDisabled(true);
            this.renderLeaveBalances(this.userData.leaveBalances || [], true, this.userData.yearNumber);
            btn.textContent = '연차 저장';
            return;
        }

        // 연차 저장
        const items = this.collectLeaveBalancesFromForm();
        await this.saveLeaveBalances(items);
        btn.textContent = '연차 수정';
        this.balEditing = false;
        this.setYearSelectDisabled(false);
        await this.loadUser();
    }

    collectLeaveBalancesFromForm() {
        const rows = Array.from(document.querySelectorAll('#leaveBalances tbody tr'));
        const selectedYear = this.selectedYear ?? this.userData?.yearNumber ?? null;
        return rows.map(tr => {
            const code = tr.querySelector('.lb-type')?.value?.trim();
            const total = tr.querySelector('.lb-total')?.value;
            const year = tr.querySelector('.lb-year')?.value;

            return {
                leaveType: code,
                totalAllocated: total ? Number(total) : null,
                yearNumber: selectedYear
            };
        }).filter(x => x.leaveType);
    }

    async saveLeaveBalances(items) {
        if (!Array.isArray(items) || items.length === 0) {
            alert('저장할 연차 항목이 없습니다.');
            return;
        }
        try {
            const res = await fetch(`${this.apiPrefix}/leaves/balances/users/${this.userId}`, {
                method: 'POST',
                headers: {'Content-Type': 'application/json'},
                body: JSON.stringify(items)
            });
            if (!res.ok) throw new Error(await res.text() || '연차 저장 실패');

            alert('연차 정보가 저장되었습니다.');
            // 최신 데이터 다시 로드
            await this.loadUser();
        } catch (e) {
            console.error(e);
            alert(e.message || e);
        }
    }

    /**
     * [뒤로] 처리
     */
    onBackBtnClick() {
        navigateTo('/team/users');
    }

    /**
     * 폼 필드 활성/비활성 제어
     */
    toggleProfileEditMode(enable) {
        ['detailPhoneNumber', 'detailFirstName', 'detailLastName', 'detailRole', 'detailStatus', 'detailJoinedDate', 'detailTeam']
            .forEach(id => document.getElementById(id).disabled = !enable);
    }

    // =========================
    // ✅ NEW: 연차 드롭다운 유틸
    // =========================
    syncYearSelect() {
        const sel = document.getElementById('lbYearSelect');
        const hint = document.getElementById('lbYearHint');
        if (!sel) return;

        const current = this.userData?.yearNumber ?? 1;
        const selected = this.selectedYear ?? current;

        sel.innerHTML = this.buildYearOptions(current, selected);
        sel.value = String(selected);
        sel.disabled = !!this.balEditing;

        if (hint) {
            hint.textContent = `현재 근속연차: ${current}년차`;
        }
    }

    buildYearOptions(current, selected) {
        // 1년차 ~ current년차까지 오름차순; 표시 텍스트는 “1년차 … (현재)”
        const opts = [];
        for (let i = 1; i <= current; i++) {
            const label = (i === current) ? `현재(${i}년차)` : `${i}년차`;
            opts.push(`<option value="${i}" ${i === selected ? 'selected' : ''}>${label}</option>`);
        }
        return opts.join('');
    }

    setYearSelectDisabled(disabled) {
        const sel = document.getElementById('lbYearSelect');
        if (sel) sel.disabled = !!disabled;
    }

}

/**
 * URL 에서 마지막 세그먼트를 ID로 추출
 */
function getUserIdFromUrl() {
    const segs = window.location.pathname.split('/');
    return segs[segs.length - 1] || null;
}

/**
 * 모듈 초기화 진입점
 */
export function initUserDetail() {
    const userId = getUserIdFromUrl();
    if (!userId) {
        alert('사용자 ID를 찾을 수 없습니다.');
        return;
    }
    const mgr = new UserDetailManager(userId);
    return mgr.init();
}
