import { request } from './http'

export const userService = {
  me: () => request('GET', '/usuarios/me'),
  list: () => request('GET', '/usuarios'),
  get: (id) => request('GET', `/usuarios/${id}`),
  create: (user) => request('POST', '/usuarios', user),
  update: (id, user) => request('PUT', `/usuarios/${id}`, user),
  remove: (id) => request('DELETE', `/usuarios/${id}`)
}
