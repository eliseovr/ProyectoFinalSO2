package servidor;

import comun.CifradoE2e;
import comun.EntradaSalidaConexion;
import comun.MensajesError;
import comun.RegistroMensaje;
import comun.Paquete;
import comun.TipoPaquete;

import servidor.modelo.ValidadorUsuario;

import javax.net.ssl.SSLSocket;
import java.io.IOException;
import java.net.Socket;
import java.util.List;

/**
 * Atiende un cliente conectado de forma concurrente.
 * Procesa el protocolo de paquetes (registro, sesión, mensajes E2E, bandeja, usuarios)
 * y mantiene el estado de la sesión TLS asociada.
 */
public class ManejadorCliente implements Runnable {
    private final Socket socket;
    private final BaseDatosSqlite baseDatos;
    private final GestorSesiones sesiones;
    private final RegistroActividad registro;

    private EntradaSalidaConexion entradaSalida;
    private String tokenSesion;
    private String nombreUsuario;
    private volatile boolean activo = true;
    private volatile boolean cierreSesionExplicito = false;

    /** @param socket   conexión TLS ya aceptada por el servidor */
    public ManejadorCliente(Socket socket, BaseDatosSqlite baseDatos, GestorSesiones sesiones, RegistroActividad registro) {
        this.socket = socket;
        this.baseDatos = baseDatos;
        this.sesiones = sesiones;
        this.registro = registro;
    }

    /** Bucle principal: handshake TLS, recepción de paquetes y cierre ordenado. */
    @Override
    public void run() {
        String remoto = socket.getRemoteSocketAddress().toString();
        registro.info("CONEXION nueva desde " + remoto);
        try {
            if (socket instanceof SSLSocket) {
                ((SSLSocket) socket).startHandshake();
            }
            entradaSalida = new EntradaSalidaConexion(socket);
            while (activo && !socket.isClosed()) {
                Paquete solicitud = entradaSalida.recibir();
                manejar(solicitud);
            }
        } catch (IOException ex) {
            registro.warn("CONEXION cerrada " + remoto + ": " + MensajesError.aEspanol(ex));
        } finally {
            cerrarSesionSilenciosamente();
            cerrarSocket();
        }
    }

    /** Enruta un paquete recibido al manejador correspondiente según su {@link TipoPaquete}. */
    private void manejar(Paquete solicitud) throws IOException {
        TipoPaquete tipo = solicitud.getTipo();
        if (tipo == null) {
            entradaSalida.enviar(Paquete.error("Tipo de paquete inválido"));
            return;
        }
        switch (tipo) {
            case REGISTRO:
                manejarRegistro(solicitud);
                break;
            case INICIO_SESION:
                manejarInicioSesion(solicitud);
                break;
            case CIERRE_SESION:
                manejarCierreSesion(solicitud);
                break;
            case ENVIAR:
                manejarEnvio(solicitud);
                break;
            case BANDEJA:
                manejarBandeja(solicitud);
                break;
            case USUARIOS:
                manejarUsuarios(solicitud);
                break;
            case ACTUALIZAR_CLAVE_PUBLICA:
                manejarActualizarClavePublica(solicitud);
                break;
            case PING:
                entradaSalida.enviar(Paquete.ok("pong"));
                break;
            default:
                entradaSalida.enviar(Paquete.error("Comando no soportado: " + tipo));
        }
    }

    private void manejarRegistro(Paquete solicitud) throws IOException {
        String usuario = ValidadorUsuario.sanitizarNombreUsuario(solicitud.getUsuario());
        String contrasena = solicitud.getContrasena();
        String errorValidacion = ValidadorUsuario.validarCredenciales(usuario, contrasena, true);
        if (errorValidacion != null) {
            entradaSalida.enviar(Paquete.error(errorValidacion));
            return;
        }
        String clavePublica = solicitud.getClavePublica();
        if (clavePublica == null || clavePublica.isBlank()) {
            entradaSalida.enviar(Paquete.error("Se requiere clave pública para cifrado de extremo a extremo"));
            return;
        }
        try {
            if (!baseDatos.registrarUsuario(usuario, contrasena, clavePublica.trim())) {
                entradaSalida.enviar(Paquete.error("El usuario ya existe"));
                registro.warn("REGISTRO fallido (duplicado): " + usuario);
                return;
            }
            registro.info("REGISTRO exitoso: " + usuario);
            entradaSalida.enviar(Paquete.ok("Cuenta creada correctamente"));
        } catch (Exception ex) {
            registro.error("REGISTRO: error interno", ex);
            entradaSalida.enviar(Paquete.error("Error al registrar usuario"));
        }
    }

    private void manejarInicioSesion(Paquete solicitud) throws IOException {
        String usuario = ValidadorUsuario.sanitizarNombreUsuario(solicitud.getUsuario());
        String contrasena = solicitud.getContrasena();
        String errorValidacion = ValidadorUsuario.validarCredenciales(usuario, contrasena, false);
        if (errorValidacion != null) {
            entradaSalida.enviar(Paquete.error(errorValidacion));
            return;
        }
        try {
            if (!baseDatos.autenticar(usuario, contrasena)) {
                entradaSalida.enviar(Paquete.error("Usuario o contraseña incorrectos"));
                registro.warn("INICIO_SESION fallido: " + usuario);
                return;
            }
            tokenSesion = sesiones.crearSesion(usuario);
            nombreUsuario = usuario;
            sesiones.registrarEnLinea(usuario, this);
            Paquete ok = Paquete.ok("Sesión iniciada");
            ok.setToken(tokenSesion);
            ok.setUsuario(usuario);
            entradaSalida.enviar(ok);
            registro.info("INICIO_SESION exitoso: " + usuario);
        } catch (Exception ex) {
            registro.error("INICIO_SESION: error interno", ex);
            entradaSalida.enviar(Paquete.error("Error de autenticación"));
        }
    }

    private void manejarCierreSesion(Paquete solicitud) throws IOException {
        if (!autorizar(solicitud)) {
            return;
        }
        entradaSalida.enviar(Paquete.ok("Sesión cerrada"));
        registro.info("CIERRE_SESION: " + nombreUsuario);
        cierreSesionExplicito = true;
        activo = false;
        cerrarSesionSilenciosamente();
    }

    private void manejarEnvio(Paquete solicitud) throws IOException {
        if (!autorizar(solicitud)) {
            return;
        }
        String destinatario = ValidadorUsuario.sanitizarNombreUsuario(solicitud.getDestinatario());
        String contenido = solicitud.getContenido();
        if (destinatario == null || contenido == null || contenido.trim().isEmpty()) {
            entradaSalida.enviar(Paquete.error("Destinatario y mensaje requeridos"));
            return;
        }
        if (!CifradoE2e.esContenidoCifrado(contenido)) {
            entradaSalida.enviar(Paquete.error("El mensaje debe ir cifrado de extremo a extremo (E2E)"));
            return;
        }
        if (destinatario.equalsIgnoreCase(nombreUsuario)) {
            entradaSalida.enviar(Paquete.error("No puedes enviarte mensajes a ti mismo"));
            return;
        }
        try {
            if (!baseDatos.existeUsuario(destinatario)) {
                entradaSalida.enviar(Paquete.error("El destinatario no existe"));
                return;
            }
            RegistroMensaje registroMensaje = baseDatos.guardarMensaje(nombreUsuario, destinatario, contenido.trim());
            registro.mensajeEnviado(nombreUsuario, destinatario);

            ManejadorCliente manejadorDestinatario = sesiones.obtenerManejador(destinatario);
            if (manejadorDestinatario != null) {
                Paquete push = Paquete.of(TipoPaquete.MENSAJE);
                push.setMensaje("Nuevo mensaje de " + nombreUsuario);
                push.setMensajes(new RegistroMensaje[]{registroMensaje});
                manejadorDestinatario.enviarPush(push);
                registro.mensajeRecibido(destinatario, nombreUsuario);
            } else {
                registro.mensajePendiente(destinatario, nombreUsuario);
            }
            Paquete ok = Paquete.ok("Mensaje enviado");
            ok.setMensajes(new RegistroMensaje[]{registroMensaje});
            entradaSalida.enviar(ok);
        } catch (Exception ex) {
            registro.error("ENVIO: error interno", ex);
            entradaSalida.enviar(Paquete.error("No se pudo enviar el mensaje"));
        }
    }

    private void manejarBandeja(Paquete solicitud) throws IOException {
        if (!autorizar(solicitud)) {
            return;
        }
        try {
            List<RegistroMensaje> bandeja = baseDatos.bandejaPara(nombreUsuario);
            Paquete respuesta = Paquete.of(TipoPaquete.OK);
            respuesta.setMensaje("Historial de mensajes");
            respuesta.setMensajes(bandeja.toArray(new RegistroMensaje[0]));
            entradaSalida.enviar(respuesta);
        } catch (Exception ex) {
            registro.error("BANDEJA: error interno", ex);
            entradaSalida.enviar(Paquete.error("No se pudo cargar la bandeja"));
        }
    }

    private void manejarUsuarios(Paquete solicitud) throws IOException {
        if (!autorizar(solicitud)) {
            return;
        }
        try {
            List<String> usuarios = baseDatos.listarUsuarios();
            List<String> claves = baseDatos.listarClavesPublicasOrdenadas();
            Paquete respuesta = Paquete.of(TipoPaquete.LISTA_USUARIOS);
            respuesta.setUsuarios(usuarios.toArray(new String[0]));
            respuesta.setClavesPublicas(claves.toArray(new String[0]));
            entradaSalida.enviar(respuesta);
        } catch (Exception ex) {
            registro.error("USUARIOS: error interno", ex);
            entradaSalida.enviar(Paquete.error("No se pudo obtener la lista de usuarios"));
        }
    }

    private void manejarActualizarClavePublica(Paquete solicitud) throws IOException {
        if (!autorizar(solicitud)) {
            return;
        }
        String clavePublica = solicitud.getClavePublica();
        if (clavePublica == null || clavePublica.isBlank()) {
            entradaSalida.enviar(Paquete.error("Clave pública vacía"));
            return;
        }
        try {
            baseDatos.actualizarClavePublica(nombreUsuario, clavePublica.trim());
            entradaSalida.enviar(Paquete.ok("Clave pública actualizada"));
            registro.info("CLAVE_PUBLICA actualizada: " + nombreUsuario);
        } catch (Exception ex) {
            registro.error("CLAVE_PUBLICA: error interno", ex);
            entradaSalida.enviar(Paquete.error("No se pudo guardar la clave pública"));
        }
    }

    private boolean autorizar(Paquete solicitud) throws IOException {
        String token = solicitud.getToken() != null ? solicitud.getToken() : tokenSesion;
        String usuario = sesiones.resolverUsuario(token);
        if (usuario == null || (nombreUsuario != null && !usuario.equalsIgnoreCase(nombreUsuario))) {
            entradaSalida.enviar(Paquete.error("No autorizado. Inicie sesión."));
            return false;
        }
        tokenSesion = token;
        nombreUsuario = usuario;
        return true;
    }

    /** Envía un paquete al cliente sin bloquear el hilo principal del receptor. */
    public void enviarPush(Paquete paquete) {
        try {
            if (entradaSalida != null) {
                entradaSalida.enviar(paquete);
            }
        } catch (IOException ex) {
            registro.warn("PUSH fallido para " + nombreUsuario + ": " + MensajesError.aEspanol(ex));
        }
    }

    private void cerrarSesionSilenciosamente() {
        if (nombreUsuario != null) {
            if (!cierreSesionExplicito) {
                registro.info("CIERRE_SESION (desconexion): " + nombreUsuario);
            }
            sesiones.desregistrarEnLinea(nombreUsuario);
        }
        if (tokenSesion != null) {
            sesiones.invalidar(tokenSesion);
        }
    }

    private void cerrarSocket() {
        if (entradaSalida != null) {
            try {
                entradaSalida.close();
            } catch (IOException ignored) {
            }
        } else {
            try {
                socket.close();
            } catch (IOException ignored) {
            }
        }
    }
}
