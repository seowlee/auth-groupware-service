export function showMainFloatToast(msg, type = 'success') {
    const main = document.getElementById('main-content');
    if (!main) return;
    let wrap = main.querySelector('.main-float-toast');
    if (!wrap) {
        wrap = document.createElement('div');
        wrap.className = 'main-float-toast';
        main.appendChild(wrap);
    }
    const card = document.createElement('div');
    card.className = `toast-card ${type}`;
    card.textContent = msg;
    wrap.appendChild(card);
    requestAnimationFrame(() => card.classList.add('show'));
    setTimeout(() => card.classList.remove('show'), 2800);
    setTimeout(() => card.remove(), 3500);
}

export function showFlashToastIfAny() {
    const msgMeta = document.querySelector('meta[name="toast-msg"]');
    const typeMeta = document.querySelector('meta[name="toast-type"]');
    const msg = msgMeta?.content;
    const type = typeMeta?.content || 'success';
    if (!msg) return;

    const main = document.getElementById('main-content');
    const run = () => {
        showMainFloatToast(msg, type);
        msgMeta.content = '';
        if (typeMeta) typeMeta.content = '';
    };

    if (main && main.children.length > 0) run();
    else if (main) {
        const mo = new MutationObserver(() => {
            if (main.children.length > 0) {
                mo.disconnect();
                run();
            }
        });
        mo.observe(main, {childList: true});
    }
}

export function highlightActiveMenu(path) {
    document.querySelectorAll('.menu-link').forEach(l => {
        l.classList.toggle('active', l.dataset.url === path);
    });
}

export function updateIntegrationButtonsFromDataset() {
    const {kakaoLinked, m365Linked, isSuperAdmin} = document.body.dataset;
    const kakaoBtn = document.getElementById('btn-kakao-link');
    if (kakaoBtn) {
        const linked = (kakaoLinked === 'true');
        kakaoBtn.disabled = linked;
        kakaoBtn.textContent = linked ? '카카오 연동 완료' : '카카오 연동';
        kakaoBtn.classList.toggle('btn-disabled', linked);
    }
    const m365Btn = document.getElementById('btn-m365-link');
    if (m365Btn) {
        const linked = (m365Linked === 'true');
        // const admin = (isSuperAdmin === 'true');
        m365Btn.disabled = linked;
        m365Btn.textContent = linked ? 'Microsoft 365 연동 완료' : 'Microsoft 365 연동';
        m365Btn.classList.toggle('btn-disabled', linked);
    }
}
