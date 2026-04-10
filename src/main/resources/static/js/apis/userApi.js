import {apiClient} from "./apiClient.js";
import {buildQuery} from "../utils/queryUtils.js";

const BASE_URL = '/api/users';

export const userApi = {
    getAllUsers: async (params) => {
        const queryString = buildQuery(params);
        return await apiClient.get(`${BASE_URL}?${queryString}`);
    },
    getUserById: async (id) => {
        return await apiClient.get(`${BASE_URL}/${id}`);
    },
    createUser: async (body) => {
        return await apiClient.post(BASE_URL, body);
    },
    updateUser: async (id, body) => {
        return await apiClient.put(`${BASE_URL}/${id}`, body);
    },
    updateStatus: async (id, status) => {
        const query = new URLSearchParams({status}).toString();
        return await apiClient.patch(`${BASE_URL}/${id}/status?${query}`);
    },
    deleteUsers: async (ids) => {
        const query = ids.map((id) => `ids=${encodeURIComponent(id)}`).join("&");
        return await apiClient.delete(`${BASE_URL}?${query}`);
    }
};
