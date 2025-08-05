class LeaveFormManager {
    constructor(mode = "create", existingData = null) {
        this.mode = mode; // "create" or "edit"
        this.leaveId = existingData?.id || null;
        this.existingData = existingData;
    }

    async init() {
        this.bindEvents();
        if (this.mode === "edit") {
            this.prefillForm();
            this.showEditButtons();
        } else {
            this.showCreateButtons();
        }
    }

    bindEvents() {
        document.getElementById("leaveApplyForm").addEventListener("submit", async (e) => {
            e.preventDefault();
            if (this.mode === "create") {
                await this.handleSubmit();
            } else if (this.mode === "edit") {
                await this.handleUpdate();
            }
        });

        document.getElementById("cancelBtn").addEventListener("click", () => {
            if (confirm("수정을 취소하시겠습니까?")) {
                history.pushState({}, "", "/leave/list");
                loadPageIntoMainContent("/leave/list");
            }
        });

        // 필요 시 삭제 버튼도 추가 가능
        // document.getElementById("deleteBtn").addEventListener("click", ...)
    }

    showCreateButtons() {
        document.getElementById("submitBtn").style.display = "inline-block";
        document.getElementById("updateBtn").style.display = "none";
        document.getElementById("cancelBtn").style.display = "none";
    }

    showEditButtons() {
        document.getElementById("submitBtn").style.display = "none";
        document.getElementById("updateBtn").style.display = "inline-block";
        document.getElementById("cancelBtn").style.display = "inline-block";
    }

    prefillForm() {
        document.getElementById("leaveType").value = this.existingData.leaveType;
        document.getElementById("startDate").value = this.existingData.startDate;
        document.getElementById("startTime").value = this.existingData.startTime;
        document.getElementById("endDate").value = this.existingData.endDate;
        document.getElementById("endTime").value = this.existingData.endTime;
        document.getElementById("reason").value = this.existingData.reason;
        document.getElementById("userUuid").value = this.existingData.userUuid;
    }

    collectFormData() {
        const userUuid = document.getElementById("userUuid").value;
        const startDate = document.getElementById("startDate").value;
        const startTime = document.getElementById("startTime").value;
        const endDate = document.getElementById("endDate").value;
        const endTime = document.getElementById("endTime").value;

        const startDateTime = `${startDate}T${startTime}`;
        const endDateTime = `${endDate}T${endTime}`;
        console.log("uuuuuuuid" + userUuid);
        return {
            userUuid: userUuid,
            leaveType: document.getElementById("leaveType").value,
            startTime: startDateTime,
            endTime: endDateTime,
            reason: document.getElementById("reason").value
        };
    }

    async handleSubmit() {
        const data = this.collectFormData();
        try {
            const res = await fetch("/api/leave", {
                method: "POST",
                headers: {"Content-Type": "application/json"},
                body: JSON.stringify(data)
            });
            if (!res.ok) throw new Error("연차 신청 실패");
            alert("신청 완료");
            history.pushState({}, "", "/leave/list");
            loadPageIntoMainContent("/leave/list");
        } catch (err) {
            console.error(err);
            alert("신청 중 오류 발생");
        }
    }

    async handleUpdate() {
        const data = this.collectFormData();
        try {
            const res = await fetch(`/api/leave/${this.leaveId}`, {
                method: "PUT",
                headers: {"Content-Type": "application/json"},
                body: JSON.stringify(data)
            });
            if (!res.ok) throw new Error("수정 실패");
            alert("수정 완료");
            history.pushState({}, "", "/leave/list");
            loadPageIntoMainContent("/leave/list");
        } catch (err) {
            console.error(err);
            alert("수정 중 오류 발생");
        }
    }
}

// 외부에서 초기화
window.initLeaveForm = function (mode = "create", data = null) {
    const manager = new LeaveFormManager(mode, data);
    manager.init();
};
