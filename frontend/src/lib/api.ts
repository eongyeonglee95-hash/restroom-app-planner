const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080'

export type Mood = 'GOOD' | 'NORMAL' | 'BAD'

export interface UrgentPlace {
  id: string
  type: 'RESTROOM' | 'TIP'
  name: string
  category: string
  address: string
  latitude: number
  longitude: number
  openHours: string | null
  hasDiaperTable: boolean
  tip: string | null
  phone: string | null
  averageRating: number | null
  reviewCount: number
  urgencyScore: number | null
  walkingTimeSeconds: number
  walkingDistanceMeters: number
  path: { lat: number; lng: number }[]
}

export interface NearbyRestroom {
  id: number
  name: string
  category: string
  address: string
  latitude: number
  longitude: number
  openHours: string
  hasDiaperTable: boolean
  distanceMeters: number
  averageRating: number | null
  reviewCount: number
}

export interface TipPlace {
  name: string
  category: string
  address: string
  latitude: number
  longitude: number
  tip: string
  phone: string | null
}

export interface PharmacyPlace {
  id: string
  type: 'GENERAL' | 'NIGHT'
  name: string
  address: string
  phone: string | null
  latitude: number
  longitude: number
  distanceMeters: number
  openNow: boolean
  todayHours: string
  walkingTimeSeconds: number
  walkingDistanceMeters: number
  path: { lat: number; lng: number }[]
}

export interface ReviewRequest {
  mood: Mood
  hasTissue: boolean
  hasBidet: boolean
  noLine: boolean
  isFree: boolean
  isClean: boolean
  noPasscode: boolean
  comment: string | null
}

export interface ReviewResponse extends ReviewRequest {
  id: number
  createdAt: string
}

export async function fetchUrgentPlaces(lat: number, lng: number, includeTips: boolean): Promise<UrgentPlace[]> {
  const response = await fetch(`${API_BASE_URL}/api/restrooms/urgent?lat=${lat}&lng=${lng}&includeTips=${includeTips}`)
  if (!response.ok) {
    throw new Error(`서버 오류 (${response.status})`)
  }
  return response.json()
}

export async function fetchNearbyRestrooms(lat: number, lng: number, radiusMeters = 1000): Promise<NearbyRestroom[]> {
  const response = await fetch(`${API_BASE_URL}/api/restrooms/nearby?lat=${lat}&lng=${lng}&radiusMeters=${radiusMeters}`)
  if (!response.ok) {
    throw new Error(`서버 오류 (${response.status})`)
  }
  return response.json()
}

export async function fetchNearbyTips(lat: number, lng: number): Promise<TipPlace[]> {
  const response = await fetch(`${API_BASE_URL}/api/tips?lat=${lat}&lng=${lng}`)
  if (!response.ok) {
    throw new Error(`서버 오류 (${response.status})`)
  }
  return response.json()
}

export async function fetchNearbyPharmacies(
  lat: number,
  lng: number,
  layer: 'ALL' | 'GENERAL' | 'NIGHT' = 'ALL',
  openNowOnly = true,
  radiusMeters = 3000,
  limit = 30,
): Promise<PharmacyPlace[]> {
  const params = new URLSearchParams({
    lat: String(lat),
    lng: String(lng),
    radiusMeters: String(radiusMeters),
    limit: String(limit),
    layer,
    openNowOnly: String(openNowOnly),
  })
  const response = await fetch(`${API_BASE_URL}/api/pharmacies/nearby?${params}`)
  if (!response.ok) {
    throw new Error(`서버 오류 (${response.status})`)
  }
  return response.json()
}

export async function submitReview(restroomId: string, review: ReviewRequest): Promise<void> {
  const response = await fetch(`${API_BASE_URL}/api/restrooms/${restroomId}/reviews`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(review),
  })
  if (!response.ok) {
    throw new Error(`서버 오류 (${response.status})`)
  }
}

export async function fetchReviews(restroomId: string): Promise<ReviewResponse[]> {
  const response = await fetch(`${API_BASE_URL}/api/restrooms/${restroomId}/reviews`)
  if (!response.ok) {
    throw new Error(`서버 오류 (${response.status})`)
  }
  return response.json()
}

/**
 * 약국 영업시간 문자열("09:00~19:00")을 시작/마감으로 분리한다.
 * "오늘 휴무" / "오늘 미운영"처럼 시간이 없는 경우엔 null.
 */
export function businessHoursOf(todayHours: string): { open: string; close: string } | null {
  const matched = todayHours.match(/^\s*(\d{1,2}:\d{2})\s*~\s*(\d{1,2}:\d{2})/)
  return matched ? { open: matched[1], close: matched[2] } : null
}

export function formatWalkingTime(totalSeconds: number): string {
  const minutes = Math.floor(totalSeconds / 60)
  const seconds = totalSeconds % 60
  if (minutes === 0) {
    return `${seconds}초`
  }
  return `${minutes}분 ${seconds}초`
}

export function kakaoDirectionsUrl(
  name: string,
  lat: number,
  lng: number,
  origin?: { lat: number; lng: number },
): string {
  const to = `${encodeURIComponent(name)},${lat},${lng}`
  if (!origin) {
    return `https://map.kakao.com/link/to/${to}`
  }
  const from = `${encodeURIComponent('현재 위치')},${origin.lat},${origin.lng}`
  return `https://map.kakao.com/link/from/${from}/to/${to}`
}

// 카카오맵에서 해당 장소를 바로 보여주는 공개 딥링크 (길찾기가 아닌 장소 카드 뷰)
export function kakaoPlaceUrl(name: string, lat: number, lng: number): string {
  return `https://map.kakao.com/link/map/${encodeURIComponent(name)},${lat},${lng}`
}

// 네이버지도는 별도 API 연동이 없어, 이름 기반 검색 딥링크로 대체
export function naverMapSearchUrl(name: string): string {
  return `https://map.naver.com/p/search/${encodeURIComponent(name)}`
}
