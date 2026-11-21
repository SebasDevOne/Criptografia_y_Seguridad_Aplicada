import { Navigate } from 'react-router-dom'
import { useAuth } from '../AuthContext.jsx'

// Sin sesion -> login. Con sesion pero rol distinto -> acceso denegado.
// Es solo comodidad de UI: el servidor vuelve a validar sesion y rol en cada peticion a /api.
export default function RutaProtegida({ rol, children }) {
  const { sesion, cargando } = useAuth()
  if (cargando) return <p className="text-center mt-5">Cargando…</p>
  if (!sesion) return <Navigate to="/login" replace />
  if (rol && sesion.rol !== rol) return <Navigate to="/acceso-denegado" replace />
  return children
}
