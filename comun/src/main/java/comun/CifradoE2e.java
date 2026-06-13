package comun;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.MGF1ParameterSpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * Cifrado de extremo a extremo (E2E): RSA-OAEP para la clave AES de sesión
 * y AES-GCM para el cuerpo del mensaje.
 * El servidor solo almacena el prefijo {@value #PREFIJO} y el payload opaco.
 */
public final class CifradoE2e {
    /** Prefijo que identifica contenido cifrado E2E en la base de datos. */
    public static final String PREFIJO = "E2E1:";
    private static final String RSA = "RSA";
    private static final String AES = "AES";
    private static final int AES_BITS = 256;
    private static final int GCM_TAG_BITS = 128;
    private static final int GCM_IV_BYTES = 12;
    private static final OAEPParameterSpec OAEP_SPEC = new OAEPParameterSpec(
            "SHA-256", "MGF1", MGF1ParameterSpec.SHA256, PSource.PSpecified.DEFAULT);

    private CifradoE2e() {
    }

    /**
     * Genera un par de claves RSA de 2048 bits para un usuario.
     *
     * @return par de claves pública y privada
     * @throws Exception si el proveedor criptográfico falla
     */
    public static KeyPair generarParClaves() throws Exception {
        KeyPairGenerator gen = KeyPairGenerator.getInstance(RSA);
        gen.initialize(2048);
        return gen.generateKeyPair();
    }

    /**
     * Codifica una clave pública en Base64 (formato X.509/DER).
     *
     * @param clavePublica clave pública RSA
     * @return representación Base64 para intercambio
     */
    public static String exportarClavePublica(PublicKey clavePublica) {
        return Base64.getEncoder().encodeToString(clavePublica.getEncoded());
    }

    /**
     * Restaura una clave pública desde su representación Base64.
     *
     * @param base64 clave codificada
     * @return clave pública RSA
     * @throws Exception si el formato es inválido
     */
    public static PublicKey importarClavePublica(String base64) throws Exception {
        byte[] bytes = Base64.getDecoder().decode(base64.trim());
        return KeyFactory.getInstance(RSA).generatePublic(new X509EncodedKeySpec(bytes));
    }

    /**
     * Codifica una clave privada en Base64 (formato PKCS#8).
     *
     * @param clavePrivada clave privada RSA
     * @return representación Base64 para almacenamiento local cifrado
     */
    public static String exportarClavePrivada(PrivateKey clavePrivada) {
        return Base64.getEncoder().encodeToString(clavePrivada.getEncoded());
    }

    /**
     * Restaura una clave privada desde su representación Base64.
     *
     * @param base64 clave codificada
     * @return clave privada RSA
     * @throws Exception si el formato es inválido
     */
    public static PrivateKey importarClavePrivada(String base64) throws Exception {
        byte[] bytes = Base64.getDecoder().decode(base64.trim());
        return KeyFactory.getInstance(RSA).generatePrivate(new PKCS8EncodedKeySpec(bytes));
    }

    /**
     * Indica si el contenido tiene el prefijo de cifrado E2E.
     *
     * @param contenido texto o payload almacenado
     * @return {@code true} si empieza por {@link #PREFIJO}
     */
    public static boolean esContenidoCifrado(String contenido) {
        return contenido != null && contenido.startsWith(PREFIJO);
    }

    /**
     * Cifra un mensaje para el destinatario y también para el remitente
     * (este último puede leer lo enviado desde su bandeja).
     *
     * @param textoPlano mensaje en claro
     * @param claveDestinatario clave pública del receptor
     * @param claveRemitente clave pública del emisor
     * @return cadena con prefijo E2E y sobres RSA para ambos
     * @throws Exception si falla el cifrado
     */
    public static String cifrar(String textoPlano, PublicKey claveDestinatario, PublicKey claveRemitente)
            throws Exception {
        KeyGenerator kg = KeyGenerator.getInstance(AES);
        kg.init(AES_BITS);
        SecretKey claveAes = kg.generateKey();

        byte[] iv = new byte[GCM_IV_BYTES];
        new java.security.SecureRandom().nextBytes(iv);
        byte[] cifrado = cifrarAes(textoPlano.getBytes(java.nio.charset.StandardCharsets.UTF_8), claveAes, iv);

        String parteDest = Base64.getEncoder().encodeToString(cifrarClaveAes(claveAes, claveDestinatario));
        String parteRem = Base64.getEncoder().encodeToString(cifrarClaveAes(claveAes, claveRemitente));

        return PREFIJO + Base64.getEncoder().encodeToString(iv) + ":"
                + Base64.getEncoder().encodeToString(cifrado) + ":"
                + parteDest + ":" + parteRem;
    }

    /**
     * Descifra un mensaje E2E usando la clave privada del lector.
     *
     * @param contenidoCifrado payload con prefijo {@link #PREFIJO}
     * @param clavePrivada clave privada del usuario que lee
     * @param usarSobreRemitente {@code true} para leer el sobre del remitente; {@code false} el del destinatario
     * @return texto en claro, o el contenido original si no está cifrado
     * @throws Exception si el formato es inválido o falla el descifrado
     */
    public static String descifrar(String contenidoCifrado, PrivateKey clavePrivada, boolean usarSobreRemitente)
            throws Exception {
        if (!esContenidoCifrado(contenidoCifrado)) {
            return contenidoCifrado;
        }
        String cuerpo = contenidoCifrado.substring(PREFIJO.length());
        String[] partes = cuerpo.split(":", 4);
        if (partes.length != 4) {
            throw new IllegalArgumentException("Formato E2E inválido");
        }
        byte[] iv = Base64.getDecoder().decode(partes[0]);
        byte[] ct = Base64.getDecoder().decode(partes[1]);
        byte[] sobre = Base64.getDecoder().decode(usarSobreRemitente ? partes[3] : partes[2]);
        SecretKey claveAes = descifrarClaveAes(sobre, clavePrivada);
        byte[] plano = descifrarAes(ct, claveAes, iv);
        return new String(plano, java.nio.charset.StandardCharsets.UTF_8);
    }

    private static byte[] cifrarAes(byte[] datos, SecretKey clave, byte[] iv) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, clave, new GCMParameterSpec(GCM_TAG_BITS, iv));
        return cipher.doFinal(datos);
    }

    private static byte[] descifrarAes(byte[] datos, SecretKey clave, byte[] iv) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, clave, new GCMParameterSpec(GCM_TAG_BITS, iv));
        return cipher.doFinal(datos);
    }

    private static byte[] cifrarClaveAes(SecretKey claveAes, PublicKey rsa) throws Exception {
        Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPPadding");
        cipher.init(Cipher.ENCRYPT_MODE, rsa, OAEP_SPEC);
        return cipher.doFinal(claveAes.getEncoded());
    }

    private static SecretKey descifrarClaveAes(byte[] envoltura, PrivateKey rsa) throws Exception {
        Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPPadding");
        cipher.init(Cipher.DECRYPT_MODE, rsa, OAEP_SPEC);
        byte[] raw = cipher.doFinal(envoltura);
        return new javax.crypto.spec.SecretKeySpec(raw, AES);
    }
}
