import {apiClient} from "./apiClient.js";
import {buildQuery} from "../utils/queryUtils.js";

const BASE_URL = "/api/audit-logs";

export const auditLogApi = {
    getAuditLogs: async (params) => {
        const queryString = buildQuery(params || {});
        return await apiClient.get(`${BASE_URL}?${queryString}`);
    }
};
