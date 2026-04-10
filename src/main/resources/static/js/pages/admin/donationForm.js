import {donationApi} from '../../apis/donationApi.js';
import {donorApi} from '../../apis/donorApi.js';
import {eventApi} from '../../apis/eventApi.js';
import {activityApi} from '../../apis/activityApi.js';
import {toDateInputValue, toStartOfDayLocalDateTime, todayDateInputValue} from '../../utils/date.js';

const form = document.getElementById('donationForm');
const donorSearchWrapper = document.getElementById('donorSearchWrapper');
const donorSearchInput = document.getElementById('donorSearchInput');
const donorDropdown = document.getElementById('donorDropdown');
const donorDropdownList = document.getElementById('donorDropdownList');
const donorIdInput = document.getElementById('donorId');
const amountInput = document.getElementById('amount');
const donatedAtInput = document.getElementById('donatedAt');
const paymentMethodInput = document.getElementById('paymentMethod');
const messageInput = document.getElementById('message');
const targetNoneCheckbox = document.getElementById('targetNone');
const targetEventCheckbox = document.getElementById('targetEvent');
const targetActivityCheckbox = document.getElementById('targetActivity');
const eventTargetGroup = document.getElementById('eventTargetGroup');
const activityTargetGroup = document.getElementById('activityTargetGroup');
const eventSearchWrapper = document.getElementById('eventSearchWrapper');
const eventSearchInput = document.getElementById('eventSearchInput');
const eventDropdown = document.getElementById('eventDropdown');
const eventDropdownList = document.getElementById('eventDropdownList');
const eventIdInput = document.getElementById('eventId');
const eventSelectedMeta = document.getElementById('eventSelectedMeta');
const activitySearchWrapper = document.getElementById('activitySearchWrapper');
const activitySearchInput = document.getElementById('activitySearchInput');
const activityDropdown = document.getElementById('activityDropdown');
const activityDropdownList = document.getElementById('activityDropdownList');
const activityIdInput = document.getElementById('activityId');
const activitySelectedMeta = document.getElementById('activitySelectedMeta');
const needReceiptCheckbox = document.getElementById('needReceipt');
const receiptFields = document.getElementById('receiptFields');
const receiptNameInput = document.getElementById('receiptName');
const receiptEmailInput = document.getElementById('receiptEmail');
const pageTitle = document.getElementById('donationFormPageTitle');
const pageDescription = document.getElementById('donationFormPageDescription');
const breadcrumbLabel = document.getElementById('donationFormBreadcrumbLabel');
const submitButton = document.getElementById('submitDonation');
const WHOLE_AMOUNT_MESSAGE = 'Chỗ này chưa code huhu, vui lòng nhập tiền chẳn';
let latestEventLookupRequestId = 0;
let latestActivityLookupRequestId = 0;

const donationId = Number(window.__DONATION_ID__ || 0) || null;
const isEditMode = donationId !== null;

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

const prefillDonorContext = {
    id: parseLongOrNull(window.__PREFILL_DONOR_ID__),
    fullName: window.__PREFILL_DONOR_NAME__ || '',
    phone: window.__PREFILL_DONOR_PHONE__ || ''
};
const returnToUrl = window.__DONATION_RETURN_TO__ || '/admin/donations';

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

const setLookupMeta = (element, emptyMessage, selectedId, selectedLabel) => {
    if (!element) return;

    if (!selectedId) {
        element.textContent = emptyMessage;
        return;
    }

    element.textContent = `Đã chọn: #${selectedId} - ${selectedLabel}`;
};

const showDonorDropdown = () => {
    if (!donorDropdown) return;
    donorDropdown.classList.remove('hidden');
};

const hideDonorDropdown = () => {
    if (!donorDropdown) return;
    donorDropdown.classList.add('hidden');
};

const showLookupDropdown = (dropdown) => {
    if (!dropdown) return;
    dropdown.classList.remove('hidden');
};

const hideLookupDropdown = (dropdown) => {
    if (!dropdown) return;
    dropdown.classList.add('hidden');
};

const selectDonor = (donor) => {
    if (!donor) return;
    setDonorIdValue(donor.id);
    if (donorSearchInput) {
        donorSearchInput.value = `${donor.fullName || 'Không rõ tên'} - ${donor.phone || '---'}`;
    }
    hideDonorDropdown();
};

const applyPrefillDonorContext = () => {
    if (isEditMode) return;
    if (!prefillDonorContext.id) return;
    if (donorIdInput?.value) return;

    selectDonor(prefillDonorContext);
};

const resetEventSelection = () => {
    if (eventIdInput) eventIdInput.value = '';
    if (eventSearchInput) eventSearchInput.value = '';
    setLookupMeta(eventSelectedMeta, 'Chưa chọn sự kiện nào.', null, null);
};

const resetActivitySelection = () => {
    if (activityIdInput) activityIdInput.value = '';
    if (activitySearchInput) activitySearchInput.value = '';
    setLookupMeta(activitySelectedMeta, 'Chưa chọn hoạt động nào.', null, null);
};

const selectEvent = (eventItem) => {
    if (!eventItem) return;
    if (eventIdInput) eventIdInput.value = eventItem.id || '';
    if (eventSearchInput) eventSearchInput.value = eventItem.name || '';
    setLookupMeta(eventSelectedMeta, 'Chưa chọn sự kiện nào.', eventItem.id, eventItem.name || 'Không rõ tên');
    hideLookupDropdown(eventDropdown);
};

const selectActivity = (activityItem) => {
    if (!activityItem) return;
    if (activityIdInput) activityIdInput.value = activityItem.id || '';
    if (activitySearchInput) activitySearchInput.value = activityItem.name || '';
    if (eventIdInput && activityItem.eventId) eventIdInput.value = activityItem.eventId;
    if (eventSearchInput && activityItem.eventName) eventSearchInput.value = activityItem.eventName;
    if (activityItem.eventId && activityItem.eventName) {
        setLookupMeta(eventSelectedMeta, 'Chưa chọn sự kiện nào.', activityItem.eventId, activityItem.eventName);
    }
    setLookupMeta(activitySelectedMeta, 'Chưa chọn hoạt động nào.', activityItem.id, activityItem.name || 'Không rõ tên');
    hideLookupDropdown(activityDropdown);
};

const setPageMode = () => {
    const title = isEditMode ? 'Chỉnh sửa quyên góp' : 'Tạo mới quyên góp';
    const description = isEditMode
        ? 'Cập nhật lại thông tin khoản quyên góp nội bộ trước khi tiếp tục xử lý.'
        : 'Tạo nhanh một khoản quyên góp nội bộ từ dữ liệu nhà hảo tâm có sẵn.';
    const buttonLabel = isEditMode ? 'Lưu thay đổi' : 'Tạo đơn quyên góp';

    if (pageTitle) pageTitle.textContent = title;
    if (pageDescription) pageDescription.textContent = description;
    if (breadcrumbLabel) breadcrumbLabel.textContent = title;
    if (submitButton) {
        submitButton.innerHTML = `
            <span class="material-icons text-sm font-bold">save</span>
            ${buttonLabel}
        `;
    }
    document.title = title;
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

const renderEventDropdown = (events) => {
    if (!eventDropdownList) return;

    if (!events || events.length === 0) {
        eventDropdownList.innerHTML = '<div class="px-3 py-3 text-sm text-slate-500">Không tìm thấy sự kiện đang diễn ra phù hợp</div>';
        return;
    }

    eventDropdownList.innerHTML = events.map((eventItem) => `
        <button type="button"
                data-event-id="${eventItem.id}"
                data-event-name="${escapeHtml(eventItem.name || '')}"
                class="grid w-full grid-cols-[minmax(0,1fr)_110px] gap-4 px-3 py-2.5 text-left text-sm hover:bg-slate-50 dark:hover:bg-slate-800 transition-colors">
            <span class="font-medium text-slate-900 dark:text-slate-100 truncate">${escapeHtml(eventItem.name || 'Không rõ tên')}</span>
            <span class="text-slate-600 dark:text-slate-300">#${escapeHtml(eventItem.id)}</span>
        </button>
    `).join('');
};

const renderActivityDropdown = (activities) => {
    if (!activityDropdownList) return;

    if (!activities || activities.length === 0) {
        activityDropdownList.innerHTML = '<div class="px-3 py-3 text-sm text-slate-500">Không tìm thấy hoạt động đang diễn ra phù hợp</div>';
        return;
    }

    activityDropdownList.innerHTML = activities.map((activityItem) => `
        <button type="button"
                data-activity-id="${activityItem.id}"
                data-activity-name="${escapeHtml(activityItem.name || '')}"
                data-activity-event-id="${escapeHtml(activityItem.event?.id || '')}"
                data-activity-event-name="${escapeHtml(activityItem.event?.name || '')}"
                class="grid w-full grid-cols-[minmax(0,1fr)_110px] gap-4 px-3 py-2.5 text-left text-sm hover:bg-slate-50 dark:hover:bg-slate-800 transition-colors">
            <span class="min-w-0">
                <span class="block font-medium text-slate-900 dark:text-slate-100 truncate">${escapeHtml(activityItem.name || 'Không rõ tên')}</span>
                <span class="block text-xs text-slate-500 dark:text-slate-400 truncate">${escapeHtml(activityItem.event?.name || 'Không thuộc sự kiện')}</span>
            </span>
            <span class="text-slate-600 dark:text-slate-300">#${escapeHtml(activityItem.id)}</span>
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

const loadEventsForSelect = async (search = '') => {
    if (!eventDropdownList) return;
    const requestId = ++latestEventLookupRequestId;
    eventDropdownList.innerHTML = '<div class="px-3 py-3 text-sm text-slate-500">Đang tải danh sách sự kiện...</div>';

    try {
        const response = await eventApi.getEvents({
            page: 1,
            size: 20,
            search: search.trim(),
            status: 'ONGOING',
            sortBy: 'name',
            sortDir: 'asc'
        });

        if (requestId !== latestEventLookupRequestId) return;

        const pageData = response?.data || {};
        const events = pageData.data || [];
        renderEventDropdown(events);
        showLookupDropdown(eventDropdown);
    } catch (error) {
        if (requestId !== latestEventLookupRequestId) return;
        console.error('Lỗi tải danh sách sự kiện:', error);
        eventDropdownList.innerHTML = '<div class="px-3 py-3 text-sm text-red-500">Không thể tải danh sách sự kiện</div>';
        showLookupDropdown(eventDropdown);
    }
};

const loadActivitiesForSelect = async (search = '') => {
    if (!activityDropdownList) return;
    const requestId = ++latestActivityLookupRequestId;
    activityDropdownList.innerHTML = '<div class="px-3 py-3 text-sm text-slate-500">Đang tải danh sách hoạt động...</div>';

    try {
        const response = await activityApi.getAllActivities({
            page: 1,
            size: 20,
            search: search.trim(),
            status: 'ONGOING'
        });

        if (requestId !== latestActivityLookupRequestId) return;

        const pageData = response?.data || {};
        const activities = pageData.data || [];
        renderActivityDropdown(activities);
        showLookupDropdown(activityDropdown);
    } catch (error) {
        if (requestId !== latestActivityLookupRequestId) return;
        console.error('Lỗi tải danh sách hoạt động:', error);
        activityDropdownList.innerHTML = '<div class="px-3 py-3 text-sm text-red-500">Không thể tải danh sách hoạt động</div>';
        showLookupDropdown(activityDropdown);
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

    if (!isEvent) {
        resetEventSelection();
        hideLookupDropdown(eventDropdown);
    }
    if (!isActivity) {
        resetActivitySelection();
        hideLookupDropdown(activityDropdown);
    }
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
    if (!Number.isFinite(payload.amount)) {
        alert('Vui lòng nhập số tiền hợp lệ.');
        return false;
    }
    if (!Number.isInteger(payload.amount)) {
        alert(WHOLE_AMOUNT_MESSAGE);
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
    if (!payload.donatedAt) {
        alert('Vui lòng chọn ngày quyên góp.');
        return false;
    }
    if (target === 'event' && !payload.eventId) {
        alert('Vui lòng chọn sự kiện đang diễn ra.');
        return false;
    }
    if (target === 'activity' && !payload.activityId) {
        alert('Vui lòng chọn hoạt động đang diễn ra.');
        return false;
    }
    if (target === 'activity' && !payload.eventId) {
        alert('Hoạt động phải thuộc một sự kiện hợp lệ.');
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

const fillForm = (donation) => {
    if (!donation) return;

    setDonorIdValue(donation.donorId);
    if (donorSearchInput) {
        donorSearchInput.value = `${donation.donorName || 'Không rõ tên'} - ${donation.donorPhone || '---'}`;
    }
    if (amountInput) amountInput.value = donation.amount ?? '';
    if (donatedAtInput) donatedAtInput.value = toDateInputValue(donation.donatedAt) || todayDateInputValue();
    if (paymentMethodInput) paymentMethodInput.value = donation.paymentMethod || 'BANK_TRANSFER_ONLINE';
    if (messageInput) messageInput.value = donation.message || '';

    const needReceipt = donation.needReceipt === true;
    if (needReceiptCheckbox) needReceiptCheckbox.checked = needReceipt;
    toggleReceiptFields();

    if (receiptNameInput) receiptNameInput.value = donation.receiptName || '';
    if (receiptEmailInput) receiptEmailInput.value = donation.receiptEmail || '';

    if (donation.target === 'EVENT' && donation.eventId) {
        activateTarget('event');
        selectEvent({
            id: donation.eventId,
            name: donation.objectName || `Sự kiện #${donation.eventId}`
        });
    } else if (donation.target === 'ACTIVITY' && donation.activityId) {
        activateTarget('activity');
        selectActivity({
            id: donation.activityId,
            name: donation.objectName || `Hoạt động #${donation.activityId}`
        });
    } else {
        activateTarget('none');
    }
};

const loadDonationDetail = async () => {
    if (!isEditMode) return;

    try {
        const response = await donationApi.getDonationById(donationId);
        const donation = response?.data;

        if (!donation) {
            throw new Error('Không tìm thấy thông tin quyên góp.');
        }

        if (donation.donationVia !== 'STAFF') {
            alert('Chỉ có thể chỉnh sửa khoản quyên góp được tạo nội bộ.');
            window.location.href = `/admin/donations/${donationId}`;
            return;
        }
        if (donation.status === 'CONFIRMED') {
            alert('Không thể chỉnh sửa khoản quyên góp đã xác nhận.');
            window.location.href = `/admin/donations/${donationId}`;
            return;
        }

        fillForm(donation);
    } catch (error) {
        console.error('Lỗi tải chi tiết quyên góp:', error);
        alert(error?.message || 'Không thể tải thông tin quyên góp.');
        window.location.href = '/admin/donations';
    }
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
        donatedAt: toStartOfDayLocalDateTime(rawData.donatedAt),
        paymentMethod: rawData.paymentMethod,
        message: rawData.message?.trim() || null,
        needReceipt,
        receiptName: needReceipt ? (rawData.receiptName?.trim() || null) : null,
        receiptEmail: needReceipt ? (rawData.receiptEmail?.trim() || null) : null,
        eventId: target === 'activity'
            ? parseLongOrNull(rawData.eventId)
            : (target === 'event' ? parseLongOrNull(rawData.eventId) : null),
        activityId: target === 'activity' ? parseLongOrNull(rawData.activityId) : null
    };

    if (!validatePayload(payload, target)) return;

    try {
        const response = isEditMode
            ? await donationApi.updateStaffDonation(donationId, payload)
            : await donationApi.createStaffDonation(payload);
        if (response.status === 200) {
            alert(response.message || (isEditMode ? 'Cập nhật đơn quyên góp thành công.' : 'Tạo đơn quyên góp thành công.'));
            window.location.href = isEditMode ? `/admin/donations/${donationId}` : returnToUrl;
        }
    } catch (error) {
        const errorMessage = error?.message || (isEditMode
            ? 'Có lỗi xảy ra khi cập nhật đơn quyên góp.'
            : 'Có lỗi xảy ra khi tạo đơn quyên góp.');
        console.error('Lỗi lưu đơn quyên góp:', errorMessage);
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

const init = async () => {
    if (!form) return;

    setPageMode();
    activateTarget('none');
    toggleReceiptFields();
    bindTargetEvents();

    if (donatedAtInput && !donatedAtInput.value) {
        donatedAtInput.value = todayDateInputValue();
    }

    if (donorSearchInput) {
        donorSearchInput.addEventListener('focus', () => {
            loadDonorsForSelect(donorSearchInput.value || '');
        });

        donorSearchInput.addEventListener('input', debounce((event) => {
            setDonorIdValue('');
            loadDonorsForSelect(event.target.value || '');
        }));
    }

    if (eventSearchInput) {
        eventSearchInput.addEventListener('focus', () => {
            loadEventsForSelect(eventSearchInput.value || '');
        });

        eventSearchInput.addEventListener('input', debounce((event) => {
            if (eventIdInput) eventIdInput.value = '';
            setLookupMeta(eventSelectedMeta, 'Chưa chọn sự kiện nào.', null, null);
            loadEventsForSelect(event.target.value || '');
        }));
    }

    if (activitySearchInput) {
        activitySearchInput.addEventListener('focus', () => {
            loadActivitiesForSelect(activitySearchInput.value || '');
        });

        activitySearchInput.addEventListener('input', debounce((event) => {
            if (activityIdInput) activityIdInput.value = '';
            setLookupMeta(activitySelectedMeta, 'Chưa chọn hoạt động nào.', null, null);
            loadActivitiesForSelect(event.target.value || '');
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

    if (eventDropdownList) {
        eventDropdownList.addEventListener('click', (event) => {
            const optionButton = event.target.closest('[data-event-id]');
            if (!optionButton) return;

            selectEvent({
                id: optionButton.getAttribute('data-event-id'),
                name: optionButton.getAttribute('data-event-name') || optionButton.children[0]?.textContent || ''
            });
        });
    }

    if (activityDropdownList) {
        activityDropdownList.addEventListener('click', (event) => {
            const optionButton = event.target.closest('[data-activity-id]');
            if (!optionButton) return;

            selectActivity({
                id: optionButton.getAttribute('data-activity-id'),
                name: optionButton.getAttribute('data-activity-name') || optionButton.children[0]?.textContent || '',
                eventId: parseLongOrNull(optionButton.getAttribute('data-activity-event-id')),
                eventName: optionButton.getAttribute('data-activity-event-name') || ''
            });
        });
    }

    document.addEventListener('click', (event) => {
        if (donorSearchWrapper && !donorSearchWrapper.contains(event.target)) {
            hideDonorDropdown();
        }
        if (eventSearchWrapper && !eventSearchWrapper.contains(event.target)) {
            hideLookupDropdown(eventDropdown);
        }
        if (activitySearchWrapper && !activitySearchWrapper.contains(event.target)) {
            hideLookupDropdown(activityDropdown);
        }
    });

    if (needReceiptCheckbox) {
        needReceiptCheckbox.addEventListener('change', toggleReceiptFields);
    }

    if (amountInput) {
        amountInput.addEventListener('change', () => {
            const amount = Number(amountInput.value);
            if (!amountInput.value || !Number.isFinite(amount) || Number.isInteger(amount)) return;

            alert(WHOLE_AMOUNT_MESSAGE);
            amountInput.value = '';
            amountInput.focus();
        });
    }

    form.addEventListener('submit', handleSubmit);

    applyPrefillDonorContext();
    await loadDonationDetail();
};

document.addEventListener('DOMContentLoaded', init);
