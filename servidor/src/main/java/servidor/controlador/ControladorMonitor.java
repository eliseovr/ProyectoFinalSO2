package servidor.controlador;

import servidor.modelo.FiltroEvento;
import servidor.modelo.MetricasMonitor;
import servidor.modelo.ModeloMonitor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;

/**
 * Orquesta el sondeo del archivo de eventos y actualiza el modelo del monitor.
 */
public class ControladorMonitor {
    private final Path archivoEventos;
    private final ModeloMonitor modelo = new ModeloMonitor();
    private boolean pausado;

    public ControladorMonitor(Path archivoEventos) {
        this.archivoEventos = archivoEventos;
    }

    public ModeloMonitor getModelo() {
        return modelo;
    }

    public Path getArchivoEventos() {
        return archivoEventos;
    }

    public Path getDirectorioLogs() {
        return archivoEventos.getParent();
    }

    public void setPausado(boolean pausado) {
        this.pausado = pausado;
    }

    public boolean isPausado() {
        return pausado;
    }

    public ResultadoSondeo sondear() {
        if (pausado) {
            return ResultadoSondeo.pausado();
        }
        if (!Files.exists(archivoEventos)) {
            return ResultadoSondeo.esperando();
        }
        try {
            List<String> lineas = Files.readAllLines(archivoEventos, StandardCharsets.UTF_8);
            int previos = modelo.getEventos().size();
            modelo.sincronizarConArchivo(lineas);
            modelo.reconstruirUsuariosEnLinea();
            boolean hayNuevos = modelo.getEventos().size() != previos || lineas.size() > previos;
            return ResultadoSondeo.enVivo(hayNuevos, modelo.getEventos().size());
        } catch (IOException ex) {
            return ResultadoSondeo.error(ex.getMessage());
        }
    }

    public void forzarActualizacion() {
        modelo.reiniciar();
        sondear();
    }

    public void limpiarVista() {
        modelo.reiniciar();
    }

    public List<String> filtrar(String consulta, FiltroEvento filtro) {
        return modelo.filtrarEventos(consulta, filtro);
    }

    public Path exportarRegistro() throws IOException {
        if (modelo.getEventos().isEmpty()) {
            throw new IOException("No hay eventos para exportar");
        }
        Path salida = getDirectorioLogs().resolve("exportacion-monitor-" + System.currentTimeMillis() + ".txt");
        Files.write(salida, modelo.getEventos(), StandardCharsets.UTF_8, StandardOpenOption.CREATE);
        return salida;
    }

    public static final class ResultadoSondeo {
        public enum Estado { EN_VIVO, ESPERANDO, PAUSADO, ERROR }

        public final Estado estado;
        public final String mensaje;
        public final int totalEventos;

        private ResultadoSondeo(Estado estado, String mensaje, int totalEventos) {
            this.estado = estado;
            this.mensaje = mensaje;
            this.totalEventos = totalEventos;
        }

        static ResultadoSondeo enVivo(boolean hayNuevos, int total) {
            return new ResultadoSondeo(Estado.EN_VIVO, null, total);
        }

        static ResultadoSondeo esperando() {
            return new ResultadoSondeo(Estado.ESPERANDO, "Esperando que el servidor genere eventos...", 0);
        }

        static ResultadoSondeo pausado() {
            return new ResultadoSondeo(Estado.PAUSADO, "Actualización en pausa", 0);
        }

        static ResultadoSondeo error(String msg) {
            return new ResultadoSondeo(Estado.ERROR, msg, 0);
        }
    }
}
