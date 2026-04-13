const TOAST_ICONS = {
    success: 'check_circle',
    warning: 'warning',
    error: 'error'
};

const escapeHtml = (value) => String(value ?? '')
    .replaceAll('&', '&amp;')
    .replaceAll('<', '&lt;')
    .replaceAll('>', '&gt;')
    .replaceAll('"', '&quot;')
    .replaceAll("'", '&#39;');

const ensureToastStack = () => {
    let stack = document.getElementById('adminToastStack');
    if (!stack) {
        stack = document.createElement('div');
        stack.id = 'adminToastStack';
        stack.className = 'admin-toast-stack';
        stack.setAttribute('aria-live', 'polite');
        stack.setAttribute('aria-atomic', 'true');
        document.body.appendChild(stack);
    }
    return stack;
};

const ensureModalHost = () => {
    let host = document.getElementById('adminModalHost');
    if (!host) {
        host = document.createElement('div');
        host.id = 'adminModalHost';
        host.className = 'admin-modal-host';
        host.setAttribute('aria-hidden', 'true');
        document.body.appendChild(host);
    }
    return host;
};

export const showAdminToast = ({
    type = 'success',
    title,
    message,
    duration = 4500
}) => {
    const stack = ensureToastStack();
    const toast = document.createElement('div');
    toast.className = `admin-toast admin-toast--${type}`;
    toast.setAttribute('role', 'status');

    toast.innerHTML = `
        <div class="admin-toast__icon">
            <span class="material-symbols-outlined text-[18px]">${TOAST_ICONS[type] || TOAST_ICONS.success}</span>
        </div>
        <div>
            <div class="admin-toast__title">${escapeHtml(title || 'Thông báo')}</div>
            <div class="admin-toast__message">${escapeHtml(message || '')}</div>
        </div>
        <button type="button" class="admin-toast__close" aria-label="Đóng thông báo">
            <span class="material-symbols-outlined text-[18px]">close</span>
        </button>
    `;

    const removeToast = () => {
        if (toast.isConnected) {
            toast.remove();
        }
    };

    toast.querySelector('.admin-toast__close')?.addEventListener('click', removeToast);
    stack.appendChild(toast);

    window.setTimeout(removeToast, duration);
};

export const openAdminSummaryModal = ({
    title,
    subtitle,
    totalRows = 0,
    successCount = 0,
    failureCount = 0,
    errorDetails = [],
    downloadUrl,
    downloadLabel = 'Tải file lỗi',
    closeLabel = 'Đóng'
}) => {
    const host = ensureModalHost();
    host.classList.add('is-open');
    host.setAttribute('aria-hidden', 'false');

    const rows = (errorDetails || []).slice(0, 20).map((detail) => `
        <tr>
            <td>${escapeHtml(detail.rowNumber ?? '')}</td>
            <td>${escapeHtml(detail.columnName ?? '')}</td>
            <td>${escapeHtml(detail.invalidValue ?? '')}</td>
            <td>${escapeHtml(detail.message ?? '')}</td>
            <td>${escapeHtml(detail.suggestion ?? '')}</td>
        </tr>
    `).join('');

    host.innerHTML = `
        <div class="admin-modal-backdrop"></div>
        <div class="admin-modal-panel" role="dialog" aria-modal="true" aria-label="${escapeHtml(title || 'Kết quả xử lý Excel')}">
            <div class="admin-modal-header">
                <div class="admin-modal-title">${escapeHtml(title || 'Kết quả xử lý Excel')}</div>
                <div class="admin-modal-subtitle">${escapeHtml(subtitle || '')}</div>
            </div>
            <div class="admin-modal-body">
                <div class="admin-summary-grid">
                    <article class="admin-summary-card">
                        <div class="admin-summary-card__label">Tổng dòng</div>
                        <div class="admin-summary-card__value">${escapeHtml(totalRows)}</div>
                    </article>
                    <article class="admin-summary-card">
                        <div class="admin-summary-card__label">Thành công</div>
                        <div class="admin-summary-card__value">${escapeHtml(successCount)}</div>
                    </article>
                    <article class="admin-summary-card">
                        <div class="admin-summary-card__label">Lỗi</div>
                        <div class="admin-summary-card__value">${escapeHtml(failureCount)}</div>
                    </article>
                </div>
                ${rows ? `
                    <div class="overflow-x-auto">
                        <table class="admin-error-table">
                            <thead>
                                <tr>
                                    <th>Dòng</th>
                                    <th>Cột</th>
                                    <th>Giá trị sai</th>
                                    <th>Lý do lỗi</th>
                                    <th>Gợi ý sửa</th>
                                </tr>
                            </thead>
                            <tbody>${rows}</tbody>
                        </table>
                    </div>
                ` : '<div class="text-sm text-slate-500">Không có dòng lỗi chi tiết.</div>'}
                ${errorDetails.length > 20 ? `<div class="text-xs text-slate-500">Đang hiển thị 20 lỗi đầu tiên. Hãy tải file lỗi để xem đầy đủ.</div>` : ''}
            </div>
            <div class="admin-modal-footer">
                ${downloadUrl ? `
                    <a href="${escapeHtml(downloadUrl)}" class="admin-action-btn admin-action-btn--ghost" download>
                        <span class="material-symbols-outlined text-[18px]">download</span>
                        <span>${escapeHtml(downloadLabel)}</span>
                    </a>
                ` : ''}
                <button type="button" class="admin-action-btn admin-action-btn--primary" data-admin-modal-close>
                    ${escapeHtml(closeLabel)}
                </button>
            </div>
        </div>
    `;

    const handleEscape = (event) => {
        if (event.key === 'Escape') {
            closeModal();
        }
    };

    const closeModal = () => {
        host.classList.remove('is-open');
        host.setAttribute('aria-hidden', 'true');
        host.innerHTML = '';
        document.removeEventListener('keydown', handleEscape);
    };

    host.querySelector('[data-admin-modal-close]')?.addEventListener('click', closeModal);
    host.querySelector('.admin-modal-backdrop')?.addEventListener('click', closeModal);
    document.addEventListener('keydown', handleEscape);
};
