package cliente;

import comun.Paquete;

/**
 * Callback funcional invocado cuando el hilo de red recibe un {@link Paquete}
 * del servidor. Permite desacoplar la lectura del socket de la lógica de la UI.
 */
@FunctionalInterface
public interface EscuchadorMensajes {

    /**
     * Procesa un paquete recibido desde el servidor.
     *
     * @param paquete paquete deserializado de la conexión
     */
    void onPaquete(Paquete paquete);
}
