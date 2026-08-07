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
  animateWalk: (variant?: WalkerVariant) => void
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

/**
 * 경로를 따라 걸어가는 참지마 마스코트 2인조.
 * 휴지는 >_< (= "참는 중")로 앞서 도망가고, 똥은 ^^ 로 신나서 쫓아간다.
 */
const WALKER_SVG = `
<svg viewBox="0 0 71 54" width="71" height="54" xmlns="http://www.w3.org/2000/svg">
  <!-- 바깥 g가 배치(translate/scale)를 맡고, 안쪽 g만 CSS로 움직인다.
       SVG transform 속성은 CSS transform에 통째로 덮어써지기 때문에 분리가 필요함. -->
  <g transform="translate(1 8) scale(0.86)">
    <ellipse class="cham-shadow-poop" cx="15" cy="48" rx="12" ry="2.4" fill="rgba(63,42,28,0.18)"/>
    <g class="cham-poop">
      <rect class="cham-leg-c" x="10" y="39" width="3.8" height="9" rx="1.9" fill="#8F5F3D"/>
      <rect class="cham-leg-d" x="17" y="39" width="3.8" height="9" rx="1.9" fill="#8F5F3D"/>
      <ellipse cx="15" cy="38" rx="14" ry="7" fill="#A9724E"/>
      <ellipse cx="15" cy="28.5" rx="10" ry="5.8" fill="#B5804F"/>
      <ellipse cx="15" cy="20.5" rx="6.2" ry="4.6" fill="#BF8B58"/>
      <path d="M15 16.5 q3.2 -3.4 0.4 -5.6" stroke="#BF8B58" stroke-width="3" fill="none" stroke-linecap="round"/>
      <path d="M9 35.5 q2.2 -2.8 4.4 0" stroke="#3F2A1C" stroke-width="1.9" fill="none" stroke-linecap="round"/>
      <path d="M16.6 35.5 q2.2 -2.8 4.4 0" stroke="#3F2A1C" stroke-width="1.9" fill="none" stroke-linecap="round"/>
      <path d="M11.8 39.6 q3.2 3.2 6.4 0" stroke="#3F2A1C" stroke-width="1.7" fill="none" stroke-linecap="round"/>
    </g>
  </g>
  <g transform="translate(30 0)">
    <ellipse class="cham-shadow-paper" cx="26" cy="50" rx="13" ry="2.5" fill="rgba(63,42,28,0.18)"/>
    <g class="cham-paper">
      <rect class="cham-leg-a" x="18" y="38" width="4.2" height="11" rx="2.1" fill="#fff" stroke="#CFE3F0" stroke-width="0.8"/>
      <rect class="cham-leg-b" x="27.5" y="38" width="4.2" height="11" rx="2.1" fill="#fff" stroke="#CFE3F0" stroke-width="0.8"/>
      <ellipse cx="12" cy="23.5" rx="5.5" ry="14.5" fill="#DCEEF8"/>
      <ellipse cx="12" cy="23.5" rx="2.2" ry="6" fill="#4A6FB5"/>
      <path d="M12 9 H36 A3.5 3.5 0 0 1 39.5 12.5 V38 H12 Z" fill="#fff"/>
      <line x1="15.5" y1="12.5" x2="37.5" y2="12.5" stroke="#B9D9EC" stroke-width="1.2" stroke-dasharray="2.2 2.2" stroke-linecap="round"/>
      <path d="M19 21 L23.2 24.2 L19 27.4" stroke="#1F2937" stroke-width="2.3" stroke-linecap="round" stroke-linejoin="round" fill="none"/>
      <path d="M33.5 21 L29.3 24.2 L33.5 27.4" stroke="#1F2937" stroke-width="2.3" stroke-linecap="round" stroke-linejoin="round" fill="none"/>
      <rect x="23" y="30.5" width="6.5" height="2.8" rx="1.4" fill="#EF6B6B"/>
    </g>
  </g>
</svg>`

/** 약국 탭용 마스코트. 캡슐 알약 캐릭터가 경로를 따라 뛰어간다. */
const PILL_SVG = `
<svg viewBox="0 0 36 54" width="36" height="54" xmlns="http://www.w3.org/2000/svg">
  <ellipse class="cham-shadow-pill" cx="17" cy="50" rx="11" ry="2.4" fill="rgba(63,42,28,0.18)"/>
  <g class="cham-pill">
    <rect class="cham-leg-a" x="12" y="39" width="3.8" height="10" rx="1.9" fill="#9B85E3"/>
    <rect class="cham-leg-b" x="19" y="39" width="3.8" height="10" rx="1.9" fill="#9B85E3"/>
    <path d="M4 23 V19 A13 13 0 0 1 30 19 V23 Z" fill="#6B4FD8"/>
    <path d="M30 23 V27 A13 13 0 0 1 4 27 V23 Z" fill="#fff" stroke="#E5DFF7" stroke-width="0.7"/>
    <line x1="4" y1="23" x2="30" y2="23" stroke="#573FBF" stroke-width="1"/>
    <path d="M9.5 12 A9 9 0 0 1 16 8.2" stroke="rgba(255,255,255,0.55)" stroke-width="2.4" stroke-linecap="round" fill="none"/>
    <circle cx="12.5" cy="30.5" r="1.9" fill="#3F3A44"/>
    <circle cx="21.5" cy="30.5" r="1.9" fill="#3F3A44"/>
    <path d="M13.8 34.5 q3.2 2.8 6.4 0" stroke="#3F3A44" stroke-width="1.7" fill="none" stroke-linecap="round"/>
  </g>
</svg>`

type WalkerVariant = 'restroom' | 'pharmacy'

function ensureWalkerStyles() {
  if (walkerStylesInjected) return
  walkerStylesInjected = true
  const style = document.createElement('style')
  style.textContent = `
    @keyframes cham-bob {
      0%, 100% { transform: translateY(0); }
      50% { transform: translateY(-2.5px); }
    }
    @keyframes cham-step-up {
      0%, 100% { transform: translateY(0); }
      50% { transform: translateY(-3.5px); }
    }
    @keyframes cham-step-down {
      0%, 100% { transform: translateY(-3.5px); }
      50% { transform: translateY(0); }
    }
    @keyframes cham-shadow-pulse {
      0%, 100% { transform: scaleX(1); opacity: 0.18; }
      50% { transform: scaleX(0.88); opacity: 0.12; }
    }
    /* 급똥 탭 캐릭터 뒤에 은은한 핑크 네온. 팔레트의 blush + coral 톤을 겹쳐서 씀. */
    @keyframes cham-neon {
      0%, 100% {
        filter: drop-shadow(0 0 3px rgba(245, 168, 181, 0.75))
                drop-shadow(0 0 8px rgba(239, 107, 107, 0.4))
                drop-shadow(0 2px 5px rgba(63, 42, 28, 0.28));
      }
      50% {
        filter: drop-shadow(0 0 6px rgba(245, 168, 181, 0.95))
                drop-shadow(0 0 15px rgba(239, 107, 107, 0.6))
                drop-shadow(0 2px 5px rgba(63, 42, 28, 0.28));
      }
    }
    .chamjima-walker {
      display: block;
      filter: drop-shadow(0 2px 5px rgba(63, 42, 28, 0.28));
    }
    .chamjima-walker-neon {
      animation: cham-neon 1.1s ease-in-out infinite;
    }
    /* 휴지와 똥이 완전히 같은 박자로 뛰면 기계적으로 보여서 살짝 어긋나게 둔다 */
    .cham-paper { animation: cham-bob 420ms ease-in-out infinite; }
    .cham-poop { animation: cham-bob 420ms ease-in-out infinite 90ms; }
    .cham-pill { animation: cham-bob 420ms ease-in-out infinite; }
    .cham-leg-a { animation: cham-step-up 420ms ease-in-out infinite; }
    .cham-leg-b { animation: cham-step-down 420ms ease-in-out infinite; }
    .cham-leg-c { animation: cham-step-up 420ms ease-in-out infinite 90ms; }
    .cham-leg-d { animation: cham-step-down 420ms ease-in-out infinite 90ms; }
    .cham-shadow-paper {
      transform-origin: 26px 50px;
      animation: cham-shadow-pulse 420ms ease-in-out infinite;
    }
    .cham-shadow-poop {
      transform-origin: 15px 48px;
      animation: cham-shadow-pulse 420ms ease-in-out infinite 90ms;
    }
    .cham-shadow-pill {
      transform-origin: 17px 50px;
      animation: cham-shadow-pulse 420ms ease-in-out infinite;
    }
    @media (prefers-reduced-motion: reduce) {
      .cham-paper, .cham-poop, .cham-pill,
      .cham-leg-a, .cham-leg-b, .cham-leg-c, .cham-leg-d,
      .cham-shadow-paper, .cham-shadow-poop, .cham-shadow-pill,
      .chamjima-walker-neon {
        animation: none;
      }
      /* 깜빡임은 빼되 네온 자체는 남긴다 */
      .chamjima-walker-neon {
        filter: drop-shadow(0 0 4px rgba(245, 168, 181, 0.85))
                drop-shadow(0 0 10px rgba(239, 107, 107, 0.5))
                drop-shadow(0 2px 5px rgba(63, 42, 28, 0.28));
      }
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

  const animateWalker = (variant: WalkerVariant = 'restroom') => {
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
    const isPharmacy = variant === 'pharmacy'
    content.className = isPharmacy ? 'chamjima-walker' : 'chamjima-walker chamjima-walker-neon'
    // 정적 마크업이라 사용자 입력이 섞이지 않음
    content.innerHTML = isPharmacy ? PILL_SVG : WALKER_SVG

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
    animateWalk: (variant?: WalkerVariant) => {
      animateWalker(variant)
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
