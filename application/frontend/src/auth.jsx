import { createContext, useCallback, useContext, useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { api } from './api.js';
import { useToast } from './toast.jsx';

const Ctx = createContext(null);
export const useAuth = () => useContext(Ctx);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(() => {
    try { return JSON.parse(localStorage.getItem('user')); } catch { return null; }
  });
  const nav = useNavigate();
  const toast = useToast();

  const logout = useCallback(() => {
    localStorage.removeItem('token');
    localStorage.removeItem('user');
    setUser(null);
  }, []);

  useEffect(() => {
    const onExpired = (e) => {
      logout();
      toast.error(e.detail || 'Your session has expired. Please log in again.');
      nav('/login');
    };
    window.addEventListener('auth-expired', onExpired);
    return () => window.removeEventListener('auth-expired', onExpired);
  }, [logout, nav, toast]);

  const login = async (email, password) => {
    const res = await api.post('/api/auth/login', { email, password });
    localStorage.setItem('token', res.token);
    localStorage.setItem('user', JSON.stringify(res.user));
    setUser(res.user);
    return res.user;
  };

  const updateUser = (u) => { localStorage.setItem('user', JSON.stringify(u)); setUser(u); };
  const roles = user?.roles || [];
  const value = { user, login, logout, updateUser, isAdmin: roles.includes('ROLE_ADMIN'), isMod: roles.includes('ROLE_MODERATOR'), isStaff: roles.includes('ROLE_ADMIN') || roles.includes('ROLE_MODERATOR') };
  return <Ctx.Provider value={value}>{children}</Ctx.Provider>;
}
