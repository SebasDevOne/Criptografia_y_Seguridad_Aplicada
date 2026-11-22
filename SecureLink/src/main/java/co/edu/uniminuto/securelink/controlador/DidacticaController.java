package co.edu.uniminuto.securelink.controlador;

import co.edu.uniminuto.securelink.seguridad.UsuarioPrincipal;
import co.edu.uniminuto.securelink.servicio.DidacticaService;
import co.edu.uniminuto.securelink.servicio.DidacticaService.AlgoritmoResumen;
import co.edu.uniminuto.securelink.servicio.DidacticaService.DetalleAlgoritmo;
import co.edu.uniminuto.securelink.servicio.DidacticaService.PeticionOperacion;
import co.edu.uniminuto.securelink.servicio.DidacticaService.RespuestaOperacion;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/** Laboratorio criptografico: disponible para cualquier rol autenticado, con detalle segun el rol. */
@RestController
@RequestMapping("/api/didactica")
public class DidacticaController {

    private final DidacticaService servicio;

    public DidacticaController(DidacticaService servicio) {
        this.servicio = servicio;
    }

    @GetMapping("/algoritmos")
    public List<AlgoritmoResumen> algoritmos(@AuthenticationPrincipal UsuarioPrincipal u) {
        return servicio.listar(u);
    }

    @GetMapping("/algoritmos/{codigo}")
    public DetalleAlgoritmo detalle(@AuthenticationPrincipal UsuarioPrincipal u, @PathVariable String codigo) {
        return servicio.detalle(u, codigo);
    }

    @PostMapping("/operar")
    public RespuestaOperacion operar(@AuthenticationPrincipal UsuarioPrincipal u, @RequestBody PeticionOperacion p,
                                     HttpServletRequest req) {
        return servicio.operar(u, p, req.getRemoteAddr());
    }

    @GetMapping("/historial")
    public List<Map<String, Object>> historial(@AuthenticationPrincipal UsuarioPrincipal u) {
        return servicio.historial(u);
    }

    @GetMapping("/roles")
    public List<Map<String, Object>> roles() {
        return servicio.roles();
    }
}
