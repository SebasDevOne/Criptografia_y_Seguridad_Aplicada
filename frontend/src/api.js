// Cliente HTTP: envia la cookie de sesion y el token CSRF (cookie XSRF-TOKEN -> cabecera X-XSRF-TOKEN).
function leerCookie(nombre) {
  const par = document.cookie.split('; ').find((c) => c.startsWith(nombre + '='))
  return par ? decodeURIComponent(par.split('=')[1]) : null
}

export class ErrorApi extends Error {
  constructor(estado, mensaje) {
    super(mensaje)
    this.estado = estado
  }
}

export async function api(ruta, { metodo = 'GET', cuerpo } = {}) {
  const cabeceras = { Accept: 'application/json' }
  if (cuerpo !== undefined) cabeceras['Content-Type'] = 'application/json'
  if (metodo !== 'GET') {
    const token = leerCookie('XSRF-TOKEN')
    if (token) cabeceras['X-XSRF-TOKEN'] = token
  }
  const res = await fetch('/api' + ruta, {
    method: metodo,
    headers: cabeceras,
    credentials: 'same-origin',
    body: cuerpo !== undefined ? JSON.stringify(cuerpo) : undefined,
  })
  let datos = null
  try { datos = await res.json() } catch { /* sin cuerpo */ }
  if (!res.ok) throw new ErrorApi(res.status, datos?.error || 'Error inesperado (' + res.status + ')')
  return datos
}
