import {donorApi} from "../../apis/donorApi.js";
import {auditLogApi} from "../../apis/auditLogApi.js";
import {renderPagination} from "../../components/pagination.js";
import {createDonor} from "../../modules/donor-submit.js";
import {formatVnd} from "../../utils/currency.js";

const donorId = window.__DONOR_ID__;
const donorTypeFromServer = window.__DONOR_TYPE__ || "INDIVIDUAL";
const canManage = Boolean(window.__CAN_MANAGE_DONOR__);
const isCreateMode = !donorId;
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
    },
    selectedDonationIds: new Set(),
    currentHistoryRows: []
};

const elements = {
    saveBtn: document.getElementById("saveBtn"),
    deleteBtn: document.getElementById("deleteBtn"),
    refreshBtn: document.getElementById("refreshBtn"),
    infoBtn: document.getElementById("tabInfoBtn"),
    historyBtn: document.getElementById("tabHistoryBtn"),
    auditBtn: document.getElementById("tabAuditBtn"),
    infoPanel: document.getElementById("tabInfoPanel"),
    historyPanel: document.getElementById("tabHistoryPanel"),
    auditPanel: document.getElementById("tabAuditPanel"),
    tableBody: document.getElementById("donorDonationHistoryBody"),
    paginationContainer: document.getElementById("donorDonationPagination"),
    selectAllHistory: document.getElementById("donorDonationSelectAll"),
    totalHistoryCount: document.getElementById("donorDonationTotalCount"),
    selectedHistoryCount: document.getElementById("donorDonationSelectedCount"),
    auditTableBody: document.getElementById("donorAuditTableBody"),
    auditPaginationContainer: document.getElementById("donorAuditPagination"),
    auditCount: document.getElementById("tabAuditCount"),
    donorTypeIndividualBtn: document.getElementById("donorTypeIndividualBtn"),
    donorTypeOrganizationBtn: document.getElementById("donorTypeOrganizationBtn"),
    donorIndividualSection: document.getElementById("donorIndividualSection"),
    donorOrganizationSection: document.getElementById("donorOrganizationSection")
};

const formatCurrency = (amount) => formatVnd(amount);

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

const formatDate = (dateTime) => {
    if (!dateTime) return "---";
    return new Date(dateTime).toLocaleDateString("vi-VN");
};

const paymentMethodLabelMap = {
    CASH: "Tiền mặt",
    BANK_TRANSFER_ONLINE: "Chuyển Khoản Online",
    BANK_TRANSFER_OFFLINE: "Chuyển Khoản"
};

const getPaymentMethodLabel = (item) => {
    if (item.paymentMethodLabel) return item.paymentMethodLabel;
    return paymentMethodLabelMap[item.paymentMethod] || "---";
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

    elements.infoPanel?.classList.toggle("hidden", !isInfo);
    elements.historyPanel?.classList.toggle("hidden", !isHistory);
    elements.auditPanel?.classList.toggle("hidden", !isAudit);

    if (elements.infoBtn) elements.infoBtn.className = isInfo
        ? "inline-flex items-center border-b-2 border-primary px-4 py-2 text-sm font-semibold text-primary"
        : "inline-flex items-center border-b-2 border-transparent px-4 py-2 text-sm font-semibold text-slate-600 transition hover:text-slate-900";
    if (elements.historyBtn) elements.historyBtn.className = isHistory
        ? "inline-flex items-center border-b-2 border-primary px-4 py-2 text-sm font-semibold text-primary"
        : "inline-flex items-center border-b-2 border-transparent px-4 py-2 text-sm font-semibold text-slate-600 transition hover:text-slate-900";
    if (elements.auditBtn) elements.auditBtn.className = isAudit
        ? "inline-flex items-center gap-2 border-b-2 border-primary px-4 py-2 text-sm font-semibold text-primary"
        : "inline-flex items-center gap-2 border-b-2 border-transparent px-4 py-2 text-sm font-semibold text-slate-600 transition hover:text-slate-900";

    if (elements.auditCount) elements.auditCount.className = isAudit
        ? "inline-flex h-6 min-w-6 items-center justify-center rounded-full bg-primary/15 px-2 text-xs font-semibold text-primary"
        : "inline-flex h-6 min-w-6 items-center justify-center rounded-full bg-slate-200 px-2 text-xs font-semibold text-slate-700";
}

function updateHistorySelectionSummary() {
    if (elements.selectedHistoryCount) {
        elements.selectedHistoryCount.textContent = String(state.selectedDonationIds.size);
    }

    if (elements.totalHistoryCount) {
        elements.totalHistoryCount.textContent = String(state.currentHistoryRows.length);
    }

    if (!elements.selectAllHistory) return;
    const currentIds = state.currentHistoryRows.map((row) => row.donationId).filter(Boolean);
    const selectedInPage = currentIds.filter((id) => state.selectedDonationIds.has(id)).length;
    elements.selectAllHistory.checked = currentIds.length > 0 && selectedInPage === currentIds.length;
    elements.selectAllHistory.indeterminate = selectedInPage > 0 && selectedInPage < currentIds.length;
}

function bindHistorySelectionEvents() {
    const rowCheckboxes = elements.tableBody.querySelectorAll("input[data-donation-id]");
    rowCheckboxes.forEach((checkbox) => {
        checkbox.addEventListener("change", () => {
            const donationId = Number(checkbox.dataset.donationId);
            if (!donationId) return;
            if (checkbox.checked) {
                state.selectedDonationIds.add(donationId);
            } else {
                state.selectedDonationIds.delete(donationId);
            }

            const row = checkbox.closest("tr");
            if (row) {
                row.classList.toggle("bg-emerald-50", checkbox.checked);
            }
            updateHistorySelectionSummary();
        });
    });
}

function renderTable(rows) {
    state.currentHistoryRows = rows || [];
    if (elements.selectAllHistory) {
        elements.selectAllHistory.checked = false;
        elements.selectAllHistory.indeterminate = false;
    }

    if (!rows || rows.length === 0) {
        elements.tableBody.innerHTML = `<tr><td class="px-6 py-8 text-center text-slate-500" colspan="8">Nhà hảo tâm chưa có lịch sử quyên góp.</td></tr>`;
        updateHistorySelectionSummary();
        return;
    }

    elements.tableBody.innerHTML = rows.map((item) => {
        const donationId = Number(item.donationId);
        const isSelected = donationId && state.selectedDonationIds.has(donationId);
        const donationCode = item.donationCode || `DTN-${String(donationId || 0).padStart(8, "0")}`;

        return `
        <tr class="transition-colors ${isSelected ? "bg-emerald-50" : "hover:bg-slate-50"}">
            <td class="px-4 py-4 text-center">
                <input type="checkbox"
                       data-donation-id="${donationId || ""}"
                       class="h-5 w-5 rounded border-slate-300 text-primary focus:ring-primary"
                       ${isSelected ? "checked" : ""}>
            </td>
            <td class="px-4 py-4 whitespace-nowrap text-sm font-semibold text-primary underline underline-offset-2">${donationCode}</td>
            <td class="px-4 py-4 whitespace-nowrap text-sm text-slate-700">${item.targetLabel || "---"}</td>
            <td class="px-4 py-4 text-sm">
                ${item.targetUrl
            ? `<a href="${item.targetUrl}" target="_blank" class="font-medium text-primary underline underline-offset-2 hover:opacity-80">${item.targetTitle || "---"}</a>`
            : `<span class="text-slate-700">${item.targetTitle || "---"}</span>`
        }
            </td>
            <td class="px-4 py-4 whitespace-nowrap text-sm text-slate-700">${formatDate(item.donatedAt)}</td>
            <td class="px-4 py-4 whitespace-nowrap text-sm text-right font-semibold text-slate-900">${formatCurrency(item.amount)}</td>
            <td class="px-4 py-4 whitespace-nowrap text-sm text-slate-700">${getPaymentMethodLabel(item)}</td>
            <td class="px-4 py-4 whitespace-nowrap text-sm text-slate-700">${item.statusLabel || item.status || "---"}</td>
        </tr>
    `;
    }).join("");

    bindHistorySelectionEvents();
    updateHistorySelectionSummary();
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
        elements.tableBody.innerHTML = `<tr><td class="px-6 py-8 text-center text-red-500" colspan="8">Không thể tải lịch sử quyên góp.</td></tr>`;
    }
}

async function loadAuditSummary() {
    if (!donorId || !elements.auditCount) return;
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

function setCreateDonorType(type) {
    const isIndividual = type === "INDIVIDUAL";
    elements.donorIndividualSection?.classList.toggle("hidden", !isIndividual);
    elements.donorOrganizationSection?.classList.toggle("hidden", isIndividual);
    elements.donorTypeIndividualBtn?.classList.toggle("bg-primary", isIndividual);
    elements.donorTypeIndividualBtn?.classList.toggle("text-white", isIndividual);
    elements.donorTypeIndividualBtn?.classList.toggle("text-slate-600", !isIndividual);
    elements.donorTypeOrganizationBtn?.classList.toggle("bg-primary", !isIndividual);
    elements.donorTypeOrganizationBtn?.classList.toggle("text-white", !isIndividual);
    elements.donorTypeOrganizationBtn?.classList.toggle("text-slate-600", isIndividual);
}

function collectCreateDonorData() {
    return {
        fullName: document.getElementById("fullName")?.value?.trim() || "",
        displayName: document.getElementById("displayName")?.value?.trim() || "",
        name: document.getElementById("orgName")?.value?.trim() || "",
        taxCode: document.getElementById("taxCode")?.value?.trim() || "",
        representative: document.getElementById("representative")?.value?.trim() || "",
        billingAddress: document.getElementById("billingAddress")?.value?.trim() || "",
        phone: document.getElementById("phone")?.value?.trim() || "",
        email: document.getElementById("email")?.value?.trim() || "",
        referralSource: document.getElementById("referralSource")?.value || "",
        note: document.getElementById("note")?.value?.trim() || ""
    };
}

function collectDonorData() {
    return {
        fullName: document.getElementById("fullName")?.value?.trim() || "",
        displayName: document.getElementById("displayName")?.value?.trim() || "",
        name: document.getElementById("orgName")?.value?.trim() || "",
        taxCode: document.getElementById("taxCode")?.value?.trim() || "",
        representative: document.getElementById("representative")?.value?.trim() || "",
        billingAddress: document.getElementById("billingAddress")?.value?.trim() || "",
        phone: document.getElementById("phone")?.value?.trim() || "",
        email: document.getElementById("email")?.value?.trim() || "",
        referralSource: document.getElementById("referralSource")?.value || "",
        note: document.getElementById("note")?.value?.trim() || ""
    };
}

function getCurrentDonorType() {
    if (!isCreateMode) {
        return donorTypeFromServer;
    }
    return elements.donorTypeOrganizationBtn?.classList.contains("bg-primary")
        ? "ORGANIZATION"
        : "INDIVIDUAL";
}

async function handleCreateDonor() {
    const donorType = getCurrentDonorType();
    const rawData = collectCreateDonorData();
    try {
        const savedDonorId = await createDonor(donorType, rawData, {});
        alert("Tạo mới nhà hảo tâm thành công");
        window.location.href = `/admin/donors/${savedDonorId}?saved=1`;
    } catch (error) {
        alert(error.message || "Không thể tạo mới nhà hảo tâm");
    }
}

async function handleUpdateDonor() {
    if (!donorId) return;
    const donorType = getCurrentDonorType();
    const rawData = collectDonorData();
    try {
        await createDonor(donorType, rawData, {donorId});
        alert("Cập nhật nhà hảo tâm thành công");
        window.location.href = `/admin/donors/${donorId}?saved=1`;
    } catch (error) {
        alert(error.message || "Không thể cập nhật nhà hảo tâm");
    }
}

async function handleDeleteDonor() {
    if (!donorId) return;
    const confirmed = window.confirm("Bạn chắc chắn muốn xóa nhà hảo tâm này?");
    if (!confirmed) return;
    try {
        const response = await donorApi.deleteDonor(donorId);
        if (response?.status !== 200) {
            throw new Error(response?.message || "Không thể xóa nhà hảo tâm");
        }
        alert("Xóa nhà hảo tâm thành công");
        window.location.href = "/admin/donors";
    } catch (error) {
        alert(error.message || "Không thể xóa nhà hảo tâm");
    }
}

function initUpdateMode() {
    if (isCreateMode || !canManage) return;
    document.querySelectorAll(".donor-updatable").forEach((field) => {
        field.removeAttribute("readonly");
        field.removeAttribute("disabled");
        field.classList.remove("border-slate-300");
        field.classList.add("border-slate-200");
    });
}

async function openInitialTab() {
    const url = new URL(window.location.href);
    const requestedTab = (url.searchParams.get("tab") || "info").toLowerCase();

    if (requestedTab === "history" && !isCreateMode && elements.historyBtn) {
        setActiveTab("history");
        if (!state.history.loaded) {
            await loadHistory();
        }
        return;
    }

    if (requestedTab === "audit" && !isCreateMode && elements.auditBtn) {
        setActiveTab("audit");
        if (!state.audit.loaded) {
            await loadAuditHistory();
        }
        return;
    }

    setActiveTab("info");
}

document.addEventListener("DOMContentLoaded", () => {
    openInitialTab();
    if (!isCreateMode) {
        loadAuditSummary();
    }

    elements.selectAllHistory?.addEventListener("change", () => {
        const checked = elements.selectAllHistory.checked;
        state.currentHistoryRows.forEach((row) => {
            if (!row.donationId) return;
            if (checked) {
                state.selectedDonationIds.add(row.donationId);
            } else {
                state.selectedDonationIds.delete(row.donationId);
            }
        });
        renderTable(state.currentHistoryRows);
    });

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

    if (isCreateMode) {
        setCreateDonorType("INDIVIDUAL");
        elements.donorTypeIndividualBtn?.addEventListener("click", () => setCreateDonorType("INDIVIDUAL"));
        elements.donorTypeOrganizationBtn?.addEventListener("click", () => setCreateDonorType("ORGANIZATION"));
        elements.saveBtn?.addEventListener("click", handleCreateDonor);
    } else {
        initUpdateMode();
        if (canManage) {
            elements.saveBtn?.addEventListener("click", handleUpdateDonor);
            elements.deleteBtn?.addEventListener("click", handleDeleteDonor);
        }
    }
    elements.refreshBtn?.addEventListener("click", () => window.location.reload());
});
