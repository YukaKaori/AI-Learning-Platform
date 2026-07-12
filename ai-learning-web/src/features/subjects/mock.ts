import type { LearningMaterial, Subject } from './types'

/**
 * Phase 5 demo fixtures — replaced by the subjects API in a later phase.
 * Content strings are demo data (not UI chrome), so they don't go through
 * i18n; timestamps are relative to "now" so the workspace always feels alive.
 */

const HOUR = 3_600_000
const DAY = 24 * HOUR
const now = Date.now()

export const mockSubjects: Subject[] = [
  {
    id: 'sub-ml',
    name: '机器学习基础',
    accent: 'indigo',
    icon: 'brain',
    description: '从线性代数与概率直觉出发，走向神经网络的系统路径。',
    status: 'active',
    progress: 62,
    studyMinutes: 2340,
    lastStudiedAt: now - 3 * HOUR,
  },
  {
    id: 'sub-ts',
    name: 'TypeScript 进阶',
    accent: 'teal',
    icon: 'code',
    description: '类型体操之外——把类型系统当作设计工具。',
    status: 'active',
    progress: 45,
    studyMinutes: 1520,
    lastStudiedAt: now - DAY,
  },
  {
    id: 'sub-jp',
    name: '日语 N2',
    accent: 'amber',
    icon: 'globe',
    description: '语法、词汇与听力并行，目标十二月的能力考。',
    status: 'active',
    progress: 78,
    studyMinutes: 3110,
    lastStudiedAt: now - 26 * HOUR,
  },
  {
    id: 'sub-algo',
    name: '数据结构与算法',
    accent: 'rose',
    icon: 'network',
    description: '用可视化和刻意练习建立对复杂度的直觉。',
    status: 'active',
    progress: 30,
    studyMinutes: 980,
    lastStudiedAt: now - 3 * DAY,
  },
  {
    id: 'sub-psy',
    name: '认知心理学',
    accent: 'violet',
    icon: 'book-open',
    description: '理解记忆、注意与学习本身的科学。',
    status: 'completed',
    progress: 100,
    studyMinutes: 1750,
    lastStudiedAt: now - 12 * DAY,
  },
]

export const mockMaterials: LearningMaterial[] = [
  {
    id: 'mat-1',
    subjectId: 'sub-ml',
    title: 'Deep Learning（花书）第 5 章：机器学习基础',
    type: 'pdf',
    description: '泛化、容量、过拟合与正则化的经典章节。',
    addedAt: now - 20 * DAY,
  },
  {
    id: 'mat-2',
    subjectId: 'sub-ml',
    title: '3Blue1Brown：神经网络系列',
    type: 'video',
    sourceUrl: 'https://www.3blue1brown.com/topics/neural-networks',
    addedAt: now - 18 * DAY,
  },
  {
    id: 'mat-3',
    subjectId: 'sub-ml',
    title: '梯度下降推导笔记',
    type: 'markdown',
    addedAt: now - 6 * DAY,
  },
  {
    id: 'mat-4',
    subjectId: 'sub-ts',
    title: 'TypeScript Handbook',
    type: 'link',
    sourceUrl: 'https://www.typescriptlang.org/docs/handbook/',
    addedAt: now - 30 * DAY,
  },
  {
    id: 'mat-5',
    subjectId: 'sub-ts',
    title: 'Type-level programming 实战文章',
    type: 'article',
    sourceUrl: 'https://type-level-typescript.com',
    addedAt: now - 9 * DAY,
  },
  {
    id: 'mat-6',
    subjectId: 'sub-jp',
    title: '新完全マスター N2 语法',
    type: 'document',
    description: '主教材，每天一课。',
    addedAt: now - 60 * DAY,
  },
  {
    id: 'mat-7',
    subjectId: 'sub-jp',
    title: 'NHK Easy News 精听合集',
    type: 'link',
    sourceUrl: 'https://www3.nhk.or.jp/news/easy/',
    addedAt: now - 15 * DAY,
  },
  {
    id: 'mat-8',
    subjectId: 'sub-algo',
    title: '算法导论：分治与递归式',
    type: 'pdf',
    addedAt: now - 11 * DAY,
  },
  {
    id: 'mat-9',
    subjectId: 'sub-algo',
    title: '可视化：排序算法对比',
    type: 'video',
    sourceUrl: 'https://visualgo.net',
    addedAt: now - 4 * DAY,
  },
  {
    id: 'mat-10',
    subjectId: 'sub-psy',
    title: '《认知心理学及其启示》全书笔记',
    type: 'markdown',
    addedAt: now - 40 * DAY,
  },
]

export function getSubject(id: string): Subject | undefined {
  return mockSubjects.find((s) => s.id === id)
}

export function materialsOf(subjectId: string): LearningMaterial[] {
  return mockMaterials.filter((m) => m.subjectId === subjectId)
}
