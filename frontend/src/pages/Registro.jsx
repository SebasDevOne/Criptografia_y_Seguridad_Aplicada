import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { motion, AnimatePresence } from 'framer-motion'
import {
  LuShieldCheck, LuUser, LuLock, LuLockKeyhole, LuEye, LuEyeOff, LuUserPlus, LuTriangleAlert, LuLoaderCircle,
} from 'react-icons/lu'
import { api } from '../api.js'

const NIVELES = [
  { txt: 'Muy débil', color: '#ff6b7d' }, { txt: 'Débil', color: '#f5b64a' }, { txt: 'Aceptable', color: '#7a9dff' },
  { txt: 'Fuerte', color: '#4d86ff' }, { txt: 'Muy fuerte', color: '#a9c8ff' },
]

function puntaje(c) {
  let p = 0
  if (c.length >= 8) p++
  if (c.length >= 12) p++
  if (/[a-z]/.test(c) && /[A-Z]/.test(c)) p++
  if (/\d/.test(c)) p++
  if (/[^A-Za-z0-9]/.test(c)) p++
  return Math.min(p, 5)
}

export default function Registro() {
  const navegar = useNavigate()
  const [f, setF] = useState({ usuario: '', contrasena: '', confirmarContrasena: '' })
  const [ver, setVer] = useState(false)
  const [error, setError] = useState(null)
  const [enviando, setEnviando] = useState(false)
  const cambiar = (k) => (e) => setF({ ...f, [k]: e.target.value })
  const p = puntaje(f.contrasena)

  async function enviar(e) {
    e.preventDefault()
    setError(null); setEnviando(true)
    try {
      const r = await api('/auth/registro', { metodo: 'POST', cuerpo: f })
      navegar('/login', { state: { mensaje: r.mensaje } })
    } catch (err) {
      setError(err.message)
    } finally { setEnviando(false) }
  }

  return (
    <div className="auth" style={{ gridTemplateColumns: '1fr', justifyItems: 'center' }}>
      <motion.div className="tarjeta brilla auth-tarjeta" style={{ width: 'min(460px, 100%)' }}
                  initial={{ opacity: 0, y: 30, scale: 0.97 }} animate={{ opacity: 1, y: 0, scale: 1 }}
                  transition={{ duration: 0.6, ease: [0.22, 1, 0.36, 1] }}>
        <div className="marca-logo" style={{ marginBottom: 16, width: 46, height: 46 }}><LuShieldCheck size={24} /></div>
        <h2>Crear cuenta</h2>
        <p className="sub">Tu cuenta se crea con rol <b>usuario</b>. Tu contraseña se guarda con bcrypt.</p>

        <AnimatePresence>
          {error && (
            <motion.div className="alerta error" initial={{ opacity: 0, x: -12 }}
                        animate={{ opacity: 1, x: [0, -8, 8, -5, 5, 0] }} exit={{ opacity: 0 }} transition={{ duration: 0.45 }}>
              <LuTriangleAlert /> {error}
            </motion.div>
          )}
        </AnimatePresence>

        <form onSubmit={enviar}>
          <div className="campo">
            <label htmlFor="u">Usuario</label>
            <div className="entrada"><LuUser className="ico" />
              <input id="u" value={f.usuario} onChange={cambiar('usuario')} placeholder="3 a 50 caracteres" required /></div>
          </div>
          <div className="campo">
            <label htmlFor="c">Contraseña</label>
            <div className="entrada con-ojo"><LuLock className="ico" />
              <input id="c" type={ver ? 'text' : 'password'} value={f.contrasena} onChange={cambiar('contrasena')}
                     autoComplete="new-password" placeholder="Mínimo 8 caracteres" required />
              <button type="button" className="ojo" onClick={() => setVer(!ver)} aria-label="Mostrar u ocultar contraseña">
                {ver ? <LuEyeOff size={18} /> : <LuEye size={18} />}
              </button>
            </div>
            {f.contrasena && (
              <>
                <div className="fuerza">
                  {[1, 2, 3, 4, 5].map((n) => <span key={n} style={n <= p ? { background: NIVELES[p - 1].color } : undefined} />)}
                </div>
                <div className="fuerza-txt">Seguridad: {p ? NIVELES[p - 1].txt : 'Muy débil'}</div>
              </>
            )}
          </div>
          <div className="campo">
            <label htmlFor="cc">Confirmar contraseña</label>
            <div className="entrada"><LuLockKeyhole className="ico" />
              <input id="cc" type={ver ? 'text' : 'password'} value={f.confirmarContrasena} onChange={cambiar('confirmarContrasena')}
                     autoComplete="new-password" placeholder="Repite la contraseña" required /></div>
          </div>
          <motion.button className="btn btn-primario btn-bloque" disabled={enviando} whileTap={{ scale: 0.98 }}>
            {enviando ? <><LuLoaderCircle className="girar" /> Creando…</> : <><LuUserPlus /> Registrarme</>}
          </motion.button>
        </form>
        <p className="pie-form">¿Ya tienes cuenta? <Link to="/login">Inicia sesión</Link></p>
      </motion.div>
    </div>
  )
}
