import { request } from './http'

export const authService = {
  login: (email, password) => request('POST', '/auth/login', { email, password })
}
