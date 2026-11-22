import { useEffect, useMemo, useState } from 'react'
import Particles, { initParticlesEngine } from '@tsparticles/react'
import { loadSlim } from '@tsparticles/slim'

// Red de nodos conectados: sugiere una red cifrada. "intensa" la hace mas visible (login).
export default function Particulas({ intensa = false }) {
  const [listo, setListo] = useState(false)

  useEffect(() => {
    initParticlesEngine(async (motor) => { await loadSlim(motor) }).then(() => setListo(true))
  }, [])

  const opciones = useMemo(() => ({
    fullScreen: { enable: true, zIndex: 0 },
    background: { color: 'transparent' },
    fpsLimit: 60,
    detectRetina: true,
    interactivity: {
      events: { onHover: { enable: true, mode: 'grab' }, resize: { enable: true } },
      modes: { grab: { distance: 160, links: { opacity: 0.6 } } },
    },
    particles: {
      number: { value: intensa ? 90 : 45, density: { enable: true, area: 900 } },
      color: { value: ['#4d86ff', '#2f6bff', '#a9c8ff'] },
      links: { enable: true, distance: 140, color: '#4d86ff', opacity: intensa ? 0.35 : 0.16, width: 1 },
      move: { enable: true, speed: intensa ? 0.7 : 0.4, outModes: { default: 'bounce' } },
      opacity: { value: { min: 0.25, max: intensa ? 0.8 : 0.5 }, animation: { enable: true, speed: 0.6, sync: false } },
      size: { value: { min: 1, max: intensa ? 3 : 2 } },
      shape: { type: ['circle', 'square'] },
    },
  }), [intensa])

  if (!listo) return null
  return <Particles id="particulas" options={opciones} />
}
