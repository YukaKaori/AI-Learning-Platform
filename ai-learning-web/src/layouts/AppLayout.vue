<script setup lang="ts">
import { ref } from 'vue'
import AppHeader from './AppHeader.vue'
import AppSidebar from './AppSidebar.vue'
import AppDrawer from '@/components/AppDrawer.vue'

const mobileNavOpen = ref(false)
</script>

<template>
  <div class="layout">
    <AppHeader @toggle-nav="mobileNavOpen = true" />

    <div class="body">
      <aside class="sidebar-static">
        <AppSidebar />
      </aside>

      <AppDrawer v-model="mobileNavOpen" direction="ltr" size="272" class="sidebar-drawer">
        <AppSidebar @navigate="mobileNavOpen = false" />
      </AppDrawer>

      <main class="content">
        <RouterView />
      </main>
    </div>
  </div>
</template>

<style scoped>
.layout {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.body {
  display: flex;
  flex: 1;
  min-height: 0;
}

.sidebar-static {
  width: var(--sidebar-width);
  flex-shrink: 0;
  border-right: var(--border-width-sm) solid var(--color-border);
  background-color: var(--color-surface);
  transition:
    background-color var(--duration-base) var(--ease-out),
    border-color var(--duration-base) var(--ease-out);
}

.content {
  flex: 1;
  overflow-y: auto;
}

@media (max-width: 768px) {
  .sidebar-static {
    display: none;
  }
}
</style>

<style>
/* AppDrawer renders via ElDrawer's teleport, so this must be unscoped;
   .sidebar-drawer scopes it to this instance. */
.sidebar-drawer .el-drawer__header {
  display: none;
}
.sidebar-drawer .el-drawer__body {
  padding: 0;
}
@media (min-width: 769px) {
  .sidebar-drawer {
    display: none;
  }
}
</style>
