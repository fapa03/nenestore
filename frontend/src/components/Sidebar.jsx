import { NavLink, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import {
    LayoutDashboard,
    Package,
    BookOpen,
    RefreshCw,
    ShoppingCart,
    CreditCard,
    Users,
    LogOut
} from 'lucide-react'

const navItems = [
    { to: '/', icon: LayoutDashboard, label: 'Dashboard' },
    { to: '/inventory', icon: Package, label: 'Inventory' },
    { to: '/sales', icon: ShoppingCart, label: 'Sales' },
    { to: '/credit', icon: CreditCard, label: 'Credit Sales' },
    { to: '/clients', icon: Users, label: 'Clients' },
    { to: '/catalog', icon: BookOpen, label: 'Catalog' },
    { to: '/sync', icon: RefreshCw, label: 'DB Update' },
]

export default function Sidebar() {
    const { user, logout } = useAuth()
    const navigate = useNavigate()

    const handleLogout = () => {
        logout()
        navigate('/login')
    }

    return (
        <aside className="w-56 min-h-screen bg-brand-black flex flex-col
                      border-r border-brand-gray/20 shrink-0">

            {/* Brand */}
            <div className="px-6 py-6 border-b border-brand-gray/20">
                <h1 className="text-brand-accent font-bold text-xl">Nenestore</h1>
                <p className="text-brand-gray text-xs mt-0.5">{user}</p>
            </div>

            {/* Nav */}
            <nav className="flex-1 px-3 py-4 space-y-1">
                {navItems.map(({ to, icon: Icon, label }) => (
                    <NavLink
                        key={to}
                        to={to}
                        end={to === '/'}
                        className={({ isActive }) =>
                            `flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm
               transition-colors ${isActive
                                ? 'bg-brand-accent text-white font-semibold'
                                : 'text-brand-gray hover:text-brand-smoke hover:bg-brand-deep'
                            }`
                        }
                    >
                        <Icon size={16} />
                        {label}
                    </NavLink>
                ))}
            </nav>

            {/* Logout */}
            <div className="px-3 py-4 border-t border-brand-gray/20">
                <button
                    onClick={handleLogout}
                    className="flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm
                     text-brand-gray hover:text-red-400 hover:bg-brand-deep
                     transition-colors w-full"
                >
                    <LogOut size={16} />
                    Sign out
                </button>
            </div>
        </aside>
    )
}