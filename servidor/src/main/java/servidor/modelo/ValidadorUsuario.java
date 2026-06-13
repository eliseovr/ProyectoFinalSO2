package servidor.modelo;

/**
 * Validación de nombres de usuario y credenciales del protocolo del servidor.
 */
public final class ValidadorUsuario {
    private ValidadorUsuario() {
    }

    public static String sanitizarNombreUsuario(String raw) {
        if (raw == null) {
            return null;
        }
        String u = raw.trim().toLowerCase();
        if (u.isEmpty() || !u.matches("[a-z0-9_]{3,20}")) {
            return null;
        }
        return u;
    }

    public static String validarCredenciales(String usuario, String contrasena, boolean exigirLongitudContrasena) {
        if (usuario == null) {
            return "Usuario inválido o vacío. Use 3-20 caracteres: a-z, 0-9 y guión bajo (_).";
        }
        if (contrasena == null || contrasena.isEmpty()) {
            return "La contraseña no puede estar vacía.";
        }
        if (exigirLongitudContrasena && contrasena.length() < 6) {
            return "La contraseña debe tener al menos 6 caracteres.";
        }
        return null;
    }
}
