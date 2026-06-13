package comun;

import javax.crypto.Cipher;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Persiste la clave privada RSA del usuario cifrada con su contraseña (solo cliente).
 * El archivo se guarda bajo {@code datos/claves/<usuario>.key} relativo a {@link RutasAplicacion}.
 */
public final class AlmacenClavePrivada {
    private static final int ITERACIONES = 120_000;
    private static final int SAL_BYTES = 16;
    private static final int GCM_TAG_BITS = 128;

    private AlmacenClavePrivada() {
    }

    /**
     * Devuelve la ruta del archivo de clave privada para un usuario.
     *
     * @param usuario nombre de la cuenta
     * @return ruta al archivo {@code .key}
     */
    public static Path rutaClave(String usuario) {
        return RutasAplicacion.baseAplicacion()
                .resolve("datos")
                .resolve("claves")
                .resolve(usuario.toLowerCase() + ".key");
    }

    /**
     * Cifra y guarda la clave privada usando PBKDF2 + AES-GCM con la contraseña del usuario.
     *
     * @param usuario nombre de la cuenta
     * @param contrasena contraseña para derivar la clave de cifrado
     * @param clavePrivada clave privada RSA a proteger
     * @throws Exception si falla la escritura o el cifrado
     */
    public static void guardar(String usuario, String contrasena, PrivateKey clavePrivada) throws Exception {
        Path archivo = rutaClave(usuario);
        Files.createDirectories(archivo.getParent());
        byte[] salt = new byte[SAL_BYTES];
        new SecureRandom().nextBytes(salt);
        byte[] claveAes = derivar(contrasena, salt);
        byte[] iv = new byte[12];
        new SecureRandom().nextBytes(iv);
        byte[] priv = clavePrivada.getEncoded();
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(claveAes, "AES"), new GCMParameterSpec(GCM_TAG_BITS, iv));
        byte[] cifrado = cipher.doFinal(priv);
        String linea = Base64.getEncoder().encodeToString(salt) + ":"
                + Base64.getEncoder().encodeToString(iv) + ":"
                + Base64.getEncoder().encodeToString(cifrado);
        Files.writeString(archivo, linea, StandardCharsets.UTF_8);
    }

    /**
     * Carga y descifra la clave privada del usuario con su contraseña.
     *
     * @param usuario nombre de la cuenta
     * @param contrasena contraseña usada al guardar
     * @return clave privada RSA, o {@code null} si no existe el archivo
     * @throws Exception si el archivo está corrupto o la contraseña es incorrecta
     */
    public static PrivateKey cargar(String usuario, String contrasena) throws Exception {
        Path archivo = rutaClave(usuario);
        if (!Files.exists(archivo)) {
            return null;
        }
        String linea = Files.readString(archivo, StandardCharsets.UTF_8).trim();
        String[] partes = linea.split(":", 3);
        if (partes.length != 3) {
            throw new IllegalStateException("Archivo de clave corrupto");
        }
        byte[] salt = Base64.getDecoder().decode(partes[0]);
        byte[] iv = Base64.getDecoder().decode(partes[1]);
        byte[] ct = Base64.getDecoder().decode(partes[2]);
        byte[] claveAes = derivar(contrasena, salt);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(claveAes, "AES"), new GCMParameterSpec(GCM_TAG_BITS, iv));
        byte[] pkcs8 = cipher.doFinal(ct);
        return java.security.KeyFactory.getInstance("RSA")
                .generatePrivate(new java.security.spec.PKCS8EncodedKeySpec(pkcs8));
    }

    /**
     * Indica si ya existe un archivo de clave privada para el usuario.
     *
     * @param usuario nombre de la cuenta
     * @return {@code true} si el archivo {@code .key} existe
     */
    public static boolean existe(String usuario) {
        return Files.exists(rutaClave(usuario));
    }

    private static byte[] derivar(String contrasena, byte[] salt) throws Exception {
        PBEKeySpec spec = new PBEKeySpec(contrasena.toCharArray(), salt, ITERACIONES, 256);
        SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        return skf.generateSecret(spec).getEncoded();
    }
}
