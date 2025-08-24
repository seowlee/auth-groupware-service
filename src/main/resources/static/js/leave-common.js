export const LEAVE_TYPES = [
    {v: 'ANNUAL', t: '연차', cls: 'lb-type-annual'},
    {v: 'BIRTHDAY', t: '생일', cls: 'lb-type-birthday'},
    {v: 'SICK', t: '병가', cls: 'lb-type-sick'},
    {v: 'CUSTOM', t: '기타휴가', cls: 'lb-type-custom'},
];

export const mapLeaveType = (code) =>
    (LEAVE_TYPES.find(o => o.v === code)?.t || code);
export const mapLeaveClass = (code) => LEAVE_TYPES.find(o => o.v === code)?.cls || 'lb-type-custom';
export const leaveTypeOptionsHtml = (selected) =>
    LEAVE_TYPES.map(o =>
        `<option value="${o.v}" ${o.v === selected ? 'selected' : ''}>${o.t}</option>`
    ).join('');

// 숫자 표시(두 자리까지, .000은 제거)
export const fmt3 = (n) => (n == null ? '' : Number(n).toFixed(3).replace(/\.000$/, ''));

export const yearNumberLabel = (target, base) => {
    if (target == null || base == null) return '';
    const diff = Number(target) - Number(base);
    if (diff === 0) return '현재 근속년차';
    if (diff === 1) return '다음 근속년차';
    if (diff === -1) return '이전 근속년차';
    return diff > 1 ? `${diff}년 후` : `${Math.abs(diff)}년 전`;
}