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

    const res = await fetch(path, {
        headers: {'X-Requested-With': 'XMLHttpRequest'}
    });
    const html = await res.text();

    const parser = new DOMParser();
    const doc = parser.parseFromString(html, 'text/html');
    // const fragment = doc.querySelector('[th\\:fragment="content"]') || doc.body;
    const content = doc.querySelector("[data-fragment='content']");

    const container = document.getElementById("main-content");
    if (!content) {
        container.innerHTML = `<p style="color:red;">[ERROR] fragment content가 비어있습니다. (path: ${path})</p>`;
        return;
    }
    container.innerHTML = '';
    container.innerHTML = content.innerHTML;

    // **★ 이 시점에서 fragment가 DOM에 삽입된 이후!**
    loadFeatureScripts(path);
}