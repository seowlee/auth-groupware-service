import {navigateTo} from './router.js';
import {fmt3, leaveTypeOptionsHtml, mapLeaveClass, mapLeaveType, yearNumberLabel} from './leave-common.js';

/**
 * 사용자 상세 정보 관리자
 */
class UserDetailManager {
    static STATUS_LABELS = {
        ACTIVE: '활성',
        INACTIVE: '비활성',
        PENDING: '승인대기',
    };

    constructor(userId) {
        this.userId = userId;
        this.userData = null;
        this.editing = false;
        this.balEditing = false;
        this.apiPrefix = '/api';
    }

    /**
     * 초기화: 사용자/팀 데이터 로드 후 이벤트 바인딩
     */
    async init() {
        await this.loadUser();
        await this.loadTeams();
        this.bindEvents();
    }

    /**
     * 사용자 정보 조회 및 렌더링
     */
    async loadUser() {
        const res = await fetch(`${this.apiPrefix}/team/users/${this.userId}?includeBalances=true`);
        if (!res.ok) throw new Error('사용자 조회 실패');
        this.userData = await res.json();
        this.render(this.userData);
    }

    /**
     * 화면에 사용자 정보 반영
     */
    render(user) {
        document.getElementById('detailUsername').textContent = user.username;
        document.getElementById('detailEmail').textContent = user.email;
        document.getElementById('detailPhoneNumber').value = user.phoneNumber;
        document.getElementById('detailFirstName').value = user.firstName;
        document.getElementById('detailLastName').value = user.lastName;
        document.getElementById('detailJoinedDate').value = user.joinedDate;
        // 상태
        const userStatus = document.getElementById('detailStatus');
        userStatus.innerHTML = [
            {v: 'ACTIVE', t: '활성'},
            {v: 'INACTIVE', t: '비활성'},
            {v: 'PENDING', t: '승인대기'}
        ].map(o => `<option value="${o.v}">${o.t}</option>`).join('');
        userStatus.value = user.status;
        // 역할 셀렉트
        const roleSelect = document.getElementById('detailRole');
        roleSelect.innerHTML = [
            {v: 'TEAM_MEMBER', t: '팀원'},
            {v: 'TEAM_LEADER', t: '팀장'},
            {v: 'SUPER_ADMIN', t: '최고관리자'}
        ].map(o => `<option value="${o.v}">${o.t}</option>`).join('');
        roleSelect.value = user.role;

        this.renderLeaveBalances(user.leaveBalances || [], false, user.yearNumber);
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
        const sum = rows.reduce((a, r) => {
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
                  ${rows.map(r => {
                const code = r.leaveType;
                const total = Number(r.totalAllocated ?? 0);
                const used = Number(r.used ?? 0);
                const remain = Math.max(0, total - used);
                const pct = total > 0 ? Math.min(100, Math.round((used / total) * 100)) : 0;
                const yr = r.yearNumber ?? yearNumber ?? '';
                const yrLabel = yearNumberLabel(yr, yearNumber);
                return `
                      <tr>
                        <td><span class="lb-type-badge ${mapLeaveClass(code)}">${mapLeaveType(code)}</span></td>
                        <td class="lb-num lb-unit">${fmt3(total)}</td>
                        <td class="lb-num lb-unit">${fmt3(used)}</td>
                        <td class="lb-num lb-unit">${fmt3(remain)}</td>
                        <td>${yrLabel}</td>
                        <td>
                          <div class="lb-bar"><span style="width:${pct}%"></span></div>
                          <div class="lb-small"><span class="lb-dim">사용</span><span>${pct}%</span></div>
                        </td>
                      </tr>`;
            }).join('')}
                  <tr class="lb-total-row">
                    <td>합계</td>
                    <td class="lb-num lb-unit">${fmt3(sum.total)}</td>
                    <td class="lb-num lb-unit">${fmt3(sum.used)}</td>
                    <td class="lb-num lb-unit">${fmt3(sumRemain)}</td>
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
            ${rows.map(r => {
            const code = r.leaveType;
            const total = Number(r.totalAllocated ?? 0);
            const used = Number(r.used ?? 0);
            const remain = Math.max(0, total - used);
            const yr = r.yearNumber ?? yearNumber ?? '';
            const yrLabel = yearNumberLabel(yr, yearNumber);
            return `
                <tr>
                  <td><select class="lb-type">${leaveTypeOptionsHtml(code)}</select></td>
                  <td><input class="lb-total" type="number" step="0.001" min="0" value="${total || ''}"></td>
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
            this.renderLeaveBalances(this.userData.leaveBalances || [], true, this.userData.yearNumber);
            btn.textContent = '연차 저장';
            return;
        }

        // 연차 저장
        const items = this.collectLeaveBalancesFromForm();
        await this.saveLeaveBalances(items);
        btn.textContent = '연차 수정';
        await this.loadUser();
    }

    collectLeaveBalancesFromForm() {
        const rows = Array.from(document.querySelectorAll('#leaveBalances tbody tr'));
        return rows.map(tr => {
            const code = tr.querySelector('.lb-type')?.value?.trim();
            const total = tr.querySelector('.lb-total')?.value;
            const year = tr.querySelector('.lb-year')?.value;

            return {
                leaveType: code,
                totalAllocated: total ? Number(total) : null,
                yearNumber: year ? Number(year) : null
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
        ['detailFirstName', 'detailLastName', 'detailRole', 'detailStatus', 'detailJoinedDate', 'detailTeam']
            .forEach(id => document.getElementById(id).disabled = !enable);
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
