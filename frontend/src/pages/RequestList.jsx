// src/pages/RequestList.jsx
import React, { useState, useEffect, useCallback } from 'react';
import { api } from '../services/api';
import { StatusBadge, PriorityBadge } from '../components/Badge';

const STATUSES   = ['', 'OPEN', 'IN_PROGRESS', 'RESOLVED', 'CLOSED'];
const PRIORITIES = ['', 'HIGH', 'MEDIUM', 'LOW'];

function formatDate(iso) {
  if (!iso) return '—';
  return new Date(iso).toLocaleString('en-US', {
    month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit',
  });
}

export function RequestList({ onSelect }) {
  const [requests, setRequests]   = useState([]);
  const [message,  setMessage]    = useState('');
  const [loading,  setLoading]    = useState(false);
  const [error,    setError]      = useState('');

  const [filters, setFilters] = useState({
    status: '', priority: '', requester: '', keyword: '',
  });

  const load = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const hasFilter = Object.values(filters).some(v => v.trim());
      const data = await api.getRequests(hasFilter ? filters : {});

      // API returns either a plain array (no filters) or a SearchResult object (with filters)
      if (Array.isArray(data)) {
        setRequests(data);
        setMessage('');
      } else {
        setRequests(data.requests || []);
        setMessage(data.message || '');
      }
    } catch (e) {
      setError(e.message);
    } finally {
      setLoading(false);
    }
  }, [filters]);

  useEffect(() => { load(); }, [load]);

  const setFilter = (key, value) =>
    setFilters(prev => ({ ...prev, [key]: value }));

  const clearFilters = () =>
    setFilters({ status: '', priority: '', requester: '', keyword: '' });

  return (
    <div style={{ padding: 32, flex: 1, overflowY: 'auto' }}>

      {/* Header */}
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 24 }}>
        <div>
          <h1 style={{ fontSize: 22, fontWeight: 600, letterSpacing: '-.02em' }}>All Requests</h1>
          <p style={{ color: 'var(--text-muted)', fontSize: 13, marginTop: 2 }}>
            {requests.length} request{requests.length !== 1 ? 's' : ''}
          </p>
        </div>
        <button onClick={load} style={{ background: 'var(--border-lt)', color: 'var(--text)' }}>
          ↻ Refresh
        </button>
      </div>

      {/* Filters — FR-11, FR-12, FR-13, FR-14 */}
      <div style={{
        background: 'var(--surface)',
        border: '1px solid var(--border)',
        borderRadius: 8,
        padding: 16,
        marginBottom: 20,
        display: 'grid',
        gridTemplateColumns: '1fr 1fr 1fr 1fr auto',
        gap: 12,
        alignItems: 'end',
      }}>
        <div>
          <label>Status</label>
          <select value={filters.status} onChange={e => setFilter('status', e.target.value)}>
            {STATUSES.map(s => <option key={s} value={s}>{s || 'All'}</option>)}
          </select>
        </div>
        <div>
          <label>Priority</label>
          <select value={filters.priority} onChange={e => setFilter('priority', e.target.value)}>
            {PRIORITIES.map(p => <option key={p} value={p}>{p || 'All'}</option>)}
          </select>
        </div>
        <div>
          <label>Requester</label>
          <input
            placeholder="e.g. jane.smith"
            value={filters.requester}
            onChange={e => setFilter('requester', e.target.value)}
          />
        </div>
        <div>
          <label>Search</label>
          <input
            placeholder="keyword in title or description"
            value={filters.keyword}
            onChange={e => setFilter('keyword', e.target.value)}
          />
        </div>
        <button onClick={clearFilters} style={{ background: 'transparent', color: 'var(--text-muted)', border: '1px solid var(--border-lt)' }}>
          Clear
        </button>
      </div>

      {/* States */}
      {error   && <p style={{ color: 'var(--red)', marginBottom: 16 }}>Error: {error}</p>}
      {loading && <p style={{ color: 'var(--text-muted)', marginBottom: 16 }}>Loading…</p>}

      {/* FR-15: empty result message */}
      {!loading && !error && requests.length === 0 && (
        <div style={{
          background: 'var(--surface)',
          border: '1px solid var(--border)',
          borderRadius: 8,
          padding: 40,
          textAlign: 'center',
          color: 'var(--text-muted)',
        }}>
          <div style={{ fontSize: 32, marginBottom: 8 }}>◎</div>
          <div style={{ fontWeight: 500, marginBottom: 4 }}>No requests found</div>
          <div style={{ fontSize: 12 }}>{message || 'Try adjusting your filters.'}</div>
        </div>
      )}

      {/* Table */}
      {!loading && requests.length > 0 && (
        <div style={{ background: 'var(--surface)', border: '1px solid var(--border)', borderRadius: 8, overflow: 'hidden' }}>
          <table style={{ width: '100%', borderCollapse: 'collapse' }}>
            <thead>
              <tr style={{ borderBottom: '1px solid var(--border)', background: 'var(--bg)' }}>
                {['Title', 'Status', 'Priority', 'Requester', 'Created'].map(h => (
                  <th key={h} style={{
                    padding: '10px 16px',
                    textAlign: 'left',
                    fontSize: 11,
                    fontWeight: 600,
                    color: 'var(--text-muted)',
                    textTransform: 'uppercase',
                    letterSpacing: '.06em',
                  }}>{h}</th>
                ))}
              </tr>
            </thead>
            <tbody>
              {requests.map((r, i) => (
                <tr
                  key={r.requestId}
                  onClick={() => onSelect(r.requestId)}
                  style={{
                    borderBottom: i < requests.length - 1 ? '1px solid var(--border)' : 'none',
                    cursor: 'pointer',
                    transition: 'background .1s',
                  }}
                  onMouseEnter={e => e.currentTarget.style.background = 'var(--border)'}
                  onMouseLeave={e => e.currentTarget.style.background = 'transparent'}
                >
                  <td style={{ padding: '12px 16px', fontWeight: 500, maxWidth: 280 }}>
                    <div style={{ overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                      {r.title}
                    </div>
                    {r.category && (
                      <div style={{ fontSize: 11, color: 'var(--text-muted)', marginTop: 2 }}>
                        {r.category}
                      </div>
                    )}
                  </td>
                  <td style={{ padding: '12px 16px' }}><StatusBadge status={r.status} /></td>
                  <td style={{ padding: '12px 16px' }}><PriorityBadge priority={r.priority} /></td>
                  <td style={{ padding: '12px 16px', color: 'var(--text-muted)', fontSize: 13, fontFamily: 'var(--font-mono)' }}>
                    {r.createdBy}
                  </td>
                  <td style={{ padding: '12px 16px', color: 'var(--text-muted)', fontSize: 12 }}>
                    {formatDate(r.createdAt)}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}
