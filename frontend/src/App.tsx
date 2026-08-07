import { useState } from 'react'
import './App.css'
import RestroomTab from './tabs/RestroomTab'
import PharmacyTab from './tabs/PharmacyTab'

type TabKey = 'restroom' | 'pharmacy'

function App() {
  const [tab, setTab] = useState<TabKey>('restroom')

  return (
    <div className="flex h-screen flex-col">
      <nav className="flex gap-1.5 bg-cham-pink p-1.5">
        <button
          type="button"
          onClick={() => setTab('restroom')}
          className={
            tab === 'restroom'
              ? 'flex-1 rounded-2xl bg-white py-3.5 text-lg font-extrabold tracking-tight text-cham-coral shadow-[0_2px_8px_rgba(245,168,181,0.55)] transition-colors'
              : 'flex-1 rounded-2xl py-3.5 text-lg font-bold tracking-tight text-cham-ink/40 transition-colors hover:bg-white/40 hover:text-cham-ink/60'
          }
        >
          🧻 참지마<span className="text-sm font-bold opacity-60">(급똥)</span>
        </button>
        <button
          type="button"
          onClick={() => setTab('pharmacy')}
          className={
            tab === 'pharmacy'
              ? 'flex-1 rounded-2xl bg-white py-3.5 text-lg font-extrabold tracking-tight text-cham-purple shadow-[0_2px_8px_rgba(245,168,181,0.55)] transition-colors'
              : 'flex-1 rounded-2xl py-3.5 text-lg font-bold tracking-tight text-cham-ink/40 transition-colors hover:bg-white/40 hover:text-cham-ink/60'
          }
        >
          💊 약국
        </button>
      </nav>

      <main className="relative flex-1">
        {tab === 'restroom' ? <RestroomTab /> : <PharmacyTab />}
      </main>
    </div>
  )
}

export default App
