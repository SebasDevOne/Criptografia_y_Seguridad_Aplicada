package co.edu.uniminuto.securelink.servicio;

import co.edu.uniminuto.securelink.repositorio.AuditoriaRepositorio;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class AuditoriaService {

    private final AuditoriaRepositorio repo;

    public AuditoriaService(AuditoriaRepositorio repo) {
        this.repo = repo;
    }

    public void registrar(String evento, Long usuarioId, String nombreIntentado, String algoritmo,
                          String detalle, String ip) {
        repo.insertar(evento, usuarioId, nombreIntentado, algoritmo, detalle, ip);
    }

    public List<Map<String, Object>> ultimos(int limite) {
        return repo.ultimos(limite);
    }

    public List<Map<String, Object>> operaciones(Long usuarioId, int limite) {
        return repo.operacionesDe(usuarioId, limite);
    }
}
