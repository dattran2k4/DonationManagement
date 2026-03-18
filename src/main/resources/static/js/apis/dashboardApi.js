import {apiClient} from './apiClient.js';
import {buildQuery} from '../utils/queryUtils.js';

const BASE_URL = '/api/dashboard';

export const dashboardApi = {
    getDonationTrend: async (params) => {
        const query = buildQuery(params);
        const url = query ? `${BASE_URL}/donation-trend?${query}` : `${BASE_URL}/donation-trend`;
        return await apiClient.get(url);
    }
};
