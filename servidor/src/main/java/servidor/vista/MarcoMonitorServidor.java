package servidor.vista;

import comun.TemaVisual;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.text.BadLocationException;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

/**
 * Panel de monitoreo del servidor con estilo minimalista y mÃ©tricas en vivo.
 * Lee {@code eventos.log} periÃ³dicamente y muestra conexiones, autenticaciÃ³n y mensajes.
 */
public class MarcoMonitorServidor extends JFrame {
    /** Intervalo entre lecturas del archivo de eventos (milisegundos). */
    private static final int INTERVALO_SONDEO_MS = 400;
    /** LÃ­mite de lÃ­neas visibles en el panel de registro (reservado para recorte futuro). */
    private static final int MAX_LINEAS = 500;
    /** Formato de la hora mostrada en la barra superior. */
    private static final DateTimeFormatter FORMATO_RELOJ = DateTimeFormatter.ofPattern("HH:mm:ss");

    /** CategorÃ­as de filtrado del registro de eventos. */
    private enum FiltroEvento {
        TODOS("Todos los eventos"),
        CONEXION("Conexiones"),
        AUTENTICACION("AutenticaciÃ³n"),
        MENSAJES("Mensajes"),
        ERRORES("Errores y advertencias");

        final String etiqueta;

        FiltroEvento(String etiqueta) {
            this.etiqueta = etiqueta;
        }

        @Override
        public String toString() {
            return etiqueta;
        }
    }

    private final Path archivoEventos;
    private final Path directorioLogs;
    private final int puertoServidor;
    /** Marca de tiempo del Ãºltimo sondeo con datos nuevos. */
    private long ultimoSondeoMs;
    /** Si estÃ¡ activo, no se actualiza la vista automÃ¡ticamente. */
    private boolean pausado;

    private int contadorConexiones;
    private int contadorLoginExitoso;
    private int contadorLoginFallido;
    private int contadorRegistros;
    private int contadorMensajesEnviados;
    private int contadorMensajesRecibidos;
    private int contadorAdvertencias;
    private int contadorErrores;
    private int lineasMostradas;

    private final List<String> todosLosEventos = new ArrayList<>();
    private final DefaultListModel<String> modeloEnLinea = new DefaultListModel<>();
    private final DefaultListModel<String> modeloActividad = new DefaultListModel<>();
    private final JTextPane panelRegistro = new JTextPane();
    private final JTextField campoBusqueda = new JTextField();
    private final JComboBox<FiltroEvento> comboFiltro = new JComboBox<>(FiltroEvento.values());
    private final JCheckBox casillaPausa = new JCheckBox("Pausar actualizaciÃ³n");
    private final JLabel indicadorEstado = new JLabel("\u25CF");
    private final JLabel etiquetaEstado = new JLabel("Esperando servidor...");
    private final JLabel etiquetaReloj = TemaVisual.etiquetaTenue("");

    private JPanel tarjetaConexiones;
    private JPanel tarjetaIniciosSesion;
    private JPanel tarjetaMensajes;
    private JPanel tarjetaEnLinea;
    private JLabel valorTarjetaConexiones;
    private JLabel valorTarjetaIniciosSesion;
    private JLabel valorTarjetaMensajes;
    private JLabel valorTarjetaEnLinea;

    /**
     * Crea el centro de control y comienza el sondeo del archivo de eventos.
     *
     * @param archivoEventos ruta a {@code registros/eventos.log}
     * @param puertoServidor puerto TLS del servidor (solo informativo en la UI)
     */
    public MarcoMonitorServidor(Path archivoEventos, int puertoServidor) {
        super("MensajerÃ­a SO2 â€” Centro de control del servidor");
        this.archivoEventos = archivoEventos;
        this.directorioLogs = archivoEventos.getParent();
        this.puertoServidor = puertoServidor;
        construirInterfaz();
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setMinimumSize(new Dimension(960, 640));
        setSize(1040, 700);
        setLocationRelativeTo(null);
        iniciarSondeo();
        iniciarReloj();
    }

    /** Construye la interfaz: barra superior, tarjetas de mÃ©tricas, filtros y paneles laterales. */
    private void construirInterfaz() {
        JPanel root = new JPanel(new BorderLayout(0, 0));
        TemaVisual.aplicarEstiloMarco(root);

        indicadorEstado.setForeground(TemaVisual.ADVERTENCIA);
        indicadorEstado.setFont(indicadorEstado.getFont().deriveFont(Font.BOLD, 18f));

        JLabel title = TemaVisual.etiquetaTitulo("Centro de control del servidor");
        JPanel titleBox = new JPanel();
        titleBox.setLayout(new javax.swing.BoxLayout(titleBox, javax.swing.BoxLayout.Y_AXIS));
        titleBox.setOpaque(false);
        titleBox.add(title);
        etiquetaEstado.setForeground(TemaVisual.TEXTO_TENUE);
        titleBox.add(etiquetaEstado);

        JLabel tlsBadge = new JLabel("  TLS Â· Puerto " + puertoServidor + "  ");
        tlsBadge.setOpaque(true);
        tlsBadge.setBackground(new Color(227, 242, 253));
        tlsBadge.setForeground(TemaVisual.PRIMARIO);
        tlsBadge.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));

        JPanel topBar = TemaVisual.barraEncabezado(
                combinar(indicadorEstado, titleBox),
                TemaVisual.etiquetaTenue("Monitoreo en tiempo real"),
                combinar(tlsBadge, etiquetaReloj));

        tarjetaConexiones = TemaVisual.crearTarjetaEstadistica("Conexiones", "0", TemaVisual.PRIMARIO);
        tarjetaIniciosSesion = TemaVisual.crearTarjetaEstadistica("Inicios de sesiÃ³n", "0", TemaVisual.EXITO);
        tarjetaMensajes = TemaVisual.crearTarjetaEstadistica("Mensajes", "0", new Color(108, 92, 231));
        tarjetaEnLinea = TemaVisual.crearTarjetaEstadistica("En lÃ­nea ahora", "0", TemaVisual.ADVERTENCIA);
        valorTarjetaConexiones = etiquetaValor(tarjetaConexiones);
        valorTarjetaIniciosSesion = etiquetaValor(tarjetaIniciosSesion);
        valorTarjetaMensajes = etiquetaValor(tarjetaMensajes);
        valorTarjetaEnLinea = etiquetaValor(tarjetaEnLinea);

        JPanel statsRow = TemaVisual.crearFilaEstadisticas(tarjetaConexiones, tarjetaIniciosSesion, tarjetaMensajes, tarjetaEnLinea);
        statsRow.setBorder(BorderFactory.createEmptyBorder(12, 16, 8, 16));

        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
        toolbar.setOpaque(false);
        toolbar.setBorder(BorderFactory.createEmptyBorder(0, 12, 8, 12));

        TemaVisual.aplicarEstiloCampoTexto(campoBusqueda);
        campoBusqueda.setPreferredSize(new Dimension(200, 36));
        campoBusqueda.setToolTipText("Buscar texto en los eventos");
        campoBusqueda.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) {
                actualizarVistaRegistro();
            }

            public void removeUpdate(javax.swing.event.DocumentEvent e) {
                actualizarVistaRegistro();
            }

            public void changedUpdate(javax.swing.event.DocumentEvent e) {
                actualizarVistaRegistro();
            }
        });

        comboFiltro.addActionListener(e -> actualizarVistaRegistro());
        casillaPausa.setForeground(TemaVisual.TEXTO);
        casillaPausa.setOpaque(false);
        casillaPausa.addActionListener(e -> pausado = casillaPausa.isSelected());

        JButton refreshBtn = new JButton("Actualizar");
        TemaVisual.aplicarEstiloBotonFantasma(refreshBtn);
        refreshBtn.addActionListener(e -> forzarActualizacion());

        JButton exportBtn = new JButton("Exportar log");
        TemaVisual.aplicarEstiloBotonFantasma(exportBtn);
        exportBtn.addActionListener(e -> exportarRegistro());

        JButton clearBtn = new JButton("Limpiar");
        TemaVisual.aplicarEstiloBotonSecundario(clearBtn);
        clearBtn.addActionListener(e -> limpiarVista());

        JButton openLogsBtn = new JButton("Carpeta logs");
        TemaVisual.aplicarEstiloBotonSecundario(openLogsBtn);
        openLogsBtn.addActionListener(e -> abrirCarpetaLogs());

        JButton adminDbBtn = new JButton("Base de datos");
        TemaVisual.aplicarEstiloBotonPrimario(adminDbBtn);
        adminDbBtn.setToolTipText("Consultas SQL, usuarios, hashes y mensajes");
        adminDbBtn.addActionListener(e -> UtilidadVentanaAdmin.abrir());

        toolbar.add(new JLabel("Buscar:"));
        toolbar.add(campoBusqueda);
        toolbar.add(new JLabel("Filtro:"));
        toolbar.add(comboFiltro);
        toolbar.add(casillaPausa);
        toolbar.add(refreshBtn);
        toolbar.add(exportBtn);
        toolbar.add(clearBtn);
        toolbar.add(openLogsBtn);
        toolbar.add(adminDbBtn);

        panelRegistro.setEditable(false);
        panelRegistro.setFont(new Font("Consolas", Font.PLAIN, 12));
        panelRegistro.setBackground(TemaVisual.TARJETA);
        JScrollPane logScroll = new JScrollPane(panelRegistro);
        logScroll.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(TemaVisual.BORDE),
                BorderFactory.createEmptyBorder(4, 4, 4, 4)));

        JList<String> onlineList = new JList<>(modeloEnLinea);
        onlineList.setFont(onlineList.getFont().deriveFont(Font.BOLD, 13f));
        onlineList.setBackground(TemaVisual.TARJETA);
        JScrollPane onlineScroll = new JScrollPane(onlineList);
        onlineScroll.setBorder(TemaVisual.tituloSeccion("Usuarios en lÃ­nea"));

        JList<String> feedList = new JList<>(modeloActividad);
        feedList.setBackground(TemaVisual.TARJETA);
        JScrollPane feedScroll = new JScrollPane(feedList);
        feedScroll.setBorder(TemaVisual.tituloSeccion("Actividad reciente"));
        feedScroll.setPreferredSize(new Dimension(0, 140));

        JPanel sidePanel = new JPanel(new BorderLayout(0, 10));
        sidePanel.setOpaque(false);
        sidePanel.setBorder(BorderFactory.createEmptyBorder(0, 12, 12, 0));
        sidePanel.add(onlineScroll, BorderLayout.CENTER);
        sidePanel.add(feedScroll, BorderLayout.SOUTH);
        sidePanel.setPreferredSize(new Dimension(220, 0));

        JPanel logPanel = new JPanel(new BorderLayout());
        logPanel.setOpaque(false);
        logPanel.setBorder(BorderFactory.createEmptyBorder(0, 12, 12, 12));
        JLabel logTitle = new JLabel("Registro de eventos");
        logTitle.setFont(logTitle.getFont().deriveFont(Font.BOLD, 13f));
        logTitle.setForeground(TemaVisual.TEXTO);
        logTitle.setBorder(BorderFactory.createEmptyBorder(0, 0, 6, 0));
        logPanel.add(logTitle, BorderLayout.NORTH);
        logPanel.add(logScroll, BorderLayout.CENTER);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, sidePanel, logPanel);
        split.setResizeWeight(0.22);
        split.setDividerLocation(230);
        split.setBorder(BorderFactory.createEmptyBorder());

        JPanel body = new JPanel(new BorderLayout());
        body.setOpaque(false);
        body.add(statsRow, BorderLayout.NORTH);

        JPanel main = new JPanel(new BorderLayout());
        main.setOpaque(false);
        main.add(toolbar, BorderLayout.NORTH);
        main.add(split, BorderLayout.CENTER);
        body.add(main, BorderLayout.CENTER);

        root.add(topBar, BorderLayout.NORTH);
        root.add(body, BorderLayout.CENTER);
        setContentPane(root);
        anexarLineaSistema("Monitor listo. Inicie el servidor para ver actividad.", TemaVisual.TEXTO_TENUE);
    }

    /** Agrupa componentes en una fila horizontal sin fondo. */
    private static JPanel combinar(JComponent... partes) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        p.setOpaque(false);
        for (JComponent c : partes) {
            p.add(c);
        }
        return p;
    }

    /** Localiza la etiqueta numÃ©rica grande dentro de una tarjeta de estadÃ­stica. */
    private static JLabel etiquetaValor(JPanel tarjeta) {
        for (int i = 0; i < tarjeta.getComponentCount(); i++) {
            java.awt.Component c = tarjeta.getComponent(i);
            if (c instanceof JLabel) {
                JLabel lbl = (JLabel) c;
                if (lbl.getFont().getSize() >= 18) {
                    return lbl;
                }
            }
        }
        return new JLabel("0");
    }

    /** Actualiza la hora en la barra superior cada segundo. */
    private void iniciarReloj() {
        Timer reloj = new Timer(1000, e -> etiquetaReloj.setText(FORMATO_RELOJ.format(LocalDateTime.now())));
        reloj.start();
    }

    /** Inicia el temporizador que sondea el archivo de eventos. */
    private void iniciarSondeo() {
        Timer temporizador = new Timer(INTERVALO_SONDEO_MS, e -> sondearArchivoEventos());
        temporizador.start();
    }

    /** Reinicia contadores y vuelve a leer todo el archivo de eventos. */
    private void forzarActualizacion() {
        todosLosEventos.clear();
        reiniciarContadores();
        sondearArchivoEventos();
        actualizarVistaRegistro();
    }

    /**
     * Lee nuevas lÃ­neas de {@code eventos.log}, actualiza mÃ©tricas y la vista.
     * Si el archivo fue truncado o rotado, reconstruye el estado desde cero.
     */
    private void sondearArchivoEventos() {
        if (pausado) {
            etiquetaEstado.setText("ActualizaciÃ³n en pausa");
            return;
        }

        if (!Files.exists(archivoEventos)) {
            indicadorEstado.setForeground(TemaVisual.ADVERTENCIA);
            etiquetaEstado.setText("Esperando que el servidor genere eventos...");
            return;
        }

        try {
            List<String> lineas = Files.readAllLines(archivoEventos, StandardCharsets.UTF_8);
            boolean hayNuevos = false;
            if (lineas.size() < todosLosEventos.size()) {
                // Archivo reiniciado o truncado
                todosLosEventos.clear();
                reiniciarContadores();
                for (String linea : lineas) {
                    procesarEvento(linea);
                }
                hayNuevos = !lineas.isEmpty();
            } else if (lineas.size() > todosLosEventos.size()) {
                for (int i = todosLosEventos.size(); i < lineas.size(); i++) {
                    procesarEvento(lineas.get(i));
                }
                hayNuevos = true;
            }
            if (hayNuevos) {
                ultimoSondeoMs = System.currentTimeMillis();
            }
            indicadorEstado.setForeground(TemaVisual.EXITO);
            etiquetaEstado.setText("En vivo Â· " + archivoEventos.getFileName() + " Â· "
                    + todosLosEventos.size() + " eventos");
            actualizarListaEnLinea();
            actualizarTarjetasEstadisticas();
            actualizarVistaRegistro();
        } catch (IOException ex) {
            indicadorEstado.setForeground(TemaVisual.PELIGRO);
            etiquetaEstado.setText("Error leyendo eventos: " + ex.getMessage());
        }
    }

    /** Incorpora una lÃ­nea de log al buffer interno y actualiza mÃ©tricas y feed. */
    private void procesarEvento(String linea) {
        if (linea.isBlank()) {
            return;
        }
        todosLosEventos.add(linea);
        aplicarMetricas(linea);
        agregarAlFeed(linea);
        if (todosLosEventos.size() > 2000) {
            todosLosEventos.remove(0);
        }
    }

    /** AÃ±ade un resumen al panel de actividad reciente (mÃ¡ximo 8 entradas). */
    private void agregarAlFeed(String linea) {
        String lineaCorta = resumirLinea(lineaLegible(linea));
        modeloActividad.add(0, lineaCorta);
        while (modeloActividad.size() > 8) {
            modeloActividad.remove(modeloActividad.size() - 1);
        }
    }

    /** Extrae el mensaje Ãºtil de una lÃ­nea de log y lo acorta para el feed lateral. */
    private static String resumirLinea(String linea) {
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

    /** Incrementa contadores segÃºn el tipo de evento detectado en la lÃ­nea. */
    private void aplicarMetricas(String linea) {
        if (linea.contains("CONEXION nueva")) {
            contadorConexiones++;
        }
        if (esInicioSesionExitoso(linea)) {
            contadorLoginExitoso++;
        }
        if (esInicioSesionFallido(linea)) {
            contadorLoginFallido++;
        }
        if (linea.contains("REGISTRO exitoso")) {
            contadorRegistros++;
        }
        if (linea.contains("MENSAJE enviado")) {
            contadorMensajesEnviados++;
        }
        if (linea.contains("MENSAJE recibido")) {
            contadorMensajesRecibidos++;
        }
        if (linea.contains("ADVERTENCIA") || linea.contains("fallido") || linea.contains("fallo")) {
            contadorAdvertencias++;
        }
        if (linea.contains("] ERROR ") || linea.contains("SEVERE")) {
            contadorErrores++;
        }
    }

    /** Refresca los valores numÃ©ricos de las tarjetas de estadÃ­sticas. */
    private void actualizarTarjetasEstadisticas() {
        valorTarjetaConexiones.setText(String.valueOf(contadorConexiones));
        valorTarjetaIniciosSesion.setText(contadorLoginExitoso + " correctos Â· " + contadorLoginFallido
                + " fallidos Â· " + contadorRegistros + " registros");
        valorTarjetaMensajes.setText(contadorMensajesEnviados + " enviados Â· "
                + contadorMensajesRecibidos + " recibidos");
        valorTarjetaEnLinea.setText(String.valueOf(modeloEnLinea.size()));
    }

    /** Reconstruye la lista de usuarios en lÃ­nea a partir de todo el historial de eventos. */
    private void actualizarListaEnLinea() {
        Set<String> enLinea = new LinkedHashSet<>();
        for (String linea : todosLosEventos) {
            aplicarEnLineaDesdeLinea(linea, enLinea);
        }
        modeloEnLinea.clear();
        for (String usuario : new TreeSet<>(enLinea)) {
            modeloEnLinea.addElement(usuario);
        }
    }

    /** Actualiza el conjunto de usuarios en lÃ­nea segÃºn login/logout en una lÃ­nea. */
    private static void aplicarEnLineaDesdeLinea(String linea, Set<String> enLinea) {
        if (linea.contains("INICIO_SESION exitoso: ")) {
            enLinea.add(extraerSufijo(linea, "INICIO_SESION exitoso: "));
        } else if (linea.contains("LOGIN exitoso: ")) {
            enLinea.add(extraerSufijo(linea, "LOGIN exitoso: "));
        } else if (linea.contains("CIERRE_SESION (desconexion): ")) {
            enLinea.remove(extraerSufijo(linea, "CIERRE_SESION (desconexion): "));
        } else if (linea.contains("LOGOUT (desconexion): ") || linea.contains("LOGOUT (desconexiÃ³n): ")) {
            enLinea.remove(extraerSufijo(linea, linea.contains("LOGOUT (desconexion): ")
                    ? "LOGOUT (desconexion): " : "LOGOUT (desconexiÃ³n): "));
        } else if (linea.contains("CIERRE_SESION: ")) {
            enLinea.remove(extraerSufijo(linea, "CIERRE_SESION: "));
        } else if (linea.contains("LOGOUT: ")) {
            enLinea.remove(extraerSufijo(linea, "LOGOUT: "));
        }
    }

    /** Obtiene el texto que sigue a un marcador dentro de una lÃ­nea de log. */
    private static String extraerSufijo(String linea, String marcador) {
        int idx = linea.indexOf(marcador);
        return idx < 0 ? "" : linea.substring(idx + marcador.length()).trim();
    }

    /** Redibuja el panel de registro aplicando filtro de categorÃ­a y bÃºsqueda de texto. */
    private void actualizarVistaRegistro() {
        String consulta = campoBusqueda.getText().trim().toLowerCase(Locale.ROOT);
        FiltroEvento seleccionado = (FiltroEvento) comboFiltro.getSelectedItem();
        final FiltroEvento filtro = seleccionado != null ? seleccionado : FiltroEvento.TODOS;

        SwingUtilities.invokeLater(() -> {
            panelRegistro.setText("");
            lineasMostradas = 0;
            for (String linea : todosLosEventos) {
                if (!coincideConFiltro(linea, filtro)) {
                    continue;
                }
                if (!consulta.isEmpty() && !linea.toLowerCase(Locale.ROOT).contains(consulta)) {
                    continue;
                }
                anexarLineaEvento(lineaLegible(linea), colorDeLinea(linea));
                lineasMostradas++;
            }
            if (lineasMostradas == 0) {
                anexarLineaSistema("Sin eventos para el filtro actual.", TemaVisual.TEXTO_TENUE);
            }
        });
    }

    /** Indica si una lÃ­nea de log pertenece a la categorÃ­a de filtro seleccionada. */
    private static boolean coincideConFiltro(String linea, FiltroEvento filtro) {
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

    /** Asigna color al texto segÃºn la severidad o tipo de evento. */
    private static Color colorDeLinea(String linea) {
        if (linea.contains("] ERROR ") || linea.contains("SEVERE")) {
            return TemaVisual.PELIGRO;
        }
        if (linea.contains("ADVERTENCIA") || linea.contains("fallido")) {
            return TemaVisual.ADVERTENCIA;
        }
        if (esInicioSesionExitoso(linea) || linea.contains("REGISTRO exitoso") || linea.contains("SERVIDOR iniciado")) {
            return TemaVisual.EXITO;
        }
        if (linea.contains("CIERRE_SESION") || linea.contains("LOGOUT")) {
            return new Color(120, 90, 60);
        }
        if (linea.contains("MENSAJE")) {
            return TemaVisual.PRIMARIO;
        }
        if (linea.contains("CONEXION")) {
            return TemaVisual.TEXTO_TENUE;
        }
        return TemaVisual.TEXTO;
    }

    /** AÃ±ade una lÃ­nea de evento con color al panel de registro. */
    private void anexarLineaEvento(String texto, Color color) {
        anexarConEstilo(texto + System.lineSeparator(), color);
    }

    /** AÃ±ade una lÃ­nea informativa del propio monitor (prefijo Â»). */
    private void anexarLineaSistema(String texto, Color color) {
        anexarConEstilo("\u00BB " + texto + System.lineSeparator(), color);
    }

    private static boolean esInicioSesionExitoso(String linea) {
        return linea.contains("INICIO_SESION exitoso") || linea.contains("LOGIN exitoso");
    }

    private static boolean esInicioSesionFallido(String linea) {
        return linea.contains("INICIO_SESION fallido") || linea.contains("LOGIN fallido");
    }

    /** Convierte etiquetas tÃ©cnicas del log a texto legible en espaÃ±ol. */
    private static String lineaLegible(String linea) {
        String s = linea;
        s = s.replace("INICIO_SESION exitoso", "Inicio de sesiÃ³n exitoso");
        s = s.replace("INICIO_SESION fallido", "Inicio de sesiÃ³n fallido");
        s = s.replace("INICIO_SESION:", "Inicio de sesiÃ³n:");
        s = s.replace("LOGIN exitoso", "Inicio de sesiÃ³n exitoso");
        s = s.replace("LOGIN fallido", "Inicio de sesiÃ³n fallido");
        s = s.replace("CIERRE_SESION (desconexion)", "Cierre de sesiÃ³n (desconexiÃ³n)");
        s = s.replace("CIERRE_SESION:", "Cierre de sesiÃ³n:");
        s = s.replace("LOGOUT (desconexion)", "Cierre de sesiÃ³n (desconexiÃ³n)");
        s = s.replace("LOGOUT (desconexiÃ³n)", "Cierre de sesiÃ³n (desconexiÃ³n)");
        s = s.replace("LOGOUT:", "Cierre de sesiÃ³n:");
        return s;
    }

    /** Inserta texto con atributos de color en el documento del panel de registro. */
    private void anexarConEstilo(String texto, Color color) {
        StyledDocument doc = panelRegistro.getStyledDocument();
        SimpleAttributeSet attrs = new SimpleAttributeSet();
        StyleConstants.setForeground(attrs, color);
        try {
            doc.insertString(doc.getLength(), texto, attrs);
            panelRegistro.setCaretPosition(doc.getLength());
        } catch (BadLocationException ignored) {
        }
    }

    /** Pone a cero todos los contadores de mÃ©tricas y limpia el feed lateral. */
    private void reiniciarContadores() {
        contadorConexiones = 0;
        contadorLoginExitoso = 0;
        contadorLoginFallido = 0;
        contadorRegistros = 0;
        contadorMensajesEnviados = 0;
        contadorMensajesRecibidos = 0;
        contadorAdvertencias = 0;
        contadorErrores = 0;
        modeloActividad.clear();
    }

    /** Borra la vista local sin modificar el archivo de eventos en disco. */
    private void limpiarVista() {
        todosLosEventos.clear();
        panelRegistro.setText("");
        reiniciarContadores();
        modeloEnLinea.clear();
        actualizarTarjetasEstadisticas();
        actualizarVistaRegistro();
        anexarLineaSistema("Vista limpiada.", TemaVisual.TEXTO_TENUE);
    }

    /** Exporta todos los eventos acumulados a un archivo de texto en la carpeta de logs. */
    private void exportarRegistro() {
        if (todosLosEventos.isEmpty()) {
            etiquetaEstado.setText("No hay eventos para exportar");
            return;
        }
        try {
            Path salida = directorioLogs.resolve("exportacion-monitor-" + System.currentTimeMillis() + ".txt");
            Files.write(salida, todosLosEventos, StandardCharsets.UTF_8, StandardOpenOption.CREATE);
            etiquetaEstado.setText("Exportado: " + salida.getFileName());
            Desktop.getDesktop().open(salida.toFile());
        } catch (IOException ex) {
            etiquetaEstado.setText("No se pudo exportar: " + ex.getMessage());
        }
    }

    /** Abre la carpeta de logs del servidor en el explorador de archivos. */
    private void abrirCarpetaLogs() {
        try {
            if (directorioLogs != null) {
                Files.createDirectories(directorioLogs);
                Desktop.getDesktop().open(directorioLogs.toFile());
            }
        } catch (IOException ex) {
            etiquetaEstado.setText("No se pudo abrir logs: " + ex.getMessage());
        }
    }
}

