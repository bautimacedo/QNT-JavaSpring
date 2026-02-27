import { createRouter, createWebHistory } from 'vue-router'
import { getToken } from '../services/api'
import AppLayout from '../components/AppLayout.vue'
import LoginView from '../views/LoginView.vue'
import DashboardView from '../views/DashboardView.vue'

const routes = [
  {
    path: '/login',
    name: 'login',
    component: LoginView,
    meta: { public: true },
  },
  {
    path: '/',
    component: AppLayout,
    meta: { requiresAuth: true },
    children: [
      {
        path: '',
        name: 'dashboard',
        component: DashboardView,
      },
    ],
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

router.beforeEach((to, _from, next) => {
  const token = getToken()
  if (to.meta.requiresAuth && !token) {
    next({ name: 'login' })
    return
  }
  if (to.meta.public && token && to.name === 'login') {
    next({ path: '/' })
    return
  }
  next()
})

export default router
