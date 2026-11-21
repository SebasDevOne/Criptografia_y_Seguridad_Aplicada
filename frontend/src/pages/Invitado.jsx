import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { api } from '../api.js'

export default function Invitado() {
  const [panel, setPanel] = useState(null)
  const [error, setError] = useState(null)
  useEffect(() => { api('/invitado/panel').then(setPanel).catch((e) => setError(e.message)) }, [])

  if (error) return <div className="alert alert-danger">{error}</div>
  if (!panel) return <p>Cargando…</p>
  return (
    <>
      <h1 className="h3">{panel.mensaje}</h1>
      <p className="text-muted">Nivel 1: solo ves resultados finales y resúmenes.</p>
      <Link className="btn btn-primary mb-4" to="/laboratorio">Probar el laboratorio</Link>
      <div className="row">
        <div className="col-md-6">
          <h2 className="h6">🔒 Algoritmos que no puedes ejecutar</h2>
          <ul>{panel.algoritmosBloqueados.map((a) => <li key={a}>{a}</li>)}</ul>
        </div>
        <div className="col-md-6">
          <h2 className="h6">🔒 Secciones de explicación ocultas</h2>
          <ul>{panel.seccionesBloqueadas.map((s) => <li key={s}>{s}</li>)}</ul>
        </div>
      </div>
    </>
  )
}
