package co.edu.uniminuto.securelink.servicio;

import co.edu.uniminuto.securelink.controlador.ErroresApi.AccesoDenegado;
import co.edu.uniminuto.securelink.controlador.ErroresApi.NoEncontrado;
import co.edu.uniminuto.securelink.controlador.ErroresApi.SolicitudInvalida;
import co.edu.uniminuto.securelink.repositorio.CatalogoRepositorio;
import co.edu.uniminuto.securelink.repositorio.CatalogoRepositorio.Algoritmo;
import co.edu.uniminuto.securelink.seguridad.UsuarioPrincipal;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Laboratorio criptografico didactico: lo que se muestra depende del rol. */
@Service
public class DidacticaService {

    public record PeticionOperacion(String algoritmo, String operacion, String texto, String clave, String referencia) {
    }

    public record AlgoritmoResumen(String codigo, String nombre, String categoria, boolean seguro,
                                   String descripcionCorta, int nivelMinimo, boolean puedeEjecutar,
                                   List<String> operaciones) {
    }

    public record SeccionVista(String codigo, String nombre, int nivelRequerido, boolean bloqueada, String contenido) {
    }

    public record DetalleAlgoritmo(AlgoritmoResumen algoritmo, List<SeccionVista> secciones, int seccionesBloqueadas) {
    }

    public record PasoVisible(String titulo, String valor, String visibilidad) {
    }

    public record RespuestaOperacion(String algoritmo, String operacion, String resultado, String rol, int nivelDetalle,
                                     List<PasoVisible> pasos, int pasosOcultos, Long tiempoMicros, String aviso) {
    }

    private final CatalogoRepositorio catalogo;
    private final CriptoService cripto;
    private final AuditoriaService auditoria;

    public DidacticaService(CatalogoRepositorio catalogo, CriptoService cripto, AuditoriaService auditoria) {
        this.catalogo = catalogo;
        this.cripto = cripto;
        this.auditoria = auditoria;
    }

    public List<AlgoritmoResumen> listar(UsuarioPrincipal u) {
        return catalogo.algoritmos().stream().map(a -> resumen(a, u.getNivel())).toList();
    }

    public DetalleAlgoritmo detalle(UsuarioPrincipal u, String codigo) {
        Algoritmo a = catalogo.algoritmo(codigo.toUpperCase(Locale.ROOT))
                .orElseThrow(() -> new NoEncontrado("Algoritmo no encontrado."));
        List<SeccionVista> secciones = catalogo.secciones(a.codigo()).stream()
                .map(s -> {
                    boolean bloqueada = u.getNivel() < s.nivelRequerido();
                    return new SeccionVista(s.codigo(), s.nombre(), s.nivelRequerido(), bloqueada,
                            bloqueada ? null : s.contenido());
                }).toList();
        int bloqueadas = (int) secciones.stream().filter(SeccionVista::bloqueada).count();
        return new DetalleAlgoritmo(resumen(a, u.getNivel()), secciones, bloqueadas);
    }

    public RespuestaOperacion operar(UsuarioPrincipal u, PeticionOperacion p, String ip) {
        if (p == null || p.algoritmo() == null || p.operacion() == null) {
            throw new SolicitudInvalida("Indica algoritmo y operación.");
        }
        String codigo = p.algoritmo().trim().toUpperCase(Locale.ROOT);
        String operacion = p.operacion().trim().toUpperCase(Locale.ROOT);
        Algoritmo a = catalogo.algoritmo(codigo).orElseThrow(() -> new NoEncontrado("Algoritmo no encontrado."));
        if (u.getNivel() < a.nivelMinimo()) {
            auditoria.registrar("ACCESO_DENEGADO", u.getId(), null, codigo, "operar " + operacion, ip);
            throw new AccesoDenegado("Tu rol no puede ejecutar " + a.nombre() + ".");
        }
        if (!catalogo.operaciones(codigo).contains(operacion)) {
            throw new SolicitudInvalida("La operación " + operacion + " no está disponible para " + a.nombre() + ".");
        }

        long t0 = System.nanoTime();
        CriptoService.Resultado r = cripto.ejecutar(codigo, operacion, p.texto(), p.clave(), p.referencia());
        long micros = (System.nanoTime() - t0) / 1000;

        auditoria.registrar("OPERACION_CRIPTO", u.getId(), null, codigo, operacion, ip);

        List<PasoVisible> pasos = new ArrayList<>();
        int ocultos = 0;
        for (CriptoService.Paso paso : r.pasos()) {
            switch (Visibilidad.clasificar(u.getNivel(), paso.nivelCompleto())) {
                case COMPLETO -> pasos.add(new PasoVisible(paso.titulo(), paso.valor(), "COMPLETO"));
                case PARCIAL -> pasos.add(new PasoVisible(paso.titulo(), Visibilidad.enmascarar(paso.valor()), "PARCIAL"));
                case OCULTO -> {
                    ocultos++;
                    pasos.add(new PasoVisible("Detalle oculto para tu rol", null, "OCULTO"));
                }
            }
        }
        String aviso = a.seguro() ? null : "Algoritmo obsoleto o inseguro: se muestra solo con fines didácticos.";
        return new RespuestaOperacion(a.nombre(), operacion, r.salida(), u.getRol(), u.getNivel(), pasos, ocultos,
                u.getNivel() >= 2 ? micros : null, aviso);
    }

    public List<Map<String, Object>> historial(UsuarioPrincipal u) {
        return auditoria.operaciones(u.getNivel() >= 3 ? null : u.getId(), 20);
    }

    public List<Map<String, Object>> roles() {
        return catalogo.roles();
    }

    private AlgoritmoResumen resumen(Algoritmo a, int nivel) {
        return new AlgoritmoResumen(a.codigo(), a.nombre(), a.categoria(), a.seguro(), a.descripcionCorta(),
                a.nivelMinimo(), nivel >= a.nivelMinimo(), catalogo.operaciones(a.codigo()));
    }
}
