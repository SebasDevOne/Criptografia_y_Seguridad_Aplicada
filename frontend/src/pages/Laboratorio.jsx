import { useEffect, useState } from 'react'
import { api } from '../api.js'
import { useAuth } from '../AuthContext.jsx'

const ETIQUETA = { COMPLETO: 'success', PARCIAL: 'warning', OCULTO: 'secondary' }
const NECESITA_CLAVE = ['HMAC_SHA256', 'RC4', 'AES_CBC', 'AES_GCM', 'CHACHA20']

export default function Laboratorio() {
  const { sesion } = useAuth()
  const [lista, setLista] = useState([])
  const [codigo, setCodigo] = useState(null)
  const [detalle, setDetalle] = useState(null)
  const [form, setForm] = useState({ operacion: '', texto: '', clave: '', referencia: '' })
  const [resp, setResp] = useState(null)
  const [error, setError] = useState(null)

  useEffect(() => { api('/didactica/algoritmos').then(setLista).catch((e) => setError(e.message)) }, [])

  async function elegir(a) {
    setCodigo(a.codigo); setResp(null); setError(null)
    setForm((f) => ({ ...f, operacion: a.operaciones[0] }))
    setDetalle(await api('/didactica/algoritmos/' + a.codigo))
  }

  async function ejecutar(e) {
    e.preventDefault(); setError(null); setResp(null)
    try {
      setResp(await api('/didactica/operar', { metodo: 'POST', cuerpo: { algoritmo: codigo, ...form } }))
    } catch (err) { setError(err.message) }
  }

  const alg = detalle?.algoritmo
  const cambiar = (k) => (e) => setForm({ ...form, [k]: e.target.value })

  return (
    <>
      <h1 className="h3">Laboratorio criptográfico</h1>
      <p className="text-muted">
        Tu rol es <strong>{sesion.rol}</strong>: cuanto más alto el nivel, más del funcionamiento interno ves.
        <span className="badge text-bg-success ms-2">completo</span>
        <span className="badge text-bg-warning ms-1">parcial</span>
        <span className="badge text-bg-secondary ms-1">oculto</span>
      </p>
      <div className="row g-4">
        <div className="col-md-4">
          <div className="list-group">
            {lista.map((a) => (
              <button key={a.codigo} onClick={() => elegir(a)}
                      className={'list-group-item list-group-item-action d-flex justify-content-between ' + (codigo === a.codigo ? 'active' : '')}>
                <span>{a.nombre}<br /><small className="opacity-75">{a.categoria}</small></span>
                <span>{!a.seguro && <span title="Inseguro">⚠️</span>}{!a.puedeEjecutar && <span title="Requiere más nivel"> 🔒</span>}</span>
              </button>
            ))}
          </div>
        </div>

        <div className="col-md-8">
          {!alg && <div className="alert alert-info">Elige un algoritmo de la lista.</div>}
          {alg && (
            <>
              <h2 className="h5">{alg.nombre} {!alg.seguro && <span className="badge text-bg-danger">obsoleto</span>}</h2>
              {detalle.secciones.map((s) => (
                <div key={s.codigo} className="mb-2">
                  <strong>{s.nombre}: </strong>
                  {s.bloqueada ? <span className="text-muted">🔒 Requiere nivel {s.nivelRequerido}</span> : s.contenido}
                </div>
              ))}

              <hr />
              {!alg.puedeEjecutar && (
                <div className="alert alert-warning">Tu rol no puede ejecutar este algoritmo (requiere nivel {alg.nivelMinimo}).</div>
              )}
              {alg.puedeEjecutar && (
                <form onSubmit={ejecutar} className="mb-3">
                  <div className="row g-2">
                    <div className="col-sm-4">
                      <select className="form-select" value={form.operacion} onChange={cambiar('operacion')}>
                        {alg.operaciones.map((o) => <option key={o}>{o}</option>)}
                      </select>
                    </div>
                    <div className="col-sm-8">
                      <input className="form-control" placeholder="Texto de entrada" value={form.texto}
                             onChange={cambiar('texto')} required />
                    </div>
                    {NECESITA_CLAVE.includes(alg.codigo) && (
                      <div className="col-12"><input className="form-control" placeholder="Clave o frase secreta"
                             value={form.clave} onChange={cambiar('clave')} /></div>
                    )}
                    {form.operacion === 'VERIFICAR' && (
                      <div className="col-12"><input className="form-control" placeholder="Valor de referencia (hash a comparar)"
                             value={form.referencia} onChange={cambiar('referencia')} /></div>
                    )}
                    <div className="col-12"><button className="btn btn-primary">Ejecutar</button></div>
                  </div>
                </form>
              )}
              {error && <div className="alert alert-danger">{error}</div>}
              {resp && (
                <div className="card"><div className="card-body">
                  {resp.aviso && <div className="alert alert-warning py-1">{resp.aviso}</div>}
                  <h3 className="h6">Resultado</h3>
                  <pre className="bg-light p-2 border rounded" style={{ whiteSpace: 'pre-wrap', wordBreak: 'break-all' }}>{resp.resultado}</pre>
                  {resp.tiempoMicros != null && <small className="text-muted">Tiempo: {resp.tiempoMicros} µs</small>}
                  <h3 className="h6 mt-3">Cómo se obtuvo</h3>
                  <ul className="list-group">
                    {resp.pasos.map((p, i) => (
                      <li key={i} className="list-group-item">
                        <span className={'badge me-2 text-bg-' + ETIQUETA[p.visibilidad]}>{p.visibilidad.toLowerCase()}</span>
                        <strong>{p.titulo}</strong>
                        {p.valor && <div><code style={{ wordBreak: 'break-all' }}>{p.valor}</code></div>}
                      </li>
                    ))}
                  </ul>
                  {resp.pasosOcultos > 0 && <small className="text-muted">🔒 {resp.pasosOcultos} detalle(s) oculto(s) para tu rol.</small>}
                </div></div>
              )}
            </>
          )}
        </div>
      </div>
    </>
  )
}
