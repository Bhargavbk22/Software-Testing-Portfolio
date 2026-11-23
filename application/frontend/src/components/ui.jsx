import { useEffect } from 'react';
import { Inbox, ChevronLeft, ChevronRight, Loader2, X } from 'lucide-react';
import { label } from '../api.js';

export function Button({ variant = 'primary', size = 'md', loading, className = '', children, ...p }) {
  const base = 'inline-flex items-center justify-center gap-2 rounded-xl font-semibold transition disabled:cursor-not-allowed disabled:opacity-60';
  const sizes = { sm: 'px-3 py-1.5 text-xs', md: 'px-4 py-2.5 text-sm' };
  const variants = {
    primary: 'bg-indigo-600 text-white shadow-sm hover:bg-indigo-700',
    secondary: 'border border-slate-300 bg-white text-slate-700 hover:bg-slate-50',
    danger: 'bg-red-600 text-white hover:bg-red-700',
    success: 'bg-emerald-600 text-white hover:bg-emerald-700',
    ghost: 'text-slate-600 hover:bg-slate-100',
  };
  return (
    <button className={`${base} ${sizes[size]} ${variants[variant]} ${className}`} disabled={loading || p.disabled} {...p}>
      {loading && <Loader2 className="h-4 w-4 animate-spin" />}
      {children}
    </button>
  );
}

export function Field({ label: text, error, hint, children }) {
  return (
    <label className="block">
      <span className="mb-1.5 block text-sm font-medium text-slate-700">{text}</span>
      {children}
      {hint && !error && <span className="mt-1 block text-xs text-slate-500">{hint}</span>}
      {error && <span role="alert" className="mt-1 block text-xs font-medium text-red-600">{error}</span>}
    </label>
  );
}

export const Input = ({ error, className = '', ...p }) => <input className={`input ${error ? 'input-error' : ''} ${className}`} aria-invalid={!!error} {...p} />;
export const Textarea = ({ error, ...p }) => <textarea rows={4} className={`input ${error ? 'input-error' : ''}`} aria-invalid={!!error} {...p} />;
export const Select = ({ error, children, ...p }) => <select className={`input ${error ? 'input-error' : ''}`} {...p}>{children}</select>;

const tones = {
  REPORTED: 'bg-slate-100 text-slate-700', PENDING: 'bg-amber-100 text-amber-800', UNDER_REVIEW: 'bg-blue-100 text-blue-800',
  MATCHED: 'bg-violet-100 text-violet-800', RESOLVED: 'bg-emerald-100 text-emerald-800', CLOSED: 'bg-slate-200 text-slate-600',
  AVAILABLE: 'bg-emerald-100 text-emerald-800', CLAIM_PENDING: 'bg-amber-100 text-amber-800', RETURNED: 'bg-teal-100 text-teal-800',
  APPROVED: 'bg-emerald-100 text-emerald-800', REJECTED: 'bg-red-100 text-red-700', CANCELLED: 'bg-slate-200 text-slate-600',
  HANDOVER_SCHEDULED: 'bg-indigo-100 text-indigo-800', SCHEDULED: 'bg-indigo-100 text-indigo-800', COMPLETED: 'bg-emerald-100 text-emerald-800',
  SUGGESTED: 'bg-amber-100 text-amber-800', CONFIRMED: 'bg-emerald-100 text-emerald-800', DISMISSED: 'bg-slate-200 text-slate-600',
};
export const StatusBadge = ({ status }) => (
  <span className={`inline-block whitespace-nowrap rounded-full px-2.5 py-1 text-xs font-semibold ${tones[status] || 'bg-slate-100 text-slate-700'}`}>{label(status)}</span>
);

export const Skeleton = ({ className = 'h-4 w-full' }) => <div className={`animate-pulse rounded-lg bg-slate-200 ${className}`} />;
export const SkeletonRows = ({ rows = 4 }) => (
  <div className="space-y-3 p-4" aria-busy="true">{Array.from({ length: rows }).map((_, i) => <Skeleton key={i} className="h-12 w-full" />)}</div>
);

export function Empty({ title, text, action }) {
  return (
    <div className="flex flex-col items-center px-6 py-14 text-center">
      <div className="mb-4 rounded-2xl bg-indigo-50 p-4"><Inbox className="h-8 w-8 text-indigo-500" /></div>
      <h3 className="text-base font-semibold text-slate-800">{title}</h3>
      {text && <p className="mt-1 max-w-sm text-sm text-slate-500">{text}</p>}
      {action && <div className="mt-5">{action}</div>}
    </div>
  );
}

export const ErrorState = ({ message, onRetry }) => (
  <Empty title="Could not load this page" text={message} action={onRetry && <Button variant="secondary" onClick={onRetry}>Try again</Button>} />
);

export function Pagination({ data, onPage }) {
  if (!data || data.totalPages <= 1) return null;
  return (
    <div className="flex items-center justify-between border-t border-slate-200 px-4 py-3 text-sm text-slate-600">
      <span>Page {data.page + 1} of {data.totalPages} · {data.totalElements} results</span>
      <div className="flex gap-2">
        <Button variant="secondary" size="sm" disabled={data.page === 0} onClick={() => onPage(data.page - 1)} aria-label="Previous page"><ChevronLeft className="h-4 w-4" /></Button>
        <Button variant="secondary" size="sm" disabled={data.page + 1 >= data.totalPages} onClick={() => onPage(data.page + 1)} aria-label="Next page"><ChevronRight className="h-4 w-4" /></Button>
      </div>
    </div>
  );
}

export function Modal({ title, onClose, children, wide }) {
  useEffect(() => {
    const k = (e) => e.key === 'Escape' && onClose();
    window.addEventListener('keydown', k);
    return () => window.removeEventListener('keydown', k);
  }, [onClose]);
  return (
    <div className="fixed inset-0 z-50 flex items-end justify-center bg-navy-950/50 p-4 sm:items-center" onMouseDown={(e) => e.target === e.currentTarget && onClose()}>
      <div role="dialog" aria-modal="true" aria-label={title} className={`card max-h-[90vh] w-full overflow-y-auto p-6 shadow-2xl ${wide ? 'max-w-2xl' : 'max-w-md'}`}>
        <div className="mb-4 flex items-center justify-between">
          <h2 className="text-lg font-semibold text-slate-900">{title}</h2>
          <button onClick={onClose} aria-label="Close" className="rounded-lg p-1 text-slate-400 hover:bg-slate-100"><X className="h-5 w-5" /></button>
        </div>
        {children}
      </div>
    </div>
  );
}

export function Confirm({ title, text, confirmLabel = 'Confirm', danger, loading, onConfirm, onClose }) {
  return (
    <Modal title={title} onClose={onClose}>
      <p className="text-sm text-slate-600">{text}</p>
      <div className="mt-6 flex justify-end gap-2">
        <Button variant="secondary" onClick={onClose}>Cancel</Button>
        <Button variant={danger ? 'danger' : 'primary'} loading={loading} onClick={onConfirm}>{confirmLabel}</Button>
      </div>
    </Modal>
  );
}

export const PageHeader = ({ title, subtitle, action }) => (
  <div className="mb-6 flex flex-wrap items-end justify-between gap-3">
    <div>
      <h1 className="text-2xl font-bold tracking-tight text-slate-900">{title}</h1>
      {subtitle && <p className="mt-1 text-sm text-slate-500">{subtitle}</p>}
    </div>
    {action}
  </div>
);

const statTones = { indigo: 'bg-indigo-50 text-indigo-600', emerald: 'bg-emerald-50 text-emerald-600', amber: 'bg-amber-50 text-amber-600', blue: 'bg-blue-50 text-blue-600', rose: 'bg-rose-50 text-rose-600', teal: 'bg-teal-50 text-teal-600' };
export const Stat = ({ icon: Icon, label: text, value, tone = 'indigo' }) => (
  <div className="card flex items-center gap-4 p-5">
    <div className={`rounded-xl p-3 ${statTones[tone]}`}><Icon className="h-6 w-6" /></div>
    <div>
      <div className="text-2xl font-bold text-slate-900">{value ?? '—'}</div>
      <div className="text-sm text-slate-500">{text}</div>
    </div>
  </div>
);

export const Table = ({ children }) => <div className="overflow-x-auto"><table className="w-full min-w-[640px] text-left text-sm">{children}</table></div>;
export const Th = ({ children }) => <th className="whitespace-nowrap border-b border-slate-200 bg-slate-50 px-4 py-3 text-xs font-semibold text-slate-500">{children}</th>;
export const Td = ({ children, className = '' }) => <td className={`border-b border-slate-100 px-4 py-3 align-middle ${className}`}>{children}</td>;