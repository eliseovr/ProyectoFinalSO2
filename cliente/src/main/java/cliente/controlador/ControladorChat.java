package cliente.controlador;

import cliente.modelo.ModeloBandeja;
import cliente.servicio.ServicioMensajeria;
import comun.MensajesError;
import comun.Paquete;
import comun.RegistroMensaje;
import comun.TipoPaquete;

import javax.swing.SwingUtilities;
import java.util.List;
import java.util.function.Supplier;

/**
 * Orquesta sincronización, envío y recepción de mensajes usando el modelo de bandeja.
 */
public class ControladorChat {
    private final ServicioMensajeria servicio;
    private final ModeloBandeja modelo;

    public ControladorChat(ServicioMensajeria servicio) {
        this.servicio = servicio;
        this.modelo = new ModeloBandeja(servicio.getNombreUsuario());
    }

    public ModeloBandeja getModelo() {
        return modelo;
    }

    public ServicioMensajeria getServicio() {
        return servicio;
    }

    public void configurarEscuchadorPush(EscuchadorVistaChat vista, Supplier<String> contactoSeleccionado) {
        servicio.getConexion().setEscuchadorMensajes(paquete ->
                SwingUtilities.invokeLater(() ->
                        manejarPaqueteEntrante(paquete, vista, contactoSeleccionado.get())));
    }

    public void cargarUsuarios(EscuchadorVistaChat vista, String contactoSeleccionado) {
        try {
            List<String> usuarios = servicio.cargarUsuarios();
            String estado = usuarios.isEmpty()
                    ? "Registre otro usuario en un segundo cliente."
                    : null;
            vista.actualizarUsuarios(usuarios, contactoSeleccionado, estado);
        } catch (Exception ex) {
            vista.actualizarEstado("Error al cargar usuarios");
        }
    }

    public void cargarBandeja(EscuchadorVistaChat vista, String contactoSeleccionado) {
        try {
            RegistroMensaje[] mensajes = servicio.cargarBandeja();
            modelo.fusionarTodos(mensajes);
            if (contactoSeleccionado != null) {
                vista.actualizarConversacion(contactoSeleccionado);
            }
        } catch (Exception ex) {
            vista.actualizarEstado(ex.getMessage());
        }
    }

    public void enviarMensaje(String destinatario, String contenido, EscuchadorVistaChat vista) {
        if (destinatario == null) {
            vista.mostrarError("Seleccione un destinatario");
            return;
        }
        if (contenido == null || contenido.trim().isEmpty()) {
            return;
        }
        vista.habilitarEnvio(false);
        try {
            RegistroMensaje enviado = servicio.enviarMensaje(destinatario, contenido.trim());
            modelo.fusionar(enviado);
            vista.limpiarCampoMensaje();
            vista.actualizarConversacion(destinatario);
            vista.actualizarEstado("Enviado");
        } catch (Exception ex) {
            vista.mostrarError(MensajesError.aEspanol(ex));
        } finally {
            vista.habilitarEnvio(true);
        }
    }

    public void seleccionarContacto(String contacto, EscuchadorVistaChat vista) {
        modelo.marcarLeido(contacto);
        vista.refrescarListaContactos();
        vista.actualizarConversacion(contacto);
    }

    public void cerrarSesion(EscuchadorVistaChat vista) {
        servicio.cerrarSesion();
        vista.onSesionCerrada();
    }

    private void manejarPaqueteEntrante(Paquete paquete, EscuchadorVistaChat vista, String contactoSeleccionadoRef) {
        if (paquete.getTipo() == TipoPaquete.MENSAJE && paquete.getMensajes() != null) {
            for (RegistroMensaje m : paquete.getMensajes()) {
                manejarMensajeEntrante(m, vista, contactoSeleccionadoRef);
            }
        } else if (paquete.getTipo() == TipoPaquete.ERROR) {
            vista.actualizarEstado(paquete.getMensaje());
        }
    }

    private void manejarMensajeEntrante(RegistroMensaje mensajeEntrante, EscuchadorVistaChat vista, String contactoSeleccionado) {
        RegistroMensaje mensaje = modelo.normalizarMarcaTiempo(mensajeEntrante);
        modelo.fusionar(mensaje);
        String contacto = modelo.contactoDe(mensaje);
        if (contacto == null) {
            return;
        }
        if (contacto.equalsIgnoreCase(contactoSeleccionado)) {
            vista.actualizarConversacion(contacto);
            vista.actualizarEstado("Nuevo mensaje");
        } else {
            modelo.incrementarNoLeido(contacto);
            vista.refrescarListaContactos();
            vista.actualizarEstado("Nuevo mensaje de " + contacto);
        }
    }
}
