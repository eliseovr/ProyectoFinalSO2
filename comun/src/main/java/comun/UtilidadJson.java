package comun;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Serialización y deserialización JSON ligera sin dependencias externas.
 * Pensada para entornos de compilación manual sin Maven.
 * Las claves JSON en la red no deben alterarse.
 */
public final class UtilidadJson {
    private static final Pattern FIELD = Pattern.compile("\"(\\w+)\"\\s*:");

    private UtilidadJson() {
    }

    /**
     * Convierte un paquete a una cadena JSON de una sola línea.
     *
     * @param paquete paquete a serializar
     * @return JSON con campos no nulos
     */
    public static String aJson(Paquete paquete) {
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        first = append(sb, "tipo", paquete.getTipo() != null ? paquete.getTipo().name() : null, first);
        first = append(sb, "token", paquete.getToken(), first);
        first = append(sb, "usuario", paquete.getUsuario(), first);
        first = append(sb, "contrasena", paquete.getContrasena(), first);
        first = append(sb, "destinatario", paquete.getDestinatario(), first);
        first = append(sb, "contenido", paquete.getContenido(), first);
        first = append(sb, "mensaje", paquete.getMensaje(), first);
        first = append(sb, "clavePublica", paquete.getClavePublica(), first);
        if (paquete.getUsuarios() != null) {
            sb.append(",\"usuarios\":[");
            for (int i = 0; i < paquete.getUsuarios().length; i++) {
                if (i > 0) {
                    sb.append(',');
                }
                sb.append('"').append(escape(paquete.getUsuarios()[i])).append('"');
            }
            sb.append(']');
        }
        if (paquete.getClavesPublicas() != null) {
            sb.append(",\"clavesPublicas\":[");
            for (int i = 0; i < paquete.getClavesPublicas().length; i++) {
                if (i > 0) {
                    sb.append(',');
                }
                String cp = paquete.getClavesPublicas()[i];
                if (cp == null) {
                    sb.append("null");
                } else {
                    sb.append('"').append(escape(cp)).append('"');
                }
            }
            sb.append(']');
        }
        if (paquete.getMensajes() != null) {
            sb.append(",\"mensajes\":[");
            for (int i = 0; i < paquete.getMensajes().length; i++) {
                if (i > 0) {
                    sb.append(',');
                }
                RegistroMensaje m = paquete.getMensajes()[i];
                sb.append("{\"id\":").append(m.getId())
                        .append(",\"remitente\":\"").append(escape(m.getRemitente())).append('"')
                        .append(",\"destinatario\":\"").append(escape(m.getDestinatario())).append('"')
                        .append(",\"contenido\":\"").append(escape(m.getContenido())).append('"')
                        .append(",\"enviadoEn\":\"").append(escape(m.getEnviadoEn())).append("\"}");
            }
            sb.append(']');
        }
        sb.append('}');
        return sb.toString();
    }

    /**
     * Parsea una línea JSON en un paquete.
     *
     * @param json cadena JSON recibida
     * @return paquete con los campos presentes en el JSON
     */
    public static Paquete desdeJson(String json) {
        Map<String, String> map = parseFlat(json);
        Paquete p = new Paquete();
        if (map.containsKey("tipo")) {
            p.setTipo(TipoPaquete.valueOf(map.get("tipo")));
        }
        p.setToken(map.get("token"));
        p.setUsuario(map.get("usuario"));
        p.setContrasena(map.get("contrasena"));
        p.setDestinatario(map.get("destinatario"));
        p.setContenido(map.get("contenido"));
        p.setMensaje(map.get("mensaje"));
        if (json.contains("\"usuarios\"")) {
            p.setUsuarios(parseStringArray(json, "usuarios"));
        }
        if (json.contains("\"clavesPublicas\"")) {
            p.setClavesPublicas(parseStringArray(json, "clavesPublicas"));
        }
        p.setClavePublica(map.get("clavePublica"));
        if (json.contains("\"mensajes\"")) {
            p.setMensajes(parseMensajes(json));
        }
        return p;
    }

    private static boolean append(StringBuilder sb, String key, String value, boolean first) {
        if (value == null) {
            return first;
        }
        if (!first) {
            sb.append(',');
        }
        sb.append('"').append(key).append("\":\"").append(escape(value)).append('"');
        return false;
    }

    private static String escape(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
    }

    private static Map<String, String> parseFlat(String json) {
        Map<String, String> map = new LinkedHashMap<>();
        Matcher m = Pattern.compile("\"(\\w+)\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"")
                .matcher(json);
        while (m.find()) {
            map.put(m.group(1), unescape(m.group(2)));
        }
        Matcher num = Pattern.compile("\"(\\w+)\"\\s*:\\s*([0-9]+)").matcher(json);
        while (num.find()) {
            map.put(num.group(1), num.group(2));
        }
        return map;
    }

    private static String unescape(String s) {
        return s.replace("\\n", "\n").replace("\\r", "\r").replace("\\\"", "\"").replace("\\\\", "\\");
    }

    private static String[] parseStringArray(String json, String key) {
        int start = json.indexOf("\"" + key + "\"");
        if (start < 0) {
            return new String[0];
        }
        int arrStart = json.indexOf('[', start);
        int arrEnd = json.indexOf(']', arrStart);
        String body = json.substring(arrStart + 1, arrEnd);
        List<String> list = new ArrayList<>();
        Matcher m = Pattern.compile("\"((?:\\\\.|[^\"\\\\])*)\"")
                .matcher(body);
        while (m.find()) {
            list.add(unescape(m.group(1)));
        }
        return list.toArray(new String[0]);
    }

    private static RegistroMensaje[] parseMensajes(String json) {
        List<RegistroMensaje> list = new ArrayList<>();
        Matcher m = Pattern.compile(
                "\\{\"id\":(\\d+),\"remitente\":\"((?:\\\\.|[^\"\\\\])*)\",\"destinatario\":\"((?:\\\\.|[^\"\\\\])*)\",\"contenido\":\"((?:\\\\.|[^\"\\\\])*)\",\"enviadoEn\":\"((?:\\\\.|[^\"\\\\])*)\"\\}")
                .matcher(json);
        while (m.find()) {
            list.add(new RegistroMensaje(
                    Long.parseLong(m.group(1)),
                    unescape(m.group(2)),
                    unescape(m.group(3)),
                    unescape(m.group(4)),
                    unescape(m.group(5))));
        }
        return list.toArray(new RegistroMensaje[0]);
    }
}
