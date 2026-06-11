// src/pages/CreateRequest.jsx
import React, { useState } from 'react';
import { api } from '../services/api';

const PRIORITIES = ['LOW', 'MEDIUM', 'HIGH'];
const CATEGORIES = ['General', 'Hardware', 'Software', 'Network', 'Facilities', 'Other'];

const field = { marginBottom: 18 };

export function CreateRequest({ onCreated }) {
  const [form, setForm] = useState({
    title: '', description: '', createdBy: '',
    priority: 'MEDIUM', category: 'General',
  });
  const [error,   setError]   = useState('');
  const [loading, setLoading] = useState(false);

  const set = (key, val) => setForm(prev => ({ ...prev, [key]: val }));

  const submit = async () => {
    setError('');
    setLoading(true);
    try {
      await api.createRequest(form);
      onCreated();
    } catch (e) {
      setError(e.message);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div style={{ padding: 32, flex: 1, overflowY: 'auto', maxWidth: 640 }}>

      <div style={{ marginBottom: 28 }}>
        <h1 style={{ fontSize: 22, fontWeight: 600, letterSpacing: '-.02em' }}>New Request</h1>
        <p style={{ color: 'var(--text-muted)', fontSize: 13, marginTop: 2 }}>
          Fill out the form below to submit a service request.
        </p>
      </div>

      <div style={{ background: 'var(--surface)', border: '1px solid var(--border)', borderRadius: 8, padding: 24 }}>

        {/* Title */}
        <div style={field}>
          <label>Title *</label>
          <input
            placeholder="Short summary of the issue"
            value={form.title}
            onChange={e => set('title', e.target.value)}
            maxLength={150}
          />
        </div>

        {/* Description */}
        <div style={field}>
          <label>Description *</label>
          <textarea
            rows={5}
            placeholder="Describe the issue in detail…"
            value={form.description}
            onChange={e => set('description', e.target.value)}
            style={{
              width: '100%',
              resize: 'vertical',
              fontFamily: 'var(--font-body)',
              fontSize: 13,
              background: 'var(--bg)',
              color: 'var(--text)',
              border: '1px solid var(--border-lt)',
              borderRadius: 'var(--radius)',
              padding: '8px 10px',
              outline: 'none',
            }}
          />
        </div>

        {/* Requester */}
        <div style={field}>
          <label>Your Username *</label>
          <input
            placeholder="e.g. jane.smith"
            value={form.createdBy}
            onChange={e => set('createdBy', e.target.value)}
          />
        </div>

        {/* Priority + Category */}
        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 16, ...field }}>
          <div>
            <label>Priority</label>
            <select value={form.priority} onChange={e => set('priority', e.target.value)}>
              {PRIORITIES.map(p => <option key={p}>{p}</option>)}
            </select>
          </div>
          <div>
            <label>Category</label>
            <select value={form.category} onChange={e => set('category', e.target.value)}>
              {CATEGORIES.map(c => <option key={c}>{c}</option>)}
            </select>
          </div>
        </div>

        {/* Error */}
        {error && (
          <div style={{
            background: '#2d1414',
            border: '1px solid #5a2020',
            borderRadius: 'var(--radius)',
            padding: '10px 14px',
            color: 'var(--red)',
            fontSize: 13,
            marginBottom: 16,
          }}>
            {error}
          </div>
        )}

        {/* Actions */}
        <div style={{ display: 'flex', gap: 10 }}>
          <button
            onClick={submit}
            disabled={loading}
            style={{ background: 'var(--accent)', color: '#fff', fontWeight: 600 }}
          >
            {loading ? 'Submitting…' : 'Submit Request'}
          </button>
        </div>
      </div>
    </div>
  );
}
