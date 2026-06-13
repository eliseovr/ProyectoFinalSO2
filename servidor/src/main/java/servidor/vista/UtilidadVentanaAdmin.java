package servidor.vista;

import comun.RutasAplicacion;
import servidor.BaseDatosSqlite;
import servidor.ServicioAdminSqlite;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import java.io.IOException;
import java.nio.file.Path;

/**
 * Utilidad para abrir la ventana de administraciÃ³n SQLite desde el monitor u otras UIs.
 */
public final class UtilidadVentanaAdmin {
    private UtilidadVentanaAdmin() {
    }

    /** Abre el administrador usando {@code datos/} bajo la base de la aplicaciÃ³n. */
    public static void abrir() {
        abrir(RutasAplicacion.baseAplicacion().resolve("datos"));
    }

    /**
     * Abre el administrador sobre el directorio de datos indicado.
     * Ejecuta migraciones de esquema antes de conectar el servicio admin.
     */
    public static void abrir(Path directorioDatos) {
        SwingUtilities.invokeLater(() -> {
            try {
                try (BaseDatosSqlite ignorar = new BaseDatosSqlite(directorioDatos)) {
                    // Asegura que el esquema SQLite exista antes de abrir el admin.
                }
                ServicioAdminSqlite servicio = new ServicioAdminSqlite(directorioDatos);
                MarcoAdminBaseDatos marco = new MarcoAdminBaseDatos(servicio);
                marco.setVisible(true);
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(null,
                        "No se pudo abrir el administrador de base de datos:\n" + ex.getMessage(),
                        "Administrador BD",
                        JOptionPane.ERROR_MESSAGE);
            }
        });
    }
}

