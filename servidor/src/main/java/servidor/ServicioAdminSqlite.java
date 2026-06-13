package servidor;

import comun.HasherContrasena;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Acceso JDBC de solo administración a {@code datos/mensajeria.db}.
 * Expone consultas, edición de usuarios/mensajes y utilidades de verificación BCrypt.
 */
public final class ServicioAdminSqlite implements AutoCloseable {
    private static final DateTimeFormatter FORMATO_MARCA_TIEMPO = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /** Contraseñas débiles habituales en demos; no sustituye un ataque real. */
    private static final List<String> DICCIONARIO_DEMO = Arrays.asList(
            "eliseo123", "eliseo2", "password", "123456", "admin", "test", "demo",
            "changeit", "mensajeria", "so2", "usuario", "contraseña", "contrasena"
    );

    private final Connection conexion;
    private final Path rutaBaseDatos;

    /** Abre la base de datos en modo lectura/escritura para la herramienta de administración. */
    public ServicioAdminSqlite(Path directorioDatos) throws IOException {
        try {
            Files.createDirectories(directorioDatos);
            rutaBaseDatos = directorioDatos.resolve("mensajeria.db");
            conexion = DriverManager.getConnection("jdbc:sqlite:" + rutaBaseDatos.toAbsolutePath());
            conexion.setAutoCommit(true);
        } catch (SQLException ex) {
            throw new IOException("No se pudo abrir " + directorioDatos.resolve("mensajeria.db"), ex);
        }
    }

    /** Devuelve la ruta absoluta del archivo SQLite. */
    public Path getRutaBaseDatos() {
        return rutaBaseDatos;
    }

    public List<String> listarTablas() throws SQLException {
        List<String> tablas = new ArrayList<>();
        try (PreparedStatement ps = conexion.prepareStatement(
                "SELECT name FROM sqlite_master WHERE type = 'table' AND name NOT LIKE 'sqlite_%' ORDER BY name");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                tablas.add(rs.getString(1));
            }
        }
        return tablas;
    }

    public ResultadoTabla consultarTabla(String nombreTabla, int limite) throws SQLException {
        validarNombreTabla(nombreTabla);
        String sql = "SELECT * FROM " + nombreTabla + " LIMIT " + limite;
        return ejecutarSeleccion(sql);
    }

    /** Ejecuta SQL arbitrario: SELECT devuelve filas; otros comandos informan filas afectadas. */
    public ResultadoTabla ejecutarSql(String sql) throws SQLException {
        String normalizado = sql.trim();
        if (normalizado.isEmpty()) {
            throw new SQLException("Escriba una consulta SQL");
        }
        if (esSoloLectura(normalizado)) {
            return ejecutarSeleccion(normalizado);
        }
        try (Statement st = conexion.createStatement()) {
            int filas = st.executeUpdate(normalizado);
            ResultadoTabla r = new ResultadoTabla();
            r.mensaje = "Comando ejecutado. Filas afectadas: " + filas;
            return r;
        }
    }

    private boolean esSoloLectura(String sql) {
        String inicio = sql.toLowerCase();
        return inicio.startsWith("select") || inicio.startsWith("pragma") || inicio.startsWith("explain");
    }

    private ResultadoTabla ejecutarSeleccion(String sql) throws SQLException {
        try (Statement st = conexion.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            return leerResultSet(rs);
        }
    }

    private ResultadoTabla leerResultSet(ResultSet rs) throws SQLException {
        ResultSetMetaData meta = rs.getMetaData();
        int columnas = meta.getColumnCount();
        ResultadoTabla resultado = new ResultadoTabla();
        for (int c = 1; c <= columnas; c++) {
            resultado.columnas.add(meta.getColumnLabel(c));
        }
        while (rs.next()) {
            List<Object> fila = new ArrayList<>();
            for (int c = 1; c <= columnas; c++) {
                fila.add(rs.getObject(c));
            }
            resultado.filas.add(fila);
        }
        return resultado;
    }

    public List<UsuarioRegistro> listarUsuarios() throws SQLException {
        List<UsuarioRegistro> lista = new ArrayList<>();
        String sql = "SELECT id, usuario, hash_contrasena, creado_en FROM usuarios ORDER BY usuario";
        try (PreparedStatement ps = conexion.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                lista.add(new UsuarioRegistro(
                        rs.getLong("id"),
                        rs.getString("usuario"),
                        rs.getString("hash_contrasena"),
                        rs.getString("creado_en")));
            }
        }
        return lista;
    }

    public void actualizarUsuario(long id, String usuario, String hashContrasena) throws SQLException {
        String sql = "UPDATE usuarios SET usuario = ?, hash_contrasena = ? WHERE id = ?";
        try (PreparedStatement ps = conexion.prepareStatement(sql)) {
            ps.setString(1, usuario.toLowerCase().trim());
            ps.setString(2, hashContrasena);
            ps.setLong(3, id);
            int n = ps.executeUpdate();
            if (n == 0) {
                throw new SQLException("No se encontró el usuario con id " + id);
            }
        }
    }

    public void cambiarContrasena(long id, String contrasenaPlana) throws SQLException {
        actualizarHash(id, HasherContrasena.hash(contrasenaPlana));
    }

    public void actualizarHash(long id, String hashContrasena) throws SQLException {
        String sql = "UPDATE usuarios SET hash_contrasena = ? WHERE id = ?";
        try (PreparedStatement ps = conexion.prepareStatement(sql)) {
            ps.setString(1, hashContrasena);
            ps.setLong(2, id);
            ps.executeUpdate();
        }
    }

    public void eliminarUsuario(long id) throws SQLException {
        try (PreparedStatement ps = conexion.prepareStatement("DELETE FROM usuarios WHERE id = ?")) {
            ps.setLong(1, id);
            ps.executeUpdate();
        }
    }

    public long insertarUsuario(String usuario, String contrasenaPlana) throws SQLException {
        String sql = "INSERT INTO usuarios (usuario, hash_contrasena, creado_en) VALUES (?, ?, ?)";
        try (PreparedStatement ps = conexion.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, usuario.toLowerCase().trim());
            ps.setString(2, HasherContrasena.hash(contrasenaPlana));
            ps.setString(3, FORMATO_MARCA_TIEMPO.format(LocalDateTime.now()));
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getLong(1);
                }
            }
        }
        throw new SQLException("No se obtuvo el id del nuevo usuario");
    }

    public boolean verificarContrasena(String hashAlmacenado, String contrasenaPlana) {
        return HasherContrasena.verificar(contrasenaPlana, hashAlmacenado);
    }

    /**
     * Prueba candidatos del diccionario demo y adicionales. BCrypt no permite descifrado real.
     */
    public String intentarRecuperarConDiccionario(String hashAlmacenado) {
        return intentarRecuperar(hashAlmacenado, List.of());
    }

    public String intentarRecuperar(String hashAlmacenado, Iterable<String> candidatosExtra) {
        for (String candidata : DICCIONARIO_DEMO) {
            if (coincide(hashAlmacenado, candidata)) {
                return candidata;
            }
        }
        if (candidatosExtra != null) {
            for (String candidata : candidatosExtra) {
                if (candidata != null && coincide(hashAlmacenado, candidata.trim())) {
                    return candidata.trim();
                }
            }
        }
        return null;
    }

    private static boolean coincide(String hashAlmacenado, String candidata) {
        return !candidata.isEmpty() && HasherContrasena.verificar(candidata, hashAlmacenado);
    }

    public List<MensajeRegistro> listarMensajes(int limite) throws SQLException {
        List<MensajeRegistro> lista = new ArrayList<>();
        String sql = "SELECT id, remitente, destinatario, contenido, enviado_en "
                + "FROM mensajes ORDER BY id DESC LIMIT ?";
        try (PreparedStatement ps = conexion.prepareStatement(sql)) {
            ps.setInt(1, limite);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(new MensajeRegistro(
                            rs.getLong("id"),
                            rs.getString("remitente"),
                            rs.getString("destinatario"),
                            rs.getString("contenido"),
                            rs.getString("enviado_en")));
                }
            }
        }
        return lista;
    }

    public void eliminarMensaje(long id) throws SQLException {
        try (PreparedStatement ps = conexion.prepareStatement("DELETE FROM mensajes WHERE id = ?")) {
            ps.setLong(1, id);
            ps.executeUpdate();
        }
    }

    public void actualizarMensaje(long id, String remitente, String destinatario, String contenido) throws SQLException {
        String sql = "UPDATE mensajes SET remitente = ?, destinatario = ?, contenido = ? WHERE id = ?";
        try (PreparedStatement ps = conexion.prepareStatement(sql)) {
            ps.setString(1, remitente.toLowerCase().trim());
            ps.setString(2, destinatario.toLowerCase().trim());
            ps.setString(3, contenido);
            ps.setLong(4, id);
            if (ps.executeUpdate() == 0) {
                throw new SQLException("Mensaje no encontrado: id " + id);
            }
        }
    }

    private static void validarNombreTabla(String nombre) throws SQLException {
        if (nombre == null || !nombre.matches("[a-zA-Z0-9_]+")) {
            throw new SQLException("Nombre de tabla no válido");
        }
    }

    @Override
    public void close() {
        try {
            conexion.close();
        } catch (SQLException ignored) {
        }
    }

    public static final class ResultadoTabla {
        public final List<String> columnas = new ArrayList<>();
        public final List<List<Object>> filas = new ArrayList<>();
        public String mensaje;
    }

    public static final class UsuarioRegistro {
        public final long id;
        public final String usuario;
        public final String hashContrasena;
        public final String creadoEn;

        public UsuarioRegistro(long id, String usuario, String hashContrasena, String creadoEn) {
            this.id = id;
            this.usuario = usuario;
            this.hashContrasena = hashContrasena;
            this.creadoEn = creadoEn;
        }

        @Override
        public String toString() {
            return id + " — " + usuario;
        }
    }

    public static final class MensajeRegistro {
        public final long id;
        public final String remitente;
        public final String destinatario;
        public final String contenido;
        public final String enviadoEn;

        public MensajeRegistro(long id, String remitente, String destinatario, String contenido, String enviadoEn) {
            this.id = id;
            this.remitente = remitente;
            this.destinatario = destinatario;
            this.contenido = contenido;
            this.enviadoEn = enviadoEn;
        }

        @Override
        public String toString() {
            return id + " · " + remitente + " → " + destinatario;
        }
    }
}
