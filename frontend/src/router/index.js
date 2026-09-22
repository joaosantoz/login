import { createRouter, createWebHistory } from 'vue-router'
import { hasRole, isAuthenticated } from '../stores/session'
import LoginView from '../views/LoginView.vue'
import ProfileView from '../views/ProfileView.vue'
import UserFormView from '../views/UserFormView.vue'
import UserListView from '../views/UserListView.vue'

const STAFF = ['ADMIN', 'OPERATOR']

const routes = [
  { path: '/login', name: 'login', component: LoginView, meta: { public: true } },
  { path: '/perfil', name: 'profile', component: ProfileView },
  { path: '/usuarios', name: 'user-list', component: UserListView, meta: { roles: STAFF } },
  { path: '/usuarios/novo', name: 'user-create', component: UserFormView, meta: { roles: ['ADMIN'] } },
  { path: '/usuarios/:id/editar', name: 'user-edit', component: UserFormView, props: true, meta: { roles: STAFF } },
  { path: '/:pathMatch(.*)*', redirect: () => (hasRole(...STAFF) ? '/usuarios' : '/perfil') }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to) => {
  if (to.meta.public) return isAuthenticated() ? '/' : true
  if (!isAuthenticated()) return { name: 'login' }
  if (to.meta.roles && !hasRole(...to.meta.roles)) return { name: 'profile' }
  return true
})

export default router
