<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { ApiError } from '@/api/types'
import { useAuthStore } from '@/stores/auth'
import { useAppStore, type ThemeMode } from '@/stores/app'
import { AppButton, AppInput, GlassSurface } from '@/components'
import type { IconName } from '@/components'
import lotusUrl from '@/assets/login/pinklotus.png'

// The opening scene of the product: a pink lotus breathing on a black
// cinematic stage, with the sign-in form floating on a sheet of real
// refractive glass (GlassSurface) in front of it.

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
  <main class="login-stage">
    <img
      class="stage-lotus"
      :src="lotusUrl"
      alt=""
      aria-hidden="true"
      decoding="async"
      fetchpriority="high"
    />
    <div class="stage-vignette" aria-hidden="true"></div>

    <GlassSurface
      class="login-card"
      width="100%"
      height="auto"
      :border-radius="28"
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
 * The stage — deliberately black in BOTH themes. The lotus artwork carries
 * its own black field, so the page extends it edge-to-edge; theme choice is
 * expressed by the glass card, not the backdrop.
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
 * The lotus fills the viewport (cover never stretches; the surround is pure
 * black so crops are invisible) and is kept optically centered — the bloom
 * sits at ~55% / 42% of the source frame. It breathes on a slow alternate
 * loop; transform/opacity only, so it stays on the compositor.
 */
.stage-lotus {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
  object-position: 55% 42%;
  animation: app-breathe 14s var(--ease-in-out) infinite alternate;
}

/* Rose aura around the bloom + a corner vignette to pull focus inward. */
.stage-vignette {
  position: absolute;
  inset: 0;
  pointer-events: none;
  background:
    radial-gradient(42% 36% at 50% 42%, var(--scene-aura), transparent 70%),
    radial-gradient(120% 100% at 50% 42%, transparent 38%, rgba(0, 0, 0, 0.45) 74%, rgba(0, 0, 0, 0.78) 100%);
}

/*
 * The glass sheet floats in front of the bloom so GlassSurface has something
 * luminous to refract. Entrance: one soft rise, then stillness — the motion
 * belongs to the lotus, not the card.
 */
.login-card {
  position: relative;
  max-width: 520px;
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

  .card-body {
    padding: var(--space-8) var(--space-5) var(--space-5);
  }
}
</style>
