package servidor;

import comun.RutasAplicacion;
import comun.MensajesError;
import comun.Protocolo;
import comun.ConfiguracionTls;

import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.cert.X509Certificate;
import java.io.FileInputStream;
import java.io.InputStream;

import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import java.nio.file.Path;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Servidor de mensajería seguro con TLS y atención concurrente.
 * Acepta conexiones SSL, delega cada cliente a un {@link ManejadorCliente}
 * y persiste datos en SQLite mediante {@link BaseDatosSqlite}.
 */
public class ServidorMensajeria {
    private final int puerto;
    private final Path rutaKeystore;
    private final Path directorioDatos;
    private final Path directorioLogs;

    /**
     * @param puerto          puerto TCP donde escucha el socket TLS
     * @param rutaKeystore    archivo PKCS12 con el certificado del servidor
     * @param directorioDatos carpeta que contiene {@code mensajeria.db}
     * @param directorioLogs  carpeta para {@code sistema.log} y {@code eventos.log}
     */
    public ServidorMensajeria(int puerto, Path rutaKeystore, Path directorioDatos, Path directorioLogs) {
        this.puerto = puerto;
        this.rutaKeystore = rutaKeystore;
        this.directorioDatos = directorioDatos;
        this.directorioLogs = directorioLogs;
    }

    /** Inicializa TLS, base de datos, registro y bucle de aceptación de clientes. */
    public void iniciar() throws Exception {
        RegistroActividad registro = new RegistroActividad(directorioLogs);
        BaseDatosSqlite baseDatos = new BaseDatosSqlite(directorioDatos);
        GestorSesiones sesiones = new GestorSesiones();

        ConfiguracionTls.publicarHuellaActiva(rutaKeystore);
        SSLServerSocketFactory fabrica = ConfiguracionTls.contextoServidor(rutaKeystore)
                .getServerSocketFactory();
        SSLServerSocket socketServidor = (SSLServerSocket) fabrica.createServerSocket(puerto);
        socketServidor.setEnabledProtocols(new String[]{"TLSv1.2", "TLSv1.3"});

        ExecutorService pool = Executors.newCachedThreadPool();
        registro.info("SERVIDOR iniciado en puerto " + puerto + " (TLS activo)");
        registro.info("Certificado TLS: " + describirCertificado(rutaKeystore));
        registro.info("Huella publicada en certificados\\" + ConfiguracionTls.ARCHIVO_HUELLA_ACTIVA);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            registro.info("SERVIDOR deteniéndose...");
            pool.shutdownNow();
            try {
                socketServidor.close();
            } catch (Exception ignored) {
            }
            try {
                baseDatos.close();
            } catch (Exception ignored) {
            }
        }));

        while (!Thread.currentThread().isInterrupted()) {
            try {
                java.net.Socket socketCliente = socketServidor.accept();
                ManejadorCliente manejador = new ManejadorCliente(socketCliente, baseDatos, sesiones, registro);
                pool.execute(manejador);
            } catch (java.net.SocketException ex) {
                if (!Thread.currentThread().isInterrupted()) {
                    registro.warn("SERVIDOR socket cerrado: " + ex.getMessage());
                }
                break;
            }
        }
    }

    /** Punto de entrada: lee puerto y rutas desde variables de entorno y arranca el servidor. */
    public static void main(String[] args) {
        try {
            int puerto = Integer.parseInt(System.getenv().getOrDefault("SERVER_PORT", String.valueOf(Protocolo.PUERTO_PREDETERMINADO)));
            Path base = RutasAplicacion.baseAplicacion();
            Path keystore = RutasAplicacion.resolver("KEYSTORE_PATH", "certificados/server.p12");
            Path directorioDatos = base.resolve("datos");
            Path directorioLogs = base.resolve("registros");

            new ServidorMensajeria(puerto, keystore, directorioDatos, directorioLogs).iniciar();
        } catch (java.net.BindException ex) {
            System.err.println();
            System.err.println("ERROR: No se pudo iniciar el servidor — el puerto ya está en uso.");
            System.err.println("Cierre otras instancias del servidor o ejecute reiniciar-todo.bat.");
            System.err.println("Detalle: " + MensajesError.aEspanol(ex));
            System.exit(1);
        } catch (Exception ex) {
            System.err.println();
            System.err.println("ERROR al iniciar el servidor:");
            System.err.println(MensajesError.aEspanol(ex));
            ex.printStackTrace(System.err);
            System.exit(1);
        }
    }

    /** Devuelve sujeto, ruta y huella SHA-256 del certificado activo en el keystore. */
    private static String describirCertificado(Path rutaKeystore) throws Exception {
        KeyStore ks = KeyStore.getInstance("PKCS12");
        try (InputStream in = new FileInputStream(rutaKeystore.toFile())) {
            ks.load(in, ConfiguracionTls.CONTRASENA_KEYSTORE.toCharArray());
        }
        X509Certificate cert = (X509Certificate) ks.getCertificate(ConfiguracionTls.ALIAS_CLAVE);
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        String huella = aHuellaHex(digest.digest(cert.getEncoded()));
        return cert.getSubjectX500Principal().getName() + " [" + rutaKeystore.toAbsolutePath() + "] SHA-256=" + huella;
    }

    /** Formatea un digest SHA-256 como cadena hexadecimal separada por dos puntos. */
    private static String aHuellaHex(byte[] hash) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < hash.length; i++) {
            if (i > 0) {
                sb.append(':');
            }
            sb.append(String.format("%02X", hash[i]));
        }
        return sb.toString();
    }
}
