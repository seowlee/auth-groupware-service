// src/js/core/loader.js

// ✅ [신규] 외부 리소스 1회 로더 (중복 로딩 방지)
const externalOnce = new Map();

/**
 * 외부 스크립트를 1회만 로드
 * @param {string} src
 * @param {{module?: boolean, async?: boolean, defer?: boolean, attrs?: Object}} [opt]
 */
export function loadScriptOnce(src, opt = {}) {
    if (externalOnce.has(src)) return externalOnce.get(src);

    const {module = false, async = true, defer = false, attrs = {}} = opt;
    const p = new Promise((resolve, reject) => {
        const s = document.createElement('script');
        s.src = src;
        if (module) s.type = 'module';
        if (async) s.async = true;
        if (defer) s.defer = true;
        Object.entries(attrs).forEach(([k, v]) => s.setAttribute(k, v));
        s.onload = () => resolve();
        s.onerror = () => reject(new Error(`Failed to load script: ${src}`));
        document.head.appendChild(s);
    });

    externalOnce.set(src, p);
    return p;
}

/**
 * 외부 스타일시트를 1회만 로드
 * @param {string} href
 */
export function loadStyleOnce(href) {
    if (externalOnce.has(href)) return externalOnce.get(href);

    const p = new Promise((resolve, reject) => {
        const l = document.createElement('link');
        l.rel = 'stylesheet';
        l.href = href;
        l.onload = () => resolve();
        l.onerror = () => reject(new Error(`Failed to load style: ${href}`));
        document.head.appendChild(l);
    });

    externalOnce.set(href, p);
    return p;
}

// ---------------------------------------
// (옵션) 공통 fetch 래퍼 + CSRF 헬퍼
//  - 화면 JS에서 API 호출할 때 재사용 가능
//  - 라우팅(페이지 교체)은 router.js가 처리하므로 여기선 API만
// ---------------------------------------

/** 메타에서 CSRF 정보 추출 */
export function readCsrfMeta() {
    const tokenMeta = document.querySelector('meta[name="_csrf"]');
    const headerMeta = document.querySelector('meta[name="_csrf_header"]');
    return {
        token: tokenMeta?.content,
        header: headerMeta?.content || 'X-CSRF-TOKEN',
    };
}

/**
 * JSON API 호출(401/403 공통 처리 포함)
 * @param {string} url
 * @param {RequestInit & {expect?: 'json'|'text'}} init
 */
export async function fetchWithCsrf(url, init = {}) {
    const {token, header} = readCsrfMeta();

    const headers = new Headers(init.headers || {});
    headers.set('Accept', 'application/json');
    if (init.body && !(init.body instanceof FormData)) {
        headers.set('Content-Type', 'application/json');
    }
    if (token) headers.set(header, token);

    const res = await fetch(url, {
        ...init,
        headers,
        credentials: 'same-origin',
    });

    if (res.status === 401 || res.status === 403) {
        alert('세션이 만료되었습니다. 다시 로그인해 주세요.');
        window.location.href = '/login';
        return Promise.reject(new Error(`${res.status}`));
    }

    if (!res.ok) {
        const msg = await res.text().catch(() => '');
        throw new Error(`${res.status} ${res.statusText} - ${msg}`);
    }

    // 응답 자동 파싱
    const expect = init.expect || 'json';
    return expect === 'text' ? res.text() : res.json();
}
