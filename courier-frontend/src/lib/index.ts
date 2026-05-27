/**
 * Recursively trims all string properties in an object.
 */
export function trimObject<T>(obj: T): T {
    if (obj === null || typeof obj !== 'object') {
        return typeof obj === 'string' ? (obj.trim() as any) : obj;
    }

    if (Array.isArray(obj)) {
        return obj.map(trimObject) as any;
    }

    const trimmed: any = {};
    for (const key in obj) {
        if (Object.prototype.hasOwnProperty.call(obj, key)) {
            trimmed[key] = trimObject((obj as any)[key]);
        }
    }
    return trimmed;
}
