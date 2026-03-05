export const renderPagination = (pageResponse, container, onPageChange) => {
    const {page: currentPage, pageSize, totalPages, totalItems} = pageResponse;

    if (totalItems === 0) {
        container.innerHTML = '<div class="px-6 py-4 text-sm text-slate-500">Không có dữ liệu</div>';
        return;
    }

    const startItem = (currentPage - 1) * pageSize + 1;
    const endItem = Math.min(currentPage * pageSize, totalItems);

    // Class Tailwind cấu hình sẵn
    const baseBtnClass = "px-3 py-1.5 text-sm font-medium rounded-md transition-colors";
    const inactiveClass = `${baseBtnClass} text-slate-600 dark:text-slate-300 bg-white dark:bg-white/5 border border-border-light dark:border-border-dark hover:bg-slate-50 dark:hover:bg-white/10`;
    const activeClass = `${baseBtnClass} text-primary-content bg-primary hover:bg-primary/90`;
    const disabledClass = `${inactiveClass} opacity-50 cursor-not-allowed`;

    // Wrapper của Footer
    let html = `
        <div class="flex items-center justify-between px-6 py-4 border-t border-border-light dark:border-border-dark bg-slate-50/50 dark:bg-white/5">
            <div class="text-sm text-slate-500 dark:text-slate-400">
                Hiển thị <span class="font-medium text-slate-900 dark:text-white">${startItem}</span> 
                đến <span class="font-medium text-slate-900 dark:text-white">${endItem}</span> 
                của <span class="font-medium text-slate-900 dark:text-white">${totalItems}</span> kết quả
            </div>
            <div class="flex gap-2">
    `;

    // 1. Nút Trước
    const prevDisabled = currentPage <= 1;
    html += `
        <button class="${prevDisabled ? disabledClass : inactiveClass}" ${prevDisabled ? 'disabled' : ''} data-page="${currentPage - 1}">
            Trước
        </button>
    `;

    // 2. Thuật toán sinh mảng trang (có dấu ...)
    let pages = [];
    if (totalPages <= 5) {
        for (let i = 1; i <= totalPages; i++) pages.push(i);
    } else {
        if (currentPage <= 3) pages = [1, 2, 3, 4, '...', totalPages];
        else if (currentPage >= totalPages - 2) pages = [1, '...', totalPages - 3, totalPages - 2, totalPages - 1, totalPages];
        else pages = [1, '...', currentPage - 1, currentPage, currentPage + 1, '...', totalPages];
    }

    // 3. Render các nút số trang
    pages.forEach(p => {
        if (p === '...') {
            html += `<span class="px-2 py-1.5 text-slate-400">...</span>`;
        } else {
            const isCurrent = (p === currentPage);
            html += `
                <button class="${isCurrent ? activeClass : inactiveClass}" data-page="${p}">
                    ${p}
                </button>
            `;
        }
    });

    // 4. Nút Sau
    const nextDisabled = currentPage >= totalPages;
    html += `
        <button class="${nextDisabled ? disabledClass : inactiveClass}" ${nextDisabled ? 'disabled' : ''} data-page="${currentPage + 1}">
            Sau
        </button>
    `;

    html += `
            </div>
        </div>
    `;

    container.innerHTML = html;

    // Lắng nghe sự kiện click bằng cách tìm các button có data-page
    const buttons = container.querySelectorAll('button[data-page]');
    buttons.forEach(btn => {
        btn.addEventListener('click', (e) => {
            if (btn.disabled) return;
            const newPage = parseInt(btn.getAttribute('data-page'));
            if (!isNaN(newPage)) onPageChange(newPage);
        });
    });
};