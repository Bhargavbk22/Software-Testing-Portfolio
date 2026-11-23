import { useState } from 'react';
import { Link } from 'react-router-dom';
import { Check, Eye, X } from 'lucide-react';
import { api, fmtDate, useFetch } from '../api.js';
import { useToast } from '../toast.jsx';
import { Button, Empty, ErrorState, Field, Modal, PageHeader, SkeletonRows, StatusBadge, Textarea } from '../components/ui.jsx';

function ReportRow({ i, act, busy }) {
  return (
    <div className="flex flex-wrap items-center justify-between gap-3 border-b border-slate-100 px-5 py-3.5 last:border-0">
      <div><Link to={`/items/${i.type}/${i.id}`} className="font-medium text-indigo-700">{i.name}</Link>
        <div className="text-xs text-slate-500">{i.type === 'LOST' ? 'Lost' : 'Found'} · {i.categoryName} · {i.location} · {fmtDate(i.date)} · by {i.reporterName}</div></div>
      <div className="flex items-center gap-2"><StatusBadge status={i.status} />
        {i.status === 'REPORTED' && <Button size="sm" variant="secondary" disabled={busy} onClick={() => act('review', i)}><Eye className="h-3.5 w-3.5" />Start review</Button>}
        {i.status === 'UNDER_REVIEW' && !i.verified && <Button size="sm" variant="success" disabled={busy} onClick={() => act('approve', i)}><Check className="h-3.5 w-3.5" />Approve</Button>}
        {['REPORTED', 'UNDER_REVIEW'].includes(i.status) && <Button size="sm" variant="danger" disabled={busy} onClick={() => act('reject', i)}><X className="h-3.5 w-3.5" />Reject</Button>}</div>
    </div>
  );
}

const Section = ({ title, count, children }) => (
  <div className="card mb-6"><div className="flex items-center justify-between border-b border-slate-200 px-5 py-4"><h2 className="font-semibold">{title}</h2><span className="rounded-full bg-slate-100 px-2.5 py-0.5 text-xs font-semibold text-slate-600">{count}</span></div>{children}</div>
);

export default function Moderator() {
  const toast = useToast();
  const { data, loading, error, reload } = useFetch(() => api.get('/api/moderator/dashboard'), []);
  const [rejecting, setRejecting] = useState(null);
  const [reason, setReason] = useState('');
  const [busy, setBusy] = useState(false);

  const run = async (fn, ok) => { setBusy(true); try { await fn(); toast.success(ok); reload(); } catch (e) { toast.error(e.friendly); } finally { setBusy(false); } };
  const act = (kind, i) => kind === 'reject' ? setRejecting(i) : run(() => api.put(`/api/moderator/reports/${i.type}/${i.id}/${kind}`), kind === 'review' ? 'Review started' : 'Report approved');
  const doReject = () => run(async () => { await api.put(`/api/moderator/reports/${rejecting.type}/${rejecting.id}/reject`, { reason }); setRejecting(null); setReason(''); }, 'Report rejected');

  if (loading) return <SkeletonRows rows={6} />;
  if (error) return <ErrorState message={error} onRetry={reload} />;
  const none = <Empty title="Nothing waiting" text="You are up to date." />;
  return (
    <>
      <PageHeader title="Moderation" subtitle="Verify reports, review potential matches and escalate claims to administrators." />
      <Section title="Pending reports" count={data.pendingLost.length + data.pendingFound.length}>
        {data.pendingLost.length + data.pendingFound.length === 0 ? none : [...data.pendingFound, ...data.pendingLost].map((i) => <ReportRow key={i.type + i.id} i={i} act={act} busy={busy} />)}</Section>
      <Section title="Awaiting verification" count={data.awaitingVerification.length}>
        {data.awaitingVerification.length === 0 ? none : data.awaitingVerification.map((i) => <ReportRow key={i.type + i.id} i={i} act={act} busy={busy} />)}</Section>
      <Section title="Potential matches" count={data.suggestedMatches.length}>
        {data.suggestedMatches.length === 0 ? none : data.suggestedMatches.map((m) => (
          <div key={m.id} className="flex flex-wrap items-center justify-between gap-3 border-b border-slate-100 px-5 py-3.5 last:border-0">
            <div className="text-sm"><Link to={`/items/LOST/${m.lostItem.id}`} className="font-medium text-indigo-700">{m.lostItem.name}</Link><span className="mx-2 text-slate-400">↔</span><Link to={`/items/FOUND/${m.foundItem.id}`} className="font-medium text-indigo-700">{m.foundItem.name}</Link>
              <div className="text-xs text-slate-500">Score {m.score}/100 — an indication, not proof of ownership</div></div>
            <div className="flex gap-2"><Button size="sm" variant="success" disabled={busy} onClick={() => run(() => api.put(`/api/moderator/matches/${m.id}/confirm`), 'Match confirmed')}>Confirm</Button><Button size="sm" variant="secondary" disabled={busy} onClick={() => run(() => api.put(`/api/moderator/matches/${m.id}/dismiss`), 'Match dismissed')}>Dismiss</Button></div>
          </div>))}</Section>
      <Section title="Claims requiring review" count={data.claimsToReview.length}>
        {data.claimsToReview.length === 0 ? none : data.claimsToReview.map((c) => (
          <div key={c.id} className="border-b border-slate-100 px-5 py-3.5 last:border-0">
            <div className="flex flex-wrap items-center justify-between gap-3"><div><span className="font-medium">{c.foundItemName}</span><span className="text-sm text-slate-500"> · claimed by {c.claimantName}</span></div>
              <div className="flex items-center gap-2"><StatusBadge status={c.status} />{c.status === 'PENDING' && <Button size="sm" disabled={busy} onClick={() => run(() => api.put(`/api/moderator/claims/${c.id}/escalate`), 'Escalated to administrators')}>Escalate</Button>}</div></div>
            <p className="mt-2 grid gap-1 text-xs text-slate-600 sm:grid-cols-2"><span><b>Claimant says:</b> {c.distinguishingCharacteristics}</span><span><b>Recorded on report:</b> {c.foundItemIdentifyingCharacteristics || '—'}</span></p>
          </div>))}</Section>
      <Section title="Recently reviewed" count={data.recentlyReviewed.length}>
        {data.recentlyReviewed.length === 0 ? none : data.recentlyReviewed.map((i) => <ReportRow key={i.type + i.id} i={i} act={act} busy={busy} />)}</Section>
      {rejecting && <Modal title={`Reject “${rejecting.name}”`} onClose={() => setRejecting(null)}>
        <Field label="Reason for rejection" hint="The reporter will see this message."><Textarea value={reason} onChange={(e) => setReason(e.target.value)} /></Field>
        <div className="mt-4 flex justify-end gap-2"><Button variant="secondary" onClick={() => setRejecting(null)}>Cancel</Button><Button variant="danger" loading={busy} onClick={doReject}>Reject report</Button></div></Modal>}
    </>
  );
}
