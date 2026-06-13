package cliente;

import cliente.ConexionServidor;
import comun.AlmacenClavePrivada;
import comun.CifradoE2e;
import comun.GestorCifradoSesion;
import comun.Paquete;
import comun.TipoPaquete;

import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.spec.RSAPublicKeySpec;

/**
 * Genera y publica claves RSA del cliente para cifrado E2E.
 * Tras el registro crea un par nuevo; tras el login reutiliza o regenera la clave local.
 */
public final class UtilidadClavesCliente {
    private UtilidadClavesCliente() {
    }

    /** Genera par RSA, lo guarda cifrado localmente y devuelve el gestor de sesión. */
    public static GestorCifradoSesion prepararTrasRegistro(String nombreUsuario, String contrasena) throws Exception {
        KeyPair par = CifradoE2e.generarParClaves();
        AlmacenClavePrivada.guardar(nombreUsuario, contrasena, par.getPrivate());
        return new GestorCifradoSesion(nombreUsuario, par.getPrivate(), par.getPublic());
    }

    /**
     * Carga o crea claves locales, publica la clave pública en el servidor
     * y devuelve el {@link GestorCifradoSesion} listo para cifrar mensajes.
     */
    public static GestorCifradoSesion prepararTrasLogin(
            String nombreUsuario, String contrasena, ConexionServidor conexion, String token) throws Exception {
        String usuario = nombreUsuario.toLowerCase();
        PrivateKey privada;
        PublicKey publica;
        if (AlmacenClavePrivada.existe(usuario)) {
            privada = AlmacenClavePrivada.cargar(usuario, contrasena);
            if (privada == null) {
                throw new IllegalStateException("Contraseña incorrecta para la clave privada local");
            }
            publica = publicaDesdePrivada(privada);
        } else {
            KeyPair par = CifradoE2e.generarParClaves();
            privada = par.getPrivate();
            publica = par.getPublic();
            AlmacenClavePrivada.guardar(usuario, contrasena, privada);
        }
        GestorCifradoSesion gestor = new GestorCifradoSesion(usuario, privada, publica);
        Paquete req = Paquete.of(TipoPaquete.ACTUALIZAR_CLAVE_PUBLICA);
        req.setToken(token);
        req.setClavePublica(gestor.exportarClavePublicaPropia());
        Paquete res = conexion.enviarYEsperar(req, 10_000);
        if (res.getTipo() == TipoPaquete.ERROR) {
            throw new IllegalStateException(res.getMensaje());
        }
        return gestor;
    }

    /** Deriva la clave pública RSA a partir de los parámetros CRT de la privada. */
    private static PublicKey publicaDesdePrivada(PrivateKey privada) throws Exception {
        RSAPrivateCrtKey rsa = (RSAPrivateCrtKey) privada;
        RSAPublicKeySpec spec = new RSAPublicKeySpec(rsa.getModulus(), rsa.getPublicExponent());
        return KeyFactory.getInstance("RSA").generatePublic(spec);
    }
}
