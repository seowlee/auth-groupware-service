export const showLoading = (flag) => {
    const el = document.querySelector('.loading');
    if (el) el.style.display = flag ? 'block' : 'none';
}

export const showMessage = (msg, type = 'info') => {
    const ma = document.getElementById('messageArea');
    if (!ma) return;
    ma.innerHTML = `<div class="message ${type}">${msg}</div>`;
    setTimeout(() => ma.innerHTML = '', 3000);
}
export const readErrorMessage = async (res) => {
    const ct = res.headers.get('content-type') || '';
    if (ct.includes('application/json')) {
        const j = await res.json().catch(() => ({}));
        return j.message || j.error || j.detail || `HTTP ${res.status}`;
    } else {
        const t = await res.text().catch(() => '');
        const first = (t || '').split('\n')[0].trim();       // 첫 줄만
        return first.replace(/^[\w.$]+:\s*/, '') || `HTTP ${res.status}`;
    }
}