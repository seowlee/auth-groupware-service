import {loadFeatureScripts} from './home.js';

export function navigateTo(path) {
    history.pushState({path}, '', path);
    loadPageIntoMainContent(path);
}

export function replaceStateAndLoad(path) {
    history.replaceState({path}, '', path);
    loadPageIntoMainContent(path);
}

export async function loadPageIntoMainContent(path) {
    const container = document.getElementById("main-content");
    if (!container) {
        console.error("[ERROR] #main-content not found");
        return;
    }
    try {
        const res = await fetch(path, {
            headers: {'X-Requested-With': 'XMLHttpRequest'}
        });
        // 인증 만료 / 권한 없음 처리
        if (res.status === 401 || res.status === 403) {
            alert("세션이 만료되었습니다. 다시 로그인 해주세요.");
            window.location.href = '/login'; // 또는 Keycloak 로그인 URL
            return;
        }
        // 일반적인 실패 처리
        if (!res.ok) {
            const body = await res.text().catch(() => "");
            container.innerHTML =
                `<p style="color:red;">[ERROR] ${res.status} ${res.statusText} (path: ${path})</p>` +
                (body ? `<pre style="white-space:pre-wrap;">${escapeHtml(body)}</pre>` : "");
            return;
        }
        // HTML 파싱
        const html = await res.text();
        const doc = new DOMParser().parseFromString(html, 'text/html');
        // const fragment = doc.querySelector('[th\\:fragment="content"]') || doc.body;
        // const content = doc.querySelector("[data-fragment='content']");
        let content =
            doc.querySelector("[data-fragment='content']") ||
            doc.querySelector("[th\\:fragment='content']") ||
            doc.body;

        if (!content) {
            container.innerHTML = `<p style="color:red;">[ERROR] fragment content가 비어있습니다. (path: ${path})</p>`;
            return;
        }
        container.innerHTML = '';
        container.innerHTML = content.innerHTML;

        // fragment가 DOM에 들어간 다음에 기능 스크립트 실행
        loadFeatureScripts(path);
    } catch (err) {
        container.innerHTML = `<p style="color:red;">[ERROR] 페이지 로딩 실패 (${err.message})</p>`;
    }
}