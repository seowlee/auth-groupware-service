import {navigateTo} from "./router.js";

class LeaveFormManager {
    constructor(mode = "create", existingData = null) {
        this.mode = mode;                      // 'create' | 'edit'
        this.data = existingData;
        this.leaveId = existingData?.id || null;
        this.editing = false;

        this.apiPrefix = "/api/leaves";
        this.permissions = {
            isSuperAdmin: false, isOwner: false, get canEditCancel() {
                return this.isSuperAdmin || this.isOwner;
            }
        };
    }

    async init() {
        this.cacheEls();
        this.bindEvents();

        if (this.mode === "edit") {
            await this.prefillFormData();
            // 소유자 여부 계산 (신청자 username == 현재 로그인 사용자)
            this.permissions.isOwner = this.data?.username === this.currentUsername;
            this.renderButtonsForEdit();
            this.toggleEditMode(false); // 보기 상태(잠금)
        } else {
            this.renderButtonsForCreate();
            this.toggleEditMode(true);  // 생성은 즉시 입력 가능
        }
    }

    cacheEls() {
        this.$container = document.getElementById("leaveFormContainer");
        this.isSuperAdmin = this.$container.dataset.isSuperAdmin === "true";
        this.currentUsername = this.$container.dataset.username;

        this.$form = document.getElementById("leaveApplyForm");
        this.$userName = document.getElementById("username");
        this.$leaveType = document.getElementById("leaveType");
        this.$startDate = document.getElementById("startDate");
        this.$startTime = document.getElementById("startTime");
        this.$endDate = document.getElementById("endDate");
        this.$endTime = document.getElementById("endTime");
        this.$reason = document.getElementById("reason");

        this.$submitBtn = document.getElementById("submitBtn");
        this.$updateBtn = document.getElementById("updateBtn");
        this.$cancelBtn = document.getElementById("cancelBtn");
        this.$backBtn = document.getElementById("backBtn");
    }

    bindEvents() {
        if (this.$form) {
            this.$form.addEventListener("submit", async (e) => {
                e.preventDefault();
                if (this.mode === "create") {
                    await this.handleCreate();
                } else if (this.mode === "edit" && this.editing) {
                    await this.handleUpdate();
                }
            });
        }
        if (this.$updateBtn) this.$updateBtn.addEventListener("click", this.onEditBtnClick.bind(this));
        if (this.$cancelBtn) this.$cancelBtn.addEventListener("click", this.onCancelLeaveClick.bind(this));
        if (this.$backBtn) this.$backBtn.addEventListener("click", this.onBackBtnClick.bind(this));
    }

    renderButtonsForCreate() {
        this.$submitBtn?.style.setProperty("display", "inline-block");
        this.$updateBtn?.style.setProperty("display", "none");
        this.$cancelBtn?.style.setProperty("display", "none");
    }

    renderButtonsForEdit() {
        // 생성 버튼 숨김
        this.$submitBtn?.style.setProperty("display", "none");
        if (this.permissions.canEditCancel) {
            this.$updateBtn?.style.setProperty("display", "inline-block");
            this.$updateBtn.textContent = "수정";
            this.$cancelBtn?.style.setProperty("display", "inline-block");
        } else {
            this.$updateBtn?.style.setProperty("display", "none");
            this.$cancelBtn?.style.setProperty("display", "none");
        }
    }

    async prefillFormData() {
        const d = this.data;
        if (!d) return;

        this.$userName.value = d.userName ?? "";
        this.$leaveType.value = d.leaveType ?? "ANNUAL";

        // "yyyy-MM-ddTHH:mm:ss" → 날짜/시간 분리
        const [sd, st] = (d.startTime ?? "").split("T");
        const [ed, et] = (d.endTime ?? "").split("T");
        this.$startDate.value = sd ?? "";
        this.$startTime.value = st ? st.slice(0, 5) : "09:00";
        this.$endDate.value = ed ?? "";
        this.$endTime.value = et ? et.slice(0, 5) : "17:00";

        this.$reason.value = d.reason ?? "";
    }

    toggleEditMode(enable) {
        const ids = ["username", "leaveType", "startDate", "startTime", "endDate", "endTime", "reason"];
        ids.forEach(id => {
            const el = document.getElementById(id);
            if (el) {
                // username은 SUPER_ADMIN만 편집 가능(템플릿에서도 readonly지만 JS에서도 가드)
                if (id === "username" && !this.permissions.isSuperAdmin) {
                    el.disabled = true;
                } else {
                    el.disabled = !enable;
                }
            }
        });
        this.editing = enable;
        if (this.$updateBtn) this.$updateBtn.textContent = enable ? "저장" : "수정";
    }

    async onEditBtnClick() {
        if (!this.permissions.canEditCancel) return;
        if (!this.editing) {
            this.toggleEditMode(true);
            return;
        }
        await this.handleUpdate();
    }

    async onCancelLeaveClick() {
        if (!this.permissions.canEditCancel) return;
        if (!this.leaveId) {
            alert("연차 ID가 없습니다.");
            return;
        }
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

    collectPayload() {
        const startDateTime = `${this.$startDate.value}T${this.$startTime.value}`;
        const endDateTime = `${this.$endDate.value}T${this.$endTime.value}`;
        return {
            username: this.$username.value,
            leaveType: this.$leaveType.value,
            startTime: startDateTime,
            endTime: endDateTime,
            reason: this.$reason.value
        };
    }

    async handleCreate() {
        const data = this.collectPayload();
        try {
            const res = await fetch("/api/leave", {  // 생성 엔드포인트(네 기존 규칙 유지)
                method: "POST",
                headers: {"Content-Type": "application/json"},
                body: JSON.stringify(data)
            });
            if (!res.ok) throw new Error(await res.text() || "연차 신청 실패");
            alert("연차가 신청되었습니다.");
            navigateTo("/leaves");
        } catch (err) {
            console.error(err);
            alert("신청 중 오류: " + (err.message || err));
        }
    }

    async handleUpdate() {
        if (!this.leaveId) {
            alert("연차 ID가 없습니다.");
            return;
        }
        const data = this.collectPayload();
        try {
            const res = await fetch(`${this.apiPrefix}/${this.leaveId}`, {
                method: "PUT",
                headers: {"Content-Type": "application/json"},
                body: JSON.stringify(data)
            });
            if (!res.ok) throw new Error(await res.text() || "연차 수정 실패");
            alert("연차가 저장되었습니다.");
            this.toggleEditMode(false);
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

/** 생성 모드 진입점 (home.js에서 호출) */
export function initLeaveCreate() {
    const mgr = new LeaveFormManager("create");
    mgr.init();
}

/** 수정 모드 진입점 (home.js에서 호출) */
export async function initLeaveEdit() {
    const id = getLeaveIdFromUrl();
    if (!id) {
        alert("연차 ID가 유효하지 않습니다.");
        return;
    }
    const res = await fetch(`/api/leaves/${id}`);
    if (!res.ok) {
        alert("연차 상세 조회 실패");
        return;
    }
    const data = await res.json();
    const mgr = new LeaveFormManager("edit", data);
    mgr.init();
}
