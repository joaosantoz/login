<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { userService } from '../services/userService'
import { ROLES } from '../constants/roles'
import { hasRole } from '../stores/session'

const props = defineProps({ id: { type: String, default: null } })

const router = useRouter()
const editing = computed(() => props.id !== null)
const canChangeRole = computed(() => hasRole('ADMIN'))
const form = reactive({ name: '', email: '', password: '', role: 'CLIENT' })
const fieldErrors = ref({})
const error = ref('')
const saving = ref(false)

onMounted(async () => {
  if (!editing.value) return
  try {
    const user = await userService.get(props.id)
    Object.assign(form, { name: user.name, email: user.email, role: user.role })
  } catch (e) {
    error.value = e.message
  }
})

async function submit() {
  error.value = ''
  fieldErrors.value = {}
  saving.value = true
  try {
    if (editing.value) {
      await userService.update(props.id, { name: form.name, email: form.email, role: form.role })
    } else {
      await userService.create({ ...form })
    }
    router.push({ name: 'user-list' })
  } catch (e) {
    error.value = e.message
    fieldErrors.value = e.fieldErrors ?? {}
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <header class="page-head">
    <div>
      <RouterLink class="back text-button" :to="{ name: 'user-list' }">Usuários</RouterLink>
      <h1>{{ editing ? 'Editar usuário' : 'Novo usuário' }}</h1>
    </div>
  </header>

  <form novalidate @submit.prevent="submit">
    <label class="field" :data-invalid="fieldErrors.name || null">
      <span>Nome</span>
      <input v-model="form.name" maxlength="120" autocomplete="off" />
      <small v-if="fieldErrors.name" class="field-error">{{ fieldErrors.name }}</small>
    </label>

    <label class="field" :data-invalid="fieldErrors.email || null">
      <span>E-mail</span>
      <input v-model="form.email" type="email" maxlength="254" autocomplete="off" />
      <small v-if="fieldErrors.email" class="field-error">{{ fieldErrors.email }}</small>
    </label>

    <label v-if="!editing" class="field" :data-invalid="fieldErrors.password || null">
      <span>Senha</span>
      <input v-model="form.password" type="password" autocomplete="new-password" />
      <small :class="fieldErrors.password ? 'field-error' : 'muted'">{{ fieldErrors.password ?? 'Entre 8 e 72 caracteres.' }}</small>
    </label>

    <fieldset class="roles" :disabled="!canChangeRole">
      <legend>Perfil de acesso</legend>
      <label v-for="role in ROLES" :key="role.value" class="role-option" :data-checked="form.role === role.value || null">
        <input v-model="form.role" type="radio" name="role" :value="role.value" />
        <span class="role" :data-role="role.value">{{ role.label }}</span>
      </label>
      <small v-if="!canChangeRole" class="muted">Apenas administradores alteram perfis.</small>
    </fieldset>

    <p v-if="error && !Object.keys(fieldErrors).length" class="alert" role="alert">{{ error }}</p>

    <div class="actions">
      <button class="button" type="submit" :disabled="saving">{{ saving ? 'Salvando...' : 'Salvar' }}</button>
      <RouterLink class="button quiet" :to="{ name: 'user-list' }">Cancelar</RouterLink>
    </div>
  </form>
</template>

<style scoped>
.back { display: inline-block; margin-bottom: 0.9rem; }
.back::before { content: '\2190\00a0'; }
form { display: grid; gap: 1.5rem; max-inline-size: 30rem; }
.roles { border: 0; padding: 0; margin: 0; display: grid; gap: 0.5rem; }
legend { font-size: var(--step--1); font-weight: 600; color: var(--ink-soft); margin-bottom: 0.4rem; padding: 0; }
.role-option {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0.7rem 0.85rem;
  border: 1px solid var(--line-strong);
  border-radius: var(--radius);
  cursor: pointer;
  transition: border-color 160ms var(--ease-out), background-color 160ms var(--ease-out);
}
.role-option:hover { border-color: var(--muted); }
.role-option[data-checked] { border-color: var(--ink); background: var(--paper-raised); }
.role-option input { accent-color: var(--ink); margin: 0; }
.roles:disabled .role-option { cursor: not-allowed; opacity: 0.55; }
.roles:disabled .role-option[data-checked] { opacity: 1; }
.actions { display: flex; gap: 0.75rem; padding-top: 0.5rem; }
</style>
