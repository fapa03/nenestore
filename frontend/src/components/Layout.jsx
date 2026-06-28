import { Outlet } from 'react-router-dom'
import Sidebar from './Sidebar'

export default function Layout() {
    return (
        <div className="flex min-h-screen bg-brand-deep dark:bg-brand-deep">
            <Sidebar />
            <main className="flex-1 p-8 overflow-auto bg-slate-100 dark:bg-brand-deep">
                <Outlet />
            </main>
        </div>
    )
}

