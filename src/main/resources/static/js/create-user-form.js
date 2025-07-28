/**
 * 사용자 생성 폼 초기화 함수
 * SPA 환경에서 동적으로 로드된 폼에 이벤트 리스너를 등록
 */
export function initCreateUserForm() {
    console.log("📢 사용자 생성 폼 초기화 시작");

    const form = document.getElementById('createUserForm');
    if (!form) {
        console.warn("❗ createUserForm을 찾을 수 없습니다");
        return;
    }

    // 기존 submit 이벤트 리스너 제거 (중복 방지)
    const existingHandler = form.onsubmit;
    if (existingHandler) {
        form.removeEventListener('submit', existingHandler);
    }

    // 새로운 submit 이벤트 리스너 등록
    form.addEventListener("submit", handleFormSubmit);

    console.log("✅ 사용자 생성 폼 이벤트 리스너 등록 완료");
}

/**
 * 폼 제출 처리 함수
 * @param {Event} e - submit 이벤트
 */
async function handleFormSubmit(e) {
    e.preventDefault();
    console.log("📌 사용자 등록 요청 시작");

    // 폼 데이터 수집
    const formData = collectFormData();

    // 유효성 검사
    if (!validateFormData(formData)) {
        return;
    }

    try {
        await submitUserData(formData);
    } catch (error) {
        console.error("등록 중 오류:", error);
        alert("❌ 네트워크 오류가 발생했습니다. 다시 시도해주세요.");
    }
}

/**
 * 폼 데이터 수집
 * @returns {Object} 폼 데이터 객체
 */
function collectFormData() {
    return {
        username: document.getElementById('username').value.trim(),
        rawPassword: document.getElementById('rawPassword').value,
        email: document.getElementById('email').value.trim(),
        firstName: document.getElementById('firstName').value.trim(),
        lastName: document.getElementById('lastName').value.trim(),
        joinedDate: document.getElementById('joinedDate').value,
        role: document.getElementById('role').value
    };
}

/**
 * 폼 데이터 유효성 검사
 * @param {Object} formData - 검사할 폼 데이터
 * @returns {boolean} 유효성 여부
 */
function validateFormData(formData) {
    const requiredFields = ['username', 'rawPassword', 'email', 'firstName', 'lastName', 'joinedDate', 'role'];

    for (const field of requiredFields) {
        if (!formData[field]) {
            alert(`❌ ${getFieldDisplayName(field)} 필드를 입력해주세요.`);
            return false;
        }
    }

    // 이메일 형식 검사
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!emailRegex.test(formData.email)) {
        alert("❌ 올바른 이메일 형식을 입력해주세요.");
        return false;
    }

    return true;
}

/**
 * 필드명을 한글로 변환
 * @param {string} fieldName - 필드명
 * @returns {string} 한글 필드명
 */
function getFieldDisplayName(fieldName) {
    const fieldNames = {
        username: '사용자명',
        rawPassword: '비밀번호',
        email: '이메일',
        firstName: '이름',
        lastName: '성',
        joinedDate: '입사일',
        role: '역할'
    };
    return fieldNames[fieldName] || fieldName;
}

/**
 * 서버로 사용자 데이터 전송
 * @param {Object} formData - 전송할 데이터
 */
async function submitUserData(formData) {
    console.log("전송할 데이터:", formData);

    const response = await fetch("/api/admin/users", {
        method: "POST",
        headers: {
            "Content-Type": "application/json",
            "Accept": "application/json"
        },
        body: JSON.stringify(formData)
    });

    console.log("응답 상태:", response.status);

    if (response.ok) {
        const result = await response.text();
        console.log("등록 성공:", result);
        handleSuccessfulRegistration();
    } else {
        const errorText = await response.text();
        console.error("등록 실패:", errorText);
        alert("❌ 사용자 등록 실패: " + errorText);
    }
}

/**
 * 등록 성공 후 처리
 */
function handleSuccessfulRegistration() {
    alert("✅ 사용자 등록이 완료되었습니다!");

    // 폼 초기화
    const form = document.getElementById('createUserForm');
    if (form) {
        form.reset();
    }

    // 사용자 목록 페이지로 이동
    const userListLink = document.querySelector('a[data-url="/admin/users"]');
    if (userListLink) {
        userListLink.click();
    }
}
