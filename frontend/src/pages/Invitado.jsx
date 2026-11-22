import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { LuEye, LuLock, LuArrowRight, LuLoaderCircle, LuCpu, LuBookOpen } from 'react-icons/lu'
import { api } from '../api.js'
import { Escalonado, Tarjeta, Encabezado } from '../components/Piezas.jsx'

export default function Invitado() {
  const [panel, setPanel] = useState(null)
  const [error, setError] = useState(null)
  useEffect(() => { api('/invitado/panel').then(setPanel).catch((e) => setError(e.message)) }, [])

  if (error) return <div className="alerta error">{error}</div>
  if (!panel) return <div className="vacio"><LuLoaderCircle className="girar" /></div>
  return (
    <Escalonado>
      <Encabezado icono={LuEye} rol="invitado" titulo={panel.mensaje}
                  subtitulo="Nivel 1 · solo ves resultados finales y resúmenes.">
        <Link to="/laboratorio" className="btn btn-primario">Probar el laboratorio <LuArrowRight /></Link>
      </Encabezado>
      <div className="rejilla-2">
        <Tarjeta brilla className="t-vi">
          <h2><LuCpu /> Algoritmos bloqueados</h2>
          <ul className="bloqueados">{panel.algoritmosBloqueados.map((a) => <li key={a}><LuLock /> {a}</li>)}</ul>
        </Tarjeta>
        <Tarjeta brilla className="t-am">
          <h2><LuBookOpen /> Explicaciones ocultas</h2>
          <ul className="bloqueados">{panel.seccionesBloqueadas.map((s) => <li key={s}><LuLock /> {s}</li>)}</ul>
        </Tarjeta>
      </div>
    </Escalonado>
  )
}
