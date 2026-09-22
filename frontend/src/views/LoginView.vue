<script setup>
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { authService } from '../services/authService'
import { startSession } from '../stores/session'

const DEMO_ACCOUNTS = [
  { email: 'admin@sistema.local', role: 'ADMIN', label: 'Administrador', scope: 'Cria, edita, exclui e altera perfis' },
  { email: 'operador@sistema.local', role: 'OPERATOR', label: 'Operador', scope: 'Consulta todos e edita dados cadastrais' },
  { email: 'cliente@sistema.local', role: 'CLIENT', label: 'Cliente', scope: 'Vê apenas o próprio cadastro' }
]
const DEMO_PASSWORD = 'Senha@123'

const route = useRoute()
const router = useRouter()
const email = ref('')
const password = ref('')
const error = ref(route.query.expired ? 'Sua sessão expirou. Entre novamente.' : '')
const loading = ref(false)

async function submit() {
  error.value = ''
  loading.value = true
  try {
    startSession(await authService.login(email.value, password.value))
    router.push('/')
  } catch (e) {
    error.value = e.message
  } finally {
    loading.value = false
  }
}

function useAccount(account) {
  email.value = account.email
  password.value = DEMO_PASSWORD
}
</script>

<template>
  <div class="login">
    <section class="intro">
      <span class="wordmark">sistema<span>/usuários</span></span>
      <div class="statement">
        <h1>Gestão de usuários</h1>
        <p class="muted">Autenticação com JWT e permissões por perfil. Clique em uma conta abaixo para preencher o login.</p>
      </div>
      <ul class="accounts">
        <li v-for="account in DEMO_ACCOUNTS" :key="account.email">
          <button type="button" :aria-pressed="email === account.email" @click="useAccount(account)">
            <span class="role" :data-role="account.role">{{ account.label }}</span>
            <span class="scope">{{ account.scope }}</span>
            <span class="email muted">{{ account.email }}</span>
          </button>
        </li>
      </ul>
      <p class="muted hint">Senha das contas de demonstração: <strong>{{ DEMO_PASSWORD }}</strong></p>
    </section>

    <section class="form-side">
      <form @submit.prevent="submit">
        <h2>Entrar</h2>
        <label class="field">
          <span>E-mail</span>
          <input v-model="email" type="email" autocomplete="username" required />
        </label>
        <label class="field">
          <span>Senha</span>
          <input v-model="password" type="password" autocomplete="current-password" required />
        </label>
        <p v-if="error" class="alert" role="alert">{{ error }}</p>
        <button class="button submit" type="submit" :disabled="loading">
          {{ loading ? 'Entrando...' : 'Entrar' }}
        </button>
      </form>
    </section>
  </div>
</template>

<style scoped>
.login {
  display: grid;
  grid-template-columns: minmax(0, 1.15fr) minmax(0, 1fr);
  min-block-size: 100dvh;
}
.intro {
  display: flex;
  flex-direction: column;
  gap: clamp(2rem, 1rem + 4vh, 4rem);
  padding: clamp(2rem, 1rem + 4vw, 4.5rem) var(--gutter);
  background: var(--paper-sunken);
  border-right: 1px solid var(--line);
}
.wordmark { font-family: var(--font-display); font-weight: 700; letter-spacing: -0.02em; }
.wordmark span { color: var(--muted); font-weight: 500; }
.statement { margin-top: auto; display: grid; gap: 1.1rem; max-inline-size: 30rem; }
.statement h1 { font-size: var(--step-4); }
.statement p { max-inline-size: 42ch; }
.accounts { list-style: none; margin: 0; padding: 0; max-inline-size: 34rem; }
.accounts li { border-top: 1px solid var(--line-strong); }
.accounts li:last-child { border-bottom: 1px solid var(--line-strong); }
.accounts button {
  display: grid;
  grid-template-columns: 8.5rem 1fr;
  gap: 0.15rem 1rem;
  inline-size: 100%;
  padding: 0.9rem 0.5rem;
  font: inherit;
  text-align: left;
  color: var(--ink);
  background: none;
  border: 0;
  cursor: pointer;
  transition: background-color 160ms var(--ease-out);
}
.accounts button:hover { background: color-mix(in oklch, var(--ink) 4%, transparent); }
.accounts button[aria-pressed='true'] { background: var(--paper-raised); box-shadow: inset 2px 0 0 var(--ink); }
.scope { font-size: var(--step--1); font-weight: 500; }
.email { grid-column: 2; font-size: var(--step--1); }
.hint { font-size: var(--step--1); }
.hint strong { color: var(--ink); font-weight: 600; }
.form-side { display: grid; place-items: center; padding: var(--gutter); }
form { display: grid; gap: 1.1rem; inline-size: min(100%, 22rem); }
form h2 { font-size: var(--step-3); margin-bottom: 0.5rem; }
.submit { justify-content: center; padding-block: 0.8rem; margin-top: 0.4rem; }
@media (max-width: 880px) {
  .login { grid-template-columns: 1fr; }
  .intro { border-right: 0; border-bottom: 1px solid var(--line); }
  .statement { margin-top: 0; }
  .form-side { padding-block: 3rem 4rem; }
}
</style>
