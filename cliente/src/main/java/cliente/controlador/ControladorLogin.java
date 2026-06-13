package cliente.controlador;

import cliente.modelo.ResultadoAutenticacion;
import cliente.modelo.SesionCliente;
import cliente.modelo.ValidadorCredenciales;
import cliente.servicio.ServicioAutenticacion;
import comun.MensajesError;

/**
 * Orquesta registro e inicio de sesión delegando la red al servicio de autenticación.
 */
public class ControladorLogin {
    private final ServicioAutenticacion servicio;

    public ControladorLogin(ServicioAutenticacion servicio) {
        this.servicio = servicio;
    }

    public ResultadoAutenticacion validar(String usuario, String contrasena) {
        String error = ValidadorCredenciales.validar(usuario, contrasena);
        return error != null ? ResultadoAutenticacion.error(error) : ResultadoAutenticacion.exito(null);
    }

    public ResultadoAutenticacion iniciarSesion(String host, int puerto, String usuario, String contrasena) {
        String error = ValidadorCredenciales.validar(usuario, contrasena);
        if (error != null) {
            return ResultadoAutenticacion.error(error);
        }
        return servicio.iniciarSesion(host, puerto, usuario.trim(), contrasena);
    }

    public ResultadoAutenticacion registrar(String host, int puerto, String usuario, String contrasena) {
        String error = ValidadorCredenciales.validar(usuario, contrasena);
        if (error != null) {
            return ResultadoAutenticacion.error(error);
        }
        return servicio.registrar(host, puerto, usuario.trim(), contrasena);
    }

    public void manejarResultadoLogin(ResultadoAutenticacion resultado, EscuchadorVistaLogin vista) {
        if (resultado == null) {
            vista.establecerOcupado(false, "Operación cancelada");
            return;
        }
        if (resultado.getError() != null) {
            vista.establecerOcupado(false, resultado.getError());
            vista.mostrarError("Error", resultado.getError());
            return;
        }
        vista.establecerOcupado(false, " ");
        vista.onLoginExitoso(resultado.getSesion());
    }

    public void manejarResultadoRegistro(ResultadoAutenticacion resultado, EscuchadorVistaLogin vista) {
        if (resultado.getError() != null) {
            vista.establecerOcupado(false, resultado.getError());
            vista.mostrarError("Error", resultado.getError());
            return;
        }
        vista.establecerOcupado(false, "Cuenta creada. Inicie sesión.");
        vista.mostrarInfo("Registro", resultado.getMensaje());
    }

    public String mensajeErrorInesperado(Exception ex) {
        return MensajesError.aEspanol(ex);
    }
}
