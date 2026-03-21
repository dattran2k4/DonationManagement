import {apiClient} from "./apiClient.js";
import {buildQuery} from "../utils/queryUtils.js";

const BASE_URL = '/api/donors';

export const donorApi = {
    getAllDonors: async (params) => {
        // Chuyển đổi object params thành query string (page, size, search, type)
        const queryString = buildQuery(params);
        return await apiClient.get(`${BASE_URL}?${queryString}`);
    },
    getDonorById: async (id) => {
        return await apiClient.get(`${BASE_URL}/${id}`);
    },
    getDonorDonations: async (id, params) => {
        const queryString = buildQuery(params);
        return await apiClient.get(`/api/donor/${id}/donations?${queryString}`);
    },
    saveIndividual: async (body) => {
        return await apiClient.post(`${BASE_URL}/individuals`, body);
    },
    updateIndividual: async (id, body) => {
        return await apiClient.put(`${BASE_URL}/${id}/individuals`, body);
    },
    saveOrganization: async (body) => {
        return await apiClient.post(`${BASE_URL}/organizations`, body);
    },
    updateOrganization: async (id, body) => {
        return await apiClient.put(`${BASE_URL}/${id}/organizations`, body);
    }
};
