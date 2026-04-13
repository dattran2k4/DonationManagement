import {donationApi} from "../apis/donationApi.js";
import {paymentApi} from "../apis/paymentApi.js";
import {createDonor} from "./donor-submit.js";

const form = document.getElementById('donationForm');
const btnSubmit = document.getElementById('submitDonation');
const receiptCheckbox = document.getElementById('needReceipt');
const receiptFields = document.getElementById('receipt-fields');
const amountInput = document.getElementById('donationAmount');
const WHOLE_AMOUNT_MESSAGE = "Vui lòng nhập tiền chẳn";

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
        if (amountInput) {
            amountInput.addEventListener('change', () => this.handleAmountChange());
        }
        btnSubmit.addEventListener('click', (e) => this.handleSubmit(e));
    },

    handleAmountChange() {
        if (!amountInput) return;

        const rawAmount = String(amountInput.value ?? '').trim();
        if (!rawAmount) return;

        const amount = Number(rawAmount);
        if (!Number.isFinite(amount) || Number.isInteger(amount)) return;

        alert(WHOLE_AMOUNT_MESSAGE);
        amountInput.value = '';
        amountInput.focus();
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
        const amount = Number(data.amount);
        if (!Number.isFinite(amount)) {
            return alert("Số tiền phải từ 1.000 đồng đến tối đa 10.000.000 đồng");
        }
        if (!Number.isInteger(amount)) {
            return alert(WHOLE_AMOUNT_MESSAGE);
        }
        if (!amount || amount < 1000 || amount > 100000000) {
            return alert("Số tiền phải từ 1.000 đồng đến tối đa 100.000.000 đồng");
        }
        return true;
    }
};

// Khởi tạo
document.addEventListener('DOMContentLoaded', () => DonationFormHandler.init());
