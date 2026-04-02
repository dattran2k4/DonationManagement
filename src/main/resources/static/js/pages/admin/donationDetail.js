import {auditLogApi} from "../../apis/auditLogApi.js";
import {renderPagination} from "../../components/pagination.js";

const state = {
    donationId: null,
    activeTab: "info",
    auditLogs: {page: 1, size: 10, loaded: false}
};

const elements = {
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

function setActiveTab(tab) {
    state.activeTab = tab;
    const isInfo = tab === "info";
    const isAuditLogs = tab === "auditLogs";

    elements.infoPanel.classList.toggle("hidden", !isInfo);
    elements.auditLogsPanel.classList.toggle("hidden", !isAuditLogs);

    elements.infoBtn.className = isInfo
        ? "inline-flex items-center border-b-2 border-primary px-4 py-2 text-sm font-semibold text-primary"
        : "inline-flex items-center border-b-2 border-transparent px-4 py-2 text-sm font-semibold text-slate-600 transition hover:text-slate-900 dark:text-slate-300 dark:hover:text-white";
    elements.auditLogsBtn.className = isAuditLogs
        ? "inline-flex items-center gap-2 border-b-2 border-primary px-4 py-2 text-sm font-semibold text-primary"
        : "inline-flex items-center gap-2 border-b-2 border-transparent px-4 py-2 text-sm font-semibold text-slate-600 transition hover:text-slate-900 dark:text-slate-300 dark:hover:text-white";

    elements.auditLogsCount.className = isAuditLogs
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

function renderAuditLogs(rows) {
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
        setActiveTab("auditLogs");
        if (!state.auditLogs.loaded) await loadAuditLogs();
    });
}

document.addEventListener("DOMContentLoaded", async () => {
    if (!elements.section) return;
    state.donationId = Number(elements.section.dataset.donationId || 0);
    if (!state.donationId) return;

    bindTabEvents();
    setActiveTab("info");
    try {
        await loadSummary();
    } catch (error) {
        console.error("Không thể tải tổng quan tab lịch sử thao tác quyên góp:", error);
    }
});
