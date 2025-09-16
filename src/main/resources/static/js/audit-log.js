import {PaginationManager} from './pagination.js';
import {showLoading, showMessage} from './list-form-common.js';

let _auditLogList = null;

export function initAuditLogList() {
    if (!_auditLogList) _auditLogList = new AuditLogList();
    _auditLogList.init();
}

class AuditLogList {
    constructor() {
        this.filters = {keyword: '', action: '', status: ''};
        this.sortField = 'createdAt';
        this.sortDir = 'desc';
    }

    async init() {
        this.pagination = new PaginationManager('paginationContainer', {
            onPageChange: (p) => this.loadLogs(p),
            onPageSizeChange: () => this.loadLogs(0),
        });
        this.pagination.init();
        this.bind();
        await this.loadLogs();
    }

    bind() {
        document.getElementById('logSearchBtn')?.addEventListener('click', () => this.handleSearch());
        document.getElementById('logResetBtn')?.addEventListener('click', () => this.reset());
        document.getElementById('refreshLogsBtn')?.addEventListener('click', () => this.loadLogs());

        document.querySelectorAll('.sortable').forEach(th => {
            th.addEventListener('click', (e) => this.handleSort(e.currentTarget.dataset.sort));
        });
    }

    collect() {
        this.filters.keyword = document.getElementById('logKeyword')?.value.trim() || '';
        this.filters.action = document.getElementById('logAction')?.value || '';
        this.filters.status = document.getElementById('logStatus')?.value || '';
    }

    async loadLogs(page = 0) {
        showLoading(true);
        this.collect();

        const params = new URLSearchParams({
            page,
            size: this.pagination.getPageSize(),
            sort: `${this.sortField},${this.sortDir}`,
            ...this.filters,
        });

        try {
            const res = await fetch(`/api/audit-logs?${params.toString()}`);
            if (!res.ok) throw new Error();
            const data = await res.json();
            this.render(data.content || []);
            this.pagination.updatePagination(data);
        } catch (e) {
            showMessage('로그 로딩 실패', 'error');
        } finally {
            showLoading(false);
        }
    }

    render(rows) {
        const body = document.getElementById('logTableBody');
        const table = document.getElementById('logTable');

        if (!rows.length) {
            body.innerHTML =
                '<tr><td colspan="6" style="text-align:center;color:#718096;font-weight:500;">검색 결과가 없습니다.</td></tr>';
            table.style.display = 'table';
            return;
        }

        body.innerHTML = rows.map(r => `
      <tr>
        <td>${this.fmtDate(r.createdAt)}</td>
        <td>${this.escape(r.createdBy ?? '-')}</td>
        <td>${r.userId ?? '-'}</td>
        <td>${this.escape(r.action)}</td>
        <td><span class="status-badge ${this.statusClass(r.status)}">${r.status}</span></td>
        <td>${this.escape(r.ipAddress ?? '-')}</td>
      </tr>
    `).join('');

        table.style.display = 'table';
    }

    handleSearch() {
        this.pagination.reset();
        this.loadLogs(0);
    }

    reset() {
        ['logKeyword', 'logAction', 'logStatus'].forEach(id => {
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
            this.sortDir = 'desc';
        }
        document.querySelectorAll('.sortable').forEach(th => th.classList.remove('asc', 'desc'));
        const cur = document.querySelector(`th[data-sort="${field}"]`);
        if (cur) cur.classList.add(this.sortDir);
        this.loadLogs(0);
    }

    // utils
    escape(s) {
        const d = document.createElement('div');
        d.textContent = s ?? '';
        return d.innerHTML;
    }

    fmtDate(iso) {
        if (!iso) return '';
        try {
            return new Date(iso).toLocaleString('ko-KR');
        } catch {
            return iso;
        }
    }

    statusClass(s) {
        // 기존 리스트의 배지 스타일 재사용
        // SUCCESS/FAILED/… → success/failed 로 맵핑
        const m = {
            SUCCESS: 'status-approved',
            FAILED: 'status-rejected',
            // 필요 시 추가: PENDING 등
        };
        return m[s] || '';
    }
}
