import type { UrgentPlace } from './api'

export type PlaceType = 'PUBLIC_TOILET' | 'CAFE' | 'CONVENIENCE_STORE' | 'OTHER'

export function resolvePlaceType(place: UrgentPlace): PlaceType {
  if (place.type === 'RESTROOM') return 'PUBLIC_TOILET'
  if (place.category === '카페') return 'CAFE'
  if (place.category === '편의점') return 'CONVENIENCE_STORE'
  return 'OTHER'
}

export function isInternallyManaged(placeType: PlaceType): boolean {
  return placeType === 'PUBLIC_TOILET'
}
