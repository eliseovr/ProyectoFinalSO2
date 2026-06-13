package cliente.modelo;

import cliente.ConexionServidor;
import comun.GestorCifradoSesion;

/**
 * Estado de una sesión autenticada en el cliente.
 */
public final class SesionCliente {
    private final ConexionServidor conexion;
    private final String nombreUsuario;
    private final String token;
    private final GestorCifradoSesion cifrado;

    public SesionCliente(ConexionServidor conexion, String nombreUsuario, String token, GestorCifradoSesion cifrado) {
        this.conexion = conexion;
        this.nombreUsuario = nombreUsuario;
        this.token = token;
        this.cifrado = cifrado;
    }

    public ConexionServidor getConexion() {
        return conexion;
    }

    public String getNombreUsuario() {
        return nombreUsuario;
    }

    public String getToken() {
        return token;
    }

    public GestorCifradoSesion getCifrado() {
        return cifrado;
    }
}
