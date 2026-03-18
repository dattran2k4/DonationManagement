import {apiClient} from './apiClient.js';

const BASE_URL = '/api/categories';

export const categoryApi = {
    getCategories: async () => {
        return await apiClient.get(BASE_URL);
    },

    saveCategory: async (categoryData) => {
        return await apiClient.post(`${BASE_URL}/save`, categoryData);
    }
};
