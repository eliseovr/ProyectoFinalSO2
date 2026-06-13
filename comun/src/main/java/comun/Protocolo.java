package comun;

/**
 * Constantes del protocolo de mensajería cliente-servidor.
 * Use {@link EntradaSalidaConexion} para enviar y recibir {@link Paquete}s por socket.
 */
public final class Protocolo {
    /** Puerto TCP predeterminado del servidor (TLS). */
    public static final int PUERTO_PREDETERMINADO = 9443;

    private Protocolo() {
    }
}
