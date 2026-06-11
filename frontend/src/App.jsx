// src/App.jsx
import React, { useState } from 'react';
import { Sidebar }       from './components/Sidebar';
import { RequestList }   from './pages/RequestList';
import { CreateRequest } from './pages/CreateRequest';
import { RequestDetail } from './pages/RequestDetail';

export default function App() {
  const [page,      setPage]      = useState('list');   // 'list' | 'create' | 'detail'
  const [selectedId, setSelectedId] = useState(null);

  const goList = () => { setPage('list'); setSelectedId(null); };

  const navPage = page === 'detail' ? 'list' : page;

  return (
    <div style={{ display: 'flex', minHeight: '100vh' }}>
      <Sidebar page={navPage} onNav={p => { setPage(p); setSelectedId(null); }} />

      <main style={{ flex: 1, display: 'flex', flexDirection: 'column', overflowY: 'auto' }}>
        {page === 'list' && (
          <RequestList
            onSelect={id => { setSelectedId(id); setPage('detail'); }}
          />
        )}
        {page === 'create' && (
          <CreateRequest
            onCreated={() => { setPage('list'); }}
          />
        )}
        {page === 'detail' && selectedId && (
          <RequestDetail
            requestId={selectedId}
            onBack={goList}
          />
        )}
      </main>
    </div>
  );
}
