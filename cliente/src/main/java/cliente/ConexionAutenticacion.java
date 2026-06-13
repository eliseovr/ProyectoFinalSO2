package cliente;

import comun.EntradaSalidaConexion;
import comun.Paquete;
import comun.ConfiguracionTls;

import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;
import java.nio.file.Path;

/**
 * Conexión TLS puntual para registro e inicio de sesión (sin hilo lector en segundo plano).
 * Tras autenticarse, el socket puede transferirse a {@link ConexionServidor}.
 */
public class ConexionAutenticacion implements AutoCloseable {
    private final String host;
    private final int puerto;
    private final Path rutaKeystoreServidor;
    private EntradaSalidaConexion entradaSalida;

    public ConexionAutenticacion(String host, int puerto, Path rutaKeystoreServidor) {
        this.host = host;
        this.puerto = puerto;
        this.rutaKeystoreServidor = rutaKeystoreServidor;
    }

    /** Establece handshake TLS con el servidor usando el keystore de confianza. */
    public void conectar() throws Exception {
        SSLSocketFactory fabrica = ConfiguracionTls.contextoCliente(rutaKeystoreServidor, host).getSocketFactory();
        SSLSocket socketSsl = (SSLSocket) fabrica.createSocket(host, puerto);
        socketSsl.setEnabledProtocols(new String[]{"TLSv1.2", "TLSv1.3"});
        SSLParameters parametrosSsl = socketSsl.getSSLParameters();
        parametrosSsl.setEndpointIdentificationAlgorithm(null);
        socketSsl.setSSLParameters(parametrosSsl);
        socketSsl.startHandshake();
        entradaSalida = new EntradaSalidaConexion(socketSsl);
    }

    /** Envía un paquete y bloquea hasta recibir la respuesta del servidor. */
    public Paquete solicitar(Paquete paquete) throws IOException {
        if (entradaSalida == null) {
            throw new IOException("No conectado al servidor");
        }
        entradaSalida.enviar(paquete);
        return entradaSalida.recibir();
    }

    public ConexionServidor aConexionSesion() throws Exception {
        ConexionServidor sesion = new ConexionServidor(host, puerto, rutaKeystoreServidor);
        sesion.adoptarSocketAbierto(entradaSalida.socket());
        entradaSalida = null;
        return sesion;
    }

    @Override
    public void close() {
        if (entradaSalida != null) {
            try {
                entradaSalida.close();
            } catch (IOException ignored) {
            }
            entradaSalida = null;
        }
    }
}
