package comun;

import org.mindrot.jbcrypt.BCrypt;

/**
 * Utilidades de hash de contraseñas con la biblioteca jBCrypt.
 * Usa 12 rondas de coste por defecto.
 */
public final class UtilidadBcrypt {
    private static final int RONDAS_LOG = 12;

    private UtilidadBcrypt() {
    }

    /**
     * Calcula el hash BCrypt de una contraseña.
     *
     * @param contrasenaPlana texto en claro
     * @return cadena hash BCrypt con salt incluido
     */
    public static String hash(String contrasenaPlana) {
        return BCrypt.hashpw(contrasenaPlana, BCrypt.gensalt(RONDAS_LOG));
    }

    /**
     * Verifica una contraseña contra un hash BCrypt existente.
     *
     * @param contrasenaPlana contraseña a comprobar
     * @param almacenado hash previamente generado
     * @return {@code true} si la contraseña es correcta
     */
    public static boolean verificar(String contrasenaPlana, String almacenado) {
        if (almacenado == null || almacenado.isEmpty()) {
            return false;
        }
        try {
            return BCrypt.checkpw(contrasenaPlana, almacenado);
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }
}
