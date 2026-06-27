import { Routes, Route } from 'react-router-dom'
import ProtectedRoute from './components/ProtectedRoute'
import Layout from './components/Layout'
import Login from './pages/Login'
import Dashboard from './pages/Dashboard'
import Inventory from './pages/Inventory'


const Sales = () => <div className="text-brand-smoke text-2xl font-bold">Sales</div>
const Credit = () => <div className="text-brand-smoke text-2xl font-bold">Credit Sales</div>
const Clients = () => <div className="text-brand-smoke text-2xl font-bold">Clients</div>
const Catalog = () => <div className="text-brand-smoke text-2xl font-bold">Catalog</div>
const Sync = () => <div className="text-brand-smoke text-2xl font-bold">DB Update</div>

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<Login />} />
      <Route path="/" element={
        <ProtectedRoute>
          <Layout />
        </ProtectedRoute>
      }>
        <Route index element={<Dashboard />} />
        <Route path="inventory" element={<Inventory />} />
        <Route path="sales" element={<Sales />} />
        <Route path="credit" element={<Credit />} />
        <Route path="clients" element={<Clients />} />
        <Route path="catalog" element={<Catalog />} />
        <Route path="sync" element={<Sync />} />
      </Route>
    </Routes>
  )
}