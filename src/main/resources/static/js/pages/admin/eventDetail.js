import {eventApi} from "../../apis/eventApi.js";
import {auditLogApi} from "../../apis/auditLogApi.js";
import {renderPagination} from "../../components/pagination.js";
import {formatDonationCode, getDonationStatusUi, DONATION_PAYMENT_METHOD_LABELS} from "../../utils/donationUi.js";
import {formatVnd} from "../../utils/currency.js";

const state = {
    eventId: null,
    activeTab: "info",
    activities: {
        page: 1,
        size: 10,
        loaded: false
    },
    donors: {
        page: 1,
        size: 10,
        loaded: false
    },
    donations: {
        page: 1,
        size: 10,
        loaded: false
    },
    auditLogs: {
        page: 1,
        size: 10,
        loaded: false
    }
};

const elements = {
    section: document.getElementById("eventDetailTabsSection"),
    infoBtn: document.getElementById("tabInfoBtn"),
    activitiesBtn: document.getElementById("tabActivitiesBtn"),
    donorsBtn: document.getElementById("tabDonorsBtn"),
    donationsBtn: document.getElementById("tabDonationsBtn"),
    auditLogsBtn: document.getElementById("tabAuditLogsBtn"),
    infoPanel: document.getElementById("tabInfoPanel"),
    activitiesPanel: document.getElementById("tabActivitiesPanel"),
    donorsPanel: document.getElementById("tabDonorsPanel"),
    donationsPanel: document.getElementById("tabDonationsPanel"),
    auditLogsPanel: document.getElementById("tabAuditLogsPanel"),
    activitiesCount: document.getElementById("tabActivitiesCount"),
    donorsCount: document.getElementById("tabDonorsCount"),
    donationsCount: document.getElementById("tabDonationsCount"),
    auditLogsCount: document.getElementById("tabAuditLogsCount"),
    activitiesTableBody: document.getElementById("eventActivitiesTableBody"),
    donorsTableBody: document.getElementById("eventDonorsTableBody"),
    donationsTableBody: document.getElementById("eventDonationsTableBody"),
    auditLogsTableBody: document.getElementById("eventAuditLogsTableBody"),
    activitiesPagination: document.getElementById("eventActivitiesPagination"),
    donorsPagination: document.getElementById("eventDonorsPagination"),
    donationsPagination: document.getElementById("eventDonationsPagination"),
    auditLogsPagination: document.getElementById("eventAuditLogsPagination")
};

const badgeStyles = {
    ONGOING: "bg-emerald-100 text-emerald-700 dark:bg-emerald-900/30 dark:text-emerald-300",
    UPCOMING: "bg-amber-100 text-amber-700 dark:bg-amber-900/30 dark:text-amber-300",
    COMPLETED: "bg-slate-200 text-slate-700 dark:bg-slate-700 dark:text-slate-200",
    DRAFT: "bg-sky-100 text-sky-700 dark:bg-sky-900/30 dark:text-sky-300"
};

const statusLabels = {
    ONGOING: "Đang diễn ra",
    UPCOMING: "Sắp diễn ra",
    COMPLETED: "Hoàn thành",
    DRAFT: "Bản nháp"
};

const auditActionLabels = {
    CREATE: "Tạo mới",
    UPDATE: "Cập nhật",
    STATUS_CHANGE: "Đổi trạng thái",
    DELETE: "Xóa"
};

const formatMoney = (amount) => formatVnd(amount);
const formatDateOnly = (value) => {
    if (!value) return "---";
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) return value;
    return date.toLocaleDateString("vi-VN", {
        year: "numeric",
        month: "2-digit",
        day: "2-digit"
    });
};
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

const formatDateRange = (startDate, endDate) => {
    const start = startDate || "---";
    const end = endDate || "---";
    return `${start} - ${end}`;
};

const getDonorTypeLabel = (type) => {
    if (type === "ORGANIZATION") return "Tổ chức";
    if (type === "INDIVIDUAL") return "Cá nhân";
    return "Chưa cập nhật";
};

function setActiveTab(tab) {
    state.activeTab = tab;

    const isInfo = tab === "info";
    const isActivities = tab === "activities";
    const isDonors = tab === "donors";
    const isDonations = tab === "donations";
    const isAuditLogs = tab === "auditLogs";

    if (elements.infoPanel) {
        elements.infoPanel.classList.toggle("hidden", !isInfo);
    }
    if (elements.section) {
        elements.section.classList.toggle("hidden", isInfo);
    }

    [elements.activitiesPanel, elements.donorsPanel, elements.donationsPanel, elements.auditLogsPanel]
        .forEach((panel) => panel?.classList.add("hidden"));
    if (isActivities) elements.activitiesPanel?.classList.remove("hidden");
    if (isDonors) elements.donorsPanel?.classList.remove("hidden");
    if (isDonations) elements.donationsPanel?.classList.remove("hidden");
    if (isAuditLogs) elements.auditLogsPanel?.classList.remove("hidden");

    if (elements.infoBtn) elements.infoBtn.className = isInfo
        ? "inline-flex items-center border-b-2 border-primary px-4 py-2 text-sm font-semibold text-primary"
        : "inline-flex items-center border-b-2 border-transparent px-4 py-2 text-sm font-semibold text-slate-600 transition hover:text-slate-900 dark:text-slate-300 dark:hover:text-white";
    if (elements.activitiesBtn) elements.activitiesBtn.className = isActivities
        ? "inline-flex items-center gap-2 border-b-2 border-primary px-4 py-2 text-sm font-semibold text-primary"
        : "inline-flex items-center gap-2 border-b-2 border-transparent px-4 py-2 text-sm font-semibold text-slate-600 transition hover:text-slate-900 dark:text-slate-300 dark:hover:text-white";
    if (elements.donorsBtn) elements.donorsBtn.className = isDonors
        ? "inline-flex items-center gap-2 border-b-2 border-primary px-4 py-2 text-sm font-semibold text-primary"
        : "inline-flex items-center gap-2 border-b-2 border-transparent px-4 py-2 text-sm font-semibold text-slate-600 transition hover:text-slate-900 dark:text-slate-300 dark:hover:text-white";
    if (elements.donationsBtn) elements.donationsBtn.className = isDonations
        ? "inline-flex items-center gap-2 border-b-2 border-primary px-4 py-2 text-sm font-semibold text-primary"
        : "inline-flex items-center gap-2 border-b-2 border-transparent px-4 py-2 text-sm font-semibold text-slate-600 transition hover:text-slate-900 dark:text-slate-300 dark:hover:text-white";
    if (elements.auditLogsBtn) elements.auditLogsBtn.className = isAuditLogs
        ? "inline-flex items-center gap-2 border-b-2 border-primary px-4 py-2 text-sm font-semibold text-primary"
        : "inline-flex items-center gap-2 border-b-2 border-transparent px-4 py-2 text-sm font-semibold text-slate-600 transition hover:text-slate-900 dark:text-slate-300 dark:hover:text-white";

    if (elements.activitiesCount) elements.activitiesCount.className = isActivities
        ? "inline-flex h-6 min-w-6 items-center justify-center rounded-full bg-primary/15 px-2 text-xs font-semibold text-primary"
        : "inline-flex h-6 min-w-6 items-center justify-center rounded-full bg-slate-200 px-2 text-xs font-semibold text-slate-700 dark:bg-slate-700 dark:text-slate-100";
    if (elements.donorsCount) elements.donorsCount.className = isDonors
        ? "inline-flex h-6 min-w-6 items-center justify-center rounded-full bg-primary/15 px-2 text-xs font-semibold text-primary"
        : "inline-flex h-6 min-w-6 items-center justify-center rounded-full bg-slate-200 px-2 text-xs font-semibold text-slate-700 dark:bg-slate-700 dark:text-slate-100";
    if (elements.donationsCount) elements.donationsCount.className = isDonations
        ? "inline-flex h-6 min-w-6 items-center justify-center rounded-full bg-primary/15 px-2 text-xs font-semibold text-primary"
        : "inline-flex h-6 min-w-6 items-center justify-center rounded-full bg-slate-200 px-2 text-xs font-semibold text-slate-700 dark:bg-slate-700 dark:text-slate-100";
    if (elements.auditLogsCount) elements.auditLogsCount.className = isAuditLogs
        ? "inline-flex h-6 min-w-6 items-center justify-center rounded-full bg-primary/15 px-2 text-xs font-semibold text-primary"
        : "inline-flex h-6 min-w-6 items-center justify-center rounded-full bg-slate-200 px-2 text-xs font-semibold text-slate-700 dark:bg-slate-700 dark:text-slate-100";
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

function renderActivities(rows) {
    if (!rows || rows.length === 0) {
        elements.activitiesTableBody.innerHTML = `
            <tr>
                <td colspan="4" class="px-6 py-10 text-center text-sm text-slate-500 dark:text-slate-400">
                    Sự kiện này chưa có hoạt động nào.
                </td>
            </tr>
        `;
        return;
    }

    elements.activitiesTableBody.innerHTML = rows.map((activity) => {
        const statusClass = badgeStyles[activity.status] || badgeStyles.DRAFT;
        const statusLabel = statusLabels[activity.status] || "Chưa cập nhật";

        return `
            <tr class="hover:bg-slate-50 dark:hover:bg-slate-800/30">
                <td class="px-6 py-4">
                    <div class="font-semibold text-slate-900 dark:text-white">${activity.name || "---"}</div>
                    <div class="text-xs text-slate-500 dark:text-slate-400">${activity.shortDescription || "Chưa có mô tả ngắn."}</div>
                </td>
                <td class="px-6 py-4">
                    <span class="inline-flex items-center rounded-full px-2.5 py-1 text-xs font-semibold ${statusClass}">
                        ${statusLabel}
                    </span>
                </td>
                <td class="px-6 py-4 text-right">
                    <div class="font-semibold text-slate-900 dark:text-white">${formatMoney(activity.currentAmount)}</div>
                    <div class="text-xs text-slate-500 dark:text-slate-400">Mục tiêu: ${formatMoney(activity.targetAmount)}</div>
                </td>
                <td class="px-6 py-4 text-sm text-slate-600 dark:text-slate-300">${formatDateRange(activity.startDate, activity.endDate)}</td>
            </tr>
        `;
    }).join("");
}

function renderDonors(rows) {
    if (!rows || rows.length === 0) {
        elements.donorsTableBody.innerHTML = `
            <tr>
                <td colspan="6" class="px-6 py-10 text-center text-sm text-slate-500 dark:text-slate-400">
                    Sự kiện này chưa có nhà hảo tâm nào.
                </td>
            </tr>
        `;
        return;
    }

    elements.donorsTableBody.innerHTML = rows.map((donor) => {
        const donorName = donor.displayName || donor.fullName || "---";
        const donorLink = donor.id ? `/admin/donors/${donor.id}` : "#";

        return `
            <tr class="hover:bg-slate-50 dark:hover:bg-slate-800/30">
                <td class="px-6 py-4">
                    <a href="${donorLink}" class="font-semibold text-slate-900 hover:text-primary dark:text-white dark:hover:text-primary">
                        ${donorName}
                    </a>
                </td>
                <td class="px-6 py-4 text-sm text-slate-600 dark:text-slate-300">${getDonorTypeLabel(donor.type)}</td>
                <td class="px-6 py-4 text-sm text-slate-600 dark:text-slate-300">${donor.phone || "---"}</td>
                <td class="px-6 py-4 text-sm text-slate-600 dark:text-slate-300">${donor.email || "---"}</td>
                <td class="px-6 py-4 text-sm text-slate-600 dark:text-slate-300">${formatDateTime(donor.createdAt)}</td>
                <td class="px-6 py-4 text-right font-semibold text-slate-900 dark:text-white">${formatMoney(donor.totalDonationAmount)}</td>
            </tr>
        `;
    }).join("");
}

function renderDonations(rows) {
    if (!rows || rows.length === 0) {
        elements.donationsTableBody.innerHTML = `
            <tr>
                <td colspan="7" class="px-6 py-10 text-center text-sm text-slate-500 dark:text-slate-400">
                    Sự kiện này chưa có khoản quyên góp nào.
                </td>
            </tr>
        `;
        return;
    }

    elements.donationsTableBody.innerHTML = rows.map((donation) => {
        const donationLink = donation.id ? `/admin/donations/${donation.id}` : "#";
        const donationCode = formatDonationCode(donation.id);
        const statusUi = getDonationStatusUi(donation.status);
        const paymentLabel = donation.paymentMethodValue || DONATION_PAYMENT_METHOD_LABELS[donation.paymentMethod] || "---";
        const donateDate = formatDateOnly(donation.donatedAt || donation.createdAt);

        return `
            <tr class="hover:bg-slate-50 dark:hover:bg-slate-800/30">
                <td class="px-6 py-4">
                    <input type="checkbox" disabled class="h-4 w-4 rounded border-slate-300 text-primary"/>
                </td>
                <td class="px-6 py-4">
                    <a href="${donationLink}" class="font-semibold text-slate-900 hover:text-primary dark:text-white dark:hover:text-primary">
                        ${donationCode}
                    </a>
                </td>
                <td class="px-6 py-4 text-sm text-slate-700 dark:text-slate-300">${donation.donorName || "---"}</td>
                <td class="px-6 py-4 text-right font-semibold text-slate-900 dark:text-white">${formatMoney(donation.amount)}</td>
                <td class="px-6 py-4 text-sm text-slate-600 dark:text-slate-300">${paymentLabel}</td>
                <td class="px-6 py-4 text-sm text-slate-600 dark:text-slate-300">${donateDate}</td>
                <td class="px-6 py-4">
                    <span class="${statusUi.className}">
                        ${statusUi.text}
                    </span>
                </td>
            </tr>
        `;
    }).join("");
}

function renderAuditLogs(rows) {
    if (!rows || rows.length === 0) {
        elements.auditLogsTableBody.innerHTML = `
            <tr>
                <td colspan="4" class="px-6 py-10 text-center text-sm text-slate-500 dark:text-slate-400">
                    Chưa có lịch sử thao tác cho sự kiện này.
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
    try {
        const response = await eventApi.getEventDetailTabsSummary(state.eventId);
        const summary = response?.data || {};
        elements.activitiesCount.textContent = summary.activityCount ?? 0;
        elements.donorsCount.textContent = summary.donorCount ?? 0;
        elements.donationsCount.textContent = summary.donationCount ?? 0;
        elements.auditLogsCount.textContent = summary.auditLogCount ?? 0;
    } catch (error) {
        console.error("Không thể tải tổng quan tab sự kiện:", error);
    }
}

async function loadActivities() {
    try {
        const response = await eventApi.getEventDetailActivities(state.eventId, {
            page: state.activities.page,
            size: state.activities.size
        });
        const pageData = response?.data || {page: 1, pageSize: state.activities.size, totalPages: 0, totalItems: 0, data: []};
        renderActivities(pageData?.data || []);
        renderPagination(pageData, elements.activitiesPagination, (newPage) => {
            state.activities.page = newPage;
            loadActivities();
        });
        state.activities.loaded = true;
    } catch (error) {
        console.error("Không thể tải hoạt động của sự kiện:", error);
        elements.activitiesTableBody.innerHTML = `
            <tr>
                <td colspan="4" class="px-6 py-10 text-center text-sm text-red-500">
                    Không thể tải danh sách hoạt động.
                </td>
            </tr>
        `;
    }
}

async function loadDonors() {
    try {
        const response = await eventApi.getEventDetailDonors(state.eventId, {
            page: state.donors.page,
            size: state.donors.size
        });
        const pageData = response?.data || {page: 1, pageSize: state.donors.size, totalPages: 0, totalItems: 0, data: []};
        renderDonors(pageData?.data || []);
        renderPagination(pageData, elements.donorsPagination, (newPage) => {
            state.donors.page = newPage;
            loadDonors();
        });
        state.donors.loaded = true;
    } catch (error) {
        console.error("Không thể tải nhà hảo tâm của sự kiện:", error);
        elements.donorsTableBody.innerHTML = `
            <tr>
                <td colspan="6" class="px-6 py-10 text-center text-sm text-red-500">
                    Không thể tải danh sách nhà hảo tâm.
                </td>
            </tr>
        `;
    }
}

async function loadDonations() {
    try {
        const response = await eventApi.getEventDetailDonations(state.eventId, {
            page: state.donations.page,
            size: state.donations.size
        });
        const pageData = response?.data || {page: 1, pageSize: state.donations.size, totalPages: 0, totalItems: 0, data: []};
        renderDonations(pageData?.data || []);
        renderPagination(pageData, elements.donationsPagination, (newPage) => {
            state.donations.page = newPage;
            loadDonations();
        });
        state.donations.loaded = true;
    } catch (error) {
        console.error("Không thể tải danh sách quyên góp của sự kiện:", error);
        elements.donationsTableBody.innerHTML = `
            <tr>
                <td colspan="7" class="px-6 py-10 text-center text-sm text-red-500">
                    Không thể tải danh sách quyên góp.
                </td>
            </tr>
        `;
    }
}

async function loadAuditLogs() {
    try {
        const response = await auditLogApi.getAuditLogs({
            page: state.auditLogs.page,
            size: state.auditLogs.size,
            entityType: "EVENT",
            entityId: state.eventId
        });
        const pageData = response?.data || {page: 1, pageSize: state.auditLogs.size, totalPages: 0, totalItems: 0, data: []};
        renderAuditLogs(pageData?.data || []);
        renderPagination(pageData, elements.auditLogsPagination, (newPage) => {
            state.auditLogs.page = newPage;
            loadAuditLogs();
        });
        state.auditLogs.loaded = true;
    } catch (error) {
        console.error("Không thể tải lịch sử thao tác sự kiện:", error);
        elements.auditLogsTableBody.innerHTML = `
            <tr>
                <td colspan="4" class="px-6 py-10 text-center text-sm text-red-500">
                    Không thể tải lịch sử thao tác.
                </td>
            </tr>
        `;
    }
}

function bindTabEvents() {
    elements.infoBtn?.addEventListener("click", async () => {
        setActiveTab("info");
    });

    elements.activitiesBtn?.addEventListener("click", async () => {
        setActiveTab("activities");
        if (state.eventId && !state.activities.loaded) {
            await loadActivities();
        }
    });

    elements.donorsBtn?.addEventListener("click", async () => {
        setActiveTab("donors");
        if (state.eventId && !state.donors.loaded) {
            await loadDonors();
        }
    });

    elements.donationsBtn?.addEventListener("click", async () => {
        setActiveTab("donations");
        if (state.eventId && !state.donations.loaded) {
            await loadDonations();
        }
    });

    elements.auditLogsBtn?.addEventListener("click", async () => {
        setActiveTab("auditLogs");
        if (state.eventId && !state.auditLogs.loaded) {
            await loadAuditLogs();
        }
    });
}

document.addEventListener("DOMContentLoaded", async () => {
    if (!elements.section) return;

    state.eventId = Number(elements.section.dataset.eventId || 0);
    bindTabEvents();
    setActiveTab("info");
    if (state.eventId) {
        await loadSummary();
    } else {
        [elements.activitiesBtn, elements.donorsBtn, elements.donationsBtn, elements.auditLogsBtn]
            .forEach((btn) => btn?.setAttribute("disabled", "disabled"));
    }
});
