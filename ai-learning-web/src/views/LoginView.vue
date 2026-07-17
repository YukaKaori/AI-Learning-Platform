<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, reactive, ref, shallowRef } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { ApiError } from '@/api/types'
import { useAuthStore } from '@/stores/auth'
import { useAppStore, type ThemeMode } from '@/stores/app'
import { useGlassSpotlight } from '@/composables/useGlassSpotlight'
import { AppButton, AppInput, GlassSurface } from '@/components'
import type { IconName } from '@/components'
import lotusUrl from '@/assets/login/pinklotus.png'

// The opening scene of the product: a single artwork in a dark gallery.
// The stage is pure black; the lotus hangs at its own natural size near the
// visual centre, a hero object surrounded by negative space — never a
// wallpaper. The sign-in glass floats in front of the flower's lower half,
// so the sheet refracts petals and leaves instead of empty black. A darkness
// shroud hides the artwork everywhere except two apertures: a card-shaped
// opening under the glass (permanent) and the pointer's travelling reveal.
// Nothing on the stage ever emits light — every change of brightness is the
// shroud giving way, and it flows back when the pointer leaves. Interaction
// timing lives in useGlassSpotlight; this view owns the stage composition.

const { t } = useI18n()
const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const appStore = useAppStore()

const REMEMBERED_USER_KEY = 'alp.login.rememberedUser'

const form = reactive({
  usernameOrEmail: '',
  password: '',
})
const rememberMe = ref(false)
const submitting = ref(false)
const errorKey = ref<string | null>(null)
const showForgotHint = ref(false)

/** Auth error codes → user-facing i18n keys (see AuthErrorCode.java). */
const AUTH_ERROR_KEYS: Record<number, string> = {
  100000: 'auth.error.invalidCredentials',
  100001: 'auth.error.accountLocked',
  100002: 'auth.error.accountDisabled',
}

// The stage is black in both themes, so the glass adapts instead: light mode
// needs a denser white frost for text contrast; dark mode stays smokier so
// the lotus glows through.
const glassFrost = computed(() => (appStore.isDark ? 0.44 : 0.66))

// Optical lighting: the composable eases the pointer light and writes CSS
// variables on the stage (shroud mask + travelling glow) and on the card
// (proximity-reactive glass). Inert on touch / reduced motion.
const stageRef = ref<HTMLElement | null>(null)
const cardRef = ref<InstanceType<typeof GlassSurface> | null>(null)
const cardEl = computed(() => cardRef.value?.element ?? null)
useGlassSpotlight(stageRef, { card: cardEl })

const CARD_RADIUS = 28

/*
 * Card aperture — the shroud's one permanent opening, shaped like the glass.
 *
 * Measured by a dedicated observer rather than useGlassSpotlight: the
 * spotlight sleeps on touch / reduced-motion, but the lotus must still live
 * inside the glass there. The aperture barely clears the card: the tighter
 * it hugs the sheet, the harder "the lotus lives only inside the glass"
 * reads. The displacement filter can sample up to |distortionScale| / 2 px
 * past the edge, so the outermost rim refracts a little feathered darkness —
 * a smoked-glass border, deliberate.
 */
const CARD_HOLE_REACH = 16
const CARD_HOLE_FEATHER = 14

const stageFrame = shallowRef({ width: 0, height: 0 })
const cardFrame = shallowRef({ x: 0, y: 0, width: 0, height: 0 })

// Rounded so sub-pixel jitter never regenerates the mask data-URI.
function measureFrames() {
  const stageEl = stageRef.value
  const card = cardEl.value
  if (!stageEl || !card) return
  const s = stageEl.getBoundingClientRect()
  const c = card.getBoundingClientRect()
  stageFrame.value = { width: Math.round(s.width), height: Math.round(s.height) }
  cardFrame.value = {
    x: Math.round(c.left - s.left),
    y: Math.round(c.top - s.top),
    width: Math.round(c.width),
    height: Math.round(c.height),
  }
}

let frameObserver: ResizeObserver | null = null

const cardHoleMask = computed(() => {
  const stage = stageFrame.value
  const card = cardFrame.value
  if (!stage.width || !card.width) return null
  const x = card.x - CARD_HOLE_REACH
  const y = card.y - CARD_HOLE_REACH
  const w = card.width + CARD_HOLE_REACH * 2
  const h = card.height + CARD_HOLE_REACH * 2
  const rx = CARD_RADIUS + CARD_HOLE_REACH
  const svg = `<svg xmlns="http://www.w3.org/2000/svg" width="${stage.width}" height="${stage.height}"><defs><filter id="f" x="-40%" y="-40%" width="180%" height="180%"><feGaussianBlur stdDeviation="${CARD_HOLE_FEATHER}"/></filter><mask id="m"><rect width="100%" height="100%" fill="#fff"/><rect x="${x}" y="${y}" width="${w}" height="${h}" rx="${rx}" fill="#000" filter="url(#f)"/></mask></defs><rect width="100%" height="100%" fill="#fff" mask="url(#m)"/></svg>`
  return `url("data:image/svg+xml,${encodeURIComponent(svg)}")`
})

/*
 * Travelling aperture — the beam's reveal, entirely subtractive. Its radius
 * is radius × strength (blooms open from zero, stays shut for touch /
 * reduced-motion) and its centre never fully clears: a floor of darkness
 * keeps every revealed petal dimmer than the glass.
 */
const revealMask =
  'radial-gradient(circle calc(var(--glass-light-radius, 360px) * var(--glass-light-strength, 0) * 1.6) at ' +
  'var(--glass-light-x, 50%) var(--glass-light-y, 50%), ' +
  'rgba(0, 0, 0, 0.22) 0%, rgba(0, 0, 0, 0.42) 30%, rgba(0, 0, 0, 0.62) 55%, ' +
  'rgba(0, 0, 0, 0.85) 78%, #000 100%)'

// Both apertures multiply through mask-composite: intersect (transparency
// from either opens the shroud). Without support, only the card aperture
// survives — the stage stays asleep but the story still stands.
const supportsMaskComposite =
  typeof CSS !== 'undefined' && CSS.supports('mask-composite', 'intersect')

const shroudStyle = computed(() => {
  const hole = cardHoleMask.value
  if (!hole) return undefined
  if (!supportsMaskComposite) {
    return { maskImage: hole, maskRepeat: 'no-repeat' }
  }
  return {
    maskImage: `${hole}, ${revealMask}`,
    maskRepeat: 'no-repeat',
    maskComposite: 'intersect',
  }
})

onMounted(() => {
  measureFrames()
  frameObserver = new ResizeObserver(measureFrames)
  if (stageRef.value) frameObserver.observe(stageRef.value)
  if (cardEl.value) frameObserver.observe(cardEl.value)
  // The entrance animation translates the card; re-aim the aperture once the
  // glass settles into place.
  cardEl.value?.addEventListener('animationend', measureFrames, { once: true })
})

onBeforeUnmount(() => {
  frameObserver?.disconnect()
  frameObserver = null
})

onMounted(() => {
  const remembered = localStorage.getItem(REMEMBERED_USER_KEY)
  if (remembered) {
    form.usernameOrEmail = remembered
    rememberMe.value = true
  }
})

async function submit() {
  if (!form.usernameOrEmail.trim() || !form.password) {
    errorKey.value = 'auth.login.required'
    return
  }
  submitting.value = true
  errorKey.value = null
  try {
    await authStore.login({
      usernameOrEmail: form.usernameOrEmail.trim(),
      password: form.password,
    })
    if (rememberMe.value) {
      localStorage.setItem(REMEMBERED_USER_KEY, form.usernameOrEmail.trim())
    } else {
      localStorage.removeItem(REMEMBERED_USER_KEY)
    }
    // Deep links and expired sessions return the user where they were;
    // a plain sign-in flows into the welcome experience.
    const redirect = route.query.redirect
    if (typeof redirect === 'string' && redirect.startsWith('/')) {
      await router.replace(redirect)
    } else {
      await router.replace({ name: 'welcome' })
    }
  } catch (error) {
    errorKey.value =
      error instanceof ApiError
        ? (AUTH_ERROR_KEYS[error.code] ?? error.messageKey)
        : 'error.unknown'
  } finally {
    submitting.value = false
  }
}

// Footer controls: cycle appearance, toggle language — quiet, icon-first.
const themeOrder: ThemeMode[] = ['light', 'dark', 'system']
const themeIcons: Record<ThemeMode, IconName> = { light: 'sun', dark: 'moon', system: 'monitor' }

const themeLabel = computed(
  () => `${t('common.theme.label')}: ${t(`common.theme.${appStore.themeMode}`)}`,
)

function cycleTheme() {
  const next = themeOrder[(themeOrder.indexOf(appStore.themeMode) + 1) % themeOrder.length]
  appStore.setThemeMode(next ?? 'system')
}

const appVersion = __APP_VERSION__

function toggleLocale() {
  appStore.setLocale(appStore.locale === 'zh-CN' ? 'en-US' : 'zh-CN')
}
</script>

<template>
  <main ref="stageRef" class="login-stage">
    <div class="stage-artwork" aria-hidden="true">
      <img class="stage-lotus" :src="lotusUrl" alt="" decoding="async" fetchpriority="high" />
    </div>
    <div class="stage-shroud" :style="shroudStyle" aria-hidden="true"></div>

    <GlassSurface
      ref="cardRef"
      class="login-card"
      width="100%"
      height="auto"
      :border-radius="CARD_RADIUS"
      :border-width="0.08"
      :blur="10"
      :opacity="0.97"
      :displace="0.5"
      :background-opacity="glassFrost"
      :saturation="1.15"
      :distortion-scale="-88"
      :red-offset="0"
      :green-offset="4"
      :blue-offset="8"
    >
      <section class="card-body" :aria-label="t('auth.login.title')">
        <header class="login-header">
          <span class="brand-mark" aria-hidden="true"></span>
          <h1 class="login-title">{{ t('app.name') }}</h1>
          <p class="login-subtitle">{{ t('auth.login.subtitle') }}</p>
        </header>

        <form class="login-form" novalidate @submit.prevent="submit">
          <AppInput
            v-model="form.usernameOrEmail"
            :label="t('auth.login.usernameOrEmail')"
            icon-left="user"
            size="lg"
            autocomplete="username"
          />
          <AppInput
            v-model="form.password"
            type="password"
            :label="t('auth.login.password')"
            icon-left="lock"
            size="lg"
            autocomplete="current-password"
          />

          <div class="login-options">
            <el-checkbox v-model="rememberMe" size="small">
              {{ t('auth.login.rememberMe') }}
            </el-checkbox>
            <AppButton
              variant="plain"
              size="sm"
              :aria-expanded="showForgotHint"
              @click="showForgotHint = !showForgotHint"
            >
              {{ t('auth.login.forgotPassword') }}
            </AppButton>
          </div>

          <Transition name="app-slide-down">
            <p v-if="showForgotHint" class="login-hint">
              {{ t('auth.login.forgotPasswordHint') }}
            </p>
          </Transition>
          <Transition name="app-slide-down">
            <p v-if="errorKey" class="login-error" role="alert">{{ t(errorKey) }}</p>
          </Transition>

          <AppButton type="submit" size="lg" block :loading="submitting">
            {{ t('auth.login.submit') }}
          </AppButton>
        </form>

        <footer class="login-footer">
          <div class="footer-controls">
            <AppButton
              variant="ghost"
              size="sm"
              :icon-left="themeIcons[appStore.themeMode]"
              :aria-label="themeLabel"
              :title="themeLabel"
              @click="cycleTheme"
            />
            <AppButton
              variant="ghost"
              size="sm"
              icon-left="globe"
              :aria-label="t('common.language')"
              @click="toggleLocale"
            >
              {{ appStore.locale === 'zh-CN' ? 'EN' : '中文' }}
            </AppButton>
          </div>
          <span class="version">v{{ appVersion }}</span>
        </footer>
      </section>
    </GlassSurface>
  </main>
</template>

<style scoped>
/*
 * The stage — pure black in BOTH themes, edge to edge: no gradients, no
 * texture, no tinted overlays. All the colour on the page belongs to the
 * artwork; theme choice is expressed by the glass card, not the backdrop.
 */
.login-stage {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  min-height: 100dvh;
  padding: var(--space-6);
  overflow: hidden;
  background: #000;
  isolation: isolate;
}

/*
 * The artwork — a hero object, not a wallpaper. The lotus hangs at its
 * native 3:2 ratio, conservatively sized (~42vw, capped) so generous black
 * negative space surrounds it, its centre a little above the viewport's:
 * the glass card, biased below centre, then overlaps the flower's lower
 * half. The wrapper owns position and a soft edge feather (the source
 * frame's own black field dissolves into the stage, never reading as a
 * pasted rectangle); the img inside owns the breathing, because app-breathe
 * animates transform and would clobber positional transforms.
 */
.stage-artwork {
  position: absolute;
  top: 36%;
  left: 50%;
  width: min(42vw, 640px);
  transform: translate(-50%, -50%);
  pointer-events: none;
  mask-image:
    linear-gradient(to right, transparent, #000 9%, #000 91%, transparent),
    linear-gradient(to bottom, transparent, #000 9%, #000 91%, transparent);
  mask-composite: intersect;
}

/* Native aspect ratio — never stretched, never cropped. */
.stage-lotus {
  display: block;
  width: 100%;
  height: auto;
  animation: app-breathe 14s var(--ease-in-out) infinite alternate;
}

/*
 * Darkness shroud — the stage sleeps nearly black; the lotus is hidden
 * inside it. Its mask (two feathered apertures: the card-shaped opening
 * under the glass and the travelling beam's reveal) is composed inline in
 * the script — the card aperture is an SVG data-URI rebuilt from measured
 * geometry, the reveal a CSS-variable-driven gradient. Every ramp is long
 * and low-contrast: darkness giving way, never a spotlight edge.
 */
.stage-shroud {
  position: absolute;
  inset: 0;
  pointer-events: none;
  background: rgba(0, 0, 0, 0.95);
  transform: translateZ(0);
}

/*
 * The glass sheet floats in front of the flower's lower half so GlassSurface
 * refracts petals and leaves, not empty black. The top margin (halved by the
 * centering flex) drops the card just below the viewport centre, letting the
 * bloom crown rise above the sheet. Entrance: one soft rise, then stillness —
 * the only continuous motion on the stage is the lotus breathing.
 */
.login-card {
  position: relative;
  max-width: 520px;
  margin-top: 14vh;
  animation: app-slide-up 640ms var(--ease-out) 60ms both;
}

.card-body {
  width: 100%;
  padding: var(--space-10) var(--space-8) var(--space-6);
}

.login-header {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--space-2);
  margin-bottom: var(--space-8);
  text-align: center;
}

.brand-mark {
  width: 44px;
  height: 44px;
  margin-bottom: var(--space-2);
  border-radius: var(--radius-lg);
  background: linear-gradient(135deg, var(--color-primary), var(--accent-violet));
  box-shadow:
    var(--shadow-glow-primary),
    var(--shadow-md),
    inset 0 1px 0 var(--glass-highlight);
}

.login-title {
  margin: 0;
  font-family: var(--font-headline-family);
  font-size: var(--font-headline-size);
  font-weight: var(--font-headline-weight);
  line-height: var(--font-headline-leading);
  letter-spacing: var(--font-headline-tracking);
  color: var(--color-text);
}

.login-subtitle {
  margin: 0;
  font-size: var(--font-caption-size);
  color: var(--color-text-secondary);
}

.login-form {
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
}

.login-options {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-block: calc(var(--space-1) * -1);
}

/* On glass the token border (tuned for solid surfaces) vanishes — lift the
   unchecked box to the muted step so it stays quietly visible. */
.login-options :deep(.el-checkbox__inner) {
  border-color: var(--color-muted);
  background-color: transparent;
}

/* Light mode's primary tint washes out on the mid-gray glass; the active
   step keeps the link on-brand but legible. Dark mode's primary already
   carries enough luminance. */
.login-options :deep(.app-button.variant-plain) {
  color: var(--color-primary-active);
}
html.dark .login-options :deep(.app-button.variant-plain) {
  color: var(--color-primary);
}

.login-hint {
  margin: 0;
  font-size: var(--text-xs);
  color: var(--color-text-secondary);
}

.login-error {
  margin: 0;
  font-size: var(--text-sm);
  color: var(--color-danger);
}

.login-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: var(--space-6);
  padding-top: var(--space-4);
  border-top: var(--border-width-sm) solid var(--glass-border);
}

.footer-controls {
  display: flex;
  gap: var(--space-1);
}

.version {
  font-size: var(--text-xs);
  color: var(--color-text-tertiary);
}

@media (max-width: 640px) {
  .login-stage {
    padding: var(--space-4);
  }

  /* Narrow screens: 42vw would shrink the artwork to a thumbnail — let it
     take most of the width while the composition stays object-in-darkness. */
  .stage-artwork {
    top: 32%;
    width: min(84vw, 420px);
  }

  .login-card {
    margin-top: 10vh;
  }

  .card-body {
    padding: var(--space-8) var(--space-5) var(--space-5);
  }
}
</style>
