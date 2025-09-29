import {navigateTo} from "./core/router.js";
import {apiFetch, readErrorMessage} from "./list-form-common.js";

class LeaveFormManager {
    constructor(id) {
        this.leaveId = id;                      // 'create' | 'edit'
        this.data = null;
        this.editing = false;
        this.apiPrefix = "/api/leaves";
        this.isSuperAdmin = false;
        this.isOwner = false;
        this.canEditCancel = false;
    }

    async init() {
        this.cacheEls();
        await this.loadLeave();

        const leaveOwnerUuid = this.data.userUuid;
        const currentUuid = this.$container.dataset.currentUuid;
        this.isOwner = currentUuid && (currentUuid === leaveOwnerUuid)
        this.isSuperAdmin = (this.$container.dataset.isSuperAdmin || '').toLowerCase() === "true";
        this.canEditCancel = this.isSuperAdmin || this.isOwner
        this.toggleActionButtons(this.canEditCancel);

        this.bindEvents();
    }

    cacheEls() {
        this.$container = document.getElementById("leaveFormContainer");
        this.$userName = document.getElementById("username");
        this.$leaveType = document.getElementById("leaveType");
        this.$startDate = document.getElementById("startDate");
        this.$startTime = document.getElementById("startTime");
        this.$endDate = document.getElementById("endDate");
        this.$endTime = document.getElementById("endTime");
        this.$reason = document.getElementById("reason");

        this.$editBtn = document.getElementById("toggleEditBtn");
        this.$deleteBtn = document.getElementById("deleteBtn");
        this.$backBtn = document.getElementById("backBtn");
    }

    toggleActionButtons(show) {
        const display = show ? '' : 'none';
        if (this.$editBtn) this.$editBtn.style.display = display;
        if (this.$deleteBtn) this.$deleteBtn.style.display = display;
    }

    /**
     * 수정/삭제/뒤로 버튼 이벤트 바인딩
     */
    bindEvents() {
        if (this.$editBtn) this.$editBtn.addEventListener('click', this.onEditBtnClick.bind(this));
        if (this.$deleteBtn) this.$deleteBtn.addEventListener('click', this.onCancelBtnClick.bind(this));
        if (this.$backBtn) this.$backBtn.addEventListener('click', this.onBackBtnClick.bind(this));
    }


    /**
     * 연차 정보 조회 및 렌더링
     */
    async loadLeave() {
        const res = await fetch(`${this.apiPrefix}/${this.leaveId}`);
        if (!res.ok) {
            alert("연차 상세 조회 실패");
            return;
        }
        this.data = await res.json();
        this.render(this.data);
    }

    /**
     * 화면에 연차 정보 반영
     */
    render(data) {
        this.$userName.value = data.userName ?? "";
        this.$leaveType.value = data.leaveType ?? "ANNUAL";

        // "yyyy-MM-ddTHH:mm:ss" → 날짜/시간 분리
        const [sd, st] = (data.startDt ?? "").split("T");
        const [ed, et] = (data.endDt ?? "").split("T");
        this.$startDate.value = sd ?? "";
        this.$startTime.value = st ? st.slice(0, 5) : "09:00";
        this.$endDate.value = ed ?? "";
        this.$endTime.value = et ? et.slice(0, 5) : "17:00";

        this.$reason.value = data.reason ?? "";
    }


    async onEditBtnClick() {
        if (!this.canEditCancel) return;

        if (!this.editing) {
            this.editing = true;
            this.toggleEditMode(true);
            this.$editBtn.textContent = '저장';
            return;
        }
        await this.handleUpdate();
    }

    /**
     * 삭제
     */
    async onCancelBtnClick() {
        if (!this.canEditCancel) return;

        if (!confirm("정말로 이 연차 신청을 취소하시겠습니까?")) return;

        try {
            const res = await fetch(`${this.apiPrefix}/${this.leaveId}/cancel`, {method: "POST"});
            if (!res.ok) throw new Error(await res.text() || "연차 취소 실패");
            alert("연차가 취소되었습니다.");
            navigateTo("/leaves");
        } catch (err) {
            console.error(err);
            alert("취소 중 오류: " + (err.message || err));
        }
    }

    onBackBtnClick() {
        navigateTo("/leaves");
    }

    /**
     * [수정/저장]
     */
    toggleEditMode(enable) {
        ["leaveType", "startDate", "startTime", "endDate", "endTime", "reason"]
            .forEach(id => document.getElementById(id).disabled = !enable);
    }

    collectPayload() {
        const startDateTime = `${this.$startDate.value}T${this.$startTime.value}`;
        const endDateTime = `${this.$endDate.value}T${this.$endTime.value}`;
        return {
            leaveType: this.$leaveType.value,
            startDt: startDateTime,
            endDt: endDateTime,
            reason: this.$reason.value
        };
    }

    async handleUpdate() {
        if (!this.leaveId) {
            alert("연차 ID가 없습니다.");
            return;
        }
        const data = this.collectPayload();
        try {
            const res = await apiFetch(`${this.apiPrefix}/${this.leaveId}`, {
                method: "POST",
                body: JSON.stringify(data)
            }, `/leaves/${this.leaveId}`);
            if (!res.ok) throw new Error(await readErrorMessage(res) || "연차 수정 실패");
            alert("연차가 수정되었습니다.");
            this.editing = false;
            this.toggleEditMode(false);
            this.$editBtn.textContent = '수정';

            navigateTo("/leaves");
        } catch (err) {
            console.error(err);
            alert("수정 중 오류: " + (err.message || err));
        }
    }
}

/** URL에서 /leaves/{id}의 {id} 추출 */
function getLeaveIdFromUrl() {
    const segs = window.location.pathname.split("/");
    return segs[segs.length - 1] || null;
}


/** 수정 모드 진입점 (home.js에서 호출) */
export function initLeaveEdit() {
    const id = getLeaveIdFromUrl();
    if (!id) {
        alert("연차 ID가 유효하지 않습니다.");
        return;
    }

    const mgr = new LeaveFormManager(id);
    return mgr.init();
}
