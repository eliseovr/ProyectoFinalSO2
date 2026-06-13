package comun;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.HashMap;
import java.util.Map;

/**
 * Orquesta el cifrado y descifrado E2E en el cliente durante una sesión activa.
 * Mantiene un mapa de claves públicas de contactos y la clave privada del usuario local.
 */
public final class GestorCifradoSesion {
    private final PrivateKey clavePrivada;
    private final PublicKey clavePublicaPropia;
    private final String nombreUsuario;
    private final Map<String, PublicKey> clavesPublicas = new HashMap<>();

    /**
     * Crea el gestor para un usuario ya autenticado con su par de claves.
     *
     * @param nombreUsuario nombre de la cuenta (se normaliza a minúsculas)
     * @param clavePrivada clave privada RSA del usuario
     * @param clavePublicaPropia clave pública RSA del usuario
     */
    public GestorCifradoSesion(String nombreUsuario, PrivateKey clavePrivada, PublicKey clavePublicaPropia) {
        this.nombreUsuario = nombreUsuario.toLowerCase();
        this.clavePrivada = clavePrivada;
        this.clavePublicaPropia = clavePublicaPropia;
        clavesPublicas.put(this.nombreUsuario, clavePublicaPropia);
    }

    /** @return nombre de usuario en minúsculas */
    public String getNombreUsuario() {
        return nombreUsuario;
    }

    /**
     * Exporta la clave pública propia en Base64 para enviarla al servidor.
     *
     * @return clave pública codificada
     */
    public String exportarClavePublicaPropia() {
        return CifradoE2e.exportarClavePublica(clavePublicaPropia);
    }

    /**
     * Registra o actualiza la clave pública de un contacto.
     *
     * @param usuario nombre del contacto
     * @param clavePublicaBase64 clave pública en Base64 (se ignora si es nula o vacía)
     * @throws Exception si la clave no se puede importar
     */
    public void registrarClavePublica(String usuario, String clavePublicaBase64) throws Exception {
        if (usuario == null || clavePublicaBase64 == null || clavePublicaBase64.isBlank()) {
            return;
        }
        clavesPublicas.put(usuario.toLowerCase(), CifradoE2e.importarClavePublica(clavePublicaBase64));
    }

    /**
     * Registra claves públicas desde arrays paralelos de usuarios y claves
     * (como los devueltos en {@code LISTA_USUARIOS}).
     *
     * @param usuarios nombres de usuario
     * @param claves claves públicas Base64 alineadas con {@code usuarios}
     * @throws Exception si alguna clave no se puede importar
     */
    public void registrarDesdeListas(String[] usuarios, String[] claves) throws Exception {
        if (usuarios == null || claves == null) {
            return;
        }
        int n = Math.min(usuarios.length, claves.length);
        for (int i = 0; i < n; i++) {
            registrarClavePublica(usuarios[i], claves[i]);
        }
    }

    /**
     * Cifra un mensaje saliente para el destinatario indicado.
     *
     * @param destinatario usuario receptor
     * @param textoPlano mensaje en claro
     * @return payload E2E listo para enviar al servidor
     * @throws Exception si no hay clave pública del destinatario o falla el cifrado
     */
    public String cifrarParaEnvio(String destinatario, String textoPlano) throws Exception {
        PublicKey pubDest = clavesPublicas.get(destinatario.toLowerCase());
        if (pubDest == null) {
            throw new IllegalStateException("No hay clave pública de " + destinatario
                    + ". Actualice la lista de usuarios.");
        }
        return CifradoE2e.cifrar(textoPlano, pubDest, clavePublicaPropia);
    }

    /**
     * Descifra un mensaje de bandeja para mostrarlo en la UI.
     * Devuelve textos descriptivos si el mensaje no es legible por este usuario.
     *
     * @param mensaje registro con contenido posiblemente cifrado
     * @return texto en claro o mensaje de error legible
     */
    public String descifrarParaMostrar(RegistroMensaje mensaje) {
        if (mensaje == null || mensaje.getContenido() == null) {
            return "";
        }
        String contenido = mensaje.getContenido();
        if (!CifradoE2e.esContenidoCifrado(contenido)) {
            return "[Mensaje antiguo sin cifrado E2E] " + contenido;
        }
        try {
            boolean yoSoyDestinatario = nombreUsuario.equalsIgnoreCase(mensaje.getDestinatario());
            boolean yoSoyRemitente = nombreUsuario.equalsIgnoreCase(mensaje.getRemitente());
            if (yoSoyDestinatario) {
                return CifradoE2e.descifrar(contenido, clavePrivada, false);
            }
            if (yoSoyRemitente) {
                return CifradoE2e.descifrar(contenido, clavePrivada, true);
            }
            return "[No autorizado para leer este mensaje]";
        } catch (Exception ex) {
            return "[No se pudo descifrar el mensaje]";
        }
    }
}
