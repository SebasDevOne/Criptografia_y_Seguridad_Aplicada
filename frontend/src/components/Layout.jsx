import { Link, NavLink, Outlet, useLocation, useNavigate } from 'react-router-dom'
import { motion } from 'framer-motion'
import { LuShieldCheck, LuLayoutDashboard, LuFlaskConical, LuLogOut } from 'react-icons/lu'
import { useAuth } from '../AuthContext.jsx'
import Particulas from './Particulas.jsx'
import { COLOR_ROL } from './Piezas.jsx'

export default function Layout() {
  const { sesion, logout } = useAuth()
  const navegar = useNavigate()
  const { pathname } = useLocation()
  const esAuth = pathname === '/login' || pathname === '/registro'

  async function salir() {
    await logout()
    navegar('/login', { replace: true, state: { mensaje: 'Sesión cerrada correctamente.' } })
  }

  return (
    <div className="app">
      <Particulas intensa={esAuth} />
      <motion.header className="barra" initial={{ y: -64, opacity: 0 }} animate={{ y: 0, opacity: 1 }}
                     transition={{ duration: 0.5, ease: [0.22, 1, 0.36, 1] }}>
        <div className="contenedor barra-in">
          <Link className="marca" to="/">
            <span className="marca-logo"><LuShieldCheck size={20} /></span>
            <span>Secure<b>Link</b></span>
          </Link>
          {sesion && (
            <nav className="nav">
              <NavLink to={sesion.redirigirA} end><LuLayoutDashboard /><span className="txt">Mi panel</span></NavLink>
              <NavLink to="/laboratorio"><LuFlaskConical /><span className="txt">Laboratorio</span></NavLink>
              <span className="sep" />
              <span className="chip-usuario" style={{ '--rol': COLOR_ROL[sesion.rol] }}>
                <span className="avatar">{sesion.usuario[0].toUpperCase()}</span>
                <span className="txt">{sesion.usuario}</span>
                <span className={'insignia ' + sesion.rol}>{sesion.rol}</span>
              </span>
              <button className="btn btn-fantasma btn-peq" onClick={salir}><LuLogOut /> Salir</button>
            </nav>
          )}
        </div>
      </motion.header>

      <main className="contenedor">
        <motion.div key={pathname} initial={{ opacity: 0, y: 14 }} animate={{ opacity: 1, y: 0 }}
                    transition={{ duration: 0.4, ease: [0.22, 1, 0.36, 1] }}>
          <Outlet />
        </motion.div>
      </main>
      <footer className="pie">SecureLink · Criptografía y Seguridad Aplicada · UNIMINUTO</footer>
    </div>
  )
}
