package servidor.vista;

import comun.TemaVisual;
import servidor.ServicioAdminSqlite;
import servidor.ServicioAdminSqlite.ResultadoTabla;
import servidor.ServicioAdminSqlite.MensajeRegistro;
import servidor.ServicioAdminSqlite.UsuarioRegistro;

import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.util.ArrayList;

import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.BoxLayout;
import javax.swing.JSplitPane;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.List;

/**
 * Administrador grÃ¡fico de SQLite: tablas, consultas SQL y ediciÃ³n de usuarios.
 */
public class MarcoAdminBaseDatos extends JFrame {
    private final ServicioAdminSqlite servicio;
    private final JLabel etiquetaRuta = new JLabel();
    private final JLabel etiquetaEstado = new JLabel(" ");

    private final JComboBox<String> comboTablas = new JComboBox<>();
    private final JTable tablaDatos = new JTable();
    private final DefaultTableModel modeloTabla = new DefaultTableModel();

    private final JTextArea areaSql = new JTextArea(6, 60);
    private final JTable tablaSql = new JTable();
    private final DefaultTableModel modeloSql = new DefaultTableModel();

    private final JList<UsuarioRegistro> listaUsuarios;
    private final JTextField campoId = new JTextField(8);
    private final JTextField campoUsuario = new JTextField(18);
    private final JTextField campoCreadoEn = new JTextField(20);
    private final JTextArea areaHash = new JTextArea(3, 40);
    private final JPasswordField campoProbarContrasena = new JPasswordField(18);
    private final JPasswordField campoNuevaContrasena = new JPasswordField(18);
    private final JLabel etiquetaContrasenaDescubierta = new JLabel(" ");
    private final JCheckBox checkMostrarPlano = new JCheckBox("Mostrar contraseÃ±as al escribir");
    private final JTextArea areaDiccionarioPersonal = new JTextArea(4, 28);

    private final JList<MensajeRegistro> listaMensajes;
    private final JTextField campoMsgId = new JTextField(8);
    private final JTextField campoRemitente = new JTextField(14);
    private final JTextField campoDestinatario = new JTextField(14);
    private final JTextArea areaContenidoMsg = new JTextArea(4, 30);
    private final JTextField campoEnviadoEn = new JTextField(20);

    /** @param servicio conexiÃ³n JDBC ya abierta a {@code mensajeria.db} */
    public MarcoAdminBaseDatos(ServicioAdminSqlite servicio) {
        super("Administrador de base de datos â€” MensajerÃ­a SO2");
        this.servicio = servicio;
        etiquetaRuta.setText("Archivo: " + servicio.getRutaBaseDatos());

        listaUsuarios = new JList<>();
        listaUsuarios.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        listaUsuarios.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                cargarUsuarioSeleccionado();
            }
        });

        listaMensajes = new JList<>();
        listaMensajes.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        listaMensajes.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                cargarMensajeSeleccionado();
            }
        });

        areaDiccionarioPersonal.setText("eliseo123\neliseo2\npassword\n123456");

        construirUi();
        aplicarEstilosGlobales();
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setMinimumSize(new Dimension(960, 640));
        setSize(1100, 720);
        setLocationRelativeTo(null);
        recargarTodo();
    }

    private void construirUi() {
        JPanel raiz = new JPanel(new BorderLayout(0, 0));
        TemaVisual.aplicarEstiloMarco(raiz);

        JLabel icono = new JLabel("\u25C6");
        icono.setForeground(TemaVisual.PRIMARIO);
        icono.setFont(icono.getFont().deriveFont(Font.BOLD, 36f));

        JPanel cajaTitulo = new JPanel();
        cajaTitulo.setLayout(new BoxLayout(cajaTitulo, BoxLayout.Y_AXIS));
        cajaTitulo.setOpaque(false);
        cajaTitulo.add(TemaVisual.etiquetaTitulo("Administrador de base de datos"));
        etiquetaRuta.setForeground(TemaVisual.TEXTO_TENUE);
        cajaTitulo.add(etiquetaRuta);

        JLabel insignia = new JLabel("  SQLite  ");
        insignia.setOpaque(true);
        insignia.setBackground(new Color(227, 242, 253));
        insignia.setForeground(TemaVisual.PRIMARIO);
        insignia.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));

        raiz.add(TemaVisual.barraEncabezado(combinarHorizontal(icono, cajaTitulo), null, insignia), BorderLayout.NORTH);

        JPanel cuerpo = new JPanel(new BorderLayout(0, 0));
        cuerpo.setOpaque(false);

        JPanel tarjetaInfo = TemaVisual.panelTarjeta();
        tarjetaInfo.setLayout(new BorderLayout(0, 6));
        tarjetaInfo.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(12, 16, 0, 16),
                TemaVisual.bordeTarjeta()));
        JLabel avisoSeguridad = TemaVisual.etiquetaTenue(
                "BCrypt: las contraseÃ±as no se descifran; puede verificar candidatos o buscarlas en un diccionario.");
        avisoSeguridad.setBorder(BorderFactory.createEmptyBorder(0, 0, 4, 0));
        tarjetaInfo.add(avisoSeguridad, BorderLayout.NORTH);
        cuerpo.add(tarjetaInfo, BorderLayout.NORTH);

        JTabbedPane pestanas = new JTabbedPane();
        TemaVisual.aplicarEstiloPestanas(pestanas);
        pestanas.addTab("  Tablas  ", panelTablas());
        pestanas.addTab("  SQL  ", panelConsulta());
        pestanas.addTab("  Usuarios  ", panelUsuarios());
        pestanas.addTab("  Mensajes  ", panelMensajes());
        cuerpo.add(pestanas, BorderLayout.CENTER);

        etiquetaEstado.setFont(etiquetaEstado.getFont().deriveFont(Font.PLAIN, 13f));
        etiquetaEstado.setForeground(TemaVisual.TEXTO_TENUE);

        JButton btnActualizarTodo = new JButton("Actualizar");
        TemaVisual.aplicarEstiloBotonFantasma(btnActualizarTodo);
        btnActualizarTodo.addActionListener(e -> recargarTodo());
        JButton btnCerrar = new JButton("Cerrar");
        TemaVisual.aplicarEstiloBotonSecundario(btnCerrar);
        btnCerrar.addActionListener(e -> dispose());
        JPanel accionesPie = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        accionesPie.setOpaque(false);
        accionesPie.add(btnActualizarTodo);
        accionesPie.add(btnCerrar);

        cuerpo.add(TemaVisual.barraEstado(etiquetaEstado, accionesPie), BorderLayout.SOUTH);
        raiz.add(cuerpo, BorderLayout.CENTER);
        setContentPane(raiz);
    }

    private static JPanel combinarHorizontal(JComponent... partes) {
        JPanel fila = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        fila.setOpaque(false);
        for (JComponent p : partes) {
            fila.add(p);
        }
        return fila;
    }

    private void aplicarEstilosGlobales() {
        TemaVisual.aplicarEstiloCampoTexto(campoId);
        TemaVisual.aplicarEstiloCampoTexto(campoUsuario);
        TemaVisual.aplicarEstiloCampoTexto(campoCreadoEn);
        TemaVisual.aplicarEstiloCampoTexto(campoMsgId);
        TemaVisual.aplicarEstiloCampoTexto(campoRemitente);
        TemaVisual.aplicarEstiloCampoTexto(campoDestinatario);
        TemaVisual.aplicarEstiloCampoTexto(campoEnviadoEn);
        TemaVisual.aplicarEstiloCampoContrasena(campoProbarContrasena);
        TemaVisual.aplicarEstiloCampoContrasena(campoNuevaContrasena);
        TemaVisual.aplicarEstiloAreaTexto(areaHash);
        TemaVisual.aplicarEstiloAreaTexto(areaSql);
        TemaVisual.aplicarEstiloAreaTexto(areaDiccionarioPersonal);
        TemaVisual.aplicarEstiloAreaTexto(areaContenidoMsg);
        areaHash.setFont(new Font("Monospaced", Font.PLAIN, 12));
        areaSql.setFont(new Font("Monospaced", Font.PLAIN, 13));
        areaDiccionarioPersonal.setFont(new Font("Monospaced", Font.PLAIN, 12));
        TemaVisual.aplicarEstiloCombo(comboTablas);
        TemaVisual.aplicarEstiloTabla(tablaDatos);
        TemaVisual.aplicarEstiloTabla(tablaSql);
        TemaVisual.aplicarEstiloLista(listaUsuarios);
        TemaVisual.aplicarEstiloLista(listaMensajes);
        TemaVisual.aplicarEstiloCasilla(checkMostrarPlano);
        comboTablas.setPreferredSize(new Dimension(180, 36));
    }

    private JPanel panelTablas() {
        JPanel panel = new JPanel(new BorderLayout(0, 12));
        panel.setOpaque(false);

        JPanel barra = barraHerramientas();
        barra.add(TemaVisual.etiquetaFormulario("Tabla"));
        barra.add(comboTablas);
        JButton btnCargar = new JButton("Cargar datos");
        TemaVisual.aplicarEstiloBotonPrimario(btnCargar);
        btnCargar.addActionListener(e -> cargarTablaSeleccionada());
        barra.add(btnCargar);
        panel.add(barra, BorderLayout.NORTH);

        tablaDatos.setModel(modeloTabla);
        tablaDatos.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        panel.add(TemaVisual.scrollConBorde(tablaDatos), BorderLayout.CENTER);
        return panel;
    }

    private JPanel panelConsulta() {
        JPanel panel = new JPanel(new BorderLayout(0, 0));
        panel.setOpaque(false);

        areaSql.setLineWrap(false);
        areaSql.setText("SELECT * FROM usuarios;\n");

        JPanel zonaSql = new JPanel(new BorderLayout(0, 10));
        zonaSql.setOpaque(false);
        zonaSql.setBorder(TemaVisual.tituloSeccion("Editor SQL"));
        zonaSql.add(TemaVisual.scrollConBorde(areaSql), BorderLayout.CENTER);

        JPanel barra = barraHerramientas();
        JButton btnEjecutar = new JButton("Ejecutar consulta");
        TemaVisual.aplicarEstiloBotonPrimario(btnEjecutar);
        btnEjecutar.addActionListener(e -> ejecutarConsultaSql());
        JButton btnLimpiar = new JButton("Limpiar");
        TemaVisual.aplicarEstiloBotonFantasma(btnLimpiar);
        btnLimpiar.addActionListener(e -> limpiarTabla(modeloSql, tablaSql));
        barra.add(btnEjecutar);
        barra.add(btnLimpiar);
        zonaSql.add(barra, BorderLayout.SOUTH);

        JPanel zonaResultados = new JPanel(new BorderLayout());
        zonaResultados.setOpaque(false);
        zonaResultados.setBorder(TemaVisual.tituloSeccion("Resultados"));
        tablaSql.setModel(modeloSql);
        zonaResultados.add(TemaVisual.scrollConBorde(tablaSql), BorderLayout.CENTER);

        JSplitPane division = new JSplitPane(JSplitPane.VERTICAL_SPLIT, zonaSql, zonaResultados);
        division.setResizeWeight(0.38);
        division.setDividerSize(8);
        division.setBorder(BorderFactory.createEmptyBorder());
        division.setBackground(TemaVisual.FONDO);
        panel.add(division, BorderLayout.CENTER);
        return panel;
    }

    private static JPanel barraHerramientas() {
        JPanel barra = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 6));
        barra.setOpaque(false);
        return barra;
    }

    private JPanel panelUsuarios() {
        JPanel panel = new JPanel(new BorderLayout(12, 0));
        panel.setOpaque(false);

        JPanel izquierda = new JPanel(new BorderLayout(0, 10));
        izquierda.setOpaque(false);
        izquierda.setBackground(TemaVisual.TARJETA);
        izquierda.setPreferredSize(new Dimension(248, 0));
        izquierda.setBorder(TemaVisual.tituloSeccion("Cuentas"));
        izquierda.add(TemaVisual.scrollConBorde(listaUsuarios), BorderLayout.CENTER);

        JPanel accionesLista = new JPanel(new GridLayout(1, 0, 6, 0));
        accionesLista.setOpaque(false);
        JButton btnNuevo = new JButton("Nuevo");
        TemaVisual.aplicarEstiloBotonSecundario(btnNuevo);
        btnNuevo.addActionListener(e -> nuevoUsuario());
        JButton btnEliminar = new JButton("Eliminar");
        TemaVisual.aplicarEstiloBotonSecundario(btnEliminar);
        btnEliminar.addActionListener(e -> eliminarUsuario());
        accionesLista.add(btnNuevo);
        accionesLista.add(btnEliminar);
        izquierda.add(accionesLista, BorderLayout.SOUTH);
        panel.add(izquierda, BorderLayout.WEST);

        JPanel formulario = new JPanel(new GridBagLayout());
        formulario.setOpaque(false);
        formulario.setBackground(TemaVisual.TARJETA);
        formulario.setBorder(TemaVisual.tituloSeccion("Editar usuario"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 6, 4, 6);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;

        int fila = 0;
        agregarFila(formulario, gbc, fila++, "Id", campoId);
        campoId.setEditable(false);
        agregarFila(formulario, gbc, fila++, "Usuario", campoUsuario);
        agregarFila(formulario, gbc, fila++, "Creado en", campoCreadoEn);
        campoCreadoEn.setEditable(false);

        gbc.gridy = fila++;
        gbc.gridx = 0;
        formulario.add(TemaVisual.etiquetaFormulario("Hash BCrypt"), gbc);
        gbc.gridx = 1;
        areaHash.setEditable(false);
        areaHash.setLineWrap(true);
        JPanel hashBarra = new JPanel(new BorderLayout(8, 0));
        hashBarra.setOpaque(false);
        hashBarra.add(TemaVisual.scrollConBorde(areaHash), BorderLayout.CENTER);
        JButton btnCopiarHash = new JButton("Copiar hash");
        TemaVisual.aplicarEstiloBotonFantasma(btnCopiarHash);
        btnCopiarHash.addActionListener(e -> copiarAlPortapapeles(areaHash.getText()));
        hashBarra.add(btnCopiarHash, BorderLayout.EAST);
        formulario.add(hashBarra, gbc);

        gbc.gridy = fila++;
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        JPanel panelContrasena = new JPanel(new GridBagLayout());
        panelContrasena.setOpaque(false);
        panelContrasena.setBackground(new Color(248, 249, 251));
        panelContrasena.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(TemaVisual.BORDE),
                BorderFactory.createTitledBorder(
                        BorderFactory.createEmptyBorder(),
                        "ContraseÃ±a",
                        javax.swing.border.TitledBorder.LEFT,
                        javax.swing.border.TitledBorder.TOP,
                        new Font("Segoe UI", Font.BOLD, 12),
                        TemaVisual.TEXTO_TENUE)));
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(4, 4, 4, 4);
        gc.anchor = GridBagConstraints.WEST;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.weightx = 1;

        gc.gridy = 0;
        gc.gridx = 0;
        panelContrasena.add(TemaVisual.etiquetaTenue("Probar candidata"), gc);
        gc.gridx = 1;
        panelContrasena.add(campoProbarContrasena, gc);
        gc.gridx = 2;
        JButton btnVerificar = new JButton("Verificar");
        TemaVisual.aplicarEstiloBotonSecundario(btnVerificar);
        btnVerificar.addActionListener(e -> verificarContrasenaManual());
        panelContrasena.add(btnVerificar, gc);

        gc.gridy = 1;
        gc.gridx = 0;
        gc.gridwidth = 3;
        JButton btnDiccionario = new JButton("Buscar en diccionario de demostraciÃ³n");
        TemaVisual.aplicarEstiloBotonSecundario(btnDiccionario);
        btnDiccionario.addActionListener(e -> buscarEnDiccionario());
        panelContrasena.add(btnDiccionario, gc);

        gc.gridy = 2;
        gc.gridx = 0;
        gc.gridwidth = 1;
        panelContrasena.add(TemaVisual.etiquetaTenue("Diccionario propio"), gc);
        gc.gridx = 1;
        gc.gridwidth = 2;
        areaDiccionarioPersonal.setToolTipText("Una contraseÃ±a candidata por lÃ­nea");
        panelContrasena.add(TemaVisual.scrollConBorde(areaDiccionarioPersonal), gc);

        gc.gridy = 3;
        gc.gridwidth = 3;
        JButton btnDiccionarioPropio = new JButton("Buscar en diccionario propio");
        TemaVisual.aplicarEstiloBotonSecundario(btnDiccionarioPropio);
        btnDiccionarioPropio.addActionListener(e -> buscarEnDiccionarioPropio());
        panelContrasena.add(btnDiccionarioPropio, gc);

        gc.gridy = 4;
        gc.gridwidth = 3;
        etiquetaContrasenaDescubierta.setForeground(TemaVisual.PRIMARIO);
        panelContrasena.add(etiquetaContrasenaDescubierta, gc);

        gc.gridy = 5;
        gc.gridx = 0;
        gc.gridwidth = 1;
        panelContrasena.add(TemaVisual.etiquetaTenue("Nueva contraseÃ±a"), gc);
        gc.gridx = 1;
        panelContrasena.add(campoNuevaContrasena, gc);
        gc.gridx = 2;
        JButton btnGuardarPass = new JButton("Guardar hash");
        TemaVisual.aplicarEstiloBotonPrimario(btnGuardarPass);
        btnGuardarPass.addActionListener(e -> guardarNuevaContrasena());
        panelContrasena.add(btnGuardarPass, gc);

        gc.gridy = 6;
        gc.gridwidth = 3;
        checkMostrarPlano.addActionListener(e -> aplicarVisibilidadContrasenas());
        panelContrasena.add(checkMostrarPlano, gc);

        formulario.add(panelContrasena, gbc);

        gbc.gridy = fila;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.NONE;
        JPanel guardar = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        guardar.setOpaque(false);
        JButton btnGuardarUsuario = new JButton("Guardar usuario");
        TemaVisual.aplicarEstiloBotonPrimario(btnGuardarUsuario);
        btnGuardarUsuario.addActionListener(e -> guardarUsuario());
        guardar.add(btnGuardarUsuario);
        formulario.add(guardar, gbc);

        JScrollPane scrollForm = new JScrollPane(formulario);
        scrollForm.setBorder(BorderFactory.createLineBorder(TemaVisual.BORDE));
        scrollForm.getViewport().setBackground(TemaVisual.TARJETA);
        panel.add(scrollForm, BorderLayout.CENTER);
        return panel;
    }

    private JPanel panelMensajes() {
        JPanel panel = new JPanel(new BorderLayout(12, 0));
        panel.setOpaque(false);

        JPanel izquierda = new JPanel(new BorderLayout(0, 10));
        izquierda.setOpaque(false);
        izquierda.setBackground(TemaVisual.TARJETA);
        izquierda.setPreferredSize(new Dimension(272, 0));
        izquierda.setBorder(TemaVisual.tituloSeccion("Historial"));
        izquierda.add(TemaVisual.scrollConBorde(listaMensajes), BorderLayout.CENTER);
        JButton btnActualizarMsg = new JButton("Actualizar");
        TemaVisual.aplicarEstiloBotonSecundario(btnActualizarMsg);
        btnActualizarMsg.addActionListener(e -> recargarListaMensajes());
        izquierda.add(btnActualizarMsg, BorderLayout.SOUTH);
        panel.add(izquierda, BorderLayout.WEST);

        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        form.setBackground(TemaVisual.TARJETA);
        form.setBorder(TemaVisual.tituloSeccion("Detalle del mensaje"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 6, 4, 6);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
        int fila = 0;
        agregarFila(form, gbc, fila++, "Id", campoMsgId);
        campoMsgId.setEditable(false);
        agregarFila(form, gbc, fila++, "Remitente", campoRemitente);
        agregarFila(form, gbc, fila++, "Destinatario", campoDestinatario);
        agregarFila(form, gbc, fila++, "Enviado en", campoEnviadoEn);
        campoEnviadoEn.setEditable(false);
        gbc.gridy = fila++;
        gbc.gridx = 0;
        form.add(TemaVisual.etiquetaFormulario("Contenido"), gbc);
        gbc.gridx = 1;
        areaContenidoMsg.setLineWrap(true);
        form.add(TemaVisual.scrollConBorde(areaContenidoMsg), gbc);
        gbc.gridy = fila;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.NONE;
        JPanel acciones = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        acciones.setOpaque(false);
        JButton btnGuardarMsg = new JButton("Guardar cambios");
        TemaVisual.aplicarEstiloBotonPrimario(btnGuardarMsg);
        btnGuardarMsg.addActionListener(e -> guardarMensaje());
        JButton btnEliminarMsg = new JButton("Eliminar");
        TemaVisual.aplicarEstiloBotonSecundario(btnEliminarMsg);
        btnEliminarMsg.addActionListener(e -> eliminarMensaje());
        acciones.add(btnGuardarMsg);
        acciones.add(btnEliminarMsg);
        form.add(acciones, gbc);
        JScrollPane scrollDetalle = new JScrollPane(form);
        scrollDetalle.setBorder(BorderFactory.createLineBorder(TemaVisual.BORDE));
        scrollDetalle.getViewport().setBackground(TemaVisual.TARJETA);
        panel.add(scrollDetalle, BorderLayout.CENTER);
        return panel;
    }

    private static void agregarFila(JPanel panel, GridBagConstraints gbc, int fila, String etiqueta, JTextField campo) {
        gbc.gridy = fila;
        gbc.gridx = 0;
        gbc.gridwidth = 1;
        gbc.weightx = 0;
        panel.add(TemaVisual.etiquetaFormulario(etiqueta), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        TemaVisual.aplicarEstiloCampoTexto(campo);
        panel.add(campo, gbc);
    }

    private void recargarTodo() {
        try {
            comboTablas.setModel(new DefaultComboBoxModel<>(
                    servicio.listarTablas().toArray(new String[0])));
            if (comboTablas.getItemCount() > 0) {
                comboTablas.setSelectedItem("usuarios");
                if (comboTablas.getSelectedItem() == null) {
                    comboTablas.setSelectedIndex(0);
                }
            }
            cargarTablaSeleccionada();
            recargarListaUsuarios();
            recargarListaMensajes();
            estado("Datos actualizados");
        } catch (SQLException ex) {
            error("No se pudo cargar la base de datos", ex);
        }
    }

    private void recargarListaUsuarios() throws SQLException {
        List<UsuarioRegistro> usuarios = servicio.listarUsuarios();
        listaUsuarios.setListData(usuarios.toArray(new UsuarioRegistro[0]));
        if (!usuarios.isEmpty()) {
            listaUsuarios.setSelectedIndex(0);
        } else {
            limpiarFormularioUsuario();
        }
    }

    private void cargarTablaSeleccionada() {
        Object item = comboTablas.getSelectedItem();
        if (item == null) {
            return;
        }
        try {
            ResultadoTabla r = servicio.consultarTabla(item.toString(), 500);
            mostrarEnTabla(modeloTabla, tablaDatos, r);
            estado("Tabla " + item + ": " + r.filas.size() + " filas");
        } catch (SQLException ex) {
            error("Error al leer tabla", ex);
        }
    }

    private void ejecutarConsultaSql() {
        String sql = areaSql.getText().trim();
        if (!sql.isEmpty() && !esSoloLectura(sql) && !confirmarEscritura(sql)) {
            return;
        }
        try {
            ResultadoTabla r = servicio.ejecutarSql(sql);
            if (r.mensaje != null) {
                limpiarTabla(modeloSql, tablaSql);
                estado(r.mensaje);
                JOptionPane.showMessageDialog(this, r.mensaje, "SQL", JOptionPane.INFORMATION_MESSAGE);
            } else {
                mostrarEnTabla(modeloSql, tablaSql, r);
                estado("Consulta: " + r.filas.size() + " filas");
            }
            try {
                recargarListaUsuarios();
                comboTablas.setModel(new DefaultComboBoxModel<>(servicio.listarTablas().toArray(new String[0])));
            } catch (SQLException ex2) {
                error("Error al refrescar listas", ex2);
            }
        } catch (SQLException ex) {
            error("Error SQL", ex);
        }
    }

    private boolean esSoloLectura(String sql) {
        String s = sql.trim().toLowerCase();
        return s.startsWith("select") || s.startsWith("pragma") || s.startsWith("explain");
    }

    private boolean confirmarEscritura(String sql) {
        int r = JOptionPane.showConfirmDialog(this,
                "Va a ejecutar un comando que puede modificar datos:\n\n" + sql,
                "Confirmar escritura",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.WARNING_MESSAGE);
        return r == JOptionPane.OK_OPTION;
    }

    private void cargarUsuarioSeleccionado() {
        UsuarioRegistro u = listaUsuarios.getSelectedValue();
        if (u == null) {
            limpiarFormularioUsuario();
            return;
        }
        campoId.setText(String.valueOf(u.id));
        campoUsuario.setText(u.usuario);
        campoCreadoEn.setText(u.creadoEn != null ? u.creadoEn : "");
        areaHash.setText(u.hashContrasena != null ? u.hashContrasena : "");
        etiquetaContrasenaDescubierta.setText(" ");
        campoProbarContrasena.setText("");
        campoNuevaContrasena.setText("");
    }

    private void limpiarFormularioUsuario() {
        campoId.setText("");
        campoUsuario.setText("");
        campoCreadoEn.setText("");
        areaHash.setText("");
        etiquetaContrasenaDescubierta.setText(" ");
    }

    private void verificarContrasenaManual() {
        String hash = areaHash.getText().trim();
        if (hash.isEmpty()) {
            return;
        }
        String candidata = new String(campoProbarContrasena.getPassword());
        if (candidata.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Escriba una contraseÃ±a para probar.", "Aviso",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        boolean ok = servicio.verificarContrasena(hash, candidata);
        if (ok) {
            etiquetaContrasenaDescubierta.setText("Coincide: Â«" + candidata + "Â»");
            etiquetaContrasenaDescubierta.setForeground(TemaVisual.EXITO);
        } else {
            etiquetaContrasenaDescubierta.setText("No coincide con el hash almacenado");
            etiquetaContrasenaDescubierta.setForeground(TemaVisual.PELIGRO);
        }
    }

    private void buscarEnDiccionario() {
        ejecutarBusquedaHash(() -> servicio.intentarRecuperarConDiccionario(areaHash.getText().trim()),
                "diccionario de demostraciÃ³n", "diccionario-demo");
    }

    private void buscarEnDiccionarioPropio() {
        List<String> candidatos = lineasDiccionarioPersonal();
        ejecutarBusquedaHash(
                () -> servicio.intentarRecuperar(areaHash.getText().trim(), candidatos),
                "diccionario propio", "diccionario-propio");
    }

    private void ejecutarBusquedaHash(java.util.concurrent.Callable<String> busqueda, String origen, String nombreHilo) {
        String hash = areaHash.getText().trim();
        if (hash.isEmpty()) {
            return;
        }
        setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.WAIT_CURSOR));
        new Thread(() -> {
            String encontrada = null;
            try {
                encontrada = busqueda.call();
            } catch (Exception ignored) {
            }
            String resultado = encontrada;
            SwingUtilities.invokeLater(() -> {
                setCursor(java.awt.Cursor.getDefaultCursor());
                mostrarContrasenaEncontrada(resultado, origen);
            });
        }, nombreHilo).start();
    }

    private void mostrarContrasenaEncontrada(String encontrada, String origen) {
        if (encontrada != null) {
            etiquetaContrasenaDescubierta.setText("ContraseÃ±a en " + origen + ": Â«" + encontrada + "Â»");
            etiquetaContrasenaDescubierta.setForeground(TemaVisual.EXITO);
            campoProbarContrasena.setText(encontrada);
            aplicarVisibilidadContrasenas();
        } else {
            etiquetaContrasenaDescubierta.setText(
                    "No encontrada en " + origen + " (BCrypt no se puede descifrar)");
            etiquetaContrasenaDescubierta.setForeground(TemaVisual.TEXTO_TENUE);
        }
    }

    private List<String> lineasDiccionarioPersonal() {
        List<String> lineas = new ArrayList<>();
        for (String linea : areaDiccionarioPersonal.getText().split("\\R")) {
            String t = linea.trim();
            if (!t.isEmpty() && !t.startsWith("#")) {
                lineas.add(t);
            }
        }
        return lineas;
    }

    private void aplicarVisibilidadContrasenas() {
        char eco = checkMostrarPlano.isSelected() ? (char) 0 : '\u2022';
        campoProbarContrasena.setEchoChar(eco);
        campoNuevaContrasena.setEchoChar(eco);
    }

    private static void copiarAlPortapapeles(String texto) {
        if (texto == null || texto.isEmpty()) {
            return;
        }
        Toolkit.getDefaultToolkit().getSystemClipboard()
                .setContents(new StringSelection(texto), null);
    }

    private void recargarListaMensajes() {
        try {
            List<MensajeRegistro> mensajes = servicio.listarMensajes(300);
            listaMensajes.setListData(mensajes.toArray(new MensajeRegistro[0]));
            if (!mensajes.isEmpty()) {
                listaMensajes.setSelectedIndex(0);
            } else {
                limpiarFormularioMensaje();
            }
            estado("Mensajes: " + mensajes.size());
        } catch (SQLException ex) {
            error("Error al cargar mensajes", ex);
        }
    }

    private void cargarMensajeSeleccionado() {
        MensajeRegistro m = listaMensajes.getSelectedValue();
        if (m == null) {
            limpiarFormularioMensaje();
            return;
        }
        campoMsgId.setText(String.valueOf(m.id));
        campoRemitente.setText(m.remitente);
        campoDestinatario.setText(m.destinatario);
        areaContenidoMsg.setText(m.contenido);
        campoEnviadoEn.setText(m.enviadoEn != null ? m.enviadoEn : "");
    }

    private void limpiarFormularioMensaje() {
        campoMsgId.setText("");
        campoRemitente.setText("");
        campoDestinatario.setText("");
        areaContenidoMsg.setText("");
        campoEnviadoEn.setText("");
    }

    private void guardarMensaje() {
        String idTxt = campoMsgId.getText().trim();
        if (idTxt.isEmpty()) {
            return;
        }
        try {
            long id = Long.parseLong(idTxt);
            servicio.actualizarMensaje(id,
                    campoRemitente.getText(),
                    campoDestinatario.getText(),
                    areaContenidoMsg.getText());
            recargarListaMensajes();
            seleccionarMensajePorId(id);
            estado("Mensaje guardado");
        } catch (SQLException ex) {
            error("No se pudo guardar el mensaje", ex);
        } catch (NumberFormatException ex) {
            error("Id de mensaje no vÃ¡lido", ex);
        }
    }

    private void eliminarMensaje() {
        MensajeRegistro m = listaMensajes.getSelectedValue();
        if (m == null) {
            return;
        }
        int r = JOptionPane.showConfirmDialog(this,
                "Â¿Eliminar el mensaje id " + m.id + "?",
                "Confirmar",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.WARNING_MESSAGE);
        if (r != JOptionPane.OK_OPTION) {
            return;
        }
        try {
            servicio.eliminarMensaje(m.id);
            recargarListaMensajes();
            cargarTablaSeleccionada();
            estado("Mensaje eliminado");
        } catch (SQLException ex) {
            error("No se pudo eliminar", ex);
        }
    }

    private void seleccionarMensajePorId(long id) {
        for (int i = 0; i < listaMensajes.getModel().getSize(); i++) {
            if (listaMensajes.getModel().getElementAt(i).id == id) {
                listaMensajes.setSelectedIndex(i);
                return;
            }
        }
    }

    private void guardarNuevaContrasena() {
        UsuarioRegistro u = listaUsuarios.getSelectedValue();
        if (u == null) {
            return;
        }
        String nueva = new String(campoNuevaContrasena.getPassword());
        if (nueva.length() < 4) {
            JOptionPane.showMessageDialog(this, "Use al menos 4 caracteres.", "Aviso",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        try {
            servicio.cambiarContrasena(u.id, nueva);
            recargarListaUsuarios();
            seleccionarPorId(u.id);
            estado("ContraseÃ±a actualizada (nuevo hash BCrypt)");
        } catch (SQLException ex) {
            error("No se pudo guardar la contraseÃ±a", ex);
        }
    }

    private void guardarUsuario() {
        String idTexto = campoId.getText().trim();
        String usuario = campoUsuario.getText().trim();
        if (usuario.isEmpty()) {
            JOptionPane.showMessageDialog(this, "El nombre de usuario es obligatorio.", "Aviso",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        try {
            if (idTexto.isEmpty()) {
                String pass = new String(campoNuevaContrasena.getPassword());
                if (pass.length() < 4) {
                    JOptionPane.showMessageDialog(this,
                            "Para un usuario nuevo indique la contraseÃ±a (mÃ­n. 4 caracteres).",
                            "Aviso", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                long id = servicio.insertarUsuario(usuario, pass);
                recargarListaUsuarios();
                seleccionarPorId(id);
                estado("Usuario creado");
            } else {
                long id = Long.parseLong(idTexto);
                String hash = areaHash.getText().trim();
                servicio.actualizarUsuario(id, usuario, hash);
                recargarListaUsuarios();
                seleccionarPorId(id);
                estado("Usuario guardado");
            }
        } catch (SQLException ex) {
            error("No se pudo guardar", ex);
        } catch (NumberFormatException ex) {
            error("Id no vÃ¡lido", ex);
        }
    }

    private void nuevoUsuario() {
        listaUsuarios.clearSelection();
        limpiarFormularioUsuario();
        campoUsuario.requestFocus();
        estado("Nuevo usuario â€” complete datos y contraseÃ±a");
    }

    private void eliminarUsuario() {
        UsuarioRegistro u = listaUsuarios.getSelectedValue();
        if (u == null) {
            return;
        }
        int r = JOptionPane.showConfirmDialog(this,
                "Â¿Eliminar al usuario Â«" + u.usuario + "Â» (id " + u.id + ")?",
                "Confirmar",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.WARNING_MESSAGE);
        if (r != JOptionPane.OK_OPTION) {
            return;
        }
        try {
            servicio.eliminarUsuario(u.id);
            recargarListaUsuarios();
            cargarTablaSeleccionada();
            estado("Usuario eliminado");
        } catch (SQLException ex) {
            error("No se pudo eliminar", ex);
        }
    }

    private void seleccionarPorId(long id) {
        for (int i = 0; i < listaUsuarios.getModel().getSize(); i++) {
            UsuarioRegistro u = listaUsuarios.getModel().getElementAt(i);
            if (u.id == id) {
                listaUsuarios.setSelectedIndex(i);
                return;
            }
        }
    }

    private static void mostrarEnTabla(DefaultTableModel modelo, JTable tabla, ResultadoTabla r) {
        modelo.setRowCount(0);
        modelo.setColumnCount(0);
        for (String col : r.columnas) {
            modelo.addColumn(col);
        }
        for (List<Object> fila : r.filas) {
            modelo.addRow(fila.toArray());
        }
        if (!r.columnas.isEmpty()) {
            for (int c = 0; c < r.columnas.size(); c++) {
                tabla.getColumnModel().getColumn(c).setPreferredWidth(120);
            }
        }
    }

    private static void limpiarTabla(DefaultTableModel modelo, JTable tabla) {
        modelo.setRowCount(0);
        modelo.setColumnCount(0);
    }

    /** Muestra un mensaje breve en la barra de estado inferior. */
    private void estado(String mensaje) {
        etiquetaEstado.setText(mensaje);
    }

    /** Informa error en barra de estado y cuadro de diÃ¡logo modal. */
    private void error(String titulo, Exception ex) {
        etiquetaEstado.setText(titulo + ": " + ex.getMessage());
        JOptionPane.showMessageDialog(this, ex.getMessage(), titulo, JOptionPane.ERROR_MESSAGE);
    }

    @Override
    public void dispose() {
        servicio.close();
        super.dispose();
    }
}

