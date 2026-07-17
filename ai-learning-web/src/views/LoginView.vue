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

// The opening scene of the product — a landing, not a dialog. One artwork in
// a dark gallery: the stage is pure black; the lotus hangs at its natural
// aspect ratio as the visual hero, surrounded by negative space — never a
// wallpaper. The sign-in glass floats over the flower's lower half so the
// sheet refracts petals, translucent leaves and part of the core. Below it,
// one line of product voice; near the bottom, a floating glass navigation
// and the colophon. A darkness shroud hides the artwork everywhere except
// two apertures: a card-shaped opening under the glass (permanent) and the
// pointer's travelling reveal. Nothing on the stage ever emits light — every
// change of brightness is the shroud giving way, and it flows back when the
// pointer leaves. Interaction timing lives in useGlassSpotlight; this view
// owns the stage composition.

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
// the lotus glows through. The nav bar is the same material one step
// thinner — a satellite of the card, never its rival.
const glassFrost = computed(() => (appStore.isDark ? 0.44 : 0.66))
const navFrost = computed(() => (appStore.isDark ? 0.3 : 0.56))

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
const copyrightYear = new Date().getFullYear()

function toggleLocale() {
  appStore.setLocale(appStore.locale === 'zh-CN' ? 'en-US' : 'zh-CN')
}

// Floating glass navigation — product identity, every link a real
// destination (the public repository and its documents). "AI Native" is the
// wordmark, not a link.
const REPO_URL = 'https://github.com/YukaKaori/AI-Learning-Platform'
const NAV_RADIUS = 36

const navLinks = computed(() => [
  { label: t('landing.nav.docs'), href: `${REPO_URL}/tree/main/docs` },
  { label: t('landing.nav.features'), href: `${REPO_URL}/blob/main/docs/ai-engine.md` },
  { label: t('landing.nav.roadmap'), href: `${REPO_URL}/blob/main/docs/architecture.md` },
  { label: 'GitHub', href: REPO_URL },
])
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
        </footer>
      </section>
    </GlassSurface>

    <p class="stage-tagline">{{ t('landing.tagline') }}</p>

    <GlassSurface
      class="landing-nav"
      width="auto"
      height="auto"
      :border-radius="NAV_RADIUS"
      :blur="10"
      :opacity="0.97"
      :displace="0.5"
      :background-opacity="navFrost"
      :saturation="1.1"
      :distortion-scale="-60"
      :red-offset="0"
      :green-offset="3"
      :blue-offset="6"
    >
      <nav class="nav-links" :aria-label="t('landing.nav.label')">
        <span class="nav-wordmark">AI Native</span>
        <a
          v-for="link in navLinks"
          :key="link.href"
          class="nav-link"
          :href="link.href"
          target="_blank"
          rel="noopener"
        >
          {{ link.label }}
        </a>
      </nav>
    </GlassSurface>

    <p class="stage-colophon">v{{ appVersion }} · © {{ copyrightYear }} {{ t('app.name') }}</p>
  </main>
</template>

<style scoped>
/*
 * The stage — pure black in BOTH themes, edge to edge: no gradients, no
 * texture, no tinted overlays. All the colour on the page belongs to the
 * artwork; theme choice is expressed by the glass surfaces, not the
 * backdrop. The page reads top to bottom as a landing: hero artwork →
 * glass login → product voice → floating navigation → colophon. Everything
 * flows in a single column so short viewports scroll instead of clipping.
 */
.login-stage {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  min-height: 100vh;
  min-height: 100dvh;
  padding: var(--space-6);
  overflow: hidden;
  background: #000;
  isolation: isolate;
}

/*
 * The artwork — the hero of the stage, composed like product photography:
 * the lotus hangs at its native 3:2 ratio, large enough to command the
 * frame (~52vw, capped) yet still surrounded by black negative space. Its
 * centre sits above the viewport's, so the glass card — biased below —
 * overlaps the flower's lower half and the bloom crown rises free above
 * the sheet. The wrapper owns position and a soft edge feather (the source
 * frame's own black field dissolves into the stage, never reading as a
 * pasted rectangle); the img inside owns the breathing, because app-breathe
 * animates transform and would clobber positional transforms.
 */
.stage-artwork {
  position: absolute;
  top: 32%;
  left: 50%;
  width: min(52vw, 800px);
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
 * refracts petals and leaves, not empty black. The top offset drops the card
 * below the lotus centre while the bloom crown rises above the sheet.
 * Entrance: one soft rise, then stillness — the only continuous motion on
 * the stage is the lotus breathing.
 */
.login-card {
  position: relative;
  width: 100%;
  max-width: 520px;
  margin-top: clamp(80px, 15vh, 180px);
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

/*
 * On-glass control skins — the card's material language extended to every
 * control inside it: transparency, a glass border, a top highlight, a hint
 * of inner depth. Same optical family as GlassSurface, but lightweight —
 * never a second backdrop-filter (one refraction pass per stage is the
 * budget) and never a nested GlassSurface. Scoped here deliberately: the
 * app-wide components stay tuned for solid surfaces; these skins assume a
 * glass parent. light-dark() follows the card's frost (white in light,
 * smoke in dark), which tokens.css enables via color-scheme.
 */
.login-form :deep(.app-input) {
  background: light-dark(rgba(255, 255, 255, 0.48), rgba(255, 255, 255, 0.05));
  border-color: light-dark(rgba(24, 24, 27, 0.12), rgba(255, 255, 255, 0.13));
  box-shadow: inset 0 1px 0 light-dark(rgba(255, 255, 255, 0.75), rgba(255, 255, 255, 0.07));
}

.login-form :deep(.app-input:focus-within) {
  background: light-dark(rgba(255, 255, 255, 0.62), rgba(255, 255, 255, 0.08));
  border-color: var(--color-primary);
  box-shadow:
    inset 0 1px 0 light-dark(rgba(255, 255, 255, 0.75), rgba(255, 255, 255, 0.08)),
    0 0 0 3px var(--color-primary-soft);
}

/* The primary action is the brightest glass on the card: the brand colour
   poured into a translucent slab — rim, top highlight, and a shaded lower
   edge give it physical thickness. */
.login-form :deep(.app-button.variant-solid.tone-primary) {
  background: light-dark(
    color-mix(in srgb, var(--color-primary) 92%, transparent),
    color-mix(in srgb, var(--color-primary) 78%, transparent)
  );
  border-color: light-dark(rgba(255, 255, 255, 0.4), rgba(255, 255, 255, 0.22));
  box-shadow:
    var(--shadow-glow-primary),
    inset 0 1px 0 rgba(255, 255, 255, 0.32),
    inset 0 -10px 18px -12px rgba(0, 0, 0, 0.4);
}
.login-form :deep(.app-button.variant-solid.tone-primary:hover:not(:disabled)) {
  background: light-dark(
    color-mix(in srgb, var(--color-primary-hover) 94%, transparent),
    color-mix(in srgb, var(--color-primary-hover) 86%, transparent)
  );
}
.login-form :deep(.app-button.variant-solid.tone-primary:active:not(:disabled)) {
  background: light-dark(
    color-mix(in srgb, var(--color-primary-active) 94%, transparent),
    color-mix(in srgb, var(--color-primary-active) 86%, transparent)
  );
}

.login-options {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-block: calc(var(--space-1) * -1);
}

/* Checkbox joins the material family: a tiny glass pane, frost fill and top
   highlight; checked pours in translucent brand colour. (The token border,
   tuned for solid surfaces, vanishes on glass.) */
.login-options :deep(.el-checkbox__inner) {
  border-color: light-dark(rgba(24, 24, 27, 0.3), rgba(255, 255, 255, 0.3));
  background-color: light-dark(rgba(255, 255, 255, 0.4), rgba(255, 255, 255, 0.05));
  box-shadow: inset 0 1px 0 light-dark(rgba(255, 255, 255, 0.7), rgba(255, 255, 255, 0.06));
}
.login-options :deep(.el-checkbox__input.is-checked .el-checkbox__inner) {
  border-color: transparent;
  background-color: color-mix(in srgb, var(--color-primary) 86%, transparent);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.28);
}

/* Ghost controls hover with light on glass, not the solid-surface token. */
.card-body :deep(.app-button.variant-ghost:hover:not(:disabled)) {
  background-color: light-dark(rgba(24, 24, 27, 0.06), rgba(255, 255, 255, 0.08));
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
  justify-content: center;
  margin-top: var(--space-6);
  padding-top: var(--space-4);
  border-top: var(--border-width-sm) solid var(--glass-border);
}

.footer-controls {
  display: flex;
  gap: var(--space-1);
}

/*
 * Product voice — one quiet line under the glass. It sits directly on the
 * black stage (not on glass), so it keeps a fixed dusk tone in both themes:
 * theme-relative text tokens would go dark-on-dark in light mode.
 */
.stage-tagline {
  position: relative;
  /* The bottom margin is the guaranteed breathing room above the nav when
     the column overflows and the nav's auto margin collapses to zero. */
  margin: var(--space-6) 0 var(--space-8);
  font-size: var(--text-sm);
  letter-spacing: var(--tracking-wide);
  color: rgba(228, 226, 240, 0.62);
  text-align: center;
  animation: app-slide-up 640ms var(--ease-out) 180ms both;
}

/*
 * Floating glass navigation — a satellite of the login card, same material
 * one step thinner, shaped as a pill and parked near the bottom of the
 * stage (margin-top: auto claims the leftover space; on short viewports it
 * simply follows the flow and the page scrolls). Motion is restrained to
 * colour and a soft light pool on each item — nothing translates, nothing
 * bounces, nothing overshoots.
 */
.landing-nav {
  position: relative;
  margin-top: auto;
  animation: app-slide-up 640ms var(--ease-out) 300ms both;
}

.nav-links {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  height: 56px;
  padding-inline: var(--space-5);
}

.nav-wordmark {
  margin-right: var(--space-3);
  padding-right: var(--space-4);
  border-right: var(--border-width-sm) solid
    light-dark(rgba(24, 24, 27, 0.16), rgba(255, 255, 255, 0.14));
  font-size: var(--text-sm);
  font-weight: 650;
  letter-spacing: var(--tracking-wide);
  color: var(--color-text);
  white-space: nowrap;
}

/* On-glass link text uses theme tokens — the nav's frost carries the theme
   just like the card, so token contrast holds. */
.nav-link {
  padding: var(--space-2) var(--space-3);
  border-radius: var(--radius-full);
  font-size: var(--text-sm);
  color: var(--color-text-secondary);
  text-decoration: none;
  white-space: nowrap;
  transition:
    color var(--duration-base) var(--ease-out),
    background-color var(--duration-base) var(--ease-out);
}

.nav-link:hover {
  color: var(--color-text);
  background-color: light-dark(rgba(24, 24, 27, 0.06), rgba(255, 255, 255, 0.08));
}

.nav-link:focus-visible {
  outline: var(--border-width-md) solid var(--color-focus-ring);
  outline-offset: 2px;
}

/* Colophon — the last, quietest line on the stage (fixed dusk tone, see
   .stage-tagline). */
.stage-colophon {
  position: relative;
  margin: var(--space-3) 0 0;
  font-size: var(--text-xs);
  color: rgba(228, 226, 240, 0.38);
  text-align: center;
  animation: app-fade-in 640ms var(--ease-out) 420ms both;
}

@media (max-width: 640px) {
  .login-stage {
    padding: var(--space-4);
  }

  /* Narrow screens: 52vw would shrink the artwork to a thumbnail — let it
     take most of the width while the composition stays object-in-darkness. */
  .stage-artwork {
    top: 30%;
    width: min(92vw, 460px);
  }

  .login-card {
    margin-top: clamp(64px, 12vh, 140px);
  }

  .card-body {
    padding: var(--space-8) var(--space-5) var(--space-5);
  }

  /* The card already carries the brand — the wordmark yields its width to
     the links on narrow stages. */
  .nav-wordmark {
    display: none;
  }

  .nav-links {
    gap: var(--space-1);
    height: 48px;
    padding-inline: var(--space-3);
  }

  .nav-link {
    padding: var(--space-1) var(--space-2);
    font-size: var(--text-xs);
  }
}
</style>
