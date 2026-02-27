<script setup>
</script>

<template>
  <div class="bg-slate-950 text-slate-100 min-h-screen">
    <div class="max-w-6xl mx-auto px-6 py-8">
      <header class="mb-10 flex items-center justify-between">
        <div>
          <h1 class="text-3xl font-semibold tracking-tight">DroneOps Manager ERP</h1>
          <p class="text-sm text-slate-400">Operaciones, mantenimiento y cumplimiento de flota</p>
        </div>
        <span class="px-3 py-1 rounded-full bg-emerald-500/10 text-emerald-300 text-sm">Panel Operativo</span>
      </header>
      <router-view />
    </div>
  </div>
</template>
