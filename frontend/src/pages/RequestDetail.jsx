// src/pages/RequestDetail.jsx
import React, { useState, useEffect } from 'react';
import { api } from '../services/api';
import { StatusBadge, PriorityBadge } from '../components/Badge';

const STATUSES   = ['OPEN', 'IN_PROGRESS', 'RESOLVED', 'CLOSED'];
const PRIORITIES = ['LOW', 'MEDIUM', 'HIGH'];

function formatDate(iso) {
  if (!iso) return '—';
  return new Date(iso).toLocaleString('en-US', {
    month: 'short', day: 'numeric', year: 'numeric',
    hour: '2-digit', minute: '2-digit',
  });
}

function Section({ title, children }) {
  return (
    <div style={{ marginBottom: 24 }}>
      <div style={{
        fontSize: 11, fontWeight: 600, color: 'var(--text-muted)',
        textTransform: 'uppercase', letterSpacing: '.06em', marginBottom: 10,
      }}>
        {title}
      </div>
      {children}
    </div>
  );
}

function Card({ children, style = {} }) {
  return (
    <div style={{
      background: 'var(--surface)',
      border: '1px solid var(--border)',
      borderRadius: 8,
      padding: 16,
      ...style,
    }}>
      {children}
    </div>
  );
}

export function RequestDetail({ requestId, onBack }) {
  const [req,     setReq]     = useState(null);
  const [loading, setLoading] = useState(true);
  const [error,   setError]   = useState('');

  // Update panel state
  const [newStatus,   setNewStatus]   = useState('');
  const [newPriority, setNewPriority] = useState('');
  const [changedBy,   setChangedBy]   = useState('');
  const [updating,    setUpdating]    = useState(false);
  const [updateErr,   setUpdateErr]   = useState('');

  // FR-16: comment form state
  const [commentContent, setCommentContent] = useState('');
  const [commentAuthor,  setCommentAuthor]  = useState('');
  const [commenting,     setCommenting]     = useState(false);
  const [commentErr,     setCommentErr]     = useState('');

  const load = async () => {
    setLoading(true);
    setError('');
    try {
      const data = await api.getRequest(requestId);
      setReq(data);
      setNewStatus(data.status);
      setNewPriority(data.priority || '');
    } catch (e) {
      setError(e.message);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { load(); }, [requestId]);

  const saveUpdate = async () => {
    setUpdateErr('');
    if (!changedBy.trim()) { setUpdateErr('Please enter your username.'); return; }
    setUpdating(true);
    try {
      const body = { changedBy };
      if (newStatus   !== req.status)               body.status   = newStatus;
      if (newPriority !== (req.priority || ''))     body.priority = newPriority;
      if (!body.status && !body.priority) { setUpdateErr('No changes detected.'); setUpdating(false); return; }
      await api.updateRequest(requestId, body);
      await load();
    } catch (e) {
      setUpdateErr(e.message);
    } finally {
      setUpdating(false);
    }
  };

  // FR-16: submit a comment
  const submitComment = async () => {
    setCommentErr('');
    if (!commentContent.trim()) { setCommentErr('Comment cannot be blank.');    return; }
    if (!commentAuthor.trim())  { setCommentErr('Please enter your username.'); return; }
    setCommenting(true);
    try {
      await api.addComment(requestId, { content: commentContent, author: commentAuthor });
      setCommentContent('');
      await load();
    } catch (e) {
      setCommentErr(e.message);
    } finally {
      setCommenting(false);
    }
  };

  if (loading) return <div style={{ padding: 32, color: 'var(--text-muted)' }}>Loading…</div>;
  if (error)   return <div style={{ padding: 32, color: 'var(--red)' }}>Error: {error}</div>;
  if (!req)    return null;

  return (
    <div style={{ padding: 32, flex: 1, overflowY: 'auto' }}>

      {/* Back + header */}
      <button onClick={onBack} style={{ background: 'transparent', color: 'var(--text-muted)', border: '1px solid var(--border-lt)', marginBottom: 20 }}>
        ← Back
      </button>

      <div style={{ marginBottom: 24 }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 10, marginBottom: 6 }}>
          <StatusBadge status={req.status} />
          <PriorityBadge priority={req.priority} />
        </div>
        <h1 style={{ fontSize: 22, fontWeight: 600, letterSpacing: '-.02em', marginBottom: 4 }}>
          {req.title}
        </h1>
        <div style={{ fontSize: 12, color: 'var(--text-muted)', fontFamily: 'var(--font-mono)' }}>
          {req.requestId} · submitted by {req.createdBy} on {formatDate(req.createdAt)}
        </div>
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: '1fr 340px', gap: 20 }}>

        {/* ── Left column ── */}
        <div>

          <Section title="Description">
            <Card>
              <p style={{ fontSize: 14, lineHeight: 1.7, whiteSpace: 'pre-wrap' }}>
                {req.description}
              </p>
            </Card>
          </Section>

          {req.category && (
            <Section title="Category">
              <span style={{ fontFamily: 'var(--font-mono)', fontSize: 13, color: 'var(--text-muted)' }}>
                {req.category}
              </span>
            </Section>
          )}

          {/* FR-17: Comments list */}
          <Section title={`Comments (${req.comments?.length || 0})`}>
            {(!req.comments || req.comments.length === 0) ? (
              <p style={{ color: 'var(--text-muted)', fontSize: 13 }}>No comments yet.</p>
            ) : (
              <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
                {[...req.comments].reverse().map(c => (
                  <Card key={c.commentId} style={{ padding: '12px 16px' }}>
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 6 }}>
                      <span style={{ fontFamily: 'var(--font-mono)', fontSize: 12, color: 'var(--accent)', fontWeight: 600 }}>
                        {c.author}
                      </span>
                      <span style={{ fontSize: 11, color: 'var(--text-muted)' }}>
                        {formatDate(c.createdAt)}
                      </span>
                    </div>
                    <p style={{ fontSize: 13, lineHeight: 1.6, margin: 0, whiteSpace: 'pre-wrap' }}>
                      {c.content}
                    </p>
                  </Card>
                ))}
              </div>
            )}
          </Section>

          {/* FR-19: History */}
          <Section title={`History (${req.history?.length || 0})`}>
            {(!req.history || req.history.length === 0) ? (
              <p style={{ color: 'var(--text-muted)', fontSize: 13 }}>No history yet.</p>
            ) : (
              <div style={{ display: 'flex', flexDirection: 'column', gap: 1 }}>
                {[...req.history].reverse().map(h => (
                  <div key={h.entryId} style={{
                    display: 'flex',
                    alignItems: 'flex-start',
                    gap: 12,
                    padding: '10px 0',
                    borderBottom: '1px solid var(--border)',
                  }}>
                    <div style={{
                      width: 6, height: 6, borderRadius: '50%',
                      background: h.field === 'comment' ? 'var(--yellow)' : 'var(--accent)',
                      marginTop: 6, flexShrink: 0,
                    }} />
                    <div style={{ flex: 1 }}>
                      <span style={{ fontFamily: 'var(--font-mono)', fontSize: 12, color: 'var(--text-muted)' }}>
                        {h.changedBy}
                      </span>
                      {' '}
                      <span style={{ fontSize: 13 }}>
                        {h.field === 'comment' ? (
                          <>added a <strong>comment</strong></>
                        ) : (
                          <>
                            changed <strong>{h.field}</strong>
                            {h.oldValue && <> from <code style={{ fontFamily: 'var(--font-mono)', fontSize: 12, color: 'var(--yellow)' }}>{h.oldValue}</code></>}
                            {' '}to <code style={{ fontFamily: 'var(--font-mono)', fontSize: 12, color: 'var(--green)' }}>{h.newValue}</code>
                          </>
                        )}
                      </span>
                      <div style={{ fontSize: 11, color: 'var(--text-muted)', marginTop: 2 }}>
                        {formatDate(h.timestamp)}
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </Section>

        </div>

        {/* ── Right column ── */}
        <div>

          {/* FR-16: Add Comment form */}
          <Section title="Add Comment">
            <Card>
              <div style={{ marginBottom: 14 }}>
                <label>Comment</label>
                <textarea
                  rows={4}
                  placeholder="Write a note or update…"
                  value={commentContent}
                  onChange={e => setCommentContent(e.target.value)}
                  style={{
                    width: '100%', resize: 'vertical',
                    fontFamily: 'var(--font-body)', fontSize: 13,
                    background: 'var(--bg)', color: 'var(--text)',
                    border: '1px solid var(--border-lt)',
                    borderRadius: 'var(--radius)', padding: '8px 10px', outline: 'none',
                  }}
                />
              </div>
              <div style={{ marginBottom: 14 }}>
                <label>Your Username</label>
                <input
                  placeholder="e.g. jane.smith"
                  value={commentAuthor}
                  onChange={e => setCommentAuthor(e.target.value)}
                />
              </div>
              {commentErr && (
                <div style={{ color: 'var(--red)', fontSize: 12, marginBottom: 10 }}>
                  {commentErr}
                </div>
              )}
              <button
                onClick={submitComment}
                disabled={commenting}
                style={{ width: '100%', background: 'var(--green)', color: '#0f1117', fontWeight: 600 }}
              >
                {commenting ? 'Posting…' : 'Post Comment'}
              </button>
            </Card>
          </Section>

          {/* Update Request panel */}
          <Section title="Update Request">
            <Card>
              <div style={{ marginBottom: 14 }}>
                <label>Status</label>
                <select value={newStatus} onChange={e => setNewStatus(e.target.value)}>
                  {STATUSES.map(s => <option key={s}>{s}</option>)}
                </select>
              </div>
              <div style={{ marginBottom: 14 }}>
                <label>Priority</label>
                <select value={newPriority} onChange={e => setNewPriority(e.target.value)}>
                  <option value="">— none —</option>
                  {PRIORITIES.map(p => <option key={p}>{p}</option>)}
                </select>
              </div>
              <div style={{ marginBottom: 14 }}>
                <label>Your Username</label>
                <input
                  placeholder="e.g. admin"
                  value={changedBy}
                  onChange={e => setChangedBy(e.target.value)}
                />
              </div>
              {updateErr && (
                <div style={{ color: 'var(--red)', fontSize: 12, marginBottom: 10 }}>
                  {updateErr}
                </div>
              )}
              <button
                onClick={saveUpdate}
                disabled={updating}
                style={{ width: '100%', background: 'var(--accent)', color: '#fff', fontWeight: 600 }}
              >
                {updating ? 'Saving…' : 'Save Changes'}
              </button>
            </Card>
          </Section>

          {/* Meta */}
          <Section title="Details">
            <Card style={{ fontSize: 13 }}>
              {[
                ['Request ID', <span style={{ fontFamily: 'var(--font-mono)', fontSize: 11 }}>{req.requestId}</span>],
                ['Submitted by', req.createdBy],
                ['Created', formatDate(req.createdAt)],
                ['Updated', formatDate(req.updatedAt)],
              ].map(([k, v]) => (
                <div key={k} style={{ display: 'flex', justifyContent: 'space-between', padding: '6px 0', borderBottom: '1px solid var(--border)' }}>
                  <span style={{ color: 'var(--text-muted)' }}>{k}</span>
                  <span>{v}</span>
                </div>
              ))}
            </Card>
          </Section>

        </div>
      </div>
    </div>
  );
}
