import { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { UploadCloud } from 'lucide-react';
import { api, useFetch } from '../api.js';
import { useToast } from '../toast.jsx';
import { Button, Field, Input, PageHeader, Select, Skeleton, Textarea } from '../components/ui.jsx';

const MAX_BYTES = 2 * 1024 * 1024;
const TYPES = ['image/jpeg', 'image/png', 'image/webp'];
const empty = { name: '', categoryId: '', description: '', date: '', location: '', color: '', brand: '', identifyingCharacteristics: '', contactInfo: '', handoverLocation: '' };

export default function ReportForm({ type: fixedType }) {
  const params = useParams();
  const type = (fixedType || params.type).toUpperCase();
  const editId = params.id;
  const isLost = type === 'LOST';
  const toast = useToast();
  const nav = useNavigate();
  const cats = useFetch(() => api.get('/api/categories'), []);
  const [f, setF] = useState(empty);
  const [files, setFiles] = useState([]);
  const [err, setErr] = useState({});
  const [busy, setBusy] = useState(false);
  const [loading, setLoading] = useState(!!editId);
  const today = new Date().toISOString().slice(0, 10);

  useEffect(() => {
    if (!editId) return;
    api.get(`/api/${isLost ? 'lost' : 'found'}-items/${editId}`).then((i) => setF({ ...empty, ...Object.fromEntries(Object.entries(i).filter(([, v]) => v != null)), categoryId: i.categoryId })).catch((e) => toast.error(e.friendly)).finally(() => setLoading(false));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [editId]);

  const set = (k) => (e) => setF({ ...f, [k]: e.target.value });
  const pick = (e) => {
    const chosen = [...e.target.files];
    const bad = chosen.find((x) => !TYPES.includes(x.type) || x.size > MAX_BYTES);
    if (bad) { setErr({ ...err, file: bad.size > MAX_BYTES ? `${bad.name} is larger than 2 MB` : `${bad.name} is not a JPG, PNG or WebP image` }); return; }
    if (chosen.length > 3) { setErr({ ...err, file: 'You can upload up to 3 images' }); return; }
    setErr({ ...err, file: undefined }); setFiles(chosen);
  };

  const submit = async (e) => {
    e.preventDefault();
    setBusy(true); setErr({});
    const body = { name: f.name, categoryId: f.categoryId ? Number(f.categoryId) : null, description: f.description, date: f.date || null, location: f.location,
      color: f.color, brand: f.brand, identifyingCharacteristics: f.identifyingCharacteristics, contactInfo: f.contactInfo, handoverLocation: f.handoverLocation };
    try {
      const base = `/api/${isLost ? 'lost' : 'found'}-items`;
      const saved = editId ? await api.put(`${base}/${editId}`, body) : await api.post(base, body);
      for (const file of files) await api.upload(`${base}/${saved.id}/images`, file);
      toast.success(editId ? 'Report updated' : 'Report submitted');
      nav(`/items/${type}/${saved.id}`);
    } catch (ex) { setErr(ex.fieldErrors || {}); toast.error(ex.friendly); } finally { setBusy(false); }
  };

  if (loading) return <Skeleton className="h-96" />;
  const text = (k, label, props = {}, hint) => <Field label={label} error={err[k]} hint={hint}><Input value={f[k]} onChange={set(k)} error={err[k]} {...props} /></Field>;
  return (
    <>
      <PageHeader title={`${editId ? 'Edit' : 'Report'} ${isLost ? 'lost' : 'found'} item`} subtitle={isLost ? 'The more detail you give, the better the matches.' : 'Identifying details stay private and are only used by moderators to verify owners.'} />
      <form onSubmit={submit} noValidate className="card space-y-4 p-6">
        <div className="grid gap-4 sm:grid-cols-2">
          {text('name', 'Item name', { placeholder: 'e.g. Black Dell laptop' })}
          <Field label="Category" error={err.categoryId}><Select value={f.categoryId} error={err.categoryId} onChange={set('categoryId')}><option value="">Select a category</option>{cats.data?.map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}</Select></Field>
        </div>
        <Field label="Description" error={err.description} hint="10 to 1000 characters."><Textarea value={f.description} error={err.description} onChange={set('description')} /></Field>
        <div className="grid gap-4 sm:grid-cols-2">
          {text('date', isLost ? 'Date lost' : 'Date found', { type: 'date', max: today })}
          {text('location', isLost ? 'Where did you lose it?' : 'Where did you find it?')}
          {text('color', 'Color')}{text('brand', 'Brand')}
        </div>
        <Field label="Unique identifying characteristics" error={err.identifyingCharacteristics} hint={isLost ? 'Scratches, stickers, serial numbers.' : 'Kept private — never shown publicly.'}><Textarea rows={3} value={f.identifyingCharacteristics} error={err.identifyingCharacteristics} onChange={set('identifyingCharacteristics')} /></Field>
        {isLost ? text('contactInfo', 'Contact information', { placeholder: 'Phone or email we can reach you on' }) : text('handoverLocation', 'Handover location', { placeholder: 'e.g. Security Office, Main Block' })}
        <Field label="Photos (JPG, PNG or WebP, up to 3 × 2 MB)" error={err.file}>
          <label className="flex cursor-pointer items-center gap-3 rounded-xl border-2 border-dashed border-slate-300 p-4 text-sm text-slate-500 hover:border-indigo-400">
            <UploadCloud className="h-6 w-6 text-indigo-500" />{files.length ? files.map((x) => x.name).join(', ') : 'Choose image files'}
            <input type="file" accept="image/jpeg,image/png,image/webp" multiple className="sr-only" onChange={pick} />
          </label>
        </Field>
        <div className="flex justify-end gap-2"><Button type="button" variant="secondary" onClick={() => nav(-1)}>Cancel</Button><Button type="submit" loading={busy}>{editId ? 'Save changes' : 'Submit report'}</Button></div>
      </form>
    </>
  );
}
