import { useEffect, useState } from 'react'
import api from '../services/api'
import { Package, ShoppingBag, DollarSign, TrendingUp } from 'lucide-react'

function StatCard({ icon: Icon, label, value, sub }) {
    return (
        <div className="bg-brand-black rounded-xl p-6 flex items-center gap-4">
            <div className="bg-brand-accent/20 p-3 rounded-lg">
                <Icon size={24} className="text-brand-accent" />
            </div>
            <div>
                <p className="text-brand-gray text-sm">{label}</p>
                <p className="text-brand-smoke text-2xl font-bold">{value}</p>
                {sub && <p className="text-brand-gray text-xs mt-0.5">{sub}</p>}
            </div>
        </div>
    )
}

export default function Dashboard() {
    const [stats, setStats] = useState(null)
    const [orders, setOrders] = useState([])
    const [loading, setLoading] = useState(true)

    useEffect(() => {
        Promise.all([
            api.get('/dashboard/stats'),
            api.get('/dashboard/orders?limit=8')
        ]).then(([statsRes, ordersRes]) => {
            setStats(statsRes.data)
            setOrders(ordersRes.data)
        }).finally(() => setLoading(false))
    }, [])

    if (loading) return (
        <div className="flex items-center justify-center h-64">
            <p className="text-brand-gray">Loading...</p>
        </div>
    )

    return (
        <div className="space-y-8">
            <h1 className="text-brand-smoke text-2xl font-bold">Dashboard</h1>

            {/* Stats */}
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
                <StatCard
                    icon={Package}
                    label="In Stock"
                    value={stats?.totalStock ?? 0}
                />
                <StatCard
                    icon={ShoppingBag}
                    label="Sold"
                    value={stats?.totalSold ?? 0}
                />
                <StatCard
                    icon={TrendingUp}
                    label="Total Units"
                    value={stats?.totalItems ?? 0}
                />
                <StatCard
                    icon={DollarSign}
                    label="Purchase Value"
                    value={`$${stats?.totalPurchaseValueUsd?.toFixed(2) ?? '0.00'}`}
                    sub="USD"
                />
            </div>

            {/* Last Orders */}
            <div className="bg-brand-black rounded-xl p-6">
                <h2 className="text-brand-smoke font-semibold mb-4">Recent Orders</h2>
                <table className="w-full text-sm">
                    <thead>
                        <tr className="text-brand-gray border-b border-brand-gray/20">
                            <th className="text-left pb-3">Order #</th>
                            <th className="text-left pb-3">Date</th>
                            <th className="text-right pb-3">Items</th>
                            <th className="text-right pb-3">Total USD</th>
                        </tr>
                    </thead>
                    <tbody className="divide-y divide-brand-gray/10">
                        {orders.map(order => (
                            <tr key={order.orderNumber} className="text-brand-smoke">
                                <td className="py-3 font-mono text-brand-accent text-xs">
                                    {order.orderNumber}
                                </td>
                                <td className="py-3 text-brand-gray">{order.orderDate}</td>
                                <td className="py-3 text-right">{order.totalItems}</td>
                                <td className="py-3 text-right">${order.totalPrice}</td>
                            </tr>
                        ))}
                    </tbody>
                </table>
            </div>
        </div>
    )
}