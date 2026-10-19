import { useState } from 'react'
import { createClaim } from '../api'

const emptyClaim = {
  claimNumber: '',
  policyNumber: '',
  claimType: '',
  incidentDate: '',
  claimAmount: '',
  description: '',
}

const MAX_DESCRIPTION = 2000

// yyyy-mm-dd in the user's local time; an incident can't be in the future.
const today = () => new Date().toLocaleDateString('en-CA')

function ClaimForm({ onClaimCreated }) {
  const [claim, setClaim] = useState(emptyClaim)
  const [status, setStatus] = useState({ type: '', text: '' })
  const [fieldErrors, setFieldErrors] = useState({})
  const [isSubmitting, setIsSubmitting] = useState(false)

  const handleChange = (event) => {
    const { name, value } = event.target

    setClaim((currentClaim) => ({ ...currentClaim, [name]: value }))
    setFieldErrors((current) => ({ ...current, [name]: undefined }))
  }

  const handleSubmit = async (event) => {
    event.preventDefault()
    setIsSubmitting(true)
    setStatus({ type: '', text: '' })
    setFieldErrors({})

    try {
      const created = await createClaim({
        ...claim,
        claimAmount: Number(claim.claimAmount),
      })

      setStatus({
        type: 'success',
        text: 'Claim submitted successfully and added to the review queue.',
      })
      setClaim(emptyClaim)
      onClaimCreated?.(created)
    } catch (error) {
      setFieldErrors(error.fieldErrors ?? {})
      setStatus({
        type: 'error',
        text:
          error.status === 400
            ? 'Please fix the highlighted fields.'
            : error.message || 'Something went wrong while submitting the claim.',
      })
    } finally {
      setIsSubmitting(false)
    }
  }

  const fieldError = (name) =>
    fieldErrors[name] ? <small className="field-error">{fieldErrors[name]}</small> : null

  return (
    <section className="panel">
      <div className="panel-header">
        <div>
          <p className="eyebrow">New submission</p>
          <h2>Claim intake</h2>
        </div>
      </div>

      <form className="claim-form" onSubmit={handleSubmit}>
        <div className="form-grid">
          <label className="field">
            <span>Claim number</span>
            <input
              type="text"
              name="claimNumber"
              value={claim.claimNumber}
              onChange={handleChange}
              placeholder="CLM-2048"
              maxLength={50}
              required
            />
            {fieldError('claimNumber')}
          </label>

          <label className="field">
            <span>Policy number</span>
            <input
              type="text"
              name="policyNumber"
              value={claim.policyNumber}
              onChange={handleChange}
              placeholder="POL-8801"
              maxLength={50}
              required
            />
            {fieldError('policyNumber')}
          </label>

          <label className="field">
            <span>Claim type</span>
            <select name="claimType" value={claim.claimType} onChange={handleChange} required>
              <option value="">Select type</option>
              <option value="Auto">Auto</option>
              <option value="Home">Home</option>
              <option value="Health">Health</option>
              <option value="Property">Property</option>
              <option value="Liability">Liability</option>
            </select>
            {fieldError('claimType')}
          </label>

          <label className="field">
            <span>Incident date</span>
            <input
              type="date"
              name="incidentDate"
              value={claim.incidentDate}
              onChange={handleChange}
              max={today()}
              required
            />
            {fieldError('incidentDate')}
          </label>

          <label className="field full-width">
            <span>Claim amount</span>
            <input
              type="number"
              name="claimAmount"
              value={claim.claimAmount}
              onChange={handleChange}
              min="0.01"
              step="0.01"
              placeholder="2500.00"
              required
            />
            {fieldError('claimAmount')}
          </label>

          <label className="field full-width">
            <span>
              Description{' '}
              <em className="char-count">
                {claim.description.length}/{MAX_DESCRIPTION}
              </em>
            </span>
            <textarea
              name="description"
              value={claim.description}
              onChange={handleChange}
              rows="5"
              maxLength={MAX_DESCRIPTION}
              placeholder="Briefly describe the incident, damages, and any supporting details..."
              required
            />
            {fieldError('description')}
          </label>
        </div>

        <button className="primary-button" type="submit" disabled={isSubmitting}>
          {isSubmitting ? 'Submitting...' : 'Submit claim'}
        </button>

        {status.text && <p className={`status-message ${status.type}`}>{status.text}</p>}
      </form>
    </section>
  )
}

export default ClaimForm
