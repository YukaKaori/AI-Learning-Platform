<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import AppIcon from '../AppIcon.vue'
import type { IconName } from '../icons/registry'

/**
 * Sponsor gallery — the quietest room of the installation (Phase 10).
 *
 * A single centred statement with three future sponsoring channels resting
 * as unlit facets. Deliberately minimal: no links yet (each channel opens
 * in a later phase), no motion beyond the shared gallery transition, no
 * GlassSurface — the page's two displacement filters belong to the sign-in
 * slab and the dock. Escape (or the dock) returns to the sign-in gallery.
 */

const { t } = useI18n()

const emit = defineEmits<{ close: [] }>()

const CHANNELS: ReadonlyArray<{ key: string; icon: IconName }> = [
  { key: 'coffee', icon: 'coffee' },
  { key: 'github', icon: 'heart' },
  { key: 'wechat', icon: 'qr-code' },
]

const channels = computed(() =>
  CHANNELS.map(({ key, icon }) => ({ key, icon, label: t(`landing.sponsor.items.${key}`) })),
)

const rootRef = ref<HTMLElement | null>(null)

onMounted(() => {
  rootRef.value?.focus()
})

function onKeydown(event: KeyboardEvent) {
  if (event.key === 'Escape') {
    event.preventDefault()
    emit('close')
  }
}
</script>

<template>
  <section
    ref="rootRef"
    class="sponsor"
    role="region"
    :aria-label="t('landing.sponsor.title')"
    tabindex="-1"
    @keydown="onKeydown"
  >
    <div class="sponsor-body">
      <span class="sponsor-glyph" aria-hidden="true">
        <AppIcon name="heart" :size="40" :stroke-width="1.25" />
      </span>
      <h2 class="sponsor-title">{{ t('landing.sponsor.title') }}</h2>
      <p class="sponsor-line">{{ t('landing.sponsor.line') }}</p>

      <ul class="sponsor-channels">
        <li v-for="channel in channels" :key="channel.key" class="sponsor-channel">
          <span class="sponsor-channel__glyph" aria-hidden="true">
            <AppIcon :name="channel.icon" size="lg" />
          </span>
          <span class="sponsor-channel__label">{{ channel.label }}</span>
          <span class="sponsor-channel__soon">{{ t('landing.sponsor.soon') }}</span>
        </li>
      </ul>
    </div>
  </section>
</template>

<style scoped>
/* Same gallery layer as the presentation: the dark stage, one step in. */
.sponsor {
  position: fixed;
  inset: 0;
  z-index: 40;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgb(2 3 7 / 0.93);
  outline: none;
}

.sponsor-body {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--space-4);
  padding: var(--space-8);
  padding-bottom: 148px;
  text-align: center;
}

.sponsor-glyph {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 96px;
  height: 96px;
  margin-bottom: var(--space-4);
  border-radius: 28px;
  border: 1px solid rgba(255, 255, 255, 0.16);
  background: linear-gradient(to bottom, rgba(255, 255, 255, 0.08), rgba(255, 255, 255, 0.02));
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.14),
    inset 0 -10px 18px -12px rgba(0, 0, 0, 0.6),
    inset 1px 1px 0 rgba(150, 216, 255, 0.05),
    inset -1px -1px 0 rgba(255, 188, 150, 0.05);
  color: rgba(240, 238, 250, 0.92);
}

.sponsor-title {
  margin: 0;
  font-family: var(--font-headline-family);
  font-size: clamp(2rem, 4vw, 3rem);
  font-weight: 650;
  line-height: 1.15;
  letter-spacing: var(--font-headline-tracking);
  color: rgba(242, 240, 252, 0.95);
}

.sponsor-line {
  margin: 0;
  max-width: 44ch;
  font-size: clamp(1rem, 1.5vw, 1.15rem);
  line-height: 1.6;
  color: rgba(230, 228, 244, 0.6);
}

/* Future channels — unlit facets waiting for their light. */
.sponsor-channels {
  display: flex;
  gap: var(--space-4);
  margin: var(--space-8) 0 0;
  padding: 0;
  list-style: none;
}

.sponsor-channel {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--space-2);
  width: 132px;
  padding: var(--space-5) var(--space-3);
  border-radius: 20px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  background: linear-gradient(to bottom, rgba(255, 255, 255, 0.04), rgba(255, 255, 255, 0.015));
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.08),
    inset 0 -6px 12px -10px rgba(0, 0, 0, 0.55);
  color: rgba(240, 238, 250, 0.85);
}

.sponsor-channel__label {
  font-size: var(--text-sm);
  font-weight: 550;
}

.sponsor-channel__soon {
  font-size: var(--text-xs);
  color: rgba(230, 228, 244, 0.42);
}

@media (max-width: 640px) {
  .sponsor-body {
    padding: var(--space-6);
    padding-bottom: 128px;
  }

  .sponsor-channels {
    flex-direction: column;
    gap: var(--space-3);
    width: min(320px, 100%);
  }

  .sponsor-channel {
    flex-direction: row;
    justify-content: flex-start;
    width: 100%;
    padding: var(--space-3) var(--space-4);
  }

  .sponsor-channel__soon {
    margin-left: auto;
  }
}
</style>
