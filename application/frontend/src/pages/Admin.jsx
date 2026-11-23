import { useState } from 'react';
import { Link } from 'react-router-dom';
import { Bar, BarChart, CartesianGrid, Cell, Legend, Pie, PieChart, ResponsiveContainer, Tooltip, XAxis, YAxis, Line, LineChart } from 'recharts';
import { CheckCircle2, ClipboardList, PackagePlus, PackageSearch, PackageCheck, Users } from 'lucide-react';
import { api, fmtDate, fmtDateTime, label, useFetch } from '../api.js';
import { useToast } from '../toast.jsx';
import { Button, Confirm, Empty, ErrorState, Field, Input, Modal, PageHeader, Pagination, Select, Skeleton, SkeletonRows, Stat, StatusBadge, Table, Td, Textarea, Th } from '../components/ui.jsx';

const COLORS = ['#4f46e5', '#0ea5e9', '#10b981', '#f59e0b', '#ef4444', '#8b5cf6', '#14b8a6', '#64748b'];
const ChartCard = ({ title, children }) => <div className="card p-5"><h2 className="mb-4 font-semibold">{title}</h2><div className="h-64">{children}</div></div>;

export function AdminDashboard() {
  const { data: d, loading, error, reload } = useFetch(() => api.get('/api/admin/dashboard'), []);
  if (loading) return <div className="grid gap-4 md:grid-cols-3">{Array.from({ length: 6 }).map((_, i) => <Skeleton key={i} className="h-24" />)}</div>;
  if (error) return <ErrorState message={error} onRetry={reload} />;
  return (
    <>
      <PageHeader title="Admin dashboard" subtitle="System-wide activity at a glance." />
      <div className="mb-6 grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
        <Stat icon={Users} label="Registered users" value={d.totalUsers} /><Stat icon={PackageSearch} label="Lost reports" value={d.totalLost} tone="rose" /><Stat icon={PackagePlus} label="Found reports" value={d.totalFound} tone="emerald" />
        <Stat icon={ClipboardList} label="Claims in review" value={d.pendingClaims} tone="amber" /><Stat icon={CheckCircle2} label="Approved claims" value={d.approvedClaims} tone="blue" /><Stat icon={PackageCheck} label="Items recovered" value={d.recoveredItems} tone="teal" />
      </div>
      <div className="grid gap-4 lg:grid-cols-2">
        <ChartCard title="Lost vs found reports"><ResponsiveContainer><BarChart data={d.lostVsFound}><CartesianGrid strokeDasharray="3 3" vertical={false} /><XAxis dataKey="name" /><YAxis allowDecimals={false} /><Tooltip /><Bar dataKey="value" radius={[8, 8, 0, 0]}>{d.lostVsFound.map((_, i) => <Cell key={i} fill={COLORS[i * 2]} />)}</Bar></BarChart></ResponsiveContainer></ChartCard>
        <ChartCard title="Monthly reports"><ResponsiveContainer><LineChart data={d.monthlyReports}><CartesianGrid strokeDasharray="3 3" vertical={false} /><XAxis dataKey="month" /><YAxis allowDecimals={false} /><Tooltip /><Legend /><Line dataKey="lost" stroke="#ef4444" strokeWidth={2} /><Line dataKey="found" stroke="#10b981" strokeWidth={2} /></LineChart></ResponsiveContainer></ChartCard>
        <ChartCard title="Claims by status"><ResponsiveContainer><BarChart data={d.claimsByStatus.map((c) => ({ ...c, name: label(c.name) }))} layout="vertical"><CartesianGrid strokeDasharray="3 3" horizontal={false} /><XAxis type="number" allowDecimals={false} /><YAxis type="category" dataKey="name" width={130} /><Tooltip /><Bar dataKey="value" fill="#4f46e5" radius={[0, 8, 8, 0]} /></BarChart></ResponsiveContainer></ChartCard>
        <ChartCard title="Item categories"><ResponsiveContainer><PieChart><Pie data={d.categories} dataKey="value" nameKey="name" innerRadius={50} outerRadius={90} paddingAngle={2}>{d.categories.map((_, i) => <Cell key={i} fill={COLORS[i % COLORS.length]} />)}</Pie><Tooltip /><Legend /></PieChart></ResponsiveContainer></ChartCard>
      </div>
    </>
  );
}

function useList(url, params) {
  const [page, setPage] = useState(0);
  const res = useFetch(() => api.get(url, { ...params, page, size: 10 }), [url, JSON.stringify(params), page]);
  return { ...res, page, setPage };
}

export function AdminUsers() {
  const toast = useToast();
  const [q, setQ] = useState(''); const [role, setRole] = useState('');
  const [applied, setApplied] = useState({});
  const { data, loading, error, reload, setPage } = useList('/api/admin/users', applied);
  const [target, setTarget] = useState(null);
  const toggle = async () => { try { await api.put(`/api/admin/users/${target.id}/status`, { enabled: !target.enabled }); toast.success('User updated'); reload(); } catch (e) { toast.error(e.friendly); } setTarget(null); };
  return (
    <>
      <PageHeader title="Users" subtitle="Search, filter and disable accounts." />
      <form onSubmit={(e) => { e.preventDefault(); setApplied({ q, role }); setPage(0); }} className="card mb-4 flex flex-wrap gap-3 p-4">
        <div className="min-w-[200px] flex-1"><Input placeholder="Search by name, email or student ID" value={q} onChange={(e) => setQ(e.target.value)} aria-label="Search users" /></div>
        <Select value={role} onChange={(e) => setRole(e.target.value)} aria-label="Role" style={{ maxWidth: 200 }}><option value="">All roles</option><option value="USER">Student</option><option value="MODERATOR">Moderator</option><option value="ADMIN">Administrator</option></Select><Button type="submit">Filter</Button></form>
      <div className="card">{loading ? <SkeletonRows /> : error ? <ErrorState message={error} onRetry={reload} /> : data.content.length === 0 ? <Empty title="No users found" text="Try a different search." /> : (<>
        <Table><thead><tr><Th>Name</Th><Th>Email</Th><Th>Student ID</Th><Th>Roles</Th><Th>Joined</Th><Th>Status</Th><Th> </Th></tr></thead><tbody>{data.content.map((u) => (
          <tr key={u.id}><Td className="font-medium">{u.fullName}</Td><Td>{u.email}</Td><Td>{u.studentId}</Td><Td>{u.roles.map((r) => label(r.replace('ROLE_', ''))).join(', ')}</Td><Td>{fmtDate(u.createdAt)}</Td>
            <Td><span className={`rounded-full px-2.5 py-1 text-xs font-semibold ${u.enabled ? 'bg-emerald-100 text-emerald-800' : 'bg-red-100 text-red-700'}`}>{u.enabled ? 'Active' : 'Disabled'}</span></Td>
            <Td><Button size="sm" variant="secondary" onClick={() => setTarget(u)}>{u.enabled ? 'Disable' : 'Enable'}</Button></Td></tr>))}</tbody></Table><Pagination data={data} onPage={setPage} /></>)}</div>
      {target && <Confirm danger={target.enabled} title={`${target.enabled ? 'Disable' : 'Enable'} ${target.fullName}?`} text={target.enabled ? 'The user will no longer be able to log in or use the API.' : 'The user will be able to log in again.'} onConfirm={toggle} onClose={() => setTarget(null)} />}
    </>
  );
}

export function AdminReports() {
  const [type, setType] = useState('FOUND'); const [status, setStatus] = useState(''); const [q, setQ] = useState('');
  const [applied, setApplied] = useState({ type: 'FOUND' });
  const { data, loading, error, reload, setPage } = useList('/api/admin/reports', applied);
  const statuses = type === 'FOUND' ? ['REPORTED', 'UNDER_REVIEW', 'AVAILABLE', 'CLAIM_PENDING', 'RETURNED', 'REJECTED'] : ['REPORTED', 'UNDER_REVIEW', 'MATCHED', 'RESOLVED', 'CLOSED', 'REJECTED'];
  return (
    <>
      <PageHeader title="Reports" subtitle="All lost and found reports, in every status." />
      <form onSubmit={(e) => { e.preventDefault(); setApplied({ type, status, q }); setPage(0); }} className="card mb-4 flex flex-wrap gap-3 p-4">
        <Select value={type} onChange={(e) => { setType(e.target.value); setStatus(''); }} style={{ maxWidth: 160 }} aria-label="Type"><option value="FOUND">Found</option><option value="LOST">Lost</option></Select>
        <Select value={status} onChange={(e) => setStatus(e.target.value)} style={{ maxWidth: 200 }} aria-label="Status"><option value="">Any status</option>{statuses.map((s) => <option key={s} value={s}>{label(s)}</option>)}</Select>
        <div className="min-w-[180px] flex-1"><Input placeholder="Item name" value={q} onChange={(e) => setQ(e.target.value)} aria-label="Item name" /></div><Button type="submit">Filter</Button></form>
      <div className="card">{loading ? <SkeletonRows /> : error ? <ErrorState message={error} onRetry={reload} /> : data.content.length === 0 ? <Empty title="No reports match" /> : (<>
        <Table><thead><tr><Th>Item</Th><Th>Category</Th><Th>Reporter</Th><Th>Location</Th><Th>Date</Th><Th>Status</Th></tr></thead><tbody>{data.content.map((i) => (
          <tr key={i.id}><Td><Link className="font-medium text-indigo-700" to={`/items/${i.type}/${i.id}`}>{i.name}</Link></Td><Td>{i.categoryName}</Td><Td>{i.reporterName}</Td><Td>{i.location}</Td><Td>{fmtDate(i.date)}</Td><Td><StatusBadge status={i.status} /></Td></tr>))}</tbody></Table><Pagination data={data} onPage={setPage} /></>)}</div>
    </>
  );
}

export function AdminClaims() {
  const toast = useToast();
  const [status, setStatus] = useState('');
  const { data, loading, error, reload, setPage } = useList('/api/admin/claims', { status });
  const [dlg, setDlg] = useState(null); // {kind, claim}
  const [form, setForm] = useState({ verified: false, reason: '', message: '', scheduledAt: '', location: 'Security Office, Main Block' });
  const [busy, setBusy] = useState(false);
  const close = () => setDlg(null);
  const run = async (fn, ok) => { setBusy(true); try { await fn(); toast.success(ok); close(); reload(); } catch (e) { toast.error(e.friendly); } finally { setBusy(false); } };
  const quick = (c, action, ok) => run(() => api.put(`/api/admin/claims/${c.id}/${action}`), ok);
  const open = (kind, claim) => { setForm({ verified: false, reason: '', message: '', scheduledAt: '', location: 'Security Office, Main Block' }); setDlg({ kind, claim }); };
  const submit = () => {
    const c = dlg.claim;
    if (dlg.kind === 'approve') return run(() => api.put(`/api/admin/claims/${c.id}/approve`, { ownershipVerified: form.verified, reason: form.reason }), 'Claim approved');
    if (dlg.kind === 'reject') return run(() => api.put(`/api/admin/claims/${c.id}/reject`, { ownershipVerified: false, reason: form.reason }), 'Claim rejected');
    if (dlg.kind === 'evidence') return run(() => api.put(`/api/admin/claims/${c.id}/request-evidence`, { message: form.message }), 'Evidence requested');
    return run(() => api.post('/api/recoveries', { claimId: c.id, scheduledAt: form.scheduledAt, location: form.location }), 'Handover scheduled');
  };
  const titles = { approve: 'Approve claim', reject: 'Reject claim', evidence: 'Request additional evidence', schedule: 'Schedule handover' };
  return (
    <>
      <PageHeader title="Claims" subtitle="Review ownership claims. Approval requires verified ownership." />
      <div className="card mb-4 flex gap-3 p-4"><Select value={status} onChange={(e) => { setStatus(e.target.value); setPage(0); }} style={{ maxWidth: 240 }} aria-label="Claim status"><option value="">All statuses</option>{['PENDING', 'UNDER_REVIEW', 'APPROVED', 'REJECTED', 'CANCELLED', 'HANDOVER_SCHEDULED', 'RETURNED'].map((s) => <option key={s} value={s}>{label(s)}</option>)}</Select></div>
      <div className="card">{loading ? <SkeletonRows /> : error ? <ErrorState message={error} onRetry={reload} /> : data.content.length === 0 ? <Empty title="No claims" text="Claims submitted by users will appear here." /> : (<>
        {data.content.map((c) => (
          <div key={c.id} className="border-b border-slate-100 p-5 last:border-0">
            <div className="flex flex-wrap items-start justify-between gap-3">
              <div><Link to={`/items/FOUND/${c.foundItemId}`} className="font-semibold text-indigo-700">{c.foundItemName}</Link><span className="text-sm text-slate-500"> · {c.claimantName} ({c.claimantEmail}) · {fmtDateTime(c.createdAt)}</span></div><StatusBadge status={c.status} />
            </div>
            <div className="mt-3 grid gap-3 text-sm text-slate-600 md:grid-cols-2">
              <div className="rounded-xl bg-slate-50 p-3"><b className="text-slate-800">Claimant's explanation</b><p className="mt-1">{c.ownershipExplanation}</p><p className="mt-2"><b>Distinguishing:</b> {c.distinguishingCharacteristics}</p>{c.evidence?.length > 0 && <p className="mt-2"><b>Evidence:</b> {c.evidence.join(' · ')}</p>}</div>
              <div className="rounded-xl bg-amber-50 p-3"><b className="text-amber-900">Private details on the found report</b><p className="mt-1 text-amber-900">{c.foundItemIdentifyingCharacteristics || 'None recorded'}</p>{c.decisionReason && <p className="mt-2 text-slate-700"><b>Decision:</b> {c.decisionReason}</p>}</div>
            </div>
            <div className="mt-3 flex flex-wrap gap-2">
              {c.status === 'PENDING' && <Button size="sm" onClick={() => quick(c, 'review', 'Review started')}>Start review</Button>}
              {c.status === 'REJECTED' && <Button size="sm" variant="secondary" onClick={() => quick(c, 'review', 'Claim reopened for review')}>Reconsider</Button>}
              {c.status === 'UNDER_REVIEW' && <><Button size="sm" variant="success" onClick={() => open('approve', c)}>Approve</Button><Button size="sm" variant="danger" onClick={() => open('reject', c)}>Reject</Button></>}
              {['PENDING', 'UNDER_REVIEW'].includes(c.status) && <Button size="sm" variant="secondary" onClick={() => open('evidence', c)}>Request evidence</Button>}
              {c.status === 'APPROVED' && <Button size="sm" onClick={() => open('schedule', c)}>Schedule handover</Button>}
            </div>
          </div>))}<Pagination data={data} onPage={setPage} /></>)}</div>
      {dlg && <Modal title={`${titles[dlg.kind]} — ${dlg.claim.foundItemName}`} onClose={close}>
        <div className="space-y-4">
          {dlg.kind === 'approve' && <><label className="flex items-start gap-3 rounded-xl border border-slate-200 p-3 text-sm"><input type="checkbox" className="mt-0.5 h-4 w-4" checked={form.verified} onChange={(e) => setForm({ ...form, verified: e.target.checked })} /><span><b>I have verified ownership</b><br /><span className="text-slate-500">Compare the claimant's details with the private details on the found report.</span></span></label>
            <Field label="Verification note"><Textarea rows={3} value={form.reason} onChange={(e) => setForm({ ...form, reason: e.target.value })} /></Field></>}
          {dlg.kind === 'reject' && <Field label="Reason for rejection" hint="Shown to the claimant."><Textarea value={form.reason} onChange={(e) => setForm({ ...form, reason: e.target.value })} /></Field>}
          {dlg.kind === 'evidence' && <Field label="What evidence is needed?"><Textarea value={form.message} onChange={(e) => setForm({ ...form, message: e.target.value })} /></Field>}
          {dlg.kind === 'schedule' && <><Field label="Handover date and time"><Input type="datetime-local" value={form.scheduledAt} onChange={(e) => setForm({ ...form, scheduledAt: e.target.value })} /></Field><Field label="Location"><Input value={form.location} onChange={(e) => setForm({ ...form, location: e.target.value })} /></Field></>}
          <div className="flex justify-end gap-2"><Button variant="secondary" onClick={close}>Cancel</Button><Button variant={dlg.kind === 'reject' ? 'danger' : 'primary'} loading={busy} onClick={submit}>{titles[dlg.kind]}</Button></div>
        </div></Modal>}
    </>
  );
}

export function AdminRecoveries() {
  const toast = useToast();
  const { data, loading, error, reload, setPage } = useList('/api/recoveries', {});
  const [target, setTarget] = useState(null);
  const complete = async () => { try { await api.put(`/api/recoveries/${target.id}/complete`); toast.success('Item marked as returned'); reload(); } catch (e) { toast.error(e.friendly); } setTarget(null); };
  return (
    <>
      <PageHeader title="Recoveries" subtitle="Scheduled handovers and recovery history." />
      <div className="card">{loading ? <SkeletonRows /> : error ? <ErrorState message={error} onRetry={reload} /> : data.content.length === 0 ? <Empty title="No recoveries yet" text="Approve a claim, then schedule its handover from the Claims page." /> : (<>
        <Table><thead><tr><Th>Item</Th><Th>Claimant</Th><Th>Scheduled</Th><Th>Location</Th><Th>Responsible admin</Th><Th>Confirmation</Th><Th>Status</Th><Th> </Th></tr></thead><tbody>{data.content.map((r) => (
          <tr key={r.id}><Td className="font-medium">{r.itemName}</Td><Td>{r.claimantName}</Td><Td>{fmtDateTime(r.scheduledAt)}</Td><Td>{r.location}</Td><Td>{r.handledBy}</Td><Td>{r.confirmationCode || '—'}</Td><Td><StatusBadge status={r.status} /></Td>
            <Td>{r.status === 'SCHEDULED' && <Button size="sm" variant="success" onClick={() => setTarget(r)}>Mark returned</Button>}</Td></tr>))}</tbody></Table><Pagination data={data} onPage={setPage} /></>)}</div>
      {target && <Confirm title="Mark item as returned?" text={`Confirm that “${target.itemName}” was handed to ${target.claimantName}. The item can never be claimed again.`} confirmLabel="Mark as returned" onConfirm={complete} onClose={() => setTarget(null)} />}
    </>
  );
}

export function AdminCategories() {
  const toast = useToast();
  const { data, loading, error, reload } = useFetch(() => api.get('/api/categories'), []);
  const [name, setName] = useState(''); const [err, setErr] = useState({});
  const [del, setDel] = useState(null);
  const add = async (e) => { e.preventDefault(); setErr({}); try { await api.post('/api/admin/categories', { name }); setName(''); toast.success('Category added'); reload(); } catch (ex) { setErr(ex.fieldErrors); toast.error(ex.friendly); } };
  const remove = async () => { try { await api.del(`/api/admin/categories/${del.id}`); toast.success('Category deleted'); reload(); } catch (e) { toast.error(e.friendly); } setDel(null); };
  return (
    <>
      <PageHeader title="Categories" subtitle="Categories used when reporting items." />
      <form onSubmit={add} className="card mb-4 flex flex-wrap items-start gap-3 p-4"><div className="min-w-[220px] flex-1"><Field label="New category" error={err.name}><Input value={name} error={err.name} onChange={(e) => setName(e.target.value)} /></Field></div><Button type="submit" className="mt-[26px]">Add category</Button></form>
      <div className="card">{loading ? <SkeletonRows /> : error ? <ErrorState message={error} onRetry={reload} /> : (<Table><thead><tr><Th>Name</Th><Th> </Th></tr></thead><tbody>{data.map((c) => <tr key={c.id}><Td className="font-medium">{c.name}</Td><Td><Button size="sm" variant="secondary" onClick={() => setDel(c)}>Delete</Button></Td></tr>)}</tbody></Table>)}</div>
      {del && <Confirm danger title={`Delete “${del.name}”?`} text="Categories in use by existing reports cannot be deleted." confirmLabel="Delete" onConfirm={remove} onClose={() => setDel(null)} />}
    </>
  );
}

export function AdminLogs() {
  const [suspicious, setSuspicious] = useState(false);
  const { data, loading, error, reload, setPage } = useList('/api/admin/activity-logs', { suspicious });
  return (
    <>
      <PageHeader title="Activity logs" subtitle="Audit trail of important actions. Suspicious events are flagged." action={<Button variant={suspicious ? 'primary' : 'secondary'} onClick={() => { setSuspicious(!suspicious); setPage(0); }}>{suspicious ? 'Showing suspicious only' : 'Show suspicious only'}</Button>} />
      <div className="card">{loading ? <SkeletonRows /> : error ? <ErrorState message={error} onRetry={reload} /> : data.content.length === 0 ? <Empty title="No log entries" /> : (<>
        <Table><thead><tr><Th>When</Th><Th>User</Th><Th>Action</Th><Th>Entity</Th><Th>Details</Th><Th> </Th></tr></thead><tbody>{data.content.map((l) => (
          <tr key={l.id}><Td className="whitespace-nowrap">{fmtDateTime(l.createdAt)}</Td><Td>{l.user || 'System'}</Td><Td className="font-medium">{label(l.action)}</Td><Td>{l.entityType ? `${l.entityType} #${l.entityId ?? ''}` : '—'}</Td><Td className="max-w-xs truncate">{l.details}</Td>
            <Td>{l.suspicious && <span className="rounded-full bg-red-100 px-2.5 py-1 text-xs font-semibold text-red-700">Suspicious</span>}</Td></tr>))}</tbody></Table><Pagination data={data} onPage={setPage} /></>)}</div>
    </>
  );
}
