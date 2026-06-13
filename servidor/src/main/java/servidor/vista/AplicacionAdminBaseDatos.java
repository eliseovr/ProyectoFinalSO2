package servidor.vista;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.nio.file.Path;

/**
 * Punto de entrada independiente del administrador de base de datos SQLite.
 */
public final class AplicacionAdminBaseDatos {
    private AplicacionAdminBaseDatos() {
    }

    /** Lanza {@link MarcoAdminBaseDatos} con el look-and-feel del sistema. */
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
        }
        SwingUtilities.invokeLater(UtilidadVentanaAdmin::abrir);
    }
}

