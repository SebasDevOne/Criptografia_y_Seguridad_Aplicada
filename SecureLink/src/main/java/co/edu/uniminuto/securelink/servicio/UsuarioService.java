package co.edu.uniminuto.securelink.servicio;

import co.edu.uniminuto.securelink.controlador.ErroresApi.Conflicto;
import co.edu.uniminuto.securelink.controlador.ErroresApi.SolicitudInvalida;
import co.edu.uniminuto.securelink.dominio.Usuario;
import co.edu.uniminuto.securelink.repositorio.UsuarioRepositorio;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.regex.Pattern;

@Service
public class UsuarioService {

    private static final Pattern NOMBRE = Pattern.compile("[A-Za-z0-9_.-]{3,50}");

    /** Estructura de un hash bcrypt ($version$costo$sal+digest). */
    public record EstructuraHash(String algoritmo, String version, Integer costo, String sal, String digest) {
    }

    /** Vista de un usuario para el panel de administracion (el admin ve todo). */
    public record UsuarioAdmin(long id, String usuario, String rol, boolean activo, String creadoEn,
                               String hash, EstructuraHash estructura) {
    }

    private final UsuarioRepositorio repo;
    private final PasswordEncoder encoder;
    private final String rolPorDefecto;

    public UsuarioService(UsuarioRepositorio repo, PasswordEncoder encoder,
                          @Value("${securelink.registro.rol-por-defecto:usuario}") String rolPorDefecto) {
        this.repo = repo;
        this.encoder = encoder;
        this.rolPorDefecto = rolPorDefecto;
    }

    /** Registro publico: el rol lo fija el servidor, nunca el formulario. */
    public Usuario registrar(String usuario, String contrasena, String confirmar) {
        String nombre = usuario == null ? "" : usuario.trim();
        if (!NOMBRE.matcher(nombre).matches()) {
            throw new SolicitudInvalida("El usuario debe tener entre 3 y 50 caracteres (letras, números, _ . -).");
        }
        if (contrasena == null || contrasena.length() < 8) {
            throw new SolicitudInvalida("La contraseña debe tener al menos 8 caracteres.");
        }
        if (contrasena.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new SolicitudInvalida("La contraseña no puede superar 72 bytes (límite de bcrypt).");
        }
        if (!contrasena.equals(confirmar)) {
            throw new SolicitudInvalida("Las contraseñas no coinciden.");
        }
        if (repo.existe(nombre)) {
            throw new Conflicto("Ese nombre de usuario ya está registrado.");
        }
        try {
            repo.crear(nombre, encoder.encode(contrasena), rolPorDefecto);
        } catch (DataIntegrityViolationException e) {
            throw new Conflicto("Ese nombre de usuario ya está registrado.");
        }
        return repo.buscarPorNombre(nombre).orElseThrow();
    }

    public List<UsuarioAdmin> listarParaAdmin() {
        return repo.listar().stream()
                .map(u -> new UsuarioAdmin(u.id(), u.nombreUsuario(), u.rol(), u.activo(), u.creadoEn(),
                        u.hash(), analizar(u.hash())))
                .toList();
    }

    public Usuario porId(long id) {
        return repo.buscarPorId(id).orElseThrow();
    }

    /** Descompone un hash bcrypt para mostrarlo de forma didactica. */
    public static EstructuraHash analizar(String hash) {
        String[] p = hash.split("\\$");
        if (p.length == 4 && p[3].length() == 53) {
            return new EstructuraHash("bcrypt", "$" + p[1], Integer.valueOf(p[2]), p[3].substring(0, 22), p[3].substring(22));
        }
        return new EstructuraHash("desconocido", null, null, null, hash);
    }
}
