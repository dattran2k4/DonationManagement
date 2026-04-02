import {donorApi} from "../../apis/donorApi.js";
import {auditLogApi} from "../../apis/auditLogApi.js";
import {renderPagination} from "../../components/pagination.js";

const donorId = window.__DONOR_ID__;
const state = {
    activeTab: "info",
    history: {
        page: 1,
        size: 10,
        loaded: false
    },
    audit: {
        page: 1,
        size: 10,
        loaded: false
    }
};

const elements = {
    infoBtn: document.getElementById("tabInfoBtn"),
    historyBtn: document.getElementById("tabHistoryBtn"),
    auditBtn: document.getElementById("tabAuditBtn"),
    infoPanel: document.getElementById("tabInfoPanel"),
    historyPanel: document.getElementById("tabHistoryPanel"),
    auditPanel: document.getElementById("tabAuditPanel"),
    tableBody: document.getElementById("donorDonationHistoryBody"),
    paginationContainer: document.getElementById("donorDonationPagination"),
    auditTableBody: document.getElementById("donorAuditTableBody"),
    auditPaginationContainer: document.getElementById("donorAuditPagination"),
    auditCount: document.getElementById("tabAuditCount")
};

const formatCurrency = (amount) => `${new Intl.NumberFormat("vi-VN").format(amount || 0)} đ`;

const formatDateTime = (dateTime) => {
    if (!dateTime) return "---";
    return new Date(dateTime).toLocaleString("vi-VN", {
        hour: "2-digit",
        minute: "2-digit",
        day: "2-digit",
        month: "2-digit",
        year: "numeric"
    });
};

const getStatusBadge = (status, label) => {
    const styles = {
        PENDING_PAYMENT: "bg-yellow-100 text-yellow-800",
        PENDING_APPROVED: "bg-amber-100 text-amber-800",
        CONFIRMED: "bg-emerald-100 text-emerald-800",
        CANCELLED: "bg-slate-100 text-slate-700",
        REJECTED: "bg-red-100 text-red-700",
        FAILED: "bg-rose-100 text-rose-700"
    };

    return `<span class="inline-flex items-center rounded-full px-2.5 py-1 text-xs font-semibold ${styles[status] || "bg-slate-100 text-slate-700"}">${label || status || "---"}</span>`;
};

const getAuditActionBadge = (action) => {
    const styles = {
        CREATE: "bg-emerald-100 text-emerald-700",
        UPDATE: "bg-sky-100 text-sky-700",
        STATUS_CHANGE: "bg-amber-100 text-amber-700",
        DELETE: "bg-rose-100 text-rose-700"
    };
    const labels = {
        CREATE: "Tạo mới",
        UPDATE: "Cập nhật",
        STATUS_CHANGE: "Đổi trạng thái",
        DELETE: "Xóa"
    };

    return `<span class="inline-flex items-center rounded-full px-2.5 py-1 text-xs font-semibold ${styles[action] || "bg-slate-100 text-slate-700"}">${labels[action] || action || "---"}</span>`;
};

function setActiveTab(tab) {
    state.activeTab = tab;
    const isInfo = tab === "info";
    const isHistory = tab === "history";
    const isAudit = tab === "audit";

    elements.infoPanel.classList.toggle("hidden", !isInfo);
    elements.historyPanel.classList.toggle("hidden", !isHistory);
    elements.auditPanel.classList.toggle("hidden", !isAudit);

    elements.infoBtn.className = isInfo
        ? "inline-flex items-center border-b-2 border-primary px-4 py-2 text-sm font-semibold text-primary"
        : "inline-flex items-center border-b-2 border-transparent px-4 py-2 text-sm font-semibold text-slate-600 transition hover:text-slate-900";
    elements.historyBtn.className = isHistory
        ? "inline-flex items-center border-b-2 border-primary px-4 py-2 text-sm font-semibold text-primary"
        : "inline-flex items-center border-b-2 border-transparent px-4 py-2 text-sm font-semibold text-slate-600 transition hover:text-slate-900";
    elements.auditBtn.className = isAudit
        ? "inline-flex items-center gap-2 border-b-2 border-primary px-4 py-2 text-sm font-semibold text-primary"
        : "inline-flex items-center gap-2 border-b-2 border-transparent px-4 py-2 text-sm font-semibold text-slate-600 transition hover:text-slate-900";

    elements.auditCount.className = isAudit
        ? "inline-flex h-6 min-w-6 items-center justify-center rounded-full bg-primary/15 px-2 text-xs font-semibold text-primary"
        : "inline-flex h-6 min-w-6 items-center justify-center rounded-full bg-slate-200 px-2 text-xs font-semibold text-slate-700";
}

function renderTable(rows) {
    if (!rows || rows.length === 0) {
        elements.tableBody.innerHTML = `<tr><td class="px-6 py-8 text-center text-slate-500" colspan="6">Nhà hảo tâm chưa có lịch sử quyên góp.</td></tr>`;
        return;
    }

    elements.tableBody.innerHTML = rows.map((item) => `
        <tr class="hover:bg-slate-50 transition-colors">
            <td class="px-6 py-4 whitespace-nowrap text-sm font-mono text-slate-700">#${item.donationCode || `DN-${item.donationId}`}</td>
            <td class="px-6 py-4 whitespace-nowrap text-sm text-slate-700">${item.targetLabel || "---"}</td>
            <td class="px-6 py-4 text-sm">
                ${item.targetUrl
        ? `<a href="${item.targetUrl}" target="_blank" class="font-medium text-primary hover:underline">${item.targetTitle || "---"}</a>`
        : `<span class="text-slate-700">${item.targetTitle || "---"}</span>`
    }
            </td>
            <td class="px-6 py-4 whitespace-nowrap text-sm text-right font-semibold text-slate-900">${formatCurrency(item.amount)}</td>
            <td class="px-6 py-4 whitespace-nowrap text-sm">${getStatusBadge(item.status, item.statusLabel)}</td>
            <td class="px-6 py-4 whitespace-nowrap text-sm text-slate-600">${formatDateTime(item.donatedAt)}</td>
        </tr>
    `).join("");
}

function renderAuditTable(rows) {
    if (!rows || rows.length === 0) {
        elements.auditTableBody.innerHTML = `<tr><td class="px-6 py-8 text-center text-slate-500" colspan="4">Nhà hảo tâm chưa có lịch sử thao tác.</td></tr>`;
        return;
    }

    elements.auditTableBody.innerHTML = rows.map((item) => {
        const actor = item.actorUsername || "Hệ thống";
        const role = item.actorRole ? ` (${item.actorRole})` : "";
        const changes = Array.isArray(item.changes) ? item.changes : [];
        const firstChanges = changes.slice(0, 3)
            .map((change) => `${change.field || "---"}: ${change.oldValue || "rỗng"} -> ${change.newValue || "rỗng"}`)
            .join("<br>");
        const moreCount = changes.length > 3 ? `<div class="mt-1 text-xs text-slate-400">+${changes.length - 3} thay đổi khác</div>` : "";

        return `
        <tr class="hover:bg-slate-50 transition-colors">
            <td class="px-6 py-4 whitespace-nowrap text-sm text-slate-600">${formatDateTime(item.createdAt)}</td>
            <td class="px-6 py-4 text-sm text-slate-700">
                <div class="font-semibold">${actor}${role}</div>
                <div class="text-xs text-slate-500">${item.ipAddress || "---"}</div>
            </td>
            <td class="px-6 py-4 whitespace-nowrap text-sm">${getAuditActionBadge(item.action)}</td>
            <td class="px-6 py-4 text-sm text-slate-600">
                <div class="font-medium text-slate-800">${item.summary || "---"}</div>
                <div class="mt-1">${firstChanges || "Không có thay đổi chi tiết"}</div>
                ${moreCount}
            </td>
        </tr>
    `;
    }).join("");
}

async function loadHistory() {
    if (!donorId) return;
    try {
        const response = await donorApi.getDonorDonations(donorId, state.history);
        const pageData = response?.data || {};
        renderTable(pageData.data || []);
        renderPagination(pageData, elements.paginationContainer, (newPage) => {
            state.history.page = newPage;
            loadHistory();
        });
        state.history.loaded = true;
    } catch (error) {
        console.error("Lỗi tải lịch sử quyên góp:", error);
        elements.tableBody.innerHTML = `<tr><td class="px-6 py-8 text-center text-red-500" colspan="6">Không thể tải lịch sử quyên góp.</td></tr>`;
    }
}

async function loadAuditSummary() {
    if (!donorId) return;
    try {
        const response = await auditLogApi.getAuditLogs({
            page: 1,
            size: 1,
            entityType: "DONOR",
            entityId: donorId
        });
        const pageData = response?.data || {};
        elements.auditCount.textContent = pageData.totalItems || 0;
    } catch (error) {
        console.error("Lỗi tải tổng số lịch sử thao tác:", error);
    }
}

async function loadAuditHistory() {
    if (!donorId) return;
    try {
        const response = await auditLogApi.getAuditLogs({
            page: state.audit.page,
            size: state.audit.size,
            entityType: "DONOR",
            entityId: donorId
        });
        const pageData = response?.data || {};
        renderAuditTable(pageData.data || []);
        renderPagination(pageData, elements.auditPaginationContainer, (newPage) => {
            state.audit.page = newPage;
            loadAuditHistory();
        });
        state.audit.loaded = true;
    } catch (error) {
        console.error("Lỗi tải lịch sử thao tác:", error);
        elements.auditTableBody.innerHTML = `<tr><td class="px-6 py-8 text-center text-red-500" colspan="4">Không thể tải lịch sử thao tác.</td></tr>`;
    }
}

document.addEventListener("DOMContentLoaded", () => {
    setActiveTab("info");
    loadAuditSummary();

    elements.infoBtn?.addEventListener("click", () => setActiveTab("info"));
    elements.historyBtn?.addEventListener("click", async () => {
        setActiveTab("history");
        if (!state.history.loaded) {
            await loadHistory();
        }
    });
    elements.auditBtn?.addEventListener("click", async () => {
        setActiveTab("audit");
        if (!state.audit.loaded) {
            await loadAuditHistory();
        }
    });
});
