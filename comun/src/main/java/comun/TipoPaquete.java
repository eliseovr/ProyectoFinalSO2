package comun;

/**
 * Tipos de mensajes del protocolo cliente-servidor.
 * Los nombres del enum se serializan tal cual en el campo JSON {@code tipo};
 * no deben modificarse sin romper la compatibilidad en la red.
 */
public enum TipoPaquete {
    /** Solicitud de registro de usuario nuevo. */
    REGISTRO,
    /** Inicio de sesión con credenciales. */
    INICIO_SESION,
    /** Cierre de sesión del cliente. */
    CIERRE_SESION,
    /** Envío de un mensaje a otro usuario. */
    ENVIAR,
    /** Solicitud de bandeja de entrada. */
    BANDEJA,
    /** Solicitud de lista de usuarios. */
    USUARIOS,
    /** Latido de conexión. */
    PING,
    /** Respuesta exitosa del servidor. */
    OK,
    /** Respuesta de error del servidor. */
    ERROR,
    /** Notificación push de mensaje entrante. */
    MENSAJE,
    /** Lista de usuarios con claves públicas. */
    LISTA_USUARIOS,
    /** Actualización de clave pública del cliente. */
    ACTUALIZAR_CLAVE_PUBLICA
}
