package co.edu.uniminuto.securelink.repositorio;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Lecturas del catalogo didactico (roles, algoritmos, secciones) y claves RSA de demostracion. */
@Repository
public class CatalogoRepositorio {

    public record Algoritmo(String codigo, String nombre, String categoria, int nivelMinimo,
                            boolean seguro, String descripcionCorta) {
    }

    public record Seccion(String codigo, String nombre, int nivelRequerido, String contenido) {
    }

    private static final String ALG = """
            SELECT a.codigo, a.nombre, c.nombre AS categoria, a.nivel_minimo_ejecucion AS nivel,
                   a.es_seguro AS seguro, a.descripcion_corta AS corta
            FROM algoritmos a JOIN categorias_algoritmo c ON c.id = a.categoria_id
            """;

    private final JdbcClient jdbc;

    public CatalogoRepositorio(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public List<Algoritmo> algoritmos() {
        return jdbc.sql(ALG + " ORDER BY c.id, a.id").query(this::mapear).list();
    }

    public Optional<Algoritmo> algoritmo(String codigo) {
        return jdbc.sql(ALG + " WHERE a.codigo = ?").param(codigo).query(this::mapear).optional();
    }

    public List<String> operaciones(String algoritmo) {
        return jdbc.sql("""
                        SELECT o.codigo FROM algoritmo_operaciones ao
                        JOIN algoritmos a ON a.id = ao.algoritmo_id
                        JOIN operaciones o ON o.id = ao.operacion_id
                        WHERE a.codigo = ? ORDER BY o.id
                        """).param(algoritmo).query(String.class).list();
    }

    public List<Seccion> secciones(String algoritmo) {
        return jdbc.sql("""
                        SELECT t.codigo, t.nombre, t.nivel_requerido, s.contenido
                        FROM secciones_algoritmo s
                        JOIN algoritmos a ON a.id = s.algoritmo_id
                        JOIN tipos_seccion t ON t.id = s.tipo_seccion_id
                        WHERE a.codigo = ? ORDER BY t.orden
                        """).param(algoritmo)
                .query((rs, i) -> new Seccion(rs.getString(1), rs.getString(2), rs.getInt(3), rs.getString(4)))
                .list();
    }

    public List<Map<String, Object>> roles() {
        return jdbc.sql("SELECT codigo, nombre, nivel, descripcion FROM roles ORDER BY nivel").query().listOfRows();
    }

    public List<Map<String, Object>> tiposSeccion() {
        return jdbc.sql("SELECT codigo, nombre, nivel_requerido AS nivelRequerido FROM tipos_seccion ORDER BY orden")
                .query().listOfRows();
    }

    /** Clave de demostracion: {publica, privada} o vacio si aun no existe. */
    public Optional<String[]> claveDemo(String algoritmo) {
        return jdbc.sql("""
                        SELECT c.clave_publica, c.clave_privada FROM claves_demo c
                        JOIN algoritmos a ON a.id = c.algoritmo_id WHERE a.codigo = ?
                        """).param(algoritmo)
                .query((rs, i) -> new String[]{rs.getString(1), rs.getString(2)}).optional();
    }

    public void guardarClaveDemo(String algoritmo, String publica, String privada) {
        jdbc.sql("""
                        INSERT OR IGNORE INTO claves_demo (algoritmo_id, clave_publica, clave_privada)
                        VALUES ((SELECT id FROM algoritmos WHERE codigo = ?), ?, ?)
                        """).param(algoritmo).param(publica).param(privada).update();
    }

    private Algoritmo mapear(ResultSet rs, int i) throws SQLException {
        return new Algoritmo(rs.getString("codigo"), rs.getString("nombre"), rs.getString("categoria"),
                rs.getInt("nivel"), rs.getInt("seguro") == 1, rs.getString("corta"));
    }
}
