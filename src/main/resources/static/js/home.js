// src/js/home.js
import {initUserListManager} from './user-list.js';
import {initUserDetail} from './user-detail.js';
import {initLeaveListManager} from "./leave-list.js";
import {loadPageIntoMainContent, navigateTo, replaceStateAndLoad} from './router.js';
import {initLeaveEdit} from "./leave-form.js";
import {initLeaveCreate} from "./create-leave-form.js";


export function setupHomePage() {
    document.addEventListener('DOMContentLoaded', onInitialLoad);
    onInitialLoad();
    document.querySelectorAll('.menu-link').forEach(link =>
        link.addEventListener('click', onMenuClick)
    );
    const ham = document.querySelector('.hamburger-btn');
    if (ham) ham.addEventListener('click', toggleMobileMenu);
    window.addEventListener('popstate', onPopState);
    window.addEventListener('resize', onWindowResize);
    showFlashToastIfAny();
}

function onInitialLoad() {
    const path = window.location.pathname;
    if (path === '/home') {
        replaceStateAndLoad('/team/users');
    } else {
        loadPageIntoMainContent(path);
    }
}

function onMenuClick(e) {
    e.preventDefault();
    const url = e.currentTarget.dataset.url;
    navigateTo(url);
}

function onPopState(e) {
    const url = e.state?.path || '/team/users';
    loadPageIntoMainContent(url);
}


function toggleMobileMenu() {
    document.querySelector('nav ul').classList.toggle('show');
}

function onWindowResize() {
    if (window.innerWidth > 768) {
        document.querySelector('nav ul').classList.remove('show');
    }
}

function showMainFloatToast(msg, type = 'success') {
    const main = document.getElementById('main-content');
    if (!main) return;

    // 래퍼가 없으면 생성해서 main에 부착
    let wrap = main.querySelector('.main-float-toast');
    if (!wrap) {
        wrap = document.createElement('div');
        wrap.className = 'main-float-toast';
        main.appendChild(wrap);
    }
    // 토스트 카드 생성
    const card = document.createElement('div');
    card.className = `toast-card ${type}`;
    card.textContent = msg;
    wrap.appendChild(card);
    // 등장 애니메이션 + 자동 제거
    requestAnimationFrame(() => card.classList.add('show'));
    setTimeout(() => card.classList.remove('show'), 2800);
    setTimeout(() => card.remove(), 3500);
}

function showFlashToastIfAny() {
    const msgMeta = document.querySelector('meta[name="toast-msg"]');
    const typeMeta = document.querySelector('meta[name="toast-type"]');
    const msg = msgMeta?.content;
    const type = (typeMeta?.content) || 'success';
    if (!msg) return;

    const main = document.getElementById('main-content');

    const run = () => {
        showMainFloatToast(msg, type);
        // 한 번만 뜨도록 비워둔다
        msgMeta.content = '';
        if (typeMeta) typeMeta.content = '';
    };

    // SPA로 프래그먼트가 비동기로 들어오니, 채워진 뒤에 실행
    if (main && main.children.length > 0) {
        run();
    } else {
        const mo = new MutationObserver(() => {
            if (main.children.length > 0) {
                mo.disconnect();
                run();
            }
        });
        mo.observe(main, {childList: true});
    }
}

function highlightActiveMenu(path) {
    document.querySelectorAll('.menu-link').forEach(l => {
        l.classList.toggle('active', l.dataset.url === path);
    });
}

function runScripts(container) {
    const scripts = container.querySelectorAll('script');
    scripts.forEach(old => {
        const s = document.createElement('script');
        if (old.src) {
            s.src = old.src;
        } else {
            s.textContent = old.textContent;
        }
        document.head.appendChild(s);
        old.remove();
    });
}

export function loadFeatureScripts(path) {
    if (path === '/team/users') {
        initUserListManager(userId => {
            navigateTo(`/team/users/${userId}`);
        });
    } else if (path.startsWith('/team/users/')) {
        initUserDetail();
    } else if (path === '/admin/users/new') {
        import('./create-user-form.js').then(m => m.initCreateUserForm());
    } else if (path === '/leaves') {
        initLeaveListManager(leaveId => {
            navigateTo(`/leaves/${leaveId}`);
        });
    } else if (path === '/leaves/calendar') {
        initLeaveListManager(leaveId => {
            navigateTo(`/leaves/${leaveId}`);
        });
    } else if (path === '/leaves/apply') {
        initLeaveCreate();
    } else if (path.startsWith('/leaves/') && path !== '/leaves') {
        initLeaveEdit();
    }
}

document.addEventListener('DOMContentLoaded', setupHomePage);
// // SPA 초기화
// setupHomePage();
// window.loadPageIntoMainContent = loadPageIntoMainContent;