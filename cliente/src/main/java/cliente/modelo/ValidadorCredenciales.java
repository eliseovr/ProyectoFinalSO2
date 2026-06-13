package cliente.modelo;

/**
 * Valida usuario y contraseña antes de enviarlos al servidor.
 */
public final class ValidadorCredenciales {
    private ValidadorCredenciales() {
    }

    /**
     * @return mensaje de error en español, o {@code null} si los datos son válidos
     */
    public static String validar(String usuario, String contrasena) {
        if (usuario == null || usuario.trim().isEmpty()) {
            return "El usuario no puede estar vacío.";
        }
        String user = usuario.trim();
        if (!user.toLowerCase().matches("[a-z0-9_]{3,20}")) {
            return "Usuario inválido: use 3-20 caracteres (a-z, 0-9, _).";
        }
        if (contrasena == null || contrasena.isEmpty()) {
            return "La contraseña no puede estar vacía.";
        }
        if (contrasena.length() < 6) {
            return "La contraseña debe tener al menos 6 caracteres.";
        }
        return null;
    }
}
