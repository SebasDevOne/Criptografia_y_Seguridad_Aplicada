import { Fragment, useEffect, useState } from 'react'
import { motion, AnimatePresence } from 'framer-motion'
import {
  LuFlaskConical, LuLock, LuTriangleAlert, LuPlay, LuCopy, LuCheck, LuLoaderCircle, LuTimer, LuEyeOff,
  LuBinary, LuHash, LuKeyRound, LuShieldCheck, LuLockKeyhole, LuMousePointerClick, LuBookOpen, LuWorkflow,
  LuLogIn, LuLogOut,
} from 'react-icons/lu'
import { api } from '../api.js'
import { useAuth } from '../AuthContext.jsx'
import { Escalonado, Encabezado, Tarjeta, Insignia, Medidor } from '../components/Piezas.jsx'

const ICONO_CAT = {
  'Codificación': LuBinary, 'Hash y MAC': LuHash, 'Contraseñas': LuKeyRound,
  'Cifrado simétrico': LuLockKeyhole, 'Cifrado asimétrico': LuShieldCheck,
}
const NECESITA_CLAVE = ['HMAC_SHA256', 'RC4', 'AES_CBC', 'AES_GCM', 'CHACHA20']
const NIVEL_ROL = { invitado: 1, usuario: 2, admin: 3 }

// Flujo visual: Entrada -> pasos del algoritmo -> Salida. Cada nodo se puede abrir para ver su detalle.
function Flujo({ resp, entrada, operacion }) {
  const nodos = [
    { clave: 'in', tipo: 'entrada', n: 'Entrada', t: operacion, v: entrada, icono: LuLogIn },
    ...resp.pasos.map((p, i) => ({ clave: 'p' + i, tipo: p.visibilidad, n: 'Paso ' + (i + 1), t: p.titulo, v: p.valor, icono: null })),
    { clave: 'out', tipo: 'salida', n: 'Salida', t: 'Resultado final', v: resp.resultado, icono: LuLogOut },
  ]
  const [sel, setSel] = useState('out')
  const activo = nodos.find((x) => x.clave === sel) || nodos[nodos.length - 1]

  return (
    <>
      <div className="flujo-wrap">
        <div className="flujo">
          {nodos.map((nd, i) => (
            <Fragment key={nd.clave}>
              {i > 0 && <motion.div className="enlace" initial={{ opacity: 0, scaleX: 0 }} animate={{ opacity: 1, scaleX: 1 }}
                                      style={{ originX: 0 }} transition={{ delay: i * 0.16 }}><i style={{ animationDelay: i * 0.25 + 's' }} /></motion.div>}
              <motion.button className={'nodo ' + nd.tipo + (sel === nd.clave ? ' activo' : '')} onClick={() => setSel(nd.clave)}
                             initial={{ opacity: 0, y: 14, scale: 0.94 }} animate={{ opacity: 1, y: 0, scale: 1 }}
                             transition={{ delay: i * 0.16, duration: 0.4 }}>
                <div className="n">{nd.icono ? <nd.icono /> : nd.tipo === 'OCULTO' ? <LuEyeOff /> : null}{nd.n}</div>
                <div className="t">{nd.tipo === 'OCULTO' ? 'Oculto para tu rol' : nd.t}</div>
                {nd.v && nd.tipo !== 'OCULTO' && <div className="v">{nd.v}</div>}
              </motion.button>
            </Fragment>
          ))}
        </div>
      </div>

      <AnimatePresence mode="wait">
        <motion.div key={activo.clave} className="detalle-nodo" initial={{ opacity: 0, y: 8 }} animate={{ opacity: 1, y: 0 }} exit={{ opacity: 0 }}
                    transition={{ duration: 0.2 }}>
          <h4>
            {activo.n}: {activo.tipo === 'OCULTO' ? 'Oculto para tu rol' : activo.t}
            {['COMPLETO', 'PARCIAL', 'OCULTO'].includes(activo.tipo) && <Insignia tipo={activo.tipo}>{activo.tipo.toLowerCase()}</Insignia>}
          </h4>
          {activo.v ? <code>{activo.v}</code> : <span style={{ color: 'var(--tenue)', fontSize: '.85rem' }}>Sube de rol para ver este detalle.</span>}
        </motion.div>
      </AnimatePresence>

      <div className="leyenda-flujo">
        <span><i style={{ background: '#5b92ff' }} />Visible completo</span>
        <span><i style={{ background: 'var(--ambar)' }} />Visible parcial</span>
        <span><i style={{ background: 'var(--violeta)' }} />Oculto para tu rol</span>
      </div>
    </>
  )
}

export default function Laboratorio() {
  const { sesion } = useAuth()
  const [lista, setLista] = useState([])
  const [cat, setCat] = useState(null)
  const [codigo, setCodigo] = useState(null)
  const [detalle, setDetalle] = useState(null)
  const [form, setForm] = useState({ operacion: '', texto: '', clave: '', referencia: '' })
  const [resp, setResp] = useState(null)
  const [enviado, setEnviado] = useState(null)
  const [error, setError] = useState(null)
  const [ejecutando, setEjecutando] = useState(false)
  const [copiado, setCopiado] = useState(false)

  useEffect(() => {
    api('/didactica/algoritmos').then((l) => { setLista(l); setCat(l[0]?.categoria) }).catch((e) => setError(e.message))
  }, [])

  async function elegir(a) {
    setCodigo(a.codigo); setResp(null); setError(null)
    setForm((f) => ({ ...f, operacion: a.operaciones[0] }))
    setDetalle(await api('/didactica/algoritmos/' + a.codigo))
  }

  async function ejecutar(e) {
    e.preventDefault(); setError(null); setResp(null); setEjecutando(true)
    try {
      const r = await api('/didactica/operar', { metodo: 'POST', cuerpo: { algoritmo: codigo, ...form } })
      setEnviado({ texto: form.texto, operacion: form.operacion })
      setResp(r)
    } catch (err) { setError(err.message) } finally { setEjecutando(false) }
  }

  async function copiar() {
    try { await navigator.clipboard.writeText(resp.resultado); setCopiado(true); setTimeout(() => setCopiado(false), 1500) } catch { /* sin permiso */ }
  }

  const alg = detalle?.algoritmo
  const cambiar = (k) => (e) => setForm({ ...form, [k]: e.target.value })
  const categorias = [...new Set(lista.map((a) => a.categoria))]
  const nivel = NIVEL_ROL[sesion.rol]

  return (
    <Escalonado>
      <Encabezado icono={LuFlaskConical} rol={sesion.rol} titulo="Laboratorio criptográfico"
                  subtitulo="Elige un algoritmo, pruébalo y sigue el recorrido del dato paso a paso.">
        <Medidor nivel={nivel} /><Insignia tipo={sesion.rol}>{sesion.rol} · nivel {nivel}</Insignia>
      </Encabezado>

      <Tarjeta className="lab-sel t-az">
        <h2><LuWorkflow /> 1 · Elige un algoritmo</h2>
        <div className="cats">
          {categorias.map((c) => {
            const Ico = ICONO_CAT[c] || LuHash
            return <button key={c} className={'cat' + (cat === c ? ' sel' : '')} onClick={() => setCat(c)}><Ico /> {c}</button>
          })}
        </div>
        <div className="chips">
          {lista.filter((a) => a.categoria === cat).map((a) => (
            <button key={a.codigo} className={'chip' + (codigo === a.codigo ? ' sel' : '')} onClick={() => elegir(a)}>
              {a.nombre}
              {!a.seguro && <LuTriangleAlert className="mal" title="Obsoleto" />}
              {!a.puedeEjecutar && <LuLock className="bloq" title="Requiere más nivel" />}
            </button>
          ))}
        </div>
      </Tarjeta>

      <AnimatePresence mode="wait">
        {!alg ? (
          <motion.div key="vacio" className="tarjeta vacio" initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }}>
            <LuMousePointerClick /><p>Selecciona un algoritmo para ver cómo funciona y probarlo.</p>
          </motion.div>
        ) : (
          <motion.div key={alg.codigo} initial={{ opacity: 0, y: 16 }} animate={{ opacity: 1, y: 0 }} exit={{ opacity: 0 }} transition={{ duration: 0.35 }}>
            <div className="lab-cols">
              <div className="tarjeta t-vi">
                <h2><LuBookOpen /> 2 · Entiende {alg.nombre}
                  {!alg.seguro && <Insignia tipo="peligro"><LuTriangleAlert /> obsoleto</Insignia>}
                </h2>
                <div className="saber">
                  {detalle.secciones.map((s, i) => (
                    <motion.div key={s.codigo} className={'seccion' + (s.bloqueada ? ' bloq' : '')}
                                initial={{ opacity: 0, x: -10 }} animate={{ opacity: 1, x: 0 }} transition={{ delay: i * 0.07 }}>
                      <h3>{s.nombre}</h3>
                      <p>{s.bloqueada ? <><LuLock /> Reservado: requiere nivel {s.nivelRequerido}.</> : s.contenido}</p>
                    </motion.div>
                  ))}
                </div>
              </div>

              <div className="tarjeta t-am">
                <h2><LuPlay /> 3 · Pruébalo</h2>
                {!alg.puedeEjecutar ? (
                  <div className="alerta aviso"><LuLock /> Tu rol no puede ejecutar este algoritmo (requiere nivel {alg.nivelMinimo}).</div>
                ) : (
                  <form onSubmit={ejecutar}>
                    <div className="form-fila">
                      <select value={form.operacion} onChange={cambiar('operacion')}>
                        {alg.operaciones.map((o) => <option key={o}>{o}</option>)}
                      </select>
                      <input placeholder="Texto de entrada" value={form.texto} onChange={cambiar('texto')} required />
                    </div>
                    {NECESITA_CLAVE.includes(alg.codigo) && (
                      <div className="campo"><input placeholder="Clave o frase secreta" value={form.clave} onChange={cambiar('clave')} /></div>
                    )}
                    {form.operacion === 'VERIFICAR' && (
                      <div className="campo"><input placeholder="Valor de referencia (hash a comparar)" value={form.referencia} onChange={cambiar('referencia')} /></div>
                    )}
                    <button className="btn btn-primario" disabled={ejecutando}>
                      {ejecutando ? <><LuLoaderCircle className="girar" /> Ejecutando…</> : <><LuPlay /> Ejecutar</>}
                    </button>
                  </form>
                )}
                <AnimatePresence>
                  {error && <motion.div className="alerta error" style={{ marginTop: 16, marginBottom: 0 }} initial={{ opacity: 0, y: -6 }} animate={{ opacity: 1, y: 0 }} exit={{ opacity: 0 }}><LuTriangleAlert /> {error}</motion.div>}
                </AnimatePresence>
                <AnimatePresence>
                  {resp && (
                    <motion.div style={{ marginTop: 18 }} initial={{ opacity: 0, y: 12 }} animate={{ opacity: 1, y: 0 }} exit={{ opacity: 0 }}>
                      {resp.aviso && <div className="alerta aviso"><LuTriangleAlert /> {resp.aviso}</div>}
                      <div className="resultado">
                        {resp.resultado}
                        <button className="btn btn-fantasma btn-peq copiar" onClick={copiar} aria-label="Copiar">{copiado ? <LuCheck /> : <LuCopy />}</button>
                      </div>
                      {resp.tiempoMicros != null && <div style={{ marginTop: 10 }}><Insignia tipo="gris"><LuTimer /> {(resp.tiempoMicros / 1000).toFixed(1)} ms</Insignia></div>}
                    </motion.div>
                  )}
                </AnimatePresence>
              </div>
            </div>

            <AnimatePresence>
              {resp && (
                <motion.div className="tarjeta t-az mt" initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }} exit={{ opacity: 0 }}>
                  <h2><LuWorkflow /> 4 · Recorrido del dato
                    {resp.pasosOcultos > 0 && <Insignia tipo="OCULTO"><LuLock /> {resp.pasosOcultos} oculto(s) para tu rol</Insignia>}
                  </h2>
                  <Flujo key={resp.resultado + resp.pasos.length} resp={resp} entrada={enviado.texto} operacion={enviado.operacion} />
                </motion.div>
              )}
            </AnimatePresence>
          </motion.div>
        )}
      </AnimatePresence>
    </Escalonado>
  )
}
