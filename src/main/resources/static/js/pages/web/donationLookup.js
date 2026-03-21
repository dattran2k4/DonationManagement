document.addEventListener("DOMContentLoaded", () => {
    const emailLookupForm = document.getElementById("emailLookupForm");
    const verifyCodeForm = document.getElementById("verifyCodeForm");
    const donationHistorySection = document.getElementById("donationHistorySection");
    const donationHistoryBody = document.getElementById("donationHistoryBody");
    const emptyDonationHistory = document.getElementById("emptyDonationHistory");
    const lookupFeedback = document.getElementById("lookupFeedback");
    const resendCodeBtn = document.getElementById("resendCodeBtn");

    function showFeedback(message, isError = false) {
        lookupFeedback.textContent = message;
        lookupFeedback.classList.remove("hidden", "border-red-200", "bg-red-50", "text-red-700", "border-green-200", "bg-green-50", "text-green-700");
        if (isError) {
            lookupFeedback.classList.add("border-red-200", "bg-red-50", "text-red-700");
            return;
        }
        lookupFeedback.classList.add("border-green-200", "bg-green-50", "text-green-700");
    }

    function renderDemoHistory() {
        donationHistoryBody.innerHTML = "";
        emptyDonationHistory.classList.add("hidden");

        const demoRows = [
            {code: "DN-2026-0001", target: "Sự kiện: Nâng bước đến trường", amount: 500000, status: "ĐÃ XÁC NHẬN", time: "20/03/2026 10:30"},
            {code: "DN-2026-0002", target: "Hoạt động: Tặng sách vùng cao", amount: 300000, status: "CHỜ XỬ LÝ", time: "21/03/2026 08:15"}
        ];

        demoRows.forEach((item) => {
            const row = document.createElement("tr");
            row.className = "border-b border-slate-100";
            row.innerHTML = `
                <td class="px-4 py-3 font-medium text-slate-800">${item.code}</td>
                <td class="px-4 py-3 text-slate-700">${item.target}</td>
                <td class="px-4 py-3 text-right text-slate-800">${Number(item.amount).toLocaleString("vi-VN")} VND</td>
                <td class="px-4 py-3 text-center text-slate-700">${item.status}</td>
                <td class="px-4 py-3 text-right text-slate-600">${item.time}</td>
            `;
            donationHistoryBody.appendChild(row);
        });
    }

    emailLookupForm?.addEventListener("submit", (event) => {
        event.preventDefault();
        const emailInput = document.getElementById("lookupEmail");
        const email = emailInput?.value?.trim();

        if (!email) {
            showFeedback("Vui lòng nhập email hợp lệ.", true);
            return;
        }

        showFeedback("Đã gửi mã xác thực về email của bạn. Vui lòng kiểm tra hộp thư.");
        verifyCodeForm?.classList.remove("hidden");
        donationHistorySection?.classList.add("hidden");
    });

    verifyCodeForm?.addEventListener("submit", (event) => {
        event.preventDefault();
        const codeInput = document.getElementById("lookupCode");
        const code = codeInput?.value?.trim();

        if (!code || code.length < 6) {
            showFeedback("Vui lòng nhập mã xác thực 6 số.", true);
            return;
        }

        showFeedback("Xác thực thành công. Đây là dữ liệu demo, sẽ nối API tra cứu ở bước tiếp theo.");
        donationHistorySection?.classList.remove("hidden");
        renderDemoHistory();
    });

    resendCodeBtn?.addEventListener("click", () => {
        showFeedback("Đã gửi lại mã xác thực. Vui lòng kiểm tra email.");
    });
});
