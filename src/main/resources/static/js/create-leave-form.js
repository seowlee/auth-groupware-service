import {navigateTo} from "./router.js";

class CreateLeaveFormManager {
    constructor() {
        this.apiPrefix = "/api/leaves";
        this.form = null;
        this.applicantSel = null;
        this.isSuperAdmin = false;

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
        this.bindEvents();
    }

    /**
     * 신청자 목록 로드 (SUPER_ADMIN만 전체 유저 로드, 일반사용자는 본인으로 고정)
     */
    async loadApplicants(defaultUuid = "") {
        try {
            // SUPER_ADMIN: 전체 또는 검색 결과 로드
            const res = await fetch('/api/admin/users/applicants');
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
            const res = await fetch(this.apiPrefix, {
                method: "POST",
                headers: {"Content-Type": "application/json"},
                body: JSON.stringify(data)
            });

            if (!res.ok) {
                throw new Error(await res.text() || "연차 신청 실패");
            }

            alert("연차 신청 완료!");
            document.getElementById("form-container").reset();

            navigateTo("/leaves");
        } catch (err) {
            console.error(err);
            alert("신청 중 오류: " + (err.message || err));
        }
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

        // 3) 연차 유형 유효값
        const allowedTypes = new Set(["ANNUAL", "BIRTHDAY", "SICK", "CUSTOM"]);
        if (!allowedTypes.has(data.leaveType)) {
            alert("연차 유형 값이 올바르지 않습니다.");
            return false;
        }

        // 4) 시간 슬롯 제약
        const allowedStart = new Set(["09:00", "13:00"]);
        const allowedEnd = new Set(["13:00", "17:00"]);
        if (!allowedStart.has(startHHmm) || !allowedEnd.has(endHHmm)) {
            alert("시작/종료 시간은 09:00/13:00/17:00 중에서 선택해주세요.");
            return false;
        }

        // 5) 같은 날짜인 경우 허용 가능한 조합만 통과
        if (startDate === endDate) {
            const okSameDay =
                (startHHmm === "09:00" && (endHHmm === "13:00" || endHHmm === "17:00")) ||
                (startHHmm === "13:00" && endHHmm === "17:00");
            if (!okSameDay) {
                alert("같은 날짜에서는 (09→13), (09→17), (13→17) 조합만 가능합니다.");
                return false;
            }
        } else {
            // 6) 다른 날짜 구간이면 최종적으로 시작<종료 보장 (실제 Date 비교)
            const s = new Date(`${startDate}T${startHHmm}:00`);
            const e = new Date(`${endDate}T${endHHmm}:00`);
            if (Number.isNaN(s.getTime()) || Number.isNaN(e.getTime())) {
                alert("시작/종료 일시 형식을 확인해주세요.");
                return false;
            }
            if (s >= e) {
                // 날짜가 다르더라도 이상 케이스 방지
                alert("시작 일시는 종료 일시보다 빨라야 합니다.");
                return false;
            }
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

/** URL에서 /leaves/{id}의 {id} 추출 */
function getLeaveIdFromUrl() {
    const segs = window.location.pathname.split("/");
    return segs[segs.length - 1] || null;
}

/** 생성 모드 진입점 (home.js에서 호출) */
export function initLeaveCreate() {
    const mgr = new CreateLeaveFormManager();
    return mgr.init();
}


