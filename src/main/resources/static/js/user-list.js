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
        this.sortField = "joinedDate";
        this.sortDirection = "desc";


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
                onPageChange: async (page, size) => {
                    await this.loadUsers(page, size);
                },
                onPageSizeChange: async (page, size) => {
                    await this.loadUsers(page, size);
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
            toggleUserFormBtn.addEventListener("click", () => window.UserPopup.toggleForm());

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
        // 정렬 이벤트 추가
        this.bindSortEvents();
    }

    bindSortEvents() {
        document.querySelectorAll("th[data-sort]").forEach(th => {
            th.addEventListener("click", () => {
                const field = th.dataset.sort;
                if (this.sortField === field) {
                    this.sortDirection = this.sortDirection === "asc" ? "desc" : "asc";
                } else {
                    this.sortField = field;
                    this.sortDirection = "asc";
                }
                this.loadUsers(0, this.paginationManager?.getPageSize() || 5);
            });
        });
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
            if (this.sortField) {
                params.append("sort", `${this.sortField},${this.sortDirection}`);
            }
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

            this.updateSortIndicator();

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
            <tr data-user-id="${user.uuid}">
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

        this.bindUserRowEvents();
    }

    bindUserRowEvents() {
        document.querySelectorAll("#userTableBody tr").forEach(row => {
            row.addEventListener("click", async () => {
                const userId = row.dataset.userId;
                try {
                    const path = `/team/users/${userId}`;
                    history.pushState({path}, "", path);
                    loadPageIntoMainContent(path);

                } catch (err) {
                    console.error("사용자 상세 페이지 로딩 에러:", err);
                    alert("사용자 정보를 불러오는데 실패했습니다.");
                }
            });
        });
    }

    handleSearch() {
        this.currentFilters = {
            keyword: document.getElementById("searchKeyword")?.value || "",
            teamId: document.getElementById("filterTeamId")?.value || "",
            role: document.getElementById("filterRole")?.value || "",
            status: document.getElementById("filterStatus")?.value || "",
        };

        this.loadUsers(0, this.paginationManager?.getPageSize() || 5);
    }

    resetFilters() {
        ["searchKeyword", "filterTeamId", "filterRole", "filterStatus"].forEach(id => {
            const el = document.getElementById(id);
            if (el) el.value = "";
        });

        this.currentFilters = {keyword: "", teamId: "", role: "", status: ""};
        this.loadUsers(0, this.paginationManager?.getPageSize() || 5);
    }

    updateSortIndicator() {
        document.querySelectorAll("th[data-sort]").forEach(th => {
            th.classList.remove("asc", "desc"); // 기존 정렬 표시 제거
            if (th.dataset.sort === this.sortField) {
                th.classList.add(this.sortDirection); // 현재 정렬 필드에 asc 또는 desc 클래스 추가
            }
        });
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
