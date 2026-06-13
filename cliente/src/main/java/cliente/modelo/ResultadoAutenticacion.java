package cliente.modelo;

/**
 * Resultado de un intento de inicio de sesión o registro.
 */
public final class ResultadoAutenticacion {
    private final SesionCliente sesion;
    private final String mensaje;
    private final String error;

    private ResultadoAutenticacion(SesionCliente sesion, String mensaje, String error) {
        this.sesion = sesion;
        this.mensaje = mensaje;
        this.error = error;
    }

    public static ResultadoAutenticacion exito(SesionCliente sesion) {
        return new ResultadoAutenticacion(sesion, null, null);
    }

    public static ResultadoAutenticacion registroOk(String mensaje) {
        return new ResultadoAutenticacion(null, mensaje, null);
    }

    public static ResultadoAutenticacion error(String error) {
        return new ResultadoAutenticacion(null, null, error);
    }

    public SesionCliente getSesion() {
        return sesion;
    }

    public String getMensaje() {
        return mensaje;
    }

    public String getError() {
        return error;
    }

    public boolean esExito() {
        return error == null;
    }
}
