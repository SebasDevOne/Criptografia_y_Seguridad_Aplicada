import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { api } from '../api.js'

export default function Registro() {
  const navegar = useNavigate()
  const [f, setF] = useState({ usuario: '', contrasena: '', confirmarContrasena: '' })
  const [error, setError] = useState(null)
  const cambiar = (k) => (e) => setF({ ...f, [k]: e.target.value })

  async function enviar(e) {
    e.preventDefault()
    setError(null)
    try {
      const r = await api('/auth/registro', { metodo: 'POST', cuerpo: f })
      navegar('/login', { state: { mensaje: r.mensaje } })
    } catch (err) {
      setError(err.message)
    }
  }

  return (
    <div className="row justify-content-center">
      <div className="col-md-5">
        <div className="card shadow-sm"><div className="card-body">
          <h1 className="h4 mb-3">Crear cuenta</h1>
          {error && <div className="alert alert-danger">{error}</div>}
          <form onSubmit={enviar}>
            <div className="mb-3">
              <label className="form-label" htmlFor="u">Usuario</label>
              <input id="u" className="form-control" value={f.usuario} onChange={cambiar('usuario')} required />
            </div>
            <div className="mb-3">
              <label className="form-label" htmlFor="c">Contraseña (mínimo 8)</label>
              <input id="c" type="password" className="form-control" value={f.contrasena}
                     onChange={cambiar('contrasena')} autoComplete="new-password" required />
            </div>
            <div className="mb-3">
              <label className="form-label" htmlFor="cc">Confirmar contraseña</label>
              <input id="cc" type="password" className="form-control" value={f.confirmarContrasena}
                     onChange={cambiar('confirmarContrasena')} autoComplete="new-password" required />
            </div>
            <button className="btn btn-success w-100">Registrarme</button>
          </form>
          <p className="mt-3 mb-0 text-center">¿Ya tienes cuenta? <Link to="/login">Inicia sesión</Link></p>
        </div></div>
      </div>
    </div>
  )
}
