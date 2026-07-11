import { defineStore } from 'pinia'
import { i18n, DEFAULT_LOCALE, isSupportedLocale, type AppLocale } from '@/locales'

export type ThemeMode = 'light' | 'dark' | 'system'

const THEME_STORAGE_KEY = 'alp.theme'
const LOCALE_STORAGE_KEY = 'alp.locale'

const darkQuery = window.matchMedia('(prefers-color-scheme: dark)')

function isThemeMode(value: string | null): value is ThemeMode {
  return value === 'light' || value === 'dark' || value === 'system'
}

/**
 * Global UI state: theme and locale. Both persist across sessions.
 * Call `init()` once in main.ts before mounting.
 */
export const useAppStore = defineStore('app', {
  state: () => ({
    themeMode: 'system' as ThemeMode,
    locale: DEFAULT_LOCALE as AppLocale,
  }),

  getters: {
    isDark(state): boolean {
      // themeMode is tracked reactively; the system preference is re-read on change events.
      return state.themeMode === 'dark' || (state.themeMode === 'system' && darkQuery.matches)
    },
  },

  actions: {
    init() {
      const storedTheme = localStorage.getItem(THEME_STORAGE_KEY)
      if (isThemeMode(storedTheme)) {
        this.themeMode = storedTheme
      }
      const storedLocale = localStorage.getItem(LOCALE_STORAGE_KEY)
      if (isSupportedLocale(storedLocale)) {
        this.locale = storedLocale
      }
      i18n.global.locale.value = this.locale
      this.applyTheme()
      darkQuery.addEventListener('change', () => {
        if (this.themeMode === 'system') {
          this.applyTheme()
        }
      })
    },

    setThemeMode(mode: ThemeMode) {
      this.themeMode = mode
      localStorage.setItem(THEME_STORAGE_KEY, mode)
      this.applyTheme()
    },

    setLocale(locale: AppLocale) {
      this.locale = locale
      localStorage.setItem(LOCALE_STORAGE_KEY, locale)
      i18n.global.locale.value = locale
      document.documentElement.lang = locale
    },

    applyTheme() {
      document.documentElement.classList.toggle('dark', this.isDark)
    },
  },
})
