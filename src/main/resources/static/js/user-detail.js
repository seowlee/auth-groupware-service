import {navigateTo} from './router.js';

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
        const res = await fetch(`${this.apiPrefix}/team/users/${this.userId}`);
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
        document.getElementById('detailFirstName').value = user.firstName;
        document.getElementById('detailLastName').value = user.lastName;
        document.getElementById('detailJoinedDate').textContent = user.joinedDate;
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

        // 연차 잔여 일수 목록
        const leaveListEl = document.getElementById('leaveStats');
        leaveListEl.innerHTML = (user.leaveBalances || [])
            .map(l => `<li>${l.typeName} : ${l.remainingDays}일</li>`)
            .join('');
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
        const editBtn = document.getElementById('toggleEditBtn');
        const deactivateUserBtn = document.getElementById('deactivateUserBtn');
        const backBtn = document.getElementById('backBtn');

        if (editBtn) editBtn.addEventListener('click', this.onEditBtnClick.bind(this));
        // if (deactivateUserBtn) deactivateUserBtn.addEventListener('click', this.onInActiveBtnClick.bind(this));
        if (backBtn) backBtn.addEventListener('click', this.onBackBtnClick.bind(this));
    }

    /**
     * [수정/저장] 토글
     */
    async onEditBtnClick() {
        const btn = document.getElementById('toggleEditBtn');
        if (!this.editing) {
            this.editing = true;
            this.toggleEditMode(true);
            btn.textContent = '저장';
            return;
        }

        // 저장 로직
        const payload = {
            firstName: document.getElementById('detailFirstName').value,
            lastName: document.getElementById('detailLastName').value,
            role: document.getElementById('detailRole').value,
            status: document.getElementById('detailStatus').value,
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
            this.toggleEditMode(false);
            btn.textContent = '수정';

            // 상세→목록 복귀
            navigateTo('/team/users');
        } catch (error) {
            console.error(error);
            alert("수정 중 오류: " + (error.message || error));
        }
    }

    // /**
    //  * [비활성화] 처리
    //  */
    // async onInActiveBtnClick() {
    //     if (!confirm('정말 비활성화하시겠습니까?')) return;
    //     const res = await fetch(`${this.apiPrefix}/admin/users/${this.userId}/deactivate`, {
    //         method: 'POST'
    //     });
    //     if (!res.ok) throw new Error('비활성화 실패');
    //     alert('사용자가 비활성화되었습니다.');
    //     navigateTo('/team/users');
    // }

    /**
     * [뒤로] 처리
     */
    onBackBtnClick() {
        navigateTo('/team/users');
    }

    /**
     * 폼 필드 활성/비활성 제어
     */
    toggleEditMode(enable) {
        ['detailFirstName', 'detailLastName', 'detailRole', 'detailStatus', 'detailTeam']
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
