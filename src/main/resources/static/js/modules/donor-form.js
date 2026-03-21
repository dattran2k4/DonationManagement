import {createDonor} from "./donor-submit.js";
import {donorApi} from "../apis/donorApi.js";

const form = document.getElementById("donorForm");
const headerSaveBtn = document.getElementById("saveDonorHeaderBtn");
const footerSaveBtn = document.getElementById("saveDonorBtn");
const donorIdInput = document.getElementById("donorId");
const donorFormHeaderTitle = document.getElementById("donorFormHeaderTitle");

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

function fillIndividualForm(donor) {
    document.getElementById("fullName").value = donor.fullName || "";
    document.getElementById("displayName").value = donor.displayName || donor.fullName || "";
    document.getElementById("phone").value = donor.phone || "";
    document.getElementById("email").value = donor.email || "";
    document.getElementById("referralSource").value = donor.referralSource || "";
    document.getElementById("note").value = donor.note || "";
}

function fillOrganizationForm(donor) {
    const organization = donor.organization || {};
    document.getElementById("orgName").value = organization.name || donor.fullName || "";
    document.getElementById("taxCode").value = organization.taxCode || "";
    document.getElementById("representative").value = organization.representative || "";
    document.getElementById("billingAddress").value = organization.billingAddress || "";
    document.getElementById("phone").value = donor.phone || "";
    document.getElementById("email").value = donor.email || "";
    document.getElementById("referralSource").value = donor.referralSource || "";
    document.getElementById("note").value = donor.note || "";
}

async function loadDonorDetailForEdit() {
    const donorId = donorIdInput?.value;
    if (!donorId) return;

    try {
        const response = await donorApi.getDonorById(donorId);
        const donor = response?.data;
        if (!donor) return;

        if (String(donor.type) === "ORGANIZATION") {
            const orgRadio = document.getElementById("donor_org");
            if (orgRadio) orgRadio.checked = true;
            updateTabUI("ORGANIZATION");
            fillOrganizationForm(donor);
        } else {
            const individualRadio = document.getElementById("donor_personal");
            if (individualRadio) individualRadio.checked = true;
            updateTabUI("INDIVIDUAL");
            fillIndividualForm(donor);
        }

        if (donorFormHeaderTitle) {
            donorFormHeaderTitle.textContent = "Chỉnh sửa Nhà hảo tâm";
        }
    } catch (error) {
        console.error("Lỗi tải chi tiết donor:", error);
        alert(error?.message || "Không thể tải thông tin nhà hảo tâm");
    }
}

async function handleSaveDonor() {
    if (!form) return;

    const donorType = document.querySelector('input[name="donor_type"]:checked')?.value;
    const formData = new FormData(form);
    const rawData = Object.fromEntries(formData.entries());

    try {
        const donorId = await createDonor(donorType, rawData);
        if (donorId) {
            alert(rawData.id ? "Cập nhật nhà hảo tâm thành công" : "Lưu nhà hảo tâm thành công");
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

    loadDonorDetailForEdit();

    headerSaveBtn?.addEventListener("click", handleSaveDonor);
    footerSaveBtn?.addEventListener("click", handleSaveDonor);
}

document.addEventListener("DOMContentLoaded", init);
