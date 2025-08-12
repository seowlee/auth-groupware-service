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