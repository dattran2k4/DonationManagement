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

    const raw = String(value || "")
        .toLowerCase()
        .replaceAll('vnđ', "")
        .replaceAll(/\s/g, "");

    if (!raw) return 0;

    const groupedThousandsRegex = /^\d{1,3}(\.\d{3})+$/;
    const decimalDotRegex = /^\d+\.\d{1,2}$/;
    const decimalCommaRegex = /^\d+,\d{1,2}$/;
    const digitsOnlyRegex = /^\d+$/;

    let normalized = raw;
    if (groupedThousandsRegex.test(raw)) {
        normalized = raw.replaceAll(".", "");
    } else if (decimalDotRegex.test(raw)) {
        normalized = raw;
    } else if (decimalCommaRegex.test(raw)) {
        normalized = raw.replace(",", ".");
    } else if (digitsOnlyRegex.test(raw)) {
        normalized = raw;
    } else {
        normalized = raw.replaceAll(/\D/g, "");
    }

    const numeric = Number(normalized);
    if (Number.isFinite(numeric)) return Math.round(numeric);

    const fallback = Number(raw.replaceAll(/\D/g, ""));
    return Number.isFinite(fallback) ? fallback : 0;
}
