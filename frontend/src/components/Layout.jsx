import { Link, Outlet, useNavigate } from 'react-router-dom'
import { useAuth } from '../AuthContext.jsx'

export default function Layout() {
  const { sesion, logout } = useAuth()
  const navegar = useNavigate()

  async function salir() {
    await logout()
    navegar('/login', { replace: true, state: { mensaje: 'Sesión cerrada correctamente.' } })
  }

  return (
    <>
      <nav className="navbar navbar-dark bg-dark mb-4">
        <div className="container">
          <Link className="navbar-brand" to="/">🔐 SecureLink</Link>
          {sesion && (
            <div className="d-flex align-items-center gap-3 text-light">
              <Link className="text-light" to={sesion.redirigirA}>Mi panel</Link>
              <Link className="text-light" to="/laboratorio">Laboratorio</Link>
              <span className="badge text-bg-info">{sesion.usuario} · {sesion.rol}</span>
              <button className="btn btn-outline-light btn-sm" onClick={salir}>Cerrar sesión</button>
            </div>
          )}
        </div>
      </nav>
      <main className="container pb-5"><Outlet /></main>
    </>
  )
}
