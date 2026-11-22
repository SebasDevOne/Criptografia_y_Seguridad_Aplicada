import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { LuUser, LuFingerprint, LuFlaskConical, LuHistory, LuArrowRight, LuLoaderCircle, LuCpu } from 'react-icons/lu'
import { api } from '../api.js'
import { Escalonado, Tarjeta, Encabezado, Insignia } from '../components/Piezas.jsx'

export default function Usuario() {
  const [panel, setPanel] = useState(null)
  const [error, setError] = useState(null)
  useEffect(() => { api('/usuario/panel').then(setPanel).catch((e) => setError(e.message)) }, [])

  if (error) return <div className="alerta error">{error}</div>
  if (!panel) return <div className="vacio"><LuLoaderCircle className="girar" /></div>
  const h = panel.miHash
  return (
    <Escalonado>
      <Encabezado icono={LuUser} rol="usuario" titulo={'Hola, ' + panel.usuario}
                  subtitulo="Nivel 2 · ves la estructura de tu hash, con la sal y el digest parciales.">
        <Link to="/laboratorio" className="btn btn-primario">Ir al laboratorio <LuArrowRight /></Link>
      </Encabezado>

      <Tarjeta brilla className="t-az">
        <h2><LuFingerprint /> Así se guarda tu contraseña ({h.algoritmo})</h2>
        <div className="hash-lin">
          <span className="seg version">{h.version}</span>$<span className="seg costo">{h.costo}</span>$
          <span className="seg sal">{h.sal}</span> <span className="seg digest">{h.digest}</span>
        </div>
        <div className="leyenda">
          <span><i style={{ background: '#8b78ff' }} />Versión</span>
          <span><i style={{ background: 'var(--ambar)' }} />Costo: 2<sup>{h.costo}</sup> = {2 ** h.costo} iteraciones</span>
          <span><i style={{ background: 'var(--azul)' }} />Sal aleatoria (parcial para tu rol)</span>
          <span><i style={{ background: 'var(--azul-vivo)' }} />Digest (parcial para tu rol)</span>
        </div>
      </Tarjeta>

      <Tarjeta className="mt t-am">
        <h2><LuHistory /> Mis últimas operaciones</h2>
        {panel.historial.length === 0 ? (
          <p style={{ color: 'var(--suave)', margin: 0 }}>Aún no has usado el laboratorio.</p>
        ) : (
          <ul className="lista-hist">
            {panel.historial.map((o) => (
              <li key={o.id}><LuCpu /><b>{o.algoritmo}</b> <Insignia tipo="usuario">{o.detalle}</Insignia><span className="fecha">{o.fecha}</span></li>
            ))}
          </ul>
        )}
      </Tarjeta>
      <p style={{ color: 'var(--tenue)', fontSize: '.85rem', marginTop: 18 }}><LuFlaskConical style={{ verticalAlign: '-2px' }} /> Sube a nivel 3 para ver la sal y el digest completos.</p>
    </Escalonado>
  )
}
