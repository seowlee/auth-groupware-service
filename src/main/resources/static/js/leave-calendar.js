import {navigateTo} from './router.js';
import {showMessage} from './list-form-common.js';


let _leaveCalendarManager = null;

export function initLeaveCalendarManager(onRowClick = (leaveId) => {
    navigateTo(`/leaves/${leaveId}`);
}) {
    if (!_leaveCalendarManager) {
        _leaveCalendarManager = new LeaveCalendarManager(onRowClick);
    }
    return _leaveCalendarManager.init();
}

class LeaveCalendarManager {
    constructor(onRowClick) {
        this.onRowClick = onRowClick;
        this.apiPrefix = '/api';
        this.calendar = null;
        this.filters = {teamId: '', type: '', status: ''};
    }

    async init() {
        await this.loadTeams();
        this.bindEvents();
        this.initCalendar();
    }

    async loadTeams() {
        try {
            const res = await fetch(`${this.apiPrefix}/teams`);
            if (!res.ok) throw new Error('팀 목록 로딩 실패');
            const teams = await res.json();
            const sel = document.getElementById('calTeamFilter');
            sel.innerHTML = `<option value="">전체 팀</option>` + teams.map(t => `<option value="${t.id}">${t.name}</option>`).join('');
        } catch (e) {
            console.error(e);
            showMessage?.('팀 목록을 불러오지 못했습니다.', 'error');
        }
    }

    bindEvents() {
        const $ = id => document.getElementById(id);

        $('calSearchBtn')?.addEventListener('click', () => {
            this.filters.teamId = $('calTeamFilter').value || '';
            this.filters.type = $('calTypeFilter').value || '';
            this.filters.status = $('calStatusFilter').value || '';
            this.refetch();
        });

        $('calResetBtn')?.addEventListener('click', () => {
            $('calTeamFilter').value = '';
            $('calTypeFilter').value = '';
            $('calStatusFilter').value = '';
            this.filters = {teamId: '', type: '', status: ''};
            this.refetch();
        });
    }

    initCalendar() {
        const el = document.getElementById('leaveCalendar');
        if (!el) return;

        this.calendar = new FullCalendar.Calendar(el, {
            initialView: 'dayGridMonth',
            height: 'auto',
            locale: 'ko',
            nowIndicator: true,
            navLinks: true,
            headerToolbar: {
                left: 'prev,next today',
                center: 'title',
                right: 'dayGridMonth,timeGridWeek,timeGridDay'
            },
            eventTimeFormat: {hour: '2-digit', minute: '2-digit'},
            displayEventTime: true,

            // FullCalendar가 start/end 쿼리를 자동으로 넘겨줌
            events: {
                url: `${this.apiPrefix}/leaves/calendar`,
                method: 'GET',
                extraParams: () => ({
                    teamId: this.filters.teamId,
                    type: this.filters.type,
                    status: this.filters.status,
                }),
                failure: () => showMessage?.('연차 이벤트 로딩 실패', 'error')
            },
            // 공휴일(배경) — FullCalendar가 start/end를 자동으로 전달
            eventSources: [
                {
                    url: `${this.apiPrefix}/holidays`,
                    method: 'GET',
                    // v6: 소스별 변환 함수는 eventDataTransform 사용
                    eventDataTransform: (h) => {
                        // end는 '다음날 0시(배타)'로 맞춰줘야 셀 전체가 칠해짐
                        const add1 = (ds) => {
                            const d = new Date(ds + 'T00:00:00');
                            d.setDate(d.getDate() + 1);
                            return d.toISOString().slice(0, 10);
                        };
                        return {
                            title: '',
                            start: h.date,
                            end: add1(h.date),
                            display: 'background',
                            classNames: ['holiday-bg'],
                            extendedProps: {holidayName: h.name}
                        };
                    }
                }
            ],
            // 클릭 시 상세로 이동
            eventClick: (info) => {
                if (info.event.display === 'background') return; // 공휴일 클릭 무시
                const id = info.event.id;
                if (id && this.onRowClick) this.onRowClick(id);
            },

            eventDidMount: (info) => {
                if (info.event.display === 'background') {
                    // 공휴일: 셀 자체에 표시 강화 (배경 + 뱃지)
                    const name = info.event.extendedProps?.holidayName || '';
                    if (name) info.el.title = name;
                    const cell = info.el.closest('.fc-daygrid-day');
                    if (cell && !cell.classList.contains('is-holiday')) {
                        cell.classList.add('is-holiday');
                        const top = cell.querySelector('.fc-daygrid-day-top');
                        if (top) {
                            const badge = document.createElement('div');
                            badge.className = 'holiday-badge';
                            badge.textContent = info.event.title;
                            badge.textContent = name;
                            top.appendChild(badge);
                        }
                    }
                    return;
                }
                const statusColor = {
                    APPROVED: '#22c55e', // green
                    PENDING: '#f59e0b', // amber
                    REJECTED: '#ef4444', // red
                    CANCELED: '#9ca3af'  // gray
                };
                const typeColor = {
                    ANNUAL: '#3b82f6', // blue
                    SICK: '#06b6d4', // cyan
                    BIRTHDAY: '#a855f7', // purple
                    ADVANCE: '#10b981'  // emerald
                };

                const s = info.event.extendedProps?.status; // ← extendedProps.status 로 접근
                const t = info.event.extendedProps?.type;

                const color = statusColor[s] || typeColor[t];
                if (color) {
                    info.el.style.backgroundColor = color;
                    info.el.style.borderColor = color;
                    info.el.style.color = '#fff';
                }
                if (s) info.el.title = `${info.event.title} · ${s}`;
            }
        });

        this.calendar.render();
    }

    refetch() {
        this.calendar?.refetchEvents();
    }
}
