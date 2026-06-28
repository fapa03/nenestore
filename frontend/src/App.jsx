import { Routes, Route } from 'react-router-dom'
import ProtectedRoute from './components/ProtectedRoute'
import Layout from './components/Layout'
import Login from './pages/Login'
import Dashboard from './pages/Dashboard'
import Inventory from './pages/Inventory'
import CreditSales from './pages/CreditSales'
import Clients from './pages/Clients'
import Sales from './pages/Sales'
import Catalog from './pages/Catalog'
import Sync from './pages/Sync'

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
        <Route path="credit" element={<CreditSales />} />
        <Route path="clients" element={<Clients />} />
        <Route path="catalog" element={<Catalog />} />
        <Route path="sync" element={<Sync />} />
        <Route path="clients" element={<Clients />} />
        <Route path="catalog" element={<Catalog />} />
        <Route path="sync" element={<Sync />} />

      </Route>
    </Routes>
  )
}