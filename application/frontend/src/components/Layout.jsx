import { useEffect, useState } from 'react';
import { NavLink, Outlet, useNavigate } from 'react-router-dom';
import { Bell, Compass, FileSearch, FolderOpen, HandHelping, LayoutDashboard, LogOut, Menu, PackageSearch, PackagePlus, ScrollText, ShieldCheck, Tags, User, Users, ClipboardList, Truck, X, Search } from 'lucide-react';
import { useAuth } from '../auth.jsx';
import { api } from '../api.js';

const userNav = [
  { to: '/', label: 'Home', icon: LayoutDashboard, end: true },
  { to: '/search', label: 'Search items', icon: Search },
  { to: '/report/lost', label: 'Report lost item', icon: PackageSearch },
  { to: '/report/found', label: 'Report found item', icon: PackagePlus },
  { to: '/my-reports', label: 'My reports', icon: FolderOpen },
  { to: '/my-claims', label: 'My claims', icon: HandHelping },
];
const modNav = [{ to: '/moderator', label: 'Moderation', icon: ShieldCheck }];
const adminNav = [
  { to: '/admin', label: 'Dashboard', icon: LayoutDashboard, end: true },
  { to: '/admin/claims', label: 'Claims', icon: ClipboardList },
  { to: '/admin/recoveries', label: 'Recoveries', icon: Truck },
  { to: '/admin/reports', label: 'Reports', icon: FileSearch },
  { to: '/admin/users', label: 'Users', icon: Users },
  { to: '/admin/categories', label: 'Categories', icon: Tags },
  { to: '/admin/logs', label: 'Activity logs', icon: ScrollText },
];

function NavGroup({ title, items, onNavigate }) {
  return (
    <div className="mb-5">
      {title && <div className="mb-1.5 px-3 text-xs font-semibold text-slate-400">{title}</div>}
      {items.map(({ to, label, icon: Icon, end }) => (
        <NavLink key={to} to={to} end={end} onClick={onNavigate}
          className={({ isActive }) => `mb-0.5 flex items-center gap-3 rounded-xl px-3 py-2.5 text-sm font-medium transition ${isActive ? 'bg-indigo-500/20 text-white' : 'text-slate-300 hover:bg-white/5 hover:text-white'}`}>
          <Icon className="h-[18px] w-[18px]" />{label}
        </NavLink>
      ))}
    </div>
  );
}

export default function Layout() {
  const { user, logout, isAdmin, isStaff } = useAuth();
  const nav = useNavigate();
  const [open, setOpen] = useState(false);
  const [unread, setUnread] = useState(0);

  useEffect(() => {
    const load = () => api.get('/api/notifications/unread-count').then((r) => setUnread(r.count)).catch(() => {});
    load();
    const t = setInterval(load, 30000);
    window.addEventListener('notifications-changed', load);
    return () => { clearInterval(t); window.removeEventListener('notifications-changed', load); };
  }, []);

  const sidebar = (
    <div className="flex h-full flex-col bg-navy-900 p-4">
      <div className="mb-6 flex items-center gap-2.5 px-2 pt-1">
        <div className="rounded-xl bg-indigo-500 p-2"><Compass className="h-5 w-5 text-white" /></div>
        <div className="text-[15px] font-bold leading-tight text-white">Lost &amp; Found<div className="text-xs font-medium text-slate-400">Campus recovery desk</div></div>
      </div>
      <nav className="flex-1 overflow-y-auto">
        <NavGroup title="Workspace" items={userNav} onNavigate={() => setOpen(false)} />
        {isStaff && <NavGroup title="Moderator" items={modNav} onNavigate={() => setOpen(false)} />}
        {isAdmin && <NavGroup title="Administration" items={adminNav} onNavigate={() => setOpen(false)} />}
      </nav>
      <NavGroup items={[{ to: '/notifications', label: 'Notifications', icon: Bell }, { to: '/profile', label: 'Profile', icon: User }]} onNavigate={() => setOpen(false)} />
      <button onClick={() => { logout(); nav('/login'); }} className="flex items-center gap-3 rounded-xl px-3 py-2.5 text-sm font-medium text-slate-300 hover:bg-white/5 hover:text-white">
        <LogOut className="h-[18px] w-[18px]" />Log out
      </button>
    </div>
  );

  return (
    <div className="min-h-screen lg:pl-64">
      <aside className="fixed inset-y-0 left-0 hidden w-64 lg:block">{sidebar}</aside>
      {open && (
        <div className="fixed inset-0 z-40 lg:hidden">
          <div className="absolute inset-0 bg-navy-950/60" onClick={() => setOpen(false)} />
          <div className="absolute inset-y-0 left-0 w-72"><button aria-label="Close menu" onClick={() => setOpen(false)} className="absolute right-3 top-3 z-10 text-white"><X /></button>{sidebar}</div>
        </div>
      )}
      <header className="sticky top-0 z-30 flex items-center justify-between border-b border-slate-200 bg-white/90 px-4 py-3 backdrop-blur sm:px-8">
        <button className="rounded-lg p-2 text-slate-600 hover:bg-slate-100 lg:hidden" onClick={() => setOpen(true)} aria-label="Open menu"><Menu className="h-5 w-5" /></button>
        <div className="hidden text-sm text-slate-500 lg:block">Signed in as <span className="font-semibold text-slate-800">{user?.fullName}</span></div>
        <div className="flex items-center gap-3">
          <button onClick={() => nav('/notifications')} className="relative rounded-xl p-2 text-slate-600 hover:bg-slate-100" aria-label={`Notifications, ${unread} unread`}>
            <Bell className="h-5 w-5" />
            {unread > 0 && <span className="absolute -right-0.5 -top-0.5 min-w-[18px] rounded-full bg-indigo-600 px-1 text-center text-[11px] font-bold text-white">{unread > 99 ? '99+' : unread}</span>}
          </button>
          <div className="flex h-9 w-9 items-center justify-center rounded-full bg-indigo-100 text-sm font-bold text-indigo-700">{user?.fullName?.[0]}</div>
        </div>
      </header>
      <main className="mx-auto max-w-7xl px-4 py-8 sm:px-8"><Outlet /></main>
    </div>
  );
}
