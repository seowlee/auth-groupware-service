// src/js/core/router.js

//  중복/경합 로딩 방지 토큰
import {highlightActiveMenu} from "./ui.js";

let currentAbort = null;

// SPA 네비게이션
export function navigateTo(path) {
    history.pushState({path}, '', path);
    return loadPageIntoMainContent(path);
}

export function replaceStateAndLoad(path) {
    history.replaceState({path}, '', path);
    return loadPageIntoMainContent(path);
}

//  프래그먼트만 안전 추출 + 401/403 공통 처리
export async function loadPageIntoMainContent(path) {
    const container = document.getElementById('main-content');
    if (!container) return;

    // [NEW] 이전 요청이 있으면 취소
    if (currentAbort) currentAbort.abort();
    const controller = new AbortController();
    currentAbort = controller;
    // if (!container) {
    //     console.error('[ERROR] #main-content not found');
    //     return;
    // }
    //
    // // 로딩 경합 방지
    // const myToken = ++loadToken;

    // 로딩 표시
    container.dataset.loading = 'true';

    try {
        const res = await fetch(path, {
            headers: {'X-Requested-With': 'XMLHttpRequest'},
            credentials: 'same-origin',
            signal: controller.signal,
        });

        //  인증/권한 만료 공통 처리
        if (res.status === 401 || res.status === 403) {
            alert('세션이 만료되었거나 권한이 없습니다. 다시 로그인해 주세요.');
            // window.location.href = '/login';
            const back = window.location.pathname; // or path
            window.location.href = '/login?redirect=' + encodeURIComponent(back);

            return;
        }

        if (!res.ok) {
            const body = await res.text().catch(() => '');
            container.innerHTML =
                `<p style="color:#c00;">[ERROR] ${res.status} ${res.statusText} (path: ${escapeHtml(path)})</p>` +
                (body ? `<pre style="white-space:pre-wrap;">${escapeHtml(body)}</pre>` : '');
            return;
        }

        const html = await res.text();
        // 이 응답이 “현재 요청”이 아닐 수도 있음 → 컨트롤러 확인
        if (currentAbort !== controller) return; // 이미 새 네비가 시작됨

        const doc = new DOMParser().parseFromString(html, 'text/html');

        // 프래그먼트 탐색 우선순위 (중복 네비 주입 방지)
        //  1) data-fragment="content"
        //  2) th:fragment="content"
        //  3) main[data-fragment], main#main-content (서버가 main만 렌더한 경우)
        //  4) 최후수단: doc.body ( 네비 중복 위험 → 가급적 1~3을 맞추기
        const content =
            doc.querySelector('[data-fragment="content"]') ||
            doc.querySelector('[th\\:fragment="content"]') ||
            doc.querySelector('main [data-fragment="content"]') ||
            doc.querySelector('main#main-content') ||
            doc.body;

        // if (!content) {
        //     // 최후수단: body 전체 삽입은 중복 네비가 생길 수 있으니 경고
        //     console.warn('[router] fragment not found; falling back to <body>');
        //     content = doc.body;
        // }

        // 기존 노드 교체
        container.innerHTML = content.innerHTML;

        // 타이틀 동기화(서버 템플릿이 title 세팅했다면)
        if (doc.title) document.title = doc.title;

        // 스크롤 상단
        container.scrollTop = 0;

        highlightActiveMenu(path);
        runScripts(container);

        // [EVENT] 프래그먼트 삽입 완료 알림 (home.js가 이걸 구독해서 동적 import)
        window.dispatchEvent(new CustomEvent('page:loaded', {detail: {path, container}}));
    } catch (err) {
        if (err.name === 'AbortError') return;
        container.innerHTML = `<p style="color:#c00;">[ERROR] 페이지 로딩 실패 (${escapeHtml(err.message)})</p>`;
    } finally {
        // 로딩 표시 제거
        if (currentAbort === controller) {
            currentAbort = null;
            delete container.dataset.loading;
        }
    }
}

// -----------------------------
// 내부 유틸
// -----------------------------

function escapeHtml(s = '') {
    return s.replace(/[&<>"']/g, (c) => ({
        '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;'
    }[c]));
}

//  프래그먼트 내 인라인 <script> 실행
function runScripts(container) {
    const scripts = container.querySelectorAll('script');
    scripts.forEach((old) => {
        const type = (old.getAttribute('type') || '').toLowerCase();
        const isModule = type === 'module';
        const isJs = !type || type === 'text/javascript' || isModule;

        // JSON, 템플릿 등 실행 비대상은 그대로 두고 패스
        if (!isJs) return;

        // 외부 src 중복 로딩 방지
        if (old.src) {
            const a = document.createElement('a');
            a.href = old.src; // 절대경로화
            if (LOADED_SCRIPT_SRCS.has(a.href)) {
                old.remove();
                return;
            }
            LOADED_SCRIPT_SRCS.add(a.href);
        }

        const s = document.createElement('script');
        if (isModule) s.type = 'module';

        // 보안/로딩 관련 속성 보존
        ['async', 'defer', 'crossorigin', 'referrerpolicy', 'integrity', 'nonce']
            .forEach(attr => {
                if (old.hasAttribute(attr)) s.setAttribute(attr, old.getAttribute(attr));
            });

        if (old.src) {
            s.src = old.src;
        } else {
            s.textContent = old.textContent;
        }

        document.head.appendChild(s);
        old.remove();
    });
}

async function safeText(res) {
    try {
        return await res.text();
    } catch {
        return '';
    }
}
