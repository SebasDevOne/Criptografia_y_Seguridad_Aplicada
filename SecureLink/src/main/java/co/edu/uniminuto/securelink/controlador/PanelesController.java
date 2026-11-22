package co.edu.uniminuto.securelink.controlador;

import co.edu.uniminuto.securelink.dominio.Usuario;
import co.edu.uniminuto.securelink.repositorio.CatalogoRepositorio;
import co.edu.uniminuto.securelink.seguridad.UsuarioPrincipal;
import co.edu.uniminuto.securelink.servicio.AuditoriaService;
import co.edu.uniminuto.securelink.servicio.DidacticaService;
import co.edu.uniminuto.securelink.servicio.UsuarioService;
import co.edu.uniminuto.securelink.servicio.UsuarioService.EstructuraHash;
import co.edu.uniminuto.securelink.servicio.UsuarioService.UsuarioAdmin;
import co.edu.uniminuto.securelink.servicio.Visibilidad;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/** Paneles protegidos por rol. Spring Security ya valido el rol antes de llegar aqui. */
@RestController
@RequestMapping("/api")
public class PanelesController {

    public record PanelAdmin(List<UsuarioAdmin> usuarios, List<Map<String, Object>> auditoria) {
    }

    public record PanelUsuario(String usuario, String mensaje, EstructuraHash miHash, List<Map<String, Object>> historial) {
    }

    public record PanelInvitado(String usuario, String mensaje, List<String> algoritmosBloqueados,
                                List<String> seccionesBloqueadas) {
    }

    private final UsuarioService usuarios;
    private final AuditoriaService auditoria;
    private final DidacticaService didactica;
    private final CatalogoRepositorio catalogo;

    public PanelesController(UsuarioService usuarios, AuditoriaService auditoria, DidacticaService didactica,
                             CatalogoRepositorio catalogo) {
        this.usuarios = usuarios;
        this.auditoria = auditoria;
        this.didactica = didactica;
        this.catalogo = catalogo;
    }

    /** El administrador ve todo: hash completo de cada usuario y la auditoria. */
    @GetMapping("/admin/panel")
    public PanelAdmin admin() {
        return new PanelAdmin(usuarios.listarParaAdmin(), auditoria.ultimos(100));
    }

    /** El usuario ve la estructura de su hash, pero la sal y el digest salen parciales. */
    @GetMapping("/usuario/panel")
    public PanelUsuario usuario(@AuthenticationPrincipal UsuarioPrincipal u) {
        Usuario actual = usuarios.porId(u.getId());
        EstructuraHash h = UsuarioService.analizar(actual.hash());
        EstructuraHash parcial = new EstructuraHash(h.algoritmo(), h.version(), h.costo(),
                Visibilidad.aplicar(u.getNivel(), 3, h.sal()), Visibilidad.aplicar(u.getNivel(), 3, h.digest()));
        return new PanelUsuario(u.getUsername(), "Bienvenido al panel de usuario.", parcial, didactica.historial(u));
    }

    /** El invitado solo ve lo basico y la lista de lo que no puede ver. */
    @GetMapping("/invitado/panel")
    public PanelInvitado invitado(@AuthenticationPrincipal UsuarioPrincipal u) {
        List<String> algoritmos = catalogo.algoritmos().stream()
                .filter(a -> a.nivelMinimo() > u.getNivel()).map(CatalogoRepositorio.Algoritmo::nombre).toList();
        List<String> secciones = catalogo.tiposSeccion().stream()
                .filter(s -> ((Number) s.get("nivelRequerido")).intValue() > u.getNivel())
                .map(s -> String.valueOf(s.get("nombre"))).toList();
        return new PanelInvitado(u.getUsername(), "Bienvenido, invitado.", algoritmos, secciones);
    }
}
