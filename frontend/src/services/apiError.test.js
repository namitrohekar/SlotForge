import { describe, expect, it } from 'vitest'
import { toApiError } from './apiError.js'

describe('toApiError', () => {
  it('reads a problem response', () => {
    const error = {
      response: {
        status: 400,
        data: {
          code: 'VALIDATION_FAILED',
          status: 400,
          detail: '1 field is invalid',
          fieldErrors: [{ field: 'capacity', message: 'must be greater than 0' }],
        },
      },
    }

    expect(toApiError(error)).toEqual({
      message: '1 field is invalid',
      code: 'VALIDATION_FAILED',
      status: 400,
      fieldErrors: [{ field: 'capacity', message: 'must be greater than 0' }],
    })
  })

  it('handles a response that is not a problem', () => {
    expect(toApiError({ response: { status: 502, data: '<html>' } })).toMatchObject({
      code: 'UNEXPECTED_RESPONSE',
      status: 502,
    })
  })

  it('handles an unreachable server', () => {
    expect(toApiError(new Error('Network Error'))).toMatchObject({ code: 'NETWORK_ERROR', status: null })
  })
})
