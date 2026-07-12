import type { Note } from './types'

/** Phase 5 demo fixtures — replaced by the notes API in a later phase. */

const HOUR = 3_600_000
const DAY = 24 * HOUR
const now = Date.now()

export const mockNotes: Note[] = [
  {
    id: 'note-1',
    subjectId: 'sub-ml',
    title: '反向传播：一次讲清楚',
    pinned: true,
    updatedAt: now - 2 * HOUR,
    content: `# 反向传播：一次讲清楚

核心只有一句话：**链式法则 + 动态规划**。

## 前向传播

每一层把输入变换为输出，同时缓存中间值（激活前的 z 和激活后的 a），
这些缓存就是反向阶段的"备忘录"。

## 反向传播

从损失函数出发，逐层向后计算梯度：

- 输出层：dL/dz = a - y（交叉熵 + softmax 的漂亮结果）
- 隐藏层：把上一层的梯度乘以权重的转置，再逐元素乘激活函数的导数

## 为什么高效

朴素地对每个参数求偏导是 O(参数数 × 前向成本)；
反向传播共享中间结果，一次反向扫过就拿到全部梯度。`,
  },
  {
    id: 'note-2',
    subjectId: 'sub-ts',
    title: 'conditional types 的分配律',
    pinned: false,
    updatedAt: now - DAY,
    content: `# conditional types 的分配律

裸类型参数在条件类型中会对联合类型**分配**：

## 规则

\`T extends U ? X : Y\` 中，若 T 是裸参数且传入联合，
则逐成员求值再合并结果。

## 关掉分配

把两边包进元组：\`[T] extends [U] ? X : Y\`。

## 什么时候在意

写 \`Exclude\` / \`Extract\` 这类工具类型时，分配正是你要的；
判断"整体是否为某联合"时，必须关掉。`,
  },
  {
    id: 'note-3',
    subjectId: 'sub-jp',
    title: 'N2 语法辨析：ばかりに / だけに',
    pinned: false,
    updatedAt: now - 2 * DAY,
    content: `# ばかりに / だけに

## ばかりに

只因为——强调**负面结果**的唯一原因。
例：留学したいばかりに、必死でお金を貯めている。

## だけに

正因为——结果与身份/程度**相称**，可好可坏。
例：教師だけに、言葉遣いがきれいだ。

## 记忆锚点

「ばかり」= 只 → 只怪它；「だけ」= 正 → 名副其实。`,
  },
  {
    id: 'note-4',
    subjectId: 'sub-algo',
    title: '快速排序为什么要三路划分',
    pinned: false,
    updatedAt: now - 4 * DAY,
    content: `# 快速排序为什么要三路划分

## 问题

大量重复元素时，二路划分退化成 O(n²)——
等于 pivot 的元素每轮都参与递归。

## 三路划分

把数组切成 < pivot、= pivot、> pivot 三段，
相等段直接排除在递归外。

## 结论

重复率高的数据（枚举值、日志级别）收益巨大；
完全随机数据则几乎无差别。`,
  },
  {
    id: 'note-5',
    subjectId: 'sub-psy',
    title: '间隔重复背后的认知科学',
    pinned: false,
    updatedAt: now - 9 * DAY,
    content: `# 间隔重复背后的认知科学

## 遗忘曲线

Ebbinghaus：记忆保持率随时间指数衰减，
但每次成功提取都会让曲线变平。

## 提取练习效应

主动回忆比重复阅读有效得多——
"想起来"这个动作本身在加固记忆。

## 对平台的启示

复习间隔应该由算法安排在"将忘未忘"的临界点，
这正是记忆卡片模块的设计依据。`,
  },
  {
    id: 'note-6',
    title: '本周学习复盘',
    pinned: false,
    updatedAt: now - 3 * DAY,
    content: `# 本周学习复盘

做得好的：机器学习保持了每天 90 分钟的深度工作。

需要调整的：算法练习总被排到深夜，效率低——
下周移到早晨第一件事。

一个洞察：给 AI 导师复述概念，比自己默读检验理解快得多。`,
  },
]
