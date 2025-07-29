/**
 * 사용자 목록 관리자
 * 검색 필터, 페이징, 사용자 등록 기능을 포함
 */
class UserListManager {
    constructor() {
        this.currentFilters = {
            keyword: "",
            teamId: "",
            role: "",
            status: "",
        };
        this.paginationManager = null;
        this.isLoading = false;

        // this.init();
    }

    async init() {
        console.log("UserListManager 초기화 시작");

        // 페이징 매니저 초기화
        this.initPagination();

        // 이벤트 바인딩
        this.bindEvents();

        // 초기 데이터 로드
        await this.loadUsers();

        // 팀 목록 로드
        await this.loadTeams();

        console.log("UserListManager 초기화 완료");
    }

    initPagination() {
        // 공통 페이징 컴포넌트 사용
        if (typeof window.PaginationManager !== "undefined") {
            this.paginationManager = new PaginationManager("paginationContainer", {
                onPageChange: (page, size) => {
                    this.loadUsers(page, size);
                },
                onPageSizeChange: (page, size) => {
                    this.loadUsers(page, size);
                },
            });
        } else {
            console.error("PaginationManager가 로드되지 않았습니다.");
        }
    }

    bindEvents() {
        // 검색 버튼
        const searchBtn = document.getElementById("searchBtn");
        if (searchBtn) {
            searchBtn.addEventListener("click", () => this.handleSearch());
        }

        // 초기화 버튼
        const resetFilterBtn = document.getElementById("resetFilterBtn");
        if (resetFilterBtn) {
            resetFilterBtn.addEventListener("click", () => this.resetFilters());
        }

        // 새로고침 버튼
        const refreshUsersBtn = document.getElementById("refreshUsersBtn");
        if (refreshUsersBtn) {
            refreshUsersBtn.addEventListener("click", () => this.loadUsers());
        }

        // 사용자 등록 폼 토글
        const toggleUserFormBtn = document.getElementById("toggleUserFormBtn");
        if (toggleUserFormBtn) {
            toggleUserFormBtn.addEventListener("click", () => this.toggleUserForm());
        }

        // 검색 필터 엔터키 이벤트
        const searchKeyword = document.getElementById("searchKeyword");
        if (searchKeyword) {
            searchKeyword.addEventListener("keypress", (e) => {
                if (e.key === "Enter") {
                    this.handleSearch();
                }
            });
        }
    }

    async loadUsers(page = 0, size = 5) {
        if (this.isLoading) return;

        this.isLoading = true;
        this.showLoading();

        try {
            const params = new URLSearchParams({
                page: page,
                size: size,
                ...this.currentFilters,
            });

            const response = await fetch(`/api/team/users?${params.toString()}`, {
                method: "GET",
                headers: {
                    "Content-Type": "application/json",
                },
            });

            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }

            const data = await response.json();
            this.renderUsers(data.content);

            if (this.paginationManager) {
                this.paginationManager.updatePagination(data);
                this.paginationManager.show();
            }
        } catch (error) {
            console.error("사용자 목록 로드 실패:", error);
            this.showError("사용자 목록을 불러오는데 실패했습니다.");
        } finally {
            this.isLoading = false;
            this.hideLoading();
        }
    }

    renderUsers(users) {
        const tbody = document.getElementById("userTableBody");
        const table = document.getElementById("userTable");

        if (!tbody || !table) return;

        if (users.length === 0) {
            tbody.innerHTML = `
                <tr>
                    <td colspan="6" class="empty-state">
                        <h3>사용자가 없습니다</h3>
                        <p>검색 조건을 변경하거나 새로운 사용자를 등록해보세요.</p>
                    </td>
                </tr>
            `;
            table.style.display = "table";
            return;
        }

        tbody.innerHTML = users
            .map(
                (user) => `
            <tr>
                <td>${this.escapeHtml(user.username)}</td>
                <td>${this.escapeHtml(user.email)}</td>
                <td>${this.getRoleDisplayName(user.role)}</td>
                <td>${this.escapeHtml(user.teamName || "-")}</td>
                <td>${this.formatDate(user.joinedDate)}</td>
                <td>
                    <span class="status-${user.status.toLowerCase()}">
                        ${this.getStatusDisplayName(user.status)}
                    </span>
                </td>
            </tr>
        `
            )
            .join("");

        table.style.display = "table";
    }

    handleSearch() {
        this.currentFilters = {
            keyword: document.getElementById("searchKeyword")?.value || "",
            teamId: document.getElementById("filterTeamId")?.value || "",
            role: document.getElementById("filterRole")?.value || "",
            status: document.getElementById("filterStatus")?.value || "",
        };

        this.loadUsers(
            0,
            this.paginationManager ? this.paginationManager.getPageSize() : 5
        );
    }

    resetFilters() {
        // 필터 초기화
        const searchKeyword = document.getElementById("searchKeyword");
        const filterTeamId = document.getElementById("filterTeamId");
        const filterRole = document.getElementById("filterRole");
        const filterStatus = document.getElementById("filterStatus");

        if (searchKeyword) searchKeyword.value = "";
        if (filterTeamId) filterTeamId.value = "";
        if (filterRole) filterRole.value = "";
        if (filterStatus) filterStatus.value = "";

        this.currentFilters = {
            keyword: "",
            teamId: "",
            role: "",
            status: "",
        };

        this.loadUsers(
            0,
            this.paginationManager ? this.paginationManager.getPageSize() : 5
        );
    }

    async toggleUserForm() {
        const container = document.getElementById("userFormContainer");
        if (!container) return;

        // 팝업이 이미 로드되어 있는지 확인
        if (container.children.length === 0) {
            try {
                // 팝업 HTML 로드
                const response = await fetch("/admin/users/create");
                if (response.ok) {
                    const html = await response.text();
                    container.innerHTML = html;

                    // 이벤트 바인딩
                    this.bindPopupEvents();
                    await this.loadTeams();
                }
            } catch (error) {
                console.error("팝업 로드 실패:", error);
                return;
            }
        }

        // 팝업 표시
        const overlay = document.getElementById("userFormOverlay");
        if (overlay) {
            overlay.classList.add("show");
            // ESC 키로 닫기
            document.addEventListener("keydown", (e) => {
                if (e.key === "Escape") {
                    this.hideUserForm();
                }
            });
        }
    }

    bindPopupEvents() {
        // 팝업 닫기 버튼들
        const cancelFormBtn = document.getElementById("cancelFormBtn");
        const closePopupBtn = document.getElementById("closePopupBtn");
        if (cancelFormBtn) {
            cancelFormBtn.addEventListener("click", () => this.hideUserForm());
        }
        if (closePopupBtn) {
            closePopupBtn.addEventListener("click", () => this.hideUserForm());
        }

        // 팝업 외부 클릭 시 닫기
        const overlay = document.getElementById("userFormOverlay");
        if (overlay) {
            overlay.addEventListener("click", (e) => {
                if (e.target === overlay) {
                    this.hideUserForm();
                }
            });
        }

        // 사용자 등록 폼 제출
        const createUserForm = document.getElementById("createUserForm");
        if (createUserForm) {
            createUserForm.addEventListener("submit", (e) =>
                this.handleCreateUser(e)
            );
        }
    }

    hideUserForm() {
        const overlay = document.getElementById("userFormOverlay");
        if (overlay) {
            overlay.classList.remove("show");
        }
    }

    async handleCreateUser(e) {
        e.preventDefault();

        const formData = new FormData(e.target);
        const userData = {
            username: formData.get("username"),
            rawPassword: formData.get("rawPassword"),
            email: formData.get("email"),
            firstName: formData.get("firstName"),
            lastName: formData.get("lastName"),
            joinedDate: formData.get("joinedDate"),
            role: formData.get("role"),
            teamId: formData.get("teamId"),
        };

        try {
            const response = await fetch("/api/admin/users", {
                method: "POST",
                headers: {
                    "Content-Type": "application/json",
                },
                body: JSON.stringify(userData),
            });

            if (!response.ok) {
                const errorData = await response.json();
                throw new Error(errorData.message || "사용자 등록에 실패했습니다.");
            }

            this.showSuccess("사용자가 성공적으로 등록되었습니다.");
            e.target.reset();
            this.hideUserForm();
            await this.loadUsers(); // 목록 새로고침
        } catch (error) {
            console.error("사용자 등록 실패:", error);
            this.showError(error.message);
        }
    }

    async loadTeams() {
        try {
            const response = await fetch("/api/team/teams", {
                method: "GET",
                headers: {
                    "Content-Type": "application/json",
                },
            });

            if (response.ok) {
                const teams = await response.json();
                this.renderTeams(teams);
            }
        } catch (error) {
            console.error("팀 목록 로드 실패:", error);
        }
    }

    renderTeams(teams) {
        // 필터용 팀 선택
        const filterTeamSelect = document.getElementById("filterTeamId");
        if (filterTeamSelect) {
            // 기존 옵션 유지 (전체 팀)
            const allTeamsOption = filterTeamSelect.querySelector('option[value=""]');
            filterTeamSelect.innerHTML = "";
            if (allTeamsOption) {
                filterTeamSelect.appendChild(allTeamsOption);
            }

            // 팀 옵션 추가
            teams.forEach((team) => {
                const option = document.createElement("option");
                option.value = team.id;
                option.textContent = team.name;
                filterTeamSelect.appendChild(option);
            });
        }

        // 팝업용 팀 선택
        const popupTeamSelect = document.getElementById("teamId");
        if (popupTeamSelect) {
            // 기존 옵션 유지 (팀을 선택하세요)
            const defaultOption = popupTeamSelect.querySelector('option[value=""]');
            popupTeamSelect.innerHTML = "";
            if (defaultOption) {
                popupTeamSelect.appendChild(defaultOption);
            }

            // 팀 옵션 추가
            teams.forEach((team) => {
                const option = document.createElement("option");
                option.value = team.id;
                option.textContent = team.name;
                popupTeamSelect.appendChild(option);
            });
        }
    }

    showLoading() {
        const loadingDiv = document.querySelector(".loading");
        if (loadingDiv) {
            loadingDiv.style.display = "block";
        }
    }

    hideLoading() {
        const loadingDiv = document.querySelector(".loading");
        if (loadingDiv) {
            loadingDiv.style.display = "none";
        }
    }

    showMessage(message, type = "success") {
        const messageArea = document.getElementById("messageArea");
        if (!messageArea) return;

        messageArea.innerHTML = `<div class="${type}">${message}</div>`;
        messageArea.style.display = "block";

        // 5초 후 자동 제거
        setTimeout(() => {
            messageArea.style.display = "none";
        }, 5000);
    }

    showSuccess(message) {
        this.showMessage(message, "success");
    }

    showError(message) {
        this.showMessage(message, "error");
    }

    // 유틸리티 메서드들
    escapeHtml(text) {
        const div = document.createElement("div");
        div.textContent = text;
        return div.innerHTML;
    }

    getRoleDisplayName(role) {
        const roleMap = {
            TEAM_MEMBER: "팀원",
            TEAM_LEADER: "팀장",
            SUPER_ADMIN: "최고관리자",
        };
        return roleMap[role] || role;
    }

    getStatusDisplayName(status) {
        const statusMap = {
            ACTIVE: "활성",
            INACTIVE: "비활성",
        };
        return statusMap[status] || status;
    }

    formatDate(dateString) {
        if (!dateString) return "-";
        const date = new Date(dateString);
        return date.toLocaleDateString("ko-KR");
    }
}

// 전역 초기화 함수
window.initUserListManager = async function () {
    console.log("initUserListManager 호출됨");
    const manager = new UserListManager();
    await manager.init();
};
