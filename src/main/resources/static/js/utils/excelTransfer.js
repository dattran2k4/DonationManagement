import {buildQuery} from './queryUtils.js';
import {openAdminSummaryModal, showAdminToast} from './adminNotifications.js';

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
        return data.message || data.error || `HTTP error! status: ${response.status}`;
    }

    const text = await response.text().catch(() => '');
    return text || `HTTP error! status: ${response.status}`;
};

const setButtonLoading = (button, isLoading, loadingText) => {
    if (!button) return;

    if (!button.dataset.originalHtml) {
        button.dataset.originalHtml = button.innerHTML;
    }

    if (isLoading) {
        button.disabled = true;
        button.innerHTML = `
            <span class="material-symbols-outlined text-[18px]">progress_activity</span>
            <span>${loadingText}</span>
        `;
        return;
    }

    button.disabled = false;
    if (button.dataset.originalHtml) {
        button.innerHTML = button.dataset.originalHtml;
    }
};

const triggerDownload = (blob, filename) => new Promise((resolve, reject) => {
    try {
        const downloadUrl = window.URL.createObjectURL(blob);
        const anchor = document.createElement('a');
        anchor.href = downloadUrl;
        anchor.download = filename;
        anchor.style.display = 'none';
        document.body.appendChild(anchor);

        requestAnimationFrame(() => {
            anchor.click();
            anchor.remove();
            window.setTimeout(() => {
                window.URL.revokeObjectURL(downloadUrl);
                resolve();
            }, 0);
        });
    } catch (error) {
        reject(error);
    }
});

const buildErrorReportUrl = (token, customBuilder) => {
    if (!token) return null;
    if (typeof customBuilder === 'function') {
        return customBuilder(token);
    }
    return `/api/admin/excel/error-reports/${encodeURIComponent(token)}`;
};

const showImportResult = ({moduleLabel, payload, result, errorReportUrlBuilder}) => {
    const summaryTitle = `Kết quả nhập Excel ${moduleLabel}`;
    const subtitle = result?.message || payload?.message || 'Hệ thống đã xử lý xong file import.';
    const totalRows = result?.totalRows || 0;
    const successCount = result?.successCount || 0;
    const failureCount = result?.failureCount || 0;
    const errorDetails = result?.errorDetails || [];
    const downloadUrl = buildErrorReportUrl(result?.errorReportToken, errorReportUrlBuilder);

    if (failureCount === 0) {
        showAdminToast({
            type: 'success',
            title: 'Nhập Excel thành công',
            message: subtitle
        });
    } else if (successCount > 0) {
        showAdminToast({
            type: 'warning',
            title: 'Nhập Excel hoàn tất, có dòng lỗi',
            message: subtitle
        });
    } else {
        showAdminToast({
            type: 'error',
            title: 'Nhập Excel thất bại',
            message: subtitle
        });
    }

    openAdminSummaryModal({
        title: summaryTitle,
        subtitle,
        totalRows,
        successCount,
        failureCount,
        errorDetails,
        downloadUrl,
        downloadLabel: result?.errorReportFilename || 'Tải file lỗi'
    });
};

const handleDownload = async ({
    button,
    url,
    queryParams,
    fallbackFilename,
    successMessage,
    loadingText
}) => {
    setButtonLoading(button, true, loadingText);

    try {
        const queryString = buildQuery(queryParams || {});
        const response = await fetch(queryString ? `${url}?${queryString}` : url, {
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
        if (!blob || blob.size === 0) {
            throw new Error('Backend không trả về dữ liệu file hợp lệ.');
        }

        await triggerDownload(blob, filename);
        showAdminToast({
            type: 'success',
            title: 'Tải file thành công',
            message: successMessage || 'Hệ thống đã bắt đầu tải file Excel.'
        });
    } catch (error) {
        showAdminToast({
            type: 'error',
            title: 'Tải file thất bại',
            message: error?.message || 'Không thể tải file Excel.'
        });
    } finally {
        setButtonLoading(button, false, loadingText);
    }
};

export const bindExcelActions = ({
    exportButton,
    templateButton,
    importButton,
    importInput,
    exportUrl,
    templateUrl,
    importUrl,
    getExportParams,
    fallbackFilename,
    templateFallbackFilename,
    successExportMessage,
    successTemplateMessage,
    moduleLabel = 'dữ liệu',
    errorReportUrlBuilder,
    onImportSuccess
}) => {
    if (exportButton && exportUrl) {
        exportButton.addEventListener('click', async () => {
            const params = typeof getExportParams === 'function' ? getExportParams() : {};
            await handleDownload({
                button: exportButton,
                url: exportUrl,
                queryParams: params,
                fallbackFilename: fallbackFilename || 'du-lieu.xlsx',
                successMessage: successExportMessage || 'Hệ thống đã bắt đầu tải file Excel.',
                loadingText: 'Đang xuất Excel...'
            });
        });
    }

    if (templateButton && templateUrl) {
        templateButton.addEventListener('click', async () => {
            await handleDownload({
                button: templateButton,
                url: templateUrl,
                queryParams: null,
                fallbackFilename: templateFallbackFilename || fallbackFilename || 'mau-import.xlsx',
                successMessage: successTemplateMessage || 'Hệ thống đã bắt đầu tải file mẫu.',
                loadingText: 'Đang tải file mẫu...'
            });
        });
    }

    if (importButton && importInput && importUrl) {
        importInput.setAttribute('accept', EXCEL_ACCEPT);

        importButton.addEventListener('click', () => {
            if (!importButton.disabled) {
                importInput.click();
            }
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

            setButtonLoading(importButton, true, 'Đang nhập Excel...');

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

                const payload = await response.json();
                const result = payload?.data || {};

                showImportResult({
                    moduleLabel,
                    payload,
                    result,
                    errorReportUrlBuilder
                });

                if ((result?.success || result?.partialSuccess || (result?.successCount || 0) > 0)
                    && typeof onImportSuccess === 'function') {
                    onImportSuccess(payload, result);
                }
            } catch (error) {
                showAdminToast({
                    type: 'error',
                    title: 'Nhập Excel thất bại',
                    message: error?.message || 'Không thể nhập file Excel.'
                });
            } finally {
                setButtonLoading(importButton, false, 'Đang nhập Excel...');
                importInput.value = '';
            }
        });
    }
};
