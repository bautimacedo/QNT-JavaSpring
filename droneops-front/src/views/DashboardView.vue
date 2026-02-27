<script setup>
import { ref, onMounted } from 'vue'
// import { api } from '../services/api'

// Datos mock (misma forma que devolverá el backend)
const mockDashboard = () => ({
  activeDrones: 5,
  licensesExpiringCount: 2,
  licensesExpiring: [
    { pilotName: 'Juan Pérez', type: 'ANAC', expirationDate: '2025-03-15', documentUrl: 'https://example.com/doc1.pdf' },
    { pilotName: 'María García', type: 'VANT', expirationDate: '2025-03-28', documentUrl: 'https://example.com/doc2.pdf' },
  ],
  weather: {
    temperature: '18°C',
    condition: 'Parcialmente nublado',
    windSpeed: 22,
  },
})

const loading = ref(true)
const error = ref('')
const data = ref(null)

function formatDate(iso) {
  if (!iso) return '—'
  const [y, m, d] = iso.split('-')
  return `${d}/${m}/${y}`
}

onMounted(async () => {
  try {
    // Cuando conectes el back: const { data: res } = await api.get('/api/dashboard'); data.value = res
    await new Promise((r) => setTimeout(r, 600))
    data.value = mockDashboard()
  } catch (e) {
    error.value = e.response?.data?.message || 'Error al cargar el dashboard.'
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <div>
    <!-- Loading -->
    <template v-if="loading">
      <div class="grid grid-cols-1 md:grid-cols-3 gap-6">
        <div v-for="i in 3" :key="i" class="bg-slate-900/70 border border-slate-800 rounded-xl p-6 shadow-lg animate-pulse">
          <div class="h-4 bg-slate-700 rounded w-1/2 mb-3"></div>
          <div class="h-10 bg-slate-700 rounded w-1/3"></div>
        </div>
      </div>
      <div class="mt-10 bg-slate-900/60 border border-slate-800 rounded-xl p-6 shadow-lg animate-pulse">
        <div class="h-6 bg-slate-700 rounded w-48 mb-4"></div>
        <div class="h-32 bg-slate-800 rounded"></div>
      </div>
    </template>

    <!-- Error -->
    <div v-else-if="error" class="bg-slate-900/70 border border-slate-800 rounded-xl p-6 text-red-400">
      {{ error }}
    </div>

    <!-- Contenido -->
    <template v-else-if="data">
      <!-- Grid 3 tarjetas KPI -->
      <div class="grid grid-cols-1 md:grid-cols-3 gap-6">
        <div class="bg-slate-900/70 border border-slate-800 rounded-xl p-6 shadow-lg">
          <p class="text-slate-400 text-sm">Drones activos</p>
          <p class="text-4xl font-bold mt-2 text-emerald-400">{{ data.activeDrones }}</p>
          <p class="text-xs text-slate-500 mt-1">Listos para desplegar</p>
        </div>
        <div class="bg-slate-900/70 border border-slate-800 rounded-xl p-6 shadow-lg">
          <p class="text-slate-400 text-sm">Licencias próximas a vencer</p>
          <p class="text-4xl font-bold mt-2 text-amber-400">{{ data.licensesExpiringCount }}</p>
          <p class="text-xs text-slate-500 mt-1">Dentro de 30 días</p>
        </div>
        <div class="bg-slate-900/70 border border-slate-800 rounded-xl p-6 shadow-lg">
          <p class="text-slate-400 text-sm">Clima actual</p>
          <p class="text-2xl font-semibold mt-2 text-slate-100">{{ data.weather.temperature }} · {{ data.weather.condition }}</p>
          <p class="text-xs text-slate-500 mt-1">Viento {{ data.weather.windSpeed }} km/h</p>
        </div>
      </div>

      <!-- Licencias a renovar -->
      <div class="mt-10 bg-slate-900/60 border border-slate-800 rounded-xl p-6 shadow-lg">
        <div class="flex items-center justify-between mb-4">
          <h2 class="text-xl font-semibold">Licencias a renovar</h2>
          <span class="text-xs text-slate-500">Monitoreo de cumplimiento</span>
        </div>
        <div class="overflow-x-auto">
          <table class="min-w-full text-sm">
            <thead>
              <tr class="text-left text-slate-400">
                <th class="py-2">Piloto</th>
                <th class="py-2">Tipo</th>
                <th class="py-2">Vencimiento</th>
                <th class="py-2">Documento</th>
              </tr>
            </thead>
            <tbody class="divide-y divide-slate-800">
              <tr v-if="data.licensesExpiring.length === 0">
                <td colspan="4" class="py-3 text-center text-slate-500">No hay licencias por vencer.</td>
              </tr>
              <tr v-for="(row, i) in data.licensesExpiring" :key="i" class="text-slate-200">
                <td class="py-2">{{ row.pilotName }}</td>
                <td class="py-2">{{ row.type }}</td>
                <td class="py-2">{{ formatDate(row.expirationDate) }}</td>
                <td class="py-2">
                  <a
                    :href="row.documentUrl"
                    target="_blank"
                    rel="noopener noreferrer"
                    class="text-indigo-300 hover:text-indigo-200"
                  >
                    Ver
                  </a>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </template>
  </div>
</template>
