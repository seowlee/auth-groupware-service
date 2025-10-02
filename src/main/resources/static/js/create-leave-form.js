import {navigateTo} from "./core/router.js";
import {apiFetch, readErrorMessage} from "./list-form-common.js"; // [ADD]
import {validateDateTimes} from "./leave-common.js"; // [ADD]

class CreateLeaveFormManager {
    constructor() {
        this.apiPrefix = "/api/leaves";
        this.form = null;
        this.applicantSel = null;
        this.isSuperAdmin = false;
        this.holidayMap = null; // Map('YYYY-MM-DD' -> '휴일명')
    }

    async init() {
        this.form = document.getElementById("form-container");
        if (!this.form) {
            console.warn("form-container 이 존재하지 않음");
            return;
        }
        this.isSuperAdmin = (this.form.dataset.isSuperAdmin || "").toLowerCase() === "true";
        this.applicantSel = document.getElementById("applicantUuid");
        if (!this.applicantSel) {
            console.warn("applicantUuid 요소가 없음");
            return;
        }
        // 관리자만 전체 사용자 목록 fetch
        if (this.isSuperAdmin) {
            await this.loadApplicants(); // 필요 시 defaultUuid를 인자로 넘기기
        }
        this.bindEvents();             // 이벤트 바인딩(힌트/차단 포함)
        await this.initDatePickers(); // Air Datepicker 달력 구동
    }

    /**
     * 신청자 목록 로드 (SUPER_ADMIN만 전체 유저 로드, 일반사용자는 본인으로 고정)
     */
    async loadApplicants(defaultUuid = "") {
        try {
            // SUPER_ADMIN: 전체 또는 검색 결과 로드
            const res = await apiFetch('/api/team/users/applicants');
            if (!res.ok) throw new Error("사용자 목록 로딩 실패");
            const users = await res.json(); // [{userUuid, username, email}, ...]
            this.renderApplicantOptions(users, defaultUuid);
        } catch (err) {
            console.error(err);
        }

    }

    renderApplicantOptions(users, defaultUuid = '') {
        this.applicantSel.innerHTML = [
            '<option value="">신청자를 선택하세요</option>',
            ...users.map(u =>
                `<option value="${u.userUuid}">${u.username}${u.email ? ` (${u.email})` : ''}</option>`
            )
        ].join("");
        // 기본값(원하면 자기자신) 설정 가능
        if (defaultUuid) this.applicantSel.value = defaultUuid;
    }

    bindEvents() {
        this.form.addEventListener("submit", this.handleSubmit.bind(this));
        const cancelBtn = document.getElementById("cancelFormBtn");
        if (cancelBtn) cancelBtn.addEventListener("click", this.onCancelBtnClick.bind(this));
        //  시작일 변경 시, 종료일 최소값을 시작일로 묶기
        // const s = document.getElementById("startDate");
        // const e = document.getElementById("endDate");
        // // const sHint = document.getElementById("startDateHint");
        // // const eHint = document.getElementById("endDateHint");
        // if (s && e) {
        //         const syncMin = () => {
        //             if (s.value) e.min = s.value;         // 종료일은 시작일 이후만
        //             if (e.value && s.value && e.value < s.value) {
        //                 e.value = s.value;                   // 잘못 선택되어 있으면 맞춰주기
        //             }
        //             // this.updateDateHint(s, sHint);
        //             // this.updateDateHint(e, eHint);
        //         };
        //         s.addEventListener("change", syncMin);
        //         // e.addEventListener("change", () => this.updateDateHint(e, eHint));
        //         // 초기 1회 동기화
        //         syncMin();
        //
        // }
    }

    /** 올해+내년 공휴일을 로드해 Map(date→name)으로 캐시 */
    async loadHolidayMap() {
        if (this.holidayMap) return this.holidayMap;
        const y = new Date().getFullYear();
        const fetchYear = async (yy) => (await (await fetch(`/api/holidays/year/${yy}`)).json());
        const [a, b] = await Promise.all([fetchYear(y), fetchYear(y + 1)]);
        this.holidayMap = new Map([...a, ...b].map(h => [h.date, h.name]));
        return this.holidayMap;
    }

    /** Flatpickr 초기화: 공휴일/주말 비활성 + 시작 선택 시 종료 minDate 연동 */
    async initDatePickers() {
        if (!window.flatpickr) return;
        const sEl = document.getElementById("startDate");
        const eEl = document.getElementById("endDate");
        if (!sEl || !eEl) return;

        flatpickr.localize(flatpickr.l10ns.ko);
        const holidayMap = await this.loadHolidayMap();
        const fmtLocal = (d) => {
            const p = (n) => (n < 10 ? "0" + n : "" + n);
            return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())}`;
        };
        const isHoliday = (d) => holidayMap.has(fmtLocal(d));
        const isWeekend = (d) => [0, 6].includes(d.getDay());

        // 🔒 정책: 무엇을 막을지 선택 (둘 다 true 권장)
        const BLOCK_HOLIDAYS = true;
        const BLOCK_WEEKENDS = true;

        // disable 규칙(Flatpickr는 true를 반환하면 그 날짜를 비활성화함)
        const disableFn = (d) =>
            (BLOCK_WEEKENDS && isWeekend(d)) ||
            (BLOCK_HOLIDAYS && isHoliday(d));

        // 셀에 시각적 표시(주말/공휴일 클래스 + 툴팁)
        const decorate = (dObj, dStr, fp, dayElem) => {
            const d = dayElem.dateObj;
            if (isWeekend(d)) dayElem.classList.add('fp-weekend');
            if (isHoliday(d)) {
                dayElem.classList.add('fp-holiday');
                dayElem.title = holidayMap.get(fmtLocal(d)) || '공휴일';
            }
        };

        // 현재 시작값(있으면) → 로컬 00:00으로 정규화
        const parseYmd = (v) => v ? new Date(v + 'T00:00') : null;
        const toLocalMidnight = (d) => new Date(d.getFullYear(), d.getMonth(), d.getDate());
        let startMin = parseYmd(sEl.value);            // 시작일 최소값(동적으로 바뀜)
        if (startMin) startMin = toLocalMidnight(startMin);

        // 종료 달력에서 "시작일 이전"을 막기 위한 동적 disable 함수
        const beforeStartFn = (d) => (startMin ? d < startMin : false);
        const endPicker = flatpickr(eEl, {
            dateFormat: 'Y-m-d',
            minDate: startMin || 'today',
            disable: [disableFn, beforeStartFn],  // 🔒 주말/공휴일 + 시작일 이전 모두 차단
            onDayCreate: decorate,
            onReady: (_sd, _ds, fp) => fp.calendarContainer.classList.add('end-cal'),
            onOpen: () => {                 // 열릴 때 현재 시작일 기준으로 뷰 이동
                const v = sEl.value;
                if (v) endPicker.jumpToDate(new Date(v + 'T00:00'));
            },
            disableMobile: true,              // 모바일도 일관된 UI
            position: 'auto center',
            prevArrow: '‹', nextArrow: '›'
        });

        const startPicker = flatpickr(sEl, {
            dateFormat: 'Y-m-d',
            disable: [disableFn],
            onDayCreate: decorate,
            onChange: ([d]) => {
                if (!d) return;
                endPicker.set('minDate', d);   // 제한만 바꾸고
                endPicker.clear();             // 종료일 값은 비우기(다시 고르게)
                endPicker.jumpToDate(d);       // 뷰를 시작일 위치로 이동
            },
            onReady: (_sd, _ds, fp) => fp.calendarContainer.classList.add('start-cal'),
            disableMobile: true,
            position: 'auto center',
            prevArrow: '‹', nextArrow: '›'
        });

        // 폼이 열릴 때부터 시작값이 있었던 경우 초기 동기화
        if (startMin) {
            endPicker.set('disable', [disableFn, beforeStartFn]);
            endPicker.clear();
            endPicker.jumpToDate(startMin);
        }
        // 🔘 아이콘 버튼으로 달력 열기
        const sBtn = sEl.closest('.date-field')?.querySelector('.calendar-btn');
        if (sBtn) sBtn.addEventListener('click', () => startPicker.open());
        const eBtn = eEl.closest('.date-field')?.querySelector('.calendar-btn');
        if (eBtn) eBtn.addEventListener('click', () => endPicker.open());
    }

    collectFormData() {
        const startDateTime = `${document.getElementById("startDate")?.value}T${document.getElementById("startTime")?.value}`;
        const endDateTime = `${document.getElementById("endDate")?.value}T${document.getElementById("endTime")?.value}`;
        return {
            // username: document.getElementById("username")?.value.trim(),
            userUuid: this.applicantSel.value,
            leaveType: document.getElementById("leaveType")?.value,
            startDt: startDateTime,
            endDt: endDateTime,
            reason: document.getElementById("reason")?.value
        };
    }

    async handleSubmit(e) {
        e.preventDefault();

        const data = this.collectFormData();
        if (!this.validateFormData(data)) return;

        try {
            const res = await apiFetch(this.apiPrefix, {
                method: "POST",
                headers: {"Content-Type": "application/json"},
                body: JSON.stringify(data)
            }, "/leaves/apply");

            if (!res.ok) {
                let userMsg = "연차 신청 실패";
                try {
                    // 우선 JSON 시도 (문제상황: {"code":"M365_SETUP_REQUIRED","message":"..."} 형태)
                    const err = await res.json();
                    if (err?.code === "M365_SETUP_REQUIRED") {
                        userMsg = "Microsoft 365 연동이 아직 완료되지 않았습니다. 관리자에게 문의하세요.";
                    } else if (typeof err?.message === "string" && err.message.trim()) {
                        userMsg = err.message;
                    }
                } catch {
                    userMsg = await readErrorMessage(res);
                }
                throw new Error(userMsg);
            }

            alert("연차 신청 완료!");
            document.getElementById("form-container").reset();

            navigateTo("/leaves");
        } catch (err) {
            // 401/403은 apiFetch에서 이미 처리/리다이렉트됨
            if (err?.message === "UNAUTHORIZED" || err?.message === "FORBIDDEN") return;
            console.error(err);
            alert("신청 중 오류: " + (err.message || err));
        }
    }

    getAllowedLeaveTypes() {
        // select 안의 option 중 value가 있는 것만 모아 Set으로
        const sel = document.getElementById("leaveType");
        if (!sel) return new Set();
        return new Set(
            Array.from(sel.querySelectorAll("option"))
                .map(o => (o.value || "").trim())
                .filter(v => v.length > 0)
        );
    }

    validateFormData(data) {
        const startDate = document.getElementById("startDate")?.value || "";
        const endDate = document.getElementById("endDate")?.value || "";
        const startHHmm = document.getElementById("startTime")?.value || "";
        const endHHmm = document.getElementById("endTime")?.value || "";

        // 1) 필수값
        const required = [
            ["userUuid", data.userUuid],
            ["leaveType", data.leaveType],
            ["startDate", startDate],
            ["startTime", startHHmm],
            ["endDate", endDate],
            ["endTime", endHHmm],
        ];
        for (const [key, val] of required) {
            if (!val || (typeof val === "string" && val.trim() === "")) {
                alert(`${this.getFieldDisplayName(key)} 입력이 필요합니다.`);
                return false;
            }
        }
        // 2) 날짜 형식 및 순서 (날짜 우선)
        const dateRe = /^\d{4}-\d{2}-\d{2}$/;
        if (!dateRe.test(startDate) || !dateRe.test(endDate)) {
            alert("날짜 형식을 확인해주세요. (예: 2025-08-21)");
            return false;
        }
        // 단순 문자열 비교로도 YYYY-MM-DD는 크기 비교 가능
        if (endDate < startDate) {
            alert("종료 날짜가 시작 날짜보다 앞설 수 없습니다.");
            return false;
        }

        // 2) 연차 유형 유효값 (서버가 내려준 option 기준)
        const allowedTypes = this.getAllowedLeaveTypes();
        if (!allowedTypes.has(data.leaveType)) {
            alert("연차 유형 값이 올바르지 않습니다.");
            return false;
        }
        // 3) 시간/날짜 검증 공통 사용
        const errMsg = validateDateTimes(startDate, startHHmm, endDate, endHHmm);
        if (errMsg) {
            alert(errMsg);
            return false;
        }
        return true;

    }

    getFieldDisplayName(field) {
        const map = {
            userUuid: "신청자",
            leaveType: "연차 유형",
            startDate: "시작 날짜",
            startTime: "시작 시간",
            endDate: "종료 날짜",
            endTime: "종료 시간",
            reason: "사유",
        };
        return map[field] || field;
    }

    onCancelBtnClick() {
        navigateTo("/leaves");

    }

}

/** 생성 모드 진입점 (home.js에서 호출) */
export function initLeaveCreate() {
    const mgr = new CreateLeaveFormManager();
    return mgr.init();
}


