import type { Flashcard, FlashcardDeck } from './types'

/** Phase 5 demo fixtures — replaced by the flashcards API in a later phase. */

const HOUR = 3_600_000
const DAY = 24 * HOUR
const now = Date.now()

export const mockDecks: FlashcardDeck[] = [
  {
    id: 'deck-jp-vocab',
    subjectId: 'sub-jp',
    name: 'N2 核心词汇',
    description: '按话题分组的高频词，每天 20 张。',
    cardCount: 480,
    dueCount: 23,
  },
  {
    id: 'deck-jp-grammar',
    subjectId: 'sub-jp',
    name: 'N2 语法句型',
    description: '易混句型对比与例句。',
    cardCount: 120,
    dueCount: 8,
  },
  {
    id: 'deck-ml-concepts',
    subjectId: 'sub-ml',
    name: '机器学习概念卡',
    description: '术语、公式与直觉解释。',
    cardCount: 96,
    dueCount: 11,
  },
  {
    id: 'deck-algo',
    subjectId: 'sub-algo',
    name: '复杂度速查',
    description: '常见数据结构与算法的时间/空间复杂度。',
    cardCount: 42,
    dueCount: 0,
  },
]

export const mockCards: Flashcard[] = [
  {
    id: 'card-1',
    deckId: 'deck-ml-concepts',
    front: '什么是过拟合（overfitting）？',
    back: '模型在训练集上表现很好、在新数据上表现差——学到了噪声而非规律。对策：更多数据、正则化、早停、简化模型。',
    dueAt: now - 2 * HOUR,
  },
  {
    id: 'card-2',
    deckId: 'deck-ml-concepts',
    front: 'L1 与 L2 正则化的区别？',
    back: 'L1（Lasso）把部分权重压到 0，产生稀疏解，可做特征选择；L2（Ridge）让权重整体变小但不为 0，解更平滑。',
    dueAt: now + 4 * HOUR,
  },
  {
    id: 'card-3',
    deckId: 'deck-ml-concepts',
    front: '交叉熵损失为什么配 softmax？',
    back: '两者组合的梯度是 (predicted − target)，形式简单且数值稳定，避免了 log 与 exp 分开计算时的溢出问题。',
    dueAt: now + DAY,
  },
  {
    id: 'card-4',
    deckId: 'deck-ml-concepts',
    front: '偏差-方差权衡（bias-variance tradeoff）',
    back: '偏差：模型假设太强导致欠拟合；方差：对训练数据太敏感导致过拟合。模型复杂度上升，偏差降、方差升——找中间的甜点。',
  },
  {
    id: 'card-5',
    deckId: 'deck-jp-vocab',
    front: '把握（はあく）',
    back: '掌握、把握。例：状況を把握する。',
    dueAt: now - HOUR,
  },
  {
    id: 'card-6',
    deckId: 'deck-jp-vocab',
    front: '検討（けんとう）',
    back: '研究、探讨。例：導入を検討している。',
    dueAt: now + 6 * HOUR,
  },
  {
    id: 'card-7',
    deckId: 'deck-algo',
    front: '堆（heap）插入 / 取顶的复杂度？',
    back: '插入 O(log n)，取顶 O(1)，删顶 O(log n)。建堆自底向上是 O(n)。',
  },
]

export function cardsOf(deckId: string): Flashcard[] {
  return mockCards.filter((c) => c.deckId === deckId)
}

export function totalDue(): number {
  return mockDecks.reduce((sum, d) => sum + d.dueCount, 0)
}
