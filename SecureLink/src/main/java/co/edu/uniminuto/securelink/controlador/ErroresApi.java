package co.edu.uniminuto.securelink.controlador;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Excepciones de la API y su traduccion a JSON {"error": "..."}. */
@RestControllerAdvice
public class ErroresApi {

    public record ErrorRespuesta(String error) {
    }

    public static class SolicitudInvalida extends RuntimeException {
        public SolicitudInvalida(String m) { super(m); }
    }

    public static class Conflicto extends RuntimeException {
        public Conflicto(String m) { super(m); }
    }

    public static class NoEncontrado extends RuntimeException {
        public NoEncontrado(String m) { super(m); }
    }

    public static class CredencialesInvalidas extends RuntimeException {
        public CredencialesInvalidas(String m) { super(m); }
    }

    public static class AccesoDenegado extends RuntimeException {
        public AccesoDenegado(String m) { super(m); }
    }

    @ExceptionHandler(SolicitudInvalida.class)
    ResponseEntity<ErrorRespuesta> invalida(SolicitudInvalida e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorRespuesta(e.getMessage()));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ErrorRespuesta> ilegible(HttpMessageNotReadableException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorRespuesta("Cuerpo de la petición inválido."));
    }

    @ExceptionHandler(Conflicto.class)
    ResponseEntity<ErrorRespuesta> conflicto(Conflicto e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorRespuesta(e.getMessage()));
    }

    @ExceptionHandler(NoEncontrado.class)
    ResponseEntity<ErrorRespuesta> noEncontrado(NoEncontrado e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorRespuesta(e.getMessage()));
    }

    @ExceptionHandler(CredencialesInvalidas.class)
    ResponseEntity<ErrorRespuesta> credenciales(CredencialesInvalidas e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ErrorRespuesta(e.getMessage()));
    }

    @ExceptionHandler(AccesoDenegado.class)
    ResponseEntity<ErrorRespuesta> denegado(AccesoDenegado e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ErrorRespuesta(e.getMessage()));
    }
}
