console.log("UserPopup:", window.UserPopup);

window.UserPopup = {
    toggleForm: async function () {
        const container = document.getElementById("userFormContainer");
        if (!container) return;

        if (container.children.length === 0) {
            try {
                const res = await fetch("/admin/users/create");
                if (res.ok) {
                    const html = await res.text();
                    container.innerHTML = html;

                    this.bindEvents();
                    await this.loadTeams();
                }
            } catch (e) {
                console.error("사용자 등록 폼 로드 실패:", e);
            }
        }

        document.getElementById("userFormOverlay")?.classList.add("show");
    },

    bindEvents() {
        document.getElementById("cancelFormBtn")?.addEventListener("click", this.hideForm);
        document.getElementById("closePopupBtn")?.addEventListener("click", this.hideForm);

        const overlay = document.getElementById("userFormOverlay");
        if (overlay) {
            overlay.addEventListener("click", (e) => {
                if (e.target === overlay) this.hideForm();
            });
        }

        const form = document.getElementById("createUserForm");
        if (form) {
            form.addEventListener("submit", (e) => this.handleSubmit(e));
        }
    },

    hideForm() {
        document.getElementById("userFormOverlay")?.classList.remove("show");
    },

    async handleSubmit(e) {
        e.preventDefault();

        const formData = new FormData(e.target);
        const userData = Object.fromEntries(formData.entries());

        try {
            const res = await fetch("/api/admin/users", {
                method: "POST",
                headers: {"Content-Type": "application/json"},
                body: JSON.stringify(userData),
            });

            if (!res.ok) {
                const err = await res.json();
                throw new Error(err.message || "사용자 등록 실패");
            }

            alert("사용자가 성공적으로 등록되었습니다.");
            e.target.reset();
            this.hideForm();

            // 목록 새로고침
            if (typeof window.initUserListManager === "function") {
                window.initUserListManager();
            }
        } catch (err) {
            console.error("사용자 등록 실패:", err);
            alert(err.message);
        }
    },

    async loadTeams() {
        try {
            const res = await fetch("/api/team/teams");
            if (!res.ok) throw new Error("팀 목록 요청 실패");

            const teams = await res.json();
            const select = document.getElementById("teamId");

            if (select) {
                select.innerHTML = `<option value="">팀을 선택하세요</option>` +
                    teams.map(team => `<option value="${team.id}">${team.name}</option>`).join("");
            }
        } catch (e) {
            console.error("팀 목록 로드 실패:", e);
        }
    }
};
