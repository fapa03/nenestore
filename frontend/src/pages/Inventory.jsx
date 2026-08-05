import { useEffect, useState } from 'react'
import api from '../services/api'
import { Search, Filter } from 'lucide-react'

export default function Inventory() {
    const [items, setItems] = useState([])
    const [loading, setLoading] = useState(true)
    const [search, setSearch] = useState('')
    const [gender, setGender] = useState('')
    const [status, setStatus] = useState('')
    const [size, setSize] = useState('')
    const [showExportMenu, setShowExportMenu] = useState(false)

    const fetchItems = () => {
        setLoading(true)
        const params = {}
        if (search) params.search = search
        if (gender) params.gender = gender
        if (status) params.status = status
        if (size) params.size = size

        api.get('/inventory', { params })
            .then(res => setItems(res.data))
            .finally(() => setLoading(false))
    }

    useEffect(() => { fetchItems() }, [])

    useEffect(() => {
        const handleClickOutside = () => setShowExportMenu(false)
        if (showExportMenu) {
            document.addEventListener('click', handleClickOutside)
        }
        return () => document.removeEventListener('click', handleClickOutside)
    }, [showExportMenu])



    const handleSearch = (e) => {
        e.preventDefault()
        fetchItems()
    }

    const toggleStatus = async (item) => {
        const newStatus = item.status === 'Stock' ? 'Unavailable' : 'Stock'
        await api.patch(`/inventory/${item.id}/status`, { status: newStatus })
        fetchItems()
    }

    const exportData = (format) => {
        if (items.length === 0) return

        if (format === 'csv') {
            const headers = [
                'SKU', 'Product', 'Color', 'Size', 'Gender',
                'Status', 'Purchase Price USD', 'Sale Price MXN',
                'Order Number', 'Order Date', 'Image URL', 'Barcode'
            ]
            const rows = items.map(i => [
                i.sku,
                `"${i.product}"`,
                i.color,
                i.size,
                i.gender,
                i.status,
                i.purchasePriceUsd,
                i.salePriceMxn ?? '',
                i.orderNumber,
                i.orderDate,
                i.imageUrl ?? '',
                i.barcode ?? ''
            ])
            const csv = [headers.join(','), ...rows.map(r => r.join(','))].join('\n')
            downloadFile(csv, 'nenestore-inventory.csv', 'text/csv')
        }

        if (format === 'json') {
            const json = JSON.stringify(items, null, 2)
            downloadFile(json, 'nenestore-inventory.json', 'application/json')
        }

        setShowExportMenu(false)
    }

    const downloadFile = (content, filename, type) => {
        const blob = new Blob([content], { type })
        const url = URL.createObjectURL(blob)
        const a = document.createElement('a')
        a.href = url
        a.download = filename
        a.click()
        URL.revokeObjectURL(url)
    }

    return (
        <div className="space-y-6">
            <h1 className="text-brand-smoke text-2xl font-bold">Inventory</h1>

            {/* Filters */}
            <div className="bg-brand-black rounded-xl p-4">
                <form onSubmit={handleSearch} className="flex flex-wrap gap-3">
                    {/* Search */}
                    <div className="flex-1 min-w-48 relative">
                        <Search size={14} className="absolute left-3 top-3 text-brand-gray" />
                        <input
                            type="text"
                            placeholder="Search SKU, product, order..."
                            value={search}
                            onChange={e => setSearch(e.target.value)}
                            className="w-full bg-brand-deep text-brand-smoke border border-brand-gray/30
                         rounded-lg pl-8 pr-3 py-2 text-sm focus:outline-none
                         focus:border-brand-accent"
                        />
                    </div>

                    {/* Gender */}
                    <select
                        value={gender}
                        onChange={e => setGender(e.target.value)}
                        className="bg-brand-deep text-brand-smoke border border-brand-gray/30
                       rounded-lg px-3 py-2 text-sm focus:outline-none
                       focus:border-brand-accent"
                    >
                        <option value="">All Genders</option>
                        <option value="M">Male</option>
                        <option value="F">Female</option>
                        <option value="U">Unisex</option>
                    </select>

                    {/* Status */}
                    <select
                        value={status}
                        onChange={e => setStatus(e.target.value)}
                        className="bg-brand-deep text-brand-smoke border border-brand-gray/30
                       rounded-lg px-3 py-2 text-sm focus:outline-none
                       focus:border-brand-accent"
                    >
                        <option value="">All Status</option>
                        <option value="Stock">Stock</option>
                        <option value="Unavailable">Unavailable</option>
                    </select>

                    {/* Size */}
                    <input
                        type="text"
                        placeholder="Size"
                        value={size}
                        onChange={e => setSize(e.target.value)}
                        className="w-20 bg-brand-deep text-brand-smoke border border-brand-gray/30
                       rounded-lg px-3 py-2 text-sm focus:outline-none
                       focus:border-brand-accent"
                    />

                    <button
                        type="submit"
                        className="bg-brand-accent hover:bg-purple-700 text-white px-4 py-2
                       rounded-lg text-sm font-semibold transition-colors flex items-center gap-2"
                    >
                        <Filter size={14} />
                        Filter
                    </button>

                    {/* Export button */}
                    <div className="relative">
                        <button
                            type="button"
                            onClick={(e) => {
                                e.stopPropagation()
                                setShowExportMenu(prev => !prev)
                            }}
                            className="bg-green-600 hover:bg-green-700 text-white px-4 py-2
               rounded-lg text-sm font-semibold transition-colors
               flex items-center gap-2"
                        >
                            Export
                            <span className="text-xs">▾</span>
                        </button>

                        {showExportMenu && (
                            <div className="absolute right-0 top-full mt-1 bg-brand-black border
                    border-brand-gray/20 rounded-lg shadow-xl z-10 overflow-hidden
                    min-w-32">
                                <button
                                    onClick={() => exportData('csv')}
                                    className="w-full text-left px-4 py-2.5 text-sm text-brand-smoke
                   hover:bg-brand-deep transition-colors"
                                >
                                    📄 CSV
                                </button>
                                <button
                                    onClick={() => exportData('json')}
                                    className="w-full text-left px-4 py-2.5 text-sm text-brand-smoke
                   hover:bg-brand-deep transition-colors border-t
                   border-brand-gray/10"
                                >
                                    📋 JSON
                                </button>
                            </div>
                        )}
                    </div>
                </form>
            </div>

            {/* Table */}
            <div className="bg-brand-black rounded-xl overflow-hidden">
                <div className="overflow-x-auto">
                    <table className="w-full text-sm">
                        <thead>
                            <tr className="text-brand-gray border-b border-brand-gray/20 text-xs uppercase">
                                <th className="text-left px-4 py-3">Image</th>
                                <th className="text-left px-4 py-3">SKU</th>
                                <th className="text-left px-4 py-3">Product</th>
                                <th className="text-left px-4 py-3">Size</th>
                                <th className="text-left px-4 py-3">Color</th>
                                <th className="text-left px-4 py-3">Gender</th>
                                <th className="text-left px-4 py-3">Purchase $USD</th>
                                <th className="text-left px-4 py-3">Sale $MXN</th>
                                <th className="text-left px-4 py-3">Status</th>
                                <th className="text-left px-4 py-3">Actions</th>
                            </tr>
                        </thead>
                        <tbody className="divide-y divide-brand-gray/10">
                            {loading ? (
                                <tr>
                                    <td colSpan={10} className="text-center py-8 text-brand-gray">
                                        Loading...
                                    </td>
                                </tr>
                            ) : items.length === 0 ? (
                                <tr>
                                    <td colSpan={10} className="text-center py-8 text-brand-gray">
                                        No items found
                                    </td>
                                </tr>
                            ) : items.map(item => (
                                <tr key={item.id} className="text-brand-smoke hover:bg-brand-deep/40
                                              transition-colors">
                                    {/* Image */}
                                    <td className="px-4 py-2">
                                        {item.imageUrl ? (
                                            <img
                                                src={`http://localhost:8080${item.imageUrl}`}
                                                alt={item.product}
                                                className="w-12 h-12 object-cover rounded-lg"
                                                onError={e => e.target.style.display = 'none'}
                                            />
                                        ) : (
                                            <div className="w-12 h-12 bg-brand-deep rounded-lg" />
                                        )}
                                    </td>

                                    {/* SKU */}
                                    <td className="px-4 py-2 font-mono text-brand-accent text-xs">
                                        {item.sku}
                                    </td>

                                    {/* Product */}
                                    <td className="px-4 py-2 max-w-48 truncate">{item.product}</td>

                                    {/* Size */}
                                    <td className="px-4 py-2">{item.size}</td>

                                    {/* Color */}
                                    <td className="px-4 py-2">{item.color}</td>

                                    {/* Gender */}
                                    <td className="px-4 py-2">
                                        <span className={`px-2 py-0.5 rounded text-xs font-semibold ${item.gender === 'F' ? 'bg-pink-500/20 text-pink-400' :
                                            item.gender === 'M' ? 'bg-blue-500/20 text-blue-400' :
                                                'bg-brand-gray/20 text-brand-gray'
                                            }`}>
                                            {item.gender}
                                        </span>
                                    </td>

                                    {/* Purchase price */}
                                    <td className="px-4 py-2">${item.purchasePriceUsd}</td>

                                    {/* Sale price */}
                                    <td className="px-4 py-2">
                                        <SalePriceCell item={item} onUpdate={fetchItems} />
                                    </td>

                                    {/* Status */}
                                    <td className="px-4 py-2">
                                        <span className={`px-2 py-0.5 rounded text-xs font-semibold ${item.status === 'Stock'
                                            ? 'bg-green-500/20 text-green-400'
                                            : 'bg-red-500/20 text-red-400'
                                            }`}>
                                            {item.status}
                                        </span>
                                    </td>

                                    {/* Actions */}
                                    <td className="px-4 py-2">
                                        <button
                                            onClick={() => toggleStatus(item)}
                                            className={`text-xs px-3 py-1 rounded-lg font-semibold transition-colors ${item.status === 'Stock'
                                                ? 'bg-red-500/20 text-red-400 hover:bg-red-500/30'
                                                : 'bg-green-500/20 text-green-400 hover:bg-green-500/30'
                                                }`}
                                        >
                                            {item.status === 'Stock' ? 'Mark Sold' : 'Restore'}
                                        </button>
                                    </td>
                                </tr>
                            ))}
                        </tbody>
                    </table>
                </div>

                {/* Count */}
                {!loading && (
                    <div className="px-4 py-3 border-t border-brand-gray/20 text-brand-gray text-xs">
                        {items.length} items
                    </div>
                )}
            </div>
        </div>
    )
}

// Inline editable sale price
function SalePriceCell({ item, onUpdate }) {
    const [editing, setEditing] = useState(false)
    const [value, setValue] = useState(item.salePriceMxn ?? '')
    const [saving, setSaving] = useState(false)

    const save = async () => {
        if (!value) return setEditing(false)
        setSaving(true)
        await api.patch(`/inventory/${item.id}/price`, { price: value })
        setSaving(false)
        setEditing(false)
        onUpdate()
    }

    if (editing) {
        return (
            <div className="flex items-center gap-1">
                <input
                    type="number"
                    value={value}
                    onChange={e => setValue(e.target.value)}
                    className="w-20 bg-brand-deep text-brand-smoke border border-brand-accent
                     rounded px-2 py-0.5 text-xs focus:outline-none"
                    autoFocus
                    onKeyDown={e => e.key === 'Enter' && save()}
                />
                <button onClick={save} disabled={saving}
                    className="text-green-400 text-xs hover:text-green-300">
                    {saving ? '...' : '✓'}
                </button>
                <button onClick={() => setEditing(false)}
                    className="text-red-400 text-xs hover:text-red-300">✕</button>
            </div>
        )
    }

    return (
        <button onClick={() => setEditing(true)}
            className="text-left hover:text-brand-accent transition-colors">
            {item.salePriceMxn ? `$${item.salePriceMxn}` : (
                <span className="text-brand-gray/50 text-xs">Set price</span>
            )}
        </button>
    )
}