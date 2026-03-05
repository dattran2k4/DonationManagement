import {donorApi} from "../apis/donorApi.js";
import {donationApi} from "../apis/donationApi.js";
import {paymentApi} from "../apis/paymentApi.js";

const form = document.getElementById('donationForm');
const btnSubmit = document.getElementById('submitDonation');
const receiptCheckbox = document.getElementById('needReceipt');
const receiptFields = document.getElementById('receipt-fields');

receiptCheckbox.addEventListener('change', (e) => {
    receiptFields.classList.toggle('hidden', !e.target.checked);

    // Nếu check vào mà mail donor đã có thì tự điền vào
    if (e.target.checked) {
        const donorEmail = document.getElementById('email').value;
        const receiptEmailInput = document.getElementById('receiptEmail');
        if (donorEmail && !receiptEmailInput.value) {
            receiptEmailInput.value = donorEmail;
        }
    }
});

const DonationFormHandler = {
    init() {
        if (!form) return;
        btnSubmit.addEventListener('click', (e) => this.handleSubmit(e));
    },

    async handleSubmit(e) {
        e.preventDefault();

        const donorType = document.querySelector('input[name="donor_type"]:checked').value;

        console.log(donorType)

        const formData = new FormData(form);
        const rawData = Object.fromEntries(formData.entries());

        console.log('Form data: ', formData)
        console.log('Raw data: ', rawData)

        if (!this.validate(donorType, rawData)) return;

        try {
            let donorRes;
            if (donorType === 'INDIVIDUAL') {
                donorRes = await donorApi.saveIndividual(rawData);
            } else {
                donorRes = await donorApi.saveOrganization(rawData);
            }

            if (donorRes.status === 200) {
                console.log("Donor saved id:! " + donorRes.data);

                const donationRequest = {
                    amount: rawData.amount,
                    message: rawData.message,
                    needReceipt: rawData.needReceipt,
                    receiptName: rawData.receiptName,
                    receiptEmail: rawData.receiptEmail,
                    paymentMethod: rawData.paymentMethod,
                    eventId: rawData.eventId,
                    donorId: donorRes.data
                };

                const donationRes = await donationApi.createWebDonation(donationRequest);

                if (donationRes.status === 200) {
                    console.log(donationRes)

                    const memoCode = donationRes.data;

                    const paymentRes = await paymentApi.createPaymentUrl(memoCode);

                    if (paymentRes.status === 201) {
                        console.log(paymentRes)
                        const paymentUrl = paymentRes.data;
                        console.log(paymentUrl)

                        window.location.href = paymentUrl;
                    }
                }
            }
        } catch (error) {
            console.log(error)
            console.log(error.message)
            console.error("Lỗi quy trình:", error.message);
        }
    },

    validate(type, data) {
        const phoneRegex = /^\+?[0-9\s\-()]{7,20}$/;

        if (!parseFloat(data.amount) || parseFloat(data.amount) > 10000000) return alert("Số tiền không được để trống và tối đa 10tr");

        if (type === 'INDIVIDUAL') {
            if (!data.fullName) return alert("Họ tên không được để trống");
            if (!phoneRegex.test(data.phone)) return alert("Số điện thoại không hợp lệ");
        } else {
            if (!data.name) return alert("Tên tổ chức không được để trống");
            if (!data.taxCode) return alert("Mã số thuế không được để trống");
            if (!data.email) return alert("Email tổ chức không được để trống");
        }

        return true;
    }
};

// Khởi tạo
document.addEventListener('DOMContentLoaded', () => DonationFormHandler.init());