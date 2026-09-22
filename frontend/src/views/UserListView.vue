<script setup>
import { onBeforeUnmount, onMounted, ref } from 'vue'
import { userService } from '../services/userService'
import { roleLabel } from '../constants/roles'
import { hasRole, session } from '../stores/session'

const CONFIRM_WINDOW_MS = 3000

const users = ref([])
const error = ref('')
const loading = ref(true)
const armedId = ref(null)
let disarmTimer

async function load() {
  loading.value = true
  try {
    users.value = await userService.list()
    error.value = ''
  } catch (e) {
    error.value = e.message
  } finally {
    loading.value = false
  }
}

function canEdit(user) {
  return hasRole('ADMIN') || user.role !== 'ADMIN'
}

function canDelete(user) {
  return hasRole('ADMIN') && user.id !== session.user.id
}

async function requestDelete(user) {
  if (armedId.value !== user.id) {
    armedId.value = user.id
    clearTimeout(disarmTimer)
    disarmTimer = setTimeout(() => { armedId.value = null }, CONFIRM_WINDOW_MS)
    return
  }
  armedId.value = null
  try {
    await userService.remove(user.id)
    users.value = users.value.filter((u) => u.id !== user.id)
  } catch (e) {
    error.value = e.message
  }
}

onMounted(load)
onBeforeUnmount(() => clearTimeout(disarmTimer))
</script>

<template>
  <header class="page-head">
    <div>
      <h1>Usuários</h1>
      <p class="muted tabular">{{ loading ? 'Carregando...' : `${users.length} cadastrados` }}</p>
    </div>
    <RouterLink v-if="hasRole('ADMIN')" class="button" :to="{ name: 'user-create' }">Novo usuário</RouterLink>
  </header>

  <p v-if="error" class="alert" role="alert">{{ error }}</p>

  <table v-if="!loading && users.length">
    <thead>
      <tr>
        <th class="id">ID</th>
        <th>Nome</th>
        <th>Perfil</th>
        <th><span class="sr-only">Ações</span></th>
      </tr>
    </thead>
    <TransitionGroup tag="tbody" name="row">
      <tr v-for="user in users" :key="user.id">
        <td class="id tabular muted">{{ user.id }}</td>
        <td>
          <span class="person">{{ user.name }}<span v-if="user.id === session.user.id" class="you">você</span></span>
          <span class="email muted">{{ user.email }}</span>
        </td>
        <td><span class="role" :data-role="user.role">{{ roleLabel(user.role) }}</span></td>
        <td class="actions">
          <RouterLink v-if="canEdit(user)" class="text-button" :to="{ name: 'user-edit', params: { id: user.id } }">Editar</RouterLink>
          <button
            v-if="canDelete(user)"
            class="text-button danger"
            :class="{ armed: armedId === user.id }"
            @click="requestDelete(user)"
          >
            {{ armedId === user.id ? 'Confirmar exclusão' : 'Excluir' }}
          </button>
        </td>
      </tr>
    </TransitionGroup>
  </table>
</template>

<style scoped>
table { inline-size: 100%; border-collapse: collapse; }
th {
  text-align: left;
  font-size: var(--step--1);
  font-weight: 600;
  color: var(--muted);
  padding: 0 1rem 0.7rem 0;
  border-bottom: 1px solid var(--line-strong);
}
td { padding: 0.95rem 1rem 0.95rem 0; border-bottom: 1px solid var(--line); vertical-align: middle; }
tbody tr { transition: background-color 160ms var(--ease-out); }
tbody tr:hover { background: color-mix(in oklch, var(--ink) 2.5%, transparent); }
.id { inline-size: 3.5rem; font-size: var(--step--1); }
.person { display: flex; align-items: center; gap: 0.6rem; font-weight: 600; }
.you {
  font-size: 0.7rem;
  font-weight: 700;
  letter-spacing: 0.05em;
  text-transform: uppercase;
  color: var(--muted);
  border: 1px solid var(--line-strong);
  border-radius: 4px;
  padding: 0 0.35rem;
}
.email { display: block; font-size: var(--step--1); }
.actions { text-align: right; white-space: nowrap; padding-right: 0; }
.actions > * + * { margin-left: 1.1rem; }
.actions .text-button { opacity: 0; transition: opacity 160ms var(--ease-out), color 160ms var(--ease-out); }
tr:hover .actions .text-button, tr:focus-within .actions .text-button, .actions .armed { opacity: 1; }
@media (hover: none) { .actions .text-button { opacity: 1; } }
.row-leave-active { transition: opacity 240ms var(--ease-out), transform 240ms var(--ease-out); }
.row-leave-to { opacity: 0; transform: translateX(-8px); }
.alert { margin-bottom: 1.5rem; }
.sr-only { position: absolute; inline-size: 1px; block-size: 1px; overflow: hidden; clip-path: inset(50%); }
@media (max-width: 640px) {
  .id, th.id { display: none; }
  td, th { padding-right: 0.6rem; }
}
</style>
