package co.edu.uniminuto.securelink.seguridad;

import co.edu.uniminuto.securelink.dominio.Usuario;
import org.springframework.security.core.CredentialsContainer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.Locale;

/** Usuario autenticado. El hash se borra de memoria tras autenticar. */
public class UsuarioPrincipal implements UserDetails, CredentialsContainer {

    private final long id;
    private final String nombre;
    private final String rol;
    private final String rutaInicio;
    private final int nivel;
    private final boolean activo;
    private String hash;

    public UsuarioPrincipal(Usuario u) {
        this.id = u.id();
        this.nombre = u.nombreUsuario();
        this.rol = u.rol();
        this.rutaInicio = u.rutaInicio();
        this.nivel = u.nivel();
        this.activo = u.activo();
        this.hash = u.hash();
    }

    public long getId() { return id; }
    public String getRol() { return rol; }
    public String getRutaInicio() { return rutaInicio; }
    public int getNivel() { return nivel; }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + rol.toUpperCase(Locale.ROOT)));
    }

    @Override public String getPassword() { return hash; }
    @Override public String getUsername() { return nombre; }
    @Override public boolean isEnabled() { return activo; }
    @Override public void eraseCredentials() { this.hash = null; }
}
