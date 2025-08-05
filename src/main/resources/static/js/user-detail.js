class UserDetailManager {
    constructor(userId) {
        this.userId = userId;
        this.userData = null;
        this.editing = false;
    }

    // 초기화: 사용자 로드 및 이벤트 바인딩
    async init() {
        console.log("사용자 상세 정보 초기화");

        await this.loadUser();
        await this.loadTeams();
        this.bindEvents();
    }

    // 사용자 API 호출 후 렌더링
    async loadUser() {
        try {
            const res = await fetch(`/api/team/users/${this.userId}`);
            if (!res.ok) throw new Error("사용자 조회 실패");

            this.userData = await res.json();
            this.render(this.userData);
        } catch (e) {
            console.error(e);
            alert("사용자 정보를 불러오는 데 실패했습니다.");
        }
    }

    // 사용자 정보 화면에 반영
    render(user) {
        document.getElementById("detailUsername").textContent = user.username;
        document.getElementById("detailEmail").textContent = user.email;
        document.getElementById("detailFirstName").value = user.firstName;
        document.getElementById("detailLastName").value = user.lastName;
        document.getElementById("detailJoinedDate").textContent = user.joinedDate;
        document.getElementById("detailTeam").value = user.teamId;
        // document.getElementById("detailRole").value = user.role;

        const roleSelect = document.getElementById("detailRole");
        roleSelect.innerHTML = `
            <option value="TEAM_MEMBER">팀원</option>
            <option value="TEAM_LEADER">팀장</option>
            <option value="SUPER_ADMIN">최고관리자</option>
        `;
        roleSelect.value = user.role;
        document.getElementById("detailStatus").value = user.status;
        // 연차 타입별 보유 연차 표시
        const leaveList = user.leaveBalances || [];
        const leaveListEl = document.getElementById("leaveStats");
        leaveListEl.innerHTML = leaveList.map(leave =>
            `<li>${leave.typeName} : ${leave.remainingDays}일</li>`
        ).join("");
    }

    async loadTeams() {
        try {
            const response = await fetch("/api/teams", {
                method: "GET",
                headers: {
                    "Content-Type": "application/json",
                },
            });

            if (response.ok) {
                const teams = await response.json();
                this.renderTeamOptions(teams);
            } else {
                console.error("팀 목록 로딩 실패:", response.status);
            }
        } catch (error) {
            console.error("팀 목록 로딩 중 에러:", error);
        }
    }

    renderTeamOptions(teams) {
        const teamSelect = document.getElementById("detailTeam");
        teamSelect.innerHTML = ""; // 기존 옵션 제거

        teams.forEach(team => {
            const option = document.createElement("option");
            option.value = team.id;
            option.textContent = team.name;
            teamSelect.appendChild(option);
        });

        // 현재 사용자 팀 선택
        if (this.userData?.teamId) {
            teamSelect.value = this.userData.teamId;
        }
    }

    toggleEditMode(enable) {
        const fields = ["detailFirstName", "detailLastName", "detailRole", "detailStatus", "detailTeam"];
        fields.forEach(id => {
            document.getElementById(id).disabled = !enable;
        });
    }

    // 이벤트 바인딩 (저장/삭제)
    bindEvents() {
        // 수정
        const editBtn = document.getElementById("toggleEditBtn");
        if (editBtn) {
            editBtn.addEventListener("click", async () => {
                if (!this.editing) {
                    this.editing = true;
                    this.toggleEditMode(true);
                    document.getElementById("toggleEditBtn").textContent = "저장";
                } else {
                    const updated = {
                        role: document.getElementById("detailRole").value,
                        teamId: document.getElementById("detailTeam").value,
                        status: document.getElementById("detailStatus").value,
                        firstName: document.getElementById("detailFirstName").value,
                        lastName: document.getElementById("detailLastName").value,
                    };

                    try {
                        const res = await fetch(`/api/admin/users/${this.userId}/update`, {
                            method: "POST",
                            headers: {"Content-Type": "application/json"},
                            body: JSON.stringify(updated),
                        });
                        if (!res.ok) throw new Error("수정 실패");
                        alert("사용자 정보가 저장되었습니다.");
                        this.toggleEditMode(false);
                        document.getElementById("toggleEditBtn").textContent = "수정";
                        this.editing = false;
                        const path = "/team/users";
                        history.pushState({path}, "", path);
                        loadPageIntoMainContent(path);
                    } catch (err) {
                        console.error(err);
                        alert("수정 중 오류가 발생했습니다.");
                    }
                }
            });
        }

        // 사용자 삭제
        const deleteBtn = document.getElementById("deleteUserBtn");
        if (deleteBtn) {
            deleteBtn.addEventListener("click", async () => {
                if (!confirm("정말 삭제하시겠습니까?")) return;

                try {
                    const res = await fetch(`/api/team/users/${this.userId}`, {
                        method: "DELETE",
                    });
                    if (!res.ok) throw new Error("삭제 실패");

                    alert("사용자가 삭제되었습니다.");
                    const path = "/team/users";
                    history.pushState({path}, "", path);
                    loadPageIntoMainContent(path);
                } catch (err) {
                    console.error(err);
                    alert("삭제 중 오류가 발생했습니다.");
                }
            });
        }
        const backBtn = document.getElementById("backBtn");
        if (backBtn) {
            backBtn.addEventListener("click", () => {
                const path = "/team/users";
                history.pushState({path}, "", path);
                loadPageIntoMainContent(path);
            });
        }
    }
}

// URL에서 사용자 ID 추출 (예: /admin/users/abc-123)
function getUserIdFromUrl() {
    const path = window.location.pathname;
    const segments = path.split("/");
    return segments[segments.length - 1] || null;
}

// HTML 로딩 후 실행
window.initUserDetail = async function () {
    const userId = window.location.pathname.split("/").pop();
    if (!userId) {
        alert("사용자 ID를 찾을 수 없습니다.");
        return;
    }

    const manager = new UserDetailManager(userId);
    await manager.init();
};
