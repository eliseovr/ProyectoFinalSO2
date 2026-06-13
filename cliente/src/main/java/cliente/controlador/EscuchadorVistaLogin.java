package cliente.controlador;

import cliente.modelo.ResultadoAutenticacion;
import cliente.modelo.SesionCliente;
import cliente.modelo.ValidadorCredenciales;
import cliente.servicio.ServicioAutenticacion;

/**
 * Contrato entre el controlador de login y la vista Swing.
 */
public interface EscuchadorVistaLogin {
    void establecerOcupado(boolean ocupado, String textoEstado);

    void mostrarAdvertencia(String titulo, String mensaje);

    void mostrarError(String titulo, String mensaje);

    void mostrarInfo(String titulo, String mensaje);

    void onLoginExitoso(SesionCliente sesion);
}
