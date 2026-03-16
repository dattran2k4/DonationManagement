import {systemConfigApi} from '../../apis/systemConfigApi.js';

const elements = {
    form: document.getElementById('systemSettingsForm'),
    resetBtn: document.getElementById('resetSettingsBtn'),
    tabButtons: document.querySelectorAll('.settings-tab-btn'),
    tabPanels: document.querySelectorAll('.settings-panel'),
    imageFileInputs: document.querySelectorAll('input[type="file"][data-path-target]')
};

const setActiveTab = (tabKey) => {
    elements.tabButtons.forEach((button) => {
        const isActive = button.dataset.tabTarget === tabKey;
        button.classList.toggle('border-primary', isActive);
        button.classList.toggle('text-primary', isActive);
        button.classList.toggle('font-semibold', isActive);
        button.classList.toggle('border-transparent', !isActive);
        button.classList.toggle('text-slate-500', !isActive);
        button.classList.toggle('font-medium', !isActive);
    });

    elements.tabPanels.forEach((panel) => {
        panel.classList.toggle('hidden', panel.dataset.tabPanel !== tabKey);
    });
};

const buildConfigPayload = () => {
    const formData = new FormData(elements.form);
    const configs = [];

    for (const [key, value] of formData.entries()) {
        configs.push({
            key,
            value: typeof value === 'string' ? value.trim() : value,
            description: null
        });
    }

    return {configs};
};

const updateImagePreviewFromPath = (pathInputId) => {
    const pathInput = document.getElementById(pathInputId);
    if (!pathInput) return;

    const path = (pathInput.value || '').trim();
    const preview = document.getElementById(`${pathInputId}_preview`);
    const placeholder = document.getElementById(`${pathInputId}_placeholder`);

    if (!preview) return;

    if (!path) {
        if (pathInputId !== 'ORG_LOGO_URL') {
            preview.classList.add('hidden');
            if (placeholder) placeholder.classList.remove('hidden');
        }
        return;
    }

    preview.src = path;
    preview.classList.remove('hidden');
    if (placeholder) placeholder.classList.add('hidden');
};

const fillFormFromConfigMap = (configMap) => {
    if (!configMap || typeof configMap !== 'object') return;

    Object.entries(configMap).forEach(([key, value]) => {
        const field = elements.form?.querySelector(`[name="${key}"]`);
        if (!field) return;
        field.value = value ?? '';
    });

    updateImagePreviewFromPath('ORG_LOGO_URL');
    updateImagePreviewFromPath('HOME_BANNER_URL');
    updateImagePreviewFromPath('ABOUT_BANNER_URL');
};

const loadSettings = async () => {
    try {
        const response = await systemConfigApi.getConfigMap();
        fillFormFromConfigMap(response?.data || {});
    } catch (error) {
        console.error('Không thể tải cấu hình hệ thống:', error);
    }
};

const handleImageInputChange = (fileInput) => {
    const selectedFile = fileInput.files?.[0];
    if (!selectedFile) return;

    if (!selectedFile.type.startsWith('image/')) {
        alert('Vui lòng chọn tệp hình ảnh hợp lệ.');
        fileInput.value = '';
        return;
    }

    const previewId = fileInput.dataset.previewTarget;
    const pathId = fileInput.dataset.pathTarget;
    const placeholderId = fileInput.dataset.placeholderTarget;

    const previewEl = previewId ? document.getElementById(previewId) : null;
    const pathInputEl = pathId ? document.getElementById(pathId) : null;
    const placeholderEl = placeholderId ? document.getElementById(placeholderId) : null;

    if (pathInputEl) {
        pathInputEl.value = `/uploads/images/${selectedFile.name}`;
    }

    if (previewEl) {
        previewEl.src = URL.createObjectURL(selectedFile);
        previewEl.classList.remove('hidden');
    }

    if (placeholderEl) {
        placeholderEl.classList.add('hidden');
    }
};

const uploadSelectedImages = async () => {
    for (const fileInput of elements.imageFileInputs) {
        const selectedFile = fileInput.files?.[0];
        if (!selectedFile) continue;

        const uploadRes = await systemConfigApi.uploadImage(selectedFile);
        const uploadedUrl = uploadRes?.data;
        const pathId = fileInput.dataset.pathTarget;
        const pathInputEl = pathId ? document.getElementById(pathId) : null;

        if (uploadedUrl && pathInputEl) {
            pathInputEl.value = uploadedUrl;
            updateImagePreviewFromPath(pathId);
        }
    }
};

const handleSaveSettings = async (event) => {
    event.preventDefault();
    const submitBtn = elements.form?.querySelector('button[type="submit"]');
    if (submitBtn) submitBtn.disabled = true;

    try {
        await uploadSelectedImages();
        const payload = buildConfigPayload();
        const response = await systemConfigApi.saveConfigs(payload);
        if (response?.status === 200) {
            alert('Đã lưu cấu hình hệ thống thành công.');
            elements.imageFileInputs.forEach((fileInput) => {
                fileInput.value = '';
            });
        }
    } catch (error) {
        console.error('Lỗi khi lưu cấu hình hệ thống:', error);
        alert(error?.message || 'Không thể lưu cấu hình hệ thống.');
    } finally {
        if (submitBtn) submitBtn.disabled = false;
    }
};

const handleReset = () => {
    elements.imageFileInputs.forEach((fileInput) => {
        fileInput.value = '';
    });
    loadSettings();
};

const bindEvents = () => {
    elements.tabButtons.forEach((button) => {
        button.addEventListener('click', () => setActiveTab(button.dataset.tabTarget));
    });

    elements.imageFileInputs.forEach((fileInput) => {
        fileInput.addEventListener('change', () => handleImageInputChange(fileInput));
    });

    elements.form?.addEventListener('submit', handleSaveSettings);
    elements.resetBtn?.addEventListener('click', handleReset);
};

const init = () => {
    if (!elements.form || !elements.tabButtons.length || !elements.tabPanels.length) return;
    setActiveTab('general');
    bindEvents();
    loadSettings();
};

document.addEventListener('DOMContentLoaded', init);
