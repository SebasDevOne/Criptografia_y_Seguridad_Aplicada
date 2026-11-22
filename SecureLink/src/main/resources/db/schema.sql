-- Esquema normalizado hasta la 3FN para SQLite.
-- 1FN: valores atomicos, una fila por hecho. 2FN: sin dependencias parciales
-- (las tablas con clave compuesta solo tienen la clave). 3FN: ningun atributo
-- no clave depende de otro atributo no clave (los catalogos viven en su tabla).

CREATE TABLE IF NOT EXISTS roles (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    codigo      TEXT    NOT NULL UNIQUE,
    nombre      TEXT    NOT NULL,
    nivel       INTEGER NOT NULL CHECK (nivel BETWEEN 1 AND 3),
    ruta_inicio TEXT    NOT NULL,
    descripcion TEXT    NOT NULL
);

CREATE TABLE IF NOT EXISTS usuarios (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    nombre_usuario  TEXT    NOT NULL UNIQUE,
    hash_contrasena TEXT    NOT NULL,
    rol_id          INTEGER NOT NULL REFERENCES roles (id),
    activo          INTEGER NOT NULL DEFAULT 1 CHECK (activo IN (0, 1)),
    creado_en       TEXT    NOT NULL DEFAULT (datetime('now'))
);

CREATE TABLE IF NOT EXISTS categorias_algoritmo (
    id     INTEGER PRIMARY KEY AUTOINCREMENT,
    codigo TEXT NOT NULL UNIQUE,
    nombre TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS algoritmos (
    id                     INTEGER PRIMARY KEY AUTOINCREMENT,
    codigo                 TEXT    NOT NULL UNIQUE,
    nombre                 TEXT    NOT NULL,
    categoria_id           INTEGER NOT NULL REFERENCES categorias_algoritmo (id),
    nivel_minimo_ejecucion INTEGER NOT NULL CHECK (nivel_minimo_ejecucion BETWEEN 1 AND 3),
    es_seguro              INTEGER NOT NULL CHECK (es_seguro IN (0, 1)),
    descripcion_corta      TEXT    NOT NULL
);

CREATE TABLE IF NOT EXISTS operaciones (
    id     INTEGER PRIMARY KEY AUTOINCREMENT,
    codigo TEXT NOT NULL UNIQUE,
    nombre TEXT NOT NULL
);

-- Relacion N:M entre algoritmos y operaciones; la clave compuesta es toda la tabla.
CREATE TABLE IF NOT EXISTS algoritmo_operaciones (
    algoritmo_id INTEGER NOT NULL REFERENCES algoritmos (id),
    operacion_id INTEGER NOT NULL REFERENCES operaciones (id),
    PRIMARY KEY (algoritmo_id, operacion_id)
);

CREATE TABLE IF NOT EXISTS tipos_seccion (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    codigo          TEXT    NOT NULL UNIQUE,
    nombre          TEXT    NOT NULL,
    nivel_requerido INTEGER NOT NULL CHECK (nivel_requerido BETWEEN 1 AND 3),
    orden           INTEGER NOT NULL
);

CREATE TABLE IF NOT EXISTS secciones_algoritmo (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    algoritmo_id    INTEGER NOT NULL REFERENCES algoritmos (id),
    tipo_seccion_id INTEGER NOT NULL REFERENCES tipos_seccion (id),
    contenido       TEXT    NOT NULL,
    UNIQUE (algoritmo_id, tipo_seccion_id)
);

CREATE TABLE IF NOT EXISTS tipos_evento (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    codigo      TEXT NOT NULL UNIQUE,
    descripcion TEXT NOT NULL
);

-- usuario_id es NULL cuando el intento se hizo con un nombre inexistente o sin sesion.
CREATE TABLE IF NOT EXISTS auditoria (
    id               INTEGER PRIMARY KEY AUTOINCREMENT,
    usuario_id       INTEGER REFERENCES usuarios (id),
    nombre_intentado TEXT,
    tipo_evento_id   INTEGER NOT NULL REFERENCES tipos_evento (id),
    algoritmo_id     INTEGER REFERENCES algoritmos (id),
    detalle          TEXT,
    ip               TEXT,
    creado_en        TEXT NOT NULL DEFAULT (datetime('now'))
);

-- Par de claves RSA de demostracion (una por algoritmo asimetrico).
CREATE TABLE IF NOT EXISTS claves_demo (
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    algoritmo_id  INTEGER NOT NULL UNIQUE REFERENCES algoritmos (id),
    clave_publica TEXT NOT NULL,
    clave_privada TEXT NOT NULL,
    creado_en     TEXT NOT NULL DEFAULT (datetime('now'))
);

CREATE INDEX IF NOT EXISTS idx_usuarios_rol ON usuarios (rol_id);
CREATE INDEX IF NOT EXISTS idx_auditoria_usuario ON auditoria (usuario_id);
CREATE INDEX IF NOT EXISTS idx_auditoria_fecha ON auditoria (creado_en);
