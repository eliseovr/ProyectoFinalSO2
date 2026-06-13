package cliente.servicio;

import cliente.ConexionServidor;
import cliente.modelo.ModeloBandeja;
import cliente.modelo.SesionCliente;
import comun.GestorCifradoSesion;
import comun.Paquete;
import comun.RegistroMensaje;
import comun.TipoPaquete;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Operaciones de mensajería: usuarios, bandeja, envío y cierre de sesión.
 */
public class ServicioMensajeria {
    private static final DateTimeFormatter FORMATO_FECHA_SERVIDOR = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final SesionCliente sesion;

    public ServicioMensajeria(SesionCliente sesion) {
        this.sesion = sesion;
    }

    public SesionCliente getSesion() {
        return sesion;
    }

    public ConexionServidor getConexion() {
        return sesion.getConexion();
    }

    public String getToken() {
        return sesion.getToken();
    }

    public GestorCifradoSesion getCifrado() {
        return sesion.getCifrado();
    }

    public String getNombreUsuario() {
        return sesion.getNombreUsuario();
    }

    public List<String> cargarUsuarios() throws Exception {
        Paquete req = Paquete.of(TipoPaquete.USUARIOS);
        req.setToken(sesion.getToken());
        Paquete res = sesion.getConexion().enviarYEsperar(req, 10_000);
        if (res.getTipo() == TipoPaquete.ERROR) {
            throw new IllegalStateException(res.getMensaje());
        }
        if (res.getTipo() == TipoPaquete.LISTA_USUARIOS && res.getUsuarios() != null) {
            sesion.getCifrado().registrarDesdeListas(res.getUsuarios(), res.getClavesPublicas());
            return Arrays.stream(res.getUsuarios())
                    .filter(u -> !u.equalsIgnoreCase(sesion.getNombreUsuario()))
                    .collect(Collectors.toList());
        }
        return List.of();
    }

    public RegistroMensaje[] cargarBandeja() throws Exception {
        Paquete req = Paquete.of(TipoPaquete.BANDEJA);
        req.setToken(sesion.getToken());
        Paquete res = sesion.getConexion().enviarYEsperar(req, 10_000);
        if (res.getTipo() == TipoPaquete.ERROR) {
            throw new IllegalStateException(res.getMensaje());
        }
        return res.getMensajes();
    }

    public RegistroMensaje enviarMensaje(String destinatario, String contenido) throws Exception {
        String contenidoCifrado = sesion.getCifrado().cifrarParaEnvio(destinatario, contenido);
        Paquete req = Paquete.of(TipoPaquete.ENVIAR);
        req.setToken(sesion.getToken());
        req.setDestinatario(destinatario);
        req.setContenido(contenidoCifrado);
        Paquete res = sesion.getConexion().enviarYEsperar(req, 10_000);
        if (res.getTipo() == TipoPaquete.ERROR) {
            throw new IllegalStateException(res.getMensaje());
        }
        if (res.getMensajes() != null && res.getMensajes().length > 0) {
            return res.getMensajes()[0];
        }
        return new RegistroMensaje(
                -System.nanoTime(),
                sesion.getNombreUsuario(),
                destinatario,
                contenidoCifrado,
                FORMATO_FECHA_SERVIDOR.format(LocalDateTime.now()));
    }

    public void cerrarSesion() {
        try {
            Paquete req = Paquete.of(TipoPaquete.CIERRE_SESION);
            req.setToken(sesion.getToken());
            sesion.getConexion().enviarYEsperar(req, 5_000);
        } catch (Exception ignored) {
        } finally {
            sesion.getConexion().close();
        }
    }

    public void registrarClavesDestinatario(String destinatario) throws Exception {
        cargarUsuarios();
    }
}
