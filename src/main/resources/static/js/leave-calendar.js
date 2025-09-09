import {navigateTo} from './router.js';
import {showMessage} from './list-form-common.js';


let _leaveCalendarManager = null;

export function initLeaveCalendarManager() {
    if (!_leaveCalendarManager) {
        _leaveCalendarManager = new LeaveCalendarManager();
    }
    return _leaveCalendarManager.init();
}

class LeaveCalendarManager {
    constructor() {
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

            // 클릭 시 상세로 이동
            eventClick: (info) => {
                const id = info.event.id;
                if (id) navigateTo(`/leave/${id}`);
            },

            eventDidMount: (info) => {
                // 툴팁 title 보강(선택)
                const s = info.event.extendedProps?.extendedProps_status;
                if (s) info.el.title = `${info.event.title} · ${s}`;
            }
        });

        this.calendar.render();
    }

    refetch() {
        this.calendar?.refetchEvents();
    }
}
