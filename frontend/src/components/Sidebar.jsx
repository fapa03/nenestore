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
import { useTheme } from '../context/ThemeContext'
import { Sun, Moon } from 'lucide-react'




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
    const { dark, toggle } = useTheme()
    const navigate = useNavigate()

    const handleLogout = () => {
        logout()
        navigate('/login')
    }

    return (
        <aside className="w-56 min-h-screen bg-white dark:bg-brand-black flex flex-col
                  border-r border-slate-200 dark:border-brand-gray/20 shrink-0">

            {/* Brand */}
            <div className="px-4 py-4 border-b border-brand-gray/20">
                <img
                    src="/LOGOTIPO_NENE.png"
                    alt="Nenestore"
                    className="w-full object-contain max-h-16"
                />
                <p className="text-brand-gray text-xs mt-2 px-1">{user}</p>
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
                                : 'text-slate-600 dark:text-brand-gray hover:text-slate-900 dark:hover:text-brand-smoke hover:bg-slate-100 dark:hover:bg-brand-deep'

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
                {/* Theme toggle */}
                <button
                    onClick={toggle}
                    className="flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm
                                text-slate-500 dark:text-brand-gray hover:text-red-400 
                                hover:bg-slate-100 dark:hover:bg-brand-deep
                                transition-colors w-full"
                >
                    {dark ? <Sun size={16} /> : <Moon size={16} />}
                    {dark ? 'Light mode' : 'Dark mode'}
                </button>



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