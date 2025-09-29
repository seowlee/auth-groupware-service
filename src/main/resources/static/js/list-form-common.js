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

export async function apiFetch(input, init = {}, redirectPath) {
    const headers = new Headers(init.headers || {});
    if (!headers.has('X-Requested-With')) headers.set('X-Requested-With', 'XMLHttpRequest');
    // ★ JSON API가 기본이면 Accept도 기본값으로
    if (!headers.has('Accept')) headers.set('Accept', 'application/json');
    // const method = (init.method || 'GET').toUpperCase();
    const hasBody = !!init.body;
    // ★ FormData/Blob/URLSearchParams는 직접 Content-Type을 세팅하지 않음
    const isFormLike =
        typeof FormData !== 'undefined' && init.body instanceof FormData ||
        typeof Blob !== 'undefined' && init.body instanceof Blob ||
        typeof URLSearchParams !== 'undefined' && init.body instanceof URLSearchParams;

    if (hasBody && !headers.has('Content-Type') && !isFormLike) {
        headers.set('Content-Type', 'application/json');
    }

    const res = await fetch(input, {
        credentials: 'same-origin', ...init, headers
    });

    // 401/403 최우선 처리
    if (res.status === 401) {
        alert("세션이 종료되었습니다. 다시 로그인해 주세요.");
        const back = redirectPath || window.location.pathname;
        window.location.href = "/login?redirect=" + encodeURIComponent(back);
        throw new Error("UNAUTHORIZED");
    }
    if (res.status === 403) {
        alert("해당 작업에 대한 권한이 없습니다.");
        throw new Error("FORBIDDEN");
    }
    return res;
}