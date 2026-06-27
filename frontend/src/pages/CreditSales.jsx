import { useEffect, useState } from 'react'
import api from '../services/api'
import { CreditCard, Plus } from 'lucide-react'

export default function CreditSales() {
    const [credits, setCredits] = useState([])
    const [loading, setLoading] = useState(true)
    const [selected, setSelected] = useState(null)
    const [paymentAmount, setPaymentAmount] = useState('')
    const [paymentNotes, setPaymentNotes] = useState('')
    const [saving, setSaving] = useState(false)

    const fetchCredits = () => {
        setLoading(true)
        api.get('/sales/credit')
            .then(res => setCredits(res.data))
            .finally(() => setLoading(false))
    }

    useEffect(() => { fetchCredits() }, [])

    const registerPayment = async () => {
        if (!paymentAmount || !selected) return
        setSaving(true)
        try {
            await api.post(`/sales/credit/${selected.creditSaleId}/payments`, {
                amount: parseFloat(paymentAmount),
                notes: paymentNotes
            })
            setSelected(null)
            setPaymentAmount('')
            setPaymentNotes('')
            fetchCredits()
        } catch (err) {
            alert(err.response?.data?.message || 'Error registering payment')
        } finally {
            setSaving(false)
        }
    }

    const statusColor = (status) => {
        if (status === 'PAID') return 'bg-green-500/20 text-green-400'
        if (status === 'PARTIAL') return 'bg-yellow-500/20 text-yellow-400'
        return 'bg-red-500/20 text-red-400'
    }

    return (
        <div className="space-y-6">
            <h1 className="text-brand-smoke text-2xl font-bold">Credit Sales</h1>

            {/* Table */}
            <div className="bg-brand-black rounded-xl overflow-hidden">
                <table className="w-full text-sm">
                    <thead>
                        <tr className="text-brand-gray border-b border-brand-gray/20 text-xs uppercase">
                            <th className="text-left px-4 py-3">Client</th>
                            <th className="text-left px-4 py-3">Date</th>
                            <th className="text-right px-4 py-3">Original Debt</th>
                            <th className="text-right px-4 py-3">Paid</th>
                            <th className="text-right px-4 py-3">Balance</th>
                            <th className="text-left px-4 py-3">Status</th>
                            <th className="text-left px-4 py-3">Action</th>
                        </tr>
                    </thead>
                    <tbody className="divide-y divide-brand-gray/10">
                        {loading ? (
                            <tr>
                                <td colSpan={7} className="text-center py-8 text-brand-gray">
                                    Loading...
                                </td>
                            </tr>
                        ) : credits.length === 0 ? (
                            <tr>
                                <td colSpan={7} className="text-center py-8 text-brand-gray">
                                    No open credit sales
                                </td>
                            </tr>
                        ) : credits.map(credit => (
                            <tr key={credit.creditSaleId}
                                className="text-brand-smoke hover:bg-brand-deep/40 transition-colors">
                                <td className="px-4 py-3">
                                    <p className="font-semibold">{credit.client?.name ?? '—'}</p>
                                    <p className="text-brand-gray text-xs">
                                        {credit.client?.whatsapp ?? ''}
                                    </p>
                                </td>
                                <td className="px-4 py-3 text-brand-gray">{credit.saleDate}</td>
                                <td className="px-4 py-3 text-right">
                                    ${credit.originalDebt?.toFixed(2)}
                                </td>
                                <td className="px-4 py-3 text-right text-green-400">
                                    ${credit.paidAmount?.toFixed(2)}
                                </td>
                                <td className="px-4 py-3 text-right font-bold text-brand-accent">
                                    ${credit.balance?.toFixed(2)}
                                </td>
                                <td className="px-4 py-3">
                                    <span className={`px-2 py-0.5 rounded text-xs font-semibold
                                   ${statusColor(credit.status)}`}>
                                        {credit.status}
                                    </span>
                                </td>
                                <td className="px-4 py-3">
                                    {credit.status !== 'PAID' && (
                                        <button
                                            onClick={() => setSelected(credit)}
                                            className="flex items-center gap-1 text-xs bg-brand-accent/20
                                 text-brand-accent hover:bg-brand-accent/30 px-3 py-1
                                 rounded-lg transition-colors"
                                        >
                                            <Plus size={12} />
                                            Payment
                                        </button>
                                    )}
                                </td>
                            </tr>
                        ))}
                    </tbody>
                </table>
            </div>

            {/* Payment Modal */}
            {selected && (
                <div className="fixed inset-0 bg-black/60 flex items-center justify-center z-50">
                    <div className="bg-brand-black rounded-2xl p-6 w-full max-w-md shadow-2xl">
                        <div className="flex items-center gap-3 mb-6">
                            <CreditCard size={20} className="text-brand-accent" />
                            <h2 className="text-brand-smoke font-semibold text-lg">
                                Register Payment
                            </h2>
                        </div>

                        {/* Summary */}
                        <div className="bg-brand-deep rounded-lg p-4 mb-4 space-y-1 text-sm">
                            <div className="flex justify-between">
                                <span className="text-brand-gray">Client</span>
                                <span className="text-brand-smoke">{selected.client?.name}</span>
                            </div>
                            <div className="flex justify-between">
                                <span className="text-brand-gray">Original debt</span>
                                <span className="text-brand-smoke">
                                    ${selected.originalDebt?.toFixed(2)}
                                </span>
                            </div>
                            <div className="flex justify-between">
                                <span className="text-brand-gray">Already paid</span>
                                <span className="text-green-400">
                                    ${selected.paidAmount?.toFixed(2)}
                                </span>
                            </div>
                            <div className="flex justify-between font-bold">
                                <span className="text-brand-gray">Remaining</span>
                                <span className="text-brand-accent">
                                    ${selected.balance?.toFixed(2)}
                                </span>
                            </div>
                        </div>

                        {/* Amount input */}
                        <div className="space-y-3">
                            <div>
                                <label className="block text-brand-gray text-sm mb-1">
                                    Payment amount (MXN)
                                </label>
                                <input
                                    type="number"
                                    value={paymentAmount}
                                    onChange={e => setPaymentAmount(e.target.value)}
                                    placeholder="0.00"
                                    className="w-full bg-brand-deep text-brand-smoke border
                             border-brand-gray/30 rounded-lg px-4 py-2.5 text-sm
                             focus:outline-none focus:border-brand-accent"
                                    autoFocus
                                />
                            </div>
                            <div>
                                <label className="block text-brand-gray text-sm mb-1">
                                    Notes (optional)
                                </label>
                                <input
                                    type="text"
                                    value={paymentNotes}
                                    onChange={e => setPaymentNotes(e.target.value)}
                                    placeholder="e.g. bank transfer"
                                    className="w-full bg-brand-deep text-brand-smoke border
                             border-brand-gray/30 rounded-lg px-4 py-2.5 text-sm
                             focus:outline-none focus:border-brand-accent"
                                />
                            </div>
                        </div>

                        {/* Buttons */}
                        <div className="flex gap-3 mt-6">
                            <button
                                onClick={() => {
                                    setSelected(null)
                                    setPaymentAmount('')
                                    setPaymentNotes('')
                                }}
                                className="flex-1 border border-brand-gray/30 text-brand-gray
                           hover:text-brand-smoke py-2.5 rounded-lg text-sm
                           transition-colors"
                            >
                                Cancel
                            </button>
                            <button
                                onClick={registerPayment}
                                disabled={saving || !paymentAmount}
                                className="flex-1 bg-brand-accent hover:bg-purple-700 text-white
                           py-2.5 rounded-lg text-sm font-semibold transition-colors
                           disabled:opacity-50"
                            >
                                {saving ? 'Saving...' : 'Confirm Payment'}
                            </button>
                        </div>
                    </div>
                </div>
            )}
        </div>
    )
}