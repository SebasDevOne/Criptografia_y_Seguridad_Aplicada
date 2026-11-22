import { motion } from 'framer-motion'

export const COLOR_ROL = { admin: 'var(--rol-admin)', usuario: 'var(--rol-usuario)', invitado: 'var(--rol-invitado)' }

// Contenedor que escalona la entrada de sus hijos <Item>.
const contenedor = { oculto: {}, visible: { transition: { staggerChildren: 0.07, delayChildren: 0.05 } } }
const item = {
  oculto: { opacity: 0, y: 18 },
  visible: { opacity: 1, y: 0, transition: { duration: 0.45, ease: [0.22, 1, 0.36, 1] } },
}

export function Escalonado({ children, className = '', ...resto }) {
  return <motion.div className={className} variants={contenedor} initial="oculto" animate="visible" {...resto}>{children}</motion.div>
}

export function Item({ children, className = '', ...resto }) {
  return <motion.div className={className} variants={item} {...resto}>{children}</motion.div>
}

export function Tarjeta({ children, className = '', brilla = false, ...resto }) {
  return (
    <motion.section className={'tarjeta ' + (brilla ? 'brilla ' : '') + className} variants={item}
                    whileHover={{ y: -2 }} transition={{ duration: 0.2 }} {...resto}>
      {children}
    </motion.section>
  )
}

export function Encabezado({ icono: Icono, titulo, subtitulo, rol, children }) {
  return (
    <Item className="encabezado" style={rol ? { '--rol': COLOR_ROL[rol] } : undefined}>
      <div className="encabezado-ico"><Icono /></div>
      <div>
        <h1>{titulo}</h1>
        <p>{subtitulo}</p>
      </div>
      <div className="espacio" />
      {children}
    </Item>
  )
}

export function Insignia({ tipo, children }) {
  return <span className={'insignia ' + tipo}>{children}</span>
}

export function Medidor({ nivel }) {
  return (
    <span className="medidor" title={'Nivel de detalle ' + nivel + ' de 3'}>
      {[1, 2, 3].map((n) => <i key={n} className={n <= nivel ? 'on' : ''} />)}
    </span>
  )
}
