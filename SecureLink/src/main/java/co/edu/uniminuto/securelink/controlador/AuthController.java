package co.edu.uniminuto.securelink.controlador;

import co.edu.uniminuto.securelink.controlador.ErroresApi.CredencialesInvalidas;
import co.edu.uniminuto.securelink.seguridad.UsuarioPrincipal;
import co.edu.uniminuto.securelink.servicio.AuditoriaService;
import co.edu.uniminuto.securelink.servicio.UsuarioService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    /** Mensaje generico: no revela si el usuario existe o la contrasena es incorrecta. */
    static final String LOGIN_FALLIDO = "Usuario o contraseña incorrectos";

    public record LoginPeticion(String usuario, String contrasena) {
    }

    public record RegistroPeticion(String usuario, String contrasena, String confirmarContrasena) {
    }

    public record SesionRespuesta(String usuario, String rol, int nivel, String redirigirA) {
    }

    public record Mensaje(String mensaje) {
    }

    private final AuthenticationManager authManager;
    private final UsuarioService usuarios;
    private final AuditoriaService auditoria;
    private final SecurityContextRepository contextoRepo = new HttpSessionSecurityContextRepository();

    public AuthController(AuthenticationManager authManager, UsuarioService usuarios, AuditoriaService auditoria) {
        this.authManager = authManager;
        this.usuarios = usuarios;
        this.auditoria = auditoria;
    }

    @PostMapping("/login")
    public SesionRespuesta login(@RequestBody LoginPeticion p, HttpServletRequest req, HttpServletResponse res) {
        String nombre = p.usuario() == null ? "" : p.usuario().trim();
        Authentication auth;
        try {
            if (nombre.isEmpty() || p.contrasena() == null) {
                throw new CredencialesInvalidas(LOGIN_FALLIDO);
            }
            auth = authManager.authenticate(UsernamePasswordAuthenticationToken.unauthenticated(nombre, p.contrasena()));
        } catch (AuthenticationException | CredencialesInvalidas e) {
            auditoria.registrar("LOGIN_FALLIDO", null, nombre, null, null, req.getRemoteAddr());
            throw new CredencialesInvalidas(LOGIN_FALLIDO);
        }

        // Evita fijacion de sesion: si habia sesion previa se cambia su identificador.
        if (req.getSession(false) != null) {
            req.changeSessionId();
        }
        SecurityContext contexto = SecurityContextHolder.createEmptyContext();
        contexto.setAuthentication(auth);
        SecurityContextHolder.setContext(contexto);
        contextoRepo.saveContext(contexto, req, res);

        UsuarioPrincipal u = (UsuarioPrincipal) auth.getPrincipal();
        auditoria.registrar("LOGIN_OK", u.getId(), null, null, null, req.getRemoteAddr());
        return new SesionRespuesta(u.getUsername(), u.getRol(), u.getNivel(), u.getRutaInicio());
    }

    @PostMapping("/registro")
    @ResponseStatus(HttpStatus.CREATED)
    public Mensaje registro(@RequestBody RegistroPeticion p, HttpServletRequest req) {
        var nuevo = usuarios.registrar(p.usuario(), p.contrasena(), p.confirmarContrasena());
        auditoria.registrar("REGISTRO", nuevo.id(), null, null, null, req.getRemoteAddr());
        return new Mensaje("Usuario registrado correctamente. Ya puedes iniciar sesión.");
    }

    @GetMapping("/me")
    public SesionRespuesta me(@AuthenticationPrincipal UsuarioPrincipal u) {
        return new SesionRespuesta(u.getUsername(), u.getRol(), u.getNivel(), u.getRutaInicio());
    }
}
