<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { setToken } from '../services/api'

const router = useRouter()
const email = ref('')
const password = ref('')
const error = ref('')
const loading = ref(false)

async function onSubmit() {
  error.value = ''
  if (!email.value.trim() || !password.value) {
    error.value = 'Email y contraseña son obligatorios.'
    return
  }
  loading.value = true
  try {
    // Base: simular login exitoso (sin llamar al backend)
    // Cuando conectes el back: const { data } = await api.post('/api/auth/login', { email: email.value, password: password.value })
    setToken('mock-token-' + Date.now())
    router.push('/')
  } catch (e) {
    error.value = e.response?.data?.message || 'Error al iniciar sesión.'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="flex flex-col min-h-screen bg-slate-950 text-slate-100 items-center justify-center px-6">
    <p class="text-slate-400 text-sm mb-4">DroneOps Manager</p>
    <div class="max-w-md w-full bg-slate-900/70 border border-slate-800 rounded-xl p-6 shadow-lg">
      <h2 class="text-xl font-semibold mb-6">Iniciar sesión</h2>
      <form @submit.prevent="onSubmit" class="space-y-4">
        <div>
          <label class="block text-sm text-slate-400 mb-1">Email</label>
          <input
            v-model="email"
            type="email"
            autocomplete="email"
            placeholder="tu@email.com"
            class="w-full rounded-lg border border-slate-700 bg-slate-800/50 px-3 py-2 text-slate-100 placeholder-slate-500 focus:outline-none focus:ring-2 focus:ring-emerald-500/50 focus:border-emerald-500"
          />
        </div>
        <div>
          <label class="block text-sm text-slate-400 mb-1">Contraseña</label>
          <input
            v-model="password"
            type="password"
            autocomplete="current-password"
            placeholder="••••••••"
            class="w-full rounded-lg border border-slate-700 bg-slate-800/50 px-3 py-2 text-slate-100 placeholder-slate-500 focus:outline-none focus:ring-2 focus:ring-emerald-500/50 focus:border-emerald-500"
          />
        </div>
        <button
          type="submit"
          :disabled="loading"
          class="w-full mt-4 py-2 rounded-lg bg-emerald-600 hover:bg-emerald-500 text-white font-medium disabled:opacity-50 disabled:cursor-not-allowed"
        >
          {{ loading ? 'Entrando...' : 'Iniciar sesión' }}
        </button>
        <p v-if="error" class="text-sm text-red-400 mt-2">{{ error }}</p>
      </form>
      <a href="#" class="mt-3 inline-block text-sm text-indigo-300 hover:text-indigo-200">¿Olvidaste tu contraseña?</a>
    </div>
  </div>
</template>
