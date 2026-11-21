import { Navigate, Route, Routes } from "react-router-dom"
import { useAuth } from "./AuthContext.jsx"
import Layout from "./components/Layout.jsx"
import RutaProtegida from "./components/RutaProtegida.jsx"
import Login from "./pages/Login.jsx"
import Registro from "./pages/Registro.jsx"
import Admin from "./pages/Admin.jsx"
import Usuario from "./pages/Usuario.jsx"
import Invitado from "./pages/Invitado.jsx"
import Laboratorio from "./pages/Laboratorio.jsx"
import AccesoDenegado from "./pages/AccesoDenegado.jsx"

function Inicio() {
  const { sesion, cargando } = useAuth()
  if (cargando) return null
  return <Navigate to={sesion ? sesion.redirigirA : "/login"} replace />
}

export default function App() {
  return (
    <Routes>
      <Route element={<Layout />}>
        <Route path="/" element={<Inicio />} />
        <Route path="/login" element={<Login />} />
        <Route path="/registro" element={<Registro />} />
        <Route path="/acceso-denegado" element={<AccesoDenegado />} />
        <Route path="/admin" element={<RutaProtegida rol="admin"><Admin /></RutaProtegida>} />
        <Route path="/usuario" element={<RutaProtegida rol="usuario"><Usuario /></RutaProtegida>} />
        <Route path="/invitado" element={<RutaProtegida rol="invitado"><Invitado /></RutaProtegida>} />
        <Route path="/laboratorio" element={<RutaProtegida><Laboratorio /></RutaProtegida>} />
        <Route path="*" element={<Navigate to="/" replace />} />
      </Route>
    </Routes>
  )
}
