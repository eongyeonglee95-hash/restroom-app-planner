function requestPosition(): Promise<GeolocationPosition | null> {
  return new Promise((resolve) => {
    navigator.geolocation.getCurrentPosition(
      (position) => resolve(position),
      () => resolve(null),
      { enableHighAccuracy: true, timeout: 6000 },
    )
  })
}

export async function getCurrentPosition(): Promise<GeolocationPosition | null> {
  if (!navigator.geolocation) {
    return null
  }

  const first = await requestPosition()
  if (first) {
    return first
  }

  // macOS CoreLocation은 첫 요청에서 kCLErrorLocationUnknown으로 실패했다가
  // 바로 재시도하면 성공하는 경우가 잦아 한 번 더 시도한다.
  return requestPosition()
}
