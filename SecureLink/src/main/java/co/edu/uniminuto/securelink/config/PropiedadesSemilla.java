package co.edu.uniminuto.securelink.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/** Usuarios iniciales leidos de application.properties (nada queda en el codigo). */
@ConfigurationProperties(prefix = "securelink.semilla")
public record PropiedadesSemilla(List<UsuarioSemilla> usuarios) {

    public record UsuarioSemilla(String nombre, String contrasena, String rol) {
    }

    public PropiedadesSemilla {
        usuarios = usuarios == null ? List.of() : usuarios;
    }
}
