import type { LearningTask } from './types'

/** Phase 5 demo fixtures — replaced by the tasks API in a later phase. */

const HOUR = 3_600_000
const DAY = 24 * HOUR
const now = Date.now()

export const mockTasks: LearningTask[] = [
  {
    id: 'task-1',
    subjectId: 'sub-ml',
    title: '完成花书第 5 章课后习题 1–6',
    status: 'inProgress',
    priority: 'high',
    dueAt: now + 5 * HOUR,
  },
  {
    id: 'task-2',
    subjectId: 'sub-jp',
    title: '背诵 N2 语法第 12 课例句',
    status: 'todo',
    priority: 'medium',
    dueAt: now + 8 * HOUR,
  },
  {
    id: 'task-3',
    subjectId: 'sub-algo',
    title: '实现并测试快速排序的三路划分',
    status: 'todo',
    priority: 'medium',
    dueAt: now + 10 * HOUR,
  },
  {
    id: 'task-4',
    subjectId: 'sub-ts',
    title: '重读 conditional types 一节并整理笔记',
    status: 'todo',
    priority: 'low',
    dueAt: now + 2 * DAY,
  },
  {
    id: 'task-5',
    subjectId: 'sub-ml',
    title: '整理反向传播推导笔记',
    status: 'done',
    priority: 'high',
    dueAt: now - DAY,
  },
  {
    id: 'task-6',
    title: '规划下个月的学习目标',
    status: 'todo',
    priority: 'low',
    dueAt: now + 4 * DAY,
  },
]

/** Tasks due within the local "today", not yet done. */
export function tasksDueToday(): LearningTask[] {
  const start = new Date()
  start.setHours(0, 0, 0, 0)
  const end = start.getTime() + DAY
  return mockTasks.filter((t) => t.dueAt !== undefined && t.dueAt >= start.getTime() && t.dueAt < end)
}
