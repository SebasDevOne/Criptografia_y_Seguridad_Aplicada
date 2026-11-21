package co.edu.uniminuto.securelink.servicio;

/**
 * Regla didactica de visibilidad. Cada dato tiene un "nivelCompleto" (1 invitado, 2 usuario, 3 admin):
 * con nivel mayor o igual se ve completo, con un nivel menos se ve parcial y con menos se oculta.
 */
public final class Visibilidad {

    public enum Estado { COMPLETO, PARCIAL, OCULTO }

    private Visibilidad() {
    }

    public static Estado clasificar(int nivelUsuario, int nivelCompleto) {
        if (nivelUsuario >= nivelCompleto) {
            return Estado.COMPLETO;
        }
        return nivelUsuario == nivelCompleto - 1 ? Estado.PARCIAL : Estado.OCULTO;
    }

    /** Muestra solo el inicio del valor y cuenta cuantos caracteres se ocultan. */
    public static String enmascarar(String valor) {
        if (valor == null || valor.isEmpty()) {
            return valor;
        }
        int visibles = Math.min(6, Math.max(1, valor.length() / 4));
        return valor.substring(0, visibles) + "… [" + (valor.length() - visibles) + " caracteres ocultos]";
    }

    /** Devuelve el valor segun el estado: completo, enmascarado o null. */
    public static String aplicar(int nivelUsuario, int nivelCompleto, String valor) {
        return switch (clasificar(nivelUsuario, nivelCompleto)) {
            case COMPLETO -> valor;
            case PARCIAL -> enmascarar(valor);
            case OCULTO -> null;
        };
    }
}
