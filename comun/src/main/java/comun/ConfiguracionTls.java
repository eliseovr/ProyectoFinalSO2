package comun;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Enumeration;

/**
 * Configuración TLS compartida entre servidor y clientes.
 * Gestiona keystores PKCS12, huellas SHA-256 del certificado activo
 * y contextos SSL para escucha y conexión cliente.
 */
public final class ConfiguracionTls {
    /** Contraseña predeterminada del keystore de desarrollo. */
    public static final String CONTRASENA_KEYSTORE = "changeit";
    /** Alias de la entrada de certificado/clave en el keystore. */
    public static final String ALIAS_CLAVE = "mensajeria";
    /** Nombre del archivo donde el servidor publica la huella del certificado activo. */
    public static final String ARCHIVO_HUELLA_ACTIVA = "active-cert.sha256";

    private ConfiguracionTls() {
    }

    /**
     * Devuelve la ruta del archivo de huella junto al keystore del servidor.
     *
     * @param rutaKeystore ruta al archivo PKCS12
     * @return ruta a {@value #ARCHIVO_HUELLA_ACTIVA}
     */
    public static Path rutaHuellaActiva(Path rutaKeystore) {
        return rutaKeystore.getParent().resolve(ARCHIVO_HUELLA_ACTIVA);
    }

    /**
     * Calcula y escribe la huella SHA-256 del certificado cargado en el keystore.
     * El cliente la usa para validar el certificado fuera de localhost.
     *
     * @param rutaKeystore keystore del servidor
     * @throws GeneralSecurityException si no hay certificado válido
     * @throws IOException si no se puede escribir el archivo de huella
     */
    public static void publicarHuellaActiva(Path rutaKeystore) throws GeneralSecurityException, IOException {
        KeyStore ks = cargarKeyStore(rutaKeystore, CONTRASENA_KEYSTORE);
        X509Certificate cert = cargarCertificadoFijado(ks);
        String huella = huellaSha256(cert);
        Files.write(rutaHuellaActiva(rutaKeystore), huella.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Crea un contexto TLS para el socket servidor con la clave del keystore.
     *
     * @param rutaKeystore keystore PKCS12 del servidor
     * @return contexto SSL listo para {@code SSLServerSocket}
     * @throws GeneralSecurityException si falla la carga de claves
     * @throws IOException si el keystore no existe o no se puede leer
     */
    public static SSLContext contextoServidor(Path rutaKeystore) throws GeneralSecurityException, IOException {
        KeyStore ks = cargarKeyStore(rutaKeystore, CONTRASENA_KEYSTORE);
        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(ks, CONTRASENA_KEYSTORE.toCharArray());
        SSLContext ctx = SSLContext.getInstance("TLS");
        ctx.init(kmf.getKeyManagers(), null, null);
        return ctx;
    }

    /**
     * Crea un contexto TLS de cliente según el host de destino.
     * En localhost acepta el certificado autofirmado (desarrollo);
     * en otros hosts valida la huella publicada por el servidor.
     *
     * @param rutaKeystoreServidor keystore del servidor para obtener la huella
     * @param host nombre o IP del servidor
     * @return contexto SSL para {@code SSLSocket}
     * @throws GeneralSecurityException si falla la configuración de confianza
     * @throws IOException si no se puede leer keystore o huella
     */
    public static SSLContext contextoCliente(Path rutaKeystoreServidor, String host)
            throws GeneralSecurityException, IOException {
        if (esLocalhost(host)) {
            return contextoConfianzaLocalhost();
        }
        String huellaEsperada = cargarHuellaEsperada(rutaKeystoreServidor);
        TrustManager[] gestoresConfianza = {gestorConfianzaPorHuella(huellaEsperada)};
        SSLContext ctx = SSLContext.getInstance("TLS");
        ctx.init(null, gestoresConfianza, null);
        return ctx;
    }

    /**
     * Crea un contexto TLS de cliente asumiendo conexión a localhost.
     *
     * @param rutaKeystoreServidor keystore del servidor (para leer la huella)
     * @return contexto SSL para conectar al servidor
     * @throws GeneralSecurityException si falla la configuración de confianza
     * @throws IOException si no se puede leer keystore o huella
     */
    public static SSLContext contextoCliente(Path rutaKeystoreServidor) throws GeneralSecurityException, IOException {
        return contextoCliente(rutaKeystoreServidor, "localhost");
    }

    private static boolean esLocalhost(String host) {
        if (host == null) {
            return true;
        }
        String h = host.trim().toLowerCase();
        return "localhost".equals(h) || "127.0.0.1".equals(h) || "0:0:0:0:0:0:0:1".equals(h) || "::1".equals(h);
    }

    private static SSLContext contextoConfianzaLocalhost() throws GeneralSecurityException {
        TrustManager[] gestoresConfianza = {new X509TrustManager() {
            @Override
            public void checkClientTrusted(X509Certificate[] chain, String authType) {
            }

            @Override
            public void checkServerTrusted(X509Certificate[] chain, String authType) {
            }

            @Override
            public X509Certificate[] getAcceptedIssuers() {
                return new X509Certificate[0];
            }
        }};
        SSLContext ctx = SSLContext.getInstance("TLS");
        ctx.init(null, gestoresConfianza, null);
        return ctx;
    }

    private static String cargarHuellaEsperada(Path rutaKeystoreServidor)
            throws GeneralSecurityException, IOException, CertificateException {
        Path archivoActivo = rutaHuellaActiva(rutaKeystoreServidor);
        if (Files.exists(archivoActivo)) {
            String delServidor = new String(Files.readAllBytes(archivoActivo), StandardCharsets.UTF_8).trim();
            if (!delServidor.isEmpty()) {
                return delServidor;
            }
        }
        KeyStore ks = cargarKeyStore(rutaKeystoreServidor, CONTRASENA_KEYSTORE);
        return huellaSha256(cargarCertificadoFijado(ks));
    }

    private static X509Certificate cargarCertificadoFijado(KeyStore truststore) throws GeneralSecurityException {
        Certificate cert = truststore.getCertificate(ALIAS_CLAVE);
        if (cert instanceof X509Certificate) {
            return (X509Certificate) cert;
        }
        try {
            Enumeration<String> aliases = truststore.aliases();
            while (aliases.hasMoreElements()) {
                String alias = aliases.nextElement();
                if (truststore.isCertificateEntry(alias) || truststore.isKeyEntry(alias)) {
                    Certificate entry = truststore.getCertificate(alias);
                    if (entry instanceof X509Certificate) {
                        return (X509Certificate) entry;
                    }
                }
            }
        } catch (GeneralSecurityException ex) {
            throw ex;
        }
        throw new GeneralSecurityException("No hay certificado X.509 en el truststore");
    }

    private static X509TrustManager gestorConfianzaPorHuella(String huellaEsperada) {
        return new X509TrustManager() {
            @Override
            public void checkClientTrusted(X509Certificate[] chain, String authType) {
            }

            @Override
            public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                if (chain == null || chain.length == 0) {
                    throw new CertificateException("El servidor no envió certificado");
                }
                if (!cadenaCoincideConHuella(chain, huellaEsperada)) {
                    throw new CertificateException(
                            "El certificado del servidor no coincide. "
                                    + "Cierre TODAS las ventanas del servidor (Ctrl+C), ejecute iniciar-servidor "
                                    + "(.bat o .sh) y vuelva a copiar certificados/ al cliente si usa VMs.");
                }
            }

            @Override
            public X509Certificate[] getAcceptedIssuers() {
                return new X509Certificate[0];
            }
        };
    }

    private static boolean cadenaCoincideConHuella(X509Certificate[] chain, String huellaEsperada)
            throws CertificateException {
        for (X509Certificate cert : chain) {
            if (huellaSha256(cert).equalsIgnoreCase(huellaEsperada)) {
                return true;
            }
        }
        return false;
    }

    static String huellaSha256(X509Certificate certificate) throws CertificateException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(certificate.getEncoded());
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < hash.length; i++) {
                if (i > 0) {
                    sb.append(':');
                }
                sb.append(String.format("%02X", hash[i]));
            }
            return sb.toString();
        } catch (Exception ex) {
            throw new CertificateException("No se pudo calcular la huella del certificado", ex);
        }
    }

    private static KeyStore cargarKeyStore(Path path, String contrasena)
            throws GeneralSecurityException, IOException {
        if (!Files.exists(path)) {
            throw new IOException("No se encontró el almacén de claves: " + path.toAbsolutePath());
        }
        KeyStore ks = KeyStore.getInstance("PKCS12");
        try (InputStream in = new FileInputStream(path.toFile())) {
            ks.load(in, contrasena.toCharArray());
        }
        return ks;
    }
}
