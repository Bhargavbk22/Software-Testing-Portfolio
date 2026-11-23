import axios from 'axios';
import { useCallback, useEffect, useState } from 'react';

const http = axios.create({ baseURL: import.meta.env.VITE_API_URL || '' });

http.interceptors.request.use((cfg) => {
  const token = localStorage.getItem('token');
  if (token) cfg.headers.Authorization = `Bearer ${token}`;
  return cfg;
});

// Normalises every failure into err.friendly (message) and err.fieldErrors (per-field validation messages).
http.interceptors.response.use(
  (r) => r,
  (err) => {
    if (!err.response) {
      err.friendly = 'Cannot reach the server. Check your connection and try again.';
      err.fieldErrors = {};
    } else {
      const d = err.response.data || {};
      err.friendly = d.message || 'Something went wrong. Please try again.';
      err.fieldErrors = d.errors || {};
      const isAuthCall = (err.config?.url || '').includes('/api/auth/');
      if (err.response.status === 401 && !isAuthCall) window.dispatchEvent(new CustomEvent('auth-expired', { detail: err.friendly }));
    }
    return Promise.reject(err);
  }
);

const unwrap = (p) => p.then((r) => r.data.data);
export const api = {
  get: (url, params) => unwrap(http.get(url, { params })),
  post: (url, body) => unwrap(http.post(url, body)),
  put: (url, body) => unwrap(http.put(url, body)),
  del: (url) => unwrap(http.delete(url)),
  upload: (url, file) => {
    const f = new FormData();
    f.append('file', file);
    return unwrap(http.post(url, f));
  },
};

/** Loads data with loading + error state; call reload() after mutations. */
export function useFetch(fn, deps = []) {
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const run = useCallback(() => {
    setLoading(true);
    setError(null);
    fn().then(setData).catch((e) => setError(e.friendly || 'Failed to load')).finally(() => setLoading(false));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, deps);
  useEffect(run, [run]);
  return { data, loading, error, reload: run };
}

export const fmtDate = (v) => (v ? new Date(v).toLocaleDateString(undefined, { day: 'numeric', month: 'short', year: 'numeric' }) : '—');
export const fmtDateTime = (v) => (v ? new Date(v).toLocaleString(undefined, { day: 'numeric', month: 'short', hour: '2-digit', minute: '2-digit' }) : '—');
export const label = (s) => (s ? s.replaceAll('_', ' ').toLowerCase().replace(/^\w/, (c) => c.toUpperCase()) : '');
