import {donorApi} from "../../apis/donorApi.js";
import {auditLogApi} from "../../apis/auditLogApi.js";
import {renderPagination} from "../../components/pagination.js";

const donorId = Number(window.__DONOR_ID__ || 0) || null;
const donorType = window.__DONOR_TYPE__ || "";
const donorFullName = window.__DONOR_FULL_NAME__ || "Không rõ nhà hảo tâm";
const canEditRelationships = Boolean(window.__CAN_EDIT_RELATIONSHIPS__);
const supportsPersonalRelationships = donorType === "INDIVIDUAL";

const state = {
    activeTab: "info",
    history: {
        page: 1,
        size: 10,
        loaded: false
    },
    relationships: {
        loaded: false,
        personItems: [],
        organizationItems: [],
        personTypes: [],
        organizationRoleTypes: []
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
    relationshipBtn: document.getElementById("tabRelationshipBtn"),
    auditBtn: document.getElementById("tabAuditBtn"),
    infoPanel: document.getElementById("tabInfoPanel"),
    historyPanel: document.getElementById("tabHistoryPanel"),
    relationshipPanel: document.getElementById("tabRelationshipPanel"),
    auditPanel: document.getElementById("tabAuditPanel"),
    tableBody: document.getElementById("donorDonationHistoryBody"),
    paginationContainer: document.getElementById("donorDonationPagination"),
    auditTableBody: document.getElementById("donorAuditTableBody"),
    auditPaginationContainer: document.getElementById("donorAuditPagination"),
    auditCount: document.getElementById("tabAuditCount"),

    personTableBody: document.getElementById("personRelationshipTableBody"),
    organizationTableBody: document.getElementById("organizationRelationshipTableBody"),

    addPersonBtn: document.getElementById("addPersonRelationshipBtn"),
    addOrganizationBtn: document.getElementById("addOrganizationRelationshipBtn"),

    personFormCard: document.getElementById("personRelationshipFormCard"),
    personFormTitle: document.getElementById("personRelationshipFormTitle"),
    personForm: document.getElementById("personRelationshipForm"),
    personFormId: document.getElementById("personRelationshipId"),
    personTypeSelect: document.getElementById("personRelationshipTypeSelect"),
    personRelatedDonorId: document.getElementById("personRelatedDonorId"),
    personRelatedDonorSearch: document.getElementById("personRelatedDonorSearch"),
    personRelatedDonorSearchWrapper: document.getElementById("personRelatedDonorSearchWrapper"),
    personRelatedDonorDropdown: document.getElementById("personRelatedDonorDropdown"),
    personRelatedDonorDropdownList: document.getElementById("personRelatedDonorDropdownList"),
    personRelatedDonorMeta: document.getElementById("personRelatedDonorMeta"),
    personNote: document.getElementById("personRelationshipNote"),
    personCancelBtn: document.getElementById("cancelPersonRelationshipBtn"),
    personResetBtn: document.getElementById("personRelationshipResetBtn"),

    organizationFormCard: document.getElementById("organizationRelationshipFormCard"),
    organizationFormTitle: document.getElementById("organizationRelationshipFormTitle"),
    organizationForm: document.getElementById("organizationRelationshipForm"),
    organizationFormId: document.getElementById("organizationRelationshipId"),
    organizationRoleSelect: document.getElementById("organizationRoleTypeSelect"),
    organizationRelatedDonorId: document.getElementById("organizationRelatedDonorId"),
    organizationRelatedDonorSearch: document.getElementById("organizationRelatedDonorSearch"),
    organizationRelatedDonorSearchWrapper: document.getElementById("organizationRelatedDonorSearchWrapper"),
    organizationRelatedDonorDropdown: document.getElementById("organizationRelatedDonorDropdown"),
    organizationRelatedDonorDropdownList: document.getElementById("organizationRelatedDonorDropdownList"),
    organizationRelatedDonorMeta: document.getElementById("organizationRelatedDonorMeta"),
    organizationNote: document.getElementById("organizationRelationshipNote"),
    organizationCancelBtn: document.getElementById("cancelOrganizationRelationshipBtn"),
    organizationResetBtn: document.getElementById("organizationRelationshipResetBtn")
};

const DEFAULT_PERSON_META = "Chưa chọn nhà hảo tâm liên quan.";
const DEFAULT_ORGANIZATION_META = donorType === "ORGANIZATION"
    ? "Chưa chọn nhà hảo tâm liên quan."
    : "Chưa chọn tổ chức liên quan.";

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

const escapeHtml = (value) => String(value ?? "")
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#39;");

const debounce = (fn, delay = 300) => {
    let timeoutId;
    return (...args) => {
        clearTimeout(timeoutId);
        timeoutId = setTimeout(() => fn(...args), delay);
    };
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

const getTabButtonClass = (active, hasGap = false) => {
    const base = `inline-flex items-center${hasGap ? " gap-2" : ""} border-b-2 px-4 py-2 text-sm font-semibold transition`;
    return active
        ? `${base} border-primary text-primary`
        : `${base} border-transparent text-slate-600 hover:text-slate-900`;
};

const getPersonDisplayName = (item) => item?.relatedDonorName || item?.relatedDonorDisplayName || "Không rõ tên";

const getRelatedDonorDisplayName = (item) => item?.relatedDonorName || "Không rõ liên kết";

function setActiveTab(tab) {
    state.activeTab = tab;
    const isInfo = tab === "info";
    const isHistory = tab === "history";
    const isRelationship = tab === "relationship";
    const isAudit = tab === "audit";

    if (elements.infoPanel) elements.infoPanel.classList.toggle("hidden", !isInfo);
    if (elements.historyPanel) elements.historyPanel.classList.toggle("hidden", !isHistory);
    if (elements.relationshipPanel) elements.relationshipPanel.classList.toggle("hidden", !isRelationship);
    if (elements.auditPanel) elements.auditPanel.classList.toggle("hidden", !isAudit);

    if (elements.infoBtn) elements.infoBtn.className = getTabButtonClass(isInfo);
    if (elements.historyBtn) elements.historyBtn.className = getTabButtonClass(isHistory);
    if (elements.relationshipBtn) elements.relationshipBtn.className = getTabButtonClass(isRelationship);
    if (elements.auditBtn) elements.auditBtn.className = getTabButtonClass(isAudit, true);

    if (elements.auditCount) {
        elements.auditCount.className = isAudit
            ? "inline-flex h-6 min-w-6 items-center justify-center rounded-full bg-primary/15 px-2 text-xs font-semibold text-primary"
            : "inline-flex h-6 min-w-6 items-center justify-center rounded-full bg-slate-200 px-2 text-xs font-semibold text-slate-700";
    }
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

function renderPersonRelationshipTable(rows) {
    if (!elements.personTableBody) return;

    if (!rows || rows.length === 0) {
        elements.personTableBody.innerHTML = `<tr><td class="px-6 py-8 text-center text-slate-500" colspan="5">Chưa có mối quan hệ cá nhân nào.</td></tr>`;
        return;
    }

    elements.personTableBody.innerHTML = rows.map((item) => `
        <tr class="hover:bg-slate-50 transition-colors">
            <td class="px-6 py-4 text-sm text-slate-700">
                <div class="font-semibold text-slate-900">${escapeHtml(getPersonDisplayName(item))}</div>
                <div class="mt-1 text-xs text-slate-500">${escapeHtml(item.relatedDonorPhone || "---")} ${item.relatedDonorEmail ? `• ${escapeHtml(item.relatedDonorEmail)}` : ""}</div>
            </td>
            <td class="px-6 py-4 whitespace-nowrap text-sm font-semibold text-slate-900">${escapeHtml(item.relationshipTypeName || "---")}</td>
            <td class="px-6 py-4 text-sm text-slate-600">${escapeHtml(item.note || "—")}</td>
            <td class="px-6 py-4 whitespace-nowrap text-sm text-slate-600">${formatDateTime(item.updatedAt)}</td>
            <td class="px-6 py-4">
                <div class="flex items-center justify-end gap-2">
                    ${canEditRelationships ? `
                        <button type="button" data-action="edit-person" data-id="${item.id}" class="inline-flex items-center rounded-lg border border-slate-200 px-3 py-1.5 text-xs font-semibold text-slate-600 transition hover:border-primary hover:text-primary">
                            Sửa
                        </button>
                        <button type="button" data-action="deactivate-person" data-id="${item.id}" class="inline-flex items-center rounded-lg border border-rose-200 px-3 py-1.5 text-xs font-semibold text-rose-600 transition hover:bg-rose-50">
                            Ngừng dùng
                        </button>
                    ` : `<span class="text-xs text-slate-400">Chỉ xem</span>`}
                </div>
            </td>
        </tr>
    `).join("");
}

function renderOrganizationRelationshipTable(rows) {
    if (!elements.organizationTableBody) return;

    if (!rows || rows.length === 0) {
        const emptyMessage = donorType === "ORGANIZATION"
            ? "Chưa có nhà hảo tâm liên quan nào."
            : "Chưa có mối quan hệ tổ chức nào.";
        elements.organizationTableBody.innerHTML = `<tr><td class="px-6 py-8 text-center text-slate-500" colspan="5">${emptyMessage}</td></tr>`;
        return;
    }

    elements.organizationTableBody.innerHTML = rows.map((item) => `
        <tr class="hover:bg-slate-50 transition-colors">
            <td class="px-6 py-4 text-sm text-slate-700">
                <div class="font-semibold text-slate-900">${escapeHtml(getRelatedDonorDisplayName(item))}</div>
                <div class="mt-1 text-xs text-slate-500">${escapeHtml(item.relatedDonorPhone || "---")} ${item.relatedDonorEmail ? `• ${escapeHtml(item.relatedDonorEmail)}` : ""}</div>
            </td>
            <td class="px-6 py-4 whitespace-nowrap text-sm font-semibold text-slate-900">${escapeHtml(item.roleTypeName || "---")}</td>
            <td class="px-6 py-4 text-sm text-slate-600">${escapeHtml(item.note || "—")}</td>
            <td class="px-6 py-4 whitespace-nowrap text-sm text-slate-600">${formatDateTime(item.updatedAt)}</td>
            <td class="px-6 py-4">
                <div class="flex items-center justify-end gap-2">
                    ${canEditRelationships ? `
                        <button type="button" data-action="edit-organization" data-id="${item.id}" class="inline-flex items-center rounded-lg border border-slate-200 px-3 py-1.5 text-xs font-semibold text-slate-600 transition hover:border-primary hover:text-primary">
                            Sửa
                        </button>
                        <button type="button" data-action="deactivate-organization" data-id="${item.id}" class="inline-flex items-center rounded-lg border border-rose-200 px-3 py-1.5 text-xs font-semibold text-rose-600 transition hover:bg-rose-50">
                            Ngừng dùng
                        </button>
                    ` : `<span class="text-xs text-slate-400">Chỉ xem</span>`}
                </div>
            </td>
        </tr>
    `).join("");
}

function populatePersonTypeOptions() {
    if (!elements.personTypeSelect) return;
    const selectedValue = elements.personTypeSelect.value;
    elements.personTypeSelect.innerHTML = `
        <option value="">Chọn loại mối quan hệ</option>
        ${state.relationships.personTypes.map((item) => `<option value="${item.id}">${escapeHtml(item.name)}</option>`).join("")}
    `;
    if (selectedValue) {
        elements.personTypeSelect.value = selectedValue;
    }
}

function populateOrganizationRoleOptions() {
    if (!elements.organizationRoleSelect) return;
    const selectedValue = elements.organizationRoleSelect.value;
    elements.organizationRoleSelect.innerHTML = `
        <option value="">Chọn vai trò trong tổ chức</option>
        ${state.relationships.organizationRoleTypes.map((item) => `<option value="${item.id}">${escapeHtml(item.name)}</option>`).join("")}
    `;
    if (selectedValue) {
        elements.organizationRoleSelect.value = selectedValue;
    }
}

function showDropdown(dropdown) {
    if (!dropdown) return;
    dropdown.classList.remove("hidden");
}

function hideDropdown(dropdown) {
    if (!dropdown) return;
    dropdown.classList.add("hidden");
}

function setSelectedMeta(element, defaultMessage, id, label) {
    if (!element) return;
    if (!id) {
        element.textContent = defaultMessage;
        return;
    }
    element.textContent = `Đã chọn: #${id} - ${label}`;
}

function resetPersonForm() {
    if (!elements.personForm) return;
    elements.personForm.reset();
    elements.personFormId.value = "";
    elements.personRelatedDonorId.value = "";
    if (elements.personFormTitle) {
        elements.personFormTitle.textContent = "Thêm quan hệ cá nhân";
    }
    setSelectedMeta(elements.personRelatedDonorMeta, DEFAULT_PERSON_META, null, null);
}

function resetOrganizationForm() {
    if (!elements.organizationForm) return;
    elements.organizationForm.reset();
    elements.organizationFormId.value = "";
    elements.organizationRelatedDonorId.value = "";
    if (elements.organizationFormTitle) {
        elements.organizationFormTitle.textContent = donorType === "ORGANIZATION"
            ? "Thêm nhà hảo tâm liên quan"
            : "Thêm quan hệ tổ chức";
    }
    setSelectedMeta(elements.organizationRelatedDonorMeta, DEFAULT_ORGANIZATION_META, null, null);
}

function closePersonForm() {
    if (!elements.personFormCard) return;
    elements.personFormCard.classList.add("hidden");
    resetPersonForm();
    hideDropdown(elements.personRelatedDonorDropdown);
}

function closeOrganizationForm() {
    if (!elements.organizationFormCard) return;
    elements.organizationFormCard.classList.add("hidden");
    resetOrganizationForm();
    hideDropdown(elements.organizationRelatedDonorDropdown);
}

function openPersonForm(item = null) {
    if (!elements.personFormCard) return;
    resetPersonForm();

    if (item) {
        elements.personFormId.value = item.id;
        elements.personTypeSelect.value = String(item.relationshipTypeId || "");
        elements.personRelatedDonorId.value = item.relatedDonorId || "";
        elements.personRelatedDonorSearch.value = getPersonDisplayName(item);
        elements.personNote.value = item.note || "";
        setSelectedMeta(elements.personRelatedDonorMeta, DEFAULT_PERSON_META, item.relatedDonorId, getPersonDisplayName(item));
        if (elements.personFormTitle) {
            elements.personFormTitle.textContent = "Chỉnh sửa quan hệ cá nhân";
        }
    }

    elements.personFormCard.classList.remove("hidden");
    elements.personFormCard.scrollIntoView({behavior: "smooth", block: "nearest"});
}

function openOrganizationForm(item = null) {
    if (!elements.organizationFormCard) return;
    resetOrganizationForm();

    if (item) {
        elements.organizationFormId.value = item.id;
        elements.organizationRoleSelect.value = String(item.roleTypeId || "");
        elements.organizationRelatedDonorId.value = item.relatedDonorId || "";
        elements.organizationRelatedDonorSearch.value = getRelatedDonorDisplayName(item);
        elements.organizationNote.value = item.note || "";
        setSelectedMeta(elements.organizationRelatedDonorMeta, DEFAULT_ORGANIZATION_META, item.relatedDonorId, getRelatedDonorDisplayName(item));
        if (elements.organizationFormTitle) {
            elements.organizationFormTitle.textContent = donorType === "ORGANIZATION"
                ? "Chỉnh sửa nhà hảo tâm liên quan"
                : "Chỉnh sửa quan hệ tổ chức";
        }
    }

    elements.organizationFormCard.classList.remove("hidden");
    elements.organizationFormCard.scrollIntoView({behavior: "smooth", block: "nearest"});
}

function renderPersonLookupDropdown(donors) {
    if (!elements.personRelatedDonorDropdownList) return;

    if (!donors || donors.length === 0) {
        elements.personRelatedDonorDropdownList.innerHTML = `<div class="px-4 py-3 text-sm text-slate-500">Không tìm thấy nhà hảo tâm cá nhân phù hợp.</div>`;
        return;
    }

    elements.personRelatedDonorDropdownList.innerHTML = donors.map((item) => `
        <button type="button"
                data-person-id="${item.id}"
                data-person-name="${escapeHtml(item.fullName || item.displayName || "Không rõ tên")}"
                class="grid w-full grid-cols-[minmax(0,1fr)_minmax(0,1fr)] gap-4 px-4 py-3 text-left text-sm transition hover:bg-slate-50">
            <span class="min-w-0">
                <span class="block truncate font-semibold text-slate-900">${escapeHtml(item.fullName || item.displayName || "Không rõ tên")}</span>
                <span class="mt-1 block truncate text-xs text-slate-500">${escapeHtml(item.phone || "---")}</span>
            </span>
            <span class="truncate text-right text-slate-500">${escapeHtml(item.email || "Không có email")}</span>
        </button>
    `).join("");
}

function renderOrganizationLookupDropdown(donors) {
    if (!elements.organizationRelatedDonorDropdownList) return;

    if (!donors || donors.length === 0) {
        const emptyMessage = donorType === "ORGANIZATION"
            ? "Không tìm thấy nhà hảo tâm cá nhân phù hợp."
            : "Không tìm thấy tổ chức phù hợp.";
        elements.organizationRelatedDonorDropdownList.innerHTML = `<div class="px-4 py-3 text-sm text-slate-500">${emptyMessage}</div>`;
        return;
    }

    elements.organizationRelatedDonorDropdownList.innerHTML = donors.map((item) => {
        const name = item.organization?.name || item.fullName || item.displayName || "Không rõ liên kết";
        return `
            <button type="button"
                    data-organization-id="${item.id}"
                    data-organization-name="${escapeHtml(name)}"
                    class="grid w-full grid-cols-[minmax(0,1fr)_minmax(0,1fr)] gap-4 px-4 py-3 text-left text-sm transition hover:bg-slate-50">
                <span class="min-w-0">
                    <span class="block truncate font-semibold text-slate-900">${escapeHtml(name)}</span>
                    <span class="mt-1 block truncate text-xs text-slate-500">${escapeHtml(item.phone || "---")}</span>
                </span>
                <span class="truncate text-right text-slate-500">${escapeHtml(item.email || "Không có email")}</span>
            </button>
        `;
    }).join("");
}

async function loadPersonLookup(search = "") {
    if (!elements.personRelatedDonorDropdownList) return;
    elements.personRelatedDonorDropdownList.innerHTML = `<div class="px-4 py-3 text-sm text-slate-500">Đang tải danh sách nhà hảo tâm...</div>`;
    showDropdown(elements.personRelatedDonorDropdown);

    try {
        const response = await donorApi.getAllDonors({
            page: 1,
            size: 20,
            search: search.trim(),
            type: "INDIVIDUAL",
            sortBy: "name",
            sortDir: "asc"
        });
        const items = (response?.data?.data || []).filter((item) => Number(item.id) !== donorId);
        renderPersonLookupDropdown(items);
    } catch (error) {
        console.error("Lỗi tải lookup nhà hảo tâm cá nhân:", error);
        elements.personRelatedDonorDropdownList.innerHTML = `<div class="px-4 py-3 text-sm text-red-500">Không thể tải danh sách nhà hảo tâm cá nhân.</div>`;
    }
}

async function loadOrganizationLookup(search = "") {
    if (!elements.organizationRelatedDonorDropdownList) return;
    elements.organizationRelatedDonorDropdownList.innerHTML = `<div class="px-4 py-3 text-sm text-slate-500">${donorType === "ORGANIZATION" ? "Đang tải danh sách nhà hảo tâm..." : "Đang tải danh sách tổ chức..."}</div>`;
    showDropdown(elements.organizationRelatedDonorDropdown);

    try {
        const response = await donorApi.getAllDonors({
            page: 1,
            size: 20,
            search: search.trim(),
            type: donorType === "ORGANIZATION" ? "INDIVIDUAL" : "ORGANIZATION",
            sortBy: "name",
            sortDir: "asc"
        });
        const items = response?.data?.data || [];
        renderOrganizationLookupDropdown(items);
    } catch (error) {
        console.error("Lỗi tải lookup quan hệ tổ chức:", error);
        elements.organizationRelatedDonorDropdownList.innerHTML = `<div class="px-4 py-3 text-sm text-red-500">Không thể tải danh sách liên quan.</div>`;
    }
}

const debouncedLoadPersonLookup = debounce((search) => loadPersonLookup(search), 300);
const debouncedLoadOrganizationLookup = debounce((search) => loadOrganizationLookup(search), 300);

function selectPersonRelatedDonor(id, label) {
    if (elements.personRelatedDonorId) elements.personRelatedDonorId.value = id || "";
    if (elements.personRelatedDonorSearch) elements.personRelatedDonorSearch.value = label || "";
    setSelectedMeta(elements.personRelatedDonorMeta, DEFAULT_PERSON_META, id, label);
    hideDropdown(elements.personRelatedDonorDropdown);
}

function selectOrganizationRelatedDonor(id, label) {
    if (elements.organizationRelatedDonorId) elements.organizationRelatedDonorId.value = id || "";
    if (elements.organizationRelatedDonorSearch) elements.organizationRelatedDonorSearch.value = label || "";
    setSelectedMeta(elements.organizationRelatedDonorMeta, DEFAULT_ORGANIZATION_META, id, label);
    hideDropdown(elements.organizationRelatedDonorDropdown);
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

async function loadRelationshipMetadata() {
    const hasPersonTypes = !supportsPersonalRelationships || state.relationships.personTypes.length > 0;
    const hasOrganizationRoleTypes = state.relationships.organizationRoleTypes.length > 0;
    if (hasPersonTypes && hasOrganizationRoleTypes) {
        return;
    }

    const requests = [
        donorApi.getOrganizationRoleTypes()
    ];
    if (supportsPersonalRelationships) {
        requests.unshift(donorApi.getPersonRelationshipTypes());
    }

    const responses = await Promise.all(requests);
    if (supportsPersonalRelationships) {
        const [personTypeResponse, organizationRoleResponse] = responses;
        state.relationships.personTypes = personTypeResponse?.data || [];
        state.relationships.organizationRoleTypes = organizationRoleResponse?.data || [];
    } else {
        const [organizationRoleResponse] = responses;
        state.relationships.organizationRoleTypes = organizationRoleResponse?.data || [];
    }

    populatePersonTypeOptions();
    populateOrganizationRoleOptions();
}

async function loadRelationships() {
    if (!donorId) return;

    try {
        await loadRelationshipMetadata();
        const requests = [donorApi.getOrganizationRelationships(donorId)];
        if (supportsPersonalRelationships) {
            requests.unshift(donorApi.getPersonRelationships(donorId));
        }

        const responses = await Promise.all(requests);
        if (supportsPersonalRelationships) {
            const [personResponse, organizationResponse] = responses;
            state.relationships.personItems = personResponse?.data || [];
            state.relationships.organizationItems = organizationResponse?.data || [];
        } else {
            const [organizationResponse] = responses;
            state.relationships.personItems = [];
            state.relationships.organizationItems = organizationResponse?.data || [];
        }

        renderPersonRelationshipTable(state.relationships.personItems);
        renderOrganizationRelationshipTable(state.relationships.organizationItems);
        state.relationships.loaded = true;
    } catch (error) {
        console.error("Lỗi tải mối quan hệ:", error);
        if (elements.personTableBody && supportsPersonalRelationships) {
            elements.personTableBody.innerHTML = `<tr><td class="px-6 py-8 text-center text-red-500" colspan="5">Không thể tải mối quan hệ cá nhân.</td></tr>`;
        }
        if (elements.organizationTableBody) {
            const errorMessage = donorType === "ORGANIZATION"
                ? "Không thể tải danh sách nhà hảo tâm liên quan."
                : "Không thể tải mối quan hệ tổ chức.";
            elements.organizationTableBody.innerHTML = `<tr><td class="px-6 py-8 text-center text-red-500" colspan="5">${errorMessage}</td></tr>`;
        }
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
        if (elements.auditCount) {
            elements.auditCount.textContent = pageData.totalItems || 0;
        }
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

async function handlePersonRelationshipSubmit(event) {
    event.preventDefault();

    const relationshipTypeId = Number(elements.personTypeSelect?.value || 0) || null;
    const relatedDonorId = Number(elements.personRelatedDonorId?.value || 0) || null;
    const relationshipId = Number(elements.personFormId?.value || 0) || null;
    const note = elements.personNote?.value?.trim() || null;

    if (!relationshipTypeId) {
        alert("Vui lòng chọn loại mối quan hệ.");
        return;
    }

    if (!relatedDonorId) {
        alert("Vui lòng chọn nhà hảo tâm liên quan.");
        return;
    }

    try {
        const payload = {relationshipTypeId, relatedDonorId, note};
        const response = relationshipId
            ? await donorApi.updatePersonRelationship(donorId, relationshipId, payload)
            : await donorApi.createPersonRelationship(donorId, payload);

        alert(response?.message || (relationshipId ? "Cập nhật mối quan hệ cá nhân thành công." : "Thêm mối quan hệ cá nhân thành công."));
        closePersonForm();
        await Promise.all([loadRelationships(), loadAuditSummary()]);
    } catch (error) {
        alert(error?.message || "Không thể lưu mối quan hệ cá nhân.");
    }
}

async function handleOrganizationRelationshipSubmit(event) {
    event.preventDefault();

    const roleTypeId = Number(elements.organizationRoleSelect?.value || 0) || null;
    const relatedDonorId = Number(elements.organizationRelatedDonorId?.value || 0) || null;
    const relationshipId = Number(elements.organizationFormId?.value || 0) || null;
    const note = elements.organizationNote?.value?.trim() || null;

    if (!roleTypeId) {
        alert("Vui lòng chọn vai trò trong tổ chức.");
        return;
    }

    if (!relatedDonorId) {
        alert(donorType === "ORGANIZATION" ? "Vui lòng chọn nhà hảo tâm liên quan." : "Vui lòng chọn tổ chức liên quan.");
        return;
    }

    try {
        const payload = {roleTypeId, organizationDonorId: relatedDonorId, note};
        const response = relationshipId
            ? await donorApi.updateOrganizationRelationship(donorId, relationshipId, payload)
            : await donorApi.createOrganizationRelationship(donorId, payload);

        alert(response?.message || (relationshipId ? "Cập nhật mối quan hệ tổ chức thành công." : "Thêm mối quan hệ tổ chức thành công."));
        closeOrganizationForm();
        await Promise.all([loadRelationships(), loadAuditSummary()]);
    } catch (error) {
        alert(error?.message || "Không thể lưu mối quan hệ tổ chức.");
    }
}

async function handlePersonTableAction(event) {
    const trigger = event.target.closest("button[data-action]");
    if (!trigger) return;

    const relationshipId = Number(trigger.dataset.id || 0) || null;
    if (!relationshipId) return;

    const item = state.relationships.personItems.find((row) => Number(row.id) === relationshipId);
    if (!item) return;

    if (trigger.dataset.action === "edit-person") {
        openPersonForm(item);
        return;
    }

    if (trigger.dataset.action === "deactivate-person") {
        const confirmed = window.confirm(`Ngừng dùng mối quan hệ "${item.relationshipTypeName}" với ${getPersonDisplayName(item)}?`);
        if (!confirmed) return;

        try {
            const response = await donorApi.deactivatePersonRelationship(donorId, relationshipId);
            alert(response?.message || "Đã ngừng sử dụng mối quan hệ cá nhân.");
            await Promise.all([loadRelationships(), loadAuditSummary()]);
        } catch (error) {
            alert(error?.message || "Không thể ngừng sử dụng mối quan hệ cá nhân.");
        }
    }
}

async function handleOrganizationTableAction(event) {
    const trigger = event.target.closest("button[data-action]");
    if (!trigger) return;

    const relationshipId = Number(trigger.dataset.id || 0) || null;
    if (!relationshipId) return;

    const item = state.relationships.organizationItems.find((row) => Number(row.id) === relationshipId);
    if (!item) return;

    if (trigger.dataset.action === "edit-organization") {
        openOrganizationForm(item);
        return;
    }

    if (trigger.dataset.action === "deactivate-organization") {
        const confirmed = window.confirm(`Ngừng dùng vai trò "${item.roleTypeName}" với ${getRelatedDonorDisplayName(item)}?`);
        if (!confirmed) return;

        try {
            const response = await donorApi.deactivateOrganizationRelationship(donorId, relationshipId);
            alert(response?.message || "Đã ngừng sử dụng mối quan hệ tổ chức.");
            await Promise.all([loadRelationships(), loadAuditSummary()]);
        } catch (error) {
            alert(error?.message || "Không thể ngừng sử dụng mối quan hệ tổ chức.");
        }
    }
}

function bindLookupEvents() {
    elements.personRelatedDonorSearch?.addEventListener("focus", () => loadPersonLookup(elements.personRelatedDonorSearch.value || ""));
    elements.personRelatedDonorSearch?.addEventListener("input", () => {
        elements.personRelatedDonorId.value = "";
        setSelectedMeta(elements.personRelatedDonorMeta, DEFAULT_PERSON_META, null, null);
        debouncedLoadPersonLookup(elements.personRelatedDonorSearch.value || "");
    });
    elements.personRelatedDonorDropdownList?.addEventListener("click", (event) => {
        const button = event.target.closest("button[data-person-id]");
        if (!button) return;
        selectPersonRelatedDonor(button.dataset.personId, button.dataset.personName);
    });

    elements.organizationRelatedDonorSearch?.addEventListener("focus", () => loadOrganizationLookup(elements.organizationRelatedDonorSearch.value || ""));
    elements.organizationRelatedDonorSearch?.addEventListener("input", () => {
        elements.organizationRelatedDonorId.value = "";
        setSelectedMeta(elements.organizationRelatedDonorMeta, DEFAULT_ORGANIZATION_META, null, null);
        debouncedLoadOrganizationLookup(elements.organizationRelatedDonorSearch.value || "");
    });
    elements.organizationRelatedDonorDropdownList?.addEventListener("click", (event) => {
        const button = event.target.closest("button[data-organization-id]");
        if (!button) return;
        selectOrganizationRelatedDonor(button.dataset.organizationId, button.dataset.organizationName);
    });

    document.addEventListener("click", (event) => {
        if (elements.personRelatedDonorSearchWrapper && !elements.personRelatedDonorSearchWrapper.contains(event.target)) {
            hideDropdown(elements.personRelatedDonorDropdown);
        }
        if (elements.organizationRelatedDonorSearchWrapper && !elements.organizationRelatedDonorSearchWrapper.contains(event.target)) {
            hideDropdown(elements.organizationRelatedDonorDropdown);
        }
    });
}

function bindRelationshipEvents() {
    if (supportsPersonalRelationships) {
        elements.addPersonBtn?.addEventListener("click", () => openPersonForm());
        elements.personCancelBtn?.addEventListener("click", closePersonForm);
        elements.personResetBtn?.addEventListener("click", resetPersonForm);
        elements.personForm?.addEventListener("submit", handlePersonRelationshipSubmit);
        elements.personTableBody?.addEventListener("click", handlePersonTableAction);
    }
    elements.addOrganizationBtn?.addEventListener("click", () => openOrganizationForm());
    elements.organizationCancelBtn?.addEventListener("click", closeOrganizationForm);
    elements.organizationResetBtn?.addEventListener("click", resetOrganizationForm);
    elements.organizationForm?.addEventListener("submit", handleOrganizationRelationshipSubmit);
    elements.organizationTableBody?.addEventListener("click", handleOrganizationTableAction);

    bindLookupEvents();
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
    elements.relationshipBtn?.addEventListener("click", async () => {
        setActiveTab("relationship");
        if (!state.relationships.loaded) {
            await loadRelationships();
        }
    });
    elements.auditBtn?.addEventListener("click", async () => {
        setActiveTab("audit");
        if (!state.audit.loaded) {
            await loadAuditHistory();
        }
    });

    bindRelationshipEvents();
});
