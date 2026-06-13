package comun;

/**
 * Representa un paquete JSON intercambiado entre cliente y servidor.
 * Los nombres de propiedades en la red siguen convención camelCase en español
 * (usuario, contrasena, clavePublica, etc.).
 */
public class Paquete {
    private TipoPaquete tipo;
    private String token;
    private String usuario;
    private String contrasena;
    private String destinatario;
    private String contenido;
    private String mensaje;
    private String[] usuarios;
    private String[] clavesPublicas;
    private String clavePublica;
    private RegistroMensaje[] mensajes;

    /** Crea un paquete vacío para rellenar con setters o deserialización. */
    public Paquete() {
    }

    /**
     * Crea un paquete con el tipo indicado.
     *
     * @param tipo tipo de mensaje del protocolo
     * @return paquete nuevo con el tipo asignado
     */
    public static Paquete of(TipoPaquete tipo) {
        Paquete p = new Paquete();
        p.tipo = tipo;
        return p;
    }

    /**
     * Crea un paquete de respuesta exitosa ({@link TipoPaquete#OK}).
     *
     * @param mensaje texto descriptivo del resultado
     * @return paquete OK con mensaje
     */
    public static Paquete ok(String mensaje) {
        Paquete p = of(TipoPaquete.OK);
        p.mensaje = mensaje;
        return p;
    }

    /**
     * Crea un paquete de error ({@link TipoPaquete#ERROR}).
     *
     * @param mensaje descripción del error
     * @return paquete ERROR con mensaje
     */
    public static Paquete error(String mensaje) {
        Paquete p = of(TipoPaquete.ERROR);
        p.mensaje = mensaje;
        return p;
    }

    /**
     * Serializa el paquete a una línea JSON.
     *
     * @param paquete paquete a serializar
     * @return cadena JSON
     */
    public static String aJson(Paquete paquete) {
        return UtilidadJson.aJson(paquete);
    }

    /**
     * Deserializa un paquete desde una línea JSON.
     *
     * @param json línea JSON recibida por la red
     * @return paquete parseado
     */
    public static Paquete desdeJson(String json) {
        return UtilidadJson.desdeJson(json);
    }

    public TipoPaquete getTipo() {
        return tipo;
    }

    public void setTipo(TipoPaquete tipo) {
        this.tipo = tipo;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getUsuario() {
        return usuario;
    }

    public void setUsuario(String usuario) {
        this.usuario = usuario;
    }

    public String getContrasena() {
        return contrasena;
    }

    public void setContrasena(String contrasena) {
        this.contrasena = contrasena;
    }

    public String getDestinatario() {
        return destinatario;
    }

    public void setDestinatario(String destinatario) {
        this.destinatario = destinatario;
    }

    public String getContenido() {
        return contenido;
    }

    public void setContenido(String contenido) {
        this.contenido = contenido;
    }

    public String getMensaje() {
        return mensaje;
    }

    public void setMensaje(String mensaje) {
        this.mensaje = mensaje;
    }

    public String[] getUsuarios() {
        return usuarios;
    }

    public void setUsuarios(String[] usuarios) {
        this.usuarios = usuarios;
    }

    public String[] getClavesPublicas() {
        return clavesPublicas;
    }

    public void setClavesPublicas(String[] clavesPublicas) {
        this.clavesPublicas = clavesPublicas;
    }

    public String getClavePublica() {
        return clavePublica;
    }

    public void setClavePublica(String clavePublica) {
        this.clavePublica = clavePublica;
    }

    public RegistroMensaje[] getMensajes() {
        return mensajes;
    }

    public void setMensajes(RegistroMensaje[] mensajes) {
        this.mensajes = mensajes;
    }
}
