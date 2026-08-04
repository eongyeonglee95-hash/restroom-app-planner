let sdkPromise: Promise<typeof window.kakao> | null = null

export function loadKakaoMapSdk(): Promise<typeof window.kakao> {
  if (window.kakao?.maps) {
    return Promise.resolve(window.kakao)
  }

  if (sdkPromise) {
    return sdkPromise
  }

  sdkPromise = new Promise((resolve, reject) => {
    const appkey = import.meta.env.VITE_KAKAO_MAP_KEY
    if (!appkey) {
      reject(new Error('VITE_KAKAO_MAP_KEY가 설정되지 않았습니다 (.env.local 확인)'))
      return
    }

    const script = document.createElement('script')
    script.src = `https://dapi.kakao.com/v2/maps/sdk.js?appkey=${appkey}&autoload=false&libraries=services`
    script.onload = () => {
      window.kakao.maps.load(() => resolve(window.kakao))
    }
    script.onerror = () => reject(new Error('카카오맵 SDK 로드 실패'))
    document.head.appendChild(script)
  })

  return sdkPromise
}
