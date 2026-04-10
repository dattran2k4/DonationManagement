import {userApi} from '../../apis/userApi.js';
import {renderPagination} from '../../components/pagination.js';

const state = {
    page: 1,
    size: 10,
    search: '',
    status: '',
    sortBy: 'id',
    sortDir: 'desc',
    selectedIds: new Set()
};

const elements = {
    tableBody: document.getElementById('userTableBody'),
    paginationContainer: document.getElementById('paginationContainer'),
    searchInput: document.getElementById('userSearchInput'),
    statusFilter: document.getElementById('userStatusFilter'),
    resetFilterBtn: document.getElementById('userResetFilterBtn'),
    selectAllCheckbox: document.getElementById('selectAllUsersCheckbox'),
    deleteSelectedUsersBtn: document.getElementById('deleteSelectedUsersBtn')
};

const roleLabels = {
    ADMIN: 'Chủ nhiệm',
    STAFF: 'Thành viên',
    ACCOUNTING: 'Kế toán',
    DONOR: 'Nhà hảo tâm'
};

const roleBadgeClass = {
    ADMIN: 'bg-emerald-100 text-emerald-700',
    ACCOUNTING: 'bg-blue-100 text-blue-700',
    STAFF: 'bg-slate-100 text-slate-600',
    DONOR: 'bg-purple-100 text-purple-700'
};

const statusBadgeClass = {
    ACTIVE: 'bg-emerald-100 text-emerald-700',
    INACTIVE: 'bg-slate-100 text-slate-500'
};

const statusLabel = {
    ACTIVE: 'Đang hoạt động',
    INACTIVE: 'Tạm khóa'
};

const renderRoleBadge = (role) => `
    <span class="inline-flex items-center rounded-md px-2 py-1 text-xs font-semibold ${roleBadgeClass[role] || 'bg-slate-100 text-slate-600'}">
        ${(roleLabels[role] || role || '---').toUpperCase()}
    </span>
`;

const renderStatusBadge = (status) => `
    <span class="inline-flex items-center gap-1 rounded-full px-3 py-1 text-sm font-semibold ${statusBadgeClass[status] || 'bg-slate-100 text-slate-500'}">
        <span class="h-1.5 w-1.5 rounded-full bg-current opacity-70"></span>
        ${statusLabel[status] || '---'}
    </span>
`;

const renderRows = (users) => {
    if (!users.length) {
        elements.tableBody.innerHTML = `
            <tr>
                <td colspan="6" class="px-6 py-10 text-center text-text-secondary">Không tìm thấy thành viên phù hợp</td>
            </tr>
        `;
        if (elements.selectAllCheckbox) elements.selectAllCheckbox.checked = false;
        updateDeleteButtonState();
        return;
    }

    elements.tableBody.innerHTML = users.map((user) => `
        <tr class="border-b border-border-light hover:bg-slate-50 transition-colors">
            <td class="px-6 py-5">
                <input type="checkbox" data-user-checkbox data-user-id="${user.id}"
                       class="h-6 w-6 rounded-md border-slate-300 text-primary focus:ring-primary/30"
                       ${state.selectedIds.has(user.id) ? 'checked' : ''}/>
            </td>
            <td class="px-6 py-5 text-lg font-semibold text-slate-900">
                <a href="/admin/users/${user.id}" class="hover:text-primary">${user.fullName || '---'}</a>
            </td>
            <td class="px-6 py-5 text-base text-slate-500">${user.phone || '---'}</td>
            <td class="px-6 py-5 text-base text-slate-500">${user.email || '---'}</td>
            <td class="px-6 py-5">${renderRoleBadge(user.role)}</td>
            <td class="px-6 py-5">${renderStatusBadge(user.status)}</td>
        </tr>
    `).join('');

    bindRowCheckboxEvents();
    syncSelectAllState();
    updateDeleteButtonState();
};

const getVisibleCheckboxes = () => Array.from(document.querySelectorAll('[data-user-checkbox]'));

const syncSelectAllState = () => {
    if (!elements.selectAllCheckbox) return;
    const checkboxes = getVisibleCheckboxes();
    if (!checkboxes.length) {
        elements.selectAllCheckbox.checked = false;
        return;
    }
    elements.selectAllCheckbox.checked = checkboxes.every((cb) => cb.checked);
};

const updateDeleteButtonState = () => {
    if (!elements.deleteSelectedUsersBtn) return;
    elements.deleteSelectedUsersBtn.disabled = state.selectedIds.size === 0;
};

const bindRowCheckboxEvents = () => {
    getVisibleCheckboxes().forEach((checkbox) => {
        checkbox.addEventListener('change', () => {
            const userId = Number(checkbox.dataset.userId);
            if (!userId) return;
            if (checkbox.checked) {
                state.selectedIds.add(userId);
            } else {
                state.selectedIds.delete(userId);
            }
            syncSelectAllState();
            updateDeleteButtonState();
        });
    });
};

const bindSelectAllEvent = () => {
    elements.selectAllCheckbox?.addEventListener('change', () => {
        const shouldSelectAll = elements.selectAllCheckbox.checked;
        getVisibleCheckboxes().forEach((checkbox) => {
            checkbox.checked = shouldSelectAll;
            const userId = Number(checkbox.dataset.userId);
            if (!userId) return;
            if (shouldSelectAll) {
                state.selectedIds.add(userId);
            } else {
                state.selectedIds.delete(userId);
            }
        });
        updateDeleteButtonState();
    });
};

const handleDeleteSelectedUsers = async () => {
    if (!state.selectedIds.size) return;
    const ids = Array.from(state.selectedIds);
    const confirmed = window.confirm(`Bạn có chắc muốn xóa ${ids.length} thành viên đã chọn?`);
    if (!confirmed) return;

    try {
        await userApi.deleteUsers(ids);
        state.selectedIds.clear();
        if (elements.selectAllCheckbox) elements.selectAllCheckbox.checked = false;
        await loadUsers();
    } catch (error) {
        alert(error?.message || 'Không thể xóa thành viên đã chọn.');
    }
};

const loadUsers = async () => {
    try {
        const response = await userApi.getAllUsers(state);
        const pageData = response.data;
        const users = pageData?.data || [];

        renderRows(users);
        renderPagination(pageData, elements.paginationContainer, (newPage) => {
            state.page = newPage;
            loadUsers();
        });
    } catch (error) {
        console.error('Error loading users:', error);
    }
};

const debounce = (fn, delay = 350) => {
    let timeoutId;
    return (...args) => {
        clearTimeout(timeoutId);
        timeoutId = setTimeout(() => fn(...args), delay);
    };
};

const bindEvents = () => {
    elements.searchInput?.addEventListener('input', debounce((event) => {
        state.search = event.target.value.trim();
        state.page = 1;
        loadUsers();
    }));

    elements.statusFilter?.addEventListener('change', (event) => {
        state.status = event.target.value;
        state.page = 1;
        loadUsers();
    });

    elements.resetFilterBtn?.addEventListener('click', () => {
        state.search = '';
        state.status = '';
        state.page = 1;

        if (elements.searchInput) elements.searchInput.value = '';
        if (elements.statusFilter) elements.statusFilter.value = '';

        loadUsers();
    });

    elements.deleteSelectedUsersBtn?.addEventListener('click', handleDeleteSelectedUsers);
};

document.addEventListener('DOMContentLoaded', () => {
    bindEvents();
    bindSelectAllEvent();
    loadUsers();
});
