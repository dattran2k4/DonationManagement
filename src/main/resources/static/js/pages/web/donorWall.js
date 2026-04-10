import {donationApi} from "../../apis/donationApi.js";
import {formatVnd} from "../../utils/currency.js";

const state = {
    period: "MONTH",
    year: null,
    month: null,
    quarter: null
};

const elements = {
    monthBtn: document.getElementById("periodMonthBtn"),
    quarterBtn: document.getElementById("periodQuarterBtn"),
    yearSelect: document.getElementById("donorWallYearSelect"),
    monthSelect: document.getElementById("donorWallMonthSelect"),
    quarterSelect: document.getElementById("donorWallQuarterSelect"),
    reloadBtn: document.getElementById("reloadDonorWallBtn"),
    periodLabel: document.getElementById("donorWallPeriodLabel"),
    fromDate: document.getElementById("donorWallFromDate"),
    toDate: document.getElementById("donorWallToDate"),
    tableBody: document.getElementById("donorWallTableBody"),
    emptyState: document.getElementById("donorWallEmptyState")
};

const formatMoney = (amount) => formatVnd(amount);

const formatDate = (value) => {
    if (!value) return "---";
    if (typeof value === "string" && value.includes("-")) {
        const parts = value.split("-");
        if (parts.length === 3) {
            return `${parts[2]}/${parts[1]}/${parts[0]}`;
        }
    }
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) return value;
    return date.toLocaleDateString("vi-VN");
};

const getRankBadgeClass = (rank) => {
    if (rank === 1) return "bg-amber-100 text-amber-700";
    if (rank === 2) return "bg-slate-200 text-slate-700";
    if (rank === 3) return "bg-orange-100 text-orange-700";
    return "bg-slate-100 text-slate-700";
};

function updatePeriodButtons() {
    const isMonth = state.period === "MONTH";
    elements.monthBtn.className = isMonth
        ? "inline-flex items-center rounded-lg bg-primary px-4 py-2 text-sm font-semibold text-white"
        : "inline-flex items-center rounded-lg border border-slate-300 bg-white px-4 py-2 text-sm font-semibold text-slate-700 hover:bg-slate-50";
    elements.quarterBtn.className = !isMonth
        ? "inline-flex items-center rounded-lg bg-primary px-4 py-2 text-sm font-semibold text-white"
        : "inline-flex items-center rounded-lg border border-slate-300 bg-white px-4 py-2 text-sm font-semibold text-slate-700 hover:bg-slate-50";

    if (elements.monthSelect && elements.quarterSelect) {
        elements.monthSelect.classList.toggle("hidden", !isMonth);
        elements.quarterSelect.classList.toggle("hidden", isMonth);
    }
}

function populateYearOptions() {
    if (!elements.yearSelect) return;
    const now = new Date();
    const currentYear = now.getFullYear();
    const startYear = currentYear - 4;
    const endYear = currentYear + 1;

    elements.yearSelect.innerHTML = "";
    for (let year = endYear; year >= startYear; year--) {
        const option = document.createElement("option");
        option.value = String(year);
        option.textContent = `Năm ${year}`;
        elements.yearSelect.appendChild(option);
    }
}

function syncStateFromSelectors() {
    const selectedYear = Number(elements.yearSelect?.value);
    const selectedMonth = Number(elements.monthSelect?.value);
    const selectedQuarter = Number(elements.quarterSelect?.value);

    if (!Number.isNaN(selectedYear)) {
        state.year = selectedYear;
    }

    if (state.period === "MONTH") {
        if (!Number.isNaN(selectedMonth)) {
            state.month = selectedMonth;
        }
    } else {
        if (!Number.isNaN(selectedQuarter)) {
            state.quarter = selectedQuarter;
            state.month = (selectedQuarter - 1) * 3 + 1;
        }
    }
}

function renderRows(donors) {
    if (!donors || donors.length === 0) {
        elements.tableBody.innerHTML = "";
        elements.emptyState.classList.remove("hidden");
        return;
    }

    elements.emptyState.classList.add("hidden");
    elements.tableBody.innerHTML = donors.map((donor) => `
        <tr class="hover:bg-slate-50 transition-colors">
            <td class="px-6 py-4">
                <span class="inline-flex min-w-9 items-center justify-center rounded-full px-2.5 py-1 text-sm font-bold ${getRankBadgeClass(donor.rank)}">
                    #${donor.rank}
                </span>
            </td>
            <td class="px-6 py-4 text-sm font-semibold text-slate-900">${donor.displayName || "Nhà hảo tâm ẩn danh"}</td>
            <td class="px-6 py-4 text-right text-sm font-semibold text-slate-900">${formatMoney(donor.totalAmount)}</td>
            <td class="px-6 py-4 text-right text-sm text-slate-600">${donor.donationCount || 0}</td>
        </tr>
    `).join("");
}

async function loadDonorWall() {
    try {
        syncStateFromSelectors();
        const response = await donationApi.getDonorWall({
            period: state.period,
            year: state.year,
            month: state.month
        });

        const data = response?.data || {};
        elements.periodLabel.textContent = data.periodLabel || "---";
        elements.fromDate.textContent = formatDate(data.fromDate);
        elements.toDate.textContent = formatDate(data.toDate);
        renderRows(data.donors || []);
    } catch (error) {
        console.error("Không thể tải bảng vàng tri ân:", error);
        elements.tableBody.innerHTML = "";
        elements.emptyState.classList.remove("hidden");
        elements.emptyState.textContent = error?.message || "Không thể tải dữ liệu bảng vàng tri ân.";
    }
}

document.addEventListener("DOMContentLoaded", async () => {
    const now = new Date();
    const currentYear = now.getFullYear();
    const currentMonth = now.getMonth() + 1;
    const currentQuarter = Math.floor((currentMonth - 1) / 3) + 1;

    populateYearOptions();

    if (elements.yearSelect) {
        elements.yearSelect.value = String(currentYear);
    }
    if (elements.monthSelect) {
        elements.monthSelect.value = String(currentMonth);
    }
    if (elements.quarterSelect) {
        elements.quarterSelect.value = String(currentQuarter);
    }

    state.year = currentYear;
    state.month = currentMonth;
    state.quarter = currentQuarter;

    updatePeriodButtons();

    elements.monthBtn?.addEventListener("click", async () => {
        state.period = "MONTH";
        updatePeriodButtons();
        await loadDonorWall();
    });

    elements.quarterBtn?.addEventListener("click", async () => {
        state.period = "QUARTER";
        updatePeriodButtons();
        await loadDonorWall();
    });

    elements.yearSelect?.addEventListener("change", async () => {
        await loadDonorWall();
    });

    elements.monthSelect?.addEventListener("change", async () => {
        await loadDonorWall();
    });

    elements.quarterSelect?.addEventListener("change", async () => {
        await loadDonorWall();
    });

    elements.reloadBtn?.addEventListener("click", async () => {
        await loadDonorWall();
    });

    await loadDonorWall();
});
