import { useEffect, useState } from 'react'
import { LuCrown, LuUsers, LuScrollText, LuShieldAlert, LuFingerprint, LuActivity, LuLoaderCircle } from 'react-icons/lu'
import { api } from '../api.js'
import { Escalonado, Tarjeta, Encabezado, Insignia, Item } from '../components/Piezas.jsx'

const TIPO_EVENTO = { LOGIN_OK: 'ok', LOGIN_FALLIDO: 'peligro', ACCESO_DENEGADO: 'peligro', OPERACION_CRIPTO: 'usuario', REGISTRO: 'aviso', LOGOUT: 'gris' }

export default function Admin() {
  const [panel, setPanel] = useState(null)
  const [error, setError] = useState(null)
  useEffect(() => { api('/admin/panel').then(setPanel).catch((e) => setError(e.message)) }, [])

  if (error) return <div className="alerta error">{error}</div>
  if (!panel) return <div className="vacio"><LuLoaderCircle className="girar" /></div>

  const fallos = panel.auditoria.filter((a) => a.evento === 'LOGIN_FALLIDO' || a.evento === 'ACCESO_DENEGADO').length
  return (
    <Escalonado>
      <Encabezado icono={LuCrown} rol="admin" titulo="Panel de administración"
                  subtitulo="Nivel 3 · ves todo: hashes completos, claves y auditoría.">
        <Insignia tipo="admin">Acceso total</Insignia>
      </Encabezado>

      <div className="rejilla-3">
        <Tarjeta className="stat c-az"><div className="ico"><LuUsers /></div><div><div className="num">{panel.usuarios.length}</div><div className="et">Usuarios registrados</div></div></Tarjeta>
        <Tarjeta className="stat c-vi"><div className="ico"><LuActivity /></div><div><div className="num">{panel.auditoria.length}</div><div className="et">Eventos recientes</div></div></Tarjeta>
        <Tarjeta className="stat c-am"><div className="ico"><LuShieldAlert /></div><div><div className="num">{fallos}</div><div className="et">Intentos fallidos / denegados</div></div></Tarjeta>
      </div>

      <Tarjeta className="mt t-az">
        <h2><LuFingerprint /> Usuarios · hash bcrypt descompuesto</h2>
        <div className="tabla-wrap">
          <table>
            <thead><tr><th>Usuario</th><th>Rol</th><th>Versión</th><th>Costo</th><th>Sal (22)</th><th>Digest (31)</th></tr></thead>
            <tbody>
              {panel.usuarios.map((u) => (
                <tr key={u.id}>
                  <td>{u.usuario}</td><td><Insignia tipo={u.rol}>{u.rol}</Insignia></td>
                  <td><code>{u.estructura.version}</code></td><td><code>{u.estructura.costo}</code></td>
                  <td><code>{u.estructura.sal}</code></td><td><code>{u.estructura.digest}</code></td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </Tarjeta>

      <Item>
        <Tarjeta className="mt t-vi">
          <h2><LuScrollText /> Auditoría</h2>
          <div className="tabla-wrap">
            <table>
              <thead><tr><th>Fecha (UTC)</th><th>Evento</th><th>Usuario</th><th>Detalle</th><th>IP</th></tr></thead>
              <tbody>
                {panel.auditoria.map((a) => (
                  <tr key={a.id}>
                    <td className="mono">{a.fecha}</td>
                    <td><Insignia tipo={TIPO_EVENTO[a.evento] || 'gris'}>{a.evento}</Insignia></td>
                    <td>{a.usuario || a.intentado || '—'}</td>
                    <td>{[a.algoritmo, a.detalle].filter(Boolean).join(' · ')}</td>
                    <td className="mono">{a.ip}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </Tarjeta>
      </Item>
    </Escalonado>
  )
}
