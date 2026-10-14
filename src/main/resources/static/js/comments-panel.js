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
        if (res.ok) {
            body.innerHTML = await res.text();
        } else {
            body.innerHTML = '<p class="text-danger" data-testid="comments-load-error">Unable to load the comments.</p>';
        }
    });

    // Toggle between the text of a comment and its edit form
    body.addEventListener('click', (e) => {
        const action = e.target.dataset.commentAction;
        if (action !== 'edit' && action !== 'cancel') return;
        const comment = e.target.closest('[data-testid="absence-comment"]');
        const editForm = comment.querySelector('[data-testid="edit-comment-form"]');
        if (action === 'cancel') editForm.reset();
        comment.querySelector('[data-testid="comment-text"]').classList.toggle('d-none', action === 'edit');
        editForm.classList.toggle('d-none', action === 'cancel');
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
