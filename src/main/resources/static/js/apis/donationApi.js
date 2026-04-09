import {apiClient} from "./apiClient.js";
import {buildQuery} from "../utils/queryUtils.js";

const BASE_URL = '/api/donations';

export const donationApi = {
    getDonations: async (params) => {
        const queryString = buildQuery(params);
        return await apiClient.get(`${BASE_URL}/list?${queryString}`);
    },
    getDonationById: async (id) => {
        return await apiClient.get(`${BASE_URL}/${id}`);
    },
    changeStatus: async (id, status) => {
        // status truyền vào cho API này nên là 'CONFIRMED'
        return await apiClient.patch(`${BASE_URL}/${id}/change-status?status=${status}`);
    },
    rejectDonation: async (id, reason) => {
        return await apiClient.patch(`${BASE_URL}/${id}/reject`, {reason});
    },
    createWebDonation: async (data) => {
        return await apiClient.post(`${BASE_URL}/donor-create`, data);
    },
    createStaffDonation: async (data) => {
        return await apiClient.post(`${BASE_URL}/staff-create`, data);
    },
    updateStaffDonation: async (id, data) => {
        return await apiClient.put(`${BASE_URL}/${id}/staff-update`, data);
    },
    submitForApproval: async (id) => {
        return await apiClient.patch(`${BASE_URL}/${id}/submit-approval`);
    },
    getDonorWall: async (params) => {
        const queryString = buildQuery(params);
        return await apiClient.get(`${BASE_URL}/donor-wall?${queryString}`);
    },
};
