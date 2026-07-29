import { useState } from 'react'
import './App.css'
import RestroomTab from './tabs/RestroomTab'
import PharmacyTab from './tabs/PharmacyTab'

type TabKey = 'restroom' | 'pharmacy'

function App() {
  const [tab, setTab] = useState<TabKey>('restroom')

  return (
    <div className="app">
      <nav className="tabs">
        <button
          type="button"
          className={tab === 'restroom' ? 'tab active' : 'tab'}
          onClick={() => setTab('restroom')}
        >
          🚽 급똥
        </button>
        <button
          type="button"
          className={tab === 'pharmacy' ? 'tab active' : 'tab'}
          onClick={() => setTab('pharmacy')}
        >
          💊 약국
        </button>
      </nav>

      <main className="tab-content">
        {tab === 'restroom' ? <RestroomTab /> : <PharmacyTab />}
      </main>
    </div>
  )
}

export default App
