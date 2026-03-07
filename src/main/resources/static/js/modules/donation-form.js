import {donationApi} from "../apis/donationApi.js";
import {paymentApi} from "../apis/paymentApi.js";
import {createDonor} from "./donor-submit.js";

const form = document.getElementById('donationForm');
const btnSubmit = document.getElementById('submitDonation');
const receiptCheckbox = document.getElementById('needReceipt');
const receiptFields = document.getElementById('receipt-fields');

if (receiptCheckbox && receiptFields) {
    receiptCheckbox.addEventListener('change', (e) => {
        receiptFields.classList.toggle('hidden', !e.target.checked);

        // Nếu check vào mà mail donor đã có thì tự điền vào
        if (e.target.checked) {
            const donorEmail = document.getElementById('email')?.value;
            const receiptEmailInput = document.getElementById('receiptEmail');
            if (donorEmail && receiptEmailInput && !receiptEmailInput.value) {
                receiptEmailInput.value = donorEmail;
            }
        }
    });
}

const DonationFormHandler = {
    init() {
        if (!form) return;
        btnSubmit.addEventListener('click', (e) => this.handleSubmit(e));
    },

    async handleSubmit(e) {
        e.preventDefault();

        const donorType = document.querySelector('input[name="donor_type"]:checked').value;

        const formData = new FormData(form);
        const rawData = Object.fromEntries(formData.entries());

        if (!this.validate(donorType, rawData)) return;

        try {
            const donorId = await createDonor(donorType, rawData);

            const isNeedReceipt = receiptCheckbox?.checked === true;

            const parseLongOrNull = (value) => {
                if (value === undefined || value === null || value === '') return null;
                const parsed = Number(value);
                return Number.isNaN(parsed) ? null : parsed;
            };

            const donationRequest = {
                amount: Number(rawData.amount),
                message: rawData.note || null,
                needReceipt: isNeedReceipt,
                receiptName: isNeedReceipt ? (rawData.receiptName || null) : null,
                receiptEmail: isNeedReceipt ? (rawData.receiptEmail || rawData.email || null) : null,
                paymentMethod: rawData.paymentMethod,
                eventId: parseLongOrNull(rawData.eventId),
                activityId: parseLongOrNull(rawData.activityId),
                donorId: donorId
            };

            const donationRes = await donationApi.createWebDonation(donationRequest);

            if (donationRes.status === 200) {
                const memoCode = donationRes.data;

                const paymentRes = await paymentApi.createPaymentUrl(memoCode);

                if (paymentRes.status === 201) {
                    const paymentUrl = paymentRes.data;
                    window.location.href = paymentUrl;
                }
            }
        } catch (error) {
            console.error("Lỗi quy trình:", error.message);
            alert(error.message || "Có lỗi xảy ra khi xử lý quyên góp");
        }
    },

    validate(type, data) {
        if (!parseFloat(data.amount) || parseFloat(data.amount) > 10000000) return alert("Số tiền không được để trống và tối đa 10tr");
        return true;
    }
};

// Khởi tạo
document.addEventListener('DOMContentLoaded', () => DonationFormHandler.init());
