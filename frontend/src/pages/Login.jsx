import { useState } from 'react'
import { Link, Navigate, useLocation, useNavigate } from 'react-router-dom'
import { motion, AnimatePresence } from 'framer-motion'
import {
  LuShieldCheck, LuUser, LuLock, LuEye, LuEyeOff, LuLogIn, LuTriangleAlert, LuCircleCheck, LuLoaderCircle,
} from 'react-icons/lu'
import { useAuth } from '../AuthContext.jsx'

// Secuencia que narra lo que realmente hace el servidor al iniciar sesion.
const LINEAS = [
  { tipo: 'cmd', t: 'POST /api/auth/login' },
  { tipo: 'res', t: <>verificando con <b>bcrypt.matches()</b> · sin descifrar nada</> },
  { tipo: 'res', t: <>sesión <b>HttpOnly</b> emitida · token <b>CSRF</b> exigido</> },
  { tipo: 'res', t: <>rol resuelto en el servidor: <b>admin | usuario | invitado</b></> },
]

function Terminal() {
  return (
    <motion.div className="terminal" initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }} transition={{ delay: 0.35, duration: 0.6 }}>
      <div className="terminal-barra"><i /><i /><i /><span>auth-node · 01</span></div>
      <div className="terminal-cuerpo">
        {LINEAS.map((l, i) => (
          <motion.div key={i} className={l.tipo} initial={{ opacity: 0, x: -8 }} animate={{ opacity: 1, x: 0 }} transition={{ delay: 0.8 + i * 0.55 }}>
            {l.t}
          </motion.div>
        ))}
        <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} transition={{ delay: 0.8 + LINEAS.length * 0.55 }}>
          <span className="cmd" /><span className="cursor" />
        </motion.div>
      </div>
    </motion.div>
  )
}

export default function Login() {
  const { sesion, login } = useAuth()
  const navegar = useNavigate()
  const { state } = useLocation()
  const [usuario, setUsuario] = useState('')
  const [contrasena, setContrasena] = useState('')
  const [ver, setVer] = useState(false)
  const [error, setError] = useState(null)
  const [enviando, setEnviando] = useState(false)

  if (sesion) return <Navigate to={sesion.redirigirA} replace />

  async function enviar(e) {
    e.preventDefault()
    setError(null); setEnviando(true)
    try {
      const s = await login(usuario, contrasena)
      navegar(s.redirigirA, { replace: true }) // redireccion segun el rol que decide el servidor
    } catch (err) {
      setError(err.message)
    } finally { setEnviando(false) }
  }

  return (
    <div className="auth">
      <motion.div className="hero" initial={{ opacity: 0, x: -30 }} animate={{ opacity: 1, x: 0 }}
                  transition={{ duration: 0.7, ease: [0.22, 1, 0.36, 1] }}>
        <span className="etiqueta">SecureLink / Autenticación</span>
        <h1>Identidad verificada.<br /><em>Nada viaja en claro.</em></h1>
        <p className="lead">Registro, inicio de sesión y control de acceso por roles, con un laboratorio donde ves cómo trabajan los algoritmos criptográficos por dentro.</p>
        <Terminal />
      </motion.div>

      <motion.div className="tarjeta brilla auth-tarjeta" initial={{ opacity: 0, y: 30 }} animate={{ opacity: 1, y: 0 }}
                  transition={{ duration: 0.6, delay: 0.15, ease: [0.22, 1, 0.36, 1] }}>
        <span className="etiqueta">Acceso</span>
        <h2>Iniciar sesión</h2>
        <p className="sub">Ingresa tus credenciales para continuar.</p>

        <AnimatePresence mode="popLayout">
          {state?.mensaje && !error && (
            <motion.div key="ok" className="alerta exito" initial={{ opacity: 0, height: 0 }} animate={{ opacity: 1, height: 'auto' }} exit={{ opacity: 0 }}>
              <LuCircleCheck /> {state.mensaje}
            </motion.div>
          )}
          {error && (
            <motion.div key="err" className="alerta error" initial={{ opacity: 0, x: -12 }}
                        animate={{ opacity: 1, x: [0, -8, 8, -5, 5, 0] }} exit={{ opacity: 0 }} transition={{ duration: 0.45 }}>
              <LuTriangleAlert /> {error}
            </motion.div>
          )}
        </AnimatePresence>

        <form onSubmit={enviar}>
          <div className="campo">
            <label htmlFor="usuario">Usuario</label>
            <div className="entrada">
              <LuUser className="ico" />
              <input id="usuario" value={usuario} autoComplete="username" placeholder="tu_usuario"
                     onChange={(e) => setUsuario(e.target.value)} required />
            </div>
          </div>
          <div className="campo">
            <label htmlFor="contrasena">Contraseña</label>
            <div className="entrada con-ojo">
              <LuLock className="ico" />
              <input id="contrasena" type={ver ? 'text' : 'password'} value={contrasena} placeholder="••••••••"
                     autoComplete="current-password" onChange={(e) => setContrasena(e.target.value)} required />
              <button type="button" className="ojo" onClick={() => setVer(!ver)} aria-label="Mostrar u ocultar contraseña">
                {ver ? <LuEyeOff size={18} /> : <LuEye size={18} />}
              </button>
            </div>
          </div>
          <motion.button className="btn btn-primario btn-bloque" disabled={enviando}>
            {enviando ? <><LuLoaderCircle className="girar" /> Verificando…</> : <><LuLogIn /> Entrar</>}
          </motion.button>
        </form>
        <p className="pie-form">¿No tienes cuenta? <Link to="/registro">Regístrate</Link></p>
        <span className="candado-sello"><LuShieldCheck size={13} /> Contraseñas protegidas con bcrypt</span>
      </motion.div>
    </div>
  )
}
