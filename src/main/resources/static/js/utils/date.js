export function toDateInputValue(value) {
    if (!value) return "";
    const raw = String(value).trim();
    if (!raw) return "";
    if (/^\d{4}-\d{2}-\d{2}$/.test(raw)) return raw;
    if (raw.includes("T")) return raw.split("T")[0];
    return "";
}

export function toStartOfDayLocalDateTime(dateValue) {
    const dateInput = toDateInputValue(dateValue);
    if (!dateInput) return null;
    return `${dateInput}T00:00:00`;
}

export function todayDateInputValue() {
    const now = new Date();
    const year = now.getFullYear();
    const month = String(now.getMonth() + 1).padStart(2, "0");
    const day = String(now.getDate()).padStart(2, "0");
    return `${year}-${month}-${day}`;
}

export function formatDateVi(value) {
    const dateInput = toDateInputValue(value);
    if (!dateInput) return "---";
    const [year, month, day] = dateInput.split("-");
    return `${day}/${month}/${year}`;
}
