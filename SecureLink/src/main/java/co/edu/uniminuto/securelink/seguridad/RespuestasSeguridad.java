package co.edu.uniminuto.securelink.seguridad;

import co.edu.uniminuto.securelink.servicio.AuditoriaService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/** Respuestas JSON de Spring Security: 401 sin sesion, 403 sin permiso y cierre de sesion. */
@Component
public class RespuestasSeguridad implements AuthenticationEntryPoint, AccessDeniedHandler, LogoutSuccessHandler {

    private final AuditoriaService auditoria;

    public RespuestasSeguridad(AuditoriaService auditoria) {
        this.auditoria = auditoria;
    }

    @Override
    public void commence(HttpServletRequest req, HttpServletResponse res, AuthenticationException e) throws IOException {
        escribir(res, 401, "No hay sesión activa. Inicia sesión.");
    }

    @Override
    public void handle(HttpServletRequest req, HttpServletResponse res, AccessDeniedException e) throws IOException {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        Long id = a != null && a.getPrincipal() instanceof UsuarioPrincipal p ? p.getId() : null;
        auditoria.registrar("ACCESO_DENEGADO", id, null, null, req.getRequestURI(), req.getRemoteAddr());
        escribir(res, 403, "Acceso denegado: tu rol no puede entrar a esta ruta.");
    }

    @Override
    public void onLogoutSuccess(HttpServletRequest req, HttpServletResponse res, Authentication a) throws IOException {
        if (a != null && a.getPrincipal() instanceof UsuarioPrincipal p) {
            auditoria.registrar("LOGOUT", p.getId(), null, null, null, req.getRemoteAddr());
        }
        res.setStatus(200);
        res.setContentType("application/json;charset=UTF-8");
        res.getOutputStream().write("{\"mensaje\":\"Sesión cerrada correctamente.\"}".getBytes(StandardCharsets.UTF_8));
    }

    private void escribir(HttpServletResponse res, int estado, String mensaje) throws IOException {
        res.setStatus(estado);
        res.setContentType("application/json;charset=UTF-8");
        res.getOutputStream().write(("{\"error\":\"" + mensaje + "\"}").getBytes(StandardCharsets.UTF_8));
    }
}
