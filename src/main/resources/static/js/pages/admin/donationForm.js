import {donationApi} from '../../apis/donationApi.js';
import {donorApi} from '../../apis/donorApi.js';

const form = document.getElementById('donationForm');
const donorSearchWrapper = document.getElementById('donorSearchWrapper');
const donorSearchInput = document.getElementById('donorSearchInput');
const donorDropdown = document.getElementById('donorDropdown');
const donorDropdownList = document.getElementById('donorDropdownList');
const donorIdInput = document.getElementById('donorId');
const targetNoneCheckbox = document.getElementById('targetNone');
const targetEventCheckbox = document.getElementById('targetEvent');
const targetActivityCheckbox = document.getElementById('targetActivity');
const eventTargetGroup = document.getElementById('eventTargetGroup');
const activityTargetGroup = document.getElementById('activityTargetGroup');
const eventIdInput = document.getElementById('eventId');
const activityIdInput = document.getElementById('activityId');
const needReceiptCheckbox = document.getElementById('needReceipt');
const receiptFields = document.getElementById('receiptFields');
const receiptNameInput = document.getElementById('receiptName');
const receiptEmailInput = document.getElementById('receiptEmail');

const targetCheckboxes = {
    none: targetNoneCheckbox,
    event: targetEventCheckbox,
    activity: targetActivityCheckbox
};

const parseLongOrNull = (value) => {
    if (value === undefined || value === null || value === '') return null;
    const parsed = Number(value);
    return Number.isNaN(parsed) ? null : parsed;
};

const debounce = (fn, delay = 350) => {
    let timeoutId;
    return (...args) => {
        clearTimeout(timeoutId);
        timeoutId = setTimeout(() => fn(...args), delay);
    };
};

const escapeHtml = (value) => String(value ?? '')
    .replaceAll('&', '&amp;')
    .replaceAll('<', '&lt;')
    .replaceAll('>', '&gt;')
    .replaceAll('"', '&quot;')
    .replaceAll("'", '&#39;');

const setDonorIdValue = (id) => {
    if (!donorIdInput) return;
    donorIdInput.value = id || '';
};

const showDonorDropdown = () => {
    if (!donorDropdown) return;
    donorDropdown.classList.remove('hidden');
};

const hideDonorDropdown = () => {
    if (!donorDropdown) return;
    donorDropdown.classList.add('hidden');
};

const selectDonor = (donor) => {
    if (!donor) return;
    setDonorIdValue(donor.id);
    if (donorSearchInput) {
        donorSearchInput.value = `${donor.fullName || 'Không rõ tên'} - ${donor.phone || '---'}`;
    }
    hideDonorDropdown();
};

const renderDonorDropdown = (donors) => {
    if (!donorDropdownList) return;

    if (!donors || donors.length === 0) {
        donorDropdownList.innerHTML = '<div class="px-3 py-3 text-sm text-slate-500">Không tìm thấy nhà hảo tâm phù hợp</div>';
        return;
    }

    donorDropdownList.innerHTML = donors.map((donor) => `
        <button type="button"
                data-donor-id="${donor.id}"
                class="grid w-full grid-cols-2 gap-4 px-3 py-2.5 text-left text-sm hover:bg-slate-50 dark:hover:bg-slate-800 transition-colors">
            <span class="font-medium text-slate-900 dark:text-slate-100">${escapeHtml(donor.fullName || 'Không rõ tên')}</span>
            <span class="text-slate-600 dark:text-slate-300">${escapeHtml(donor.phone || '---')}</span>
        </button>
    `).join('');
};

const loadDonorsForSelect = async (search = '') => {
    if (!donorDropdownList) return;
    donorDropdownList.innerHTML = '<div class="px-3 py-3 text-sm text-slate-500">Đang tải danh sách nhà hảo tâm...</div>';

    try {
        const response = await donorApi.getAllDonors({
            page: 1,
            size: 20,
            search: search.trim(),
            type: ''
        });
        const pageData = response?.data || {};
        const donors = pageData.data || [];
        renderDonorDropdown(donors);
        showDonorDropdown();
    } catch (error) {
        console.error('Lỗi tải danh sách nhà hảo tâm:', error);
        donorDropdownList.innerHTML = '<div class="px-3 py-3 text-sm text-red-500">Không thể tải danh sách nhà hảo tâm</div>';
        showDonorDropdown();
    }
};

const getSelectedTarget = () => {
    if (targetEventCheckbox?.checked) return 'event';
    if (targetActivityCheckbox?.checked) return 'activity';
    return 'none';
};

const activateTarget = (target) => {
    Object.entries(targetCheckboxes).forEach(([key, checkbox]) => {
        if (!checkbox) return;
        checkbox.checked = key === target;
    });

    const isEvent = target === 'event';
    const isActivity = target === 'activity';

    if (eventTargetGroup) eventTargetGroup.classList.toggle('hidden', !isEvent);
    if (activityTargetGroup) activityTargetGroup.classList.toggle('hidden', !isActivity);

    if (!isEvent && eventIdInput) eventIdInput.value = '';
    if (!isActivity && activityIdInput) activityIdInput.value = '';
};

const toggleReceiptFields = () => {
    const show = needReceiptCheckbox?.checked === true;
    if (receiptFields) receiptFields.classList.toggle('hidden', !show);
    if (!show) {
        if (receiptNameInput) receiptNameInput.value = '';
        if (receiptEmailInput) receiptEmailInput.value = '';
    }
};

const validatePayload = (payload, target) => {
    if (!payload.donorId || payload.donorId < 1) {
        alert('Vui lòng chọn nhà hảo tâm hợp lệ.');
        return false;
    }
    if (!payload.amount || payload.amount < 1000) {
        alert('Số tiền tối thiểu là 1.000 đồng.');
        return false;
    }
    if (!payload.paymentMethod) {
        alert('Vui lòng chọn phương thức thanh toán.');
        return false;
    }
    if (target === 'event' && !payload.eventId) {
        alert('Vui lòng nhập ID sự kiện.');
        return false;
    }
    if (target === 'activity' && !payload.activityId) {
        alert('Vui lòng nhập ID hoạt động.');
        return false;
    }
    if (payload.needReceipt) {
        if (!payload.receiptName?.trim()) {
            alert('Vui lòng nhập tên trên biên lai.');
            return false;
        }
        if (!payload.receiptEmail?.trim()) {
            alert('Vui lòng nhập email nhận biên lai.');
            return false;
        }
    }
    return true;
};

const handleSubmit = async (event) => {
    event.preventDefault();

    const formData = new FormData(form);
    const rawData = Object.fromEntries(formData.entries());
    const target = getSelectedTarget();
    const needReceipt = needReceiptCheckbox?.checked === true;

    const payload = {
        donorId: parseLongOrNull(rawData.donorId),
        amount: Number(rawData.amount),
        paymentMethod: rawData.paymentMethod,
        message: rawData.message?.trim() || null,
        needReceipt,
        receiptName: needReceipt ? (rawData.receiptName?.trim() || null) : null,
        receiptEmail: needReceipt ? (rawData.receiptEmail?.trim() || null) : null,
        eventId: target === 'event' ? parseLongOrNull(rawData.eventId) : null,
        activityId: target === 'activity' ? parseLongOrNull(rawData.activityId) : null
    };

    if (!validatePayload(payload, target)) return;

    try {
        const response = await donationApi.createStaffDonation(payload);
        if (response.status === 200) {
            alert(response.message || 'Tạo đơn từ thiện thành công từ staff');
            window.location.href = '/admin/donations';
        }
    } catch (error) {
        const errorMessage = error?.message || 'Có lỗi xảy ra khi tạo đơn quyên góp.';
        console.error('Lỗi tạo đơn quyên góp:', errorMessage);
        alert(errorMessage);
    }
};

const bindTargetEvents = () => {
    Object.entries(targetCheckboxes).forEach(([key, checkbox]) => {
        if (!checkbox) return;
        checkbox.addEventListener('change', () => {
            if (checkbox.checked) {
                activateTarget(key);
            } else {
                activateTarget('none');
            }
        });
    });
};

const init = () => {
    if (!form) return;
    activateTarget('none');
    toggleReceiptFields();
    bindTargetEvents();

    if (donorSearchInput) {
        donorSearchInput.addEventListener('focus', () => {
            loadDonorsForSelect(donorSearchInput.value || '');
        });

        donorSearchInput.addEventListener('input', debounce((event) => {
            setDonorIdValue('');
            loadDonorsForSelect(event.target.value || '');
        }));
    }

    if (donorDropdownList) {
        donorDropdownList.addEventListener('click', (event) => {
            const optionButton = event.target.closest('[data-donor-id]');
            if (!optionButton) return;

            const donor = {
                id: optionButton.getAttribute('data-donor-id'),
                fullName: optionButton.children[0]?.textContent || '',
                phone: optionButton.children[1]?.textContent || ''
            };
            selectDonor(donor);
        });
    }

    document.addEventListener('click', (event) => {
        if (!donorSearchWrapper) return;
        if (!donorSearchWrapper.contains(event.target)) {
            hideDonorDropdown();
        }
    });

    if (needReceiptCheckbox) {
        needReceiptCheckbox.addEventListener('change', toggleReceiptFields);
    }

    form.addEventListener('submit', handleSubmit);
};

document.addEventListener('DOMContentLoaded', init);
