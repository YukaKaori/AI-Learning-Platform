<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import AppIcon from '@/components/AppIcon.vue'

defineEmits<{ 'toggle-nav': [] }>()

const { t } = useI18n()
</script>

<template>
  <header class="app-header">
    <button
      type="button"
      class="nav-toggle"
      :aria-label="t('nav.menu')"
      @click="$emit('toggle-nav')"
    >
      <AppIcon name="menu" />
    </button>
    <span class="brand-mark" aria-hidden="true"></span>
    <span class="brand-name">{{ t('app.name') }}</span>
  </header>
</template>

<style scoped>
.app-header {
  display: none;
  align-items: center;
  gap: var(--space-2);
  height: var(--header-height);
  flex-shrink: 0;
  padding: 0 var(--space-4);
  border-bottom: var(--border-width-sm) solid var(--color-border);
  background-color: var(--color-surface);
}

.nav-toggle {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  margin-left: calc(var(--space-2) * -1);
  border: none;
  border-radius: var(--radius-sm);
  background: transparent;
  color: var(--color-text-secondary);
  cursor: pointer;
}

.nav-toggle:hover {
  background-color: var(--color-surface-hover);
}

.brand-mark {
  width: 18px;
  height: 18px;
  border-radius: var(--radius-sm);
  background: linear-gradient(135deg, var(--color-primary), var(--color-primary-hover));
}

.brand-name {
  font-size: var(--text-sm);
  font-weight: 600;
  letter-spacing: var(--tracking-tight);
}

@media (max-width: 768px) {
  .app-header {
    display: flex;
  }
}
</style>
