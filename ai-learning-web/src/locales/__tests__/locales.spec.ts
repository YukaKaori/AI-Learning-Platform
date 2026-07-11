import { describe, expect, it } from 'vitest'
import zhCN from '../zh-CN'
import enUS from '../en-US'

function flattenKeys(value: unknown, prefix = ''): string[] {
  if (typeof value !== 'object' || value === null) {
    return [prefix]
  }
  return Object.entries(value).flatMap(([key, child]) =>
    flattenKeys(child, prefix ? `${prefix}.${key}` : key),
  )
}

describe('locale messages', () => {
  it('en-US covers exactly the same keys as zh-CN', () => {
    expect(flattenKeys(enUS).sort()).toEqual(flattenKeys(zhCN).sort())
  })

  it('has no empty translations', () => {
    for (const messages of [zhCN, enUS]) {
      const leaves = JSON.stringify(messages)
      expect(leaves).not.toContain('""')
    }
  })
})
