import { useEffect, useState } from 'react'
import api from '../services/api'
import { Search, Plus, X } from 'lucide-react'

const LEVELS = ['NEW', 'REGULAR', 'VIP', 'WHOLESALE']

const levelColor = (level) => {
    if (level === 'VIP') return 'bg-yellow-500/20 text-yellow-400'
    if (level === 'WHOLESALE') return 'bg-blue-500/20 text-blue-400'
    if (level === 'REGULAR') return 'bg-green-500/20 text-green-400'
    return 'bg-brand-gray/20 text-brand-gray'
}

const emptyForm = {
    name: '', whatsapp: '', email: '', instagram: '', level: 'NEW', notes: ''
}

export default function Clients() {
    const [clients, setClients] = useState([])
    const [loading, setLoading] = useState(true)
    const [search, setSearch] = useState('')
    const [showForm, setShowForm] = useState(false)
    const [form, setForm] = useState(emptyForm)
    const [saving, setSaving] = useState(false)
    const [editing, setEditing] = useState(null)

    const fetchClients = (q = '') => {
        setLoading(true)
        const params = q ? { search: q } : {}
        api.get('/clients', { params })
            .then(res => setClients(res.data))
            .finally(() => setLoading(false))
    }

    useEffect(() => { fetchClients() }, [])

    const handleSearch = (e) => {
        e.preventDefault()
        fetchClients(search)
    }

    const openCreate = () => {
        setEditing(null)
        setForm(emptyForm)
        setShowForm(true)
    }

    const openEdit = (client) => {
        setEditing(client.id)
        setForm({
            name: client.name ?? '',
            whatsapp: client.whatsapp ?? '',
            email: client.email ?? '',
            instagram: client.instagram ?? '',
            level: client.level ?? 'NEW',
            notes: client.notes ?? ''
        })
        setShowForm(true)
    }

    const handleSave = async () => {
        if (!form.name.trim()) return
        setSaving(true)
        try {
            if (editing) {
                await api.put(`/clients/${editing}`, form)
            } else {
                await api.post('/clients', form)
            }
            setShowForm(false)
            setForm(emptyForm)
            setEditing(null)
            fetchClients(search)
        } catch (err) {
            alert(err.response?.data?.message || 'Error saving client')
        } finally {
            setSaving(false)
        }
    }

    return (
        <div className="space-y-6">
            <div className="flex items-center justify-between">
                <h1 className="text-brand-smoke text-2xl font-bold">Clients</h1>
                <button
                    onClick={openCreate}
                    className="flex items-center gap-2 bg-brand-accent hover:bg-purple-700
                     text-white px-4 py-2 rounded-lg text-sm font-semibold
                     transition-colors"
                >
                    <Plus size={16} />
                    New Client
                </button>
            </div>

            {/* Search */}
            <form onSubmit={handleSearch} className="flex gap-3">
                <div className="relative flex-1 max-w-sm">
                    <Search size={14} className="absolute left-3 top-3 text-brand-gray" />
                    <input
                        type="text"
                        placeholder="Search by name, WhatsApp, Instagram..."
                        value={search}
                        onChange={e => setSearch(e.target.value)}
                        className="w-full bg-brand-black text-brand-smoke border border-brand-gray/30
                       rounded-lg pl-8 pr-3 py-2 text-sm focus:outline-none
                       focus:border-brand-accent"
                    />
                </div>
                <button type="submit"
                    className="bg-brand-black border border-brand-gray/30 text-brand-gray
                     hover:text-brand-smoke px-4 py-2 rounded-lg text-sm transition-colors">
                    Search
                </button>
            </form>

            {/* Table */}
            <div className="bg-brand-black rounded-xl overflow-hidden">
                <table className="w-full text-sm">
                    <thead>
                        <tr className="text-brand-gray border-b border-brand-gray/20 text-xs uppercase">
                            <th className="text-left px-4 py-3">Name</th>
                            <th className="text-left px-4 py-3">WhatsApp</th>
                            <th className="text-left px-4 py-3">Instagram</th>
                            <th className="text-left px-4 py-3">Level</th>
                            <th className="text-left px-4 py-3">Notes</th>
                            <th className="text-left px-4 py-3">Actions</th>
                        </tr>
                    </thead>
                    <tbody className="divide-y divide-brand-gray/10">
                        {loading ? (
                            <tr>
                                <td colSpan={6} className="text-center py-8 text-brand-gray">
                                    Loading...
                                </td>
                            </tr>
                        ) : clients.length === 0 ? (
                            <tr>
                                <td colSpan={6} className="text-center py-8 text-brand-gray">
                                    No clients found
                                </td>
                            </tr>
                        ) : clients.map(client => (
                            <tr key={client.id}
                                className="text-brand-smoke hover:bg-brand-deep/40 transition-colors">
                                <td className="px-4 py-3 font-semibold">{client.name}</td>
                                <td className="px-4 py-3 text-brand-gray">{client.whatsapp ?? '—'}</td>
                                <td className="px-4 py-3">
                                    {client.instagram ? (
                                        <a
                                            href={`https://www.instagram.com/${client.instagram.replace('@', '')}`}
                                            target="_blank"
                                            rel="noopener noreferrer"
                                            className="text-brand-accent hover:underline"
                                        >
                                            {client.instagram}
                                        </a>
                                    ) : '—'}
                                </td>
                                <td className="px-4 py-3">
                                    <span className={`px-2 py-0.5 rounded text-xs font-semibold
                                   ${levelColor(client.level)}`}>
                                        {client.level}
                                    </span>
                                </td>
                                <td className="px-4 py-3 text-brand-gray text-xs max-w-xs truncate">
                                    {client.notes ?? '—'}
                                </td>
                                <td className="px-4 py-3">
                                    <button
                                        onClick={() => openEdit(client)}
                                        className="text-xs bg-brand-accent/20 text-brand-accent
                               hover:bg-brand-accent/30 px-3 py-1 rounded-lg
                               transition-colors"
                                    >
                                        Edit
                                    </button>
                                </td>
                            </tr>
                        ))}
                    </tbody>
                </table>

                {!loading && (
                    <div className="px-4 py-3 border-t border-brand-gray/20 text-brand-gray text-xs">
                        {clients.length} clients
                    </div>
                )}
            </div>

            {/* Create/Edit Modal */}
            {
                showForm && (
                    <div className="fixed inset-0 bg-black/60 flex items-center justify-center z-50">
                        <div className="bg-brand-black rounded-2xl p-6 w-full max-w-md shadow-2xl">
                            <div className="flex items-center justify-between mb-6">
                                <h2 className="text-brand-smoke font-semibold text-lg">
                                    {editing ? 'Edit Client' : 'New Client'}
                                </h2>
                                <button onClick={() => setShowForm(false)}
                                    className="text-brand-gray hover:text-brand-smoke">
                                    <X size={18} />
                                </button>
                            </div>

                            <div className="space-y-3">
                                {[
                                    { label: 'Name *', key: 'name', placeholder: 'Maria Garcia' },
                                    { label: 'WhatsApp', key: 'whatsapp', placeholder: '5215512345678' },
                                    { label: 'Email', key: 'email', placeholder: 'maria@email.com' },
                                    { label: 'Instagram', key: 'instagram', placeholder: '@mariagarcia' },
                                ].map(({ label, key, placeholder }) => (
                                    <div key={key}>
                                        <label className="block text-brand-gray text-xs mb-1">{label}</label>
                                        <input
                                            type="text"
                                            value={form[key]}
                                            onChange={e => setForm({ ...form, [key]: e.target.value })}
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
                                        value={form.level}
                                        onChange={e => setForm({ ...form, level: e.target.value })}
                                        className="w-full bg-brand-deep text-brand-smoke border
                             border-brand-gray/30 rounded-lg px-3 py-2 text-sm
                             focus:outline-none focus:border-brand-accent"
                                    >
                                        {LEVELS.map(l => <option key={l} value={l}>{l}</option>)}
                                    </select>
                                </div>

                                <div>
                                    <label className="block text-brand-gray text-xs mb-1">Notes</label>
                                    <textarea
                                        value={form.notes}
                                        onChange={e => setForm({ ...form, notes: e.target.value })}
                                        placeholder="Any relevant notes..."
                                        rows={2}
                                        className="w-full bg-brand-deep text-brand-smoke border
                             border-brand-gray/30 rounded-lg px-3 py-2 text-sm
                             focus:outline-none focus:border-brand-accent resize-none"
                                    />
                                </div>
                            </div>

                            <div className="flex gap-3 mt-6">
                                <button
                                    onClick={() => setShowForm(false)}
                                    className="flex-1 border border-brand-gray/30 text-brand-gray
                           hover:text-brand-smoke py-2.5 rounded-lg text-sm
                           transition-colors"
                                >
                                    Cancel
                                </button>
                                <button
                                    onClick={handleSave}
                                    disabled={saving || !form.name.trim()}
                                    className="flex-1 bg-brand-accent hover:bg-purple-700 text-white
                           py-2.5 rounded-lg text-sm font-semibold transition-colors
                           disabled:opacity-50"
                                >
                                    {saving ? 'Saving...' : editing ? 'Update' : 'Create'}
                                </button>
                            </div>
                        </div>
                    </div>
                )}
        </div >
    )
}