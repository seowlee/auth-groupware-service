class CreateUserForm {
    constructor() {
        this.init();
    }

    async init() {
        const form = document.getElementById("form-container");
        if (!form) {
            console.warn("form-container 이 존재하지 않음");
            return;
        }

        form.addEventListener("submit", this.handleSubmit.bind(this));

        // 취소 버튼 이벤트
        document.getElementById("cancelFormBtn")?.addEventListener("click", () => {
            window.location.href = "/home";
        });

        // 팀 목록 로딩
        await this.loadTeams();
    }

    async handleSubmit(e) {
        e.preventDefault();

        const data = this.collectFormData();
        if (!this.validateFormData(data)) return;

        try {
            const res = await fetch("/api/admin/users", {
                method: "POST",
                headers: {
                    "Content-Type": "application/json",
                    Accept: "application/json",
                },
                body: JSON.stringify(data),
            });

            if (!res.ok) {
                const text = await res.text();
                throw new Error(text || "등록 실패");
            }

            alert("사용자 등록 완료!");
            document.getElementById("form-container").reset();

            // 등록 후 사용자 목록 페이지로 이동
            const userListLink = document.querySelector('a[data-url="/admin/users"]');
            if (userListLink) {
                userListLink.click(); // SPA 내비게이션
            } else {
                window.location.href = "/home";
            }
        } catch (err) {
            console.error("사용자 등록 실패:", err);
            alert("오류: " + err.message);
        }
    }

    collectFormData() {
        return {
            username: document.getElementById("username")?.value.trim(),
            rawPassword: document.getElementById("rawPassword")?.value,
            email: document.getElementById("email")?.value.trim(),
            phoneNumber: document.getElementById('detailPhoneNumber')?.value,
            firstName: document.getElementById("firstName")?.value.trim(),
            lastName: document.getElementById("lastName")?.value.trim(),
            joinedDate: document.getElementById("joinedDate")?.value,
            role: document.getElementById("role")?.value,
            teamId: document.getElementById("teamId")?.value,
        };
    }

    validateFormData(data) {
        const requiredFields = [
            "username",
            "rawPassword",
            "email",
            "phoneNumber",
            "firstName",
            "lastName",
            "joinedDate",
            "role",
            "teamId",
        ];

        for (const field of requiredFields) {
            if (!data[field]) {
                alert(` ${this.getFieldDisplayName(field)} 입력이 필요합니다.`);
                return false;
            }
        }

        // 이메일 형식 검사
        const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
        if (!emailRegex.test(data.email)) {
            alert(" 유효한 이메일 주소를 입력해주세요.");
            return false;
        }

        return true;
    }

    getFieldDisplayName(field) {
        const map = {
            username: "사용자명",
            rawPassword: "비밀번호",
            email: "이메일",
            firstName: "이름",
            lastName: "성",
            joinedDate: "입사일",
            role: "역할",
            teamId: "소속 팀",
        };
        return map[field] || field;
    }

    async loadTeams() {
        try {
            const res = await fetch("/api/teams");
            if (!res.ok) throw new Error("팀 목록 로드 실패");

            const teams = await res.json();
            const select = document.getElementById("teamId");

            if (select) {
                select.innerHTML =
                    `<option value="">팀을 선택하세요</option>` +
                    teams.map((t) => `<option value="${t.id}">${t.name}</option>`).join("");
            }
        } catch (e) {
            console.error("팀 목록 로드 실패", e);
            alert("팀 목록을 불러오는 데 실패했습니다.");
        }
    }
}

export async function initCreateUserForm() {
    new CreateUserForm();
}

