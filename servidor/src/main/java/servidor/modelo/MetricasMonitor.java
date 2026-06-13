package servidor.modelo;

/**
 * Contadores agregados del monitor del servidor.
 */
public final class MetricasMonitor {
    public int conexiones;
    public int loginExitoso;
    public int loginFallido;
    public int registros;
    public int mensajesEnviados;
    public int mensajesRecibidos;
    public int advertencias;
    public int errores;

    public void reiniciar() {
        conexiones = 0;
        loginExitoso = 0;
        loginFallido = 0;
        registros = 0;
        mensajesEnviados = 0;
        mensajesRecibidos = 0;
        advertencias = 0;
        errores = 0;
    }
}
