import { Link } from 'react-router-dom';
import { HandHelping, PackagePlus, PackageSearch, Search } from 'lucide-react';
import { api, fmtDate, useFetch } from '../api.js';
import { useAuth } from '../auth.jsx';
import { Empty, PageHeader, SkeletonRows, Stat, StatusBadge } from '../components/ui.jsx';

export default function Home() {
  const { user } = useAuth();
  const lost = useFetch(() => api.get('/api/lost-items', { mine: true, size: 5 }), []);
  const found = useFetch(() => api.get('/api/found-items', { mine: true, size: 5 }), []);
  const claims = useFetch(() => api.get('/api/claims/my-claims', { size: 5 }), []);
  const recent = [...(lost.data?.content || []), ...(found.data?.content || [])].sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt)).slice(0, 6);
  const actions = [
    { to: '/report/lost', icon: PackageSearch, title: 'Report a lost item', text: 'Describe it once; we match it against found items.' },
    { to: '/report/found', icon: PackagePlus, title: 'Report a found item', text: 'List what you found and where it can be collected.' },
    { to: '/search', icon: Search, title: 'Search found items', text: 'Filter by category, place and date.' },
  ];
  return (
    <>
      <PageHeader title={`Hello, ${user.fullName.split(' ')[0]}`} subtitle="Here is where your reports and claims stand." />
      <div className="mb-6 grid gap-4 md:grid-cols-3">{actions.map(({ to, icon: Icon, title, text }) => (
        <Link key={to} to={to} className="card group p-5 transition hover:border-indigo-300 hover:shadow-md">
          <Icon className="mb-3 h-6 w-6 text-indigo-600" /><div className="font-semibold text-slate-900">{title}</div><p className="mt-1 text-sm text-slate-500">{text}</p>
        </Link>))}</div>
      <div className="mb-6 grid gap-4 sm:grid-cols-3">
        <Stat icon={PackageSearch} label="My lost reports" value={lost.data?.totalElements} />
        <Stat icon={PackagePlus} label="My found reports" value={found.data?.totalElements} tone="emerald" />
        <Stat icon={HandHelping} label="My claims" value={claims.data?.totalElements} tone="amber" />
      </div>
      <div className="card">
        <div className="flex items-center justify-between border-b border-slate-200 px-5 py-4"><h2 className="font-semibold">Recent reports</h2><Link className="text-sm font-medium text-indigo-600" to="/my-reports">View all</Link></div>
        {lost.loading || found.loading ? <SkeletonRows /> : recent.length === 0 ? (
          <Empty title="No reports yet" text="Lost something or found something? Start with a report." />
        ) : recent.map((i) => (
          <Link key={`${i.type}${i.id}`} to={`/items/${i.type}/${i.id}`} className="flex items-center justify-between gap-3 border-b border-slate-100 px-5 py-3.5 last:border-0 hover:bg-slate-50">
            <div><div className="font-medium text-slate-900">{i.name}</div><div className="text-xs text-slate-500">{i.type === 'LOST' ? 'Lost' : 'Found'} · {i.location} · {fmtDate(i.date)}</div></div>
            <StatusBadge status={i.status} />
          </Link>))}
      </div>
    </>
  );
}
