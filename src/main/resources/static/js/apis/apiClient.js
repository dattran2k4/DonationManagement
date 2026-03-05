const getCsrfToken = () => {
    const token = document.querySelector('meta[name="_csrf"]')?.getAttribute('content');
    const header = document.querySelector('meta[name="_csrf_header"]')?.getAttribute('content');
    return {token, header};
};

// Hàm fetch wrapper cơ bản
const request = async (url, options = {}) => {
    const headers = {
        'Content-Type': 'application/json',
        'Accept': 'application/json',
        ...options.headers
    };

    // Tự động gắn CSRF token cho các method không phải GET
    const method = options.method ? options.method.toUpperCase() : 'GET';
    if (method !== 'GET') {
        const csrf = getCsrfToken();
        if (csrf.token && csrf.header) {
            headers[csrf.header] = csrf.token;
        }
    }

    try {
        const response = await fetch(url, {
            ...options,
            headers,
            credentials: 'same-origin' // Quan trọng cho Spring Security Session
        });

        // Xử lý lỗi HTTP (VD: 401 hết session, 403 không có quyền)
        if (!response.ok) {
            if (response.status === 401) {
                window.location.href = '/login'; // Chuyển hướng nếu mất session
                return;
            }
            const errorData = await response.json().catch(() => ({}));
            throw new Error(errorData.message || `HTTP error! status: ${response.status}`);
        }

        // Nếu trả về no-content (204)
        if (response.status === 204) return null;

        return await response.json();
    } catch (error) {
        console.error('API Request Failed:', error);
        throw error; // Ném lỗi ra để trang cụ thể tự xử lý (hiển thị Toast/Alert)
    }
};

export const apiClient = {
    get: (url) => request(url, {method: 'GET'}),
    post: (url, data) => request(url, {method: 'POST', body: JSON.stringify(data)}),
    put: (url, data) => request(url, {method: 'PUT', body: JSON.stringify(data)}),
    patch: (url, data) => request(url, {method: 'PATCH', body: data ? JSON.stringify(data) : undefined}),
    delete: (url) => request(url, {method: 'DELETE'})
};