import {apiClient} from "./apiClient.js";
import {buildQuery} from "../utils/queryUtils.js";

const BASE_URL = '/api/donors';

export const donorApi = {
    getAllDonors: async (params) => {
        const queryString = buildQuery(params);
        return await apiClient.get(`${BASE_URL}?${queryString}`);
    },
    getDonorById: async (id) => {
        return await apiClient.get(`${BASE_URL}/${id}`);
    },
    getPersonRelationshipTypes: async () => {
        return await apiClient.get(`${BASE_URL}/relationship-types/person`);
    },
    getOrganizationRoleTypes: async () => {
        return await apiClient.get(`${BASE_URL}/relationship-types/organization-roles`);
    },
    getPersonRelationships: async (id) => {
        return await apiClient.get(`${BASE_URL}/${id}/relationships/person`);
    },
    getOrganizationRelationships: async (id) => {
        return await apiClient.get(`${BASE_URL}/${id}/relationships/organizations`);
    },
    getDonorDonations: async (id, params) => {
        const queryString = buildQuery(params);
        return await apiClient.get(`${BASE_URL}/${id}/donations?${queryString}`);
    },
    sendLookupCode: async (email) => {
        return await apiClient.post(`${BASE_URL}/lookup/send-code`, {email});
    },
    getLookupDonations: async (email, code, params) => {
        const queryString = buildQuery(params);
        return await apiClient.post(`${BASE_URL}/lookup/donations?${queryString}`, {email, code});
    },
    saveIndividual: async (body) => {
        return await apiClient.post(`${BASE_URL}/individuals`, body);
    },
    updateIndividual: async (id, body) => {
        return await apiClient.put(`${BASE_URL}/${id}/individuals`, body);
    },
    saveOrganization: async (body) => {
        return await apiClient.post(`${BASE_URL}/organizations`, body);
    },
    updateOrganization: async (id, body) => {
        return await apiClient.put(`${BASE_URL}/${id}/organizations`, body);
    },
    createPersonRelationship: async (id, body) => {
        return await apiClient.post(`${BASE_URL}/${id}/relationships/person`, body);
    },
    updatePersonRelationship: async (id, relationshipId, body) => {
        return await apiClient.put(`${BASE_URL}/${id}/relationships/person/${relationshipId}`, body);
    },
    deactivatePersonRelationship: async (id, relationshipId) => {
        return await apiClient.delete(`${BASE_URL}/${id}/relationships/person/${relationshipId}`);
    },
    createOrganizationRelationship: async (id, body) => {
        return await apiClient.post(`${BASE_URL}/${id}/relationships/organizations`, body);
    },
    updateOrganizationRelationship: async (id, relationshipId, body) => {
        return await apiClient.put(`${BASE_URL}/${id}/relationships/organizations/${relationshipId}`, body);
    },
    deactivateOrganizationRelationship: async (id, relationshipId) => {
        return await apiClient.delete(`${BASE_URL}/${id}/relationships/organizations/${relationshipId}`);
    },
    deleteDonor: async (id) => {
        return await apiClient.delete(`${BASE_URL}/${id}`);
    }
};
