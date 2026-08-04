import { useState } from 'react'
import type { Mood, ReviewRequest } from '../lib/api'

interface ReviewFormProps {
  onSubmit: (review: ReviewRequest) => void
  onCancel: () => void
  submitting: boolean
}

const MOOD_OPTIONS: { mood: Mood; emoji: string; label: string }[] = [
  { mood: 'GOOD', emoji: '😀', label: '좋았어요' },
  { mood: 'NORMAL', emoji: '😐', label: '보통' },
  { mood: 'BAD', emoji: '😱', label: '별로였어요' },
]

const CHECKBOX_OPTIONS: { key: keyof Pick<ReviewRequest, 'hasTissue' | 'hasBidet' | 'noLine' | 'isFree' | 'isClean' | 'noPasscode'>; label: string }[] = [
  { key: 'hasTissue', label: '휴지 있음' },
  { key: 'hasBidet', label: '비데 있음' },
  { key: 'noLine', label: '줄 없음' },
  { key: 'isFree', label: '무료' },
  { key: 'isClean', label: '깨끗함' },
  { key: 'noPasscode', label: '비밀번호 없음' },
]

const COMMENT_MAX_LENGTH = 200

function ReviewForm({ onSubmit, onCancel, submitting }: ReviewFormProps) {
  const [mood, setMood] = useState<Mood | null>(null)
  const [checks, setChecks] = useState<Record<string, boolean>>({})
  const [comment, setComment] = useState('')

  const toggleCheck = (key: string) => {
    setChecks((prev) => ({ ...prev, [key]: !prev[key] }))
  }

  const handleSubmit = () => {
    if (!mood) return
    onSubmit({
      mood,
      hasTissue: !!checks.hasTissue,
      hasBidet: !!checks.hasBidet,
      noLine: !!checks.noLine,
      isFree: !!checks.isFree,
      isClean: !!checks.isClean,
      noPasscode: !!checks.noPasscode,
      comment: comment.trim() ? comment.trim() : null,
    })
  }

  return (
    <div className="mt-2 rounded-2xl bg-slate-50 p-3" onClick={(e) => e.stopPropagation()}>
      <div className="mb-2 flex gap-2">
        {MOOD_OPTIONS.map((option) => (
          <button
            key={option.mood}
            type="button"
            onClick={() => setMood(option.mood)}
            className={
              mood === option.mood
                ? 'flex flex-1 flex-col items-center gap-0.5 rounded-xl border border-blue-500 bg-blue-50 py-2'
                : 'flex flex-1 flex-col items-center gap-0.5 rounded-xl border border-slate-200 bg-white py-2'
            }
          >
            <span className="text-lg">{option.emoji}</span>
            <span className="text-[10px] text-slate-500">{option.label}</span>
          </button>
        ))}
      </div>

      <div className="mb-3 grid grid-cols-2 gap-x-2 gap-y-1">
        {CHECKBOX_OPTIONS.map((option) => (
          <label key={option.key} className="flex items-center gap-1.5 text-xs text-slate-700">
            <input
              type="checkbox"
              checked={!!checks[option.key]}
              onChange={() => toggleCheck(option.key)}
            />
            {option.label}
          </label>
        ))}
      </div>

      <textarea
        value={comment}
        onChange={(e) => setComment(e.target.value.slice(0, COMMENT_MAX_LENGTH))}
        placeholder="한마디 남겨주세요 (선택)"
        rows={2}
        className="mb-3 w-full resize-none rounded-lg border border-slate-200 bg-white p-2 text-xs text-slate-700 outline-none placeholder:text-slate-400"
      />

      <div className="flex justify-end gap-2">
        <button type="button" onClick={onCancel} className="text-sm text-slate-400">취소</button>
        <button
          type="button"
          onClick={handleSubmit}
          disabled={!mood || submitting}
          className="rounded-full bg-blue-600 px-4 py-1.5 text-sm font-semibold text-white disabled:opacity-60"
        >
          {submitting ? '등록 중...' : '등록'}
        </button>
      </div>
    </div>
  )
}

export default ReviewForm
