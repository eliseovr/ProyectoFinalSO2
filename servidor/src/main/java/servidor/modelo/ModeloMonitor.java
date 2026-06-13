package servidor.modelo;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

/**
 * Estado y lógica de parseo del archivo de eventos del servidor.
 */
public class ModeloMonitor {
    private static final int MAX_EVENTOS = 2000;
    private static final int MAX_FEED = 8;

    private final List<String> todosLosEventos = new ArrayList<>();
    private final List<String> feedActividad = new ArrayList<>();
    private final MetricasMonitor metricas = new MetricasMonitor();
    private final Set<String> usuariosEnLinea = new LinkedHashSet<>();

    public List<String> getEventos() {
        return todosLosEventos;
    }

    public List<String> getFeedActividad() {
        return feedActividad;
    }

    public MetricasMonitor getMetricas() {
        return metricas;
    }

    public Set<String> getUsuariosEnLinea() {
        return new TreeSet<>(usuariosEnLinea);
    }

    public void reiniciar() {
        todosLosEventos.clear();
        feedActividad.clear();
        metricas.reiniciar();
        usuariosEnLinea.clear();
    }

    public void sincronizarConArchivo(List<String> lineas) {
        if (lineas.size() < todosLosEventos.size()) {
            reiniciar();
            for (String linea : lineas) {
                procesarEvento(linea);
            }
            return;
        }
        if (lineas.size() > todosLosEventos.size()) {
            for (int i = todosLosEventos.size(); i < lineas.size(); i++) {
                procesarEvento(lineas.get(i));
            }
        }
    }

    public List<String> filtrarEventos(String consulta, FiltroEvento filtro) {
        String q = consulta != null ? consulta.trim().toLowerCase(Locale.ROOT) : "";
        FiltroEvento f = filtro != null ? filtro : FiltroEvento.TODOS;
        List<String> resultado = new ArrayList<>();
        for (String linea : todosLosEventos) {
            if (!coincideConFiltro(linea, f)) {
                continue;
            }
            if (!q.isEmpty() && !linea.toLowerCase(Locale.ROOT).contains(q)) {
                continue;
            }
            resultado.add(lineaLegible(linea));
        }
        return resultado;
    }

    private void procesarEvento(String linea) {
        if (linea.isBlank()) {
            return;
        }
        todosLosEventos.add(linea);
        aplicarMetricas(linea);
        agregarAlFeed(linea);
        aplicarEnLineaDesdeLinea(linea, usuariosEnLinea);
        if (todosLosEventos.size() > MAX_EVENTOS) {
            todosLosEventos.remove(0);
        }
    }

    private void agregarAlFeed(String linea) {
        feedActividad.add(0, resumirLinea(lineaLegible(linea)));
        while (feedActividad.size() > MAX_FEED) {
            feedActividad.remove(feedActividad.size() - 1);
        }
    }

    private void aplicarMetricas(String linea) {
        if (linea.contains("CONEXION nueva")) {
            metricas.conexiones++;
        }
        if (esInicioSesionExitoso(linea)) {
            metricas.loginExitoso++;
        }
        if (esInicioSesionFallido(linea)) {
            metricas.loginFallido++;
        }
        if (linea.contains("REGISTRO exitoso")) {
            metricas.registros++;
        }
        if (linea.contains("MENSAJE enviado")) {
            metricas.mensajesEnviados++;
        }
        if (linea.contains("MENSAJE recibido")) {
            metricas.mensajesRecibidos++;
        }
        if (linea.contains("ADVERTENCIA") || linea.contains("fallido") || linea.contains("fallo")) {
            metricas.advertencias++;
        }
        if (linea.contains("] ERROR ") || linea.contains("SEVERE")) {
            metricas.errores++;
        }
    }

    public void reconstruirUsuariosEnLinea() {
        usuariosEnLinea.clear();
        for (String linea : todosLosEventos) {
            aplicarEnLineaDesdeLinea(linea, usuariosEnLinea);
        }
    }

    private static void aplicarEnLineaDesdeLinea(String linea, Set<String> enLinea) {
        if (linea.contains("INICIO_SESION exitoso: ")) {
            enLinea.add(extraerSufijo(linea, "INICIO_SESION exitoso: "));
        } else if (linea.contains("LOGIN exitoso: ")) {
            enLinea.add(extraerSufijo(linea, "LOGIN exitoso: "));
        } else if (linea.contains("CIERRE_SESION (desconexion): ")) {
            enLinea.remove(extraerSufijo(linea, "CIERRE_SESION (desconexion): "));
        } else if (linea.contains("LOGOUT (desconexion): ") || linea.contains("LOGOUT (desconexión): ")) {
            enLinea.remove(extraerSufijo(linea, linea.contains("LOGOUT (desconexion): ")
                    ? "LOGOUT (desconexion): " : "LOGOUT (desconexión): "));
        } else if (linea.contains("CIERRE_SESION: ")) {
            enLinea.remove(extraerSufijo(linea, "CIERRE_SESION: "));
        } else if (linea.contains("LOGOUT: ")) {
            enLinea.remove(extraerSufijo(linea, "LOGOUT: "));
        }
    }

    public static boolean coincideConFiltro(String linea, FiltroEvento filtro) {
        if (filtro == FiltroEvento.TODOS) {
            return true;
        }
        if (filtro == FiltroEvento.CONEXION) {
            return linea.contains("CONEXION");
        }
        if (filtro == FiltroEvento.AUTENTICACION) {
            return linea.contains("INICIO_SESION") || linea.contains("LOGIN")
                    || linea.contains("CIERRE_SESION") || linea.contains("LOGOUT")
                    || linea.contains("REGISTRO");
        }
        if (filtro == FiltroEvento.MENSAJES) {
            return linea.contains("MENSAJE");
        }
        return linea.contains("ERROR") || linea.contains("ADVERTENCIA")
                || linea.contains("fallido") || linea.contains("fallo");
    }

    public static String resumirLinea(String linea) {
        int idx = linea.indexOf("] INFO ");
        if (idx < 0) {
            idx = linea.indexOf("] ADVERTENCIA ");
        }
        if (idx < 0) {
            idx = linea.indexOf("] ERROR ");
        }
        String cuerpo = idx >= 0 ? linea.substring(idx).replaceFirst("\\] (INFO|ADVERTENCIA|ERROR) ", "") : linea;
        if (cuerpo.length() > 72) {
            return cuerpo.substring(0, 69) + "...";
        }
        return cuerpo;
    }

    public static String lineaLegible(String linea) {
        String s = linea;
        s = s.replace("INICIO_SESION exitoso", "Inicio de sesión exitoso");
        s = s.replace("INICIO_SESION fallido", "Inicio de sesión fallido");
        s = s.replace("INICIO_SESION:", "Inicio de sesión:");
        s = s.replace("LOGIN exitoso", "Inicio de sesión exitoso");
        s = s.replace("LOGIN fallido", "Inicio de sesión fallido");
        s = s.replace("CIERRE_SESION (desconexion)", "Cierre de sesión (desconexión)");
        s = s.replace("CIERRE_SESION:", "Cierre de sesión:");
        s = s.replace("LOGOUT (desconexion)", "Cierre de sesión (desconexión)");
        s = s.replace("LOGOUT (desconexión)", "Cierre de sesión (desconexión)");
        s = s.replace("LOGOUT:", "Cierre de sesión:");
        return s;
    }

    private static String extraerSufijo(String linea, String marcador) {
        int idx = linea.indexOf(marcador);
        return idx < 0 ? "" : linea.substring(idx + marcador.length()).trim();
    }

    private static boolean esInicioSesionExitoso(String linea) {
        return linea.contains("INICIO_SESION exitoso") || linea.contains("LOGIN exitoso");
    }

    private static boolean esInicioSesionFallido(String linea) {
        return linea.contains("INICIO_SESION fallido") || linea.contains("LOGIN fallido");
    }
}
