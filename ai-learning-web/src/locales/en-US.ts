import type zhCN from './zh-CN'

const enUS: typeof zhCN = {
  app: {
    name: 'AI Learning Platform',
  },
  nav: {
    home: 'Workspace',
  },
  designSystem: {
    title: 'Design System',
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
  ds: {
    input: {
      showPassword: 'Show password',
      hidePassword: 'Hide password',
      clear: 'Clear',
    },
    empty: {
      title: 'No data',
    },
    search: {
      placeholder: 'Search…',
      clear: 'Clear search',
    },
    tag: {
      remove: 'Remove',
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
  auth: {
    login: {
      title: 'Sign in',
      subtitle: 'Sign in to continue to your AI learning space',
      usernameOrEmail: 'Username or email',
      password: 'Password',
      submit: 'Sign in',
      required: 'Please fill in all sign-in fields',
    },
    logout: 'Sign out',
    error: {
      invalidCredentials: 'Incorrect username or password',
      accountLocked: 'This account is locked — please contact an administrator',
      accountDisabled: 'This account is disabled — please contact an administrator',
      sessionExpired: 'Your session has expired, please sign in again',
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
