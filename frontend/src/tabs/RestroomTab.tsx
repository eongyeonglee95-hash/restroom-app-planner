import { useEffect, useMemo, useRef, useState } from 'react'
import KakaoMap, { type KakaoMapHandle } from '../components/KakaoMap'
import DetailCard from '../components/DetailCard'
import {
  fetchUrgentPlaces,
  submitReview,
  formatWalkingTime,
  type UrgentPlace,
  type ReviewRequest,
} from '../lib/api'
import { TIER_CLASS, tierOf } from '../lib/tier'

type FilterKey = '전체' | '공공화장실' | '편의점' | '카페' | '주유소'
const FILTERS: FilterKey[] = ['전체', '공공화장실', '편의점', '카페', '주유소']
const FILTER_ICON: Partial<Record<FilterKey, string>> = {
  공공화장실: '🚻',
  편의점: '🏪',
  카페: '☕',
  주유소: '⛽',
}
const PAGE_SIZE = 6

function matchesFilter(place: UrgentPlace, filter: FilterKey): boolean {
  if (filter === '전체') return true
  if (filter === '공공화장실') return place.type === 'RESTROOM'
  return place.type === 'TIP' && place.category === filter
}

function RestroomTab() {
  const [markerPosition, setMarkerPosition] = useState<{ lat: number; lng: number } | null>(null)
  const [places, setPlaces] = useState<UrgentPlace[] | null>(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [usedFallbackLocation, setUsedFallbackLocation] = useState(false)
  const [filter, setFilter] = useState<FilterKey>('공공화장실')
  const [withKid, setWithKid] = useState(false)
  const [expanded, setExpanded] = useState(false)
  const [selectedId, setSelectedId] = useState<string | null>(null)
  const [detailExpanded, setDetailExpanded] = useState(false)
  const [submittingReview, setSubmittingReview] = useState(false)
  const [reviewRefreshTick, setReviewRefreshTick] = useState(0)
  const [addressInput, setAddressInput] = useState('위치 확인 중...')
  const [searchError, setSearchError] = useState<string | null>(null)
  const mapRef = useRef<KakaoMapHandle>(null)

  useEffect(() => {
    if (!markerPosition) return
    let cancelled = false
    setLoading(true)
    setError(null)

    fetchUrgentPlaces(markerPosition.lat, markerPosition.lng, true)
      .then((data) => {
        if (cancelled) return
        setPlaces(data)
      })
      .catch((e) => {
        if (cancelled) return
        setError(e instanceof Error ? e.message : '알 수 없는 오류가 발생했습니다.')
      })
      .finally(() => {
        if (!cancelled) setLoading(false)
      })

    return () => {
      cancelled = true
    }
  }, [markerPosition])

  const filtered = useMemo(() => {
    if (!places) return []
    return places.filter((p) => matchesFilter(p, filter) && (!withKid || p.hasDiaperTable))
  }, [places, filter, withKid])

  const visible = expanded ? filtered : filtered.slice(0, PAGE_SIZE)

  const rankedSpots = useMemo(
    () =>
      filtered.map((p, i) => ({
        id: p.id,
        rank: i + 1,
        name: p.name,
        lat: p.latitude,
        lng: p.longitude,
        tier: tierOf(p.walkingTimeSeconds),
        label: `${formatWalkingTime(p.walkingTimeSeconds)} · ${p.walkingDistanceMeters}m`,
      })),
    [filtered],
  )

  useEffect(() => {
    if (filtered.length === 0) {
      setSelectedId(null)
      return
    }
    if (!filtered.some((p) => p.id === selectedId)) {
      setSelectedId(filtered[0].id)
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [filtered])

  const selected = filtered.find((p) => p.id === selectedId) ?? filtered[0] ?? null
  const routePath = selected?.path ?? null

  useEffect(() => {
    if (!selected) return
    mapRef.current?.focusRoute()
    mapRef.current?.animateWalk()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [selected?.id])

  const handleReviewSubmit = async (restroomId: string, review: ReviewRequest) => {
    setSubmittingReview(true)
    try {
      await submitReview(restroomId, review)
      setReviewRefreshTick((v) => v + 1)
      if (markerPosition) {
        const data = await fetchUrgentPlaces(markerPosition.lat, markerPosition.lng, true)
        setPlaces(data)
      }
    } catch {
      setError('리뷰 등록에 실패했습니다.')
    } finally {
      setSubmittingReview(false)
    }
  }

  return (
    <div className="relative h-full overflow-hidden">
      <div className="absolute inset-0">
        <KakaoMap
          ref={mapRef}
          onPositionChange={(lat, lng) => setMarkerPosition({ lat, lng })}
          onAddressChange={setAddressInput}
          onSearchError={setSearchError}
          onLocationUnavailable={() => setUsedFallbackLocation(true)}
          routePath={routePath}
          rankedSpots={rankedSpots}
          selectedSpotId={selectedId}
          onSelectSpot={setSelectedId}
        />
      </div>

      {/* 상단 플로팅: 검색(왼쪽) + 그 아래 필터 옵션 */}
      <div className="pointer-events-none absolute inset-x-3 top-3 z-20 flex flex-col gap-2">
        <form
          className="pointer-events-auto flex w-full items-center gap-3 rounded-full bg-white/40 px-6 py-3 shadow-lg shadow-black/10 backdrop-blur-md transition-colors duration-200 hover:bg-white/95 focus-within:bg-white/95 lg:w-[390px]"
          onSubmit={(e) => {
            e.preventDefault()
            setSearchError(null)
            mapRef.current?.searchAndMoveTo(addressInput)
          }}
        >
          <span className="text-base">📍</span>
          <input
            type="text"
            value={addressInput}
            onChange={(e) => setAddressInput(e.target.value)}
            placeholder="지역명, 장소로 검색"
            className="min-w-0 flex-1 bg-transparent text-base text-slate-700 outline-none"
          />
          <button type="submit" className="shrink-0 text-base" aria-label="검색">
            🔍
          </button>
        </form>

        <div className="pointer-events-auto -mx-3 flex w-[calc(100%+1.5rem)] items-center gap-0.5 px-3 py-1 lg:mx-0 lg:w-[390px] lg:overflow-x-auto lg:px-0">
          {FILTERS.map((f) => (
            <button
              key={f}
              type="button"
              onClick={() => setFilter(f)}
              className={
                filter === f
                  ? 'flex-1 whitespace-nowrap rounded-full bg-blue-600 px-1 py-1 text-center text-[10px] font-semibold text-white shadow-md shadow-blue-600/30 lg:flex-none lg:px-2'
                  : 'flex-1 whitespace-nowrap rounded-full bg-white/40 px-1 py-1 text-center text-[10px] font-semibold text-slate-600 shadow-sm shadow-black/5 backdrop-blur-md lg:flex-none lg:px-2'
              }
            >
              {FILTER_ICON[f] ? `${FILTER_ICON[f]} ${f}` : f}
            </button>
          ))}
          <button
            type="button"
            onClick={() => setWithKid((v) => !v)}
            className={
              withKid
                ? 'flex flex-1 items-center justify-center gap-0.5 whitespace-nowrap rounded-full bg-emerald-500 px-1 py-1 text-[10px] font-semibold text-white shadow-md shadow-emerald-500/30 lg:flex-none lg:px-2'
                : 'flex flex-1 items-center justify-center gap-0.5 whitespace-nowrap rounded-full bg-white/40 px-1 py-1 text-[10px] font-semibold text-slate-600 shadow-sm shadow-black/5 backdrop-blur-md lg:flex-none lg:px-2'
            }
          >
            👶 아이와 함께
          </button>
        </div>

        {(error || searchError || usedFallbackLocation) && (
          <div className="pointer-events-auto rounded-xl bg-white/95 px-3 py-2 text-xs shadow-md backdrop-blur">
            {error && <p className="font-semibold text-red-500">{error}</p>}
            {searchError && <p className="font-medium text-red-500">{searchError}</p>}
            {usedFallbackLocation && !error && (
              <p className="text-slate-400">위치를 가져올 수 없어 서울시청 기준으로 표시합니다.</p>
            )}
          </div>
        )}
      </div>

      {/* 모바일: 리스트는 footer로 하단 고정, 상세카드는 그 위에 플로팅. 데스크톱: 리스트+상세카드 세로 스택 */}
      <div className="absolute inset-x-0 bottom-0 top-28 z-20 lg:flex lg:flex-col lg:inset-x-auto lg:bottom-3 lg:left-3 lg:top-28 lg:w-[390px]">
        {/* 리스트 패널: 모바일에서는 하단 고정 footer, 데스크톱에서는 기존처럼 상단 스택 */}
        <div
          className={`absolute inset-x-0 bottom-0 z-10 flex h-60 min-h-0 flex-col overflow-hidden rounded-t-3xl shadow-[0_-8px_30px_rgba(0,0,0,0.18)] backdrop-blur-md transition-colors duration-200 hover:bg-white/95 lg:static lg:inset-auto lg:z-auto lg:h-auto lg:max-h-[54rem] lg:rounded-3xl ${
            detailExpanded ? 'bg-white/15' : 'bg-white/40'
          }`}
        >
          <div className="flex shrink-0 justify-center pt-2 lg:hidden">
            <span className="h-1 w-10 rounded-full bg-slate-200" />
          </div>

          <div className="flex shrink-0 items-center justify-between px-4 pb-1 pt-2">
            <h2 className="text-xs font-semibold text-slate-400">
              가까운 화장실 {loading ? '검색 중...' : `· ${filtered.length}곳`}
            </h2>
          </div>

          <div className="min-h-0 max-h-[778px] flex-1 overflow-y-auto">
            <ul className="flex flex-col gap-1 px-2 py-2">
              {visible.map((place, i) => {
                const tier = TIER_CLASS[tierOf(place.walkingTimeSeconds)]
                const active = place.id === selectedId
                return (
                  <li key={place.id}>
                    <button
                      type="button"
                      onClick={() => {
                        setSelectedId(place.id)
                        setDetailExpanded(true)
                      }}
                      className={`flex w-full items-start gap-3 rounded-xl px-4 py-3 text-left transition-colors ${
                        active ? 'bg-blue-50' : 'hover:bg-slate-50'
                      }`}
                    >
                      <div
                        className={`font-mono-time flex h-7 w-7 shrink-0 items-center justify-center rounded-full text-xs font-bold ${
                          active ? 'bg-blue-600 text-white' : 'bg-slate-100 text-slate-500'
                        }`}
                      >
                        {i + 1}
                      </div>
                      <div className="min-w-0 flex-1">
                        <div className="flex items-center gap-1.5">
                          <span className="truncate text-sm font-semibold">{place.name}</span>
                          <span className="shrink-0 rounded bg-blue-50 px-1.5 py-0.5 text-[10px] font-semibold text-blue-600">
                            {place.category}
                          </span>
                        </div>
                        {place.type === 'RESTROOM' && (
                          <div className="mt-1 flex items-center gap-1 text-xs text-slate-600">
                            <span className="text-amber-400">★</span>
                            {place.averageRating !== null ? (
                              <>
                                <span className="font-medium">{place.averageRating.toFixed(1)}</span>
                                <span className="text-[11px] text-slate-400">({place.reviewCount})</span>
                              </>
                            ) : (
                              <span className="text-[11px] text-slate-400">리뷰 없음</span>
                            )}
                          </div>
                        )}
                      </div>
                      <div className="shrink-0 text-right">
                        <div className={`font-mono-time flex items-center justify-end gap-1 text-sm font-bold ${tier.text}`}>
                          <span className={`inline-block h-2 w-2 rounded-full ${tier.dot}`} />
                          {formatWalkingTime(place.walkingTimeSeconds)}
                        </div>
                        <div className="font-mono-time mt-0.5 text-[11px] text-slate-400">{place.walkingDistanceMeters}m</div>
                      </div>
                    </button>
                  </li>
                )
              })}
            </ul>
            {filtered.length > PAGE_SIZE && (
              <button
                type="button"
                onClick={() => setExpanded((v) => !v)}
                className="w-full border-t border-slate-100 py-2.5 text-xs font-semibold text-slate-500"
              >
                {expanded ? '접기' : '더보기'}
              </button>
            )}
          </div>
        </div>

        {/* 선택된 카드 상세: 모바일에서는 리스트(footer)와 동일한 위치/크기로 그 위에 덮어씌워 팝업처럼 표시, 데스크톱에서는 리스트 바로 밑에서 남은 공간을 채움 */}
        {selected && detailExpanded && (
          <div className="absolute inset-x-0 bottom-0 z-20 h-60 overflow-hidden rounded-t-3xl bg-white shadow-[0_-8px_30px_rgba(0,0,0,0.18)] lg:relative lg:inset-auto lg:z-auto lg:mt-2 lg:h-auto lg:min-h-[24rem] lg:flex-1 lg:w-max lg:max-w-[640px] lg:min-w-full lg:self-start lg:rounded-3xl lg:shadow-[0_8px_30px_rgba(0,0,0,0.18)]">
            <button
              type="button"
              onClick={() => setDetailExpanded(false)}
              aria-label="닫기"
              className="absolute right-3 top-3 z-10 flex h-7 w-7 items-center justify-center rounded-full bg-slate-100 text-sm text-slate-500 shadow-sm"
            >
              ✕
            </button>
            <div className="h-full p-1.5">
              <DetailCard
                place={selected}
                origin={markerPosition}
                onSubmitReview={handleReviewSubmit}
                submittingReview={submittingReview}
                refreshKey={reviewRefreshTick}
              />
            </div>
          </div>
        )}
      </div>
    </div>
  )
}

export default RestroomTab
