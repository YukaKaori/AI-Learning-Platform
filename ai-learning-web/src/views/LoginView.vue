<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { ApiError } from '@/api/types'
import { useAuthStore } from '@/stores/auth'

// Functional version only — the signature login experience ships in a later phase.

const { t } = useI18n()
const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const form = reactive({
  usernameOrEmail: '',
  password: '',
})
const submitting = ref(false)
const errorKey = ref<string | null>(null)

/** Auth error codes → user-facing i18n keys (see AuthErrorCode.java). */
const AUTH_ERROR_KEYS: Record<number, string> = {
  100000: 'auth.error.invalidCredentials',
  100001: 'auth.error.accountLocked',
  100002: 'auth.error.accountDisabled',
}

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
    const redirect = route.query.redirect
    await router.replace(typeof redirect === 'string' && redirect.startsWith('/') ? redirect : '/')
  } catch (error) {
    errorKey.value =
      error instanceof ApiError
        ? (AUTH_ERROR_KEYS[error.code] ?? error.messageKey)
        : 'error.unknown'
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="login-page">
    <main class="login-card">
      <header class="login-header">
        <span class="brand-mark" aria-hidden="true"></span>
        <h1 class="login-title">{{ t('auth.login.title') }}</h1>
        <p class="login-subtitle">{{ t('auth.login.subtitle') }}</p>
      </header>

      <form class="login-form" novalidate @submit.prevent="submit">
        <el-input
          v-model="form.usernameOrEmail"
          :placeholder="t('auth.login.usernameOrEmail')"
          name="username"
          autocomplete="username"
          size="large"
        />
        <el-input
          v-model="form.password"
          type="password"
          :placeholder="t('auth.login.password')"
          name="password"
          autocomplete="current-password"
          show-password
          size="large"
        />

        <p v-if="errorKey" class="login-error" role="alert">{{ t(errorKey) }}</p>

        <el-button
          class="login-submit"
          type="primary"
          size="large"
          native-type="submit"
          :loading="submitting"
        >
          {{ t('auth.login.submit') }}
        </el-button>
      </form>
    </main>
  </div>
</template>

<style scoped>
.login-page {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 100%;
  padding: var(--space-6);
  background-color: var(--color-bg);
}

.login-card {
  width: 100%;
  max-width: 360px;
  padding: var(--space-10) var(--space-8);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  background-color: var(--color-surface);
  box-shadow: var(--shadow-md);
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
  width: 32px;
  height: 32px;
  margin-bottom: var(--space-2);
  border-radius: var(--radius-md);
  background: linear-gradient(135deg, var(--color-primary), var(--color-primary-hover));
}

.login-title {
  font-size: var(--text-xl);
  font-weight: 600;
  letter-spacing: -0.01em;
  color: var(--color-text);
}

.login-subtitle {
  font-size: var(--text-sm);
  color: var(--color-text-secondary);
}

.login-form {
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
}

.login-error {
  font-size: var(--text-sm);
  color: var(--color-danger);
}

.login-submit {
  width: 100%;
}

@media (max-width: 480px) {
  .login-card {
    max-width: 100%;
    padding: var(--space-8) var(--space-5);
    border: none;
    box-shadow: none;
    background-color: transparent;
  }
}
</style>
