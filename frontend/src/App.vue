<script setup>
import ApiResponsePanel from './components/ApiResponsePanel.vue'
import AppHeader from './components/AppHeader.vue'
import { useRoute } from 'vue-router'
import { session } from './stores/session'

const route = useRoute()
</script>

<template>
  <template v-if="session.user">
    <AppHeader />
    <div class="shell">
      <RouterView v-slot="{ Component, route }">
        <Transition name="page" appear>
          <main :key="route.fullPath" class="page">
            <component :is="Component" />
          </main>
        </Transition>
      </RouterView>
      <ApiResponsePanel />
    </div>
  </template>
  <RouterView v-else-if="route.meta.public" />
</template>
