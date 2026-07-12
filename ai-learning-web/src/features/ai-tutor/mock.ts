import type { Conversation } from './types'

/** Phase 5 demo fixtures — replaced by conversation persistence in the AI phase. */

const HOUR = 3_600_000
const DAY = 24 * HOUR
const now = Date.now()

export const mockConversations: Conversation[] = [
  {
    id: 'conv-1',
    title: '梯度消失是怎么发生的？',
    subjectId: 'sub-ml',
    updatedAt: now - 3 * HOUR,
    messages: [
      {
        id: 'm-1-1',
        role: 'user',
        content: '深层网络里梯度消失到底是怎么发生的？',
        createdAt: now - 3 * HOUR - 4 * 60_000,
      },
      {
        id: 'm-1-2',
        role: 'assistant',
        content:
          '想象梯度从输出层往回走：每经过一层，都要乘上该层激活函数的导数。sigmoid 的导数最大只有 0.25——十层下来梯度最多剩 0.25¹⁰ ≈ 百万分之一，前面的层几乎学不到东西。\n\n这也是三个经典对策的由来：ReLU（正区间导数恒为 1）、残差连接（给梯度一条不经过乘法的捷径）、以及合适的初始化（让每层的方差大致守恒）。',
        createdAt: now - 3 * HOUR,
      },
    ],
  },
  {
    id: 'conv-2',
    title: 'ばかりに 和 だけに 的区别',
    subjectId: 'sub-jp',
    updatedAt: now - DAY,
    messages: [
      {
        id: 'm-2-1',
        role: 'user',
        content: 'ばかりに 和 だけに 都翻译成"正因为"，考试里怎么区分？',
        createdAt: now - DAY - 3 * 60_000,
      },
      {
        id: 'm-2-2',
        role: 'assistant',
        content:
          '抓住感情色彩：ばかりに 几乎总是接**坏结果**，带着"都怪这一个原因"的懊悔；だけに 是"名副其实"，结果与原因相称，好坏皆可。\n\n考试技巧：看句尾——消极结局选 ばかりに；中性或积极、强调程度相称的选 だけに。',
        createdAt: now - DAY,
      },
    ],
  },
  {
    id: 'conv-3',
    title: '帮我规划算法学习路径',
    subjectId: 'sub-algo',
    updatedAt: now - 2 * DAY,
    messages: [
      {
        id: 'm-3-1',
        role: 'user',
        content: '我算法基础比较弱，三个月想系统补一遍，怎么安排？',
        createdAt: now - 2 * DAY - 6 * 60_000,
      },
      {
        id: 'm-3-2',
        role: 'assistant',
        content:
          '三个月按"结构 → 思想 → 综合"分期：\n\n第 1 个月：数组/链表/栈/队列/哈希/树，每个结构配 10 道基础题，重点是手写实现。\n\n第 2 个月：二分、双指针、分治、回溯、动态规划——一周一个思想，先看模板题再做变形。\n\n第 3 个月：混合刷题 + 复盘错题，把每道错题归因到"哪个思想没识别出来"。\n\n配合你的间隔重复卡片记复杂度结论，效果会更稳。',
        createdAt: now - 2 * DAY,
      },
    ],
  },
]
