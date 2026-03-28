import {donorApi} from '../../apis/donorApi.js';
import {renderPagination} from '../../components/pagination.js';
import {bindExcelActions} from '../../utils/excelTransfer.js';
const canManageDonors = window.__CAN_MANAGE_DONORS__ === true;

const state = {
    page: 1,
    size: 50,
    search: '',
    type: '',
    sortBy: 'id',
    sortDir: 'desc'
};

const elements = {
    tableBody: document.getElementById('donorTableBody'),
    paginationContainer: document.getElementById('paginationContainer'),
    searchInput: document.getElementById('donorSearchInput'),
    typeFilter: document.getElementById('donorTypeFilter'),
    sortButtons: document.querySelectorAll('[data-donor-sort]'),
    exportBtn: document.getElementById('donorExportBtn'),
    importBtn: document.getElementById('donorImportBtn'),
    importInput: document.getElementById('donorImportInput')
};

const getDefaultSortDirection = (field) => {
    if (['createdAt', 'numberOfDonations', 'totalDonationAmount'].includes(field)) {
        return 'desc';
    }

    return 'asc';
};

const getSortIcon = (field) => {
    if (state.sortBy !== field) return 'unfold_more';
    return state.sortDir === 'asc' ? 'arrow_upward' : 'arrow_downward';
};

const updateSortIndicators = () => {
    elements.sortButtons.forEach((button) => {
        const field = button.dataset.donorSort;
        const icon = button.querySelector('[data-sort-icon]');
        const isActive = state.sortBy === field;

        button.classList.toggle('text-primary', isActive);
        button.classList.toggle('font-bold', isActive);

        if (icon) {
            icon.textContent = getSortIcon(field);
        }
    });
};

// 1. Hàm helper lấy chữ cái đầu của tên (Avatar cá nhân)
const getInitials = (name) => {
    return name ? name.split(' ').map(n => n[0]).join('').toUpperCase().substring(0, 2) : 'NA';
};

// 2. Hàm helper định dạng tiền tệ
const formatCurrency = (amount) => {
    return new Intl.NumberFormat('vi-VN').format(amount || 0) + ' ₫';
};

// 3. Render Badge cho loại nhà hảo tâm
const getTypeBadge = (type) => {
    const isOrg = type === 'ORGANIZATION';
    const config = isOrg
        ? {text: 'Tổ chức', class: 'bg-emerald-100 text-emerald-800 dark:bg-emerald-900/30 dark:text-emerald-300'}
        : {text: 'Cá nhân', class: 'bg-blue-100 text-blue-800 dark:bg-blue-900/30 dark:text-blue-300'};

    return `<span class="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${config.class}">${config.text}</span>`;
};

// 4. Hàm Render Row
const renderDonorRow = (donor) => {
    const isOrg = donor.type === 'ORGANIZATION';
    const orgInfo = donor.organization;
    const joinDate = donor.createdAt ? new Date(donor.createdAt).toLocaleDateString('vi-VN') : '---';

    // Avatar Logic: Nếu là tổ chức hiện icon tòa nhà, cá nhân hiện chữ cái đầu
    const avatarHtml = isOrg
        ? `<div class="h-10 w-10 rounded-full bg-emerald-100 dark:bg-emerald-900/30 text-emerald-600 dark:text-emerald-400 flex items-center justify-center font-bold">
             <span class="material-symbols-outlined text-[20px]">apartment</span>
           </div>`
        : `<div class="h-10 w-10 rounded-full bg-indigo-100 dark:bg-indigo-900/30 text-indigo-600 dark:text-indigo-400 flex items-center justify-center font-bold text-sm">
             ${getInitials(donor.fullName)}
           </div>`;

    return `
    <tr class="hover:bg-slate-50 dark:hover:bg-white/5 transition-colors group">
        <td class="px-6 py-4 whitespace-nowrap">
            <div class="flex items-center">
                <div class="h-10 w-10 shrink-0">${avatarHtml}</div>
                    <div class="ml-4">
                        ${isOrg && orgInfo ? `
                            <div class="text-sm font-semibold text-text-main dark:text-white">
                                ${orgInfo.name}
                            </div>
                            <div class="text-xs text-text-secondary mt-0.5 flex items-center">
                                <span class="material-symbols-outlined text-[12px] mr-1">person_pin</span>
                                Đại diện: ${orgInfo.representative || '---'}
                            </div>
                    ` : `<div class="text-sm font-semibold text-text-main dark:text-white">${donor.fullName}</div>`}
                    </div>
            </div>
        </td>
        <td class="px-6 py-4 whitespace-nowrap">
            ${getTypeBadge(donor.type)}
        </td>
        <td class="px-6 py-4 whitespace-nowrap">
            <div class="flex flex-col gap-1">
                <div class="flex items-center text-sm text-text-main dark:text-slate-200">
                    <span class="material-symbols-outlined text-[16px] mr-1.5 text-slate-400">call</span>
                    ${donor.phone || '---'}
                </div>
                <div class="flex items-center text-sm text-text-secondary">
                    <span class="material-symbols-outlined text-[16px] mr-1.5 text-slate-400">mail</span>
                    ${donor.email || '---'}
                </div>
            </div>
        </td>
        <td class="px-6 py-4 whitespace-nowrap text-sm text-text-secondary">
            ${joinDate}
        </td>
        <td class="px-6 py-4 whitespace-nowrap text-sm text-text-main dark:text-slate-200 text-center">
            ${donor.numberOfDonations || 0}
        </td>
        <td class="px-6 py-4 whitespace-nowrap text-sm text-right font-bold text-text-main dark:text-white">
            ${formatCurrency(donor.totalDonationAmount)}
        </td>
        <td class="px-6 py-4 whitespace-nowrap text-center text-sm font-medium sticky right-0 bg-surface-light dark:bg-surface-dark group-hover:bg-slate-50 dark:group-hover:bg-white/5 shadow-[-10px_0_15px_-10px_rgba(0,0,0,0.1)] dark:shadow-[-10px_0_15px_-10px_rgba(0,0,0,0.45)] transition-colors">
            <div class="flex items-center justify-center gap-2">
                <button onclick="viewDonorProfile(${donor.id})" class="text-slate-400 hover:text-primary p-1 rounded-md hover:bg-slate-100 dark:hover:bg-slate-700 transition-colors" title="Xem hồ sơ">
                    <span class="material-symbols-outlined text-[20px]">visibility</span>
                </button>
                ${canManageDonors ? `
                    <button onclick="editDonor(${donor.id})" class="text-slate-400 hover:text-blue-500 p-1 rounded-md hover:bg-slate-100 dark:hover:bg-slate-700 transition-colors" title="Chỉnh sửa">
                        <span class="material-symbols-outlined text-[20px]">edit</span>
                    </button>
                ` : ''}
                <button onclick="viewDonationHistory(${donor.id})" class="text-slate-400 hover:text-orange-500 p-1 rounded-md hover:bg-slate-100 dark:hover:bg-slate-700 transition-colors" title="Lịch sử quyên góp">
                    <span class="material-symbols-outlined text-[20px]">history</span>
                </button>
            </div>
        </td>
    </tr>`;
};

// 5. Hàm Load dữ liệu
const loadDonors = async () => {
    try {
        const response = await donorApi.getAllDonors(state);

        const pageData = response.data;
        const donors = pageData.data || [];

        if (donors.length === 0) {
            elements.tableBody.innerHTML = `<tr><td colspan="7" class="px-6 py-10 text-center text-text-secondary">Không tìm thấy nhà hảo tâm nào</td></tr>`;
        } else {
            elements.tableBody.innerHTML = donors.map(d => renderDonorRow(d)).join('');
        }

        renderPagination(pageData, elements.paginationContainer, (newPage) => {
            state.page = newPage;
            loadDonors();
        });
    } catch (error) {
        console.error("Error loading donors:", error);
    }
};

const debounce = (fn, delay = 350) => {
    let timeoutId;
    return (...args) => {
        clearTimeout(timeoutId);
        timeoutId = setTimeout(() => fn(...args), delay);
    };
};

const bindFilters = () => {
    if (elements.searchInput) {
        elements.searchInput.addEventListener('input', debounce((event) => {
            state.search = event.target.value.trim();
            state.page = 1;
            loadDonors();
        }));
    }

    if (elements.typeFilter) {
        elements.typeFilter.addEventListener('change', (event) => {
            state.type = event.target.value;
            state.page = 1;
            loadDonors();
        });
    }

    elements.sortButtons.forEach((button) => {
        button.addEventListener('click', () => {
            const field = button.dataset.donorSort;

            if (state.sortBy === field) {
                state.sortDir = state.sortDir === 'asc' ? 'desc' : 'asc';
            } else {
                state.sortBy = field;
                state.sortDir = getDefaultSortDirection(field);
            }

            state.page = 1;
            updateSortIndicators();
            loadDonors();
        });
    });
};

// Khởi chạy
document.addEventListener('DOMContentLoaded', () => {
    bindFilters();
    updateSortIndicators();
    bindExcelActions({
        exportButton: elements.exportBtn,
        importButton: elements.importBtn,
        importInput: elements.importInput,
        exportUrl: '/api/admin/excel/donors/export',
        importUrl: '/api/admin/excel/donors/import',
        getExportParams: () => ({
            search: state.search,
            type: state.type,
            sortBy: state.sortBy,
            sortDir: state.sortDir
        }),
        fallbackFilename: 'nha-hao-tam.xlsx',
        successExportMessage: 'Xuất Excel nhà hảo tâm thành công.',
        onImportSuccess: () => {
            state.page = 1;
            loadDonors();
        }
    });
    loadDonors();
});

// Gắn các hàm hành động vào window để HTML onclick gọi được
window.viewDonorProfile = (id) => {
    window.location.href = `/admin/donors/${id}`;
};
window.editDonor = (id) => {
    window.location.href = `/admin/donors/${id}/form`;
};
window.viewDonationHistory = (id) => console.log('Lịch sử:', id);
