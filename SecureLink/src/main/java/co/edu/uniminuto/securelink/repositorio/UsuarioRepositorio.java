package co.edu.uniminuto.securelink.repositorio;

import co.edu.uniminuto.securelink.dominio.Usuario;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

@Repository
public class UsuarioRepositorio {

    private static final String SELECT = """
            SELECT u.id, u.nombre_usuario, u.hash_contrasena, r.codigo AS rol, r.nombre AS rol_nombre,
                   r.nivel, r.ruta_inicio, u.activo, u.creado_en
            FROM usuarios u JOIN roles r ON r.id = u.rol_id
            """;

    private final JdbcClient jdbc;

    public UsuarioRepositorio(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<Usuario> buscarPorNombre(String nombre) {
        return jdbc.sql(SELECT + " WHERE u.nombre_usuario = ?").param(nombre).query(this::mapear).optional();
    }

    public Optional<Usuario> buscarPorId(long id) {
        return jdbc.sql(SELECT + " WHERE u.id = ?").param(id).query(this::mapear).optional();
    }

    public List<Usuario> listar() {
        return jdbc.sql(SELECT + " ORDER BY u.nombre_usuario").query(this::mapear).list();
    }

    public boolean existe(String nombre) {
        Integer n = jdbc.sql("SELECT COUNT(*) FROM usuarios WHERE nombre_usuario = ?")
                .param(nombre).query(Integer.class).single();
        return n != null && n > 0;
    }

    public void crear(String nombre, String hash, String rolCodigo) {
        int filas = jdbc.sql("""
                        INSERT INTO usuarios (nombre_usuario, hash_contrasena, rol_id)
                        VALUES (?, ?, (SELECT id FROM roles WHERE codigo = ?))
                        """)
                .param(nombre).param(hash).param(rolCodigo).update();
        if (filas != 1) {
            throw new IllegalStateException("No se pudo crear el usuario");
        }
    }

    private Usuario mapear(ResultSet rs, int fila) throws SQLException {
        return new Usuario(rs.getLong("id"), rs.getString("nombre_usuario"), rs.getString("hash_contrasena"),
                rs.getString("rol"), rs.getString("rol_nombre"), rs.getInt("nivel"), rs.getString("ruta_inicio"),
                rs.getInt("activo") == 1, rs.getString("creado_en"));
    }
}
