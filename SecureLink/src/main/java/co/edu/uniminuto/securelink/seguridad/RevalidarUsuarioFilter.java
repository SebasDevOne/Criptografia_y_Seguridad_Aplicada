package co.edu.uniminuto.securelink.seguridad;

import co.edu.uniminuto.securelink.dominio.Usuario;
import co.edu.uniminuto.securelink.repositorio.UsuarioRepositorio;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

/**
 * En cada peticion vuelve a leer al usuario en la base: si fue eliminado o desactivado se
 * invalida la sesion, y si su rol cambio se usa el rol actual (no el guardado en la sesion).
 */
public class RevalidarUsuarioFilter extends OncePerRequestFilter {

    private final UsuarioRepositorio usuarios;

    public RevalidarUsuarioFilter(UsuarioRepositorio usuarios) {
        this.usuarios = usuarios;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UsuarioPrincipal principal) {
            Optional<Usuario> actual = usuarios.buscarPorId(principal.getId());
            if (actual.isEmpty() || !actual.get().activo()) {
                HttpSession sesion = request.getSession(false);
                if (sesion != null) {
                    sesion.invalidate();
                }
                SecurityContextHolder.clearContext();
            } else if (!actual.get().rol().equals(principal.getRol())) {
                UsuarioPrincipal nuevo = new UsuarioPrincipal(actual.get());
                nuevo.eraseCredentials();
                SecurityContextHolder.getContext().setAuthentication(
                        UsernamePasswordAuthenticationToken.authenticated(nuevo, null, nuevo.getAuthorities()));
            }
        }
        chain.doFilter(request, response);
    }
}
