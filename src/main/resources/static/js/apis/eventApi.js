import {apiClient} from "./apiClient.js";

const BASE_URL = '/api/events';

export const eventApi = {
    // Lấy danh sách kèm filter và pagination
    getEvents: async (params) => {
        // Chuyển object { page: 0, size: 10, search: 'abc' } thành query string
        const queryString = new URLSearchParams(params).toString();
        return await apiClient.get(`${BASE_URL}?${queryString}`);
    },

    saveEvent: async (eventData) => {
        return await apiClient.post(`${BASE_URL}/save`, eventData);
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
    }
};