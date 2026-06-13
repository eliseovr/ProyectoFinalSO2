package cliente;

import cliente.modelo.ResultadoAutenticacion;
import cliente.modelo.SesionCliente;
import cliente.servicio.ServicioAutenticacion;
import cliente.servicio.ServicioMensajeria;
import comun.RutasAplicacion;
import comun.RegistroMensaje;
import comun.MensajesError;
import comun.Protocolo;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Cliente de consola para pruebas automatizadas (sin interfaz gráfica).
 * Reutiliza los servicios de la capa MVC.
 */
public final class ClienteConsola {
    private static final int REINTENTOS_CONEXION = 30;
    private static final long RETRASO_CONEXION_MS = 2_000;
    private static final long ESPERA_MENSAJES_MS = 15_000;

    private ClienteConsola() {
    }

    /**
     * Ejecuta el flujo completo leyendo host, puerto, usuario y mensaje desde variables de entorno.
     */
    public static void ejecutar(String[] args) throws Exception {
        Path keystore = RutasAplicacion.resolver("KEYSTORE_PATH", "certificados/server.p12");
        String host = entorno("SERVER_HOST", "localhost");
        int puerto = Integer.parseInt(entorno("SERVER_PORT", String.valueOf(Protocolo.PUERTO_PREDETERMINADO)));
        String nombreUsuario = entorno("CLIENT_USER", "eliseo");
        String contrasena = entorno("CLIENT_PASSWORD", "eliseo123");
        String destinatario = entorno("CLIENT_PEER", "eliseo2");
        String contenido = entorno("CLIENT_MESSAGE", "Mensaje de prueba desde consola");

        if (!Files.exists(keystore)) {
            throw new IllegalStateException("No existe el certificado TLS: " + keystore.toAbsolutePath());
        }

        System.out.println("[ClienteConsola] Conectando a " + host + ":" + puerto + " como " + nombreUsuario);
        ServicioAutenticacion servicioAuth = new ServicioAutenticacion(keystore);
        esperarServidor(servicioAuth, host, puerto);

        ResultadoAutenticacion registro = servicioAuth.registrarSiNecesario(host, puerto, nombreUsuario, contrasena);
        if (!registro.esExito()) {
            throw new IllegalStateException("Registro fallido: " + registro.getError());
        }
        if (registro.getMensaje() != null) {
            System.out.println("[ClienteConsola] " + registro.getMensaje() + ": " + nombreUsuario);
        }

        ResultadoAutenticacion login = servicioAuth.iniciarSesion(host, puerto, nombreUsuario, contrasena);
        if (!login.esExito() || login.getSesion() == null) {
            throw new IllegalStateException("Login fallido: " + login.getError());
        }

        SesionCliente sesion = login.getSesion();
        ServicioMensajeria servicio = new ServicioMensajeria(sesion);
        servicio.getConexion().setEscuchadorMensajes(paquete -> {
            if (paquete.getTipo() == comun.TipoPaquete.MENSAJE && paquete.getMensajes() != null) {
                for (RegistroMensaje msg : paquete.getMensajes()) {
                    System.out.println("[ClienteConsola] Mensaje recibido de " + msg.getRemitente() + ": "
                            + sesion.getCifrado().descifrarParaMostrar(msg));
                }
            }
        });
        System.out.println("[ClienteConsola] Sesión iniciada (E2E activo)");

        try {
            if (!destinatario.isEmpty()) {
                servicio.registrarClavesDestinatario(destinatario);
                servicio.enviarMensaje(destinatario, contenido);
                System.out.println("[ClienteConsola] Mensaje E2E enviado a " + destinatario);
            }
            Thread.sleep(ESPERA_MENSAJES_MS);
        } finally {
            servicio.cerrarSesion();
        }
        System.out.println("[ClienteConsola] Finalizado correctamente");
    }

    private static void esperarServidor(ServicioAutenticacion servicio, String host, int puerto) throws Exception {
        Exception ultimo = null;
        for (int intento = 1; intento <= REINTENTOS_CONEXION; intento++) {
            try {
                servicio.abrirConexion(host, puerto).close();
                System.out.println("[ClienteConsola] Conexión TLS establecida (intento " + intento + ")");
                return;
            } catch (Exception ex) {
                ultimo = ex;
                System.out.println("[ClienteConsola] Servidor no disponible, reintento " + intento + "/" + REINTENTOS_CONEXION);
                Thread.sleep(RETRASO_CONEXION_MS);
            }
        }
        throw new IllegalStateException("No se pudo conectar al servidor: " + MensajesError.aEspanol(ultimo));
    }

    private static String entorno(String clave, String predeterminado) {
        String v = System.getenv(clave);
        return v != null && !v.isBlank() ? v : predeterminado;
    }
}
