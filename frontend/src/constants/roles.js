export const ROLES = [
  { value: 'ADMIN', label: 'Administrador' },
  { value: 'OPERATOR', label: 'Operador' },
  { value: 'CLIENT', label: 'Cliente' }
]

export function roleLabel(value) {
  return ROLES.find((role) => role.value === value)?.label ?? value
}
