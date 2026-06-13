package comun;

/**
 * Fachada de alto nivel para hash y verificación de contraseñas con BCrypt.
 * Delega en {@link UtilidadBcrypt} la implementación criptográfica.
 */
public final class HasherContrasena {
    private HasherContrasena() {
    }

    /**
     * Genera un hash BCrypt de la contraseña en texto plano.
     *
     * @param contrasenaPlana contraseña del usuario
     * @return hash almacenable en base de datos
     */
    public static String hash(String contrasenaPlana) {
        return UtilidadBcrypt.hash(contrasenaPlana);
    }

    /**
     * Comprueba si la contraseña plana coincide con el hash almacenado.
     *
     * @param contrasenaPlana contraseña introducida
     * @param hashAlmacenado hash persistido (BCrypt)
     * @return {@code true} si coinciden
     */
    public static boolean verificar(String contrasenaPlana, String hashAlmacenado) {
        if (hashAlmacenado == null || hashAlmacenado.trim().isEmpty()) {
            return false;
        }
        return UtilidadBcrypt.verificar(contrasenaPlana, hashAlmacenado);
    }
}
