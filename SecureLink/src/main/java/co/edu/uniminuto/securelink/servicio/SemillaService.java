package co.edu.uniminuto.securelink.servicio;

import co.edu.uniminuto.securelink.config.PropiedadesSemilla;
import co.edu.uniminuto.securelink.repositorio.UsuarioRepositorio;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/** Crea al arrancar los usuarios iniciales (solo guarda su hash bcrypt). */
@Component
public class SemillaService implements ApplicationRunner {

    private final PropiedadesSemilla semilla;
    private final UsuarioRepositorio usuarios;
    private final PasswordEncoder encoder;

    public SemillaService(PropiedadesSemilla semilla, UsuarioRepositorio usuarios, PasswordEncoder encoder) {
        this.semilla = semilla;
        this.usuarios = usuarios;
        this.encoder = encoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        for (PropiedadesSemilla.UsuarioSemilla u : semilla.usuarios()) {
            if (u.nombre() != null && u.contrasena() != null && !u.contrasena().isBlank()
                    && !usuarios.existe(u.nombre())) {
                usuarios.crear(u.nombre(), encoder.encode(u.contrasena()), u.rol());
            }
        }
    }
}
