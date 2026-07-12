import type { StudySession } from './types'

/** Phase 5 demo fixtures — sessions spread over the current week so both
 * calendar views and the workspace "today" panel have something to show. */

const HOUR = 3_600_000
const DAY = 24 * HOUR

/** Start of a day offset from today (local midnight). */
function day(offset: number): number {
  const d = new Date()
  d.setHours(0, 0, 0, 0)
  return d.getTime() + offset * DAY
}

export const mockSessions: StudySession[] = [
  // Today
  {
    id: 'ses-1',
    subjectId: 'sub-ml',
    title: '深度工作：花书第 5 章',
    startsAt: day(0) + 9 * HOUR,
    endsAt: day(0) + 10.5 * HOUR,
  },
  {
    id: 'ses-2',
    subjectId: 'sub-jp',
    title: 'N2 听力精听',
    startsAt: day(0) + 14 * HOUR,
    endsAt: day(0) + 15 * HOUR,
  },
  {
    id: 'ses-3',
    subjectId: 'sub-algo',
    title: '算法题：分治专题',
    startsAt: day(0) + 20 * HOUR,
    endsAt: day(0) + 21 * HOUR,
  },
  // Earlier this week
  {
    id: 'ses-4',
    subjectId: 'sub-ml',
    startsAt: day(-1) + 9 * HOUR,
    endsAt: day(-1) + 11 * HOUR,
  },
  {
    id: 'ses-5',
    subjectId: 'sub-ts',
    title: '类型体操练习',
    startsAt: day(-1) + 19 * HOUR,
    endsAt: day(-1) + 20 * HOUR,
  },
  {
    id: 'ses-6',
    subjectId: 'sub-jp',
    startsAt: day(-2) + 8 * HOUR,
    endsAt: day(-2) + 9 * HOUR,
  },
  {
    id: 'ses-7',
    subjectId: 'sub-psy',
    title: '复习：记忆的编码与提取',
    startsAt: day(-3) + 15 * HOUR,
    endsAt: day(-3) + 16.5 * HOUR,
  },
  // Upcoming
  {
    id: 'ses-8',
    subjectId: 'sub-ts',
    title: '重构练习项目的类型层',
    startsAt: day(1) + 10 * HOUR,
    endsAt: day(1) + 12 * HOUR,
  },
  {
    id: 'ses-9',
    subjectId: 'sub-jp',
    title: 'N2 模拟卷（限时）',
    startsAt: day(2) + 9 * HOUR,
    endsAt: day(2) + 11 * HOUR,
  },
  {
    id: 'ses-10',
    subjectId: 'sub-ml',
    startsAt: day(3) + 9 * HOUR,
    endsAt: day(3) + 10 * HOUR,
  },
]

export function sessionsOfDay(dayStart: number): StudySession[] {
  const end = dayStart + DAY
  return mockSessions
    .filter((s) => s.startsAt >= dayStart && s.startsAt < end)
    .sort((a, b) => a.startsAt - b.startsAt)
}
