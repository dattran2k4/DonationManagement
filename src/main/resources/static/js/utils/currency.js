export function formatNumberVi(value) {
    const amount = Number(value || 0);
    if (!Number.isFinite(amount)) return "0";
    return Math.round(amount).toLocaleString("vi-VN");
}

export function formatVnd(value) {
    return `${formatNumberVi(value)} vnđ`;
}

export function parseVndInput(value) {
    if (typeof value === "number") {
        return Number.isFinite(value) ? Math.round(value) : 0;
    }

    let normalized = String(value || "")
        .toLowerCase()
        .replaceAll('vnđ', "")
        .replaceAll(/\s/g, "");

    if (!normalized) return 0;

    const dotCount = (normalized.match('.') || []).length;
    const commaCount = (normalized.match(/,/g) || []).length;

    if (dotCount > 1 && commaCount === 0) {
        normalized = normalized.replaceAll('.', "");
    } else if (commaCount > 1 && dotCount === 0) {
        normalized = normalized.replaceAll(',', "");
    } else if (commaCount === 1 && dotCount === 0) {
        normalized = normalized.replaceAll(",", ".");
    }

    const numeric = Number(normalized);
    if (Number.isFinite(numeric)) return Math.round(numeric);

    const fallback = Number(String(value || "").replaceAll(/\D/g, ""));
    return Number.isFinite(fallback) ? fallback : 0;
}
