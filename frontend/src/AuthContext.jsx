import { createContext, useCallback, useContext, useEffect, useState } from 'react'
import { api } from './api.js'

const AuthContext = createContext(null)

export function AuthProvider({ children }) {
  const [sesion, setSesion] = useState(null)
  const [cargando, setCargando] = useState(true)

  // Al cargar la app se pregunta al servidor si hay sesion activa (el servidor es la fuente de verdad).
  useEffect(() => {
    api('/auth/me').then(setSesion).catch(() => setSesion(null)).finally(() => setCargando(false))
  }, [])

  const login = useCallback(async (usuario, contrasena) => {
    const s = await api('/auth/login', { metodo: 'POST', cuerpo: { usuario, contrasena } })
    setSesion(s)
    return s
  }, [])

  const logout = useCallback(async () => {
    try { await api('/auth/logout', { metodo: 'POST' }) } finally { setSesion(null) }
  }, [])

  return <AuthContext.Provider value={{ sesion, cargando, login, logout }}>{children}</AuthContext.Provider>
}

// eslint-disable-next-line react-refresh/only-export-components
export const useAuth = () => useContext(AuthContext)
