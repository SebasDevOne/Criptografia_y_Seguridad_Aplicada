import { useState } from 'react'
import { Link, Navigate, useLocation, useNavigate } from 'react-router-dom'
import { useAuth } from '../AuthContext.jsx'

export default function Login() {
  const { sesion, login } = useAuth()
  const navegar = useNavigate()
  const { state } = useLocation()
  const [usuario, setUsuario] = useState('')
  const [contrasena, setContrasena] = useState('')
  const [error, setError] = useState(null)

  if (sesion) return <Navigate to={sesion.redirigirA} replace />

  async function enviar(e) {
    e.preventDefault()
    setError(null)
    try {
      const s = await login(usuario, contrasena)
      navegar(s.redirigirA, { replace: true }) // redireccion segun el rol que decide el servidor
    } catch (err) {
      setError(err.message)
    }
  }

  return (
    <div className="row justify-content-center">
      <div className="col-md-5">
        <div className="card shadow-sm"><div className="card-body">
          <h1 className="h4 mb-3">Iniciar sesión</h1>
          {state?.mensaje && <div className="alert alert-success">{state.mensaje}</div>}
          {error && <div className="alert alert-danger">{error}</div>}
          <form onSubmit={enviar}>
            <div className="mb-3">
              <label className="form-label" htmlFor="usuario">Usuario</label>
              <input id="usuario" className="form-control" value={usuario} autoComplete="username"
                     onChange={(e) => setUsuario(e.target.value)} required />
            </div>
            <div className="mb-3">
              <label className="form-label" htmlFor="contrasena">Contraseña</label>
              <input id="contrasena" type="password" className="form-control" value={contrasena}
                     autoComplete="current-password" onChange={(e) => setContrasena(e.target.value)} required />
            </div>
            <button className="btn btn-primary w-100">Entrar</button>
          </form>
          <p className="mt-3 mb-0 text-center">¿No tienes cuenta? <Link to="/registro">Regístrate</Link></p>
        </div></div>
      </div>
    </div>
  )
}
