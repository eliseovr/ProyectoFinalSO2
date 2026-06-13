package servidor;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tokens de sesión y usuarios conectados en tiempo real.
 * Asocia cada token UUID con un nombre de usuario y, opcionalmente,
 * con el {@link ManejadorCliente} activo para envío push de mensajes.
 */
public class GestorSesiones {
    private final Map<String, String> tokenAUsuario = new ConcurrentHashMap<>();
    private final Map<String, ManejadorCliente> usuariosEnLinea = new ConcurrentHashMap<>();

    /** Genera un token UUID y lo asocia al usuario (en minúsculas). */
    public String crearSesion(String nombreUsuario) {
        String token = UUID.randomUUID().toString();
        tokenAUsuario.put(token, nombreUsuario.toLowerCase());
        return token;
    }

    /** Devuelve el nombre de usuario del token o {@code null} si no es válido. */
    public String resolverUsuario(String token) {
        if (token == null) {
            return null;
        }
        return tokenAUsuario.get(token);
    }

    /** Elimina un token de sesión (logout o desconexión). */
    public void invalidar(String token) {
        if (token != null) {
            tokenAUsuario.remove(token);
        }
    }

    /** Registra el manejador activo de un usuario para mensajes push en tiempo real. */
    public void registrarEnLinea(String nombreUsuario, ManejadorCliente manejador) {
        usuariosEnLinea.put(nombreUsuario.toLowerCase(), manejador);
    }

    /** Quita al usuario del mapa de conexiones en línea. */
    public void desregistrarEnLinea(String nombreUsuario) {
        if (nombreUsuario != null) {
            usuariosEnLinea.remove(nombreUsuario.toLowerCase());
        }
    }

    /** Obtiene el manejador del destinatario si está conectado, o {@code null}. */
    public ManejadorCliente obtenerManejador(String nombreUsuario) {
        if (nombreUsuario == null) {
            return null;
        }
        return usuariosEnLinea.get(nombreUsuario.toLowerCase());
    }
}
