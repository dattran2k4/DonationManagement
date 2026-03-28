import {donorApi} from "../apis/donorApi.js";
import {createDonor} from "./donor-submit.js";

const form = document.getElementById("donorForm");
const headerSaveBtn = document.getElementById("saveDonorHeaderBtn");
const footerSaveBtn = document.getElementById("saveDonorBtn");
const donorIdInput = document.getElementById("donorId");
const donorTypeRadios = document.querySelectorAll('input[name="donor_type"]');
const pageTitle = document.getElementById("donorPageTitle");
const pageDescription = document.getElementById("donorPageDescription");
const headerTitle = document.getElementById("donorHeaderTitle");
const headerSaveText = document.getElementById("saveDonorHeaderText");

const individualSection = document.getElementById("individual-section");
const organizationSection = document.getElementById("organization-section");
const referralSourceSelect = document.getElementById("referralSource");

const getDonorId = () => {
    const donorId = donorIdInput?.value?.trim();
    return donorId ? Number(donorId) : null;
};

const isEditMode = () => Boolean(getDonorId());

function toggleRequired(section, isRequired) {
    const inputs = section.querySelectorAll("input[required], input[data-required]");
    inputs.forEach((input) => {
        if (isRequired) {
            if (input.dataset.required === "true") input.required = true;
        } else if (input.required) {
            input.dataset.required = "true";
            input.required = false;
        }
    });
}

function updateTabUI(selectedValue) {
    const personalLabel = document.querySelector('label[for="donor_personal"]');
    const orgLabel = document.querySelector('label[for="donor_org"]');

    if (selectedValue === "INDIVIDUAL") {
        individualSection.classList.remove("hidden");
        organizationSection.classList.add("hidden");
        personalLabel.classList.add("bg-primary", "text-white", "font-bold");
        orgLabel.classList.remove("bg-primary", "text-white", "font-bold");
        toggleRequired(individualSection, true);
        toggleRequired(organizationSection, false);
    } else {
        individualSection.classList.add("hidden");
        organizationSection.classList.remove("hidden");
        orgLabel.classList.add("bg-primary", "text-white", "font-bold");
        personalLabel.classList.remove("bg-primary", "text-white", "font-bold");
        toggleRequired(individualSection, false);
        toggleRequired(organizationSection, true);
    }
}

function updatePageCopy() {
    const editMode = isEditMode();
    const title = editMode ? "Chỉnh sửa Nhà hảo tâm" : "Thêm Nhà hảo tâm mới";
    const subtitle = editMode
        ? "Cập nhật thông tin nhà hảo tâm và lưu lại thay đổi."
        : "Vui lòng chọn loại hình và điền đầy đủ thông tin bên dưới.";
    const saveText = editMode ? "Cập nhật thông tin" : "Lưu thông tin";
    const confirmText = editMode ? "Xác nhận cập nhật" : "Xác nhận lưu";

    if (headerTitle) headerTitle.textContent = title;
    if (pageTitle) pageTitle.textContent = "Thông tin Nhà hảo tâm";
    if (pageDescription) pageDescription.textContent = subtitle;
    if (headerSaveText) headerSaveText.textContent = saveText;
    if (footerSaveBtn) footerSaveBtn.textContent = confirmText;
    document.title = editMode ? "Chỉnh sửa Nhà hảo tâm" : "Thêm Nhà hảo tâm";
}

function setSelectValue(selectElement, value) {
    if (!selectElement) return;
    const normalizedValue = value || "";
    const existingOption = Array.from(selectElement.options).some((option) => option.value === normalizedValue);

    if (!existingOption && normalizedValue) {
        const customOption = new Option(normalizedValue, normalizedValue);
        selectElement.add(customOption);
    }

    selectElement.value = normalizedValue;
}

function fillDonorForm(donor) {
    if (!donor) return;

    const donorType = donor.type || "INDIVIDUAL";
    const donorTypeRadio = document.querySelector(`input[name="donor_type"][value="${donorType}"]`);
    if (donorTypeRadio) {
        donorTypeRadio.checked = true;
        updateTabUI(donorType);
    }

    donorTypeRadios.forEach((radio) => {
        radio.disabled = true;
    });

    document.getElementById("fullName").value = donor.fullName || "";
    document.getElementById("displayName").value = donor.displayName || "";
    document.getElementById("phone").value = donor.phone || "";
    document.getElementById("email").value = donor.email || "";
    setSelectValue(referralSourceSelect, donor.referralSource);
    document.getElementById("note").value = donor.note || "";

    if (donorType === "ORGANIZATION") {
        document.getElementById("orgName").value = donor.organization?.name || donor.fullName || "";
        document.getElementById("taxCode").value = donor.organization?.taxCode || "";
        document.getElementById("representative").value = donor.organization?.representative || "";
        document.getElementById("billingAddress").value = donor.organization?.billingAddress || "";
    }
}

async function loadDonorDetail() {
    const donorId = getDonorId();
    if (!donorId) return;

    try {
        const response = await donorApi.getDonorById(donorId);
        fillDonorForm(response.data);
    } catch (error) {
        console.error("Lỗi khi tải chi tiết donor:", error);
        alert(error.message || "Không thể tải thông tin nhà hảo tâm");
        window.location.href = "/admin/donors";
    }
}

async function handleSaveDonor() {
    if (!form) return;

    const donorType = document.querySelector('input[name="donor_type"]:checked')?.value;
    const formData = new FormData(form);
    const rawData = Object.fromEntries(formData.entries());
    const donorId = getDonorId();

    try {
        const savedDonorId = await createDonor(donorType, rawData, {donorId});
        if (savedDonorId) {
            alert(donorId ? "Cập nhật nhà hảo tâm thành công" : "Lưu nhà hảo tâm thành công");
            window.location.href = "/admin/donors";
        }
    } catch (error) {
        console.error("Lỗi khi lưu donor:", error);
        alert(error.message || "Không thể lưu nhà hảo tâm");
    }
}

function init() {
    if (!form) return;

    donorTypeRadios.forEach((radio) => {
        radio.addEventListener("change", (e) => updateTabUI(e.target.value));
    });

    const checkedRadio = document.querySelector('input[name="donor_type"]:checked');
    if (checkedRadio) {
        updateTabUI(checkedRadio.value);
    }

    updatePageCopy();
    headerSaveBtn?.addEventListener("click", handleSaveDonor);
    footerSaveBtn?.addEventListener("click", handleSaveDonor);
    loadDonorDetail();
}

document.addEventListener("DOMContentLoaded", init);
