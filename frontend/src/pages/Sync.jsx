import { useState, useRef } from 'react'
import { RefreshCw, CheckCircle, AlertCircle, Circle, Loader } from 'lucide-react'

const STEPS = [
    { id: 1, label: 'Connect to Google Sheets' },
    { id: 2, label: 'Scan for new orders' },
    { id: 3, label: 'Process orders' },
    { id: 4, label: 'Download images' },
    { id: 5, label: 'Insert into database' },
]

function StepRow({ step, status, message, detail }) {
    return (
        <div className="flex items-start gap-3 py-2">
            {/* Icon */}
            <div className="mt-0.5 shrink-0">
                {status === 'done' && (
                    <CheckCircle size={16} className="text-green-400" />
                )}
                {status === 'running' && (
                    <Loader size={16} className="text-brand-accent animate-spin" />
                )}
                {status === 'idle' && (
                    <Circle size={16} className="text-brand-gray/40" />
                )}
                {status === 'error' && (
                    <AlertCircle size={16} className="text-red-400" />
                )}
            </div>

            {/* Text */}
            <div className="flex-1 min-w-0">
                <p className={`text-sm ${status === 'done' ? 'text-brand-smoke' :
                        status === 'running' ? 'text-brand-accent font-semibold' :
                            status === 'error' ? 'text-red-400' :
                                'text-brand-gray/50'
                    }`}>
                    {message || step.label}
                </p>
                {detail && (
                    <p className="text-brand-gray text-xs mt-0.5">{detail}</p>
                )}
            </div>
        </div>
    )
}

export default function Sync() {
    const [running, setRunning] = useState(false)
    const [steps, setSteps] = useState([])
    const [complete, setComplete] = useState(null)
    const [error, setError] = useState(null)
    const eventSourceRef = useRef(null)

    const updateStep = (stepData) => {
        setSteps(prev => {
            const existing = prev.findIndex(s =>
                s.step === stepData.step && s.status === 'running'
            )
            if (existing >= 0) {
                const updated = [...prev]
                updated[existing] = stepData
                return updated
            }
            return [...prev, stepData]
        })
    }

    const runSync = () => {
        setRunning(true)
        setSteps([])
        setComplete(null)
        setError(null)

        const token = localStorage.getItem('token')
        const url = `http://localhost:8080/api/sync/stream`

        // SSE doesn't support custom headers natively
        // we pass token as query param for this endpoint
        const es = new EventSource(`${url}?token=${token}`)
        eventSourceRef.current = es

        es.addEventListener('step', (e) => {
            const data = JSON.parse(e.data)
            updateStep(data)
        })

        es.addEventListener('complete', (e) => {
            const data = JSON.parse(e.data)
            setComplete(data)
            setRunning(false)
            es.close()
        })

        es.addEventListener('error', (e) => {
            try {
                const data = JSON.parse(e.data)
                setError(data.message)
            } catch {
                setError('Connection lost')
            }
            setRunning(false)
            es.close()
        })
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
                    disabled={running}
                    className="w-full flex items-center justify-center gap-2 bg-brand-accent
                     hover:bg-purple-700 text-white py-3 rounded-lg text-sm
                     font-semibold transition-colors disabled:opacity-50 mb-6"
                >
                    <RefreshCw size={16} className={running ? 'animate-spin' : ''} />
                    {running ? 'Syncing...' : 'Sync Now'}
                </button>

                {/* Pipeline steps */}
                {steps.length > 0 && (
                    <div className="border-l-2 border-brand-gray/20 pl-4 space-y-1">
                        {steps.map((s, i) => (
                            <StepRow
                                key={i}
                                step={s}
                                status={s.status}
                                message={s.message}
                                detail={s.detail}
                            />
                        ))}
                    </div>
                )}

                {/* Complete */}
                {complete && (
                    <div className={`mt-4 rounded-lg p-4 flex items-start gap-3 ${complete.status === 'SUCCESS' || complete.status === 'UP_TO_DATE'
                            ? 'bg-green-500/10 border border-green-500/20'
                            : 'bg-red-500/10 border border-red-500/20'
                        }`}>
                        <CheckCircle size={18} className="text-green-400 shrink-0 mt-0.5" />
                        <div>
                            <p className="text-green-400 font-semibold text-sm">
                                {complete.status === 'UP_TO_DATE'
                                    ? 'Already up to date'
                                    : 'Sync completed successfully'}
                            </p>
                            <p className="text-brand-gray text-xs mt-0.5">
                                {complete.itemsProcessed} items processed
                            </p>
                        </div>
                    </div>
                )}

                {/* Error */}
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