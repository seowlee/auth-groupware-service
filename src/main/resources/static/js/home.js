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
    // 메뉴 링크 클릭 → SPA 네비게이션
    // document.querySelectorAll('.menu-link').forEach(link => {
    //     link.addEventListener('click', onMenuClick);
    // });
    // 메뉴 클릭(이벤트 위임)
    document.addEventListener('click', (e) => {
        const link = e.target.closest('.menu-link');
        if (!link) return;
        e.preventDefault();
        const url = link.dataset.url;
        if (url) void navigateTo(url);
    });

    // 햄버거
    const ham = document.querySelector('.hamburger-btn');
    if (ham) ham.addEventListener('click', toggleMobileMenu);

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

async function onMenuClick(e) {
    e.preventDefault();
    const url = e.currentTarget.dataset.url;
    if (!url) return;
    await navigateTo(url);
}

async function onPopState(e) {
    const url = e.state?.path || '/team/users';
    await loadPageIntoMainContent(url);
}

function toggleMobileMenu() {
    document.querySelector('nav ul')?.classList.toggle('show');
}

function onWindowResize() {
    if (window.innerWidth > 768) {
        document.querySelector('nav ul')?.classList.remove('show');
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
