// Behaviour of the absence comments side panel (see the comments_panel fragment in components.html)
(() => {
    const panel = document.getElementById('commentsPanel');
    if (!panel) return;

    const baseUrl = panel.dataset.baseUrl;
    const body = document.getElementById('comments-body');
    const form = document.getElementById('comment-form');

    // Load the comments of the absence whose button opened the panel
    panel.addEventListener('show.bs.offcanvas', async (e) => {
        const url = `${baseUrl}/${e.relatedTarget.dataset.absenceId}/comments`;
        form.action = url;
        body.innerHTML = '';
        const res = await fetch(url);
        body.innerHTML = await res.text();
    });

    // The error only concerns the absence it was raised for
    panel.addEventListener('hidden.bs.offcanvas', () => {
        panel.querySelector('[data-testid="comment-error"]')?.remove();
    });

    // Reopen the panel on the absence that was just commented on
    const openFor = panel.dataset.openFor;
    if (openFor) {
        document.querySelector(`[data-bs-target="#commentsPanel"][data-absence-id="${openFor}"]`)?.click();
    }
})();
