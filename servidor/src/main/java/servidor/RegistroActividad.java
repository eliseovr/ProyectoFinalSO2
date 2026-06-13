package servidor;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

/**
 * Registro de eventos del sistema en consola, archivo y feed para el monitor gráfico.
 * Escribe en {@code sistema.log} (java.util.logging) y en {@code eventos.log}
 * (formato plano leído por {@link servidor.vista.MarcoMonitorServidor}).
 */
public class RegistroActividad {
    private static final DateTimeFormatter FORMATO = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final Logger logger;
    private final Path archivoEventos;
    private final Object bloqueoEscritura = new Object();

    /** Configura handlers de consola y archivo, y prepara {@code eventos.log}. */
    public RegistroActividad(Path directorioLogs) throws IOException {
        Files.createDirectories(directorioLogs);
        logger = Logger.getLogger("MensajeriaSO2");
        logger.setUseParentHandlers(false);

        ConsoleHandler consola = new ConsoleHandler();
        consola.setLevel(Level.INFO);
        consola.setFormatter(new SimpleFormatter());
        logger.addHandler(consola);

        FileHandler archivo = new FileHandler(directorioLogs.resolve("sistema.log").toString(), true);
        archivo.setLevel(Level.INFO);
        archivo.setFormatter(new SimpleFormatter());
        logger.addHandler(archivo);

        archivoEventos = directorioLogs.resolve("eventos.log");
    }

    /** Evento informativo (nivel INFO). */
    public void info(String evento) {
        escribir(Level.INFO, evento, null);
    }

    /** Advertencia operativa (nivel WARNING). */
    public void warn(String evento) {
        escribir(Level.WARNING, evento, null);
    }

    /** Error grave con traza opcional (nivel SEVERE). */
    public void error(String evento, Throwable t) {
        escribir(Level.SEVERE, evento, t);
    }

    /** Registro anonimizado de mensaje (sin contenido en claro). */
    public void mensajeEnviado(String remitente, String destinatario) {
        info("MENSAJE enviado de=" + remitente + " para=" + destinatario);
    }

    /** Registro de entrega en tiempo real al destinatario conectado. */
    public void mensajeRecibido(String destinatario, String remitente) {
        info("MENSAJE recibido por=" + destinatario + " de=" + remitente);
    }

    public void mensajePendiente(String destinatario, String remitente) {
        info("MENSAJE pendiente para=" + destinatario + " de=" + remitente + " (destinatario offline)");
    }

    private void escribir(Level nivel, String evento, Throwable t) {
        String marcaTiempo = FORMATO.format(LocalDateTime.now());
        String linea = "[" + marcaTiempo + "] " + etiquetaNivel(nivel) + " " + evento;
        if (t != null) {
            logger.log(nivel, "[" + marcaTiempo + "] " + evento, t);
        } else {
            logger.log(nivel, "[" + marcaTiempo + "] " + evento);
        }
        anexarLineaEvento(linea);
    }

    private static String etiquetaNivel(Level nivel) {
        if (nivel == Level.SEVERE) {
            return "ERROR";
        }
        if (nivel == Level.WARNING) {
            return "ADVERTENCIA";
        }
        return "INFO";
    }

    /** Anexa una línea con marca de tiempo a {@code eventos.log} de forma atómica. */
    private void anexarLineaEvento(String linea) {
        synchronized (bloqueoEscritura) {
            try (BufferedWriter escritor = Files.newBufferedWriter(
                    archivoEventos, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
                escritor.write(linea);
                escritor.newLine();
            } catch (IOException ex) {
                logger.warning("No se pudo escribir en eventos.log: " + ex.getMessage());
            }
        }
    }
}
