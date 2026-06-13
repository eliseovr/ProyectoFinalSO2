package cliente.servicio;

import cliente.ConexionAutenticacion;
import cliente.ConexionServidor;
import cliente.UtilidadClavesCliente;
import cliente.modelo.ResultadoAutenticacion;
import cliente.modelo.SesionCliente;
import comun.GestorCifradoSesion;
import comun.MensajesError;
import comun.Paquete;
import comun.TipoPaquete;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Operaciones de registro e inicio de sesión contra el servidor TLS.
 */
public class ServicioAutenticacion {
    private final Path rutaAlmacenServidor;

    public ServicioAutenticacion(Path rutaAlmacenServidor) {
        this.rutaAlmacenServidor = rutaAlmacenServidor;
    }

    public ConexionAutenticacion abrirConexion(String host, int puerto) throws Exception {
        if (!Files.exists(rutaAlmacenServidor)) {
            throw new IOException("No existe el certificado TLS: " + rutaAlmacenServidor.toAbsolutePath());
        }
        ConexionAutenticacion auth = new ConexionAutenticacion(host, puerto, rutaAlmacenServidor);
        auth.conectar();
        return auth;
    }

    public ResultadoAutenticacion iniciarSesion(String host, int puerto, String usuario, String contrasena) {
        try (ConexionAutenticacion auth = abrirConexion(host, puerto)) {
            Paquete req = Paquete.of(TipoPaquete.INICIO_SESION);
            req.setUsuario(usuario);
            req.setContrasena(contrasena);
            Paquete res = auth.solicitar(req);
            if (res.getTipo() == TipoPaquete.ERROR) {
                return ResultadoAutenticacion.error(res.getMensaje() != null ? res.getMensaje() : "No se pudo iniciar sesión");
            }
            if (res.getTipo() != TipoPaquete.OK) {
                return ResultadoAutenticacion.error("Respuesta inesperada del servidor");
            }
            if (res.getToken() == null || res.getToken().isEmpty()) {
                return ResultadoAutenticacion.error("El servidor no devolvió un token de sesión");
            }
            String nombreUsuario = res.getUsuario() != null ? res.getUsuario() : usuario;
            ConexionServidor conexion = auth.aConexionSesion();
            GestorCifradoSesion gestor = UtilidadClavesCliente.prepararTrasLogin(
                    nombreUsuario, contrasena, conexion, res.getToken());
            return ResultadoAutenticacion.exito(new SesionCliente(conexion, nombreUsuario, res.getToken(), gestor));
        } catch (Exception ex) {
            return ResultadoAutenticacion.error(formatearErrorConexion(ex));
        }
    }

    public ResultadoAutenticacion registrar(String host, int puerto, String usuario, String contrasena) {
        try (ConexionAutenticacion auth = abrirConexion(host, puerto)) {
            GestorCifradoSesion gestor = UtilidadClavesCliente.prepararTrasRegistro(usuario, contrasena);
            Paquete req = Paquete.of(TipoPaquete.REGISTRO);
            req.setUsuario(usuario);
            req.setContrasena(contrasena);
            req.setClavePublica(gestor.exportarClavePublicaPropia());
            Paquete res = auth.solicitar(req);
            if (res.getTipo() == TipoPaquete.ERROR) {
                return ResultadoAutenticacion.error(res.getMensaje() != null ? res.getMensaje() : "Registro rechazado");
            }
            String mensaje = res.getMensaje() != null ? res.getMensaje() : "Cuenta creada. Ya puede iniciar sesión.";
            return ResultadoAutenticacion.registroOk(mensaje);
        } catch (Exception ex) {
            return ResultadoAutenticacion.error(MensajesError.aEspanol(ex));
        }
    }

    /**
     * Registra si la cuenta no existe; ignora el error de duplicado.
     */
    public ResultadoAutenticacion registrarSiNecesario(String host, int puerto, String usuario, String contrasena) {
        ResultadoAutenticacion resultado = registrar(host, puerto, usuario, contrasena);
        if (resultado.esExito()) {
            return resultado;
        }
        if (resultado.getError() != null && resultado.getError().contains("ya existe")) {
            return ResultadoAutenticacion.registroOk("Usuario existente");
        }
        return resultado;
    }

    public static String formatearErrorConexion(Exception ex) {
        String msg = MensajesError.aEspanol(ex);
        if (msg.contains("certificado") || msg.contains("TLS") || msg.contains("PKIX")) {
            return msg + System.lineSeparator() + System.lineSeparator()
                    + "Solución: reiniciar-todo.bat → iniciar-servidor.bat → iniciar-cliente.bat";
        }
        if (msg.contains("rechazada") || msg.contains("Tiempo de espera")) {
            return msg + System.lineSeparator() + System.lineSeparator()
                    + "Verifique que el servidor esté activo (iniciar-servidor.bat).";
        }
        return msg;
    }
}
