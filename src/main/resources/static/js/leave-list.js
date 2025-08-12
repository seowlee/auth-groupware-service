import {PaginationManager} from "./pagination.js";
import {navigateTo} from './router.js';

let _leaveListManager = null;

export function initLeaveListManager(onRowClick = (leaveId) => {
    navigateTo(`/leave/${leaveId}`);
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
        // // 폼 토글 버튼
        // document.getElementById('toggleLeaveFormBtn').addEventListener('click', () => {
        //     this.toggleLeaveForm();
        // });
        //
        // // 취소 버튼
        // document.getElementById('cancelLeaveFormBtn').addEventListener('click', () => {
        //     this.hideLeaveForm();
        // });
        //
        // // 폼 제출
        // document.getElementById('createLeaveForm').addEventListener('submit', (e) => {
        //     e.preventDefault();
        //     this.submitLeaveForm();
        // });

        // // 종료일이 시작일보다 이전이 되지 않도록 검증
        // document.getElementById('startDate').addEventListener('change', () => {
        //     this.validateDateRange();
        // });
        //
        // document.getElementById('endDate').addEventListener('change', () => {
        //     this.validateDateRange();
        // });
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
            this.showMessage('팀 목록 로딩 실패', 'error');
        }
    }

    async loadLeaves(page = 0) {
        this.showLoading(true);
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
            this.showMessage('연차 목록 로딩 실패', 'error');
        } finally {
            this.showLoading(false);
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
            const start = this.formatDate(r.startTime);
            const end = this.formatDate(r.endTime);
            const days = this.calculateDays(r.startTime, r.endTime);
            return `
        <tr class="leave-row" data-id="${r.id}">
          <td>${this.escape(r.userName || '-')}</td>
          <td>${this.mapType(r.leaveType)}</td>
          <td>${start}</td>
          <td>${end}</td>
          <td>${days}일</td>
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
    showLoading(flag) {
        const el = document.querySelector('.loading');
        if (el) el.style.display = flag ? 'block' : 'none';
    }

    showMessage(msg, type = 'info') {
        const ma = document.getElementById('messageArea');
        if (!ma) return;
        ma.innerHTML = `<div class="message ${type}">${msg}</div>`;
        setTimeout(() => ma.innerHTML = '', 3000);
    }

    escape(s) {
        const d = document.createElement('div');
        d.textContent = s ?? '';
        return d.innerHTML;
    }

    calculateDays(startIso, endIso) {
        if (!startIso || !endIso) return 0;
        const s = new Date(startIso), e = new Date(endIso);
        return Math.max(1, Math.ceil((e - s) / (1000 * 60 * 60 * 24)) + 1);
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
        return ({PENDING: 'status-pending', APPROVED: 'status-approved', REJECTED: 'status-rejected'}[s] || '');
    }

    validateDateRange() {
        const startDate = document.getElementById('startDate').value;
        const endDate = document.getElementById('endDate').value;

        if (startDate && endDate && new Date(endDate) < new Date(startDate)) {
            document.getElementById('endDate').value = startDate;
        }
    }

    // toggleLeaveForm() {
    //     const container = document.getElementById('leaveFormContainer');
    //     const btn = document.getElementById('toggleLeaveFormBtn');
    //
    //     if (container.classList.contains('show')) {
    //         this.hideLeaveForm();
    //     } else {
    //         this.showLeaveForm();
    //     }
    // }

    // showLeaveForm() {
    //     const container = document.getElementById('leaveFormContainer');
    //     const btn = document.getElementById('toggleLeaveFormBtn');
    //
    //     container.classList.add('show');
    //     btn.textContent = '신청 취소';
    //
    //     // 폼 초기화
    //     document.getElementById('createLeaveForm').reset();
    //
    //     // 오늘 날짜를 기본값으로 설정
    //     const today = new Date().toISOString().split('T')[0];
    //     document.getElementById('startDate').value = today;
    //     document.getElementById('endDate').value = today;
    // }
    //
    // hideLeaveForm() {
    //     const container = document.getElementById('leaveFormContainer');
    //     const btn = document.getElementById('toggleLeaveFormBtn');
    //
    //     container.classList.remove('show');
    //     btn.textContent = '연차 신청';
    // }

    //
    // renderLeaveList() {
    //     const container = document.getElementById('leaveListArea');
    //
    //     if (this.leaves.length === 0) {
    //         container.innerHTML = `
    //                 <div class="empty-state">
    //                     <h3>등록된 연차가 없습니다</h3>
    //                     <p>새로운 연차를 신청해보세요.</p>
    //                 </div>
    //             `;
    //         return;
    //     }
    //
    //     const table = `
    //             <table class="data-table">
    //                 <thead>
    //                     <tr>
    //                         <th>신청자</th>
    //                         <th>연차 유형</th>
    //                         <th>시작일</th>
    //                         <th>종료일</th>
    //                         <th>일수</th>
    //                         <th>사유</th>
    //                         <th>상태</th>
    //                         <th>신청일</th>
    //                     </tr>
    //                 </thead>
    //                 <tbody>
    //                     ${this.leaves.map(leave => `
    //                         <tr>
    //                             <td>${this.escapeHtml(leave.username || leave.applicant || '알 수 없음')}</td>
    //                             <td>${this.getLeaveTypeText(leave.leaveType || leave.type)}</td>
    //                             <td>${leave.startDate}</td>
    //                             <td>${leave.endDate}</td>
    //                             <td>${this.calculateDays(leave.startDate, leave.endDate)}일</td>
    //                             <td>${this.escapeHtml(leave.reason || '')}</td>
    //                             <td class="status-${(leave.status || 'pending').toLowerCase()}">
    //                                 ${this.getStatusText(leave.status || 'PENDING')}
    //                             </td>
    //                             <td>${this.formatDate(leave.createdDate || leave.applicationDate || '')}</td>
    //                         </tr>
    //                     `).join('')}
    //                 </tbody>
    //             </table>
    //         `;
    //
    //     container.innerHTML = table;
    // }

    // async submitLeaveForm() {
    //     try {
    //         const formData = new FormData(document.getElementById('createLeaveForm'));
    //         const leaveData = Object.fromEntries(formData.entries());
    //
    //         this.showMessage('연차를 신청하는 중...', 'loading');
    //
    //         const response = await fetch('/api/leaves', {
    //             method: 'POST',
    //             headers: {
    //                 'Content-Type': 'application/json',
    //             },
    //             body: JSON.stringify(leaveData)
    //         });
    //
    //         if (!response.ok) {
    //             const errorData = await response.text();
    //             throw new Error(errorData || `HTTP error! status: ${response.status}`);
    //         }
    //
    //         this.showMessage('연차가 성공적으로 신청되었습니다.', 'success');
    //         this.hideLeaveForm();
    //         this.loadLeaves(); // 목록 새로고침
    //
    //     } catch (error) {
    //         console.error('연차 신청 실패:', error);
    //         this.showMessage('연차 신청에 실패했습니다: ' + error.message, 'error');
    //     }
    // }


    clearMessage() {
        document.getElementById('messageArea').innerHTML = '';
    }

    escapeHtml(text) {
        const div = document.createElement('div');
        div.textContent = text;
        return div.innerHTML;
    }
}
