const API_BASE = import.meta.env.VITE_API_BASE || '';

function withQuery(path, params = {}) {
  const query = new URLSearchParams();
  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== '') {
      query.set(key, String(value));
    }
  });
  const queryString = query.toString();
  return queryString ? `${path}?${queryString}` : path;
}

async function request(path, options = {}) {
  const { headers = {}, ...restOptions } = options;
  const response = await fetch(`${API_BASE}${path}`, {
    ...restOptions,
    headers: {
      'Content-Type': 'application/json',
      ...headers,
    },
  });
  const contentType = response.headers.get('content-type') || '';
  const body = contentType.includes('application/json')
    ? await response.json()
    : { success: false, message: await response.text() };
  if (!response.ok || !body.success) {
    throw new Error(body.message || `Request failed: ${response.status}`);
  }
  return body.data;
}

export const api = {
  health: () => request('/api/system/health'),
  summary: () => request('/api/dashboard/summary'),
  products: (params = {}) => request(withQuery('/api/products', params)),
  createProduct: (payload) => request('/api/products', { method: 'POST', body: JSON.stringify(payload) }),
  updateProduct: (id, payload) => request(`/api/products/${id}`, { method: 'PUT', body: JSON.stringify(payload) }),
  devices: (params = {}) => request(withQuery('/api/devices', params)),
  createDevice: (payload) => request('/api/devices', { method: 'POST', body: JSON.stringify(payload) }),
  updateDevice: (id, payload) => request(`/api/devices/${id}`, { method: 'PUT', body: JSON.stringify(payload) }),
  latest: (id) => request(`/api/devices/${id}/latest`),
  telemetry: (id, { metric = '', start = '', end = '', limit = 200 } = {}) => {
    const params = new URLSearchParams({ limit: String(limit) });
    if (metric) params.set('metric', metric);
    if (start) params.set('start', start);
    if (end) params.set('end', end);
    return request(`/api/devices/${id}/telemetry?${params.toString()}`);
  },
  sensors: (productId) => request(`/api/products/${productId}/sensors`),
  createSensor: (productId, payload) => request(`/api/products/${productId}/sensors`, { method: 'POST', body: JSON.stringify(payload) }),
  updateSensor: (id, payload) => request(`/api/sensor-definitions/${id}`, { method: 'PUT', body: JSON.stringify(payload) }),
  deleteSensor: (id) => request(`/api/sensor-definitions/${id}`, { method: 'DELETE' }),
  rules: (params = {}) => request(withQuery('/api/rules', params)),
  createRule: (payload) => request('/api/rules', { method: 'POST', body: JSON.stringify(payload) }),
  updateRule: (id, payload) => request(`/api/rules/${id}`, { method: 'PUT', body: JSON.stringify(payload) }),
  enableRule: (id) => request(`/api/rules/${id}/enable`, { method: 'POST' }),
  disableRule: (id) => request(`/api/rules/${id}/disable`, { method: 'POST' }),
  alarms: (params = {}) => request(withQuery('/api/alarms', params)),
  ackAlarm: (id) => request(`/api/alarms/${id}/ack`, { method: 'POST' }),
  ingest: (deviceKey, secret, payload) => request(`/api/ingest/http/${deviceKey}/telemetry`, {
    method: 'POST',
    headers: secret ? { 'X-Device-Secret': secret } : {},
    body: JSON.stringify(payload),
  }),
};
