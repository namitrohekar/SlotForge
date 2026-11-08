// Turns any failed request into one shape the UI can show.
export function toApiError(error) {
  const response = error?.response
  const problem = response?.data

  if (problem?.code) {
    return {
      message: problem.detail ?? problem.title,
      code: problem.code,
      status: problem.status,
      fieldErrors: problem.fieldErrors ?? [],
    }
  }

  if (response) {
    return {
      message: 'The server sent an unexpected response.',
      code: 'UNEXPECTED_RESPONSE',
      status: response.status,
      fieldErrors: [],
    }
  }

  return {
    message: 'Could not reach the server. Check that it is running and try again.',
    code: 'NETWORK_ERROR',
    status: null,
    fieldErrors: [],
  }
}
