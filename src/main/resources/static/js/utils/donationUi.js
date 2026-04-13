export const DONATION_STATUS_UI = {
    PENDING_APPROVED: {
        text: "Chờ duyệt",
        className: "donation-status-badge donation-status-pending-approved"
    },
    PENDING_PAYMENT: {
        text: "Chờ thanh toán",
        className: "donation-status-badge donation-status-pending-payment"
    },
    CONFIRMED: {
        text: "Đã xác nhận",
        className: "donation-status-badge donation-status-confirmed"
    },
    REJECTED: {
        text: "Đã từ chối",
        className: "donation-status-badge donation-status-rejected"
    },
    FAILED: {
        text: "Thất bại",
        className: "donation-status-badge donation-status-failed"
    },
    CANCELLED: {
        text: "Đã hủy",
        className: "donation-status-badge donation-status-cancelled"
    }
};

export const DONATION_PAYMENT_METHOD_LABELS = {
    CASH: "Tiền mặt",
    BANK_TRANSFER_ONLINE: "Chuyển khoản online",
    BANK_TRANSFER_OFFLINE: "Chuyển khoản"
};

export function formatDonationCode(id) {
    const numericId = Number(id || 0);
    if (!Number.isFinite(numericId) || numericId <= 0) return "DNT-00000000";
    return `DNT-${String(Math.trunc(numericId)).padStart(8, "0")}`;
}

export function getDonationStatusUi(status) {
    return DONATION_STATUS_UI[status] || DONATION_STATUS_UI.PENDING_APPROVED;
}
