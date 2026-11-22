import { useEffect, useState } from 'react'
import { api } from '../api.js'

export default function Admin() {
  const [panel, setPanel] = useState(null)
  const [error, setError] = useState(null)
  useEffect(() => { api('/admin/panel').then(setPanel).catch((e) => setError(e.message)) }, [])

  if (error) return <div className="alert alert-danger">{error}</div>
  if (!panel) return <p>Cargando…</p>
  return (
    <>
      <h1 className="h3">Panel de administración</h1>
      <p className="text-muted">Nivel 3: ves todo, incluidos los hashes completos y la auditoría.</p>

      <h2 className="h5 mt-4">Usuarios (hash bcrypt descompuesto)</h2>
      <div className="table-responsive">
        <table className="table table-sm align-middle">
          <thead><tr><th>Usuario</th><th>Rol</th><th>Versión</th><th>Costo</th><th>Sal (22)</th><th>Digest (31)</th></tr></thead>
          <tbody>
            {panel.usuarios.map((u) => (
              <tr key={u.id}>
                <td>{u.usuario}</td><td><span className="badge text-bg-secondary">{u.rol}</span></td>
                <td>{u.estructura.version}</td><td>{u.estructura.costo}</td>
                <td><code>{u.estructura.sal}</code></td><td><code>{u.estructura.digest}</code></td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      <h2 className="h5 mt-4">Auditoría (últimos eventos)</h2>
      <div className="table-responsive" style={{ maxHeight: 360, overflow: 'auto' }}>
        <table className="table table-sm table-striped">
          <thead><tr><th>Fecha (UTC)</th><th>Evento</th><th>Usuario</th><th>Detalle</th><th>IP</th></tr></thead>
          <tbody>
            {panel.auditoria.map((a) => (
              <tr key={a.id}>
                <td>{a.fecha}</td><td>{a.evento}</td><td>{a.usuario || a.intentado || '—'}</td>
                <td>{[a.algoritmo, a.detalle].filter(Boolean).join(' · ')}</td><td>{a.ip}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </>
  )
}
