import {userApi} from '../../apis/userApi.js';
import {renderPagination} from '../../components/pagination.js';

const state = {
    page: 1,
    size: 10,
    search: '',
    status: '',
    sortBy: 'id',
    sortDir: 'desc'
};

const elements = {
    tableBody: document.getElementById('userTableBody'),
    paginationContainer: document.getElementById('paginationContainer'),
    searchInput: document.getElementById('userSearchInput'),
    statusFilter: document.getElementById('userStatusFilter'),
    resetFilterBtn: document.getElementById('userResetFilterBtn')
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
        return;
    }

    elements.tableBody.innerHTML = users.map((user) => `
        <tr class="border-b border-border-light hover:bg-slate-50 transition-colors">
            <td class="px-6 py-5">
                <input type="checkbox" class="h-6 w-6 rounded-md border-slate-300 text-primary focus:ring-primary/30"/>
            </td>
            <td class="px-6 py-5 text-lg font-semibold text-slate-900">${user.fullName || '---'}</td>
            <td class="px-6 py-5 text-base text-slate-500">${user.phone || '---'}</td>
            <td class="px-6 py-5 text-base text-slate-500">${user.email || '---'}</td>
            <td class="px-6 py-5">${renderRoleBadge(user.role)}</td>
            <td class="px-6 py-5">${renderStatusBadge(user.status)}</td>
        </tr>
    `).join('');
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
};

document.addEventListener('DOMContentLoaded', () => {
    bindEvents();
    loadUsers();
});
