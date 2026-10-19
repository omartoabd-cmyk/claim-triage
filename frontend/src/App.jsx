import { useEffect, useMemo, useState } from 'react'
import './App.css'
import { fetchClaims } from './api'
import ClaimForm from './components/ClaimForm'
import ClaimList from './components/ClaimList'

const OPEN_STATUSES = ['SUBMITTED', 'IN_REVIEW']

function App() {
  const [claims, setClaims] = useState([])
  const [loading, setLoading] = useState(true)
  const [loadError, setLoadError] = useState('')

  useEffect(() => {
    let cancelled = false

    fetchClaims()
      .then((data) => {
        if (!cancelled) setClaims(data)
      })
      .catch((error) => {
        if (!cancelled) setLoadError(error.message)
      })
      .finally(() => {
        if (!cancelled) setLoading(false)
      })

    return () => {
      cancelled = true
    }
  }, [])

  // Replace one claim (after a status change or analysis) without refetching.
  const handleClaimChanged = (updated) => {
    setClaims((current) => current.map((claim) => (claim.id === updated.id ? updated : claim)))
  }

  const handleClaimCreated = (created) => {
    setLoadError('')
    setClaims((current) => [created, ...current])
  }

  const metrics = useMemo(
    () => ({
      total: claims.length,
      open: claims.filter((claim) => OPEN_STATUSES.includes(claim.status)).length,
      highRisk: claims.filter((claim) => claim.analysis?.riskLevel === 'High').length,
    }),
    [claims],
  )

  return (
    <div className="app-shell">
      <header className="topbar">
        <div>
          <p className="eyebrow">Insurance operations</p>
          <h1>Claim Triage Center</h1>
        </div>
        <span className={`status-pill ${loadError ? 'offline' : ''}`}>
          {loadError ? 'Offline' : 'Connected'}
        </span>
      </header>

      <section className="summary-grid" aria-label="Summary metrics">
        <div className="summary-card accent">
          <span className="summary-label">Total claims</span>
          <strong>{metrics.total}</strong>
          <small>All time</small>
        </div>
        <div className="summary-card">
          <span className="summary-label">Open</span>
          <strong>{metrics.open}</strong>
          <small>Awaiting a decision</small>
        </div>
        <div className="summary-card">
          <span className="summary-label">High risk</span>
          <strong>{metrics.highRisk}</strong>
          <small>Per AI analysis</small>
        </div>
      </section>

      <main className="workspace-grid">
        <ClaimForm onClaimCreated={handleClaimCreated} />
        <ClaimList
          claims={claims}
          loading={loading}
          error={loadError}
          onClaimChanged={handleClaimChanged}
        />
      </main>
    </div>
  )
}

export default App
