# Parte III - Registro, autenticación y control de acceso por roles

Taller **Criptografía y Seguridad Aplicada** (UNIMINUTO), empresa ficticia **SECURELINK**.

Estructura del repositorio:

```
Criptograf-a_y_Seguridad_Aplicada/
├── SecureLink/   Backend: API REST con Spring Boot 4 + Spring Security + SQLite
├── frontend/     Frontend: React (Vite) + React Router + Bootstrap
└── Parte_II/     Simulación RC5 y demo RC4
```

## Tecnologías

- **Backend:** Java 17 (probado con JDK 25), Spring Boot 4.1.2, `@RestController`, Spring Security (sesión + CSRF + roles),
  JDBC (`JdbcClient`) sobre **SQLite**, bcrypt (login) y Argon2id/PBKDF2 (laboratorio, con BouncyCastle).
- **Frontend:** React 19, React Router, Bootstrap 5. Vite reenvía `/api` al backend (mismo origen, así funcionan la cookie de sesión y el token CSRF).

## Ejecutar

Backend (terminal 1):

```powershell
cd SecureLink
.\mvnw.cmd spring-boot:run
```

Frontend (terminal 2):

```powershell
cd frontend
npm install
npm run dev
```

Abrir **http://localhost:5173**. Pruebas del backend: `.\mvnw.cmd test`.

La base se crea sola en `SecureLink/securelink.db` (archivo SQLite, ignorado por git). Para empezar de cero, borra el archivo.

## Usuarios iniciales

Se leen de `application.properties` (no están en el código) y en la base solo queda el hash bcrypt. Se pueden cambiar con las variables
`SECURELINK_ADMIN_PASS`, `SECURELINK_USUARIO_PASS`, `SECURELINK_INVITADO_PASS`.

| Usuario | Contraseña | Rol | Ruta |
|---|---|---|---|
| `admin` | `Admin123*` | admin (nivel 3) | `/admin` |
| `usuario` | `Usuario123*` | usuario (nivel 2) | `/usuario` |
| `invitado` | `Invitado123*` | invitado (nivel 1) | `/invitado` |

## API REST

| Método y ruta | Acceso | Descripción |
|---|---|---|
| `POST /api/auth/login` | público | Valida con bcrypt, crea sesión y devuelve `redirigirA` según el rol |
| `POST /api/auth/registro` | público | Crea usuario; el rol siempre es `usuario` (lo fija el servidor) |
| `POST /api/auth/logout` | cualquiera | Destruye la sesión |
| `GET /api/auth/me` | sesión | Usuario actual (401 sin sesión) |
| `GET /api/admin/panel` | admin | Usuarios con hash descompuesto + auditoría |
| `GET /api/usuario/panel` | usuario | Su hash con sal y digest parciales + su historial |
| `GET /api/invitado/panel` | invitado | Lo básico y lo que tiene bloqueado |
| `GET /api/didactica/algoritmos[/{codigo}]` | cualquier rol | Catálogo y explicación (secciones por nivel) |
| `POST /api/didactica/operar` | cualquier rol* | Cifrar, descifrar, hash, verificar, codificar |
| `GET /api/didactica/historial` | cualquier rol | Operaciones (las propias; el admin ve todas) |

\* Cada algoritmo tiene un nivel mínimo de ejecución guardado en la base.

## Base de datos (3FN)

`src/main/resources/db/schema.sql` y `data.sql`. Tablas: `roles`, `usuarios`, `categorias_algoritmo`, `algoritmos`, `operaciones`,
`algoritmo_operaciones` (N:M), `tipos_seccion`, `secciones_algoritmo`, `tipos_evento`, `auditoria`, `claves_demo`.

- **1FN:** valores atómicos, una fila por hecho.
- **2FN:** la única tabla de clave compuesta (`algoritmo_operaciones`) no tiene más columnas.
- **3FN:** los datos que dependían de otro atributo no clave están en su catálogo (rol → nivel y ruta; algoritmo → categoría; evento → descripción).
- Llaves foráneas activas (`foreign_keys=on`). Los textos didácticos, roles, algoritmos y niveles viven en la base, no en el código.

## Laboratorio didáctico: más información según el perfil

| Nivel | Rol | Qué ve |
|---|---|---|
| 1 | invitado | Resultado final y resumen. Solo Base64, SHA-256 y MD5. Pasos internos ocultos. |
| 2 | usuario | Parámetros y pasos; sal, IV y claves aparecen **parciales**. Explicación de "cómo funciona". Tiempo de ejecución. |
| 3 | admin | Todo: sal, IV, clave derivada, texto cifrado en hex, claves RSA, análisis de seguridad e implementación, auditoría. |

Algoritmos: Base64, SHA-256, MD5, HMAC-SHA256, bcrypt, Argon2id, PBKDF2, RC4, AES-CBC, AES-GCM, ChaCha20-Poly1305, RSA-2048 OAEP.
Los cifradores simétricos derivan la clave de una frase con PBKDF2 y devuelven `Base64(sal ‖ IV ‖ cifrado)`, así que se pueden descifrar con la misma frase.
La clave privada RSA de demostración se guarda en `claves_demo` en texto plano: es solo didáctica, nunca hacerlo en producción.

## Análisis frente a los criterios de la profesora

| Criterio | Estado | Dónde |
|---|---|---|
| bcrypt o Argon2id, no SHA-256/512 solos | Cumple (bcrypt) | `SeguridadConfig.passwordEncoder` |
| Registro con usuario y contraseña, hash en BD, sin texto plano | Cumple | `UsuarioService.registrar`; prueba `laBaseGuardaSoloHashBcrypt` |
| Mínimo 3 usuarios con rol (admin, usuario, invitado) | Cumple | `application.properties` + `SemillaService` |
| Login comparando con el hash, sin descifrar | Cumple | `DaoAuthenticationProvider` → `matches()` |
| Impedir acceso con credenciales malas | Cumple (401, mensaje genérico) | `AuthController.login`; `loginFallidoDevuelve401...` |
| Redirección según rol y permisos validados en el servidor en cada petición | Cumple | `redirigirA` + `SeguridadConfig` + `RevalidarUsuarioFilter` |
| Protección de rutas / sin sesión redirige al login | Cumple (API 401, React redirige a `/login`) | `RutaProtegida.jsx`; `rutaProtegidaSinSesionDevuelve401` |
| Cerrar sesión (botón) y volver al login | Cumple | `Layout.jsx`; `trasCerrarSesion...` |
| Pruebas: login ok/fallido, sin sesión, no-admin denegado, tras logout | Cumple (12 pruebas) | `FlujoAutenticacionTest` |
| Interfaz con Bootstrap | Cumple | React + Bootstrap |
| Entrega ZIP con proyecto | Pendiente de ti | Comprimir `SecureLink/` y `frontend/` (sin `node_modules`, `target`, `dist`) |
| Informe PDF con investigación, capturas y pruebas | **Pendiente de ti** | Falta redactarlo y tomar capturas |

**Observaciones honestas:**

- El criterio dice "redirigir al login" sin sesión. En una API REST el servidor responde 401 y es el frontend quien redirige; el comportamiento visible es el mismo.
- El informe en PDF y las capturas de pantalla no se pueden generar desde el código: faltan por hacer.
- La sesión es de memoria del servidor (se pierde al reiniciar). Para producción convendría HTTPS, `cookie.secure=true` y límite de intentos de login (hoy solo se audita cada intento fallido).
