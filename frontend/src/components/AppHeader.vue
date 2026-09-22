<script setup>
import { useRouter } from 'vue-router'
import { roleLabel } from '../constants/roles'
import { responseHistory } from '../services/http'
import { clearSession, hasRole, session } from '../stores/session'

const router = useRouter()

function logout() {
  clearSession()
  responseHistory.splice(0)
  router.push({ name: 'login' })
}
</script>

<template>
  <header class="header">
    <RouterLink to="/" class="wordmark">sistema<span>/usuários</span></RouterLink>
    <nav aria-label="Principal">
      <RouterLink v-if="hasRole('ADMIN', 'OPERATOR')" :to="{ name: 'user-list' }">Usuários</RouterLink>
      <RouterLink :to="{ name: 'profile' }">Meu perfil</RouterLink>
    </nav>
    <div class="account">
      <span class="name">{{ session.user.name }}</span>
      <span class="role" :data-role="session.user.role">{{ roleLabel(session.user.role) }}</span>
      <button class="text-button" @click="logout">Sair</button>
    </div>
  </header>
</template>

<style scoped>
.header {
  position: sticky;
  top: 0;
  z-index: 10;
  display: flex;
  align-items: center;
  gap: clamp(1.5rem, 1rem + 2vw, 3rem);
  block-size: 57px;
  padding-inline: var(--gutter);
  background: color-mix(in oklch, var(--paper) 88%, transparent);
  backdrop-filter: saturate(1.4) blur(10px);
  border-bottom: 1px solid var(--line);
}
.wordmark {
  font-family: var(--font-display);
  font-weight: 700;
  font-size: 1.05rem;
  letter-spacing: -0.02em;
  text-decoration: none;
}
.wordmark span { color: var(--muted); font-weight: 500; }
nav { display: flex; gap: 1.5rem; flex: 1; align-self: stretch; }
nav a {
  display: flex;
  align-items: center;
  font-size: var(--step--1);
  font-weight: 600;
  color: var(--muted);
  text-decoration: none;
  box-shadow: inset 0 -2px 0 transparent;
  transition: color 160ms var(--ease-out), box-shadow 160ms var(--ease-out);
}
nav a:hover { color: var(--ink); }
nav a.router-link-active { color: var(--ink); box-shadow: inset 0 -2px 0 var(--ink); }
.account { display: flex; align-items: center; gap: 1rem; }
.name { font-size: var(--step--1); font-weight: 600; }
@media (max-width: 640px) {
  .name { display: none; }
  .wordmark span { display: none; }
}
</style>
