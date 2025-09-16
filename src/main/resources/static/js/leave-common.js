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
    if (window.ENUMS) return window.ENUMS;
    const el = document.getElementById('enums-data');
    try {
        window.ENUMS = el ? JSON.parse(el.textContent.trim()) : {roles: [], statuses: [], leaveTypes: []};
    } catch {
        window.ENUMS = {roles: [], statuses: [], leaveTypes: []};
    }
    return window.ENUMS;
}


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
    return (leaveTypes.find(t => t.name === code)?.cls) || 'lb-type-custom';
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