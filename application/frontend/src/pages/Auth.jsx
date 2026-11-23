import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { Compass } from 'lucide-react';
import { useAuth } from '../auth.jsx';
import { useToast } from '../toast.jsx';
import { api } from '../api.js';
import { Button, Field, Input } from '../components/ui.jsx';

function Shell({ title, subtitle, children }) {
  return (
    <div className="grid min-h-screen lg:grid-cols-[1.05fr_1fr]">
      <div className="hidden flex-col justify-between bg-navy-900 p-12 text-white lg:flex">
        <div className="flex items-center gap-2.5 text-lg font-bold"><div className="rounded-xl bg-indigo-500 p-2"><Compass className="h-5 w-5" /></div>Lost &amp; Found</div>
        <div>
          <h2 className="max-w-md text-4xl font-bold leading-tight">Every lost item has a way back to its owner.</h2>
          <p className="mt-4 max-w-md text-slate-300">Report what you lost, list what you found, and let the recovery desk verify and return it — with a clear record of every step.</p>
        </div>
        <p className="text-sm text-slate-400">Academic Software Testing portfolio project</p>
      </div>
      <div className="flex items-center justify-center p-6">
        <div className="w-full max-w-md">
          <h1 className="text-2xl font-bold text-slate-900">{title}</h1>
          <p className="mb-6 mt-1 text-sm text-slate-500">{subtitle}</p>
          {children}
        </div>
      </div>
    </div>
  );
}

export function Login() {
  const { login } = useAuth();
  const toast = useToast();
  const nav = useNavigate();
  const [f, setF] = useState({ email: '', password: '' });
  const [err, setErr] = useState({});
  const [busy, setBusy] = useState(false);
  const submit = async (e) => {
    e.preventDefault();
    setBusy(true); setErr({});
    try { await login(f.email, f.password); nav('/'); }
    catch (ex) { setErr(ex.fieldErrors || {}); toast.error(ex.friendly); }
    finally { setBusy(false); }
  };
  return (
    <Shell title="Welcome back" subtitle="Log in to manage your reports and claims.">
      <form onSubmit={submit} className="space-y-4" noValidate>
        <Field label="Email" error={err.email}><Input type="email" autoComplete="username" value={f.email} error={err.email} onChange={(e) => setF({ ...f, email: e.target.value })} /></Field>
        <Field label="Password" error={err.password}><Input type="password" autoComplete="current-password" value={f.password} error={err.password} onChange={(e) => setF({ ...f, password: e.target.value })} /></Field>
        <div className="text-right text-sm"><Link className="font-medium text-indigo-600 hover:underline" to="/forgot-password">Forgot password?</Link></div>
        <Button type="submit" loading={busy} className="w-full">Log in</Button>
      </form>
      <p className="mt-6 text-center text-sm text-slate-500">New here? <Link className="font-semibold text-indigo-600" to="/register">Create an account</Link></p>
    </Shell>
  );
}

export function Register() {
  const toast = useToast();
  const nav = useNavigate();
  const [f, setF] = useState({ fullName: '', email: '', password: '', confirmPassword: '', studentId: '', department: '', phone: '' });
  const [err, setErr] = useState({});
  const [busy, setBusy] = useState(false);
  const set = (k) => (e) => setF({ ...f, [k]: e.target.value });
  const submit = async (e) => {
    e.preventDefault();
    setBusy(true); setErr({});
    try { await api.post('/api/auth/register', f); toast.success('Account created. You can log in now.'); nav('/login'); }
    catch (ex) { setErr(ex.fieldErrors || {}); toast.error(ex.friendly); }
    finally { setBusy(false); }
  };
  const field = (k, text, props = {}) => <Field label={text} error={err[k]}><Input value={f[k]} onChange={set(k)} error={err[k]} {...props} /></Field>;
  return (
    <Shell title="Create your account" subtitle="Use your campus details so the recovery desk can verify you.">
      <form onSubmit={submit} className="space-y-3.5" noValidate>
        {field('fullName', 'Full name')}
        {field('email', 'Email', { type: 'email' })}
        <div className="grid gap-3.5 sm:grid-cols-2">
          {field('password', 'Password', { type: 'password', autoComplete: 'new-password' })}
          {field('confirmPassword', 'Confirm password', { type: 'password', autoComplete: 'new-password' })}
        </div>
        <p className="-mt-1 text-xs text-slate-500">At least 8 characters with upper and lower case letters, a digit and a special character.</p>
        <div className="grid gap-3.5 sm:grid-cols-2">{field('studentId', 'Student ID')}{field('department', 'Department')}</div>
        {field('phone', 'Phone number', { placeholder: '+919876543210' })}
        <Button type="submit" loading={busy} className="w-full">Create account</Button>
      </form>
      <p className="mt-6 text-center text-sm text-slate-500">Already registered? <Link className="font-semibold text-indigo-600" to="/login">Log in</Link></p>
    </Shell>
  );
}

export function Forgot() {
  const toast = useToast();
  const [email, setEmail] = useState('');
  const [err, setErr] = useState({});
  const [done, setDone] = useState(false);
  const [busy, setBusy] = useState(false);
  const submit = async (e) => {
    e.preventDefault();
    setBusy(true); setErr({});
    try { await api.post('/api/auth/forgot-password', { email }); setDone(true); }
    catch (ex) { setErr(ex.fieldErrors || {}); toast.error(ex.friendly); }
    finally { setBusy(false); }
  };
  return (
    <Shell title="Reset your password" subtitle="Enter your campus email and we will send instructions.">
      {done ? (
        <div className="card p-5 text-sm text-slate-600">If an account exists for <b>{email}</b>, reset instructions have been sent. <Link to="/login" className="mt-3 block font-semibold text-indigo-600">Back to log in</Link></div>
      ) : (
        <form onSubmit={submit} className="space-y-4" noValidate>
          <Field label="Email" error={err.email}><Input type="email" value={email} error={err.email} onChange={(e) => setEmail(e.target.value)} /></Field>
          <Button type="submit" loading={busy} className="w-full">Send reset link</Button>
          <Link to="/login" className="block text-center text-sm font-medium text-indigo-600">Back to log in</Link>
        </form>
      )}
    </Shell>
  );
}
