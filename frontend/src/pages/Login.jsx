import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'

export default function Login() {
    const { login } = useAuth()
    const navigate = useNavigate()
    const [username, setUsername] = useState('')
    const [password, setPassword] = useState('')
    const [error, setError] = useState('')
    const [loading, setLoading] = useState(false)

    const handleSubmit = async (e) => {
        e.preventDefault()
        setError('')
        setLoading(true)
        try {
            await login(username, password)
            navigate('/')
        } catch (err) {
            setError('Invalid username or password')
        } finally {
            setLoading(false)
        }
    }

    return (
        <div className="min-h-screen bg-brand-deep flex items-center justify-center">
            <div className="w-full max-w-sm">

                {/* Logo */}
                <div className="text-center mb-8">
                    <h1 className="text-4xl font-bold text-brand-accent">Nenestore</h1>
                    <p className="text-brand-gray mt-2 text-sm">Internal Operations Platform</p>
                </div>

                {/* Card */}
                <div className="bg-brand-black rounded-2xl p-8 shadow-2xl">
                    <h2 className="text-brand-smoke text-xl font-semibold mb-6">Sign in</h2>

                    <form onSubmit={handleSubmit} className="space-y-4">
                        <div>
                            <label className="block text-brand-gray text-sm mb-1">
                                Username
                            </label>
                            <input
                                type="text"
                                value={username}
                                onChange={(e) => setUsername(e.target.value)}
                                className="w-full bg-brand-deep text-brand-smoke border border-brand-gray
                           rounded-lg px-4 py-2.5 text-sm focus:outline-none
                           focus:border-brand-accent transition-colors"
                                placeholder="antonio"
                                required
                            />
                        </div>

                        <div>
                            <label className="block text-brand-gray text-sm mb-1">
                                Password
                            </label>
                            <input
                                type="password"
                                value={password}
                                onChange={(e) => setPassword(e.target.value)}
                                className="w-full bg-brand-deep text-brand-smoke border border-brand-gray
                           rounded-lg px-4 py-2.5 text-sm focus:outline-none
                           focus:border-brand-accent transition-colors"
                                placeholder="••••••••"
                                required
                            />
                        </div>

                        {error && (
                            <p className="text-red-400 text-sm">{error}</p>
                        )}

                        <button
                            type="submit"
                            disabled={loading}
                            className="w-full bg-brand-accent hover:bg-purple-700 text-white
                         font-semibold py-2.5 rounded-lg transition-colors
                         disabled:opacity-50 disabled:cursor-not-allowed mt-2"
                        >
                            {loading ? 'Signing in...' : 'Sign in'}
                        </button>
                    </form>
                </div>
            </div>
        </div>
    )
}