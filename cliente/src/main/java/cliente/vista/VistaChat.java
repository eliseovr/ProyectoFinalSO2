package cliente.vista;

import cliente.controlador.ControladorChat;
import cliente.controlador.EscuchadorVistaChat;
import cliente.modelo.ModeloBandeja;
import cliente.modelo.SesionCliente;
import cliente.servicio.ServicioMensajeria;
import comun.RutasAplicacion;
import comun.TemaVisual;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.nio.file.Path;
import java.util.List;

/**
 * Vista Swing del chat (capa MVC).
 */
public class VistaChat extends JFrame implements EscuchadorVistaChat {
    private static final int MS_SINCRONIZACION_BANDEJA = 20_000;

    private final ControladorChat controlador;
    private final ModeloBandeja modelo;
    private final String nombreUsuario;

    private final DefaultListModel<String> modeloUsuarios = new DefaultListModel<>();
    private final JList<String> listaUsuarios = new JList<>(modeloUsuarios);
    private final JEditorPane panelChat = new JEditorPane();
    private final JTextField campoMensaje = new JTextField();
    private final JLabel etiquetaEstado = new JLabel("Conectado · cifrado E2E");
    private final JLabel etiquetaEncabezadoChat = new JLabel("Seleccione un contacto");
    private Timer temporizadorSincronizacion;

    public VistaChat(SesionCliente sesion) {
        super("Mensajería Segura - " + sesion.getNombreUsuario());
        this.controlador = new ControladorChat(new ServicioMensajeria(sesion));
        this.modelo = controlador.getModelo();
        this.nombreUsuario = sesion.getNombreUsuario();

        construirInterfaz();
        EscuchadorVistaChat escuchador = envoltorioEdt();
        controlador.configurarEscuchadorPush(escuchador, () -> listaUsuarios.getSelectedValue());
        iniciarSincronizacionFondo(escuchador);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setMinimumSize(new java.awt.Dimension(720, 520));
        setSize(900, 620);
        setLocationRelativeTo(null);
        actualizarTodo();
    }

    private void iniciarSincronizacionFondo(EscuchadorVistaChat escuchador) {
        temporizadorSincronizacion = new Timer(MS_SINCRONIZACION_BANDEJA, e ->
                new Thread(() -> controlador.cargarBandeja(escuchador, listaUsuarios.getSelectedValue()), "sincronizar-bandeja").start());
        temporizadorSincronizacion.start();
    }

    private void actualizarTodo() {
        EscuchadorVistaChat escuchador = envoltorioEdt();
        new Thread(() -> {
            controlador.cargarUsuarios(escuchador, listaUsuarios.getSelectedValue());
            controlador.cargarBandeja(escuchador, listaUsuarios.getSelectedValue());
        }, "hilo-actualizacion").start();
    }

    private void construirInterfaz() {
        panelChat.setEditable(false);
        panelChat.setContentType("text/html");
        panelChat.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
        panelChat.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        mostrarChatPlaceholder();

        listaUsuarios.setFixedCellHeight(48);
        listaUsuarios.setBackground(TemaVisual.TARJETA);
        listaUsuarios.setSelectionBackground(new Color(227, 242, 253));
        listaUsuarios.setSelectionForeground(TemaVisual.TEXTO);
        listaUsuarios.setCellRenderer(new RenderizadorListaUsuarios());

        JPanel panelIzquierdo = new JPanel(new BorderLayout(0, 0));
        panelIzquierdo.setBackground(TemaVisual.TARJETA);
        panelIzquierdo.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, TemaVisual.BORDE));
        panelIzquierdo.setPreferredSize(new java.awt.Dimension(220, 0));

        JLabel etiquetaTituloChats = new JLabel("  Chats");
        etiquetaTituloChats.setFont(etiquetaTituloChats.getFont().deriveFont(Font.BOLD, 15f));
        etiquetaTituloChats.setForeground(TemaVisual.TEXTO);
        etiquetaTituloChats.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, TemaVisual.BORDE),
                BorderFactory.createEmptyBorder(14, 8, 14, 8)));
        panelIzquierdo.add(etiquetaTituloChats, BorderLayout.NORTH);
        JScrollPane desplazamientoUsuarios = new JScrollPane(listaUsuarios);
        desplazamientoUsuarios.setBorder(BorderFactory.createEmptyBorder());
        panelIzquierdo.add(desplazamientoUsuarios, BorderLayout.CENTER);

        TemaVisual.aplicarEstiloCampoTexto(campoMensaje);
        campoMensaje.addActionListener(e -> enviarMensaje());

        JButton botonEnviar = new JButton("Enviar");
        TemaVisual.aplicarEstiloBotonPrimario(botonEnviar);
        botonEnviar.addActionListener(e -> enviarMensaje());

        JPanel panelInferior = new JPanel(new BorderLayout(10, 0));
        panelInferior.setBackground(TemaVisual.ENCABEZADO);
        panelInferior.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, TemaVisual.BORDE),
                BorderFactory.createEmptyBorder(12, 16, 12, 16)));
        panelInferior.add(campoMensaje, BorderLayout.CENTER);
        panelInferior.add(botonEnviar, BorderLayout.EAST);

        JButton botonCerrarSesion = new JButton("Cerrar sesión");
        TemaVisual.aplicarEstiloBotonFantasma(botonCerrarSesion);
        botonCerrarSesion.addActionListener(e -> cerrarSesion());

        JLabel avatar = new JLabel(String.valueOf(nombreUsuario.charAt(0)).toUpperCase());
        avatar.setOpaque(true);
        avatar.setBackground(TemaVisual.PRIMARIO);
        avatar.setForeground(Color.WHITE);
        avatar.setHorizontalAlignment(JLabel.CENTER);
        avatar.setPreferredSize(new java.awt.Dimension(40, 40));
        avatar.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        avatar.setFont(avatar.getFont().deriveFont(Font.BOLD, 16f));

        JPanel cajaTitulo = new JPanel();
        cajaTitulo.setLayout(new javax.swing.BoxLayout(cajaTitulo, javax.swing.BoxLayout.Y_AXIS));
        cajaTitulo.setOpaque(false);
        etiquetaEncabezadoChat.setFont(etiquetaEncabezadoChat.getFont().deriveFont(Font.BOLD, 15f));
        etiquetaEncabezadoChat.setForeground(TemaVisual.TEXTO);
        JLabel etiquetaConectadoComo = TemaVisual.etiquetaTenue("Conectado como " + nombreUsuario);
        cajaTitulo.add(etiquetaEncabezadoChat);
        cajaTitulo.add(etiquetaConectadoComo);

        JPanel barraSuperior = TemaVisual.barraEncabezado(avatar, cajaTitulo, botonCerrarSesion);

        JScrollPane desplazamientoChat = new JScrollPane(panelChat);
        desplazamientoChat.setBorder(BorderFactory.createEmptyBorder());

        JPanel panelContenedorChat = new JPanel(new BorderLayout());
        panelContenedorChat.setBackground(TemaVisual.FONDO_CHAT);
        panelContenedorChat.add(desplazamientoChat, BorderLayout.CENTER);

        JPanel panelCentro = new JPanel(new BorderLayout());
        panelCentro.setBackground(TemaVisual.FONDO_CHAT);
        panelCentro.add(barraSuperior, BorderLayout.NORTH);
        panelCentro.add(panelContenedorChat, BorderLayout.CENTER);
        panelCentro.add(panelInferior, BorderLayout.SOUTH);

        JPanel barraEstado = new JPanel(new BorderLayout());
        barraEstado.setBackground(TemaVisual.FONDO);
        barraEstado.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, TemaVisual.BORDE));
        etiquetaEstado.setForeground(TemaVisual.TEXTO_TENUE);
        etiquetaEstado.setBorder(BorderFactory.createEmptyBorder(6, 16, 6, 16));
        barraEstado.add(etiquetaEstado, BorderLayout.CENTER);

        JPanel panelContenido = new JPanel(new BorderLayout());
        TemaVisual.aplicarEstiloMarco(panelContenido);
        panelContenido.add(panelIzquierdo, BorderLayout.WEST);
        panelContenido.add(panelCentro, BorderLayout.CENTER);
        panelContenido.add(barraEstado, BorderLayout.SOUTH);
        setContentPane(panelContenido);

        listaUsuarios.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && listaUsuarios.getSelectedValue() != null) {
                controlador.seleccionarContacto(listaUsuarios.getSelectedValue(), this);
            }
        });
    }

    private void mostrarChatPlaceholder() {
        panelChat.setText("<html><body style='margin:24px;font-family:Segoe UI,sans-serif;color:#65676B;"
                + "background:#E5DDD5;text-align:center;'>Seleccione un chat para comenzar</body></html>");
    }

    private void enviarMensaje() {
        String destinatario = listaUsuarios.getSelectedValue();
        String contenido = campoMensaje.getText();
        new Thread(() -> controlador.enviarMensaje(destinatario, contenido, envoltorioEdt()), "hilo-envio").start();
    }

    private void cerrarSesion() {
        if (temporizadorSincronizacion != null) {
            temporizadorSincronizacion.stop();
        }
        new Thread(() -> controlador.cerrarSesion(envoltorioEdt()), "hilo-cierre-sesion").start();
    }

    private EscuchadorVistaChat envoltorioEdt() {
        return new EscuchadorVistaChat() {
            @Override
            public void actualizarUsuarios(List<String> usuarios, String contactoSeleccionado, String mensajeEstado) {
                SwingUtilities.invokeLater(() -> VistaChat.this.actualizarUsuarios(usuarios, contactoSeleccionado, mensajeEstado));
            }

            @Override
            public void actualizarConversacion(String contacto) {
                SwingUtilities.invokeLater(() -> VistaChat.this.actualizarConversacion(contacto));
            }

            @Override
            public void actualizarEstado(String texto) {
                SwingUtilities.invokeLater(() -> VistaChat.this.actualizarEstado(texto));
            }

            @Override
            public void mostrarError(String mensaje) {
                SwingUtilities.invokeLater(() -> VistaChat.this.mostrarError(mensaje));
            }

            @Override
            public void habilitarEnvio(boolean habilitado) {
                SwingUtilities.invokeLater(() -> VistaChat.this.habilitarEnvio(habilitado));
            }

            @Override
            public void limpiarCampoMensaje() {
                SwingUtilities.invokeLater(() -> VistaChat.this.limpiarCampoMensaje());
            }

            @Override
            public void refrescarListaContactos() {
                SwingUtilities.invokeLater(() -> VistaChat.this.refrescarListaContactos());
            }

            @Override
            public void onSesionCerrada() {
                SwingUtilities.invokeLater(() -> VistaChat.this.onSesionCerrada());
            }
        };
    }

    @Override
    public void actualizarUsuarios(List<String> usuarios, String contactoSeleccionado, String mensajeEstado) {
        modeloUsuarios.clear();
        usuarios.forEach(modeloUsuarios::addElement);
        if (contactoSeleccionado != null) {
            listaUsuarios.setSelectedValue(contactoSeleccionado, true);
        }
        if (mensajeEstado != null) {
            etiquetaEstado.setText(mensajeEstado);
        }
        listaUsuarios.repaint();
    }

    @Override
    public void actualizarConversacion(String contacto) {
        if (contacto == null) {
            etiquetaEncabezadoChat.setText("Seleccione un contacto");
            mostrarChatPlaceholder();
            return;
        }
        etiquetaEncabezadoChat.setText("Chat con " + contacto);
        String html = modelo.renderizarConversacionHtml(contacto, controlador.getServicio().getCifrado());
        panelChat.setText(html);
        panelChat.setCaretPosition(panelChat.getDocument().getLength());
    }

    @Override
    public void actualizarEstado(String texto) {
        etiquetaEstado.setText(texto);
    }

    @Override
    public void mostrarError(String mensaje) {
        JOptionPane.showMessageDialog(this, mensaje, "Error", JOptionPane.ERROR_MESSAGE);
    }

    @Override
    public void habilitarEnvio(boolean habilitado) {
        campoMensaje.setEnabled(habilitado);
    }

    @Override
    public void limpiarCampoMensaje() {
        campoMensaje.setText("");
    }

    @Override
    public void refrescarListaContactos() {
        listaUsuarios.repaint();
    }

    @Override
    public void onSesionCerrada() {
        dispose();
        Path keystore = RutasAplicacion.resolver("KEYSTORE_PATH", "certificados/server.p12");
        String host = System.getenv().getOrDefault("SERVER_HOST", "localhost");
        new VistaLogin(keystore, host, sesion -> new VistaChat(sesion).setVisible(true)).setVisible(true);
    }

    @Override
    public void dispose() {
        if (temporizadorSincronizacion != null) {
            temporizadorSincronizacion.stop();
        }
        super.dispose();
    }

    private final class RenderizadorListaUsuarios extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                      boolean isSelected, boolean cellHasFocus) {
            JComponent c = (JComponent) super.getListCellRendererComponent(
                    list, value, index, isSelected, cellHasFocus);
            c.setBorder(BorderFactory.createEmptyBorder(10, 16, 10, 12));
            if (!isSelected) {
                c.setBackground(TemaVisual.TARJETA);
            }
            if (value != null) {
                int unread = modelo.getNoLeidos(value.toString());
                String label = value.toString();
                if (unread > 0) {
                    setText("<html><b>" + label + "</b> <span style='color:#1877F2;'>(" + unread + ")</span></html>");
                    if (!isSelected) {
                        c.setFont(c.getFont().deriveFont(Font.BOLD));
                    }
                } else {
                    setText(label);
                }
            }
            return c;
        }
    }
}
