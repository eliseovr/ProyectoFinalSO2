package servidor.modelo;

/**
 * Categorías de filtrado del registro de eventos del monitor.
 */
public enum FiltroEvento {
    TODOS("Todos los eventos"),
    CONEXION("Conexiones"),
    AUTENTICACION("Autenticación"),
    MENSAJES("Mensajes"),
    ERRORES("Errores y advertencias");

    private final String etiqueta;

    FiltroEvento(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    @Override
    public String toString() {
        return etiqueta;
    }
}
