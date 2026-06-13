package comun;

/**
 * Traduce mensajes técnicos (a menudo en inglés del JDK o la red) a español claro
 * para mostrarlos al usuario final en la interfaz.
 */
public final class MensajesError {
    private MensajesError() {
    }

    /**
     * Obtiene un mensaje en español a partir de una excepción,
     * recorriendo la cadena de causas hasta la raíz.
     *
     * @param throwable excepción capturada
     * @return descripción legible en español
     */
    public static String aEspanol(Throwable throwable) {
        if (throwable == null) {
            return "Error desconocido";
        }
        Throwable raiz = throwable;
        while (raiz.getCause() != null && raiz.getCause() != raiz) {
            raiz = raiz.getCause();
        }
        String mensaje = raiz.getMessage();
        if (mensaje == null || mensaje.isBlank()) {
            mensaje = raiz.getClass().getSimpleName();
        }
        return aEspanol(mensaje);
    }

    /**
     * Traduce un mensaje de error conocido al español; si no hay regla,
     * devuelve el texto original.
     *
     * @param mensaje mensaje técnico (puede ser en inglés)
     * @return equivalente en español o el mensaje sin cambios
     */
    public static String aEspanol(String mensaje) {
        if (mensaje == null || mensaje.isBlank()) {
            return "Error desconocido";
        }
        String lower = mensaje.toLowerCase();

        if (lower.contains("connection refused")) {
            return "Conexión rechazada: el servidor no está activo o el puerto es incorrecto.";
        }
        if (lower.contains("connection reset")) {
            return "Conexión reiniciada por el servidor. Verifique que siga en ejecución.";
        }
        if (lower.contains("connection timed out") || lower.contains("timed out") || lower.contains("timeout")) {
            return "Tiempo de espera agotado: el servidor no respondió a tiempo.";
        }
        if (lower.contains("address already in use") || lower.contains("bind")) {
            return "El puerto ya está en uso. Cierre otras instancias del servidor.";
        }
        if (lower.contains("pkix") || lower.contains("path building failed")) {
            return "Certificado TLS no confiable. Ejecute reiniciar-todo.bat e inicie de nuevo el servidor.";
        }
        if (lower.contains("remote host terminated the handshake")) {
            return "Fallo en el handshake TLS. Regeneré certificados con reiniciar-todo.bat.";
        }
        if (lower.contains("invalidpath") || lower.contains("trailing char")) {
            return "Ruta de archivo inválida. Reinicie el servidor desde iniciar-servidor.bat.";
        }
        if (lower.contains("no route to host") || lower.contains("network is unreachable")) {
            return "No hay ruta al servidor. Verifique la red o la dirección del host.";
        }
        if (lower.contains("closed by the peer") || lower.contains("closed by peer") || lower.contains("forcibly closed")) {
            return "Conexión cerrada por el otro extremo.";
        }
        if (lower.contains("conexión cerrada por el peer")) {
            return "Conexión cerrada por el servidor.";
        }
        if (lower.contains("no such file") || lower.contains("cannot find")) {
            return "Archivo no encontrado: " + mensaje;
        }
        if (lower.contains("access denied") || lower.contains("permission denied")) {
            return "Acceso denegado. Verifique permisos de archivos o carpetas.";
        }
        if (lower.contains("illegalargumentexception") && lower.contains("bcrypt")) {
            return "Contraseña o hash inválido.";
        }
        if (lower.contains("socket closed")) {
            return "Conexión cerrada.";
        }
        if (lower.contains("not connected")) {
            return "No hay conexión activa con el servidor.";
        }
        if (lower.contains("unable to find valid certification path")) {
            return "No se pudo validar el certificado del servidor.";
        }
        if (lower.contains("hostname") && lower.contains("verify")) {
            return "El nombre del servidor no coincide con el certificado TLS.";
        }

        return mensaje;
    }
}
