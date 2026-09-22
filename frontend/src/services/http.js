import { reactive } from 'vue'
import router from '../router'
import { clearSession, session } from '../stores/session'

const API_URL = import.meta.env.VITE_API_URL ?? 'http://localhost:8080'
const HISTORY_SIZE = 8
let sequence = 0

export const responseHistory = reactive([])

class ApiError extends Error {
  constructor(status, body) {
    super(body?.detail ?? `Erro HTTP ${status}`)
    this.status = status
    this.fieldErrors = body?.errors ?? {}
  }
}

function redactToken(data) {
  if (!data?.accessToken) return data
  return { ...data, accessToken: `${data.accessToken.slice(0, 24)}...` }
}

function record(method, path, status, data) {
  responseHistory.unshift({ id: ++sequence, method, path, status, data: redactToken(data) })
  responseHistory.splice(HISTORY_SIZE)
}

export async function request(method, path, body) {
  const headers = { Accept: 'application/json' }
  if (body !== undefined) headers['Content-Type'] = 'application/json'
  if (session.token) headers.Authorization = `Bearer ${session.token}`

  const response = await fetch(`${API_URL}/api${path}`, {
    method,
    headers,
    body: body === undefined ? undefined : JSON.stringify(body)
  })
  const text = await response.text()
  const data = text ? JSON.parse(text) : null
  record(method, path, response.status, data)

  if (response.status === 401 && session.token) {
    clearSession()
    router.push({ name: 'login', query: { expired: '1' } })
  }
  if (!response.ok) throw new ApiError(response.status, data)
  return data
}
