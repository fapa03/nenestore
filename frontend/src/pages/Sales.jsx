import { useEffect, useState } from 'react'
import api from '../services/api'
import { Search, X, Plus, ShoppingCart } from 'lucide-react'

export default function Sales() {
    const [sales, setSales] = useState([])
    const [loading, setLoading] = useState(true)
    const [showForm, setShowForm] = useState(false)

    // sale form state
    const [clients, setClients] = useState([])
    const [selectedClient, setSelectedClient] = useState(null)
    const [clientSearch, setClientSearch] = useState('')
    const [paymentType, setPaymentType] = useState('CASH')
    const [discount, setDiscount] = useState(0)
    const [notes, setNotes] = useState('')
    const [cart, setCart] = useState([])

    // inventory search
    const [itemSearch, setItemSearch] = useState('')
    const [searchResults, setSearchResults] = useState([])
    const [searching, setSearching] = useState(false)
    const [saving, setSaving] = useState(false)

    const [showNewClient, setShowNewClient] = useState(false)
    const [newClientForm, setNewClientForm] = useState({
        name: '', whatsapp: '', instagram: '', level: 'NEW'
    })
    const [savingClient, setSavingClient] = useState(false)

    const fetchSales = () => {
        setLoading(true)
        api.get('/sales')
            .then(res => setSales(res.data))
            .finally(() => setLoading(false))
    }

    useEffect(() => { fetchSales() }, [])

    // search inventory
    const searchItems = async () => {
        if (!itemSearch.trim()) return
        setSearching(true)
        try {
            const res = await api.get('/inventory', {
                params: { search: itemSearch, status: 'Stock' }
            })
            setSearchResults(res.data)
        } finally {
            setSearching(false)
        }
    }

    // search clients
    const searchClients = async (q) => {
        setClientSearch(q)
        if (!q.trim()) return setClients([])
        const res = await api.get('/clients', { params: { search: q } })
        setClients(res.data)
    }

    const addToCart = (item) => {
        if (cart.find(c => c.id === item.id)) return
        setCart([...cart, { ...item, priceMxn: item.salePriceMxn ?? '' }])
        setSearchResults([])
        setItemSearch('')
    }

    const removeFromCart = (id) => {
        setCart(cart.filter(c => c.id !== id))
    }

    const updateCartPrice = (id, price) => {
        setCart(cart.map(c => c.id === id ? { ...c, priceMxn: price } : c))
    }

    const subtotal = cart.reduce((sum, item) =>
        sum + (parseFloat(item.priceMxn) || 0), 0)
    const total = subtotal - (parseFloat(discount) || 0)

    const handleSubmit = async () => {
        if (cart.length === 0) return alert('Add at least one item')
        const invalidPrices = cart.some(c => !c.priceMxn || parseFloat(c.priceMxn) <= 0)
        if (invalidPrices) return alert('All items must have a price')

        setSaving(true)
        try {
            await api.post('/sales', {
                clientId: selectedClient?.id ?? null,
                paymentType,
                discountMxn: parseFloat(discount) || 0,
                notes,
                items: cart.map(c => ({
                    itemId: c.id,
                    priceMxn: parseFloat(c.priceMxn)
                }))
            })
            setShowForm(false)
            resetForm()
            fetchSales()
        } catch (err) {
            alert(err.response?.data?.message || 'Error registering sale')
        } finally {
            setSaving(false)
        }
    }

    const resetForm = () => {
        setCart([])
        setSelectedClient(null)
        setClientSearch('')
        setPaymentType('CASH')
        setDiscount(0)
        setNotes('')
        setItemSearch('')
        setSearchResults([])
    }
    const handleCreateClient = async () => {
        if (!newClientForm.name.trim()) return
        setSavingClient(true)
        try {
            const res = await api.post('/clients', {
                ...newClientForm,
                createdAt: new Date().toISOString(),
                updatedAt: new Date().toISOString()
            })
            setSelectedClient(res.data)
            setShowNewClient(false)
            setNewClientForm({ name: '', whatsapp: '', instagram: '', level: 'NEW' })
        } catch (err) {
            alert(err.response?.data?.message || 'Error creating client')
        } finally {
            setSavingClient(false)
        }
    }

    return (
        <div className="space-y-6">
            <div className="flex items-center justify-between">
                <h1 className="text-brand-smoke text-2xl font-bold">Sales</h1>
                <button
                    onClick={() => setShowForm(true)}
                    className="flex items-center gap-2 bg-brand-accent hover:bg-purple-700
                     text-white px-4 py-2 rounded-lg text-sm font-semibold
                     transition-colors"
                >
                    <Plus size={16} />
                    New Sale
                </button>
            </div>

            {/* Sales history table */}
            <div className="bg-brand-black rounded-xl overflow-hidden">
                <table className="w-full text-sm">
                    <thead>
                        <tr className="text-brand-gray border-b border-brand-gray/20 text-xs uppercase">
                            <th className="text-left px-4 py-3">Sale #</th>
                            <th className="text-left px-4 py-3">Date</th>
                            <th className="text-left px-4 py-3">Client</th>
                            <th className="text-left px-4 py-3">Type</th>
                            <th className="text-right px-4 py-3">Discount</th>
                            <th className="text-right px-4 py-3">Total MXN</th>
                            <th className="text-left px-4 py-3">Items</th>
                        </tr>
                    </thead>
                    <tbody className="divide-y divide-brand-gray/10">
                        {loading ? (
                            <tr>
                                <td colSpan={7} className="text-center py-8 text-brand-gray">
                                    Loading...
                                </td>
                            </tr>
                        ) : sales.length === 0 ? (
                            <tr>
                                <td colSpan={7} className="text-center py-8 text-brand-gray">
                                    No sales yet
                                </td>
                            </tr>
                        ) : sales.map(sale => (
                            <tr key={sale.saleId}
                                className="text-brand-smoke hover:bg-brand-deep/40 transition-colors">
                                <td className="px-4 py-3 font-mono text-brand-accent text-xs">
                                    #{sale.saleId}
                                </td>
                                <td className="px-4 py-3 text-brand-gray">{sale.saleDate}</td>
                                <td className="px-4 py-3">{sale.client?.name ?? '—'}</td>
                                <td className="px-4 py-3">
                                    <span className={`px-2 py-0.5 rounded text-xs font-semibold ${sale.paymentType === 'CREDIT'
                                        ? 'bg-yellow-500/20 text-yellow-400'
                                        : 'bg-green-500/20 text-green-400'
                                        }`}>
                                        {sale.paymentType}
                                    </span>
                                </td>
                                <td className="px-4 py-3 text-right text-red-400">
                                    {sale.discountMxn > 0 ? `-$${sale.discountMxn}` : '—'}
                                </td>
                                <td className="px-4 py-3 text-right font-bold">
                                    ${sale.totalMxn}
                                </td>
                                <td className="px-4 py-3 text-brand-gray text-xs">
                                    {sale.items?.length ?? 0} items
                                </td>
                            </tr>
                        ))}
                    </tbody>
                </table>
            </div>

            {/* New Sale Modal */}
            {showForm && (
                <div className="fixed inset-0 bg-black/60 flex items-start justify-center
                        z-50 overflow-y-auto py-8">
                    <div className="bg-brand-black rounded-2xl p-6 w-full max-w-2xl shadow-2xl">
                        <div className="flex items-center justify-between mb-6">
                            <div className="flex items-center gap-3">
                                <ShoppingCart size={20} className="text-brand-accent" />
                                <h2 className="text-brand-smoke font-semibold text-lg">New Sale</h2>
                            </div>
                            <button onClick={() => { setShowForm(false); resetForm() }}
                                className="text-brand-gray hover:text-brand-smoke">
                                <X size={18} />
                            </button>
                        </div>

                        {/* Item search */}
                        <div className="mb-4">
                            <label className="block text-brand-gray text-xs mb-1">
                                Search items (Stock only)
                            </label>
                            <div className="flex gap-2">
                                <div className="relative flex-1">
                                    <Search size={14} className="absolute left-3 top-3 text-brand-gray" />
                                    <input
                                        type="text"
                                        value={itemSearch}
                                        onChange={e => setItemSearch(e.target.value)}
                                        onKeyDown={e => e.key === 'Enter' && searchItems()}
                                        placeholder="SKU, product name, order number..."
                                        className="w-full bg-brand-deep text-brand-smoke border
                               border-brand-gray/30 rounded-lg pl-8 pr-3 py-2
                               text-sm focus:outline-none focus:border-brand-accent"
                                    />
                                </div>
                                <button onClick={searchItems} disabled={searching}
                                    className="bg-brand-accent/20 text-brand-accent hover:bg-brand-accent/30
                             px-4 py-2 rounded-lg text-sm transition-colors">
                                    {searching ? '...' : 'Search'}
                                </button>
                            </div>

                            {/* Search results */}
                            {searchResults.length > 0 && (
                                <div className="mt-2 bg-brand-deep rounded-lg border border-brand-gray/20
                                max-h-48 overflow-y-auto">
                                    {searchResults.map(item => (
                                        <button
                                            key={item.id}
                                            onClick={() => addToCart(item)}
                                            className="w-full flex items-center gap-3 px-3 py-2 hover:bg-brand-black
                                 transition-colors text-left"
                                        >
                                            {item.imageUrl && (
                                                <img
                                                    src={`http://localhost:8080${item.imageUrl}`}
                                                    className="w-8 h-8 rounded object-cover"
                                                    alt=""
                                                />
                                            )}
                                            <div className="flex-1 min-w-0">
                                                <p className="text-brand-smoke text-xs font-semibold truncate">
                                                    {item.product}
                                                </p>
                                                <p className="text-brand-gray text-xs">
                                                    {item.sku} · {item.size} · {item.color}
                                                </p>
                                            </div>
                                            <span className="text-brand-accent text-xs shrink-0">
                                                {item.salePriceMxn ? `$${item.salePriceMxn}` : 'No price'}
                                            </span>
                                        </button>
                                    ))}
                                </div>
                            )}
                        </div>

                        {/* Cart */}
                        {cart.length > 0 && (
                            <div className="mb-4 bg-brand-deep rounded-lg overflow-hidden">
                                <table className="w-full text-xs">
                                    <thead>
                                        <tr className="text-brand-gray border-b border-brand-gray/20">
                                            <th className="text-left px-3 py-2">Item</th>
                                            <th className="text-right px-3 py-2">Price MXN</th>
                                            <th className="px-3 py-2"></th>
                                        </tr>
                                    </thead>
                                    <tbody className="divide-y divide-brand-gray/10">
                                        {cart.map(item => (
                                            <tr key={item.id} className="text-brand-smoke">
                                                <td className="px-3 py-2">
                                                    <p className="font-semibold truncate max-w-xs">
                                                        {item.product}
                                                    </p>
                                                    <p className="text-brand-gray">{item.sku}</p>
                                                </td>
                                                <td className="px-3 py-2 text-right">
                                                    <input
                                                        type="number"
                                                        value={item.priceMxn}
                                                        onChange={e => updateCartPrice(item.id, e.target.value)}
                                                        className="w-24 bg-brand-black text-brand-smoke border
                                       border-brand-gray/30 rounded px-2 py-1 text-xs
                                       text-right focus:outline-none focus:border-brand-accent"
                                                    />
                                                </td>
                                                <td className="px-3 py-2 text-center">
                                                    <button onClick={() => removeFromCart(item.id)}
                                                        className="text-red-400 hover:text-red-300">
                                                        <X size={14} />
                                                    </button>
                                                </td>
                                            </tr>
                                        ))}
                                    </tbody>
                                </table>
                            </div>
                        )}

                        {/* Client search */}
                        <div className="mb-4">
                            <label className="block text-brand-gray text-xs mb-1">
                                Client (optional)
                            </label>
                            {selectedClient ? (
                                <div className="flex items-center justify-between bg-brand-deep
                    rounded-lg px-3 py-2">
                                    <span className="text-brand-smoke text-sm">
                                        {selectedClient.name}
                                    </span>
                                    <button onClick={() => setSelectedClient(null)}
                                        className="text-brand-gray hover:text-red-400">
                                        <X size={14} />
                                    </button>
                                </div>
                            ) : (
                                <div className="flex gap-2">
                                    <div className="relative flex-1">
                                        <input
                                            type="text"
                                            value={clientSearch}
                                            onChange={e => searchClients(e.target.value)}
                                            placeholder="Search client by name or WhatsApp..."
                                            className="w-full bg-brand-deep text-brand-smoke border
                     border-brand-gray/30 rounded-lg px-3 py-2 text-sm
                     focus:outline-none focus:border-brand-accent"
                                        />
                                        {clients.length > 0 && (
                                            <div className="absolute top-full left-0 right-0 bg-brand-black
                          border border-brand-gray/20 rounded-lg mt-1
                          max-h-36 overflow-y-auto z-10">
                                                {clients.map(c => (
                                                    <button key={c.id}
                                                        onClick={() => {
                                                            setSelectedClient(c)
                                                            setClientSearch('')
                                                            setClients([])
                                                        }}
                                                        className="w-full text-left px-3 py-2 text-sm text-brand-smoke
                           hover:bg-brand-deep transition-colors">
                                                        {c.name}
                                                        {c.whatsapp && (
                                                            <span className="text-brand-gray text-xs ml-2">
                                                                {c.whatsapp}
                                                            </span>
                                                        )}
                                                    </button>
                                                ))}
                                            </div>
                                        )}
                                    </div>

                                    {/* ← New button */}
                                    <button
                                        onClick={() => setShowNewClient(true)}
                                        className="flex items-center gap-1 bg-brand-accent/20 text-brand-accent
                                        hover:bg-brand-accent/30 px-3 py-2 rounded-lg text-sm
                                        transition-colors shrink-0"
                                    >
                                        <Plus size={14} />
                                        New
                                    </button>
                                </div>
                            )}
                        </div>

                        {/* Payment type + discount */}
                        <div className="grid grid-cols-2 gap-3 mb-4">
                            <div>
                                <label className="block text-brand-gray text-xs mb-1">
                                    Payment Type
                                </label>
                                <select
                                    value={paymentType}
                                    onChange={e => setPaymentType(e.target.value)}
                                    className="w-full bg-brand-deep text-brand-smoke border
                             border-brand-gray/30 rounded-lg px-3 py-2 text-sm
                             focus:outline-none focus:border-brand-accent"
                                >
                                    <option value="CASH">Cash</option>
                                    <option value="CREDIT">Credit</option>
                                </select>
                            </div>
                            <div>
                                <label className="block text-brand-gray text-xs mb-1">
                                    Discount (MXN)
                                </label>
                                <input
                                    type="number"
                                    value={discount}
                                    onChange={e => setDiscount(e.target.value)}
                                    placeholder="0"
                                    className="w-full bg-brand-deep text-brand-smoke border
                             border-brand-gray/30 rounded-lg px-3 py-2 text-sm
                             focus:outline-none focus:border-brand-accent"
                                />
                            </div>
                        </div>

                        {/* Notes */}
                        <div className="mb-4">
                            <label className="block text-brand-gray text-xs mb-1">Notes</label>
                            <input
                                type="text"
                                value={notes}
                                onChange={e => setNotes(e.target.value)}
                                placeholder="Optional notes..."
                                className="w-full bg-brand-deep text-brand-smoke border
                           border-brand-gray/30 rounded-lg px-3 py-2 text-sm
                           focus:outline-none focus:border-brand-accent"
                            />
                        </div>

                        {/* Totals */}
                        {cart.length > 0 && (
                            <div className="bg-brand-deep rounded-lg p-3 mb-4 text-sm space-y-1">
                                <div className="flex justify-between text-brand-gray">
                                    <span>Subtotal</span>
                                    <span>${subtotal.toFixed(2)}</span>
                                </div>
                                {discount > 0 && (
                                    <div className="flex justify-between text-red-400">
                                        <span>Discount</span>
                                        <span>-${parseFloat(discount).toFixed(2)}</span>
                                    </div>
                                )}
                                <div className="flex justify-between text-brand-smoke font-bold
                                border-t border-brand-gray/20 pt-1">
                                    <span>Total</span>
                                    <span className="text-brand-accent">${total.toFixed(2)} MXN</span>
                                </div>
                            </div>
                        )}

                        {/* Actions */}
                        <div className="flex gap-3">
                            <button
                                onClick={() => { setShowForm(false); resetForm() }}
                                className="flex-1 border border-brand-gray/30 text-brand-gray
                           hover:text-brand-smoke py-2.5 rounded-lg text-sm
                           transition-colors"
                            >
                                Cancel
                            </button>
                            <button
                                onClick={handleSubmit}
                                disabled={saving || cart.length === 0}
                                className="flex-1 bg-brand-accent hover:bg-purple-700 text-white
                           py-2.5 rounded-lg text-sm font-semibold transition-colors
                           disabled:opacity-50"
                            >
                                {saving ? 'Processing...' : `Confirm Sale · $${total.toFixed(2)}`}
                            </button>
                        </div>
                    </div>
                </div>
            )}

            {/* New Client Modal (inside sale flow) */}
            {showNewClient && (
                <div className="fixed inset-0 bg-black/70 flex items-center justify-center z-[60]">
                    <div className="bg-brand-black rounded-2xl p-6 w-full max-w-sm shadow-2xl">
                        <div className="flex items-center justify-between mb-5">
                            <h3 className="text-brand-smoke font-semibold">New Client</h3>
                            <button
                                onClick={() => setShowNewClient(false)}
                                className="text-brand-gray hover:text-brand-smoke"
                            >
                                <X size={16} />
                            </button>
                        </div>

                        <div className="space-y-3">
                            {[
                                { label: 'Name *', key: 'name', placeholder: 'Maria Garcia' },
                                { label: 'WhatsApp', key: 'whatsapp', placeholder: '5215512345678' },
                                { label: 'Instagram', key: 'instagram', placeholder: '@mariagarcia' },
                            ].map(({ label, key, placeholder }) => (
                                <div key={key}>
                                    <label className="block text-brand-gray text-xs mb-1">{label}</label>
                                    <input
                                        type="text"
                                        value={newClientForm[key]}
                                        onChange={e => setNewClientForm({ ...newClientForm, [key]: e.target.value })}
                                        placeholder={placeholder}
                                        className="w-full bg-brand-deep text-brand-smoke border
                         border-brand-gray/30 rounded-lg px-3 py-2 text-sm
                         focus:outline-none focus:border-brand-accent"
                                    />
                                </div>
                            ))}

                            <div>
                                <label className="block text-brand-gray text-xs mb-1">Level</label>
                                <select
                                    value={newClientForm.level}
                                    onChange={e => setNewClientForm({ ...newClientForm, level: e.target.value })}
                                    className="w-full bg-brand-deep text-brand-smoke border
                       border-brand-gray/30 rounded-lg px-3 py-2 text-sm
                       focus:outline-none focus:border-brand-accent"
                                >
                                    {['NEW', 'REGULAR', 'VIP', 'WHOLESALE'].map(l => (
                                        <option key={l} value={l}>{l}</option>
                                    ))}
                                </select>
                            </div>
                        </div>

                        <div className="flex gap-3 mt-5">
                            <button
                                onClick={() => setShowNewClient(false)}
                                className="flex-1 border border-brand-gray/30 text-brand-gray
                     hover:text-brand-smoke py-2.5 rounded-lg text-sm
                     transition-colors"
                            >
                                Cancel
                            </button>
                            <button
                                onClick={handleCreateClient}
                                disabled={savingClient || !newClientForm.name.trim()}
                                className="flex-1 bg-brand-accent hover:bg-purple-700 text-white
                     py-2.5 rounded-lg text-sm font-semibold transition-colors
                     disabled:opacity-50"
                            >
                                {savingClient ? 'Saving...' : 'Create & Select'}
                            </button>
                        </div>
                    </div>
                </div>
            )}
        </div>
    )
}