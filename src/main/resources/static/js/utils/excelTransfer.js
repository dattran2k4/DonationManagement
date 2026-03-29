import {buildQuery} from './queryUtils.js';

const EXCEL_ACCEPT = '.xlsx,.xls';

const getCsrfToken = () => {
    const token = document.querySelector('meta[name="_csrf"]')?.getAttribute('content');
    const header = document.querySelector('meta[name="_csrf_header"]')?.getAttribute('content');
    return {token, header};
};

const extractFilename = (contentDisposition, fallback) => {
    if (!contentDisposition) return fallback;

    const utf8Match = contentDisposition.match(/filename\*=UTF-8''([^;]+)/i);
    if (utf8Match?.[1]) {
        return decodeURIComponent(utf8Match[1]);
    }

    const plainMatch = contentDisposition.match(/filename="?([^"]+)"?/i);
    if (plainMatch?.[1]) {
        return plainMatch[1];
    }

    return fallback;
};

const parseErrorResponse = async (response) => {
    const contentType = response.headers.get('content-type') || '';

    if (contentType.includes('application/json')) {
        const data = await response.json().catch(() => ({}));
        return data.message || `HTTP error! status: ${response.status}`;
    }

    const text = await response.text().catch(() => '');
    return text || `HTTP error! status: ${response.status}`;
};

const triggerDownload = (blob, filename) => {
    const downloadUrl = window.URL.createObjectURL(blob);
    const anchor = document.createElement('a');
    anchor.href = downloadUrl;
    anchor.download = filename;
    document.body.appendChild(anchor);
    anchor.click();
    anchor.remove();
    window.URL.revokeObjectURL(downloadUrl);
};

export const bindExcelActions = ({
    exportButton,
    importButton,
    importInput,
    exportUrl,
    importUrl,
    getExportParams,
    fallbackFilename,
    successExportMessage,
    onImportSuccess
}) => {
    if (exportButton) {
        exportButton.addEventListener('click', async () => {
            try {
                const params = typeof getExportParams === 'function' ? getExportParams() : {};
                const queryString = buildQuery(params || {});
                const response = await fetch(queryString ? `${exportUrl}?${queryString}` : exportUrl, {
                    method: 'GET',
                    credentials: 'same-origin'
                });

                if (!response.ok) {
                    throw new Error(await parseErrorResponse(response));
                }

                const filename = extractFilename(
                    response.headers.get('content-disposition'),
                    fallbackFilename || 'du-lieu.xlsx'
                );

                const blob = await response.blob();
                triggerDownload(blob, filename);
                alert(successExportMessage || 'Xuất Excel thành công.');
            } catch (error) {
                alert(error?.message || 'Không thể xuất file Excel.');
            }
        });
    }

    if (importButton && importInput) {
        importInput.setAttribute('accept', EXCEL_ACCEPT);

        importButton.addEventListener('click', () => {
            importInput.click();
        });

        importInput.addEventListener('change', async (event) => {
            const file = event.target.files?.[0];
            if (!file) return;

            const formData = new FormData();
            formData.append('file', file);

            const csrf = getCsrfToken();
            const headers = {};
            if (csrf.token && csrf.header) {
                headers[csrf.header] = csrf.token;
            }

            try {
                const response = await fetch(importUrl, {
                    method: 'POST',
                    body: formData,
                    headers,
                    credentials: 'same-origin'
                });

                if (!response.ok) {
                    throw new Error(await parseErrorResponse(response));
                }

                const data = await response.json();
                alert(data?.message || 'Nhập Excel thành công.');

                if (typeof onImportSuccess === 'function') {
                    onImportSuccess(data);
                }
            } catch (error) {
                alert(error?.message || 'Không thể nhập file Excel.');
            } finally {
                importInput.value = '';
            }
        });
    }
};
