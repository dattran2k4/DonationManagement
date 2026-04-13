export const debounce = (fn, delay = 350) => {
    let timeoutId;
    return (...args) => {
        clearTimeout(timeoutId);
        timeoutId = setTimeout(() => fn(...args), delay);
    };
};

const parseValue = (rawValue, fallbackValue) => {
    if (typeof fallbackValue === 'number') {
        const parsed = Number.parseInt(rawValue, 10);
        return Number.isFinite(parsed) ? parsed : fallbackValue;
    }

    if (typeof fallbackValue === 'boolean') {
        return rawValue === 'true';
    }

    return rawValue;
};

export const readStateFromUrl = (defaults, options = {}) => {
    const { paramMap = {} } = options;
    const params = new URLSearchParams(window.location.search);
    const state = { ...defaults };

    Object.entries(defaults).forEach(([key, fallbackValue]) => {
        const paramName = paramMap[key] || key;
        const rawValue = params.get(paramName);

        if (rawValue == null || rawValue === '') {
            return;
        }

        state[key] = parseValue(rawValue, fallbackValue);
    });

    return state;
};

export const syncStateToUrl = (state, defaults = {}, options = {}) => {
    const { paramMap = {}, omitDefaults = true } = options;
    const params = new URLSearchParams(window.location.search);
    const keys = new Set([...Object.keys(defaults), ...Object.keys(state)]);

    keys.forEach((key) => {
        const paramName = paramMap[key] || key;
        const value = state[key];
        const defaultValue = defaults[key];

        if (value == null || value === '') {
            params.delete(paramName);
            return;
        }

        if (omitDefaults && defaultValue != null && String(defaultValue) === String(value)) {
            params.delete(paramName);
            return;
        }

        params.set(paramName, String(value));
    });

    const queryString = params.toString();
    const nextUrl = queryString ? `${window.location.pathname}?${queryString}` : window.location.pathname;
    window.history.replaceState({}, '', nextUrl);
};

export const getSortIcon = (state, field) => {
    if (state.sortBy !== field) {
        return 'unfold_more';
    }

    return state.sortDir === 'asc' ? 'arrow_upward' : 'arrow_downward';
};

export const bindSortButtons = ({
    state,
    buttons,
    datasetKey,
    getDefaultDirection = () => 'asc',
    onChange
}) => {
    const buttonList = Array.from(buttons || []);

    const updateIndicators = () => {
        buttonList.forEach((button) => {
            const field = button.dataset[datasetKey];
            const icon = button.querySelector('[data-sort-icon]');
            const isActive = state.sortBy === field;

            button.classList.toggle('text-primary', isActive);
            button.classList.toggle('font-bold', isActive);
            button.setAttribute('aria-pressed', isActive ? 'true' : 'false');

            if (icon) {
                icon.textContent = getSortIcon(state, field);
            }
        });
    };

    buttonList.forEach((button) => {
        button.addEventListener('click', () => {
            const field = button.dataset[datasetKey];
            if (!field) {
                return;
            }

            if (state.sortBy === field) {
                state.sortDir = state.sortDir === 'asc' ? 'desc' : 'asc';
            } else {
                state.sortBy = field;
                state.sortDir = getDefaultDirection(field);
            }

            if (Object.prototype.hasOwnProperty.call(state, 'page')) {
                state.page = 1;
            }

            updateIndicators();
            onChange?.(field, state.sortDir);
        });
    });

    return { updateIndicators };
};
