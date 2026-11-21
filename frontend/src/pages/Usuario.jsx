import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { api } from '../api.js'

export default function Usuario() {
  const [panel, setPanel] = useState(null)
  const [error, setError] = useState(null)
  useEffect(() => { api('/usuario/panel').then(setPanel).catch((e) => setError(e.message)) }, [])

  if (error) return <div className="alert alert-danger">{error}</div>
  if (!panel) return <p>Cargando…</p>
  const h = panel.miHash
  return (
    <>
      <h1 className="h3">Panel de usuario</h1>
      <p className="text-muted">Nivel 2: ves la estructura de tu hash, pero la sal y el digest salen parciales.</p>
      <div className="card mb-4"><div className="card-body">
        <h2 className="h6">Tu hash almacenado ({h.algoritmo})</h2>
        <dl className="row mb-0">
          <dt className="col-sm-3">Versión</dt><dd className="col-sm-9">{h.version}</dd>
          <dt className="col-sm-3">Costo</dt><dd className="col-sm-9">{h.costo} (2^{h.costo} iteraciones)</dd>
          <dt className="col-sm-3">Sal</dt><dd className="col-sm-9"><code>{h.sal}</code></dd>
          <dt className="col-sm-3">Digest</dt><dd className="col-sm-9"><code>{h.digest}</code></dd>
        </dl>
      </div></div>
      <Link className="btn btn-primary" to="/laboratorio">Ir al laboratorio criptográfico</Link>
      <h2 className="h5 mt-4">Mis últimas operaciones</h2>
      <ul className="list-group">
        {panel.historial.length === 0 && <li className="list-group-item text-muted">Aún no has usado el laboratorio.</li>}
        {panel.historial.map((o) => (
          <li key={o.id} className="list-group-item">{o.fecha} — {o.algoritmo} · {o.detalle}</li>
        ))}
      </ul>
    </>
  )
}
