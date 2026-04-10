import {donorApi} from "../../apis/donorApi.js";
import {formatVnd} from "../../utils/currency.js";

document.addEventListener("DOMContentLoaded", () => {
    const state = {
        email: "",
        code: "",
        page: 1,
        size: 10
    };

    const emailLookupForm = document.getElementById("emailLookupForm");
    const verifyCodeForm = document.getElementById("verifyCodeForm");
    const donationHistorySection = document.getElementById("donationHistorySection");
    const donationHistoryBody = document.getElementById("donationHistoryBody");
    const emptyDonationHistory = document.getElementById("emptyDonationHistory");
    const lookupFeedback = document.getElementById("lookupFeedback");
    const resendCodeBtn = document.getElementById("resendCodeBtn");
    const sendCodeBtn = document.getElementById("sendCodeBtn");
    const verifyCodeBtn = document.getElementById("verifyCodeBtn");
    const paginationContainer = document.getElementById("lookupPagination");
    const stepCircle1 = document.getElementById("stepCircle1");
    const stepCircle2 = document.getElementById("stepCircle2");
    const stepCircle3 = document.getElementById("stepCircle3");
    const stepLabel1 = document.getElementById("stepLabel1");
    const stepLabel2 = document.getElementById("stepLabel2");
    const stepLabel3 = document.getElementById("stepLabel3");

    function showFeedback(message, isError = false) {
        lookupFeedback.textContent = message;
        lookupFeedback.classList.remove("hidden", "border-red-200", "bg-red-50", "text-red-700", "border-green-200", "bg-green-50", "text-green-700");
        if (isError) {
            lookupFeedback.classList.add("border-red-200", "bg-red-50", "text-red-700");
            return;
        }
        lookupFeedback.classList.add("border-green-200", "bg-green-50", "text-green-700");
    }

    function setBtnLoading(button, isLoading) {
        if (!button) return;
        button.disabled = isLoading;
        button.classList.toggle("opacity-70", isLoading);
        button.classList.toggle("cursor-not-allowed", isLoading);
    }

    function setStepState(circleElement, labelElement, active) {
        if (!circleElement || !labelElement) return;
        circleElement.className = active
            ? "inline-flex items-center justify-center w-8 h-8 rounded-full bg-primary text-white font-semibold"
            : "inline-flex items-center justify-center w-8 h-8 rounded-full bg-slate-200 text-slate-600 font-semibold";
        labelElement.className = active ? "text-slate-700" : "text-slate-500";
    }

    function setActiveStep(stepNumber) {
        setStepState(stepCircle1, stepLabel1, stepNumber === 1);
        setStepState(stepCircle2, stepLabel2, stepNumber === 2);
        setStepState(stepCircle3, stepLabel3, stepNumber === 3);
    }

    function formatCurrency(amount) {
        return formatVnd(amount);
    }

    function formatDateTime(dateTime) {
        if (!dateTime) return "---";
        return new Date(dateTime).toLocaleString("vi-VN", {
            hour: "2-digit",
            minute: "2-digit",
            day: "2-digit",
            month: "2-digit",
            year: "numeric"
        });
    }

    function getStatusBadge(statusLabel, status) {
        const styles = {
            PENDING_PAYMENT: "bg-yellow-100 text-yellow-800",
            PENDING_APPROVED: "bg-amber-100 text-amber-800",
            CONFIRMED: "bg-emerald-100 text-emerald-800",
            CANCELLED: "bg-slate-100 text-slate-700",
            REJECTED: "bg-red-100 text-red-700",
            FAILED: "bg-rose-100 text-rose-700"
        };

        return `<span class="inline-flex items-center rounded-full px-2.5 py-1 text-xs font-semibold ${styles[status] || "bg-slate-100 text-slate-700"}">${statusLabel || status || "---"}</span>`;
    }

    function renderDonationRows(rows) {
        donationHistoryBody.innerHTML = "";
        if (!rows || rows.length === 0) {
            emptyDonationHistory.classList.remove("hidden");
            return;
        }

        emptyDonationHistory.classList.add("hidden");
        rows.forEach((item) => {
            const row = document.createElement("tr");
            row.className = "border-b border-slate-100";

            const targetText = item?.targetLabel
                ? `${item.targetLabel}: ${item?.targetTitle || "---"}`
                : (item?.targetTitle || "---");

            const targetHtml = item?.targetUrl
                ? `<a href="${item.targetUrl}" class="text-primary font-medium hover:underline" target="_blank">${targetText}</a>`
                : `<span class="text-slate-700">${targetText}</span>`;

            row.innerHTML = `
                <td class="px-4 py-3 font-medium text-slate-800">#${item?.donationCode || `DN-${item?.donationId || "---"}`}</td>
                <td class="px-4 py-3">${targetHtml}</td>
                <td class="px-4 py-3 text-right text-slate-800 font-semibold">${formatCurrency(item?.amount)}</td>
                <td class="px-4 py-3 text-center">${getStatusBadge(item?.statusLabel, item?.status)}</td>
                <td class="px-4 py-3 text-right text-slate-600">${formatDateTime(item?.donatedAt)}</td>
            `;
            donationHistoryBody.appendChild(row);
        });
    }

    function renderPagination(pageData) {
        if (!paginationContainer) return;

        const currentPage = pageData?.page || 1;
        const totalPages = pageData?.totalPages || 0;
        const totalItems = pageData?.totalItems || 0;

        if (totalItems === 0 || totalPages <= 1) {
            paginationContainer.innerHTML = "";
            return;
        }

        const prevDisabled = currentPage <= 1;
        const nextDisabled = currentPage >= totalPages;

        paginationContainer.innerHTML = `
            <div class="flex items-center justify-between gap-3 text-sm mt-4 pt-4 border-t border-slate-200">
                <div class="text-slate-600">Trang <span class="font-semibold text-slate-900">${currentPage}</span> / ${totalPages} - Tổng ${totalItems} giao dịch</div>
                <div class="flex items-center gap-2">
                    <button type="button" data-page="${currentPage - 1}" ${prevDisabled ? "disabled" : ""}
                        class="px-3 py-1.5 rounded-lg border border-slate-300 text-slate-700 ${prevDisabled ? "opacity-50 cursor-not-allowed" : "hover:bg-slate-50"}">Trước</button>
                    <button type="button" data-page="${currentPage + 1}" ${nextDisabled ? "disabled" : ""}
                        class="px-3 py-1.5 rounded-lg border border-slate-300 text-slate-700 ${nextDisabled ? "opacity-50 cursor-not-allowed" : "hover:bg-slate-50"}">Sau</button>
                </div>
            </div>
        `;

        paginationContainer.querySelectorAll("button[data-page]").forEach((button) => {
            button.addEventListener("click", async () => {
                if (button.disabled) return;
                const nextPage = Number(button.dataset.page);
                await loadDonationHistory(nextPage);
            });
        });
    }

    async function loadDonationHistory(page = 1) {
        state.page = page;

        try {
            const response = await donorApi.getLookupDonations(state.email, state.code, {
                page: state.page,
                size: state.size
            });

            const pageData = response?.data || {};
            renderDonationRows(pageData?.data || []);
            renderPagination(pageData);
            donationHistorySection?.classList.remove("hidden");
            showFeedback("Xác thực thành công. Đã tải lịch sử quyên góp.");
            setActiveStep(3);
        } catch (error) {
            donationHistorySection?.classList.add("hidden");
            showFeedback(error?.message || "Mã xác thực không hợp lệ hoặc đã hết hạn.", true);
            setActiveStep(2);
        }
    }

    emailLookupForm?.addEventListener("submit", async (event) => {
        event.preventDefault();
        const emailInput = document.getElementById("lookupEmail");
        const email = emailInput?.value?.trim();

        if (!email) {
            showFeedback("Vui lòng nhập email hợp lệ.", true);
            return;
        }

        setBtnLoading(sendCodeBtn, true);

        try {
            const response = await donorApi.sendLookupCode(email);
            state.email = email;
            state.code = "";
            showFeedback(response?.message || "Nếu email tồn tại trong hệ thống, mã xác nhận đã được gửi.");
            verifyCodeForm?.classList.remove("hidden");
            donationHistorySection?.classList.add("hidden");
            setActiveStep(2);
            if (paginationContainer) {
                paginationContainer.innerHTML = "";
            }
        } catch (error) {
            showFeedback(error?.message || "Không thể gửi mã xác thực. Vui lòng thử lại.", true);
        } finally {
            setBtnLoading(sendCodeBtn, false);
        }
    });

    verifyCodeForm?.addEventListener("submit", async (event) => {
        event.preventDefault();
        const emailInput = document.getElementById("lookupEmail");
        const codeInput = document.getElementById("lookupCode");

        const email = emailInput?.value?.trim();
        const code = codeInput?.value?.trim();

        if (!email) {
            showFeedback("Vui lòng nhập email hợp lệ.", true);
            return;
        }

        if (!code || !/^\d{6}$/.test(code)) {
            showFeedback("Vui lòng nhập mã xác thực gồm đúng 6 chữ số.", true);
            return;
        }

        state.email = email;
        state.code = code;

        setBtnLoading(verifyCodeBtn, true);
        try {
            await loadDonationHistory(1);
        } finally {
            setBtnLoading(verifyCodeBtn, false);
        }
    });

    resendCodeBtn?.addEventListener("click", async () => {
        const emailInput = document.getElementById("lookupEmail");
        const email = emailInput?.value?.trim();

        if (!email) {
            showFeedback("Vui lòng nhập email hợp lệ.", true);
            return;
        }

        setBtnLoading(resendCodeBtn, true);
        try {
            const response = await donorApi.sendLookupCode(email);
            state.email = email;
            showFeedback(response?.message || "Nếu email tồn tại trong hệ thống, mã xác nhận đã được gửi.");
            setActiveStep(2);
        } catch (error) {
            showFeedback(error?.message || "Không thể gửi lại mã xác thực. Vui lòng thử lại.", true);
        } finally {
            setBtnLoading(resendCodeBtn, false);
        }
    });

    setActiveStep(1);
});
