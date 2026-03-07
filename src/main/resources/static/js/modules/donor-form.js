import {donorApi} from "../apis/donorApi.js";

const form = document.getElementById("donorForm");
const headerSaveBtn = document.getElementById("saveDonorHeaderBtn");
const footerSaveBtn = document.getElementById("saveDonorBtn");

const individualSection = document.getElementById("individual-section");
const organizationSection = document.getElementById("organization-section");

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
        personalLabel.classList.add("bg-primary", "text-slate-900", "font-bold");
        orgLabel.classList.remove("bg-primary", "text-slate-900", "font-bold");
        toggleRequired(individualSection, true);
        toggleRequired(organizationSection, false);
    } else {
        individualSection.classList.add("hidden");
        organizationSection.classList.remove("hidden");
        orgLabel.classList.add("bg-primary", "text-slate-900", "font-bold");
        personalLabel.classList.remove("bg-primary", "text-slate-900", "font-bold");
        toggleRequired(individualSection, false);
        toggleRequired(organizationSection, true);
    }
}

function validate(rawData, donorType) {
    const phoneRegex = /^\+?[0-9\s\-()]{7,20}$/;

    if (!rawData.phone || !phoneRegex.test(rawData.phone)) {
        alert("Số điện thoại không hợp lệ");
        return false;
    }

    if (!rawData.email) {
        alert("Email không được để trống");
        return false;
    }

    if (donorType === "INDIVIDUAL") {
        if (!rawData.fullName) {
            alert("Họ và tên không được để trống");
            return false;
        }
        if (!rawData.displayName) {
            alert("Tên hiển thị không được để trống");
            return false;
        }
    } else {
        if (!rawData.name) {
            alert("Tên tổ chức không được để trống");
            return false;
        }
        if (!rawData.taxCode) {
            alert("Mã số thuế không được để trống");
            return false;
        }
        if (!rawData.representative) {
            alert("Người đại diện không được để trống");
            return false;
        }
    }

    return true;
}

async function handleSaveDonor() {
    if (!form) return;

    const donorType = document.querySelector('input[name="donor_type"]:checked')?.value;
    const formData = new FormData(form);
    const rawData = Object.fromEntries(formData.entries());

    if (!validate(rawData, donorType)) return;

    try {
        let response;
        if (donorType === "INDIVIDUAL") {
            response = await donorApi.saveIndividual(rawData);
        } else {
            response = await donorApi.saveOrganization(rawData);
        }

        if (response?.status === 200) {
            alert("Lưu nhà hảo tâm thành công");
            window.location.href = "/admin/donors";
        }
    } catch (error) {
        console.error("Lỗi khi lưu donor:", error);
        alert(error.message || "Không thể lưu nhà hảo tâm");
    }
}

function init() {
    if (!form) return;

    const donorTypeRadios = document.querySelectorAll('input[name="donor_type"]');
    donorTypeRadios.forEach((radio) => {
        radio.addEventListener("change", (e) => updateTabUI(e.target.value));
    });

    const checkedRadio = document.querySelector('input[name="donor_type"]:checked');
    if (checkedRadio) {
        updateTabUI(checkedRadio.value);
    }

    headerSaveBtn?.addEventListener("click", handleSaveDonor);
    footerSaveBtn?.addEventListener("click", handleSaveDonor);
}

document.addEventListener("DOMContentLoaded", init);
