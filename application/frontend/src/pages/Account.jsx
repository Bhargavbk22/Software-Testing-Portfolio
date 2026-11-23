import { useState } from 'react';
import { Link } from 'react-router-dom';
import { BellOff } from 'lucide-react';
import { api, fmtDateTime, label, useFetch } from '../api.js';
import { useAuth } from '../auth.jsx';
import { useToast } from '../toast.jsx';
import { Button, Empty, ErrorState, Field, Input, PageHeader, Pagination, SkeletonRows } from '../components/ui.jsx';

export function Notifications() {
  const [page, setPage] = useState(0);
  const [unreadOnly, setUnreadOnly] = useState(false);
  const toast = useToast();
  const { data, loading, error, reload } = useFetch(() => api.get('/api/notifications', { page, size: 12, unreadOnly }), [page, unreadOnly]);
  const changed = () => window.dispatchEvent(new Event('notifications-changed'));
  const read = async (n) => { if (n.read) return; try { await api.put(`/api/notifications/${n.id}/read`); reload(); changed(); } catch (e) { toast.error(e.friendly); } };
  const readAll = async () => { try { await api.put('/api/notifications/read-all'); reload(); changed(); } catch (e) { toast.error(e.friendly); } };
  const link = (n) => (n.relatedType === 'CLAIM' ? '/my-claims' : n.relatedType === 'LOST' ? `/items/LOST/${n.relatedId}` : n.relatedType === 'FOUND' ? `/items/FOUND/${n.relatedId}` : null);
  return (
    <>
      <PageHeader title="Notifications" subtitle="Updates about your reports, matches and claims." action={<div className="flex gap-2"><Button variant="secondary" onClick={() => { setUnreadOnly(!unreadOnly); setPage(0); }}>{unreadOnly ? 'Show all' : 'Unread only'}</Button><Button variant="secondary" onClick={readAll}>Mark all as read</Button></div>} />
      <div className="card">
        {loading ? <SkeletonRows /> : error ? <ErrorState message={error} onRetry={reload} /> : data.content.length === 0 ? <Empty title="You are all caught up" text="New updates will appear here." /> : (<>
          {data.content.map((n) => { const to = link(n); return (
            <div key={n.id} onClick={() => read(n)} className={`flex cursor-pointer items-start gap-3 border-b border-slate-100 px-5 py-4 last:border-0 ${n.read ? '' : 'bg-indigo-50/50'}`}>
              <span className={`mt-1.5 h-2.5 w-2.5 shrink-0 rounded-full ${n.read ? 'bg-slate-300' : 'bg-indigo-600'}`} aria-label={n.read ? 'Read' : 'Unread'} />
              <div className="flex-1"><p className="text-sm text-slate-800">{n.message}</p><p className="mt-1 text-xs text-slate-500">{label(n.type)} · {fmtDateTime(n.createdAt)}</p></div>
              {to && <Link to={to} className="text-sm font-medium text-indigo-600">Open</Link>}
            </div>); })}<Pagination data={data} onPage={setPage} /></>)}
      </div>
    </>
  );
}

export function Profile() {
  const { user, updateUser } = useAuth();
  const toast = useToast();
  const [f, setF] = useState({ fullName: user.fullName, department: user.department || '', phone: user.phone || '' });
  const [err, setErr] = useState({});
  const [busy, setBusy] = useState(false);
  const save = async (e) => {
    e.preventDefault(); setBusy(true); setErr({});
    try { updateUser(await api.put('/api/users/profile', f)); toast.success('Profile updated'); } catch (ex) { setErr(ex.fieldErrors || {}); toast.error(ex.friendly); } finally { setBusy(false); }
  };
  const set = (k) => (e) => setF({ ...f, [k]: e.target.value });
  return (
    <>
      <PageHeader title="Profile" subtitle="Your contact details are used by the recovery desk when a match is found." />
      <form onSubmit={save} className="card max-w-xl space-y-4 p-6" noValidate>
        <div className="grid gap-4 sm:grid-cols-2">
          <Field label="Email"><Input value={user.email} disabled /></Field>
          <Field label="Student ID"><Input value={user.studentId} disabled /></Field>
        </div>
        <Field label="Full name" error={err.fullName}><Input value={f.fullName} error={err.fullName} onChange={set('fullName')} /></Field>
        <Field label="Department" error={err.department}><Input value={f.department} error={err.department} onChange={set('department')} /></Field>
        <Field label="Phone number" error={err.phone}><Input value={f.phone} error={err.phone} onChange={set('phone')} /></Field>
        <p className="text-xs text-slate-500">Roles: {user.roles.map((r) => label(r.replace('ROLE_', ''))).join(', ')}</p>
        <div className="flex justify-end"><Button type="submit" loading={busy}>Save changes</Button></div>
      </form>
    </>
  );
}
