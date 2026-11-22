import { Link } from 'react-router-dom'
import { motion } from 'framer-motion'
import { LuShieldX, LuArrowLeft } from 'react-icons/lu'

export default function AccesoDenegado() {
  return (
    <motion.div className="tarjeta brilla vacio" style={{ maxWidth: 520, margin: '60px auto' }}
                initial={{ opacity: 0, scale: 0.94 }} animate={{ opacity: 1, scale: 1 }}>
      <motion.div animate={{ rotate: [0, -6, 6, -4, 4, 0] }} transition={{ duration: 0.7, delay: 0.3 }}>
        <LuShieldX style={{ color: 'var(--rojo)', fontSize: '3.4rem' }} />
      </motion.div>
      <h1 style={{ margin: '8px 0' }}>403 · Acceso denegado</h1>
      <p>Tu rol no tiene permiso para entrar a esta página. Este intento queda en la auditoría.</p>
      <Link to="/" className="btn btn-fantasma" style={{ marginTop: 12 }}><LuArrowLeft /> Volver a mi panel</Link>
    </motion.div>
  )
}
