// src/js/user-list.js
import {PaginationManager} from "./pagination.js";
import {navigateTo} from "./core/router.js";
import {showLoading, showMessage} from "./list-form-common.js";

let _userListManager = null;

export function initUserListManager(onRowClick = (userId) => {
    navigateTo(`/team/users/${userId}`);
}) {
    if (!_userListManager) {
        _userListManager = new UserListManager(onRowClick);
    }
    _userListManager.init();
}

class UserListManager {
    constructor(onRowClick) {
        this.onRowClick = onRowClick;
        this.filters = {keyword: '', teamId: '', role: '', status: ''};
        this.sortField = 'joinedDate';
        this.sortDir = 'desc';

    }

    async init() {
        await this.loadTeams();
        this.pagination = new PaginationManager('paginationContainer', {
            onPageChange: page => this.loadUsers(page),
            onPageSizeChange: () => this.loadUsers(0),
        });
        this.pagination.init();

        this.bindEvents();
        await this.loadUsers();
    }

    bindEvents() {
        document.getElementById('searchBtn').addEventListener('click', () => this.handleSearch());
        document.getElementById('resetFilterBtn').addEventListener('click', () => this.resetFilters());
        document.getElementById('refreshUsersBtn').addEventListener('click', () => this.loadUsers());
        document.querySelectorAll('.sortable').forEach(header =>
            header.addEventListener('click', e => this.handleSort(e.currentTarget.dataset.sort))
        );
    }

    async loadTeams() {
        try {
            const res = await fetch('/api/teams');
            if (!res.ok) throw new Error();
            const teams = await res.json();
            const sel = document.getElementById('filterTeamId');
            sel.innerHTML = '<option value="">전체 팀</option>';
            teams.forEach(t => {
                sel.insertAdjacentHTML('beforeend', `<option value="${t.id}">${t.name}</option>`);
            });
        } catch {
            showMessage('팀 목록 로딩 실패', 'error');
        }
    }

    async loadUsers(page = 0) {
        showLoading(true);
        this.collectFilters();
        const params = new URLSearchParams({
            page, size: this.pagination.getPageSize(),
            sort: `${this.sortField},${this.sortDir}`,
            ...this.filters
        });
        try {
            const res = await fetch(`/api/team/users?${params}`);
            if (!res.ok) throw new Error();
            const data = await res.json();
            // const data = await fetchWithCsrf(`/api/team/users?${params}`);
            this.renderUsers(data.content);
            this.pagination.updatePagination(data);
        } catch (e) {
            console.error('[loadUsers] failed:', e);
            showMessage('사용자 목록 로딩 실패', 'error');
        } finally {
            showLoading(false);
        }
    }

    collectFilters() {
        this.filters.keyword = document.getElementById('searchKeyword').value;
        this.filters.teamId = document.getElementById('filterTeamId').value;
        this.filters.role = document.getElementById('filterRole').value;
        this.filters.status = document.getElementById('filterStatus').value;
    }

    renderUsers(users) {
        const body = document.getElementById('userTableBody');
        const table = document.getElementById('userTable');
        if (!users.length) {
            body.innerHTML = '<tr><td colspan="6" style="text-align:center; color: #718096; font-weight: 500;">검색 결과가 없습니다.</td></tr>';

        } else {
            body.innerHTML = users.map(u => `
                <tr class="user-row" data-user-id="${u.uuid}">
                  <td>${u.username}</td>
                  <td>${this.mapRole(u.role)}</td><td>${u.teamName || '-'}</td>
                  <td>${u.joinedDate}</td>
                  <td>
                    <span class="status-badge ${this.mapStatusClass(u.status)}">
                      ${this.mapStatus(u.status)}
                    </span>
                  </td>    
                </tr>`
            ).join('');
            body.querySelectorAll('.user-row').forEach(row =>
                row.addEventListener('click', () => this.onRowClick(row.dataset.userId))
            );
        }
        table.style.display = 'table';
    }


    handleSearch() {
        this.pagination.reset();
        this.loadUsers(0);
    }

    resetFilters() {
        ['searchKeyword', 'filterTeamId', 'filterRole', 'filterStatus'].forEach(id =>
            document.getElementById(id).value = ''
        );
        this.handleSearch();
    }

    handleSort(field) {
        if (this.sortField === field) {
            this.sortDir = this.sortDir === 'asc' ? 'desc' : 'asc';
        } else {
            this.sortField = field;
            this.sortDir = 'asc';
        }
        // 정렬 아이콘 클래스 초기화
        document.querySelectorAll('.sortable').forEach(th => {
            th.classList.remove('asc', 'desc');
        });

        // 현재 정렬 필드에 클래스 추가
        const currentTh = document.querySelector(`th[data-sort="${field}"]`);
        if (currentTh) {
            currentTh.classList.add(this.sortDir);
        }
        this.loadUsers(0);
    }

    // navigateToDetail(userId) {
    //     const path = `/team/users/${userId}`;
    //     history.pushState({path}, '', path);
    //     window.loadPageIntoMainContent(path);
    // }


    mapRole(r) {
        return {SUPER_ADMIN: '최고관리자', TEAM_LEADER: '팀장', TEAM_MEMBER: '팀원'}[r] || r;
    }

    mapStatusClass(status) {
        return {
            ACTIVE: 'status-active',
            INACTIVE: 'status-inactive',
            PENDING: 'status-pending',
        }[status] || '';
    }

    mapStatus(s) {
        return {ACTIVE: '활성', INACTIVE: '비활성', PENDING: '승인대기'}[s] || s;
    }

    getCurrentPage() {
        console.log("this currentpage " + this.pagination.getCurrentPage());
        return this.pagination.getCurrentPage();
    }
}

