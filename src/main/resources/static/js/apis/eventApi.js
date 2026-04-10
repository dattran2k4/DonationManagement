import {apiClient} from "./apiClient.js";
import {buildQuery} from "../utils/queryUtils.js";

const BASE_URL = '/api/events';

export const eventApi = {
    // Lấy danh sách kèm filter và pagination
    getEvents: async (params) => {
        const queryString = buildQuery(params);
        return await apiClient.get(`${BASE_URL}?${queryString}`);
    },

    createEvent: async (eventData) => {
        const payload = {...eventData};
        delete payload.id;
        return await apiClient.post(`${BASE_URL}`, payload);
    },

    updateEvent: async (id, eventData) => {
        const payload = {...eventData};
        delete payload.id;
        return await apiClient.put(`${BASE_URL}/${id}`, payload);
    },

    saveEvent: async (eventData) => {
        if (eventData?.id) {
            return await eventApi.updateEvent(eventData.id, eventData);
        }
        return await eventApi.createEvent(eventData);
    },

    // Ví dụ các API khác (bạn có thể mở rộng sau)
    deleteEvent: async (id) => {
        return await apiClient.delete(`${BASE_URL}/${id}`);
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
    },

    getEventDetailTabsSummary: async (id) => {
        return await apiClient.get(`${BASE_URL}/${id}/detail-tabs/summary`);
    },

    getEventDetailActivities: async (id, params) => {
        const queryString = buildQuery(params);
        return await apiClient.get(`${BASE_URL}/${id}/detail-tabs/activities?${queryString}`);
    },

    getEventDetailDonors: async (id, params) => {
        const queryString = buildQuery(params);
        return await apiClient.get(`${BASE_URL}/${id}/detail-tabs/donors?${queryString}`);
    },

    getEventDetailDonations: async (id, params) => {
        const queryString = buildQuery(params);
        return await apiClient.get(`${BASE_URL}/${id}/detail-tabs/donations?${queryString}`);
    }
};
