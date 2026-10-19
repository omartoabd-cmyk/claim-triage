// Single place that knows where the API lives and how errors look.
const API_BASE = import.meta.env.VITE_API_URL ?? ''

export class ApiError extends Error {
  constructor(message, { status, fieldErrors } = {}) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.fieldErrors = fieldErrors ?? {}
  }
}

async function request(path, options = {}) {
  let response
  try {
    response = await fetch(`${API_BASE}${path}`, {
      ...options,
      headers: { 'Content-Type': 'application/json', ...options.headers },
    })
  } catch {
    throw new ApiError('Cannot reach the server. Is the backend running?', { status: 0 })
  }

  if (!response.ok) {
    let body = null
    try {
      body = await response.json()
    } catch {
      // non-JSON error body; fall through to the generic message
    }
    throw new ApiError(body?.message ?? `Request failed (${response.status})`, {
      status: response.status,
      fieldErrors: body?.fieldErrors,
    })
  }

  return response.json()
}

export const fetchClaims = () => request('/api/claims')

export const createClaim = (claim) =>
  request('/api/claims', { method: 'POST', body: JSON.stringify(claim) })

export const updateClaimStatus = (id, status) =>
  request(`/api/claims/${id}/status`, { method: 'PATCH', body: JSON.stringify({ status }) })

export const analyzeClaim = (id, { refresh = false } = {}) =>
  request(`/api/claims/${id}/analyze${refresh ? '?refresh=true' : ''}`, { method: 'POST' })
