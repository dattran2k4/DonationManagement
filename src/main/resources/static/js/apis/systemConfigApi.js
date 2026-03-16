import {apiClient} from './apiClient.js';

const BASE_URL = '/api/configs';

const getCsrf = () => {
    const token = document.querySelector('meta[name="_csrf"]')?.getAttribute('content');
    const header = document.querySelector('meta[name="_csrf_header"]')?.getAttribute('content');
    return {token, header};
};

export const systemConfigApi = {
    getConfigMap: async () => {
        return await apiClient.get(`${BASE_URL}/map`);
    },
    saveConfigs: async (payload) => {
        return await apiClient.put(BASE_URL, payload);
    },
    uploadImage: async (file) => {
        const formData = new FormData();
        formData.append('file', file);

        const csrf = getCsrf();
        const headers = {};
        if (csrf.token && csrf.header) {
            headers[csrf.header] = csrf.token;
        }

        const response = await fetch(`${BASE_URL}/upload-image`, {
            method: 'POST',
            body: formData,
            headers,
            credentials: 'same-origin'
        });

        if (!response.ok) {
            const errorData = await response.json().catch(() => ({}));
            throw new Error(errorData.message || 'Không thể tải ảnh lên.');
        }

        return await response.json();
    }
};
