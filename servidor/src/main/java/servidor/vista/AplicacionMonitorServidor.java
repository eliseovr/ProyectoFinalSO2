package servidor.vista;

import comun.RutasAplicacion;
import comun.Protocolo;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.nio.file.Path;

/**
 * Punto de entrada del monitor grÃ¡fico del servidor.
 * Abre {@link MarcoMonitorServidor} sobre {@code registros/eventos.log}.
 */
public final class AplicacionMonitorServidor {
    private AplicacionMonitorServidor() {
    }

    /** Inicia la interfaz Swing del centro de control en el hilo de eventos. */
    public static void main(String[] args) {
        Path directorioLogs = RutasAplicacion.baseAplicacion().resolve("registros");
        Path archivoEventos = directorioLogs.resolve("eventos.log");
        int puerto = Integer.parseInt(System.getenv().getOrDefault("SERVER_PORT", String.valueOf(Protocolo.PUERTO_PREDETERMINADO)));

        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
        }

        SwingUtilities.invokeLater(() -> {
            MarcoMonitorServidor marco = new MarcoMonitorServidor(archivoEventos, puerto);
            marco.setVisible(true);
        });
    }
}

