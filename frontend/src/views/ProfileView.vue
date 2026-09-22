<script setup>
import { computed, onMounted, ref } from 'vue'
import { userService } from '../services/userService'
import { roleLabel } from '../constants/roles'
import { tokenClaims } from '../stores/session'

const PROBES = [
  { label: 'Listar usuários', request: 'GET /api/usuarios', run: () => userService.list() },
  { label: 'Consultar usuário 1', request: 'GET /api/usuarios/1', run: () => userService.get(1) }
]

const me = ref(null)
const error = ref('')
const claims = computed(tokenClaims)

const claimRows = computed(() => {
  if (!claims.value) return []
  const c = claims.value
  return [
    ['sub', c.sub, 'ID do usuário'],
    ['name', c.name, 'Nome'],
    ['role', c.role, 'Perfil'],
    ['iss', c.iss, 'Emissor'],
    ['iat', asDate(c.iat), 'Emitido em'],
    ['exp', asDate(c.exp), 'Expira em']
  ]
})

function asDate(epochSeconds) {
  return new Date(epochSeconds * 1000).toLocaleString('pt-BR', { dateStyle: 'short', timeStyle: 'medium' })
}

async function probe(item) {
  await item.run().catch(() => {})
}

onMounted(async () => {
  try {
    me.value = await userService.me()
  } catch (e) {
    error.value = e.message
  }
})
</script>

<template>
  <header class="page-head">
    <div v-if="me">
      <span class="role" :data-role="me.role">{{ roleLabel(me.role) }}</span>
      <h1>{{ me.name }}</h1>
      <p class="muted">{{ me.email }}</p>
    </div>
  </header>

  <p v-if="error" class="alert" role="alert">{{ error }}</p>

  <div class="sections">
    <section v-if="claimRows.length">
      <h2>Token JWT</h2>
      <p class="muted lead">Claims lidas do token desta sessão. Assinado com HS256, sem dados sensíveis.</p>
      <dl>
        <template v-for="[claim, value, meaning] in claimRows" :key="claim">
          <dt><code>{{ claim }}</code><span class="muted">{{ meaning }}</span></dt>
          <dd class="tabular">{{ value }}</dd>
        </template>
      </dl>
    </section>

    <section>
      <h2>Testar permissões</h2>
      <p class="muted lead">Dispara a requisição com o seu token. O resultado aparece em Respostas da API.</p>
      <ul class="probes">
        <li v-for="item in PROBES" :key="item.request">
          <button class="button quiet" @click="probe(item)">{{ item.label }}</button>
          <code class="muted">{{ item.request }}</code>
        </li>
      </ul>
    </section>
  </div>
</template>

<style scoped>
.page-head .role { margin-bottom: 0.9rem; }
.sections { display: grid; gap: clamp(2.5rem, 2rem + 2vw, 4rem); max-inline-size: 40rem; }
.lead { margin: 0.45rem 0 1.25rem; font-size: var(--step--1); }
dl { display: grid; grid-template-columns: minmax(10rem, max-content) 1fr; margin: 0; }
dt, dd { padding: 0.7rem 0; border-top: 1px solid var(--line); margin: 0; }
dt { display: flex; gap: 0.75rem; align-items: baseline; padding-right: 2rem; font-size: var(--step--1); }
dd { font-weight: 500; word-break: break-all; }
code { font-family: var(--font-mono); font-size: 0.8rem; }
.probes { list-style: none; margin: 0; padding: 0; display: grid; gap: 0.75rem; }
.probes li { display: flex; align-items: center; gap: 1rem; flex-wrap: wrap; }
</style>
