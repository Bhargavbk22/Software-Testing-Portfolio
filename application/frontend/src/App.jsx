import { Navigate, Route, Routes } from 'react-router-dom';
import { useAuth } from './auth.jsx';
import Layout from './components/Layout.jsx';
import { Login, Register, Forgot } from './pages/Auth.jsx';
import Home from './pages/Home.jsx';
import Search from './pages/Search.jsx';
import ItemDetail from './pages/ItemDetail.jsx';
import ReportForm from './pages/ReportForm.jsx';
import { MyReports, MyClaims } from './pages/MyStuff.jsx';
import { Notifications, Profile } from './pages/Account.jsx';
import Moderator from './pages/Moderator.jsx';
import { AdminDashboard, AdminUsers, AdminReports, AdminClaims, AdminRecoveries, AdminCategories, AdminLogs } from './pages/Admin.jsx';

function Guard({ children, role }) {
  const { user, isAdmin, isStaff } = useAuth();
  if (!user) return <Navigate to="/login" replace />;
  if (role === 'admin' && !isAdmin) return <Navigate to="/" replace />;
  if (role === 'staff' && !isStaff) return <Navigate to="/" replace />;
  return children;
}

export default function App() {
  const { user } = useAuth();
  return (
    <Routes>
      <Route path="/login" element={user ? <Navigate to="/" replace /> : <Login />} />
      <Route path="/register" element={user ? <Navigate to="/" replace /> : <Register />} />
      <Route path="/forgot-password" element={<Forgot />} />
      <Route element={<Guard><Layout /></Guard>}>
        <Route index element={<Home />} />
        <Route path="search" element={<Search />} />
        <Route path="items/:type/:id" element={<ItemDetail />} />
        <Route path="items/:type/:id/edit" element={<ReportForm />} />
        <Route path="report/lost" element={<ReportForm type="LOST" />} />
        <Route path="report/found" element={<ReportForm type="FOUND" />} />
        <Route path="my-reports" element={<MyReports />} />
        <Route path="my-claims" element={<MyClaims />} />
        <Route path="notifications" element={<Notifications />} />
        <Route path="profile" element={<Profile />} />
        <Route path="moderator" element={<Guard role="staff"><Moderator /></Guard>} />
        <Route path="admin" element={<Guard role="admin"><AdminDashboard /></Guard>} />
        <Route path="admin/users" element={<Guard role="admin"><AdminUsers /></Guard>} />
        <Route path="admin/reports" element={<Guard role="admin"><AdminReports /></Guard>} />
        <Route path="admin/claims" element={<Guard role="admin"><AdminClaims /></Guard>} />
        <Route path="admin/recoveries" element={<Guard role="admin"><AdminRecoveries /></Guard>} />
        <Route path="admin/categories" element={<Guard role="admin"><AdminCategories /></Guard>} />
        <Route path="admin/logs" element={<Guard role="admin"><AdminLogs /></Guard>} />
      </Route>
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}
