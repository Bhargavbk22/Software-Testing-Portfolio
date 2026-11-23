import { useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { ImageOff, MapPin, Calendar, Info } from 'lucide-react';
import { api, fmtDate, fmtDateTime, useFetch } from '../api.js';
import { useAuth } from '../auth.jsx';
import { useToast } from '../toast.jsx';
import { Button, Confirm, ErrorState, Field, Modal, Skeleton, StatusBadge, Textarea } from '../components/ui.jsx';

function ClaimModal({ item, onClose, onDone }) {
  const toast = useToast();
  const [f, setF] = useState({ ownershipExplanation: '', distinguishingCharacteristics: '', additionalDetails: '', evidence: '' });
  const [err, setErr] = useState({});
  const [busy, setBusy] = useState(false);
  const set = (k) => (e) => setF({ ...f, [k]: e.target.value });
  const submit = async () => {
    setBusy(true); setErr({});
    try {
      await api.post('/api/claims', { foundItemId: item.id, ...f, evidence: f.evidence.split('\n').filter(Boolean) });
      toast.success('Claim submitted. You will be notified about the decision.');
      onDone();
    } catch (ex) { setErr(ex.fieldErrors || {}); toast.error(ex.friendly); } finally { setBusy(false); }
  };
  return (
    <Modal title={`Claim “${item.name}”`} onClose={onClose} wide>
      <div className="space-y-4">
        <Field label="Why is this item yours?" error={err.ownershipExplanation} hint="At least 20 characters."><Textarea value={f.ownershipExplanation} error={err.ownershipExplanation} onChange={set('ownershipExplanation')} /></Field>
        <Field label="Distinguishing characteristics" error={err.distinguishingCharacteristics} hint="Marks, contents, serial numbers — details only the owner would know."><Textarea rows={3} value={f.distinguishingCharacteristics} error={err.distinguishingCharacteristics} onChange={set('distinguishingCharacteristics')} /></Field>
        <Field label="Supporting evidence (one per line)" hint="For example: purchase invoice number, photo you took before losing it."><Textarea rows={3} value={f.evidence} onChange={set('evidence')} /></Field>
        <Field label="Additional verification details (optional)"><Textarea rows={2} value={f.additionalDetails} onChange={set('additionalDetails')} /></Field>
        <div className="flex justify-end gap-2"><Button variant="secondary" onClick={onClose}>Cancel</Button><Button loading={busy} onClick={submit}>Submit claim</Button></div>
      </div>
    </Modal>
  );
}

export default function ItemDetail() {
  const { type, id } = useParams();
  const { user, isStaff } = useAuth();
  const nav = useNavigate();
  const toast = useToast();
  const isLost = type.toUpperCase() === 'LOST';
  const { data: i, loading, error, reload } = useFetch(() => api.get(`/api/${isLost ? 'lost' : 'found'}-items/${id}`), [type, id]);
  const [claiming, setClaiming] = useState(false);
  const [withdrawing, setWithdrawing] = useState(false);

  if (loading) return <div className="space-y-4"><Skeleton className="h-8 w-64" /><Skeleton className="h-80" /></div>;
  if (error) return <ErrorState message={error} onRetry={reload} />;
  const owner = i.reporterId === user.id;
  const canClaim = !isLost && !owner && ['AVAILABLE', 'CLAIM_PENDING'].includes(i.status);
  const canEdit = owner && (isLost ? ['REPORTED', 'UNDER_REVIEW'].includes(i.status) : i.status === 'REPORTED');
  const canWithdraw = isLost && (owner || user.roles.includes('ROLE_ADMIN')) && !['CLOSED', 'REJECTED'].includes(i.status);
  const withdraw = async () => {
    try { await api.del(`/api/lost-items/${id}`); toast.success('Report withdrawn'); setWithdrawing(false); reload(); } catch (e) { toast.error(e.friendly); }
  };
  const row = (k, v) => v ? <div className="flex justify-between gap-4 border-b border-slate-100 py-2.5 text-sm last:border-0"><span className="text-slate-500">{k}</span><span className="text-right font-medium text-slate-800">{v}</span></div> : null;
  return (
    <>
      <div className="mb-6 flex flex-wrap items-start justify-between gap-3">
        <div><div className="mb-1 text-sm font-medium text-indigo-600">{isLost ? 'Lost report' : 'Found item'}</div><h1 className="text-2xl font-bold text-slate-900">{i.name}</h1>
          <div className="mt-2 flex flex-wrap items-center gap-3 text-sm text-slate-500"><StatusBadge status={i.status} />{i.verified && <span className="font-medium text-emerald-600">Verified</span>}<span className="flex items-center gap-1"><MapPin className="h-4 w-4" />{i.location}</span><span className="flex items-center gap-1"><Calendar className="h-4 w-4" />{fmtDate(i.date)}</span></div></div>
        <div className="flex gap-2">
          {canEdit && <Link to={`/items/${type}/${id}/edit`}><Button variant="secondary">Edit report</Button></Link>}
          {canWithdraw && <Button variant="secondary" onClick={() => setWithdrawing(true)}>Withdraw</Button>}
          {canClaim && <Button onClick={() => setClaiming(true)}>Claim this item</Button>}
        </div>
      </div>
      <div className="grid gap-6 lg:grid-cols-[1.4fr_1fr]">
        <div className="space-y-6">
          <div className="card overflow-hidden">
            {i.images?.length ? <div className="grid gap-1 sm:grid-cols-2">{i.images.map((s) => <img key={s} src={s} alt={i.name} className="h-64 w-full object-cover" />)}</div>
              : <div className="flex h-48 items-center justify-center bg-slate-100 text-slate-400"><ImageOff className="mr-2 h-6 w-6" />No photo provided</div>}
            <div className="p-5"><h2 className="mb-2 font-semibold">Description</h2><p className="text-sm leading-relaxed text-slate-600">{i.description}</p></div>
          </div>
          {i.matches?.length > 0 && (
            <div className="card p-5"><h2 className="font-semibold">Potential matches</h2>
              <p className="mb-3 flex items-center gap-1.5 text-xs text-slate-500"><Info className="h-3.5 w-3.5" />The score is an indication, not proof of ownership.</p>
              {i.matches.map((m) => { const o = isLost ? m.foundItem : m.lostItem; return (
                <Link key={m.id} to={`/items/${o.type}/${o.id}`} className="flex items-center justify-between gap-3 border-t border-slate-100 py-3 hover:bg-slate-50">
                  <div><div className="text-sm font-medium">{o.name}</div><div className="text-xs text-slate-500">{o.location} · {fmtDate(o.date)}</div></div>
                  <div className="flex items-center gap-2"><StatusBadge status={m.status} /><span className="rounded-lg bg-indigo-50 px-2 py-1 text-sm font-bold text-indigo-700">{m.score}/100</span></div></Link>); })}
            </div>)}
          {i.history?.length > 0 && (
            <div className="card p-5"><h2 className="mb-3 font-semibold">Status history</h2>
              <ol className="space-y-3 border-l-2 border-slate-200 pl-4">{i.history.map((h, k) => (
                <li key={k} className="text-sm"><span className="font-medium">{h.fromStatus ? `${h.fromStatus.replaceAll('_', ' ')} → ` : ''}{h.toStatus.replaceAll('_', ' ')}</span><div className="text-xs text-slate-500">{h.changedBy} · {fmtDateTime(h.at)}{h.note ? ` · ${h.note}` : ''}</div></li>))}</ol></div>)}
        </div>
        <div className="card h-fit p-5">
          <h2 className="mb-2 font-semibold">Details</h2>
          {row('Category', i.categoryName)}{row('Color', i.color)}{row('Brand', i.brand)}{row(isLost ? 'Lost on' : 'Found on', fmtDate(i.date))}
          {row('Collect from', i.handoverLocation)}{row('Contact', i.contactInfo)}{row('Reported by', i.reporterName)}
          {i.identifyingCharacteristics && <div className="mt-3 rounded-xl bg-amber-50 p-3 text-xs text-amber-800"><b>Private identifying details</b> (visible to you and staff only): {i.identifyingCharacteristics}</div>}
        </div>
      </div>
      {claiming && <ClaimModal item={i} onClose={() => setClaiming(false)} onDone={() => { setClaiming(false); nav('/my-claims'); }} />}
      {withdrawing && <Confirm danger title="Withdraw this report?" text="The report will be closed and can no longer be edited. Its history is kept." confirmLabel="Withdraw report" onConfirm={withdraw} onClose={() => setWithdrawing(false)} />}
    </>
  );
}
