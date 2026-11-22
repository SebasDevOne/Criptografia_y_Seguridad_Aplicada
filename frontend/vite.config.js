import { defineConfig } from "vite"
import react from "@vitejs/plugin-react"

// En desarrollo /api se reenvia al backend (mismo origen: cookies de sesion y CSRF funcionan).
export default defineConfig({
  plugins: [react()],
  server: { port: 5173, proxy: { "/api": "http://localhost:8080" } },
})
