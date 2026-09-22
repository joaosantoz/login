<script setup>
import { ref, watch } from 'vue'
import { responseHistory } from '../services/http'

const openId = ref(null)

watch(() => responseHistory[0]?.id, (id) => { openId.value = id })

function tone(status) {
  if (status < 300) return 'ok'
  if (status < 500) return 'warn'
  return 'fail'
}

function toggle(id) {
  openId.value = openId.value === id ? null : id
}
</script>

<template>
  <aside class="console" aria-label="Respostas da API">
    <div class="console-inner">
    <div class="console-head">
      <h2>Respostas da API</h2>
      <span class="eyebrow tabular">{{ responseHistory.length }}/8</span>
    </div>

    <p v-if="!responseHistory.length" class="empty muted">
      Cada chamada feita pela interface aparece aqui com método, rota, status e corpo da resposta.
      Tente uma ação que seu perfil não permite para ver o 403.
    </p>

    <TransitionGroup name="entry" tag="ol" class="entries">
      <li v-for="entry in responseHistory" :key="entry.id" :data-open="openId === entry.id || null">
        <button class="summary" :aria-expanded="openId === entry.id" @click="toggle(entry.id)">
          <span class="status tabular" :data-tone="tone(entry.status)">{{ entry.status }}</span>
          <span class="method">{{ entry.method }}</span>
          <span class="path">/api{{ entry.path }}</span>
        </button>
        <div class="body">
          <div>
            <pre>{{ entry.data === null ? 'sem corpo' : JSON.stringify(entry.data, null, 2) }}</pre>
          </div>
        </div>
      </li>
    </TransitionGroup>
    </div>
  </aside>
</template>

<style scoped>
.console {
  border-left: 1px solid var(--line);
  background: var(--paper-sunken);
}
.console-inner {
  position: sticky;
  top: 57px;
  max-block-size: calc(100dvh - 57px);
  overflow: auto;
  padding: clamp(2rem, 1.2rem + 3vw, 4rem) 1.5rem 2rem;
}
.console-head { display: flex; justify-content: space-between; align-items: baseline; margin-bottom: 1.25rem; }
.empty { font-size: var(--step--1); max-inline-size: 34ch; }
.entries { list-style: none; margin: 0; padding: 0; }
li { border-top: 1px solid var(--line); }
li:last-child { border-bottom: 1px solid var(--line); }
.summary {
  display: grid;
  grid-template-columns: 2.6rem 3.6rem 1fr;
  align-items: center;
  inline-size: 100%;
  padding: 0.7rem 0;
  font: inherit;
  font-size: var(--step--1);
  text-align: left;
  color: var(--ink);
  background: none;
  border: 0;
  cursor: pointer;
}
.status { font-family: var(--font-mono); font-weight: 700; }
.status[data-tone='ok'] { color: var(--ok); }
.status[data-tone='warn'] { color: var(--warn); }
.status[data-tone='fail'] { color: var(--fail); }
.method { font-weight: 700; color: var(--muted); letter-spacing: 0.03em; }
.path { font-family: var(--font-mono); font-size: 0.78rem; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.body { display: grid; grid-template-rows: 0fr; transition: grid-template-rows 280ms var(--ease-out); }
li[data-open] .body { grid-template-rows: 1fr; }
.body > div { overflow: hidden; }
pre {
  margin: 0 0 0.9rem;
  padding: 0.85rem 0.95rem;
  font-family: var(--font-mono);
  font-size: 0.74rem;
  line-height: 1.6;
  color: var(--ink-soft);
  background: var(--paper-raised);
  border: 1px solid var(--line);
  border-radius: var(--radius);
  white-space: pre-wrap;
  word-break: break-word;
  max-block-size: 22rem;
  overflow: auto;
}
.entry-enter-active { transition: opacity 300ms var(--ease-out), transform 300ms var(--ease-out); }
.entry-enter-from { opacity: 0; transform: translateX(10px); }
.entry-leave-active { display: none; }
@media (max-width: 960px) {
  .console { border-left: 0; border-top: 1px solid var(--line); }
  .console-inner { position: static; max-block-size: none; padding-top: 2rem; }
}
</style>
