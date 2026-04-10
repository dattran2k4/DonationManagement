import {auditLogApi} from "../../apis/auditLogApi.js";
import {donationApi} from "../../apis/donationApi.js";
import {donorApi} from "../../apis/donorApi.js";
import {eventApi} from "../../apis/eventApi.js";
import {activityApi} from "../../apis/activityApi.js";
import {renderPagination} from "../../components/pagination.js";
import {formatVnd, parseVndInput} from "../../utils/currency.js";
import {toDateInputValue, toStartOfDayLocalDateTime, todayDateInputValue} from "../../utils/date.js";

const state = {
    donationId: null,
    activeTab: "info",
    auditLogs: {page: 1, size: 10, loaded: false}
};

const elements = {
    refreshBtn: document.getElementById("refreshBtn"),
    saveDonationBtn: document.getElementById("saveDonationBtn"),
    submitApprovalBtn: document.getElementById("submitApprovalBtn"),
    approveBtn: document.getElementById("approveBtn"),
    rejectBtn: document.getElementById("rejectBtn"),
    paymentMethodSelect: document.getElementById("donationPaymentMethodSelect"),
    editDonorId: document.getElementById("editDonorId"),
    editDonorSearchWrapper: document.getElementById("editDonorSearchWrapper"),
    editDonorSearchInput: document.getElementById("editDonorSearchInput"),
    editDonorDropdown: document.getElementById("editDonorDropdown"),
    editDonorDropdownList: document.getElementById("editDonorDropdownList"),
    editAmount: document.getElementById("editAmount"),
    editDonatedAt: document.getElementById("editDonatedAt"),
    editEventId: document.getElementById("editEventId"),
    editEventSearchWrapper: document.getElementById("editEventSearchWrapper"),
    editEventSearchInput: document.getElementById("editEventSearchInput"),
    editEventDropdown: document.getElementById("editEventDropdown"),
    editEventDropdownList: document.getElementById("editEventDropdownList"),
    editActivityId: document.getElementById("editActivityId"),
    editActivitySearchWrapper: document.getElementById("editActivitySearchWrapper"),
    editActivitySearchInput: document.getElementById("editActivitySearchInput"),
    editActivityDropdown: document.getElementById("editActivityDropdown"),
    editActivityDropdownList: document.getElementById("editActivityDropdownList"),
    editNeedReceipt: document.getElementById("editNeedReceipt"),
    editReceiptFields: document.getElementById("editReceiptFields"),
    editReceiptName: document.getElementById("editReceiptName"),
    editReceiptEmail: document.getElementById("editReceiptEmail"),
    editMessage: document.getElementById("editMessage"),
    editMemoCode: document.getElementById("editMemoCode"),
    editDonorPhoneText: document.getElementById("editDonorPhoneText"),
    editDonorEmailText: document.getElementById("editDonorEmailText"),
    resetTargetBtn: document.getElementById("resetTargetBtn"),
    section: document.getElementById("donationDetailTabsSection"),
    infoBtn: document.getElementById("tabInfoBtn"),
    auditLogsBtn: document.getElementById("tabAuditLogsBtn"),
    infoPanel: document.getElementById("tabInfoPanel"),
    auditLogsPanel: document.getElementById("tabAuditLogsPanel"),
    auditLogsCount: document.getElementById("tabAuditLogsCount"),
    auditLogsTableBody: document.getElementById("donationAuditLogsTableBody"),
    auditLogsPagination: document.getElementById("donationAuditLogsPagination")
};

const auditActionLabels = {
    CREATE: "Tạo mới",
    UPDATE: "Cập nhật",
    STATUS_CHANGE: "Đổi trạng thái",
    DELETE: "Xóa"
};

const debounce = (fn, delay = 350) => {
    let timeoutId;
    return (...args) => {
        clearTimeout(timeoutId);
        timeoutId = setTimeout(() => fn(...args), delay);
    };
};

const escapeHtml = (value) => String(value ?? "")
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#39;");

const formatDateTime = (value) => {
    if (!value) return "---";
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) return value;
    return date.toLocaleString("vi-VN", {
        hour12: false,
        year: "numeric",
        month: "2-digit",
        day: "2-digit",
        hour: "2-digit",
        minute: "2-digit"
    });
};

const parseLongOrNull = (value) => {
    if (value === undefined || value === null || value === "") return null;
    const parsed = Number(value);
    return Number.isNaN(parsed) ? null : parsed;
};

const setFormattedAmountValue = (inputEl, value) => {
    if (!inputEl) return;
    inputEl.value = formatVnd(parseVndInput(value));
};

const setDonorReadonlyInfo = (phone, email) => {
    if (elements.editDonorPhoneText) {
        elements.editDonorPhoneText.textContent = phone && String(phone).trim() ? phone : "---";
    }
    if (elements.editDonorEmailText) {
        elements.editDonorEmailText.textContent = email && String(email).trim() ? email : "---";
    }
};

const toggleReceiptFields = () => {
    if (!elements.editNeedReceipt) return;
    const checked = elements.editNeedReceipt.checked === true;
    const locked = elements.editNeedReceipt.disabled === true;
    const canWrite = !locked;

    if (elements.editReceiptFields) {
        elements.editReceiptFields.classList.toggle("hidden", !checked);
    }

    if (elements.editReceiptName) {
        elements.editReceiptName.disabled = !canWrite || !checked;
        elements.editReceiptName.required = checked;
        if (!checked) elements.editReceiptName.value = "";
    }
    if (elements.editReceiptEmail) {
        elements.editReceiptEmail.disabled = !canWrite || !checked;
        elements.editReceiptEmail.required = checked;
        if (!checked) elements.editReceiptEmail.value = "";
    }
};

const showDropdown = (el) => el?.classList.remove("hidden");
const hideDropdown = (el) => el?.classList.add("hidden");

function renderDonorDropdown(donors) {
    if (!elements.editDonorDropdownList) return;
    if (!donors || donors.length === 0) {
        elements.editDonorDropdownList.innerHTML = '<div class="px-3 py-3 text-sm text-slate-500">Không tìm thấy nhà hảo tâm phù hợp</div>';
        return;
    }
    elements.editDonorDropdownList.innerHTML = donors.map((donor) => `
        <button type="button" data-donor-id="${donor.id}" data-donor-name="${escapeHtml(donor.fullName || "")}" data-donor-phone="${escapeHtml(donor.phone || "")}" data-donor-email="${escapeHtml(donor.email || "")}"
                class="grid w-full grid-cols-2 gap-4 px-3 py-2.5 text-left text-sm hover:bg-slate-50 transition-colors">
            <span class="font-medium text-slate-900">${escapeHtml(donor.fullName || "Không rõ tên")}</span>
            <span class="text-slate-600">${escapeHtml(donor.phone || "---")}</span>
        </button>
    `).join("");
}

function renderEventDropdown(events) {
    if (!elements.editEventDropdownList) return;
    if (!events || events.length === 0) {
        elements.editEventDropdownList.innerHTML = '<div class="px-3 py-3 text-sm text-slate-500">Không tìm thấy sự kiện phù hợp</div>';
        return;
    }
    elements.editEventDropdownList.innerHTML = events.map((eventItem) => `
        <button type="button" data-event-id="${eventItem.id}" data-event-name="${escapeHtml(eventItem.name || "")}"
                class="grid w-full grid-cols-[minmax(0,1fr)_110px] gap-4 px-3 py-2.5 text-left text-sm hover:bg-slate-50 transition-colors">
            <span class="font-medium text-slate-900 truncate">${escapeHtml(eventItem.name || "Không rõ tên")}</span>
            <span class="text-slate-600">#${escapeHtml(eventItem.id)}</span>
        </button>
    `).join("");
}

function renderActivityDropdown(activities) {
    if (!elements.editActivityDropdownList) return;
    if (!activities || activities.length === 0) {
        elements.editActivityDropdownList.innerHTML = '<div class="px-3 py-3 text-sm text-slate-500">Không tìm thấy hoạt động phù hợp</div>';
        return;
    }
    elements.editActivityDropdownList.innerHTML = activities.map((activity) => `
        <button type="button"
                data-activity-id="${activity.id}"
                data-activity-name="${escapeHtml(activity.name || "")}"
                data-activity-event-id="${escapeHtml(activity.event?.id || "")}"
                data-activity-event-name="${escapeHtml(activity.event?.name || "")}"
                class="grid w-full grid-cols-[minmax(0,1fr)_110px] gap-4 px-3 py-2.5 text-left text-sm hover:bg-slate-50 transition-colors">
            <span class="min-w-0">
                <span class="block font-medium text-slate-900 truncate">${escapeHtml(activity.name || "Không rõ tên")}</span>
                <span class="block text-xs text-slate-500 truncate">${escapeHtml(activity.event?.name || "Không thuộc sự kiện")}</span>
            </span>
            <span class="text-slate-600">#${escapeHtml(activity.id)}</span>
        </button>
    `).join("");
}

async function loadDonors(search = "") {
    if (!elements.editDonorDropdownList) return;
    elements.editDonorDropdownList.innerHTML = '<div class="px-3 py-3 text-sm text-slate-500">Đang tải danh sách nhà hảo tâm...</div>';
    try {
        const response = await donorApi.getAllDonors({page: 1, size: 20, search: search.trim(), type: ""});
        renderDonorDropdown(response?.data?.data || []);
        showDropdown(elements.editDonorDropdown);
    } catch (error) {
        elements.editDonorDropdownList.innerHTML = '<div class="px-3 py-3 text-sm text-red-500">Không thể tải danh sách nhà hảo tâm</div>';
        showDropdown(elements.editDonorDropdown);
    }
}

async function loadEvents(search = "") {
    if (!elements.editEventDropdownList) return;
    elements.editEventDropdownList.innerHTML = '<div class="px-3 py-3 text-sm text-slate-500">Đang tải danh sách sự kiện...</div>';
    try {
        const response = await eventApi.getEvents({page: 1, size: 20, search: search.trim(), sortBy: "name", sortDir: "asc"});
        renderEventDropdown(response?.data?.data || []);
        showDropdown(elements.editEventDropdown);
    } catch (error) {
        elements.editEventDropdownList.innerHTML = '<div class="px-3 py-3 text-sm text-red-500">Không thể tải danh sách sự kiện</div>';
        showDropdown(elements.editEventDropdown);
    }
}

async function loadActivities(search = "") {
    if (!elements.editActivityDropdownList) return;
    elements.editActivityDropdownList.innerHTML = '<div class="px-3 py-3 text-sm text-slate-500">Đang tải danh sách hoạt động...</div>';
    try {
        const response = await activityApi.getAllActivities({page: 1, size: 20, search: search.trim()});
        renderActivityDropdown(response?.data?.data || []);
        showDropdown(elements.editActivityDropdown);
    } catch (error) {
        elements.editActivityDropdownList.innerHTML = '<div class="px-3 py-3 text-sm text-red-500">Không thể tải danh sách hoạt động</div>';
        showDropdown(elements.editActivityDropdown);
    }
}

function bindLookupEvents() {
    if (elements.editDonorSearchInput && !elements.editDonorSearchInput.disabled) {
        elements.editDonorSearchInput.addEventListener("focus", () => loadDonors(elements.editDonorSearchInput.value || ""));
        elements.editDonorSearchInput.addEventListener("input", debounce((e) => {
            if (elements.editDonorId) elements.editDonorId.value = "";
            loadDonors(e.target.value || "");
        }));
    }

    if (elements.editEventSearchInput && !elements.editEventSearchInput.disabled) {
        elements.editEventSearchInput.addEventListener("focus", () => loadEvents(elements.editEventSearchInput.value || ""));
        elements.editEventSearchInput.addEventListener("input", debounce((e) => {
            if (elements.editEventId) elements.editEventId.value = "";
            loadEvents(e.target.value || "");
        }));
    }

    if (elements.editActivitySearchInput && !elements.editActivitySearchInput.disabled) {
        elements.editActivitySearchInput.addEventListener("focus", () => loadActivities(elements.editActivitySearchInput.value || ""));
        elements.editActivitySearchInput.addEventListener("input", debounce((e) => {
            if (elements.editActivityId) elements.editActivityId.value = "";
            loadActivities(e.target.value || "");
        }));
    }

    elements.editDonorDropdownList?.addEventListener("click", (event) => {
        const button = event.target.closest("[data-donor-id]");
        if (!button) return;
        if (elements.editDonorId) elements.editDonorId.value = button.getAttribute("data-donor-id") || "";
        if (elements.editDonorSearchInput) {
            const name = button.getAttribute("data-donor-name") || "";
            const phone = button.getAttribute("data-donor-phone") || "";
            elements.editDonorSearchInput.value = phone ? `${name} - ${phone}` : name;
            setDonorReadonlyInfo(phone, button.getAttribute("data-donor-email") || "");
        }
        hideDropdown(elements.editDonorDropdown);
    });

    elements.editEventDropdownList?.addEventListener("click", (event) => {
        const button = event.target.closest("[data-event-id]");
        if (!button) return;
        if (elements.editEventId) elements.editEventId.value = button.getAttribute("data-event-id") || "";
        if (elements.editEventSearchInput) elements.editEventSearchInput.value = button.getAttribute("data-event-name") || "";
        hideDropdown(elements.editEventDropdown);
    });

    elements.editActivityDropdownList?.addEventListener("click", (event) => {
        const button = event.target.closest("[data-activity-id]");
        if (!button) return;
        if (elements.editActivityId) elements.editActivityId.value = button.getAttribute("data-activity-id") || "";
        if (elements.editActivitySearchInput) elements.editActivitySearchInput.value = button.getAttribute("data-activity-name") || "";
        const parentEventId = button.getAttribute("data-activity-event-id") || "";
        const parentEventName = button.getAttribute("data-activity-event-name") || "";
        if (elements.editEventId && parentEventId) elements.editEventId.value = parentEventId;
        if (elements.editEventSearchInput && parentEventName) elements.editEventSearchInput.value = parentEventName;
        hideDropdown(elements.editActivityDropdown);
    });

    document.addEventListener("click", (event) => {
        if (elements.editDonorSearchWrapper && !elements.editDonorSearchWrapper.contains(event.target)) {
            hideDropdown(elements.editDonorDropdown);
        }
        if (elements.editEventSearchWrapper && !elements.editEventSearchWrapper.contains(event.target)) {
            hideDropdown(elements.editEventDropdown);
        }
        if (elements.editActivitySearchWrapper && !elements.editActivitySearchWrapper.contains(event.target)) {
            hideDropdown(elements.editActivityDropdown);
        }
    });

    elements.resetTargetBtn?.addEventListener("click", () => {
        if (elements.editEventId) elements.editEventId.value = "";
        if (elements.editEventSearchInput) elements.editEventSearchInput.value = "";
        if (elements.editActivityId) elements.editActivityId.value = "";
        if (elements.editActivitySearchInput) elements.editActivitySearchInput.value = "";
    });
}

const buildUpdatePayload = () => ({
    donorId: parseLongOrNull(elements.editDonorId?.value ?? window.__DONATION_DONOR_ID__),
    amount: parseVndInput(elements.editAmount?.value ?? window.__DONATION_AMOUNT__ ?? 0),
    donatedAt: toStartOfDayLocalDateTime(elements.editDonatedAt?.value),
    message: (() => {
        const value = elements.editMessage?.value;
        if (value == null) return null;
        const normalized = value.trim();
        return normalized.length > 0 ? normalized : null;
    })(),
    needReceipt: elements.editNeedReceipt?.checked === true,
    receiptName: elements.editNeedReceipt?.checked === true ? (elements.editReceiptName?.value?.trim() || null) : null,
    receiptEmail: elements.editNeedReceipt?.checked === true ? (elements.editReceiptEmail?.value?.trim() || null) : null,
    memoCode: elements.editMemoCode?.value?.trim() || null,
    paymentMethod: elements.paymentMethodSelect?.value || null,
    eventId: parseLongOrNull(elements.editEventId?.value),
    activityId: parseLongOrNull(elements.editActivityId?.value)
});

const validatePayload = (payload) => {
    if (!payload.donorId || payload.donorId < 1) {
        alert("ID nhà hảo tâm không hợp lệ.");
        return false;
    }
    if (!Number.isFinite(payload.amount) || payload.amount < 1000) {
        alert("Số tiền tối thiểu là 1.000 đồng.");
        return false;
    }
    if (!Number.isInteger(payload.amount)) {
        alert("Vui lòng nhập số tiền nguyên.");
        return false;
    }
    if (!payload.donatedAt) {
        alert("Vui lòng chọn ngày quyên góp.");
        return false;
    }
    if (payload.needReceipt) {
        if (!payload.receiptName?.trim()) {
            alert("Vui lòng nhập tên trên biên lai.");
            return false;
        }
        if (!payload.receiptEmail?.trim()) {
            alert("Vui lòng nhập email nhận biên lai.");
            return false;
        }
    }
    return true;
};

async function saveDonationDetail(successMessage = "Cập nhật khoản quyên góp thành công") {
    const payload = buildUpdatePayload();
    if (!validatePayload(payload)) return false;

    try {
        const response = state.donationId
            ? await donationApi.updateStaffDonation(state.donationId, payload)
            : await donationApi.createStaffDonation(payload);
        if (response?.status !== 200) {
            throw new Error(response?.message || (state.donationId ? "Không thể cập nhật khoản quyên góp" : "Không thể tạo khoản quyên góp"));
        }
        alert(successMessage);
        if (!state.donationId) {
            const createdId = Number(response?.data || 0);
            if (createdId) {
                window.location.href = `/admin/donations/${createdId}?saved=1`;
                return true;
            }
            window.location.href = "/admin/donations";
            return true;
        }
        return true;
    } catch (error) {
        alert(error.message || (state.donationId ? "Không thể cập nhật khoản quyên góp" : "Không thể tạo khoản quyên góp"));
        return false;
    }
}

function setActiveTab(tab) {
    if (tab === "auditLogs" && (!elements.auditLogsBtn || !elements.auditLogsPanel)) {
        tab = "info";
    }

    state.activeTab = tab;
    const isInfo = tab === "info";
    const isAuditLogs = tab === "auditLogs";

    if (elements.infoPanel) {
        elements.infoPanel.classList.toggle("hidden", !isInfo);
        elements.infoPanel.style.display = isInfo ? "" : "none";
    }
    if (elements.auditLogsPanel) {
        elements.auditLogsPanel.classList.toggle("hidden", !isAuditLogs);
        elements.auditLogsPanel.style.display = isAuditLogs ? "" : "none";
    }

    if (elements.infoBtn) {
        elements.infoBtn.className = isInfo
            ? "inline-flex items-center border-b-2 border-primary px-4 py-2 text-sm font-semibold text-primary"
            : "inline-flex items-center border-b-2 border-transparent px-4 py-2 text-sm font-semibold text-slate-600 transition hover:text-slate-900 dark:text-slate-300 dark:hover:text-white";
    }
    if (elements.auditLogsBtn) {
        elements.auditLogsBtn.className = isAuditLogs
            ? "inline-flex items-center gap-2 border-b-2 border-primary px-4 py-2 text-sm font-semibold text-primary"
            : "inline-flex items-center gap-2 border-b-2 border-transparent px-4 py-2 text-sm font-semibold text-slate-600 transition hover:text-slate-900 dark:text-slate-300 dark:hover:text-white";
    }

    if (elements.auditLogsCount) {
        elements.auditLogsCount.className = isAuditLogs
            ? "inline-flex h-6 min-w-6 items-center justify-center rounded-full bg-primary/15 px-2 text-xs font-semibold text-primary"
            : "inline-flex h-6 min-w-6 items-center justify-center rounded-full bg-slate-200 px-2 text-xs font-semibold text-slate-700 dark:bg-slate-700 dark:text-slate-100";
    }
}

function getAuditActionBadge(action) {
    const styles = {
        CREATE: "bg-emerald-100 text-emerald-700 dark:bg-emerald-900/30 dark:text-emerald-300",
        UPDATE: "bg-sky-100 text-sky-700 dark:bg-sky-900/30 dark:text-sky-300",
        STATUS_CHANGE: "bg-amber-100 text-amber-700 dark:bg-amber-900/30 dark:text-amber-300",
        DELETE: "bg-rose-100 text-rose-700 dark:bg-rose-900/30 dark:text-rose-300"
    };
    return `<span class="inline-flex items-center rounded-full px-2.5 py-1 text-xs font-semibold ${styles[action] || "bg-slate-100 text-slate-700 dark:bg-slate-700 dark:text-slate-100"}">${auditActionLabels[action] || action || "---"}</span>`;
}

function renderAuditLogs(rows) {
    if (!elements.auditLogsTableBody) return;
    if (!rows || rows.length === 0) {
        elements.auditLogsTableBody.innerHTML = `
            <tr>
                <td colspan="4" class="px-6 py-10 text-center text-sm text-slate-500 dark:text-slate-400">
                    Chưa có lịch sử thao tác cho khoản quyên góp này.
                </td>
            </tr>
        `;
        return;
    }

    elements.auditLogsTableBody.innerHTML = rows.map((auditLog) => {
        const actor = auditLog.actorUsername || "Hệ thống";
        const role = auditLog.actorRole ? ` (${auditLog.actorRole})` : "";
        const changes = Array.isArray(auditLog.changes) ? auditLog.changes : [];
        const firstChanges = changes.slice(0, 3)
            .map((change) => `${change.field || "---"}: ${change.oldValue || "rỗng"} -> ${change.newValue || "rỗng"}`)
            .join("<br>");
        const moreCount = changes.length > 3 ? `<div class="mt-1 text-xs text-slate-400">+${changes.length - 3} thay đổi khác</div>` : "";

        return `
            <tr class="hover:bg-slate-50 dark:hover:bg-slate-800/30">
                <td class="px-6 py-4 text-sm text-slate-600 dark:text-slate-300 whitespace-nowrap">${formatDateTime(auditLog.createdAt)}</td>
                <td class="px-6 py-4 text-sm text-slate-700 dark:text-slate-200">
                    <div class="font-semibold">${actor}${role}</div>
                    <div class="text-xs text-slate-500">${auditLog.ipAddress || "---"}</div>
                </td>
                <td class="px-6 py-4 text-sm">${getAuditActionBadge(auditLog.action)}</td>
                <td class="px-6 py-4 text-sm text-slate-600 dark:text-slate-300">
                    <div class="font-medium text-slate-800 dark:text-slate-100">${auditLog.summary || "---"}</div>
                    <div class="mt-1">${firstChanges || "Không có thay đổi chi tiết"}</div>
                    ${moreCount}
                </td>
            </tr>
        `;
    }).join("");
}

async function loadSummary() {
    if (!elements.auditLogsCount) return;
    const response = await auditLogApi.getAuditLogs({
        page: 1,
        size: 1,
        entityType: "DONATION",
        entityId: state.donationId
    });
    const pageData = response?.data || {};
    elements.auditLogsCount.textContent = pageData.totalItems || 0;
}

async function loadAuditLogs() {
    if (!elements.auditLogsTableBody || !elements.auditLogsPagination) return;
    const response = await auditLogApi.getAuditLogs({
        page: state.auditLogs.page,
        size: state.auditLogs.size,
        entityType: "DONATION",
        entityId: state.donationId
    });
    const pageData = response?.data || {page: 1, pageSize: state.auditLogs.size, totalPages: 0, totalItems: 0, data: []};
    renderAuditLogs(pageData.data || []);
    renderPagination(pageData, elements.auditLogsPagination, (newPage) => {
        state.auditLogs.page = newPage;
        loadAuditLogs();
    });
    state.auditLogs.loaded = true;
}

function bindTabEvents() {
    elements.infoBtn?.addEventListener("click", () => setActiveTab("info"));
    elements.auditLogsBtn?.addEventListener("click", async () => {
        if (!elements.auditLogsPanel) return;
        setActiveTab("auditLogs");
        if (!state.auditLogs.loaded) await loadAuditLogs();
    });
}

function normalizeTabPanelsLayout() {
    if (!elements.infoPanel || !elements.auditLogsPanel) return;
    if (!elements.infoPanel.contains(elements.auditLogsPanel)) return;

    const parent = elements.infoPanel.parentElement;
    if (!parent) return;

    parent.insertBefore(elements.auditLogsPanel, elements.infoPanel.nextSibling);
}

async function handleApprove() {
    if (!state.donationId) return;
    try {
        const response = await donationApi.changeStatus(state.donationId, "CONFIRMED");
        if (response?.status !== 200) {
            throw new Error(response?.message || "Không thể duyệt khoản quyên góp");
        }
        alert("Duyệt khoản quyên góp thành công");
        window.location.reload();
    } catch (error) {
        alert(error.message || "Không thể duyệt khoản quyên góp");
    }
}

async function handleReject() {
    if (!state.donationId) return;
    const reason = window.prompt("Nhập lý do từ chối:");
    if (reason == null) return;
    if (!reason.trim()) {
        alert("Lý do từ chối không được để trống");
        return;
    }
    try {
        const response = await donationApi.rejectDonation(state.donationId, reason.trim());
        if (response?.status !== 200) {
            throw new Error(response?.message || "Không thể từ chối khoản quyên góp");
        }
        alert("Từ chối khoản quyên góp thành công");
        window.location.reload();
    } catch (error) {
        alert(error.message || "Không thể từ chối khoản quyên góp");
    }
}

async function handleSubmitApproval() {
    if (!state.donationId) return;
    try {
        const saved = await saveDonationDetail("Lưu chỉnh sửa thành công");
        if (!saved) return;

        const response = await donationApi.submitForApproval(state.donationId);
        if (response?.status !== 200) {
            throw new Error(response?.message || "Không thể gửi duyệt khoản quyên góp");
        }
        alert("Gửi duyệt khoản quyên góp thành công");
        window.location.reload();
    } catch (error) {
        alert(error.message || "Không thể gửi duyệt khoản quyên góp");
    }
}

document.addEventListener("DOMContentLoaded", async () => {
    if (!elements.section) return;
    state.donationId = Number(elements.section.dataset.donationId || 0);

    normalizeTabPanelsLayout();
    bindTabEvents();
    setActiveTab("info");
    if (state.donationId) {
        try {
            await loadSummary();
        } catch (error) {
            console.error("Không thể tải tổng quan tab lịch sử thao tác quyên góp:", error);
        }
    }

    elements.refreshBtn?.addEventListener("click", () => window.location.reload());
    elements.approveBtn?.addEventListener("click", handleApprove);
    elements.rejectBtn?.addEventListener("click", handleReject);
    elements.saveDonationBtn?.addEventListener("click", async () => {
        const saved = await saveDonationDetail(state.donationId ? "Cập nhật khoản quyên góp thành công" : "Tạo khoản quyên góp thành công");
        if (saved) {
            if (state.donationId) {
                window.location.href = `/admin/donations/${state.donationId}?saved=1`;
            }
        }
    });
    if (state.donationId) {
        elements.submitApprovalBtn?.addEventListener("click", handleSubmitApproval);
    }
    bindLookupEvents();

    elements.editNeedReceipt?.addEventListener("change", toggleReceiptFields);

    if (elements.editAmount) {
        setFormattedAmountValue(elements.editAmount, elements.editAmount.value || window.__DONATION_AMOUNT__ || 0);
        if (!elements.editAmount.disabled) {
            elements.editAmount.addEventListener("input", () => {
                setFormattedAmountValue(elements.editAmount, elements.editAmount.value);
            });
            elements.editAmount.addEventListener("blur", () => {
                setFormattedAmountValue(elements.editAmount, elements.editAmount.value);
            });
        }
    }

    if (elements.editDonatedAt) {
        const initialDonatedAt = window.__DONATION_DONATED_AT__ || todayDateInputValue();
        elements.editDonatedAt.value = toDateInputValue(initialDonatedAt);
    }

    if (!state.donationId) {
        setDonorReadonlyInfo("", "");
        if (elements.editNeedReceipt) {
            elements.editNeedReceipt.disabled = false;
            elements.editNeedReceipt.checked = false;
        }
        if (elements.editMessage) {
            elements.editMessage.disabled = false;
            elements.editMessage.value = "";
        }
        if (elements.paymentMethodSelect && !elements.paymentMethodSelect.value) {
            elements.paymentMethodSelect.value = "CASH";
        }
        if (elements.editMemoCode) {
            elements.editMemoCode.value = "";
        }
    }

    toggleReceiptFields();
});
