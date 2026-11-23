import { createContext, useCallback, useContext, useState } from 'react';
import { CheckCircle2, XCircle, Info, X } from 'lucide-react';

const Ctx = createContext(null);
export const useToast = () => useContext(Ctx);

export function ToastProvider({ children }) {
  const [items, setItems] = useState([]);
  const remove = (id) => setItems((s) => s.filter((t) => t.id !== id));
  const push = useCallback((type, message) => {
    const id = Math.random().toString(36).slice(2);
    setItems((s) => [...s, { id, type, message }]);
    setTimeout(() => remove(id), 4500);
  }, []);
  const api = { success: (m) => push('success', m), error: (m) => push('error', m), info: (m) => push('info', m) };
  const icon = { success: <CheckCircle2 className="h-5 w-5 text-emerald-500" />, error: <XCircle className="h-5 w-5 text-red-500" />, info: <Info className="h-5 w-5 text-indigo-500" /> };
  return (
    <Ctx.Provider value={api}>
      {children}
      <div className="fixed bottom-4 right-4 z-[100] flex w-[min(92vw,22rem)] flex-col gap-2" role="status" aria-live="polite">
        {items.map((t) => (
          <div key={t.id} className="card flex items-start gap-3 p-3.5 shadow-lg">
            {icon[t.type]}
            <p className="flex-1 text-sm text-slate-700">{t.message}</p>
            <button onClick={() => remove(t.id)} aria-label="Dismiss" className="text-slate-400 hover:text-slate-600"><X className="h-4 w-4" /></button>
          </div>
        ))}
      </div>
    </Ctx.Provider>
  );
}
