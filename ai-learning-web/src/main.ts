import { createApp } from 'vue'
import { createPinia } from 'pinia'

import 'element-plus/theme-chalk/dark/css-vars.css'
import '@/styles/index.css'

import App from './App.vue'
import router from './router'
import { i18n } from '@/locales'
import { useAppStore } from '@/stores/app'

const app = createApp(App)

app.use(createPinia())
app.use(i18n)
app.use(router)

// Apply persisted theme/locale before mounting to avoid a flash of wrong theme.
useAppStore().init()

app.mount('#app')
