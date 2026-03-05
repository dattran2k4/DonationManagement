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
    }
};