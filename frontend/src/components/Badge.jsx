// src/components/Badge.jsx
import React from 'react';

const STATUS_COLORS = {
  OPEN:        { bg: '#1e3460', color: '#4f8ef7' },
  IN_PROGRESS: { bg: '#2d2a14', color: '#fbbf24' },
  RESOLVED:    { bg: '#14291e', color: '#34d399' },
  CLOSED:      { bg: '#252a38', color: '#7a8499' },
};

const PRIORITY_COLORS = {
  HIGH:   { bg: '#2d1414', color: '#f87171' },
  MEDIUM: { bg: '#2d2a14', color: '#fbbf24' },
  LOW:    { bg: '#14291e', color: '#34d399' },
};

export function StatusBadge({ status }) {
  const style = STATUS_COLORS[status] || { bg: '#252a38', color: '#7a8499' };
  return (
    <span style={{
      background: style.bg,
      color: style.color,
      borderRadius: 4,
      padding: '2px 8px',
      fontSize: 11,
      fontWeight: 600,
      fontFamily: 'var(--font-mono)',
      letterSpacing: '.04em',
      whiteSpace: 'nowrap',
    }}>
      {status?.replace('_', ' ')}
    </span>
  );
}

export function PriorityBadge({ priority }) {
  if (!priority) return <span style={{ color: 'var(--text-muted)', fontSize: 11 }}>—</span>;
  const style = PRIORITY_COLORS[priority] || { bg: '#252a38', color: '#7a8499' };
  return (
    <span style={{
      background: style.bg,
      color: style.color,
      borderRadius: 4,
      padding: '2px 8px',
      fontSize: 11,
      fontWeight: 600,
      fontFamily: 'var(--font-mono)',
      letterSpacing: '.04em',
    }}>
      {priority}
    </span>
  );
}
