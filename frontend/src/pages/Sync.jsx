import { useState } from 'react'
import api from '../services/api'
import { RefreshCw, CheckCircle, AlertCircle } from 'lucide-react'

export default function Sync() {
    const [loading, setLoading] = useState(false)
    const [result, setResult] = useState(null)
    const [error, setError] = useState(null)

    const runSync = async () => {
        setLoading(true)
        setResult(null)
        setError(null)
        try {
            const res = await api.post('/sync/all')
            setResult(res.data)
        } catch (err) {
            setError(err.response?.data?.message || 'Sync failed')
        } finally {
            setLoading(false)
        }
    }

    return (
        <div className="space-y-6">
            <h1 className="text-brand-smoke text-2xl font-bold">Database Update</h1>

            <div className="bg-brand-black rounded-xl p-6 max-w-lg">
                <p className="text-brand-gray text-sm mb-6">
                    Reads new orders from Google Sheets and imports them into the database.
                    Already imported orders are skipped automatically.
                </p>

                <button
                    onClick={runSync}
                    disabled={loading}
                    className="w-full flex items-center justify-center gap-2 bg-brand-accent
                     hover:bg-purple-700 text-white py-3 rounded-lg text-sm
                     font-semibold transition-colors disabled:opacity-50"
                >
                    <RefreshCw size={16} className={loading ? 'animate-spin' : ''} />
                    {loading ? 'Syncing...' : 'Sync Now'}
                </button>

                {/* Result */}
                {result && (
                    <div className={`mt-4 rounded-lg p-4 flex items-start gap-3 ${result.status === 'SUCCESS' || result.status === 'UP_TO_DATE'
                        ? 'bg-green-500/10 border border-green-500/20'
                        : 'bg-red-500/10 border border-red-500/20'
                        }`}>
                        <CheckCircle size={18} className="text-green-400 shrink-0 mt-0.5" />
                        <div>
                            <p className="text-green-400 font-semibold text-sm">
                                {result.status === 'UP_TO_DATE'
                                    ? 'Already up to date'
                                    : 'Sync completed'}
                            </p>
                            <p className="text-brand-gray text-xs mt-0.5">
                                {result.itemsProcessed} items processed
                            </p>
                        </div>
                    </div>
                )}

                {error && (
                    <div className="mt-4 rounded-lg p-4 flex items-start gap-3
                          bg-red-500/10 border border-red-500/20">
                        <AlertCircle size={18} className="text-red-400 shrink-0 mt-0.5" />
                        <div>
                            <p className="text-red-400 font-semibold text-sm">Sync failed</p>
                            <p className="text-brand-gray text-xs mt-0.5">{error}</p>
                        </div>
                    </div>
                )}
            </div>
        </div>
    )
}