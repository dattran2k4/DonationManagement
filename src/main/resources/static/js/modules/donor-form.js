import {createDonor} from "./donor-submit.js";

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

async function handleSaveDonor() {
    if (!form) return;

    const donorType = document.querySelector('input[name="donor_type"]:checked')?.value;
    const formData = new FormData(form);
    const rawData = Object.fromEntries(formData.entries());

    try {
        const donorId = await createDonor(donorType, rawData);
        if (donorId) {
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
