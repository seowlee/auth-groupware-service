// export const LEAVE_TYPES = [
//     {v: 'ANNUAL', t: '연차', cls: 'lb-type-annual'},
//     {v: 'BIRTHDAY', t: '생일', cls: 'lb-type-birthday'},
//     {v: 'SICK', t: '병가', cls: 'lb-type-sick'},
//     {v: 'CUSTOM', t: '기타휴가', cls: 'lb-type-custom'},
// ];
//
// export const mapLeaveType = (code) =>
//     (LEAVE_TYPES.find(o => o.v === code)?.t || code);
// export const mapLeaveClass = (code) => LEAVE_TYPES.find(o => o.v === code)?.cls || 'lb-type-custom';
// export const leaveTypeOptionsHtml = (selected) =>
//     LEAVE_TYPES.map(o =>
//         `<option value="${o.v}" ${o.v === selected ? 'selected' : ''}>${o.t}</option>`
//     ).join('');
// ===== ENUM 로딩 공통 =====
export const ensureEnums = () => {
    // 프래그먼트에 #enums-data가 있으면 항상 재파싱하여 window.ENUMS 갱신
    const el = document.getElementById('enums-data');
    if (el) {
        try {
            window.ENUMS = JSON.parse(el.textContent.trim());   // ← 최신 프래그먼트 기준으로 덮어쓰기
        } catch (e) {
            console.error('ENUM 파싱 오류', e);
            window.ENUMS = {roles: [], statuses: [], leaveTypes: []};
        }
        return window.ENUMS;
    }
    // 프래그먼트에 없을 때만 기존 캐시 사용
    return window.ENUMS || (window.ENUMS = {roles: [], statuses: [], leaveTypes: []});
};


// ── 코드 → 라벨(들여쓰기 반영)
export const mapLeaveType = (code) => {
    const {leaveTypes = []} = ensureEnums();
    const item = leaveTypes.find(t => t.name === code);
    if (!item) return code;
    return item.parent ? `　　- ${item.krName}` : item.krName;   // 부모가 있으면 들여쓰기
};

// ── 코드 → 배지 클래스
export const mapLeaveClass = (code) => {
    const {leaveTypes = []} = ensureEnums();
    return leaveTypes.find(t => t.name === code)?.cls || 'lb-type-custom';
};

// ── 편집 Select 옵션(필요 시 특정 타입 제외 가능)
export const leaveTypeOptionsHtml = (selected) => {
    const {leaveTypes = []} = ensureEnums();
    // 예: 정산(ADVANCE/BORROWED)은 선택 불가로 숨기고 싶다면 filter로 제외
    const list = leaveTypes.filter(t => !t.parent); // 상위 타입만 노출
    return list
        .map(t => `<option value="${t.name}" ${t.name === selected ? 'selected' : ''}>${t.krName}</option>`)
        .join('');
};
// 숫자 표시(두 자리까지, .000은 제거)
export const fmt3 = (n) => (n == null ? '' : Number(n).toFixed(3).replace(/\.000$/, ''));

// (8시간 = 1일 기준 변환)
export function formatLeaveDays(daysDecimal) {
    if (daysDecimal == null) return "";
    const hours = Math.round(daysDecimal * 8); // 1일 = 8시간
    const d = Math.floor(hours / 8);
    const h = hours % 8;
    if (h === 0) return `${d}일`;
    if (d === 0) return `${h}시간`;
    return `${d}일 ${h}시간`;
}

export const yearNumberLabel = (target, base) => {
    if (target == null || base == null) return '';
    const diff = Number(target) - Number(base);
    if (diff === 0) return '현재 근속년차';
    if (diff === 1) return '다음 근속년차';
    if (diff === -1) return '이전 근속년차';
    return diff > 1 ? `${diff}년 후` : `${Math.abs(diff)}년 전`;
}

// 시간 슬롯 공통
export const ALLOWED_START_TIMES = ["09:00", "13:00"];
export const ALLOWED_END_TIMES = ["13:00", "17:00"];

// 같은 날짜에서 가능한 조합
export function isValidSameDayRange(startHHmm, endHHmm) {
    return (
        (startHHmm === "09:00" && (endHHmm === "13:00" || endHHmm === "17:00")) ||
        (startHHmm === "13:00" && endHHmm === "17:00")
    );
}

// 날짜/시간 유효성 (문자열)
export function validateDateTimes(startDate, startHHmm, endDate, endHHmm) {
    // 날짜 형식 및 순서 (날짜 우선)
    const dateRe = /^\d{4}-\d{2}-\d{2}$/;
    if (!dateRe.test(startDate) || !dateRe.test(endDate)) {
        return "날짜 형식을 확인해주세요. (예: 2025-08-21)";
    }
    // 단순 문자열 비교로도 YYYY-MM-DD는 크기 비교 가능
    if (endDate < startDate) {
        return "종료 날짜가 시작 날짜보다 앞설 수 없습니다.";
    }
    // 시간 슬롯 제약
    if (!ALLOWED_START_TIMES.includes(startHHmm) || !ALLOWED_END_TIMES.includes(endHHmm)) {
        return "시작/종료 시간은 09:00/13:00/17:00 중에서 선택해주세요.";
    }
    // 같은 날짜인 경우 허용 가능한 조합만 통과
    if (startDate === endDate) {
        if (!isValidSameDayRange(startHHmm, endHHmm)) {
            return "같은 날짜에서는 (09→13), (09→17), (13→17) 조합만 가능합니다.";
        }
    } else {
        //  다른 날짜 구간이면 최종적으로 시작<종료 보장 (실제 Date 비교)
        const s = new Date(`${startDate}T${startHHmm}:00`);
        const e = new Date(`${endDate}T${endHHmm}:00`);
        if (Number.isNaN(s.getTime()) || Number.isNaN(e.getTime())) {
            return "시작/종료 일시 형식을 확인해주세요.";
        }
        if (s >= e) {
            return "시작 일시는 종료 일시보다 빨라야 합니다.";
        }
    }
    return null; // ok
}