export function buildQuery(params) {
    return new URLSearchParams(
        Object.fromEntries(
            Object.entries(params)
                .filter(([_, v]) => v != null && v !== '')
        )
    ).toString();
}