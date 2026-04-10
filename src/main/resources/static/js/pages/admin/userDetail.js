import {userApi} from "../../apis/userApi.js";

const pageEl = document.getElementById("userDetailPage");

const state = {
    userId: null,
    canEdit: window.__CAN_EDIT_USER__ === true
};

const elements = {
    formTitle: document.getElementById("formTitle"),
    saveBtn: document.getElementById("saveBtn"),
    refreshBtn: document.getElementById("refreshBtn"),
    statusToggle: document.getElementById("userStatusToggle"),
    fullNameInput: document.getElementById("fullNameInput"),
    memberCodeText: document.getElementById("memberCodeText"),
    usernameInput: document.getElementById("usernameInput"),
    phoneInput: document.getElementById("phoneInput"),
    emailInput: document.getElementById("emailInput"),
    roleSelect: document.getElementById("roleSelect"),
    createdAtText: document.getElementById("createdAtText"),
    createdByText: document.getElementById("createdByText"),
    passwordInput: document.getElementById("passwordInput"),
    passwordLabel: document.getElementById("passwordLabel")
};

const formatMemberCode = (id) => `TV-${String(id).padStart(6, "0")}`;

const formatDateTime = (value) => {
    if (!value) return "---";
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) return "---";
    return date.toLocaleString("vi-VN", {
        hour12: false,
        day: "2-digit",
        month: "2-digit",
        year: "numeric",
        hour: "2-digit",
        minute: "2-digit"
    });
};

const resetCreateForm = () => {
    if (elements.fullNameInput) elements.fullNameInput.value = "";
    if (elements.usernameInput) elements.usernameInput.value = "";
    if (elements.phoneInput) elements.phoneInput.value = "";
    if (elements.emailInput) elements.emailInput.value = "";
    if (elements.passwordInput) elements.passwordInput.value = "";
    if (elements.roleSelect) elements.roleSelect.value = "STAFF";
    if (elements.statusToggle) elements.statusToggle.checked = true;
    if (elements.memberCodeText) elements.memberCodeText.textContent = "TV-000000";
    if (elements.createdAtText) elements.createdAtText.textContent = "---";
    if (elements.createdByText) elements.createdByText.textContent = "---";
};

const setReadOnlyMode = (readonly) => {
    [
        elements.fullNameInput,
        elements.usernameInput,
        elements.phoneInput,
        elements.emailInput,
        elements.roleSelect,
        elements.statusToggle
    ].forEach((el) => {
        if (!el) return;
        el.disabled = readonly;
        el.classList.toggle("cursor-not-allowed", readonly);
    });

    if (elements.saveBtn) {
        elements.saveBtn.classList.toggle("hidden", readonly);
    }
};

const togglePasswordField = (show) => {
    const hiddenClass = "hidden";
    if (elements.passwordLabel) {
        elements.passwordLabel.classList.toggle(hiddenClass, !show);
    }
    if (elements.passwordInput) {
        elements.passwordInput.classList.toggle(hiddenClass, !show);
        elements.passwordInput.required = show;
        if (!show) elements.passwordInput.value = "";
    }
};

const fillUserDetail = (user) => {
    if (!user) return;
    if (elements.formTitle) elements.formTitle.textContent = user.fullName || "Chi tiết thành viên";
    if (elements.memberCodeText) elements.memberCodeText.textContent = user.id ? formatMemberCode(user.id) : "TV-000000";
    if (elements.fullNameInput) elements.fullNameInput.value = user.fullName || "";
    if (elements.usernameInput) elements.usernameInput.value = user.username || "";
    if (elements.phoneInput) elements.phoneInput.value = user.phone || "";
    if (elements.emailInput) elements.emailInput.value = user.email || "";
    if (elements.roleSelect && user.role) elements.roleSelect.value = user.role;
    if (elements.statusToggle) elements.statusToggle.checked = user.status !== "INACTIVE";
    if (elements.createdAtText) elements.createdAtText.textContent = formatDateTime(user.createdAt);
    if (elements.createdByText) elements.createdByText.textContent = user.createdBy || "Hệ thống";
};

const validatePayload = (payload) => {
    if (!payload.fullName) {
        alert("Vui lòng nhập tên thành viên.");
        return false;
    }
    if (!payload.phone) {
        alert("Vui lòng nhập số điện thoại.");
        return false;
    }
    if (!payload.username) {
        alert("Vui lòng nhập username.");
        return false;
    }
    if (!payload.email) {
        alert("Vui lòng nhập email.");
        return false;
    }
    if (!state.userId && (!payload.password || payload.password.length < 6)) {
        alert("Mật khẩu tối thiểu 6 ký tự.");
        return false;
    }
    return true;
};

const buildPayload = () => ({
    fullName: elements.fullNameInput?.value?.trim() || "",
    username: elements.usernameInput?.value?.trim() || "",
    phone: elements.phoneInput?.value?.trim() || "",
    email: elements.emailInput?.value?.trim() || "",
    password: elements.passwordInput?.value || "",
    role: elements.roleSelect?.value || "STAFF"
});

const saveUser = async () => {
    const payload = buildPayload();
    if (!validatePayload(payload)) return;

    try {
        if (state.userId) {
            const updatePayload = {
                fullName: payload.fullName,
                username: payload.username,
                phone: payload.phone,
                email: payload.email,
                role: payload.role
            };
            await userApi.updateUser(state.userId, updatePayload);
            window.location.href = `/admin/users/${state.userId}?saved=1`;
            return;
        }
        const response = await userApi.createUser(payload);
        const newId = response?.data;
        if (newId) {
            window.location.href = `/admin/users/${newId}?saved=1`;
            return;
        }
        window.location.href = "/admin/users";
    } catch (error) {
        alert(error?.message || "Không thể lưu thông tin thành viên.");
    }
};

const loadUserDetail = async (id) => {
    try {
        const response = await userApi.getUserById(id);
        fillUserDetail(response?.data);
        setReadOnlyMode(!state.canEdit);
        togglePasswordField(false);
    } catch (error) {
        alert(error?.message || "Không thể tải chi tiết thành viên.");
    }
};

document.addEventListener("DOMContentLoaded", () => {
    state.userId = Number(pageEl?.dataset.userId || "") || null;

    if (state.userId) {
        loadUserDetail(state.userId);
    } else {
        setReadOnlyMode(!state.canEdit);
        togglePasswordField(true);
        resetCreateForm();
        if (elements.statusToggle) {
            elements.statusToggle.checked = true;
            elements.statusToggle.disabled = true;
            elements.statusToggle.classList.add("cursor-not-allowed");
        }
    }

    elements.refreshBtn?.addEventListener("click", () => {
        if (state.userId) {
            loadUserDetail(state.userId);
            return;
        }
        resetCreateForm();
    });

    elements.saveBtn?.addEventListener("click", saveUser);

    elements.statusToggle?.addEventListener("change", async () => {
        if (!state.userId || !state.canEdit) {
            return;
        }

        const nextStatus = elements.statusToggle.checked ? "ACTIVE" : "INACTIVE";
        try {
            await userApi.updateStatus(state.userId, nextStatus);
        } catch (error) {
            elements.statusToggle.checked = !elements.statusToggle.checked;
            alert(error?.message || "Không thể cập nhật trạng thái thành viên.");
        }
    });
});
