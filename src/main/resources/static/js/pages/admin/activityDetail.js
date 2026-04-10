import {activityApi} from "../../apis/activityApi.js";
import {auditLogApi} from "../../apis/auditLogApi.js";
import {renderPagination} from "../../components/pagination.js";
import {formatDonationCode, getDonationStatusUi, DONATION_PAYMENT_METHOD_LABELS} from "../../utils/donationUi.js";
import {formatVnd} from "../../utils/currency.js";

const state = {
    activityId: null,
    activeTab: "info",
    donors: {page: 1, size: 10, loaded: false},
    donations: {page: 1, size: 10, loaded: false},
    auditLogs: {page: 1, size: 10, loaded: false}
};

const elements = {
    section: document.getElementById("activityDetailTabsSection"),
    infoBtn: document.getElementById("tabInfoBtn"),
    donorsBtn: document.getElementById("tabDonorsBtn"),
    donationsBtn: document.getElementById("tabDonationsBtn"),
    auditLogsBtn: document.getElementById("tabAuditLogsBtn"),
    infoPanel: document.getElementById("tabInfoPanel"),
    donorsPanel: document.getElementById("tabDonorsPanel"),
    donationsPanel: document.getElementById("tabDonationsPanel"),
    auditLogsPanel: document.getElementById("tabAuditLogsPanel"),
    donorsCount: document.getElementById("tabDonorsCount"),
    donationsCount: document.getElementById("tabDonationsCount"),
    auditLogsCount: document.getElementById("tabAuditLogsCount"),
    donorsTableBody: document.getElementById("activityDonorsTableBody"),
    donationsTableBody: document.getElementById("activityDonationsTableBody"),
    auditLogsTableBody: document.getElementById("activityAuditLogsTableBody"),
    donorsPagination: document.getElementById("activityDonorsPagination"),
    donationsPagination: document.getElementById("activityDonationsPagination"),
    auditLogsPagination: document.getElementById("activityAuditLogsPagination")
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
    return date.toLocaleDateString("vi-VN", {year: "numeric", month: "2-digit", day: "2-digit"});
};
const formatDateTime = (value) => {
    if (!value) return "---";
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) return value;
    return date.toLocaleString("vi-VN", {
        hour12: false, year: "numeric", month: "2-digit", day: "2-digit", hour: "2-digit", minute: "2-digit"
    });
};

const getDonorTypeLabel = (type) => {
    if (type === "ORGANIZATION") return "Tổ chức";
    if (type === "INDIVIDUAL") return "Cá nhân";
    return "Chưa cập nhật";
};

function setActiveTab(tab) {
    state.activeTab = tab;
    const isInfo = tab === "info";
    const isDonors = tab === "donors";
    const isDonations = tab === "donations";
    const isAuditLogs = tab === "auditLogs";

    if (elements.infoPanel) {
        elements.infoPanel.classList.toggle("hidden", !isInfo);
    }
    if (elements.section) {
        elements.section.classList.toggle("hidden", isInfo);
    }

    [elements.donorsPanel, elements.donationsPanel, elements.auditLogsPanel]
        .forEach((panel) => panel?.classList.add("hidden"));
    if (isDonors) elements.donorsPanel?.classList.remove("hidden");
    if (isDonations) elements.donationsPanel?.classList.remove("hidden");
    if (isAuditLogs) elements.auditLogsPanel?.classList.remove("hidden");

    if (elements.infoBtn) elements.infoBtn.className = isInfo
        ? "inline-flex items-center border-b-2 border-primary px-4 py-2 text-sm font-semibold text-primary"
        : "inline-flex items-center border-b-2 border-transparent px-4 py-2 text-sm font-semibold text-slate-600 transition hover:text-slate-900";
    if (elements.donorsBtn) elements.donorsBtn.className = isDonors
        ? "inline-flex items-center gap-2 border-b-2 border-primary px-4 py-2 text-sm font-semibold text-primary"
        : "inline-flex items-center gap-2 border-b-2 border-transparent px-4 py-2 text-sm font-semibold text-slate-600 transition hover:text-slate-900";
    if (elements.donationsBtn) elements.donationsBtn.className = isDonations
        ? "inline-flex items-center gap-2 border-b-2 border-primary px-4 py-2 text-sm font-semibold text-primary"
        : "inline-flex items-center gap-2 border-b-2 border-transparent px-4 py-2 text-sm font-semibold text-slate-600 transition hover:text-slate-900";
    if (elements.auditLogsBtn) elements.auditLogsBtn.className = isAuditLogs
        ? "inline-flex items-center gap-2 border-b-2 border-primary px-4 py-2 text-sm font-semibold text-primary"
        : "inline-flex items-center gap-2 border-b-2 border-transparent px-4 py-2 text-sm font-semibold text-slate-600 transition hover:text-slate-900";

    if (elements.donorsCount) elements.donorsCount.className = isDonors
        ? "inline-flex h-6 min-w-6 items-center justify-center rounded-full bg-primary/15 px-2 text-xs font-semibold text-primary"
        : "inline-flex h-6 min-w-6 items-center justify-center rounded-full bg-slate-200 px-2 text-xs font-semibold text-slate-700";
    if (elements.donationsCount) elements.donationsCount.className = isDonations
        ? "inline-flex h-6 min-w-6 items-center justify-center rounded-full bg-primary/15 px-2 text-xs font-semibold text-primary"
        : "inline-flex h-6 min-w-6 items-center justify-center rounded-full bg-slate-200 px-2 text-xs font-semibold text-slate-700";
    if (elements.auditLogsCount) elements.auditLogsCount.className = isAuditLogs
        ? "inline-flex h-6 min-w-6 items-center justify-center rounded-full bg-primary/15 px-2 text-xs font-semibold text-primary"
        : "inline-flex h-6 min-w-6 items-center justify-center rounded-full bg-slate-200 px-2 text-xs font-semibold text-slate-700";
}

function getAuditActionBadge(action) {
    const styles = {
        CREATE: "bg-emerald-100 text-emerald-700",
        UPDATE: "bg-sky-100 text-sky-700",
        STATUS_CHANGE: "bg-amber-100 text-amber-700",
        DELETE: "bg-rose-100 text-rose-700"
    };
    return `<span class="inline-flex items-center rounded-full px-2.5 py-1 text-xs font-semibold ${styles[action] || "bg-slate-100 text-slate-700"}">${auditActionLabels[action] || action || "---"}</span>`;
}

function renderDonors(rows) {
    if (!rows || rows.length === 0) {
        elements.donorsTableBody.innerHTML = `
            <tr><td colspan="6" class="px-6 py-10 text-center text-sm text-slate-500">Hoạt động này chưa có nhà hảo tâm.</td></tr>
        `;
        return;
    }

    elements.donorsTableBody.innerHTML = rows.map((donor) => `
        <tr class="hover:bg-slate-50 transition-colors">
            <td class="px-6 py-4">
                <a href="/admin/donors/${donor.id}" class="font-semibold text-slate-900 hover:text-primary">${donor.displayName || donor.fullName || "---"}</a>
            </td>
            <td class="px-6 py-4 text-sm text-slate-600">${getDonorTypeLabel(donor.type)}</td>
            <td class="px-6 py-4 text-sm text-slate-600">${donor.phone || "---"}</td>
            <td class="px-6 py-4 text-sm text-slate-600">${donor.email || "---"}</td>
            <td class="px-6 py-4 text-sm text-slate-600">${formatDateTime(donor.createdAt)}</td>
            <td class="px-6 py-4 text-right font-semibold text-slate-900">${formatMoney(donor.totalDonationAmount)}</td>
        </tr>
    `).join("");
}

function renderDonations(rows) {
    if (!rows || rows.length === 0) {
        elements.donationsTableBody.innerHTML = `
            <tr><td colspan="7" class="px-6 py-10 text-center text-sm text-slate-500">Hoạt động này chưa có khoản quyên góp.</td></tr>
        `;
        return;
    }

    elements.donationsTableBody.innerHTML = rows.map((donation) => {
        const statusUi = getDonationStatusUi(donation.status);
        const paymentLabel = donation.paymentMethodValue || DONATION_PAYMENT_METHOD_LABELS[donation.paymentMethod] || "---";
        const donatedDate = formatDateOnly(donation.donatedAt || donation.createdAt);
        return `
        <tr class="hover:bg-slate-50 transition-colors">
            <td class="px-6 py-4">
                <input type="checkbox" disabled class="h-4 w-4 rounded border-slate-300 text-primary"/>
            </td>
            <td class="px-6 py-4">
                <a href="/admin/donations/${donation.id}" class="font-semibold text-slate-900 hover:text-primary">${formatDonationCode(donation.id)}</a>
            </td>
            <td class="px-6 py-4 text-sm text-slate-700">${donation.donorName || "---"}</td>
            <td class="px-6 py-4 text-right font-semibold text-slate-900">${formatMoney(donation.amount)}</td>
            <td class="px-6 py-4 text-sm text-slate-600">${paymentLabel}</td>
            <td class="px-6 py-4 text-sm text-slate-600">${donatedDate}</td>
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
            <tr><td colspan="4" class="px-6 py-10 text-center text-sm text-slate-500">Chưa có lịch sử thao tác cho hoạt động này.</td></tr>
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
            <tr class="hover:bg-slate-50 transition-colors">
                <td class="px-6 py-4 text-sm text-slate-600 whitespace-nowrap">${formatDateTime(auditLog.createdAt)}</td>
                <td class="px-6 py-4 text-sm text-slate-700">
                    <div class="font-semibold">${actor}${role}</div>
                    <div class="text-xs text-slate-500">${auditLog.ipAddress || "---"}</div>
                </td>
                <td class="px-6 py-4 text-sm">${getAuditActionBadge(auditLog.action)}</td>
                <td class="px-6 py-4 text-sm text-slate-600">
                    <div class="font-medium text-slate-800">${auditLog.summary || "---"}</div>
                    <div class="mt-1">${firstChanges || "Không có thay đổi chi tiết"}</div>
                    ${moreCount}
                </td>
            </tr>
        `;
    }).join("");
}

async function loadSummary() {
    const response = await activityApi.getActivityDetailTabsSummary(state.activityId);
    const summary = response?.data || {};
    elements.donorsCount.textContent = summary.donorCount ?? 0;
    elements.donationsCount.textContent = summary.donationCount ?? 0;

    const auditResponse = await auditLogApi.getAuditLogs({
        page: 1,
        size: 1,
        entityType: "ACTIVITY",
        entityId: state.activityId
    });
    const auditPage = auditResponse?.data || {};
    elements.auditLogsCount.textContent = auditPage.totalItems ?? 0;
}

async function loadDonors() {
    const response = await activityApi.getActivityDetailDonors(state.activityId, {
        page: state.donors.page, size: state.donors.size
    });
    const pageData = response?.data || {page: 1, pageSize: state.donors.size, totalPages: 0, totalItems: 0, data: []};
    renderDonors(pageData.data || []);
    renderPagination(pageData, elements.donorsPagination, (newPage) => {
        state.donors.page = newPage;
        loadDonors();
    });
    state.donors.loaded = true;
}

async function loadDonations() {
    const response = await activityApi.getActivityDetailDonations(state.activityId, {
        page: state.donations.page, size: state.donations.size
    });
    const pageData = response?.data || {page: 1, pageSize: state.donations.size, totalPages: 0, totalItems: 0, data: []};
    renderDonations(pageData.data || []);
    renderPagination(pageData, elements.donationsPagination, (newPage) => {
        state.donations.page = newPage;
        loadDonations();
    });
    state.donations.loaded = true;
}

async function loadAuditLogs() {
    const response = await auditLogApi.getAuditLogs({
        page: state.auditLogs.page,
        size: state.auditLogs.size,
        entityType: "ACTIVITY",
        entityId: state.activityId
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
    elements.donorsBtn?.addEventListener("click", async () => {
        setActiveTab("donors");
        if (!state.donors.loaded) await loadDonors();
    });
    elements.donationsBtn?.addEventListener("click", async () => {
        setActiveTab("donations");
        if (!state.donations.loaded) await loadDonations();
    });
    elements.auditLogsBtn?.addEventListener("click", async () => {
        setActiveTab("auditLogs");
        if (!state.auditLogs.loaded) await loadAuditLogs();
    });
}

document.addEventListener("DOMContentLoaded", async () => {
    if (!elements.section) return;
    state.activityId = Number(elements.section.dataset.activityId || 0);
    bindTabEvents();
    setActiveTab("info");

    if (state.activityId) {
        try {
            await loadSummary();
        } catch (error) {
            console.error("Không thể tải tổng quan tab hoạt động:", error);
        }
    } else {
        [elements.donorsBtn, elements.donationsBtn, elements.auditLogsBtn]
            .forEach((btn) => btn?.setAttribute("disabled", "disabled"));
    }
});
