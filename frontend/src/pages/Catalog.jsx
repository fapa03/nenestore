import { useState } from 'react'
import { BookOpen, Download } from 'lucide-react'

export default function Catalog() {
    const [gender, setGender] = useState('')
    const [status, setStatus] = useState('Stock')
    const [size, setSize] = useState('')
    const [search, setSearch] = useState('')
    const [loading, setLoading] = useState(false)

    const generatePdf = async () => {
        setLoading(true)
        try {
            const params = new URLSearchParams()
            if (gender) params.append('gender', gender)
            if (status) params.append('status', status)
            if (size) params.append('size', size)
            if (search) params.append('search', search)

            const token = localStorage.getItem('token')
            const response = await fetch(
                `http://localhost:8080/api/catalog/pdf?${params.toString()}`,
                { headers: { Authorization: `Bearer ${token}` } }
            )

            if (!response.ok) {
                const err = await response.json()
                alert(err.message || 'Error generating catalog')
                return
            }

            const blob = await response.blob()
            const url = URL.createObjectURL(blob)
            const a = document.createElement('a')
            a.href = url
            a.download = 'nenestore-catalog.pdf'
            a.click()
            URL.revokeObjectURL(url)
        } catch (err) {
            alert('Error generating catalog')
        } finally {
            setLoading(false)
        }
    }

    return (
        <div className="space-y-6">
            <h1 className="text-brand-smoke text-2xl font-bold">Catalog</h1>

            <div className="bg-brand-black rounded-xl p-6 max-w-lg">
                <div className="flex items-center gap-3 mb-6">
                    <BookOpen size={20} className="text-brand-accent" />
                    <h2 className="text-brand-smoke font-semibold">Generate PDF Catalog</h2>
                </div>

                <div className="space-y-4">
                    {/* Gender */}
                    <div>
                        <label className="block text-brand-gray text-xs mb-1">Gender</label>
                        <select
                            value={gender}
                            onChange={e => setGender(e.target.value)}
                            className="w-full bg-brand-deep text-brand-smoke border border-brand-gray/30
                         rounded-lg px-3 py-2 text-sm focus:outline-none
                         focus:border-brand-accent"
                        >
                            <option value="">All</option>
                            <option value="M">Male</option>
                            <option value="F">Female</option>
                            <option value="U">Unisex</option>
                        </select>
                    </div>

                    {/* Status */}
                    <div>
                        <label className="block text-brand-gray text-xs mb-1">Status</label>
                        <select
                            value={status}
                            onChange={e => setStatus(e.target.value)}
                            className="w-full bg-brand-deep text-brand-smoke border border-brand-gray/30
                         rounded-lg px-3 py-2 text-sm focus:outline-none
                         focus:border-brand-accent"
                        >
                            <option value="Stock">Stock only</option>
                            <option value="">All (including sold)</option>
                        </select>
                    </div>

                    {/* Size */}
                    <div>
                        <label className="block text-brand-gray text-xs mb-1">Size</label>
                        <input
                            type="text"
                            value={size}
                            onChange={e => setSize(e.target.value)}
                            placeholder="e.g. L, XL, M"
                            className="w-full bg-brand-deep text-brand-smoke border border-brand-gray/30
                         rounded-lg px-3 py-2 text-sm focus:outline-none
                         focus:border-brand-accent"
                        />
                    </div>

                    {/* Search */}
                    <div>
                        <label className="block text-brand-gray text-xs mb-1">
                            Search (optional)
                        </label>
                        <input
                            type="text"
                            value={search}
                            onChange={e => setSearch(e.target.value)}
                            placeholder="Product name, SKU..."
                            className="w-full bg-brand-deep text-brand-smoke border border-brand-gray/30
                         rounded-lg px-3 py-2 text-sm focus:outline-none
                         focus:border-brand-accent"
                        />
                    </div>

                    <button
                        onClick={generatePdf}
                        disabled={loading}
                        className="w-full flex items-center justify-center gap-2 bg-brand-accent
                       hover:bg-purple-700 text-white py-3 rounded-lg text-sm
                       font-semibold transition-colors disabled:opacity-50 mt-2"
                    >
                        <Download size={16} />
                        {loading ? 'Generating...' : 'Download PDF'}
                    </button>
                </div>
            </div>
        </div>
    )
}