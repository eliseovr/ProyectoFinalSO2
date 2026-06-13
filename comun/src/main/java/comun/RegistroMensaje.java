package comun;

/**
 * Representa un mensaje almacenado en la base de datos o devuelto en la bandeja.
 * Los campos coinciden con las claves JSON {@code id}, {@code remitente},
 * {@code destinatario}, {@code contenido} y {@code enviadoEn}.
 */
public class RegistroMensaje {
    private long id;
    private String remitente;
    private String destinatario;
    private String contenido;
    private String enviadoEn;

    /** Crea un registro vacío (útil para deserialización). */
    public RegistroMensaje() {
    }

    /**
     * Crea un registro de mensaje con todos sus campos.
     *
     * @param id identificador en base de datos
     * @param remitente nombre del usuario emisor
     * @param destinatario nombre del usuario receptor
     * @param contenido texto o payload cifrado E2E
     * @param enviadoEn marca temporal ISO de envío
     */
    public RegistroMensaje(long id, String remitente, String destinatario, String contenido, String enviadoEn) {
        this.id = id;
        this.remitente = remitente;
        this.destinatario = destinatario;
        this.contenido = contenido;
        this.enviadoEn = enviadoEn;
    }

    /** @return identificador del mensaje */
    public long getId() {
        return id;
    }

    /** @return nombre del remitente */
    public String getRemitente() {
        return remitente;
    }

    /** @return nombre del destinatario */
    public String getDestinatario() {
        return destinatario;
    }

    /** @return contenido del mensaje (posiblemente cifrado E2E) */
    public String getContenido() {
        return contenido;
    }

    /** @return fecha/hora de envío en formato texto */
    public String getEnviadoEn() {
        return enviadoEn;
    }
}
