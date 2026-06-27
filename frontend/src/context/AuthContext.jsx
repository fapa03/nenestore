import { createContext, useContext, useState } from 'react'
import api from '../services/api'

const AuthContext = createContext(null)

export function AuthProvider({ children }) {
    const [token, setToken] = useState(localStorage.getItem('token'))
    const [user, setUser] = useState(
        localStorage.getItem('username') || null
    )

    const login = async (username, password) => {
        const response = await api.post('/auth/login', { username, password })
        const { token } = response.data
        localStorage.setItem('token', token)
        localStorage.setItem('username', username)
        setToken(token)
        setUser(username)
        return token
    }

    const logout = () => {
        localStorage.removeItem('token')
        localStorage.removeItem('username')
        setToken(null)
        setUser(null)
    }

    return (
        <AuthContext.Provider value={{ token, user, login, logout }}>
            {children}
        </AuthContext.Provider>
    )
}

export function useAuth() {
    return useContext(AuthContext)
}