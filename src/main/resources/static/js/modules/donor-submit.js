import {donorApi} from "../apis/donorApi.js";

const phoneRegex = /^\+?[0-9\s\-()]{7,20}$/;

function isEmpty(value) {
    return value === undefined || value === null || String(value).trim() === "";
}

function normalize(rawData) {
    return Object.fromEntries(
        Object.entries(rawData || {}).map(([key, value]) => [key, typeof value === "string" ? value.trim() : value])
    );
}

function validateCommon(data, {requireEmail = true} = {}) {
    if (isEmpty(data.phone) || !phoneRegex.test(data.phone)) {
        return "Số điện thoại không hợp lệ";
    }

    if (requireEmail && isEmpty(data.email)) {
        return "Email không được để trống";
    }

    return null;
}

function validateIndividual(data, options) {
    const commonError = validateCommon(data, options);
    if (commonError) return commonError;

    if (isEmpty(data.fullName)) {
        return "Họ và tên không được để trống";
    }

    return null;
}

function validateOrganization(data, options) {
    const commonError = validateCommon(data, options);
    if (commonError) return commonError;

    if (isEmpty(data.name)) {
        return "Tên tổ chức không được để trống";
    }

    if (isEmpty(data.taxCode)) {
        return "Mã số thuế không được để trống";
    }

    if (isEmpty(data.representative)) {
        return "Người đại diện không được để trống";
    }

    return null;
}

export function validateDonorData(donorType, rawData, options = {}) {
    const data = normalize(rawData);

    if (donorType === "INDIVIDUAL") {
        return validateIndividual(data, options);
    }

    if (donorType === "ORGANIZATION") {
        return validateOrganization(data, options);
    }

    return "Loại nhà hảo tâm không hợp lệ";
}

export function buildDonorPayload(donorType, rawData) {
    const data = normalize(rawData);

    if (donorType === "INDIVIDUAL") {
        return {
            fullName: data.fullName,
            displayName: isEmpty(data.displayName) ? data.fullName : data.displayName,
            phone: data.phone,
            email: isEmpty(data.email) ? null : data.email,
            note: isEmpty(data.note) ? null : data.note,
            referralSource: isEmpty(data.referralSource) ? null : data.referralSource
        };
    }

    if (donorType === "ORGANIZATION") {
        return {
            name: data.name,
            taxCode: data.taxCode,
            representative: data.representative,
            phone: data.phone,
            email: data.email,
            billingAddress: isEmpty(data.billingAddress) ? null : data.billingAddress,
            note: isEmpty(data.note) ? null : data.note,
            referralSource: isEmpty(data.referralSource) ? null : data.referralSource
        };
    }

    throw new Error("Loại nhà hảo tâm không hợp lệ");
}

export async function createDonor(donorType, rawData, options = {}) {
    const validationError = validateDonorData(donorType, rawData, options);
    if (validationError) {
        throw new Error(validationError);
    }

    const payload = buildDonorPayload(donorType, rawData);

    const response = donorType === "INDIVIDUAL"
        ? await donorApi.saveIndividual(payload)
        : await donorApi.saveOrganization(payload);

    if (!response || response.status !== 200 || response.data == null) {
        throw new Error(response?.message || "Không thể tạo nhà hảo tâm");
    }

    return response.data;
}
