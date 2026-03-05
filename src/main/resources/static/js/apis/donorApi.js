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
    saveIndividual: async (body) => {
        return await apiClient.post(`${BASE_URL}/individuals`, body);
    },
    saveOrganization: async (body) => {
        return await apiClient.post(`${BASE_URL}/organizations`, body);
    }
};