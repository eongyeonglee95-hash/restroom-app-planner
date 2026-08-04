import { useState } from "react";
import {
  Bell, Menu, MapPin, Locate, Layers, Plus, Minus, Navigation,
  Star, Heart, PenLine, ChevronRight, ChevronDown, List, User,
  ShieldCheck, Baby, Clock, DoorOpen,
} from "lucide-react";

const TIER = {
  green: { ring: "#10B981", text: "text-emerald-600", bg: "bg-emerald-50", dot: "bg-emerald-500" },
  amber: { ring: "#F59E0B", text: "text-amber-600", bg: "bg-amber-50", dot: "bg-amber-500" },
  rose: { ring: "#F43F5E", text: "text-rose-600", bg: "bg-rose-50", dot: "bg-rose-500" },
};

const SPOTS = [
  {
    id: 1, rank: 1, name: "GS25 63빌딩점", category: "편의점", tier: "green",
    seconds: 78, distanceM: 120, score: 94, rating: 4.7, reviews: 128,
    hours: "24시간", tags: ["내부", "남/여 분리"], badge: "이용팁",
    badgeNote: "직원에게 말하면 바로 이용 가능해요",
    x: 30, y: 46,
    photo: "https://images.unsplash.com/photo-1604719312566-8912e9227c6a?w=400&q=60",
    facilities: ["깨끗해요", "냄새 없어요", "휴지 있어요", "핸드솝 있어요"],
    reviewsList: [
      { emoji: "😀", text: "깨끗하고 휴지 넉넉해요!", time: "2분 전" },
      { emoji: "😐", text: "사람 많지만 금방 빠져요.", time: "10분 전" },
    ],
  },
  {
    id: 2, rank: 2, name: "% 아라비카 여의도 63빌딩점", category: "카페", tier: "green",
    seconds: 78, distanceM: 150, score: 88, rating: 4.5, reviews: 96,
    hours: "07:30–22:00", tags: ["내부", "남/여 분리"], badge: null,
    x: 55, y: 60,
    photo: "https://images.unsplash.com/photo-1509042239860-f550ce710b93?w=400&q=60",
    facilities: ["깨끗해요", "휴지 있어요"],
    reviewsList: [{ emoji: "😀", text: "매장이 넓고 쾌적해요.", time: "18분 전" }],
  },
  {
    id: 3, rank: 3, name: "퍼센트 아라비카 여의도63빌딩점", category: "카페", tier: "amber",
    seconds: 78, distanceM: 160, score: 85, rating: 4.6, reviews: 87,
    hours: "07:30–22:00", tags: ["내부", "남/여 분리"], badge: null,
    x: 74, y: 33,
    photo: "https://images.unsplash.com/photo-1521017432531-fbd92d768814?w=400&q=60",
    facilities: ["깨끗해요"],
    reviewsList: [{ emoji: "😀", text: "줄 거의 안 서요.", time: "31분 전" }],
  },
  {
    id: 4, rank: 4, name: "이디야커피 라이프점", category: "카페", tier: "amber",
    seconds: 211, distanceM: 260, score: 76, rating: 4.3, reviews: 74,
    hours: "07:00–23:00", tags: ["내부", "남/여 분리"], badge: null,
    x: 82, y: 47,
    photo: "https://images.unsplash.com/photo-1447933601403-0c6688de566e?w=400&q=60",
    facilities: ["휴지 있어요"],
    reviewsList: [{ emoji: "😐", text: "평범하지만 무난해요.", time: "1시간 전" }],
  },
  {
    id: 5, rank: 5, name: "카페보스코", category: "카페", tier: "rose",
    seconds: 335, distanceM: 410, score: 68, rating: 4.2, reviews: 61,
    hours: "08:00–22:00", tags: ["내부", "남/여 분리"], badge: null,
    x: 90, y: 63,
    photo: "https://images.unsplash.com/photo-1495474472287-4d71bcdd2085?w=400&q=60",
    facilities: ["깨끗해요"],
    reviewsList: [{ emoji: "😀", text: "조용해서 좋아요.", time: "2시간 전" }],
  },
  {
    id: 6, rank: 6, name: "여의도성모병원(1층)", category: "공공화장실", tier: "rose",
    seconds: 508, distanceM: 650, score: 91, rating: 4.8, reviews: 203,
    hours: "24시간", tags: ["내부", "남/여 분리"], badge: "안심",
    badgeNote: "가족화장실 · 기저귀교환대 있음",
    x: 20, y: 74,
    photo: "https://images.unsplash.com/photo-1584622650111-993a426fbf0a?w=400&q=60",
    facilities: ["깨끗해요", "기저귀교환대", "가족화장실"],
    reviewsList: [{ emoji: "😀", text: "병원이라 매우 청결해요.", time: "40분 전" }],
  },
];

function fmtTime(sec) {
  const m = Math.floor(sec / 60);
  const s = sec % 60;
  return m > 0 ? `${m}분 ${s}초` : `${s}초`;
}

function TierBadge({ tier }) {
  const t = TIER[tier];
  return <span className={`inline-block h-2 w-2 rounded-full ${t.dot}`} />;
}

export default function ChamjimaMockup() {
  const [selected, setSelected] = useState(SPOTS[0]);
  const [filter, setFilter] = useState("전체");
  const [expanded, setExpanded] = useState(false);
  const [aiWithKid, setAiWithKid] = useState(false);

  const visible = expanded ? SPOTS : SPOTS.slice(0, 4);
  const filters = ["전체", "공공화장실", "편의점", "카페"];

  return (
    <div className="min-h-screen bg-slate-50 text-slate-900" style={{ fontFamily: "'Inter', ui-sans-serif, system-ui" }}>
      <style>{`
        @import url('https://fonts.googleapis.com/css2?family=Space+Grotesk:wght@500;600;700&family=Inter:wght@400;500;600;700&family=IBM+Plex+Mono:wght@500;600&display=swap');
        .font-display { font-family: 'Space Grotesk', ui-sans-serif, system-ui; }
        .font-mono-time { font-family: 'IBM Plex Mono', ui-monospace, monospace; font-variant-numeric: tabular-nums; }
        .text-2xs { font-size: 0.6875rem; line-height: 1rem; }
        .text-3xs { font-size: 0.625rem; line-height: 0.875rem; }

        .app-grid { display: grid; grid-template-columns: 1fr; gap: 1rem; }
        @media (min-width: 1024px) { .app-grid { grid-template-columns: 360px 1fr; } }

        .map-box { position: relative; height: 520px; }
        @media (min-width: 1024px) { .map-box { height: auto; min-height: 600px; } }

        .detail-grid { display: grid; grid-template-columns: 1fr; gap: 1rem; }
        @media (min-width: 768px) { .detail-grid { grid-template-columns: 200px 1fr; } }

        .list-scroll { max-height: 360px; overflow-y: auto; }
        @media (min-width: 1024px) { .list-scroll { max-height: none; } }

        .order-map { order: 1; }
        .order-list { order: 2; }
        @media (min-width: 1024px) {
          .order-map { order: 2; }
          .order-list { order: 1; }
        }

        .glow-blue { box-shadow: 0 10px 25px -5px rgba(37, 99, 235, 0.35); }
        .row-active { background-color: #eff6ff; }
      `}</style>

      {/* Top bar */}
      <header className="sticky top-0 z-30 bg-white border-b border-slate-200">
        <div className="max-w-6xl mx-auto px-4 md:px-6 py-3 flex items-center justify-between">
          <div className="flex items-center gap-2.5">
            <div className="h-9 w-9 rounded-xl bg-blue-600 flex items-center justify-center shrink-0">
              <DoorOpen className="h-5 w-5 text-white" strokeWidth={2.25} />
            </div>
            <div>
              <div className="font-display font-bold text-lg leading-tight tracking-tight">참지마</div>
              <div className="text-2xs text-slate-400 leading-tight -mt-0.5">참지 마세요, 초 단위로 찾아드려요</div>
            </div>
          </div>
          <div className="flex items-center gap-1">
            <button className="relative h-9 w-9 rounded-full hover:bg-slate-100 flex items-center justify-center">
              <Bell className="h-4 w-4 text-slate-600" />
              <span className="absolute top-2 right-2 h-1.5 w-1.5 rounded-full bg-rose-500" />
            </button>
            <button className="h-9 w-9 rounded-full hover:bg-slate-100 flex items-center justify-center">
              <Menu className="h-4 w-4 text-slate-600" />
            </button>
          </div>
        </div>

        {/* Location + filters */}
        <div className="max-w-6xl mx-auto px-4 md:px-6 pb-3 flex flex-col sm:flex-row sm:items-center gap-2.5">
          <div className="flex items-center gap-2 bg-slate-50 border border-slate-200 rounded-xl px-3 py-2 text-sm text-slate-600 flex-1 min-w-0">
            <MapPin className="h-4 w-4 text-blue-600 shrink-0" />
            <span className="truncate">서울특별시 영등포구 여의도동</span>
            <Locate className="h-3.5 w-3.5 text-slate-400 ml-auto shrink-0" />
          </div>
          <div className="flex items-center gap-1.5 overflow-x-auto">
            {filters.map((f) => (
              <button
                key={f}
                onClick={() => setFilter(f)}
                className={`shrink-0 px-3 py-2 rounded-lg text-xs font-semibold transition-colors ${
                  filter === f ? "bg-blue-600 text-white" : "bg-slate-50 text-slate-500 border border-slate-200 hover:bg-slate-100"
                }`}
              >
                {f}
              </button>
            ))}
            <button
              onClick={() => setAiWithKid((v) => !v)}
              className={`shrink-0 flex items-center gap-1 px-3 py-2 rounded-lg text-xs font-semibold border transition-colors ${
                aiWithKid ? "bg-emerald-50 border-emerald-300 text-emerald-700" : "bg-slate-50 border-slate-200 text-slate-500 hover:bg-slate-100"
              }`}
            >
              <Baby className="h-3.5 w-3.5" /> 아이와 함께
            </button>
          </div>
        </div>
      </header>

      {/* Main: list + map */}
      <main className="app-grid max-w-6xl mx-auto px-4 md:px-6 py-4">
        {/* List */}
        <section className="order-list bg-white border border-slate-200 rounded-2xl overflow-hidden">
          <div className="px-4 pt-4 pb-2 flex items-center justify-between">
            <h2 className="font-display font-semibold text-sm">가장 가까운 화장실</h2>
            <span className="text-2xs text-slate-400">{SPOTS.length}곳 · 500m 이내</span>
          </div>
          <ul className="list-scroll">
            {visible.map((s) => {
              const t = TIER[s.tier];
              const active = selected.id === s.id;
              return (
                <li key={s.id}>
                  <button
                    onClick={() => setSelected(s)}
                    className={`w-full text-left px-4 py-3 flex items-start gap-3 border-t border-slate-100 transition-colors ${
                      active ? "row-active" : "hover:bg-slate-50"
                    }`}
                  >
                    <div
                      className={`shrink-0 h-7 w-7 rounded-full flex items-center justify-center text-xs font-bold font-mono-time ${
                        active ? "bg-blue-600 text-white" : "bg-slate-100 text-slate-500"
                      }`}
                    >
                      {s.rank}
                    </div>
                    <div className="min-w-0 flex-1">
                      <div className="flex items-center gap-1.5">
                        <span className="font-semibold text-sm truncate">{s.name}</span>
                        <span className="shrink-0 text-3xs font-semibold text-blue-600 bg-blue-50 px-1.5 py-0.5 rounded">
                          {s.category}
                        </span>
                      </div>
                      <div className="mt-1 flex items-center gap-1 text-2xs text-slate-400">
                        <Clock className="h-3 w-3" />
                        <span>{s.hours}</span>
                        <span>·</span>
                        <span>{s.tags.join(" · ")}</span>
                      </div>
                      <div className="mt-1.5 flex items-center gap-2">
                        <Star className="h-3 w-3 text-amber-400 fill-amber-400" />
                        <span className="text-xs font-medium text-slate-600">{s.rating}</span>
                        <span className="text-2xs text-slate-400">({s.reviews})</span>
                        {s.badge && (
                          <span className="text-3xs font-semibold text-emerald-600 bg-emerald-50 px-1.5 py-0.5 rounded flex items-center gap-0.5">
                            <ShieldCheck className="h-2.5 w-2.5" /> {s.badge}
                          </span>
                        )}
                      </div>
                    </div>
                    <div className="shrink-0 text-right">
                      <div className={`font-mono-time font-bold text-sm ${t.text} flex items-center gap-1 justify-end`}>
                        <TierBadge tier={s.tier} />
                        {fmtTime(s.seconds)}
                      </div>
                      <div className="text-2xs text-slate-400 mt-0.5">{s.distanceM}m</div>
                    </div>
                  </button>
                </li>
              );
            })}
          </ul>
          <button
            onClick={() => setExpanded((v) => !v)}
            className="w-full py-3 border-t border-slate-100 text-xs font-semibold text-slate-500 flex items-center justify-center gap-1 hover:bg-slate-50"
          >
            {expanded ? "접기" : "더보기"}
            <ChevronDown className={`h-3.5 w-3.5 transition-transform ${expanded ? "rotate-180" : ""}`} />
          </button>
        </section>

        {/* Map (stylized mock) */}
        <section className="order-map map-box bg-white border border-slate-200 rounded-2xl overflow-hidden">
          <div
            className="absolute inset-0"
            style={{
              backgroundColor: "#EEF2F9",
              backgroundImage:
                "linear-gradient(#DCE3F0 1px, transparent 1px), linear-gradient(90deg, #DCE3F0 1px, transparent 1px)",
              backgroundSize: "44px 44px",
            }}
          />
          {/* river */}
          <div className="absolute left-0 right-0 bottom-0" style={{ height: 96, background: "linear-gradient(180deg, transparent, #BFE0F5)" }} />

          {/* park block */}
          <div className="absolute rounded-md" style={{ left: "6%", top: "10%", width: "22%", height: "34%", backgroundColor: "#D7ECD2" }} />
          <span className="absolute text-2xs font-medium" style={{ left: "8%", top: "12%", color: "#047857b3" }}>여의도공원</span>

          {/* building blocks */}
          <div className="absolute rounded-md bg-white shadow-sm border border-slate-200" style={{ left: "45%", top: "38%", width: "16%", height: "16%" }} />
          <span className="absolute text-2xs font-medium text-slate-400" style={{ left: "47%", top: "56%" }}>63빌딩</span>
          <div className="absolute rounded-md bg-white shadow-sm border border-slate-200" style={{ left: "64%", top: "18%", width: "13%", height: "12%" }} />
          <span className="absolute text-2xs font-medium text-slate-400" style={{ left: "65%", top: "31%" }}>여의도성당</span>
          <div className="absolute rounded-md bg-white shadow-sm border border-slate-200" style={{ left: "8%", top: "56%", width: "14%", height: "10%" }} />
          <span className="absolute text-2xs font-medium text-slate-400" style={{ left: "9%", top: "67%" }}>KBS 본관</span>

          {/* route line from current location to rank-1 spot */}
          <svg className="absolute inset-0 w-full h-full" preserveAspectRatio="none">
            <line
              x1="50%" y1="50%"
              x2={`${SPOTS[0].x}%`} y2={`${SPOTS[0].y}%`}
              stroke="#2563EB" strokeWidth="3" strokeDasharray="1 10" strokeLinecap="round"
            />
          </svg>

          {/* current location dot */}
          <div className="absolute" style={{ left: "50%", top: "50%", transform: "translate(-50%,-50%)" }}>
            <div className="h-4 w-4 rounded-full bg-blue-600 border-2 border-white shadow-md" />
            <div className="absolute inset-0 h-4 w-4 rounded-full bg-blue-400 animate-ping opacity-50" />
          </div>

          {/* pins */}
          {SPOTS.map((s) => {
            const t = TIER[s.tier];
            const active = selected.id === s.id;
            return (
              <button
                key={s.id}
                onClick={() => setSelected(s)}
                className="absolute flex flex-col items-center"
                style={{ left: `${s.x}%`, top: `${s.y}%`, transform: "translate(-50%, -100%)" }}
              >
                <div
                  className={`h-7 w-7 rounded-full flex items-center justify-center text-white text-xs font-bold font-mono-time shadow-md border-2 border-white ${
                    active ? "scale-110" : ""
                  }`}
                  style={{ backgroundColor: t.ring }}
                >
                  {s.rank}
                </div>
                <div style={{ width: 2, height: 8, backgroundColor: t.ring }} />
                {active && (
                  <div className="mt-1 bg-slate-900 text-white text-3xs font-mono-time px-2 py-1 rounded-md whitespace-nowrap shadow-lg">
                    {fmtTime(s.seconds)} · {s.distanceM}m
                  </div>
                )}
              </button>
            );
          })}

          {/* map controls */}
          <div className="absolute right-3 top-3 flex flex-col gap-2">
            <button className="h-8 w-8 rounded-lg bg-white shadow border border-slate-200 flex items-center justify-center">
              <Locate className="h-4 w-4 text-slate-600" />
            </button>
            <button className="h-8 w-8 rounded-lg bg-white shadow border border-slate-200 flex items-center justify-center">
              <Layers className="h-4 w-4 text-slate-600" />
            </button>
          </div>
          <div className="absolute right-3 bottom-20 lg:bottom-3 flex flex-col rounded-lg overflow-hidden shadow border border-slate-200">
            <button className="h-8 w-8 bg-white flex items-center justify-center border-b border-slate-200">
              <Plus className="h-4 w-4 text-slate-600" />
            </button>
            <button className="h-8 w-8 bg-white flex items-center justify-center">
              <Minus className="h-4 w-4 text-slate-600" />
            </button>
          </div>

          {/* floating CTA */}
          <button className="glow-blue absolute bottom-4 bg-blue-600 hover:bg-blue-700 text-white font-semibold text-sm px-5 py-3 rounded-full flex items-center gap-2" style={{ left: "50%", transform: "translateX(-50%)" }}>
            <Navigation className="h-4 w-4" /> 길찾기
          </button>
        </section>
      </main>

      {/* Detail card */}
      <section className="max-w-6xl mx-auto px-4 md:px-6 pb-6">
        <div className="bg-white border border-slate-200 rounded-2xl overflow-hidden">
          <div className="px-4 pt-4 flex items-center justify-between">
            <h3 className="font-display font-semibold text-sm">추천 화장실 상세</h3>
            <button className="text-xs font-medium text-blue-600 flex items-center">더보기 <ChevronRight className="h-3.5 w-3.5" /></button>
          </div>

          <div className="detail-grid p-4">
            <div className="relative rounded-xl overflow-hidden h-40 md:h-full">
              <img src={selected.photo} alt={selected.name} className="w-full h-full object-cover" />
              <span className="absolute top-2 left-2 bg-blue-600 text-white text-3xs font-bold px-2 py-0.5 rounded-full">
                {selected.rank}위
              </span>
            </div>

            <div className="min-w-0">
              <div className="flex items-start justify-between gap-3">
                <div>
                  <div className="flex items-center gap-2">
                    <h4 className="font-display font-bold text-base">{selected.name}</h4>
                    <span className="text-3xs font-semibold text-blue-600 bg-blue-50 px-1.5 py-0.5 rounded">{selected.category}</span>
                  </div>
                  <p className="text-xs text-slate-400 mt-1">{selected.hours} · {selected.tags.join(" · ")}</p>
                </div>
                <div className={`shrink-0 rounded-xl px-3 py-2 text-center ${TIER[selected.tier].bg}`}>
                  <div className={`font-mono-time font-bold text-lg ${TIER[selected.tier].text}`}>{selected.score}</div>
                  <div className="text-3xs text-slate-400 font-medium">급똥지수</div>
                </div>
              </div>

              <div className="mt-2 flex items-center gap-1.5">
                <Star className="h-3.5 w-3.5 text-amber-400 fill-amber-400" />
                <span className="text-sm font-semibold">{selected.rating}</span>
                <span className="text-xs text-slate-400">({selected.reviews}개 후기)</span>
              </div>

              {selected.badgeNote && (
                <div className="mt-2 flex items-center gap-1.5 text-xs text-emerald-700 bg-emerald-50 rounded-lg px-2.5 py-1.5" style={{ width: "fit-content" }}>
                  <ShieldCheck className="h-3.5 w-3.5 shrink-0" /> {selected.badgeNote}
                </div>
              )}

              <div className="mt-3 flex flex-wrap gap-1.5">
                {selected.facilities.map((f) => (
                  <span key={f} className="text-2xs font-medium text-slate-600 bg-slate-100 px-2 py-1 rounded-full">
                    {f}
                  </span>
                ))}
              </div>

              <div className="mt-3 space-y-1.5">
                {selected.reviewsList.map((r, i) => (
                  <div key={i} className="flex items-center gap-2 text-xs bg-slate-50 rounded-lg px-2.5 py-2">
                    <span>{r.emoji}</span>
                    <span className="text-slate-600 flex-1 truncate">{r.text}</span>
                    <span className="text-slate-300 shrink-0">{r.time}</span>
                  </div>
                ))}
              </div>
            </div>
          </div>

          <div className="px-4 pb-4 grid grid-cols-3 gap-2">
            <button className="flex items-center justify-center gap-1.5 border border-slate-200 rounded-xl py-2.5 text-sm font-semibold text-slate-600 hover:bg-slate-50">
              <Heart className="h-4 w-4" /> 즐겨찾기
            </button>
            <button className="flex items-center justify-center gap-1.5 border border-slate-200 rounded-xl py-2.5 text-sm font-semibold text-slate-600 hover:bg-slate-50">
              <PenLine className="h-4 w-4" /> 후기 작성
            </button>
            <button className="flex items-center justify-center gap-1.5 bg-blue-600 hover:bg-blue-700 rounded-xl py-2.5 text-sm font-semibold text-white">
              <Navigation className="h-4 w-4" /> 길찾기
            </button>
          </div>
        </div>
      </section>

      {/* Bottom nav */}
      <nav className="sticky bottom-0 bg-white border-t border-slate-200">
        <div className="max-w-6xl mx-auto px-4 md:px-6 py-2 grid grid-cols-5 text-2xs font-medium text-slate-400">
          <button className="flex flex-col items-center gap-1 text-blue-600">
            <MapPin className="h-5 w-5" /> 지도
          </button>
          <button className="flex flex-col items-center gap-1 hover:text-slate-600">
            <List className="h-5 w-5" /> 목록
          </button>
          <button className="flex flex-col items-center gap-1 hover:text-slate-600">
            <div className="glow-blue h-9 w-9 rounded-full bg-blue-600 text-white flex items-center justify-center" style={{ marginTop: -16 }}>
              <Locate className="h-4 w-4" />
            </div>
          </button>
          <button className="flex flex-col items-center gap-1 hover:text-slate-600">
            <Heart className="h-5 w-5" /> 즐겨찾기
          </button>
          <button className="flex flex-col items-center gap-1 hover:text-slate-600">
            <User className="h-5 w-5" /> 마이페이지
          </button>
        </div>
      </nav>
    </div>
  );
}
