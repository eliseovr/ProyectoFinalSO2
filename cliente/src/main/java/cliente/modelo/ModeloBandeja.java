package cliente.modelo;

import comun.GestorCifradoSesion;
import comun.RegistroMensaje;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Estado de la bandeja de mensajes: caché local, no leídos y utilidades de conversación.
 */
public class ModeloBandeja {
    private static final DateTimeFormatter FORMATO_FECHA_SERVIDOR = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter FORMATO_SOLO_HORA = DateTimeFormatter.ofPattern("HH:mm:ss");
    public static final DateTimeFormatter FORMATO_MOSTRAR_FECHA = DateTimeFormatter.ofPattern("dd/MM HH:mm");

    private final String nombreUsuario;
    private final Map<Long, RegistroMensaje> cacheMensajes = new LinkedHashMap<>();
    private final Map<String, Integer> noLeidosPorContacto = new HashMap<>();

    public ModeloBandeja(String nombreUsuario) {
        this.nombreUsuario = nombreUsuario;
    }

    public String getNombreUsuario() {
        return nombreUsuario;
    }

    public void fusionar(RegistroMensaje mensaje) {
        RegistroMensaje normalizado = normalizarMarcaTiempo(mensaje);
        cacheMensajes.put(normalizado.getId(), normalizado);
    }

    public void fusionarTodos(RegistroMensaje[] mensajes) {
        if (mensajes == null) {
            return;
        }
        for (RegistroMensaje m : mensajes) {
            fusionar(m);
        }
    }

    public int getNoLeidos(String contacto) {
        if (contacto == null) {
            return 0;
        }
        return noLeidosPorContacto.getOrDefault(contacto.toLowerCase(), 0);
    }

    public void marcarLeido(String contacto) {
        if (contacto != null) {
            noLeidosPorContacto.remove(contacto.toLowerCase());
        }
    }

    public void incrementarNoLeido(String contacto) {
        if (contacto != null) {
            noLeidosPorContacto.merge(contacto.toLowerCase(), 1, Integer::sum);
        }
    }

    public Map<String, Integer> getNoLeidosPorContacto() {
        return noLeidosPorContacto;
    }

    public List<RegistroMensaje> mensajesCon(String contacto) {
        List<RegistroMensaje> hilo = new ArrayList<>();
        for (RegistroMensaje m : cacheMensajes.values()) {
            if (esConversacionCon(m, contacto)) {
                hilo.add(m);
            }
        }
        hilo.sort(Comparator.comparingLong(this::claveOrdenMensaje).thenComparingLong(RegistroMensaje::getId));
        return hilo;
    }

    public String contactoDe(RegistroMensaje mensaje) {
        if (nombreUsuario.equalsIgnoreCase(mensaje.getRemitente())) {
            return mensaje.getDestinatario();
        }
        if (nombreUsuario.equalsIgnoreCase(mensaje.getDestinatario())) {
            return mensaje.getRemitente();
        }
        return null;
    }

    public RegistroMensaje normalizarMarcaTiempo(RegistroMensaje m) {
        String at = m.getEnviadoEn();
        if (at == null || at.trim().isEmpty()) {
            at = FORMATO_FECHA_SERVIDOR.format(LocalDateTime.now());
        } else if (at.length() <= 8 && at.contains(":")) {
            at = FORMATO_FECHA_SERVIDOR.format(LocalDateTime.of(LocalDate.now(), LocalTime.parse(at, FORMATO_SOLO_HORA)));
        }
        return new RegistroMensaje(m.getId(), m.getRemitente(), m.getDestinatario(), m.getContenido(), at);
    }

    public static String formatearHoraMostrar(String sentAt) {
        if (sentAt == null || sentAt.trim().isEmpty()) {
            return FORMATO_MOSTRAR_FECHA.format(LocalDateTime.now());
        }
        try {
            if (sentAt.length() <= 8 && sentAt.contains(":")) {
                LocalDateTime dt = LocalDateTime.of(LocalDate.now(), LocalTime.parse(sentAt, FORMATO_SOLO_HORA));
                return FORMATO_MOSTRAR_FECHA.format(dt);
            }
            return FORMATO_MOSTRAR_FECHA.format(LocalDateTime.parse(sentAt, FORMATO_FECHA_SERVIDOR));
        } catch (DateTimeParseException ex) {
            return sentAt;
        }
    }

    public static String escaparHtml(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    public String renderizarConversacionHtml(String contacto, GestorCifradoSesion cifrado) {
        if (contacto == null) {
            return null;
        }
        List<RegistroMensaje> mensajesHilo = mensajesCon(contacto);
        if (mensajesHilo.isEmpty()) {
            return "<html><body style='margin:24px;font-family:Segoe UI,sans-serif;color:#65676B;"
                    + "background:#E5DDD5;text-align:center;'>Sin mensajes con <b style='color:#1877F2;'>"
                    + escaparHtml(contacto) + "</b><br><span style='font-size:12px;'>Escriba abajo para enviar el primero.</span></body></html>";
        }

        StringBuilder contenidoHtml = new StringBuilder();
        contenidoHtml.append("<html><head><style>");
        contenidoHtml.append("body{margin:12px 16px;background:#E5DDD5;font-family:'Segoe UI',sans-serif;font-size:14px;}");
        contenidoHtml.append("</style></head><body>");

        for (RegistroMensaje m : mensajesHilo) {
            boolean propio = nombreUsuario.equalsIgnoreCase(m.getRemitente());
            String hora = escaparHtml(formatearHoraMostrar(m.getEnviadoEn()));
            String textoVisible = cifrado.descifrarParaMostrar(m);
            String texto = escaparHtml(textoVisible).replace("\n", "<br>");

            if (propio) {
                contenidoHtml.append("<div style='text-align:right;margin:6px 0 14px 56px;'>");
                contenidoHtml.append("<div style='display:inline-block;background:#D9FDD3;padding:10px 14px;");
                contenidoHtml.append("border-radius:18px 18px 4px 18px;max-width:75%;text-align:left;color:#050505;");
                contenidoHtml.append("box-shadow:0 1px 2px rgba(0,0,0,0.06);'>").append(texto).append("</div>");
                contenidoHtml.append("<div style='font-size:11px;color:#65676B;margin-top:4px;'>").append(hora).append("</div>");
                contenidoHtml.append("</div>");
            } else {
                contenidoHtml.append("<div style='text-align:left;margin:6px 56px 14px 0;'>");
                contenidoHtml.append("<div style='font-size:12px;color:#1877F2;font-weight:600;margin:0 0 4px 4px;'>")
                        .append(escaparHtml(m.getRemitente())).append("</div>");
                contenidoHtml.append("<div style='display:inline-block;background:#FFFFFF;padding:10px 14px;");
                contenidoHtml.append("border-radius:18px 18px 18px 4px;max-width:75%;color:#050505;");
                contenidoHtml.append("box-shadow:0 1px 2px rgba(0,0,0,0.06);'>").append(texto).append("</div>");
                contenidoHtml.append("<div style='font-size:11px;color:#65676B;margin-top:4px;margin-left:4px;'>")
                        .append(hora).append("</div>");
                contenidoHtml.append("</div>");
            }
        }
        contenidoHtml.append("</body></html>");
        return contenidoHtml.toString();
    }

    private boolean esConversacionCon(RegistroMensaje m, String contacto) {
        String yo = nombreUsuario.toLowerCase();
        String otro = contacto.toLowerCase();
        String remitente = m.getRemitente() != null ? m.getRemitente().toLowerCase() : "";
        String destinatario = m.getDestinatario() != null ? m.getDestinatario().toLowerCase() : "";
        return (remitente.equals(yo) && destinatario.equals(otro)) || (remitente.equals(otro) && destinatario.equals(yo));
    }

    private long claveOrdenMensaje(RegistroMensaje m) {
        try {
            return LocalDateTime.parse(m.getEnviadoEn(), FORMATO_FECHA_SERVIDOR)
                    .atZone(java.time.ZoneId.systemDefault())
                    .toInstant()
                    .toEpochMilli();
        } catch (DateTimeParseException ex) {
            return m.getId();
        }
    }
}
