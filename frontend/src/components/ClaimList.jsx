import { useState } from 'react'
import { analyzeClaim, updateClaimStatus } from '../api'

const STATUS_LABELS = {
  SUBMITTED: 'Submitted',
  IN_REVIEW: 'In review',
  APPROVED: 'Approved',
  DENIED: 'Denied',
}

const formatDateTime = (iso) => (iso ? new Date(iso).toLocaleString() : '')

function ClaimList({ claims, loading, error, onClaimChanged }) {
  const [busy, setBusy] = useState({})
  const [errors, setErrors] = useState({})

  const run = async (id, action) => {
    setBusy((current) => ({ ...current, [id]: true }))
    setErrors((current) => ({ ...current, [id]: '' }))

    try {
      await action()
    } catch (err) {
      setErrors((current) => ({ ...current, [id]: err.message }))
    } finally {
      setBusy((current) => ({ ...current, [id]: false }))
    }
  }

  const handleAnalyze = (claim, refresh) =>
    run(claim.id, async () => {
      const analysis = await analyzeClaim(claim.id, { refresh })
      onClaimChanged({ ...claim, analysis })
    })

  const handleStatusChange = (claim, status) =>
    run(claim.id, async () => {
      const updated = await updateClaimStatus(claim.id, status)
      // Keep any analysis already in state; the server copy has it too.
      onClaimChanged({ ...claim, ...updated })
    })

  return (
    <section className="panel">
      <div className="panel-header">
        <div>
          <p className="eyebrow">Review board</p>
          <h2>Claims dashboard</h2>
        </div>
        <span className="chip">{claims.length} total</span>
      </div>

      {loading ? (
        <div className="empty-state">Loading claims...</div>
      ) : error ? (
        <div className="empty-state">
          <p className="status-message error">{error}</p>
        </div>
      ) : claims.length === 0 ? (
        <div className="empty-state">
          No claims have been submitted yet. Use the intake form to add the first one.
        </div>
      ) : (
        <div className="claim-list">
          {claims.map((claim) => {
            const analysis = claim.analysis
            const isBusy = Boolean(busy[claim.id])

            return (
              <article key={claim.id} className="claim-card">
                <div className="claim-card-top">
                  <div>
                    <span className="claim-tag">{claim.claimType || 'General'}</span>
                    <h3>{claim.claimNumber || 'Unnamed claim'}</h3>
                  </div>
                  <strong className="claim-amount">
                    ${Number(claim.claimAmount ?? 0).toLocaleString(undefined, {
                      minimumFractionDigits: 2,
                      maximumFractionDigits: 2,
                    })}
                  </strong>
                </div>

                <dl className="claim-meta">
                  <div>
                    <dt>Policy</dt>
                    <dd>{claim.policyNumber}</dd>
                  </div>
                  <div>
                    <dt>Date</dt>
                    <dd>{claim.incidentDate}</dd>
                  </div>
                  <div>
                    <dt>Status</dt>
                    <dd>
                      <select
                        className={`status-select status-${claim.status}`}
                        value={claim.status}
                        disabled={isBusy}
                        aria-label={`Status for ${claim.claimNumber}`}
                        onChange={(event) => handleStatusChange(claim, event.target.value)}
                      >
                        {Object.entries(STATUS_LABELS).map(([value, label]) => (
                          <option key={value} value={value}>
                            {label}
                          </option>
                        ))}
                      </select>
                    </dd>
                  </div>
                </dl>

                <p className="claim-description">
                  {claim.description || 'No description provided.'}
                </p>

                <div className="card-actions">
                  <button
                    type="button"
                    className="secondary-button"
                    onClick={() => handleAnalyze(claim, Boolean(analysis))}
                    disabled={isBusy}
                  >
                    {isBusy ? 'Working...' : analysis ? 'Re-run analysis' : 'Analyze claim'}
                  </button>
                </div>

                {errors[claim.id] && (
                  <p className="status-message error">{errors[claim.id]}</p>
                )}

                {analysis && (
                  <div className="analysis-panel">
                    <div className="analysis-header">
                      <h4>AI analysis</h4>
                      <span className={`risk-badge risk-${analysis.riskLevel.toLowerCase()}`}>
                        {analysis.riskLevel} risk
                      </span>
                    </div>

                    <p>
                      <strong>Summary:</strong> {analysis.summary}
                    </p>
                    <p>
                      <strong>Suggested category:</strong> {analysis.suggestedCategory}
                    </p>

                    <div className="analysis-columns">
                      <div>
                        <h5>Missing information</h5>
                        <ul>
                          {analysis.missingInformation.map((item, index) => (
                            <li key={`${claim.id}-missing-${index}`}>{item}</li>
                          ))}
                        </ul>
                      </div>

                      <div>
                        <h5>Recommended next steps</h5>
                        <ul>
                          {analysis.recommendedNextSteps.map((step, index) => (
                            <li key={`${claim.id}-step-${index}`}>{step}</li>
                          ))}
                        </ul>
                      </div>
                    </div>

                    <p className="analysis-footnote">
                      AI-generated suggestion for adjuster review, not a decision.
                      Analyzed {formatDateTime(analysis.analyzedAt)}.
                    </p>
                  </div>
                )}
              </article>
            )
          })}
        </div>
      )}
    </section>
  )
}

export default ClaimList
