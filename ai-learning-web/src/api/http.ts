import axios, { AxiosError, type AxiosRequestConfig } from 'axios'
import { API_SUCCESS_CODE, ApiError, type ApiResponse } from './types'

export const TOKEN_STORAGE_KEY = 'alp.accessToken'

const http = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL,
  timeout: 15_000,
})

http.interceptors.request.use((config) => {
  const token = localStorage.getItem(TOKEN_STORAGE_KEY)
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

function toApiError(error: unknown): ApiError {
  if (error instanceof ApiError) {
    return error
  }
  if (error instanceof AxiosError) {
    if (error.code === AxiosError.ECONNABORTED) {
      return new ApiError(-1, error.message, 'error.timeout')
    }
    const body = error.response?.data as Partial<ApiResponse<unknown>> | undefined
    if (body && typeof body.code === 'number') {
      return new ApiError(body.code, body.message ?? error.message, 'error.server')
    }
    if (error.response) {
      return new ApiError(-1, error.message, 'error.server')
    }
    return new ApiError(-1, error.message, 'error.network')
  }
  return new ApiError(-1, error instanceof Error ? error.message : String(error), 'error.unknown')
}

async function unwrap<T>(request: Promise<{ data: ApiResponse<T> }>): Promise<T> {
  try {
    const { data: envelope } = await request
    if (envelope.code !== API_SUCCESS_CODE) {
      throw new ApiError(envelope.code, envelope.message, 'error.server')
    }
    return envelope.data
  } catch (error) {
    throw toApiError(error)
  }
}

/** Typed request helpers — every endpoint module goes through these. */
export const api = {
  get: <T>(url: string, config?: AxiosRequestConfig) =>
    unwrap<T>(http.get<ApiResponse<T>>(url, config)),
  post: <T>(url: string, body?: unknown, config?: AxiosRequestConfig) =>
    unwrap<T>(http.post<ApiResponse<T>>(url, body, config)),
  put: <T>(url: string, body?: unknown, config?: AxiosRequestConfig) =>
    unwrap<T>(http.put<ApiResponse<T>>(url, body, config)),
  delete: <T>(url: string, config?: AxiosRequestConfig) =>
    unwrap<T>(http.delete<ApiResponse<T>>(url, config)),
}
