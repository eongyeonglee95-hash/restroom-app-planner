import { forwardRef, useEffect, useImperativeHandle, useRef, useState } from 'react'
import { loadKakaoMapSdk } from '../lib/loadKakaoMapSdk'
import { getCurrentPosition } from '../lib/geolocation'
import { SEOUL_CITY_HALL } from '../lib/constants'
import { TIER_COLOR, type Tier } from '../lib/tier'

export interface RankedSpot {
  id: string
  rank: number
  name: string
  lat: number
  lng: number
  tier: Tier
  label: string
}

export interface KakaoMapHandle {
  searchAndMoveTo: (query: string) => void
  focusRoute: () => void
  animateWalk: () => void
  locateMe: () => void
}

interface KakaoMapProps {
  onPositionChange?: (lat: number, lng: number) => void
  onAddressChange?: (address: string) => void
  onSearchError?: (message: string) => void
  onLocationUnavailable?: () => void
  routePath?: { lat: number; lng: number }[] | null
  rankedSpots?: RankedSpot[]
  selectedSpotId?: string | null
  onSelectSpot?: (id: string) => void
}

let walkerStylesInjected = false

function ensureWalkerStyles() {
  if (walkerStylesInjected) return
  walkerStylesInjected = true
  const style = document.createElement('style')
  style.textContent = `
    @keyframes chamjima-walker-glow {
      0%, 100% {
        box-shadow: 0 0 8px 2px rgba(255, 105, 180, 0.55), 0 0 16px 5px rgba(255, 215, 0, 0.35);
      }
      50% {
        box-shadow: 0 0 14px 5px rgba(255, 105, 180, 0.9), 0 0 26px 9px rgba(255, 215, 0, 0.6);
      }
    }
    .chamjima-walker {
      display: inline-flex;
      align-items: center;
      justify-content: center;
      font-size: 24px;
      padding: 4px 8px;
      border-radius: 999px;
      background: radial-gradient(circle, rgba(255,255,255,0.95), rgba(255,240,250,0.85));
      border: 2px solid rgba(255, 105, 180, 0.8);
      transform: translateY(-4px);
      animation: chamjima-walker-glow 1.1s ease-in-out infinite;
    }
  `
  document.head.appendChild(style)
}

let rankPinStylesInjected = false

function ensureRankPinStyles() {
  if (rankPinStylesInjected) return
  rankPinStylesInjected = true
  const style = document.createElement('style')
  style.textContent = `
    .chamjima-rank-pin {
      display: flex;
      align-items: center;
      justify-content: center;
      width: 28px;
      height: 28px;
      border-radius: 9999px;
      color: #fff;
      font-size: 12px;
      font-weight: 700;
      font-family: 'IBM Plex Mono', ui-monospace, monospace;
      border: 2px solid #fff;
      box-shadow: 0 2px 6px rgba(15, 23, 42, 0.25);
      cursor: pointer;
      transition: transform 150ms ease, box-shadow 150ms ease;
    }
    .chamjima-rank-pin[data-selected="true"] {
      transform: scale(1.25);
      border-width: 3px;
      box-shadow: 0 4px 12px rgba(15, 23, 42, 0.35);
    }
  `
  document.head.appendChild(style)
}

function createRankPinElement(rank: number, tier: Tier, selected: boolean, onClick: () => void) {
  ensureRankPinStyles()
  const el = document.createElement('div')
  el.className = 'chamjima-rank-pin'
  el.dataset.selected = String(selected)
  el.style.backgroundColor = TIER_COLOR[tier]
  el.textContent = String(rank)
  el.addEventListener('click', onClick)
  return el
}

const KakaoMap = forwardRef<KakaoMapHandle, KakaoMapProps>(function KakaoMap(
  {
    onPositionChange,
    onAddressChange,
    onSearchError,
    onLocationUnavailable,
    routePath,
    rankedSpots,
    selectedSpotId,
    onSelectSpot,
  },
  ref,
) {
  const containerRef = useRef<HTMLDivElement>(null)
  const [error, setError] = useState<string | null>(null)
  const mapRef = useRef<any>(null)
  const meMarkerRef = useRef<any>(null)
  const addressOverlayRef = useRef<any>(null)
  const infoOverlayRef = useRef<any>(null)
  const geocoderRef = useRef<any>(null)
  const placesRef = useRef<any>(null)
  const kakaoRef = useRef<any>(null)
  const polylineRef = useRef<any>(null)
  const spotMarkersRef = useRef<any[]>([])
  const relayoutCleanupRef = useRef<(() => void) | null>(null)
  const onPositionChangeRef = useRef(onPositionChange)
  onPositionChangeRef.current = onPositionChange
  const onSelectSpotRef = useRef(onSelectSpot)
  onSelectSpotRef.current = onSelectSpot
  const onAddressChangeRef = useRef(onAddressChange)
  onAddressChangeRef.current = onAddressChange
  const onSearchErrorRef = useRef(onSearchError)
  onSearchErrorRef.current = onSearchError
  const onLocationUnavailableRef = useRef(onLocationUnavailable)
  onLocationUnavailableRef.current = onLocationUnavailable
  const routePathRef = useRef(routePath)
  routePathRef.current = routePath
  const walkerOverlayRef = useRef<any>(null)
  const walkerAnimationIdRef = useRef<number | null>(null)

  const updateAddressLabel = (lat: number, lng: number) => {
    const kakao = kakaoRef.current
    const geocoder = geocoderRef.current
    if (!kakao || !geocoder) return

    geocoder.coord2Address(lng, lat, (result: any[], status: string) => {
      if (status !== kakao.maps.services.Status.OK || result.length === 0) return

      const address = result[0].road_address?.address_name ?? result[0].address?.address_name ?? ''
      onAddressChangeRef.current?.(address)
      const content = document.createElement('div')
      content.className = 'map-address-label'
      content.textContent = address

      if (addressOverlayRef.current) {
        addressOverlayRef.current.setContent(content)
        addressOverlayRef.current.setPosition(new kakao.maps.LatLng(lat, lng))
      } else {
        addressOverlayRef.current = new kakao.maps.CustomOverlay({
          map: mapRef.current,
          position: new kakao.maps.LatLng(lat, lng),
          content,
          yAnchor: 1.8,
        })
      }
    })
  }

  const moveToPosition = (lat: number, lng: number, notify = true) => {
    const kakao = kakaoRef.current
    const map = mapRef.current
    if (!kakao || !map) return

    const position = new kakao.maps.LatLng(lat, lng)
    map.setCenter(position)

    if (meMarkerRef.current) {
      meMarkerRef.current.setPosition(position)
    } else {
      const marker = new kakao.maps.Marker({ map, position, draggable: true })
      kakao.maps.event.addListener(marker, 'dragend', () => {
        const dragged = marker.getPosition()
        const draggedLat = dragged.getLat()
        const draggedLng = dragged.getLng()
        updateAddressLabel(draggedLat, draggedLng)
        onPositionChangeRef.current?.(draggedLat, draggedLng)
      })
      meMarkerRef.current = marker
    }

    updateAddressLabel(lat, lng)

    if (notify) {
      onPositionChangeRef.current?.(lat, lng)
    }
  }

  const fitBoundsToRoute = () => {
    const kakao = kakaoRef.current
    const map = mapRef.current
    const path = routePathRef.current
    if (!kakao || !map || !path || path.length === 0) return

    const bounds = new kakao.maps.LatLngBounds()
    path.forEach((point) => bounds.extend(new kakao.maps.LatLng(point.lat, point.lng)))
    map.setBounds(bounds)
  }

  const animateWalker = () => {
    const kakao = kakaoRef.current
    const map = mapRef.current
    const path = routePathRef.current
    if (!kakao || !map || !path || path.length < 2) return

    if (walkerAnimationIdRef.current !== null) {
      cancelAnimationFrame(walkerAnimationIdRef.current)
      walkerAnimationIdRef.current = null
    }

    const segLens: number[] = []
    let total = 0
    for (let i = 0; i < path.length - 1; i++) {
      const a = path[i]
      const b = path[i + 1]
      const dx = (b.lng - a.lng) * Math.cos((a.lat * Math.PI) / 180)
      const dy = b.lat - a.lat
      const len = Math.sqrt(dx * dx + dy * dy)
      segLens.push(len)
      total += len
    }
    if (total === 0) return

    ensureWalkerStyles()
    const content = document.createElement('div')
    content.className = 'chamjima-walker'
    content.textContent = '💩🧻'

    walkerOverlayRef.current?.setMap(null)
    walkerOverlayRef.current = new kakao.maps.CustomOverlay({
      map,
      position: new kakao.maps.LatLng(path[0].lat, path[0].lng),
      content,
      yAnchor: 0.8,
      zIndex: 50,
    })

    const DURATION_MS = 3000
    const startTime = performance.now()

    const step = (now: number) => {
      const elapsed = now - startTime
      const t = Math.min(elapsed / DURATION_MS, 1)
      const targetDist = t * total

      let acc = 0
      let segIndex = 0
      for (; segIndex < segLens.length; segIndex++) {
        if (acc + segLens[segIndex] >= targetDist || segIndex === segLens.length - 1) break
        acc += segLens[segIndex]
      }
      const segLen = segLens[segIndex] || 1
      const segT = segLen === 0 ? 0 : Math.min((targetDist - acc) / segLen, 1)
      const a = path[segIndex]
      const b = path[segIndex + 1] ?? a
      const lat = a.lat + (b.lat - a.lat) * segT
      const lng = a.lng + (b.lng - a.lng) * segT

      walkerOverlayRef.current?.setPosition(new kakao.maps.LatLng(lat, lng))

      if (t < 1) {
        walkerAnimationIdRef.current = requestAnimationFrame(step)
      } else {
        walkerAnimationIdRef.current = null
        setTimeout(() => {
          walkerOverlayRef.current?.setMap(null)
          walkerOverlayRef.current = null
        }, 1200)
      }
    }

    walkerAnimationIdRef.current = requestAnimationFrame(step)
  }

  useImperativeHandle(ref, () => ({
    searchAndMoveTo: (query: string) => {
      const kakao = kakaoRef.current
      const places = placesRef.current
      if (!kakao || !places || !query.trim()) return

      places.keywordSearch(query.trim(), (data: any[], status: string) => {
        if (status !== kakao.maps.services.Status.OK || data.length === 0) {
          onSearchErrorRef.current?.('검색 결과가 없습니다.')
          return
        }
        moveToPosition(parseFloat(data[0].y), parseFloat(data[0].x))
      })
    },
    focusRoute: () => {
      fitBoundsToRoute()
    },
    animateWalk: () => {
      animateWalker()
    },
    locateMe: () => {
      locateMe()
    },
  }))

  const locateMe = async () => {
    const position = await getCurrentPosition()
    if (position) {
      moveToPosition(position.coords.latitude, position.coords.longitude)
    }
  }

  useEffect(() => {
    let cancelled = false

    loadKakaoMapSdk()
      .then(async (kakao) => {
        if (cancelled || !containerRef.current) return
        kakaoRef.current = kakao
        geocoderRef.current = new kakao.maps.services.Geocoder()
        placesRef.current = new kakao.maps.services.Places()

        const map = new kakao.maps.Map(containerRef.current, {
          center: new kakao.maps.LatLng(SEOUL_CITY_HALL.lat, SEOUL_CITY_HALL.lng),
          level: 4,
        })
        map.addControl(new kakao.maps.ZoomControl(), kakao.maps.ControlPosition.RIGHT)
        mapRef.current = map

        const relayout = () => map.relayout()
        window.addEventListener('resize', relayout)

        const resizeObserver = new ResizeObserver(relayout)
        if (containerRef.current) resizeObserver.observe(containerRef.current)
        relayoutCleanupRef.current = () => {
          window.removeEventListener('resize', relayout)
          resizeObserver.disconnect()
        }

        const position = await getCurrentPosition()
        if (cancelled) return
        if (position === null) {
          onLocationUnavailableRef.current?.()
          moveToPosition(SEOUL_CITY_HALL.lat, SEOUL_CITY_HALL.lng)
          return
        }
        moveToPosition(position.coords.latitude, position.coords.longitude)
      })
      .catch((err: Error) => setError(err.message))

    return () => {
      cancelled = true
      relayoutCleanupRef.current?.()
      if (walkerAnimationIdRef.current !== null) {
        cancelAnimationFrame(walkerAnimationIdRef.current)
      }
    }
  }, [])

  useEffect(() => {
    const kakao = kakaoRef.current
    const map = mapRef.current
    if (!kakao || !map) return

    polylineRef.current?.setMap(null)

    if (!routePath || routePath.length === 0) {
      return
    }

    const linePath = routePath.map((point) => new kakao.maps.LatLng(point.lat, point.lng))

    polylineRef.current = new kakao.maps.Polyline({
      map,
      path: linePath,
      strokeWeight: 5,
      strokeColor: '#2563EB',
      strokeOpacity: 0.9,
      strokeStyle: 'solid',
    })

    fitBoundsToRoute()
  }, [routePath])

  useEffect(() => {
    const kakao = kakaoRef.current
    const map = mapRef.current
    if (!kakao || !map) return

    spotMarkersRef.current.forEach((overlay) => overlay.setMap(null))
    spotMarkersRef.current = (rankedSpots ?? []).map((spot) => {
      const selected = spot.id === selectedSpotId
      const content = createRankPinElement(spot.rank, spot.tier, selected, () => {
        onSelectSpotRef.current?.(spot.id)
      })
      return new kakao.maps.CustomOverlay({
        map,
        position: new kakao.maps.LatLng(spot.lat, spot.lng),
        content,
        xAnchor: 0.5,
        yAnchor: 0.5,
        zIndex: selected ? 50 : 1,
      })
    })
  }, [rankedSpots, selectedSpotId])

  useEffect(() => {
    const kakao = kakaoRef.current
    if (!kakao) return

    infoOverlayRef.current?.setMap(null)
    infoOverlayRef.current = null

    const spot = (rankedSpots ?? []).find((s) => s.id === selectedSpotId)
    if (!spot) return

    const content = document.createElement('div')
    content.className = 'map-info-bubble'
    content.textContent = spot.label

    infoOverlayRef.current = new kakao.maps.CustomOverlay({
      map: mapRef.current,
      position: new kakao.maps.LatLng(spot.lat, spot.lng),
      content,
      yAnchor: 2.2,
    })
  }, [selectedSpotId, rankedSpots])

  if (error) {
    return <div className="map-placeholder">지도를 불러오지 못했습니다: {error}</div>
  }

  return (
    <div className="kakao-map-wrap">
      <div ref={containerRef} className="kakao-map" />

      <button
        type="button"
        onClick={locateMe}
        aria-label="내 위치로 이동"
        className="absolute bottom-6 right-4 z-30 flex h-11 w-11 items-center justify-center rounded-full bg-white shadow-lg shadow-black/20 active:bg-slate-100"
      >
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
          <circle cx="12" cy="12" r="3" fill="#2563EB" />
          <circle cx="12" cy="12" r="7.5" stroke="#2563EB" strokeWidth="1.6" />
          <line x1="12" y1="1.5" x2="12" y2="4.5" stroke="#2563EB" strokeWidth="1.6" strokeLinecap="round" />
          <line x1="12" y1="19.5" x2="12" y2="22.5" stroke="#2563EB" strokeWidth="1.6" strokeLinecap="round" />
          <line x1="1.5" y1="12" x2="4.5" y2="12" stroke="#2563EB" strokeWidth="1.6" strokeLinecap="round" />
          <line x1="19.5" y1="12" x2="22.5" y2="12" stroke="#2563EB" strokeWidth="1.6" strokeLinecap="round" />
        </svg>
      </button>
    </div>
  )
})

export default KakaoMap
