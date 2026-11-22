package co.edu.uniminuto.securelink.dominio;

import java.io.Serializable;

/** Usuario leido de la base, ya unido con su rol. */
public record Usuario(long id, String nombreUsuario, String hash, String rol, String rolNombre,
                      int nivel, String rutaInicio, boolean activo, String creadoEn) implements Serializable {
}
