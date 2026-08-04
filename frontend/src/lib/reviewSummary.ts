import type { ReviewResponse } from './api'

interface TagRule {
  key: keyof Pick<ReviewResponse, 'hasTissue' | 'hasBidet' | 'noLine' | 'isFree' | 'isClean' | 'noPasscode'>
  positiveLabel: string
  negativeLabel: string | null
}

const TAG_RULES: TagRule[] = [
  { key: 'hasTissue', positiveLabel: '휴지 항상 있음', negativeLabel: '휴지 없을 때 있음' },
  { key: 'isClean', positiveLabel: '깨끗함', negativeLabel: '청결 아쉬움' },
  { key: 'noLine', positiveLabel: '줄 거의 없음', negativeLabel: '줄 있는 편' },
  { key: 'hasBidet', positiveLabel: '비데 있음', negativeLabel: null },
  { key: 'isFree', positiveLabel: '무료', negativeLabel: null },
  { key: 'noPasscode', positiveLabel: '비밀번호 없음', negativeLabel: null },
]

export interface ReviewTagSummary {
  positive: string[]
  negative: string[]
}

export function summarizeReviewTags(reviews: ReviewResponse[], maxPositive = 3, maxNegative = 1): ReviewTagSummary {
  if (reviews.length === 0) {
    return { positive: [], negative: [] }
  }

  const scored = TAG_RULES.map((rule) => {
    const trueCount = reviews.filter((r) => r[rule.key]).length
    return { rule, ratio: trueCount / reviews.length }
  })

  const positive = scored
    .filter((s) => s.ratio >= 0.6)
    .sort((a, b) => b.ratio - a.ratio)
    .slice(0, maxPositive)
    .map((s) => s.rule.positiveLabel)

  const negative = scored
    .filter((s) => s.rule.negativeLabel && s.ratio <= 0.4)
    .sort((a, b) => a.ratio - b.ratio)
    .slice(0, maxNegative)
    .map((s) => s.rule.negativeLabel as string)

  return { positive, negative }
}
