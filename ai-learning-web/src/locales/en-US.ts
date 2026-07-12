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
  welcome: {
    title: 'Welcome',
    hero: {
      eyebrow: 'The AI-native learning space',
      titleLine1: 'Learn Beyond',
      titleLine2: 'Knowledge',
      w1: 'Think',
      w2: 'Create',
      w3: 'Grow',
      subtitle: 'Your intelligent learning companion.',
      start: 'Start Learning',
      discover: 'Discover Platform',
      scrollHint: 'Scroll to explore',
    },
    philosophy: {
      title: 'AI Learning Philosophy',
      subtitle: 'Not about finishing courses faster — about understanding the world more deeply.',
      items: {
        tutor: {
          title: 'AI Tutor',
          desc: 'A guide who is always present, understands your goals, and asks the right question at the right moment.',
        },
        graph: {
          title: 'Knowledge Graph',
          desc: 'Knowledge is not a list but a living network — see how concepts connect and grow together.',
        },
        personal: {
          title: 'Personal Learning',
          desc: 'Pace, path and depth belong to you — the platform adapts as you grow.',
        },
      },
    },
    capabilities: {
      title: 'Core Capabilities',
      subtitle: 'A complete toolkit designed for deep learning.',
      items: {
        assistant: {
          title: 'AI Assistant',
          desc: 'Conversational answers that lead from a question to a topic.',
        },
        analytics: {
          title: 'Learning Analytics',
          desc: 'See your progress curve and your blind spots.',
        },
        playground: {
          title: 'Coding Playground',
          desc: 'Practice, run and verify code right in the browser.',
        },
        flashcards: {
          title: 'Flashcards',
          desc: 'Spaced repetition built on the forgetting curve.',
        },
        workspace: {
          title: 'Study Workspace',
          desc: 'Notes, tasks and materials in one calm place.',
        },
        path: {
          title: 'Learning Path',
          desc: 'A roadmap derived from your goals — every step has direction.',
        },
      },
    },
    journey: {
      title: 'Learning Journey',
      subtitle: 'From meeting a concept to creating something of your own.',
      steps: {
        discover: { title: 'Discover', desc: 'Meet new fields and questions' },
        learn: { title: 'Learn', desc: 'Build concepts and structure' },
        practice: { title: 'Practice', desc: 'Apply them in real scenarios' },
        master: { title: 'Master', desc: 'Connect the dots into intuition' },
        create: { title: 'Create', desc: 'Produce work that is yours' },
      },
    },
    values: {
      title: 'Platform Values',
      subtitle: 'The qualities we believe a great learning tool must have.',
      items: {
        personalized: { title: 'Personalized', desc: 'Built around your goals and pace' },
        reliable: { title: 'Reliable', desc: 'Engineering quality you can trust' },
        private: { title: 'Private', desc: 'Your data belongs to you alone' },
        aiNative: { title: 'AI Native', desc: 'Intelligence woven into every interaction' },
        modern: { title: 'Modern', desc: 'Technology and experience of this era' },
        beautiful: { title: 'Beautiful', desc: 'Calm, restrained, worth looking at' },
      },
    },
    cta: {
      title: 'Ready to begin?',
      subtitle: 'Your learning space is ready.',
      action: 'Start Learning',
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
      rememberMe: 'Remember me',
      forgotPassword: 'Forgot password',
      forgotPasswordHint:
        'Password reset arrives in a later release — please contact your administrator.',
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
