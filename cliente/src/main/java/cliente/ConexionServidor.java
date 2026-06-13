package cliente;

import comun.EntradaSalidaConexion;
import comun.MensajesError;
import comun.Paquete;
import comun.TipoPaquete;
import comun.ConfiguracionTls;

import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Conexión TLS al servidor con hilo lector para mensajes en tiempo real.
 * Separa respuestas síncronas (cola) de notificaciones push (escuchador).
 */
public class ConexionServidor implements AutoCloseable {
    private final String host;
    private final int puerto;
    private final Path rutaKeystoreServidor;

    private EntradaSalidaConexion entradaSalida;
    private Thread hiloLector;
    private volatile boolean conectado;
    private EscuchadorMensajes escuchadorMensajes;

    private final BlockingQueue<Paquete> colaRespuestas = new LinkedBlockingQueue<>();
    private final AtomicReference<Thread> hiloEspera = new AtomicReference<>();
    private final Object bloqueoSolicitud = new Object();

    public ConexionServidor(String host, int puerto, Path rutaKeystoreServidor) {
        this.host = host;
        this.puerto = puerto;
        this.rutaKeystoreServidor = rutaKeystoreServidor;
    }

    /** Abre un socket TLS nuevo e inicia el hilo lector en segundo plano. */
    public void conectar() throws Exception {
        SSLSocketFactory fabrica = ConfiguracionTls.contextoCliente(rutaKeystoreServidor, host).getSocketFactory();
        SSLSocket socketSsl = (SSLSocket) fabrica.createSocket(host, puerto);
        prepararSocket(socketSsl);
        adoptarSocketAbierto(socketSsl);
    }

    /** Reutiliza un socket ya autenticado por TLS (tras login con {@link ConexionAutenticacion}). */
    public void adoptarSocketAbierto(java.net.Socket socket) throws IOException {
        if (conectado) {
            close();
        }
        entradaSalida = new EntradaSalidaConexion(socket);
        conectado = true;
        hiloLector = new Thread(this::bucleLectura, "lector-cliente");
        hiloLector.setDaemon(true);
        hiloLector.start();
    }

    private void prepararSocket(SSLSocket socketSsl) throws Exception {
        socketSsl.setEnabledProtocols(new String[]{"TLSv1.2", "TLSv1.3"});
        SSLParameters parametrosSsl = socketSsl.getSSLParameters();
        parametrosSsl.setEndpointIdentificationAlgorithm(null);
        socketSsl.setSSLParameters(parametrosSsl);
        socketSsl.startHandshake();
    }

    /** Registra el callback para paquetes push (mensajes entrantes, errores de conexión). */
    public void setEscuchadorMensajes(EscuchadorMensajes escuchador) {
        this.escuchadorMensajes = escuchador;
    }

    /**
     * Envía una solicitud y espera la respuesta correlacionada en la cola interna.
     * @param tiempoEsperaMs tiempo máximo de espera en milisegundos
     */
    public Paquete enviarYEsperar(Paquete solicitud, long tiempoEsperaMs) throws IOException, InterruptedException {
        synchronized (bloqueoSolicitud) {
            if (!conectado) {
                throw new IOException("No conectado al servidor");
            }
            Thread espera = Thread.currentThread();
            hiloEspera.set(espera);
            colaRespuestas.clear();
            try {
                entradaSalida.enviar(solicitud);
                Paquete respuesta = colaRespuestas.poll(tiempoEsperaMs, TimeUnit.MILLISECONDS);
                if (respuesta == null) {
                    throw new IOException("Tiempo de espera agotado (el servidor no respondió)");
                }
                return respuesta;
            } finally {
                hiloEspera.compareAndSet(espera, null);
            }
        }
    }

    public void enviar(Paquete solicitud) throws IOException {
        entradaSalida.enviar(solicitud);
    }

    /** Hilo lector: enruta MENSAJE al escuchador; demás paquetes a la cola de respuesta. */
    private void bucleLectura() {
        try {
            while (conectado) {
                Paquete paquete = entradaSalida.recibir();
                if (paquete.getTipo() == TipoPaquete.MENSAJE) {
                    despacharPush(paquete);
                    continue;
                }
                if (hiloEspera.get() != null) {
                    colaRespuestas.offer(paquete);
                } else {
                    despacharPush(paquete);
                }
            }
        } catch (IOException ex) {
            if (conectado) {
                despacharPush(Paquete.error("Conexión perdida: " + MensajesError.aEspanol(ex)));
            }
        }
    }

    private void despacharPush(Paquete paquete) {
        if (escuchadorMensajes != null) {
            escuchadorMensajes.onPaquete(paquete);
        }
    }

    @Override
    public void close() {
        conectado = false;
        if (hiloEspera.get() != null) {
            colaRespuestas.offer(Paquete.error("Conexión cerrada"));
        }
        if (entradaSalida != null) {
            try {
                entradaSalida.close();
            } catch (IOException ignored) {
            }
        }
        if (hiloLector != null) {
            hiloLector.interrupt();
        }
    }
}
