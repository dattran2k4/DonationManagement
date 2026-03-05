import {apiClient} from "./apiClient.js";

const BASE_URL = '/api/donations';

export const donationApi = {
    getDonations: async (params) => {
        const queryString = new URLSearchParams(params).toString();
        return await apiClient.get(`${BASE_URL}/list?${queryString}`);
    },
    changeStatus: async (id, status) => {
        // status truyền vào phải là 'CONFIRMED' hoặc 'REJECTED'
        return await apiClient.patch(`${BASE_URL}/${id}/change-status?status=${status}`);
    },
    createWebDonation: async (data) => {
        return await apiClient.post(`${BASE_URL}/donor-create`, data);
    },
};