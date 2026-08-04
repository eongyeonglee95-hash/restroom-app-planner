import { useState } from 'react'
import './App.css'
import RestroomTab from './tabs/RestroomTab'
import PharmacyTab from './tabs/PharmacyTab'

type TabKey = 'restroom' | 'pharmacy'

function App() {
  const [tab, setTab] = useState<TabKey>('restroom')

  return (
    <div className="flex h-screen flex-col">
      <nav className="flex border-b border-slate-200">
        <button
          type="button"
          onClick={() => setTab('restroom')}
          className={
            tab === 'restroom'
              ? 'flex-1 border-b-2 border-slate-900 py-3.5 text-base font-semibold text-slate-900'
              : 'flex-1 py-3.5 text-base font-semibold text-slate-400'
          }
        >
          🚽 급똥
        </button>
        <button
          type="button"
          onClick={() => setTab('pharmacy')}
          className={
            tab === 'pharmacy'
              ? 'flex-1 border-b-2 border-slate-900 py-3.5 text-base font-semibold text-slate-900'
              : 'flex-1 py-3.5 text-base font-semibold text-slate-400'
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
