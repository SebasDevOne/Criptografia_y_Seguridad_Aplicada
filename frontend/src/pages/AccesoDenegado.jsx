import { Link } from 'react-router-dom'

export default function AccesoDenegado() {
  return (
    <div className="alert alert-danger">
      <h1 className="h4">403 · Acceso denegado</h1>
      <p>Tu rol no tiene permiso para entrar a esta página.</p>
      <Link to="/" className="btn btn-outline-danger btn-sm">Volver a mi panel</Link>
    </div>
  )
}
