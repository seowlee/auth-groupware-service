import {PaginationManager} from "./pagination.js";
import {navigateTo} from './router.js';
import {formatLeaveDays, mapLeaveType} from './leave-common.js';
import {showLoading, showMessage} from "./list-form-common.js";

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
        this.filters = {keyword: '', teamId: '', type: '', status: ''};
        this.sortField = 'appliedAt';
        this.sortDir = 'desc';
    }

    async init() {
        await this.loadTeams();
        this.pagination = new PaginationManager('paginationContainer', {
            onPageChange: page => this.loadLeaves(page),
            onPageSizeChange: () => this.loadLeaves(0),
        });
        this.pagination.init();
        this.bindEvents();
        await this.loadLeaves();
    }

    bindEvents() {
        document.getElementById('leaveSearchBtn').addEventListener('click', () => this.handleSearch());
        document.getElementById('leaveResetBtn').addEventListener('click', () => this.resetFilters());
        document.getElementById('refreshLeavesBtn').addEventListener('click', () => this.loadLeaves());
        document.querySelectorAll('.sortable').forEach(th =>
            th.addEventListener('click', e => this.handleSort(e.currentTarget.dataset.sort))
        );

    }

    async loadTeams() {
        try {
            const res = await fetch('/api/teams');
            if (!res.ok) throw new Error();
            const teams = await res.json();
            const sel = document.getElementById('leaveTeamId');
            sel.innerHTML = '<option value="">전체 팀</option>';
            teams.forEach(t => sel.insertAdjacentHTML('beforeend', `<option value="${t.id}">${t.name}</option>`));
        } catch {
            showMessage('팀 목록 로딩 실패', 'error');
        }
    }

    async loadLeaves(page = 0) {
        showLoading(true);
        this.collectFilters();
        const params = new URLSearchParams({
            page, size: this.pagination.getPageSize(),
            sort: `${this.sortField},${this.sortDir}`,
            ...this.filters
            // keyword: this.filters.keyword || '',
            // teamId: this.filters.teamId || '',
            // leaveType: this.filters.type || '',
            // status: this.filters.status || '',
            // // 필요 시 userUuid 추가 가능: userUuid: '...'
        });

        try {
            const res = await fetch(`/api/leaves?${params.toString()}`);
            if (!res.ok) throw new Error();
            const data = await res.json();
            this.renderLeaves(data.content || []);
            this.pagination.updatePagination(data);
        } catch (e) {
            showMessage('연차 목록 로딩 실패', 'error');
        } finally {
            showLoading(false);
        }
    }

    collectFilters() {
        this.filters.keyword = document.getElementById('leaveKeyword').value.trim();
        this.filters.teamId = document.getElementById('leaveTeamId').value;
        this.filters.type = document.getElementById('leaveType').value;
        this.filters.status = document.getElementById('leaveStatus').value;
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
        ['leaveKeyword', 'leaveTeamId', 'leaveType', 'leaveStatus'].forEach(id => {
            const el = document.getElementById(id);
            if (el) el.value = '';
        });
        this.handleSearch();
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
