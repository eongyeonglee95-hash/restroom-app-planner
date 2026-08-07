import type { PharmacyPlace } from '../lib/api'
import { businessHoursOf, formatWalkingTime, kakaoDirectionsUrl, naverMapSearchUrl } from '../lib/api'

interface PharmacyDetailCardProps {
  place: PharmacyPlace
  origin: { lat: number; lng: number } | null
}

function PharmacyDetailCard({ place, origin }: PharmacyDetailCardProps) {
  const hours = businessHoursOf(place.todayHours)

  return (
    <div
      key={place.id}
      className="chamjima-detail-fade flex h-full flex-col overflow-hidden rounded-2xl border border-slate-100 bg-white shadow-sm"
    >
      <div className="flex shrink-0 gap-3.5 p-4">
        <div className="flex h-14 w-14 shrink-0 items-center justify-center rounded-2xl bg-cham-lilac">
          <span className="text-2xl">{place.type === 'NIGHT' ? '🌙' : '💊'}</span>
        </div>
        <div className="min-w-0 flex-1">
          <div className="font-mono-time inline-block rounded-xl bg-cham-lilac px-2.5 py-1 text-2xl font-extrabold leading-none text-cham-purple">
            {formatWalkingTime(place.walkingTimeSeconds)}
          </div>
          <div className="mt-2 flex items-center gap-1.5">
            <h3 className="truncate text-base font-bold tracking-tight text-cham-ink">{place.name}</h3>
            <span className="shrink-0 rounded bg-cham-lilac px-1.5 py-0.5 text-[10px] font-semibold text-cham-purple">
              {place.type === 'NIGHT' ? '공공심야' : '약국'}
            </span>
          </div>
          <p className="font-mono-time mt-1 truncate text-[11px] font-light text-cham-ink/45">
            {place.walkingDistanceMeters}m · {place.address}
          </p>
          {hours ? (
            <p className="mt-0.5 truncate font-mono-time tracking-tight">
              <span className="text-sm font-medium text-cham-ink/50">{hours.open}~</span>
              <span className="text-base font-extrabold text-cham-ink">{hours.close}</span>
              <span className="ml-1 font-sans text-xs font-semibold text-cham-ink/60">마감</span>
            </p>
          ) : (
            <p className="truncate text-[11px] font-light text-cham-ink/45">오늘 {place.todayHours}</p>
          )}
        </div>
        <div className="flex w-20 shrink-0 flex-col gap-1.5">
          <div className={`rounded-xl px-1 py-1.5 text-center ${place.openNow ? 'bg-emerald-50' : 'bg-slate-100'}`}>
            <div
              className={`text-[11px] font-bold leading-tight ${
                place.openNow ? 'text-emerald-600' : 'text-slate-400'
              }`}
            >
              {place.openNow ? '영업중' : '영업종료'}
            </div>
            <div className="text-[9px] font-light leading-tight text-cham-ink/40">현재 상태</div>
          </div>
          {place.phone && (
            <a
              href={`tel:${place.phone}`}
              className="whitespace-nowrap rounded-xl bg-cham-purple px-1 py-2.5 text-center text-[10px] font-bold leading-tight text-white"
            >
              📞 전화
            </a>
          )}
        </div>
      </div>

      <div className="relative min-h-0 flex-1">
        <div className="h-full overflow-y-auto px-4">
          <div className="pb-4">
            {place.type === 'NIGHT' && (
              <div className="w-fit rounded-lg bg-cham-lilac px-2.5 py-1.5 text-[11px] font-medium text-cham-purple">
                🌙 서울시 지정 공공심야약국 · 밤 10시~새벽 1시 운영
              </div>
            )}

            {place.phone && (
              <a
                href={`tel:${place.phone}`}
                className="mt-2.5 flex w-fit items-center gap-1 text-xs font-medium text-cham-ink/70"
              >
                📞 {place.phone}
              </a>
            )}

            {/* TODO: 네이버/카카오 별점·리뷰 수는 Place Detail API 연동 전까지 실제 수치를 표시하지
                않고 검색 링크로만 연결한다 (허위 평점 표기를 피하기 위함) */}
            <a
              href={naverMapSearchUrl(place.name)}
              target="_blank"
              rel="noreferrer"
              className="mt-2.5 block rounded-xl border border-slate-200 py-2.5 text-center text-[11px] font-semibold text-cham-ink/70"
            >
              💬 네이버에서 리뷰 보기
            </a>

            {/* cham-tangerine은 채도가 높아 작은 텍스트로 쓰면 대비가 부족해서, 배경 액센트로만
                쓰고 글자는 진한 앰버를 쓴다 */}
            <div className="mt-2.5 rounded-xl border-l-[3px] border-cham-tangerine bg-orange-50 px-3 py-2.5 text-[11px] font-light leading-relaxed text-amber-800">
              💡 영업시간은 약국이 등록한 정보라 실제와 다를 수 있어요. 방문 전 전화로 확인하시는 걸 추천해요.
            </div>
          </div>
        </div>
        {/* 스크롤 가능한 콘텐츠가 더 있음을 알려주는 하단 페이드 힌트 */}
        <div className="pointer-events-none absolute inset-x-0 bottom-0 h-6 bg-gradient-to-t from-white to-transparent" />
      </div>

      <div className="flex shrink-0 gap-2 border-t border-slate-100 p-4">
        <a
          href={kakaoDirectionsUrl(place.name, place.latitude, place.longitude, origin ?? undefined)}
          target="_blank"
          rel="noreferrer"
          className="flex-1 rounded-xl bg-cham-purple py-3 text-center text-sm font-bold tracking-tight text-white"
        >
          🧭 길찾기
        </a>
      </div>
    </div>
  )
}

export default PharmacyDetailCard
