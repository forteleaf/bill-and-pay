import { type ClassValue, clsx } from "clsx";
import { twMerge } from "tailwind-merge";

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs));
}

export type WithoutChild<T> = T extends { child?: unknown } ? Omit<T, "child"> : T;
export type WithoutChildren<T> = T extends { children?: unknown } ? Omit<T, "children"> : T;
export type WithoutChildrenOrChild<T> = WithoutChildren<WithoutChild<T>>;
export type WithElementRef<T, U extends HTMLElement = HTMLElement> = T & { ref?: U | null };

/**
 * Convert date from display format (yyyy/MM/dd) to API format (yyyy-MM-dd)
 */
export function toApiDateFormat(date: string | undefined): string | undefined {
  if (!date) return undefined;
  return date.replace(/\//g, '-');
}

/**
 * Convert date to API DateTime format with timezone (yyyy-MM-ddTHH:mm:ss+09:00)
 * @param date - Date string in yyyy/MM/dd or yyyy-MM-dd format
 * @param time - Time string (default: '00:00:00')
 */
export function toApiDateTime(date: string | undefined, time = '00:00:00'): string | undefined {
  if (!date) return undefined;
  return `${toApiDateFormat(date)}T${time}+09:00`;
}
