const BASE = '/api/requests';

async function request(url, options = {}) {
  const res = await fetch(url, {
    headers: { 'Content-Type': 'application/json' },
    ...options,
  });
  const data = await res.json().catch(() => null);
  if (!res.ok) {
    throw new Error(data?.error || `Request failed: ${res.status}`);
  }
  return data;
}

export const api = {
  getRequests(params = {}) {
    const query = new URLSearchParams();
    if (params.status)    query.set('status', params.status);
    if (params.priority)  query.set('priority', params.priority);
    if (params.requester) query.set('requester', params.requester);
    if (params.keyword)   query.set('keyword', params.keyword);
    const qs = query.toString();
    return request(`${BASE}${qs ? '?' + qs : ''}`);
  },

  getRequest(id) {
    return request(`${BASE}/${id}`);
  },

  createRequest(body) {
    return request(BASE, { method: 'POST', body: JSON.stringify(body) });
  },

  updateRequest(id, body) {
    return request(`${BASE}/${id}`, { method: 'PATCH', body: JSON.stringify(body) });
  },

  // FR-16: add a comment to a request
  addComment(id, body) {
    return request(`${BASE}/${id}/comments`, { method: 'POST', body: JSON.stringify(body) });
  },
};
