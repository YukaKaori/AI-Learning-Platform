import { api } from '@/api/http'

/** Mirror of AuthUserResponse.java — the snowflake id arrives as a string. */
export interface AuthUser {
  id: string
  username: string
  email: string
  nickname: string | null
  avatar: string | null
}

export interface LoginPayload {
  usernameOrEmail: string
  password: string
}

export interface LoginResult {
  accessToken: string
  refreshToken: string
  /** Access-token lifetime in seconds. */
  expiresIn: number
  user: AuthUser
}

export function login(payload: LoginPayload) {
  return api.post<LoginResult>('/v1/auth/login', payload)
}

export function logout(refreshToken: string) {
  return api.post<void>('/v1/auth/logout', { refreshToken })
}

export function getCurrentUser() {
  return api.get<AuthUser>('/v1/auth/me')
}

// Note: /v1/auth/refresh is intentionally absent — token refresh is owned by
// the http layer (see api/http.ts) so it can run inside the 401 interceptor.
