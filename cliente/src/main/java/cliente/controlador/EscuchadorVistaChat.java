package cliente.controlador;

import comun.Paquete;
import comun.RegistroMensaje;

import java.util.List;

/**
 * Contrato entre el controlador de chat y la vista Swing.
 */
public interface EscuchadorVistaChat {
    void actualizarUsuarios(List<String> usuarios, String contactoSeleccionado, String mensajeEstado);

    void actualizarConversacion(String contacto);

    void actualizarEstado(String texto);

    void mostrarError(String mensaje);

    void habilitarEnvio(boolean habilitado);

    void limpiarCampoMensaje();

    void refrescarListaContactos();

    void onSesionCerrada();
}
