import { useEffect, useState } from 'react'
import type { UrgentPlace, ReviewResponse, ReviewRequest } from '../lib/api'
import { formatWalkingTime, fetchReviews, kakaoDirectionsUrl, naverMapSearchUrl } from '../lib/api'
import { summarizeReviewTags } from '../lib/reviewSummary'
import { TIER_CLASS, tierOf } from '../lib/tier'
import { resolvePlaceType, isInternallyManaged, type PlaceType } from '../lib/placeType'
import ReviewForm from './ReviewForm'

interface DetailCardProps {
  place: UrgentPlace
  origin: { lat: number; lng: number } | null
  onSubmitReview: (restroomId: string, review: ReviewRequest) => void
  submittingReview: boolean
  refreshKey: number
}

const MOOD_EMOJI: Record<ReviewResponse['mood'], string> = {
  GOOD: '😀',
  NORMAL: '😐',
  BAD: '😱',
}

export function categoryIcon(place: UrgentPlace): string {
  if (place.type === 'TIP') {
    if (place.category === '편의점') return '🏪'
    if (place.category === '카페') return '☕'
    if (place.category === '주유소') return '⛽'
    return '🏬'
  }
  return '🚻'
}

function moodCounts(reviews: ReviewResponse[]) {
  return {
    good: reviews.filter((r) => r.mood === 'GOOD').length,
    normal: reviews.filter((r) => r.mood === 'NORMAL').length,
    bad: reviews.filter((r) => r.mood === 'BAD').length,
  }
}

function timeAgo(isoString: string): string {
  const diffMs = Date.now() - new Date(isoString).getTime()
  const minutes = Math.floor(diffMs / 60000)
  if (minutes < 1) return '방금 전'
  if (minutes < 60) return `${minutes}분 전`
  const hours = Math.floor(minutes / 60)
  if (hours < 24) return `${hours}시간 전`
  return `${Math.floor(hours / 24)}일 전`
}

interface InternalReviewSectionProps {
  place: UrgentPlace
  tags: { positive: string[]; negative: string[] }
  moodCounts: { good: number; normal: number; bad: number }
  recentComments: ReviewResponse[]
  showReviewForm: boolean
  onToggleReviewForm: () => void
  submittingReview: boolean
  onSubmitReview: (restroomId: string, review: ReviewRequest) => void
}

function InternalReviewSection({
  place,
  tags,
  moodCounts: counts,
  recentComments,
  showReviewForm,
  onToggleReviewForm,
  submittingReview,
  onSubmitReview,
}: InternalReviewSectionProps) {
  const reviewCount = counts.good + counts.normal + counts.bad

  return (
    <div className="pb-3">
      <div className="flex items-center gap-2 text-xs">
        <span className="flex items-center gap-1">
          <span className="text-amber-400">★</span>
          {place.averageRating !== null ? (
            <span className="font-semibold">{place.averageRating.toFixed(1)}</span>
          ) : (
            <span className="text-[11px] text-slate-400">첫 리뷰를 남겨보세요</span>
          )}
        </span>
        {reviewCount > 0 && (
          <span className="text-[11px] text-slate-400">
            😀{counts.good} 😐{counts.normal} 😱{counts.bad}
          </span>
        )}
      </div>

      {place.hasDiaperTable && (
        <div className="mt-2 w-fit rounded-lg bg-emerald-50 px-2 py-1 text-[11px] text-emerald-700">
          🛡️ 안심 · 기저귀교환대 있음
        </div>
      )}

      <div className="mt-2 flex flex-wrap gap-1">
        {tags.positive.map((t) => (
          <span key={t} className="rounded-full bg-emerald-50 px-1.5 py-0.5 text-[11px] font-medium text-emerald-700">
            👍 {t}
          </span>
        ))}
        {tags.negative.map((t) => (
          <span key={t} className="rounded-full bg-red-50 px-1.5 py-0.5 text-[11px] font-medium text-red-600">
            👎 {t}
          </span>
        ))}
        {/* TODO: 남녀분리/장애인 화장실 여부는 아직 백엔드 데이터가 없음 (Restroom 도메인에 컬럼 추가 필요) */}
        <span className="rounded-full bg-slate-50 px-1.5 py-0.5 text-[11px] font-medium text-slate-400">
          남녀분리 정보 없음
        </span>
        <span className="rounded-full bg-slate-50 px-1.5 py-0.5 text-[11px] font-medium text-slate-400">
          장애인 화장실 정보 없음
        </span>
      </div>

      {recentComments.length > 0 && (
        <div className="mt-2 space-y-1">
          <p className="text-[11px] font-semibold text-slate-400">최근 후기</p>
          {recentComments.map((r) => (
            <div key={r.id} className="flex items-center gap-2 rounded-lg bg-slate-50 px-2.5 py-1.5 text-xs">
              <span>{MOOD_EMOJI[r.mood]}</span>
              <span className="flex-1 truncate text-slate-600">{r.comment}</span>
              <span className="shrink-0 text-[10px] text-slate-300">{timeAgo(r.createdAt)}</span>
            </div>
          ))}
        </div>
      )}

      {showReviewForm && (
        <ReviewForm
          submitting={submittingReview}
          onCancel={onToggleReviewForm}
          onSubmit={(review) => onSubmitReview(place.id, review)}
        />
      )}
    </div>
  )
}

function ExternalPlaceInfoSection({ place, placeType }: { place: UrgentPlace; placeType: PlaceType }) {
  return (
    <div className="pb-3">
      <div className="w-fit rounded-lg bg-amber-50 px-2 py-1 text-[11px] text-amber-700">
        💡 {place.tip ?? '화장실 이용 가능 여부는 매장 직원에게 확인해 주세요.'}
      </div>

      {place.phone && (
        <a href={`tel:${place.phone}`} className="mt-2 flex w-fit items-center gap-1 text-xs font-medium text-slate-600">
          📞 {place.phone}
        </a>
      )}

      {/* TODO: 카카오/네이버 별점·리뷰 수는 각 Place Detail API 연동 전까지 실제 수치를 표시하지 않음
          (허위 평점 표기를 피하기 위해 항상 "정보 없음" 안내로 대체) */}
      <div className="mt-2 rounded-lg bg-slate-50 px-2.5 py-2 text-[11px] text-slate-400">
        외부 리뷰 정보가 없습니다.
        <br />
        화장실 이용 가능 여부는 매장 직원에게 확인해 주세요.
      </div>

      {placeType === 'CAFE' && (
        <a
          href={naverMapSearchUrl(place.name)}
          target="_blank"
          rel="noreferrer"
          className="mt-2 block rounded-lg border border-slate-200 py-2 text-center text-[11px] font-semibold text-slate-600"
        >
          💬 네이버에서 리뷰 보기
        </a>
      )}
    </div>
  )
}

function DetailCard({ place, origin, onSubmitReview, submittingReview, refreshKey }: DetailCardProps) {
  const placeType = resolvePlaceType(place)
  const isInternal = isInternallyManaged(placeType)
  const tier = TIER_CLASS[tierOf(place.walkingTimeSeconds)]

  const [tags, setTags] = useState<{ positive: string[]; negative: string[] }>({ positive: [], negative: [] })
  const [counts, setCounts] = useState({ good: 0, normal: 0, bad: 0 })
  const [recentComments, setRecentComments] = useState<ReviewResponse[]>([])
  const [showReviewForm, setShowReviewForm] = useState(false)
  // TODO: 즐겨찾기는 현재 로컬 상태로만 표시되며 저장/서버 연동은 없음 (기획서상 미구현 기능)
  const [favorited, setFavorited] = useState(false)

  useEffect(() => {
    setShowReviewForm(false)
    setFavorited(false)
    if (!isInternal) {
      setTags({ positive: [], negative: [] })
      setCounts({ good: 0, normal: 0, bad: 0 })
      setRecentComments([])
      return
    }
    let cancelled = false
    fetchReviews(place.id)
      .then((reviews) => {
        if (cancelled) return
        setTags(summarizeReviewTags(reviews))
        setCounts(moodCounts(reviews))
        setRecentComments(reviews.filter((r) => r.comment).slice(0, 2))
      })
      .catch(() => {
        // 요약은 부가 정보라 실패해도 조용히 무시
      })
    return () => {
      cancelled = true
    }
  }, [place.id, isInternal, refreshKey])

  return (
    <div key={place.id} className="chamjima-detail-fade flex h-full flex-col overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm">
      <div className="flex shrink-0 gap-3 p-3">
        <div className={`flex h-14 w-14 shrink-0 items-center justify-center rounded-xl ${tier.bg}`}>
          <span className="text-2xl">{categoryIcon(place)}</span>
        </div>
        <div className="min-w-0 flex-1">
          <div className={`font-mono-time inline-block rounded-xl px-2.5 py-1 text-2xl font-extrabold leading-none ${tier.bg} ${tier.text}`}>
            {formatWalkingTime(place.walkingTimeSeconds)}
          </div>
          <div className="mt-1.5 flex items-center gap-1.5">
            <h3 className="truncate text-base font-bold text-slate-900">{place.name}</h3>
            <span className="shrink-0 rounded bg-blue-50 px-1.5 py-0.5 text-[10px] font-semibold text-blue-600">
              {place.category}
            </span>
          </div>
          <p className="font-mono-time mt-0.5 truncate text-[11px] text-slate-400">
            {place.walkingDistanceMeters}m · {place.address}
          </p>
          <p className="truncate text-[11px] text-slate-400">{place.openHours ?? '운영시간 정보 없음'}</p>
        </div>
        {isInternal && place.urgencyScore !== null && (
          <div className="flex w-20 shrink-0 flex-col gap-1">
            <div className={`rounded-xl px-1 py-1.5 text-center ${tier.bg}`}>
              <div className={`font-mono-time text-sm font-bold leading-tight ${tier.text}`}>{place.urgencyScore}</div>
              <div className="text-[9px] font-medium leading-tight text-slate-400">급똥지수</div>
            </div>
            <button
              type="button"
              onClick={() => setShowReviewForm((v) => !v)}
              className="whitespace-nowrap rounded-xl bg-blue-600 px-1 py-2.5 text-center text-[10px] font-bold leading-tight text-white"
            >
              ✍️ 후기작성
            </button>
          </div>
        )}
      </div>

      <div className="relative min-h-0 flex-1">
        <div className="h-full overflow-y-auto px-3">
          {isInternal ? (
            <InternalReviewSection
              place={place}
              tags={tags}
              moodCounts={counts}
              recentComments={recentComments}
              showReviewForm={showReviewForm}
              onToggleReviewForm={() => setShowReviewForm((v) => !v)}
              submittingReview={submittingReview}
              onSubmitReview={onSubmitReview}
            />
          ) : (
            <ExternalPlaceInfoSection place={place} placeType={placeType} />
          )}
        </div>
        {/* 스크롤 가능한 콘텐츠가 더 있음을 알려주는 하단 페이드 힌트 */}
        <div className="pointer-events-none absolute inset-x-0 bottom-0 h-6 bg-gradient-to-t from-white to-transparent" />
      </div>

      <div className="flex shrink-0 gap-2 border-t border-slate-100 p-3">
        <a
          href={kakaoDirectionsUrl(place.name, place.latitude, place.longitude, origin ?? undefined)}
          target="_blank"
          rel="noreferrer"
          className="flex-1 rounded-lg bg-blue-600 py-2.5 text-center text-sm font-bold text-white"
        >
          🧭 길찾기
        </a>
        <button
          type="button"
          onClick={() => setFavorited((v) => !v)}
          aria-label="즐겨찾기"
          className={
            favorited
              ? 'shrink-0 rounded-lg border border-amber-300 bg-amber-50 px-3 text-lg text-amber-500'
              : 'shrink-0 rounded-lg border border-slate-200 px-3 text-lg text-slate-300'
          }
        >
          {favorited ? '★' : '☆'}
        </button>
      </div>
    </div>
  )
}

export default DetailCard
