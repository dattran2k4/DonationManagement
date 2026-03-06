import {apiClient} from "./apiClient.js";
import {buildQuery} from "../utils/queryUtils.js";

const BASE_URL = '/api/activities';

export const activityApi = {
    getAllActivities: async (params) => {
        const queryString = buildQuery(params);
        return await apiClient.get(`${BASE_URL}?${queryString}`);
    },

    saveActivity: async (activityData) => {
        return await apiClient.post(`${BASE_URL}/save`, activityData);
    },

    uploadThumbnail: async (file, id = null) => {
        const formData = new FormData();
        formData.append('file', file);

        const url = id ? `${BASE_URL}/${id}/upload` : `${BASE_URL}/upload`

        const response = await fetch(url, {
            method: 'POST',
            body: formData
        });
        return response.json();
    }
};