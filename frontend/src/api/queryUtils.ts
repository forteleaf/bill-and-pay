import { toApiDateFormat, toApiDateTime } from '$lib/utils';

type ParamValue = string | number | boolean | undefined | null;

/**
 * Build URLSearchParams string from an object, filtering out undefined/null/empty values.
 */
export function buildQueryString(params: Record<string, ParamValue>): string {
  const searchParams = new URLSearchParams();
  for (const [key, value] of Object.entries(params)) {
    if (value !== undefined && value !== null && value !== '') {
      searchParams.set(key, String(value));
    }
  }
  return searchParams.toString();
}

/**
 * Build a full endpoint URL with query parameters.
 */
export function buildEndpoint(basePath: string, params: Record<string, ParamValue>): string {
  const queryString = buildQueryString(params);
  return queryString ? `${basePath}?${queryString}` : basePath;
}

export { toApiDateFormat, toApiDateTime };
