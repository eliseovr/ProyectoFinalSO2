package comun;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 * Lectura y escritura de paquetes JSON sobre un socket TCP.
 * Usa un único par de streams por conexión y sincroniza las escrituras.
 */
public final class EntradaSalidaConexion implements AutoCloseable {
    private final BufferedReader lector;
    private final BufferedWriter escritor;
    private final Socket socket;
    private final Object bloqueoEscritura = new Object();

    /**
     * Abre streams de texto UTF-8 sobre el socket dado.
     *
     * @param socket conexión TCP ya establecida
     * @throws IOException si no se pueden abrir los streams
     */
    public EntradaSalidaConexion(Socket socket) throws IOException {
        this.socket = socket;
        this.lector = new BufferedReader(
                new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        this.escritor = new BufferedWriter(
                new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
    }

    /**
     * Envía un paquete como una línea JSON terminada en nueva línea.
     *
     * @param paquete paquete a transmitir
     * @throws IOException si falla la escritura
     */
    public void enviar(Paquete paquete) throws IOException {
        synchronized (bloqueoEscritura) {
            escritor.write(Paquete.aJson(paquete));
            escritor.newLine();
            escritor.flush();
        }
    }

    /**
     * Lee la siguiente línea JSON y la convierte en paquete.
     *
     * @return paquete recibido
     * @throws IOException si la conexión se cerró o la línea está vacía
     */
    public Paquete recibir() throws IOException {
        String linea = lector.readLine();
        if (linea == null || linea.trim().isEmpty()) {
            throw new IOException("Conexión cerrada por el servidor");
        }
        return Paquete.desdeJson(linea);
    }

    /**
     * Devuelve el socket subyacente de esta conexión.
     *
     * @return socket TCP
     */
    public Socket socket() {
        return socket;
    }

    /** Cierra lector, escritor y socket, ignorando errores parciales. */
    @Override
    public void close() throws IOException {
        try {
            lector.close();
        } catch (IOException ignored) {
        }
        try {
            escritor.close();
        } catch (IOException ignored) {
        }
        try {
            socket.close();
        } catch (IOException ignored) {
        }
    }
}
