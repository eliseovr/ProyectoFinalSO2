package servidor;

import comun.RegistroMensaje;
import comun.HasherContrasena;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Persistencia en SQLite (JDBC) para usuarios e historial de mensajes.
 * Esquema en español: tablas {@code usuarios} y {@code mensajes}.
 */
public class BaseDatosSqlite implements AutoCloseable {
    /** Formato de fecha/hora usado en columnas <code>creado_en</code> y <code>enviado_en</code>. */
    private static final DateTimeFormatter FORMATO_MARCA_TIEMPO = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final Connection conexion;
    private final Object bloqueo = new Object();

    /** Abre o crea la base de datos y aplica migraciones del esquema. */
    public BaseDatosSqlite(Path directorioDatos) throws IOException {
        try {
            Files.createDirectories(directorioDatos);
            Path archivoDb = directorioDatos.resolve("mensajeria.db");
            conexion = DriverManager.getConnection("jdbc:sqlite:" + archivoDb.toAbsolutePath());
            conexion.setAutoCommit(true);
            inicializarEsquema();
            migrarColumnaClavePublica();
        } catch (SQLException ex) {
            throw new IOException("No se pudo abrir la base de datos SQLite", ex);
        }
    }

    /** Crea las tablas {@code usuarios} y {@code mensajes} si no existen. */
    private void inicializarEsquema() throws SQLException {
        synchronized (bloqueo) {
            try (Statement st = conexion.createStatement()) {
                st.execute("CREATE TABLE IF NOT EXISTS usuarios ("
                        + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                        + "usuario TEXT NOT NULL UNIQUE,"
                        + "hash_contrasena TEXT NOT NULL,"
                        + "creado_en TEXT NOT NULL,"
                        + "clave_publica TEXT)");
                st.execute("CREATE TABLE IF NOT EXISTS mensajes ("
                        + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                        + "remitente TEXT NOT NULL,"
                        + "destinatario TEXT NOT NULL,"
                        + "contenido TEXT NOT NULL,"
                        + "enviado_en TEXT NOT NULL)");
            }
        }
    }

    private void migrarColumnaClavePublica() throws SQLException {
        synchronized (bloqueo) {
            if (!columnaExiste("usuarios", "clave_publica")) {
                try (Statement st = conexion.createStatement()) {
                    st.execute("ALTER TABLE usuarios ADD COLUMN clave_publica TEXT");
                }
            }
        }
    }

    private boolean columnaExiste(String tabla, String columna) throws SQLException {
        try (PreparedStatement ps = conexion.prepareStatement("PRAGMA table_info(" + tabla + ")")) {
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    if (columna.equalsIgnoreCase(rs.getString("name"))) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /** Registra un usuario nuevo con hash BCrypt y clave pública E2E. @return {@code false} si ya existe */
    public boolean registrarUsuario(String nombreUsuario, String contrasenaPlana, String clavePublicaBase64)
            throws IOException {
        synchronized (bloqueo) {
            try {
                if (existeUsuario(nombreUsuario)) {
                    return false;
                }
                String sql = "INSERT INTO usuarios (usuario, hash_contrasena, creado_en, clave_publica) "
                        + "VALUES (?, ?, ?, ?)";
                try (PreparedStatement ps = conexion.prepareStatement(sql)) {
                    ps.setString(1, nombreUsuario.toLowerCase());
                    ps.setString(2, HasherContrasena.hash(contrasenaPlana));
                    ps.setString(3, FORMATO_MARCA_TIEMPO.format(LocalDateTime.now()));
                    ps.setString(4, clavePublicaBase64);
                    ps.executeUpdate();
                    return true;
                }
            } catch (SQLException ex) {
                throw new IOException("Error al registrar usuario", ex);
            }
        }
    }

    /** Actualiza la clave pública RSA del usuario tras login o rotación local. */
    public void actualizarClavePublica(String nombreUsuario, String clavePublicaBase64) throws IOException {
        synchronized (bloqueo) {
            try {
                String sql = "UPDATE usuarios SET clave_publica = ? WHERE usuario = ?";
                try (PreparedStatement ps = conexion.prepareStatement(sql)) {
                    ps.setString(1, clavePublicaBase64);
                    ps.setString(2, nombreUsuario.toLowerCase());
                    if (ps.executeUpdate() == 0) {
                        throw new IOException("Usuario no encontrado");
                    }
                }
            } catch (SQLException ex) {
                throw new IOException("Error al guardar clave pública", ex);
            }
        }
    }

    public String obtenerClavePublica(String nombreUsuario) throws IOException {
        synchronized (bloqueo) {
            try {
                String sql = "SELECT clave_publica FROM usuarios WHERE usuario = ?";
                try (PreparedStatement ps = conexion.prepareStatement(sql)) {
                    ps.setString(1, nombreUsuario.toLowerCase());
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            return rs.getString("clave_publica");
                        }
                    }
                }
                return null;
            } catch (SQLException ex) {
                throw new IOException("Error al leer clave pública", ex);
            }
        }
    }

    public List<String> listarClavesPublicasOrdenadas() throws IOException {
        synchronized (bloqueo) {
            List<String> claves = new ArrayList<>();
            try (PreparedStatement ps = conexion.prepareStatement(
                    "SELECT clave_publica FROM usuarios ORDER BY usuario");
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String cp = rs.getString("clave_publica");
                    claves.add(cp != null ? cp : "");
                }
                return claves;
            } catch (SQLException ex) {
                throw new IOException("Error al listar claves públicas", ex);
            }
        }
    }

    /** Verifica usuario y contraseña contra el hash almacenado. */
    public boolean autenticar(String nombreUsuario, String contrasenaPlana) throws IOException {
        synchronized (bloqueo) {
            try {
                String hash = buscarHashUsuario(nombreUsuario);
                return hash != null && HasherContrasena.verificar(contrasenaPlana, hash);
            } catch (SQLException ex) {
                throw new IOException("Error de autenticación", ex);
            }
        }
    }

    public boolean existeUsuario(String nombreUsuario) throws IOException {
        synchronized (bloqueo) {
            try {
                return buscarHashUsuario(nombreUsuario) != null;
            } catch (SQLException ex) {
                throw new IOException("Error al consultar usuario", ex);
            }
        }
    }

    public List<String> listarUsuarios() throws IOException {
        synchronized (bloqueo) {
            List<String> usuarios = new ArrayList<>();
            try (PreparedStatement ps = conexion.prepareStatement(
                    "SELECT usuario FROM usuarios ORDER BY usuario");
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    usuarios.add(rs.getString("usuario"));
                }
                return usuarios;
            } catch (SQLException ex) {
                throw new IOException("Error al listar usuarios", ex);
            }
        }
    }

    /** Persiste un mensaje cifrado E2E y devuelve el registro con id generado. */
    public RegistroMensaje guardarMensaje(String remitente, String destinatario, String contenido) throws IOException {
        synchronized (bloqueo) {
            try {
                String de = remitente.toLowerCase();
                String para = destinatario.toLowerCase();
                String enviadoEn = FORMATO_MARCA_TIEMPO.format(LocalDateTime.now());
                String sql = "INSERT INTO mensajes (remitente, destinatario, contenido, enviado_en) "
                        + "VALUES (?, ?, ?, ?)";
                try (PreparedStatement ps = conexion.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setString(1, de);
                    ps.setString(2, para);
                    ps.setString(3, contenido);
                    ps.setString(4, enviadoEn);
                    ps.executeUpdate();
                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        if (keys.next()) {
                            long id = keys.getLong(1);
                            return new RegistroMensaje(id, de, para, contenido, enviadoEn);
                        }
                    }
                }
                throw new IOException("No se obtuvo el identificador del mensaje");
            } catch (SQLException ex) {
                throw new IOException("Error al guardar mensaje", ex);
            }
        }
    }

    /**
     * Historial del usuario: mensajes enviados y recibidos (últimos 200).
     */
    public List<RegistroMensaje> bandejaPara(String nombreUsuario) throws IOException {
        synchronized (bloqueo) {
            List<RegistroMensaje> lista = new ArrayList<>();
            String usuario = nombreUsuario.toLowerCase();
            String sql = "SELECT id, remitente, destinatario, contenido, enviado_en "
                    + "FROM mensajes WHERE remitente = ? OR destinatario = ? "
                    + "ORDER BY id DESC LIMIT 200";
            try (PreparedStatement ps = conexion.prepareStatement(sql)) {
                ps.setString(1, usuario);
                ps.setString(2, usuario);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        lista.add(new RegistroMensaje(
                                rs.getLong("id"),
                                rs.getString("remitente"),
                                rs.getString("destinatario"),
                                rs.getString("contenido"),
                                rs.getString("enviado_en")));
                    }
                }
                lista.sort((a, b) -> Long.compare(a.getId(), b.getId()));
                return lista;
            } catch (SQLException ex) {
                throw new IOException("Error al cargar bandeja", ex);
            }
        }
    }

    private String buscarHashUsuario(String nombreUsuario) throws SQLException {
        String sql = "SELECT hash_contrasena FROM usuarios WHERE usuario = ?";
        try (PreparedStatement ps = conexion.prepareStatement(sql)) {
            ps.setString(1, nombreUsuario.toLowerCase());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("hash_contrasena");
                }
            }
        }
        return null;
    }

    @Override
    public void close() {
        synchronized (bloqueo) {
            try {
                conexion.close();
            } catch (SQLException ignored) {
            }
        }
    }
}

