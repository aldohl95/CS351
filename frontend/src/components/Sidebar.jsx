// src/components/Sidebar.jsx
import React from 'react';

const NAV = [
  { key: 'list',   label: 'All Requests',  icon: '▤' },
  { key: 'create', label: 'New Request',   icon: '+' },
];

export function Sidebar({ page, onNav }) {
  return (
    <aside style={{
      width: 200,
      minHeight: '100vh',
      background: 'var(--surface)',
      borderRight: '1px solid var(--border)',
      display: 'flex',
      flexDirection: 'column',
      padding: '24px 0',
      flexShrink: 0,
    }}>
      {/* Logo */}
      <div style={{ padding: '0 20px 24px', borderBottom: '1px solid var(--border)' }}>
        <div style={{
          fontFamily: 'var(--font-mono)',
          fontSize: 13,
          fontWeight: 500,
          color: 'var(--accent)',
          letterSpacing: '.06em',
        }}>
          SERVICE
        </div>
        <div style={{
          fontFamily: 'var(--font-mono)',
          fontSize: 13,
          fontWeight: 500,
          color: 'var(--text-muted)',
          letterSpacing: '.06em',
        }}>
          TRACKER
        </div>
      </div>

      {/* Nav links */}
      <nav style={{ padding: '16px 12px', flex: 1 }}>
        {NAV.map(item => (
          <button
            key={item.key}
            onClick={() => onNav(item.key)}
            style={{
              display: 'flex',
              alignItems: 'center',
              gap: 10,
              width: '100%',
              background: page === item.key ? 'var(--accent-dim)' : 'transparent',
              color: page === item.key ? 'var(--accent)' : 'var(--text-muted)',
              borderRadius: 'var(--radius)',
              padding: '8px 10px',
              marginBottom: 4,
              textAlign: 'left',
              fontWeight: page === item.key ? 600 : 400,
              fontSize: 13,
            }}
          >
            <span style={{ fontFamily: 'var(--font-mono)', fontSize: 14 }}>{item.icon}</span>
            {item.label}
          </button>
        ))}
      </nav>

      {/* Footer */}
      <div style={{ padding: '16px 20px', borderTop: '1px solid var(--border)' }}>
        <div style={{ fontSize: 11, color: 'var(--text-muted)', fontFamily: 'var(--font-mono)' }}>
          v0.1.0
        </div>
      </div>
    </aside>
  );
}
