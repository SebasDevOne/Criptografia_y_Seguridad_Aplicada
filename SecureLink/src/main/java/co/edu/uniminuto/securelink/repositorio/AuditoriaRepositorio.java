package co.edu.uniminuto.securelink.repositorio;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public class AuditoriaRepositorio {

    private static final String SELECT = """
            SELECT a.id, a.creado_en AS fecha, e.codigo AS evento, u.nombre_usuario AS usuario,
                   a.nombre_intentado AS intentado, g.codigo AS algoritmo, a.detalle, a.ip
            FROM auditoria a
            JOIN tipos_evento e ON e.id = a.tipo_evento_id
            LEFT JOIN usuarios u ON u.id = a.usuario_id
            LEFT JOIN algoritmos g ON g.id = a.algoritmo_id
            """;

    private final JdbcClient jdbc;

    public AuditoriaRepositorio(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public void insertar(String evento, Long usuarioId, String nombreIntentado, String algoritmo,
                         String detalle, String ip) {
        jdbc.sql("""
                        INSERT INTO auditoria (usuario_id, nombre_intentado, tipo_evento_id, algoritmo_id, detalle, ip)
                        VALUES (?, ?, (SELECT id FROM tipos_evento WHERE codigo = ?),
                                (SELECT id FROM algoritmos WHERE codigo = ?), ?, ?)
                        """)
                .param(usuarioId).param(nombreIntentado).param(evento).param(algoritmo).param(detalle).param(ip)
                .update();
    }

    public List<Map<String, Object>> ultimos(int limite) {
        return jdbc.sql(SELECT + " ORDER BY a.id DESC LIMIT ?").param(limite).query().listOfRows();
    }

    public List<Map<String, Object>> operacionesDe(Long usuarioId, int limite) {
        if (usuarioId == null) {
            return jdbc.sql(SELECT + " WHERE e.codigo = 'OPERACION_CRIPTO' ORDER BY a.id DESC LIMIT ?")
                    .param(limite).query().listOfRows();
        }
        return jdbc.sql(SELECT + " WHERE e.codigo = 'OPERACION_CRIPTO' AND a.usuario_id = ? ORDER BY a.id DESC LIMIT ?")
                .param(usuarioId).param(limite).query().listOfRows();
    }

    public int contar(String evento, String nombreIntentado) {
        Integer n = jdbc.sql("""
                        SELECT COUNT(*) FROM auditoria a JOIN tipos_evento e ON e.id = a.tipo_evento_id
                        WHERE e.codigo = ? AND a.nombre_intentado = ?
                        """).param(evento).param(nombreIntentado).query(Integer.class).single();
        return n == null ? 0 : n;
    }
}
