import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { ImageOff, SearchX } from 'lucide-react';
import { api, fmtDate, useFetch } from '../api.js';
import { Button, Empty, ErrorState, Input, Pagination, Select, Skeleton, StatusBadge } from '../components/ui.jsx';
import { PageHeader } from '../components/ui.jsx';

const blank = { type: 'FOUND', q: '', categoryId: '', location: '', from: '', to: '', status: '', sort: 'recent' };

export default function Search() {
  const [f, setF] = useState(blank);
  const [applied, setApplied] = useState(blank);
  const [page, setPage] = useState(0);
  const cats = useFetch(() => api.get('/api/categories'), []);
  const res = useFetch(() => api.get('/api/items/search', { ...Object.fromEntries(Object.entries(applied).filter(([, v]) => v !== '')), page, size: 9 }), [applied, page]);
  useEffect(() => setPage(0), [applied]);
  const set = (k) => (e) => setF({ ...f, [k]: e.target.value });
  const statuses = f.type === 'FOUND' ? ['AVAILABLE', 'CLAIM_PENDING'] : ['REPORTED', 'UNDER_REVIEW', 'MATCHED'];
  return (
    <>
      <PageHeader title="Search items" subtitle="Browse verified found items or lost reports. Identifying details of found items stay private." />
      <form onSubmit={(e) => { e.preventDefault(); setApplied(f); }} className="card mb-6 grid gap-3 p-4 sm:grid-cols-2 lg:grid-cols-4">
        <Select value={f.type} onChange={(e) => setF({ ...f, type: e.target.value, status: '' })} aria-label="Item type"><option value="FOUND">Found items</option><option value="LOST">Lost reports</option></Select>
        <Input placeholder="Search by item name" value={f.q} onChange={set('q')} aria-label="Item name" />
        <Select value={f.categoryId} onChange={set('categoryId')} aria-label="Category"><option value="">All categories</option>{cats.data?.map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}</Select>
        <Input placeholder="Location" value={f.location} onChange={set('location')} aria-label="Location" />
        <Input type="date" value={f.from} onChange={set('from')} aria-label="From date" />
        <Input type="date" value={f.to} onChange={set('to')} aria-label="To date" />
        <Select value={f.status} onChange={set('status')} aria-label="Status"><option value="">Any status</option>{statuses.map((s) => <option key={s} value={s}>{s.replaceAll('_', ' ').toLowerCase()}</option>)}</Select>
        <div className="flex gap-2"><Select value={f.sort} onChange={set('sort')} aria-label="Sort"><option value="recent">Recently reported</option><option value="date">By date</option><option value="oldest">Oldest first</option></Select><Button type="submit">Search</Button></div>
      </form>
      {res.loading ? <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">{Array.from({ length: 6 }).map((_, i) => <Skeleton key={i} className="h-64" />)}</div>
        : res.error ? <ErrorState message={res.error} onRetry={res.reload} />
        : res.data.content.length === 0 ? <div className="card"><Empty title="No items match your search" text="Try removing a filter or searching a broader location. New items are listed as soon as they are verified." action={<Button variant="secondary" onClick={() => { setF(blank); setApplied(blank); }}><SearchX className="h-4 w-4" />Clear filters</Button>} /></div>
        : (<>
          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">{res.data.content.map((i) => (
            <Link key={i.id} to={`/items/${i.type}/${i.id}`} className="card overflow-hidden transition hover:border-indigo-300 hover:shadow-md">
              <div className="flex h-40 items-center justify-center bg-slate-100">{i.images?.[0] ? <img src={i.images[0]} alt={i.name} className="h-full w-full object-cover" /> : <ImageOff className="h-8 w-8 text-slate-300" />}</div>
              <div className="p-4"><div className="flex items-start justify-between gap-2"><h3 className="font-semibold text-slate-900">{i.name}</h3><StatusBadge status={i.status} /></div>
                <p className="mt-1 text-sm text-slate-500">{i.categoryName}{i.color ? ` · ${i.color}` : ''}</p><p className="mt-2 text-xs text-slate-500">{i.location} · {fmtDate(i.date)}</p></div>
            </Link>))}</div>
          <div className="card mt-4"><Pagination data={res.data} onPage={setPage} /></div></>)}
    </>
  );
}
