package cliente;

import cliente.modelo.SesionCliente;
import cliente.servicio.ServicioAutenticacion;
import cliente.servicio.ServicioMensajeria;
import cliente.vista.VistaChat;
import cliente.vista.VistaLogin;
import comun.RutasAplicacion;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.nio.file.Path;

/**
 * Punto de entrada del cliente con interfaz gráfica Swing (MVC).
 * Si la variable de entorno {@code HEADLESS=true}, delega en {@link ClienteConsola}.
 */
public class AplicacionCliente {
    /** Arranca login gráfico o cliente de consola según el entorno. */
    public static void main(String[] args) throws Exception {
        if ("true".equalsIgnoreCase(System.getenv("HEADLESS"))) {
            ClienteConsola.ejecutar(args);
            return;
        }

        Path keystoreServidor = RutasAplicacion.resolver("KEYSTORE_PATH", "certificados/server.p12");
        String hostPredeterminado = System.getenv().getOrDefault("SERVER_HOST", "localhost");
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
        }
        SwingUtilities.invokeLater(() -> new VistaLogin(keystoreServidor, hostPredeterminado,
                sesion -> new VistaChat(sesion).setVisible(true)
        ).setVisible(true));
    }
}
