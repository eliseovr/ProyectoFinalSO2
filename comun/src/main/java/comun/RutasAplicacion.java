package comun;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Resuelve rutas relativas al directorio base de la aplicación.
 * El directorio base se toma de la variable de entorno {@code APP_BASE}
 * o del directorio actual si no está definida.
 */
public final class RutasAplicacion {
    private RutasAplicacion() {
    }

    /**
     * Devuelve la ruta absoluta del directorio base de la aplicación.
     *
     * @return ruta normalizada de {@code APP_BASE} o {@code .}
     */
    public static Path baseAplicacion() {
        return Paths.get(System.getenv().getOrDefault("APP_BASE", ".").trim())
                .toAbsolutePath()
                .normalize();
    }

    /**
     * Resuelve una ruta desde una variable de entorno o un valor relativo predeterminado.
     * Las rutas absolutas se devuelven tal cual; las relativas se anclan a {@link #baseAplicacion()}.
     *
     * @param claveEntorno nombre de la variable de entorno
     * @param relativoPredeterminado ruta relativa si la variable no existe
     * @return ruta absoluta normalizada
     */
    public static Path resolver(String claveEntorno, String relativoPredeterminado) {
        String valor = System.getenv().getOrDefault(claveEntorno, relativoPredeterminado).trim();
        Path ruta = Paths.get(valor);
        if (ruta.isAbsolute()) {
            return ruta.normalize();
        }
        return baseAplicacion().resolve(ruta).normalize();
    }
}
