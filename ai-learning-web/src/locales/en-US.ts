import type zhCN from './zh-CN'

const enUS: typeof zhCN = {
  app: {
    name: 'AI Learning Platform',
  },
  nav: {
    home: 'Workspace',
  },
  common: {
    retry: 'Retry',
    loading: 'Loading',
    language: 'Language',
    theme: {
      label: 'Appearance',
      light: 'Light',
      dark: 'Dark',
      system: 'System',
    },
  },
  home: {
    title: 'Workspace',
    subtitle: 'Your AI learning space is under construction',
    status: {
      title: 'Service status',
      connected: 'Backend connected',
      disconnected: 'Cannot reach the backend service',
      profile: 'Profile',
      serverTime: 'Server time',
    },
  },
  error: {
    network: 'Network error, please try again later',
    timeout: 'Request timed out, please try again later',
    server: 'Service temporarily unavailable',
    unknown: 'An unknown error occurred',
  },
}

export default enUS
