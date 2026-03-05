import {apiClient} from "./apiClient.js";

const BASE_URL = '/api/payments';

export const paymentApi = {
    createPaymentUrl: async (donationMemoCode) => {
        return await apiClient.post(`${BASE_URL}`, {
            donationMemoCode: donationMemoCode
        });
    }
};