import { reactive } from 'vue'

const STORAGE_KEY = 'sistema.session'

const empty = { token: null, expiresAt: null, user: null }

function restore() {
  try {
    return { ...empty, ...JSON.parse(sessionStorage.getItem(STORAGE_KEY)) }
  } catch {
    return { ...empty }
  }
}

export const session = reactive(restore())

export function startSession({ accessToken, expiresAt, user }) {
  Object.assign(session, { token: accessToken, expiresAt, user })
  sessionStorage.setItem(STORAGE_KEY, JSON.stringify(session))
}

export function clearSession() {
  Object.assign(session, empty)
  sessionStorage.removeItem(STORAGE_KEY)
}

export function isAuthenticated() {
  return Boolean(session.token) && new Date(session.expiresAt) > new Date()
}

export function hasRole(...roles) {
  return roles.includes(session.user?.role)
}

export function tokenClaims() {
  if (!session.token) return null
  const payload = session.token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/')
  const json = new TextDecoder().decode(Uint8Array.from(atob(payload), (c) => c.charCodeAt(0)))
  return JSON.parse(json)
}
