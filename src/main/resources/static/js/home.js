// [ENTRY] 홈 엔트리: 라우터를 단방향 import, 프래그먼트 완료 이벤트(page:loaded) 구독
//        경로별 기능 모듈/외부 CDN을 "필요할 때"만 동적 로딩
//
import {loadPageIntoMainContent, navigateTo, replaceStateAndLoad} from './core/router.js';
import {loadScriptOnce, loadStyleOnce} from './core/loader.js';
import {showFlashToastIfAny, updateIntegrationButtonsFromDataset} from './core/ui.js';

// [NOTE] 일부 브라우저에서 초기 진입 시 중복 실행을 막기 위한 가드(선택)
let featuresInitializedOnce = false;
// ==============================
// 홈 페이지 초기 세팅
// ==============================
export async function setupHomePage() {
    // 프래그먼트 로드 완료 이벤트를 한 곳에서 처리
    window.addEventListener('page:loaded', (e) => {
        onFragmentLoaded(e).catch(console.error);
    });

    await onInitialLoad();
    // 1) 서버 플래시 메시지 토스트
    showFlashToastIfAny();
    // 2) 버튼 상태 즉시 반영
    updateIntegrationButtonsFromDataset();

    // SPA 네비게이션 (이벤트 위임)
    document.addEventListener('click', onAnyMenuClick);

    // 햄버거
    // 오프캔버스 초기화(모바일 네비 클릭 시 닫기 + 네비게이션 보장)
    initNavOffcanvas();
    // 뒤로/앞으로
    window.addEventListener('popstate', (e) => {
        onPopState(e).catch(console.error);
    });

    // 반응형
    window.addEventListener('resize', onWindowResize);

}

// ==============================
// 네비게이션
// ==============================
async function onInitialLoad() {
    const path = window.location.pathname;
    if (path.startsWith('/home')) {
        // 로그인 후 홈 → 기본 탭
        await replaceStateAndLoad('/leaves/calendar');
    } else {
        await loadPageIntoMainContent(path);
    }
}

function onAnyMenuClick(e) {
    const a = e.target.closest('a.menu-link');
    if (!a) return;
    e.preventDefault();
    const url = a.dataset.url;
    if (url) void navigateTo(url);
}

async function onPopState(e) {
    const url = e.state?.path || '/team/users';
    await loadPageIntoMainContent(url);
}

function toggleMobileMenu() {
    document.querySelector('nav ul')?.classList.toggle('show');
}

// 모바일 오프캔버스: drawer 전체를 토글 + 내부 항목 클릭 시 네비 후 닫기
function initNavOffcanvas() {
    if (window.__navInitDone) return;
    window.__navInitDone = true;

    const nav = document.querySelector('nav');
    const drawer = nav && nav.querySelector('.nav-drawer');
    const btn = nav && nav.querySelector('.hamburger-btn');
    const backdrop = document.querySelector('.nav-backdrop');
    if (!nav || !drawer || !btn || !backdrop) return;

    const open = () => {
        drawer.classList.add('open');
        backdrop.hidden = false;
        document.body.style.overflow = 'hidden';
    };
    const close = () => {
        drawer.classList.remove('open');
        backdrop.hidden = true;
        document.body.style.overflow = '';
    };

    btn.addEventListener('click', (e) => {
        e.preventDefault();
        (drawer.classList.contains('open') ? close : open)();
    });

    backdrop.addEventListener('click', close);
    window.addEventListener('keydown', (e) => {
        if (e.key === 'Escape') close();
    });

    // (A) 드로어 내부 메뉴 링크 클릭 시: 닫기만 (네비게이션은 전역 onAnyMenuClick이 처리)
    drawer.addEventListener('click', (e) => {
        const a = e.target.closest('a.menu-link');
        if (!a) return;
        // 기본 앵커 동작은 전역에서 preventDefault + navigateTo 수행
        // 여기서는 드로어만 닫아 UI가 가리지 않게 함
        close();
    });

    // (B) 드로어 내부 폼 제출(카카오 연동/로그아웃/M365): 제출은 그대로, 바로 닫기
    drawer.addEventListener('submit', () => {
        // 기본 제출 흐름 보장 후 비동기적으로 닫기
        setTimeout(close, 0);
    });

    // (C) 버튼 클릭만으로도 닫히게 (submit은 기본대로 진행됨)
    drawer.querySelectorAll('.nav-action-form .nav-btn')
        .forEach(b => b.addEventListener('click', () => {
            // 제출 버튼이면 폼 submit과 함께 닫힘, 단순 버튼도 닫힘
            setTimeout(close, 0);
        }));
}

function onWindowResize() {
    if (window.innerWidth > 1024) {
        // document.querySelector('nav ul')?.classList.remove('show');
        const drawer = document.querySelector('.nav-drawer');
        const backdrop = document.querySelector('.nav-backdrop');
        if (drawer) drawer.classList.remove('open');
        if (backdrop) backdrop.hidden = true;
        document.body.style.overflow = '';
    }
}

// ====== [핵심] 프래그먼트 삽입 완료 후 경로별 초기화 ======
async function onFragmentLoaded(e) {
    const path = e.detail?.path || window.location.pathname;

    // 경로별 기능 모듈을 지연 로딩
    // 팀 사용자 목록
    if (path === '/team/users') {
        const m = await import('./user-list.js');
        m.initUserListManager?.(id => navigateTo(`/team/users/${id}`));
    } // 사용자 상세 (/team/users/{id})
    else if (path.startsWith('/team/users/') && path !== '/team/users') {
        const m = await import('./user-detail.js');
        await m.initUserDetail?.();
    } // 사용자 등록 (최고관리자)
    else if (path === '/admin/users/new') {
        const m = await import('./create-user-form.js');
        await m.initCreateUserForm?.();
    } // 연차 목록
    else if (path === '/leaves') {
        const m = await import('./leave-list.js');
        m.initLeaveListManager?.(id => navigateTo(`/leaves/${id}`));
    } // 연차 캘린더 (FullCalendar를 필요)
    else if (path.startsWith('/leaves/calendar')) {
        await loadScriptOnce('https://cdn.jsdelivr.net/npm/fullcalendar@6.1.15/index.global.min.js');
        const m = await import('./leave-calendar.js');
        m.initLeaveCalendarManager?.(id => navigateTo(`/leaves/${id}`));
    } // 연차 신청 (flatpickr 필요)
    else if (path === '/leaves/apply') {
        // 외부 위젯 로딩
        await loadStyleOnce('https://cdn.jsdelivr.net/npm/flatpickr/dist/flatpickr.min.css');
        await loadScriptOnce('https://cdn.jsdelivr.net/npm/flatpickr');
        await loadScriptOnce('https://cdn.jsdelivr.net/npm/flatpickr/dist/l10n/ko.js');
        const m = await import('./create-leave-form.js');
        m.initLeaveCreate?.();
    } // 연차 수정 (/leaves/{id})
    else if (path.startsWith('/leaves/') && path !== '/leaves') {
        const m = await import('./leave-form.js');
        m.initLeaveEdit?.();
    } // 감사 로그
    else if (path === '/admin/logs') {
        const m = await import('./audit-log.js');
        m.initAuditLogList?.();
    }
    // 프래그먼트가 들어온 뒤에도 버튼 상태를 항상 재반영
    updateIntegrationButtonsFromDataset();
    // 초기 진입 시 중복 방지(일부 브라우저에서 DOMContentLoaded 직후 한 번 더 들어오는 걸 방지)
    if (!featuresInitializedOnce) featuresInitializedOnce = true;
}


// 엔트리
document.addEventListener('DOMContentLoaded', () => {
    setupHomePage().catch(console.error);
});
