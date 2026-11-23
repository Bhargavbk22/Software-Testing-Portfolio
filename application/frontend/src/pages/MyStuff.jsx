import { useState } from 'react';
import { Link } from 'react-router-dom';
import { api, fmtDate, useFetch } from '../api.js';
import { useToast } from '../toast.jsx';
import { Button, Confirm, Empty, ErrorState, Field, Modal, PageHeader, Pagination, SkeletonRows, StatusBadge, Table, Td, Textarea, Th } from '../components/ui.jsx';

export function MyReports() {
  const [tab, setTab] = useState('LOST');
  const [page, setPage] = useState(0);
  const { data, loading, error, reload } = useFetch(() => api.get(`/api/${tab === 'LOST' ? 'lost' : 'found'}-items`, { mine: true, page, size: 8 }), [tab, page]);
  return (
    <>
      <PageHeader title="My reports" subtitle="Track the status of everything you have reported." />
      <div className="mb-4 inline-flex rounded-xl bg-slate-200/70 p-1">{['LOST', 'FOUND'].map((t) => (
        <button key={t} onClick={() => { setTab(t); setPage(0); }} className={`rounded-lg px-4 py-1.5 text-sm font-semibold ${tab === t ? 'bg-white text-indigo-700 shadow-sm' : 'text-slate-600'}`}>{t === 'LOST' ? 'Lost' : 'Found'}</button>))}</div>
      <div className="card">
        {loading ? <SkeletonRows /> : error ? <ErrorState message={error} onRetry={reload} /> : data.content.length === 0 ? (
          <Empty title={`No ${tab.toLowerCase()} reports yet`} text="Your submitted reports will appear here with their live status." action={<Link to={`/report/${tab.toLowerCase()}`}><Button>Report a {tab.toLowerCase()} item</Button></Link>} />
        ) : (<><Table><thead><tr><Th>Item</Th><Th>Category</Th><Th>Location</Th><Th>Date</Th><Th>Status</Th></tr></thead>
          <tbody>{data.content.map((i) => <tr key={i.id} className="hover:bg-slate-50"><Td><Link className="font-medium text-indigo-700" to={`/items/${i.type}/${i.id}`}>{i.name}</Link></Td><Td>{i.categoryName}</Td><Td>{i.location}</Td><Td>{fmtDate(i.date)}</Td><Td><StatusBadge status={i.status} /></Td></tr>)}</tbody></Table>
          <Pagination data={data} onPage={setPage} /></>)}
      </div>
    </>
  );
}

export function MyClaims() {
  const toast = useToast();
  const [page, setPage] = useState(0);
  const { data, loading, error, reload } = useFetch(() => api.get('/api/claims/my-claims', { page, size: 8 }), [page]);
  const rec = useFetch(() => api.get('/api/recoveries/mine', { size: 50 }), []);
  const [cancel, setCancel] = useState(null);
  const [evidence, setEvidence] = useState(null);
  const [text, setText] = useState('');
  const doCancel = async () => { try { await api.put(`/api/claims/${cancel.id}/cancel`); toast.success('Claim cancelled'); setCancel(null); reload(); } catch (e) { toast.error(e.friendly); setCancel(null); } };
  const addEvidence = async () => { try { await api.post(`/api/claims/${evidence.id}/evidence`, { message: text }); toast.success('Evidence added'); setEvidence(null); setText(''); reload(); } catch (e) { toast.error(e.friendly); } };
  const code = (claimId) => rec.data?.content.find((r) => r.claimId === claimId && r.status === 'COMPLETED')?.confirmationCode;
  return (
    <>
      <PageHeader title="My claims" subtitle="Ownership claims you submitted for found items." />
      <div className="card">
        {loading ? <SkeletonRows /> : error ? <ErrorState message={error} onRetry={reload} /> : data.content.length === 0 ? (
          <Empty title="No claims yet" text="Found something that is yours? Open the item and choose “Claim this item”." action={<Link to="/search"><Button>Search found items</Button></Link>} />
        ) : (<>{data.content.map((c) => (
          <div key={c.id} className="flex flex-wrap items-start justify-between gap-3 border-b border-slate-100 p-5 last:border-0">
            <div className="max-w-xl"><Link to={`/items/FOUND/${c.foundItemId}`} className="font-semibold text-indigo-700">{c.foundItemName}</Link>
              <p className="mt-1 text-sm text-slate-500">Submitted {fmtDate(c.createdAt)}</p>
              {c.decisionReason && <p className="mt-2 rounded-lg bg-slate-50 p-2.5 text-sm text-slate-600"><b>Decision note:</b> {c.decisionReason}</p>}
              {code(c.id) && <p className="mt-2 text-sm font-semibold text-emerald-700">Recovery confirmation: {code(c.id)}</p>}</div>
            <div className="flex flex-col items-end gap-2"><StatusBadge status={c.status} />
              <div className="flex gap-2">{['PENDING', 'UNDER_REVIEW'].includes(c.status) && <><Button size="sm" variant="secondary" onClick={() => setEvidence(c)}>Add evidence</Button><Button size="sm" variant="secondary" onClick={() => setCancel(c)}>Cancel</Button></>}</div></div>
          </div>))}<Pagination data={data} onPage={setPage} /></>)}
      </div>
      {cancel && <Confirm danger title="Cancel this claim?" text="A cancelled claim is final. You can submit a new claim only if the item is still available." confirmLabel="Cancel claim" onConfirm={doCancel} onClose={() => setCancel(null)} />}
      {evidence && <Modal title="Add supporting evidence" onClose={() => setEvidence(null)}><Field label="Describe the evidence"><Textarea value={text} onChange={(e) => setText(e.target.value)} /></Field><div className="mt-4 flex justify-end gap-2"><Button variant="secondary" onClick={() => setEvidence(null)}>Cancel</Button><Button onClick={addEvidence}>Add evidence</Button></div></Modal>}
    </>
  );
}
