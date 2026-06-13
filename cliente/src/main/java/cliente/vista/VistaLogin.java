package cliente.vista;

import cliente.controlador.ControladorLogin;
import cliente.controlador.EscuchadorVistaLogin;
import cliente.modelo.ResultadoAutenticacion;
import cliente.modelo.SesionCliente;
import cliente.servicio.ServicioAutenticacion;
import comun.MensajesError;
import comun.Protocolo;
import comun.TemaVisual;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import javax.swing.Timer;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * Vista Swing de registro e inicio de sesión (capa MVC).
 */
public class VistaLogin extends JFrame implements EscuchadorVistaLogin {
    private static final int MS_TEMPORIZADOR_ESPERA = 12_000;

    private final ControladorLogin controlador;
    private final Consumer<SesionCliente> alExitoLogin;

    private final JTextField campoHost = new JTextField(18);
    private final JTextField campoPuerto = new JTextField(String.valueOf(Protocolo.PUERTO_PREDETERMINADO), 6);
    private final JTextField campoUsuario = new JTextField(16);
    private final JPasswordField campoContrasena = new JPasswordField(16);
    private JButton botonIniciarSesion;
    private JButton botonRegistro;
    private JCheckBox casillaMostrarContrasena;
    private char caracterEcoContrasena;
    private final AtomicInteger idOperacion = new AtomicInteger();
    private Timer temporizadorEspera;
    private final JLabel etiquetaEstado = new JLabel(" ", JLabel.CENTER);

    public VistaLogin(Path rutaKeystoreServidor, String hostPredeterminado, Consumer<SesionCliente> alExitoLogin) {
        super("Mensajería Segura SO2");
        this.controlador = new ControladorLogin(new ServicioAutenticacion(rutaKeystoreServidor));
        this.alExitoLogin = alExitoLogin;
        campoHost.setText(hostPredeterminado != null && !hostPredeterminado.isBlank() ? hostPredeterminado : "localhost");
        construirInterfaz();
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(440, 620));
        setSize(480, 680);
        setLocationRelativeTo(null);
    }

    private void construirInterfaz() {
        TemaVisual.aplicarEstiloCampoTexto(campoHost);
        TemaVisual.aplicarEstiloCampoTexto(campoPuerto);
        TemaVisual.aplicarEstiloCampoTexto(campoUsuario);
        TemaVisual.aplicarEstiloCampoTexto(campoContrasena);

        JPanel root = new JPanel(new BorderLayout());
        TemaVisual.aplicarEstiloMarco(root);
        root.setBorder(BorderFactory.createEmptyBorder(32, 24, 32, 24));

        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(TemaVisual.TARJETA);
        card.setBorder(TemaVisual.bordeTarjeta());
        card.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.setMaximumSize(new Dimension(420, 2000));

        JLabel logo = new JLabel("\u25CF");
        logo.setForeground(TemaVisual.PRIMARIO);
        logo.setFont(logo.getFont().deriveFont(java.awt.Font.BOLD, 42f));
        logo.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel title = TemaVisual.etiquetaTitulo("Mensajería Segura");
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel subtitle = TemaVisual.etiquetaTenue("Mensajería cifrada de extremo a extremo (E2E)");
        subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        card.add(logo);
        card.add(Box.createVerticalStrut(8));
        card.add(title);
        card.add(Box.createVerticalStrut(4));
        card.add(subtitle);
        card.add(Box.createVerticalStrut(20));

        JPanel serverBox = new JPanel(new GridBagLayout());
        serverBox.setOpaque(false);
        serverBox.setBorder(TemaVisual.tituloSeccion("Servidor"));
        addFormRow(serverBox, 0, "Servidor", campoHost, "localhost o IP");
        addFormRow(serverBox, 1, "Puerto", campoPuerto, String.valueOf(Protocolo.PUERTO_PREDETERMINADO));
        serverBox.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(serverBox);
        card.add(Box.createVerticalStrut(12));

        JPanel accountBox = new JPanel(new GridBagLayout());
        accountBox.setOpaque(false);
        accountBox.setBorder(TemaVisual.tituloSeccion("Cuenta"));
        addFormRow(accountBox, 0, "Usuario", campoUsuario, "ejemplo: Usuario");
        addFormRow(accountBox, 1, "Contrase\u00F1a", createPasswordFieldPanel(), "m\u00EDn. 6 caracteres");
        accountBox.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(accountBox);
        card.add(Box.createVerticalStrut(16));

        botonIniciarSesion = new JButton("Iniciar sesión");
        TemaVisual.aplicarEstiloBotonPrimario(botonIniciarSesion);
        botonIniciarSesion.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        botonIniciarSesion.setAlignmentX(Component.CENTER_ALIGNMENT);
        botonIniciarSesion.addActionListener(e -> solicitarInicioSesion());

        botonRegistro = new JButton("Crear cuenta nueva");
        TemaVisual.aplicarEstiloBotonSecundario(botonRegistro);
        botonRegistro.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        botonRegistro.setAlignmentX(Component.CENTER_ALIGNMENT);
        botonRegistro.addActionListener(e -> solicitarRegistro());

        card.add(botonIniciarSesion);
        card.add(Box.createVerticalStrut(8));
        card.add(botonRegistro);
        card.add(Box.createVerticalStrut(12));

        etiquetaEstado.setAlignmentX(Component.CENTER_ALIGNMENT);
        etiquetaEstado.setForeground(TemaVisual.TEXTO_TENUE);
        card.add(etiquetaEstado);

        JPanel centerWrap = new JPanel(new FlowLayout(FlowLayout.CENTER));
        centerWrap.setOpaque(false);
        centerWrap.add(card);
        root.add(centerWrap, BorderLayout.CENTER);

        setContentPane(root);
        getRootPane().setDefaultButton(botonIniciarSesion);
        campoContrasena.addActionListener(e -> solicitarInicioSesion());
    }

    private static void addFormRow(JPanel panel, int row, String label, Component field, String hint) {
        int baseRow = row * 2;
        GridBagConstraints labelGc = new GridBagConstraints();
        labelGc.gridx = 0;
        labelGc.gridy = baseRow;
        labelGc.anchor = GridBagConstraints.WEST;
        labelGc.insets = new Insets(row == 0 ? 0 : 10, 0, 4, 8);
        JLabel lbl = new JLabel(label);
        lbl.setForeground(TemaVisual.TEXTO);
        lbl.setFont(lbl.getFont().deriveFont(java.awt.Font.BOLD, 12f));
        panel.add(lbl, labelGc);

        GridBagConstraints fieldGc = new GridBagConstraints();
        fieldGc.gridx = 1;
        fieldGc.gridy = baseRow;
        fieldGc.weightx = 1.0;
        fieldGc.fill = GridBagConstraints.HORIZONTAL;
        fieldGc.insets = new Insets(row == 0 ? 0 : 10, 0, 0, 0);
        panel.add(field, fieldGc);

        if (hint != null && !hint.isEmpty()) {
            GridBagConstraints hintGc = new GridBagConstraints();
            hintGc.gridx = 1;
            hintGc.gridy = baseRow + 1;
            hintGc.weightx = 1.0;
            hintGc.fill = GridBagConstraints.HORIZONTAL;
            panel.add(TemaVisual.etiquetaTenue(hint), hintGc);
        }
    }

    private JPanel createPasswordFieldPanel() {
        caracterEcoContrasena = campoContrasena.getEchoChar();
        if (caracterEcoContrasena == 0) {
            caracterEcoContrasena = '\u2022';
        }
        JPanel wrap = new JPanel(new BorderLayout(8, 0));
        wrap.setOpaque(false);
        wrap.add(campoContrasena, BorderLayout.CENTER);
        casillaMostrarContrasena = new JCheckBox("Ver");
        casillaMostrarContrasena.setForeground(TemaVisual.TEXTO_TENUE);
        casillaMostrarContrasena.setOpaque(false);
        casillaMostrarContrasena.addActionListener(e ->
                campoContrasena.setEchoChar(casillaMostrarContrasena.isSelected() ? (char) 0 : caracterEcoContrasena));
        wrap.add(casillaMostrarContrasena, BorderLayout.EAST);
        return wrap;
    }

    private void solicitarInicioSesion() {
        String usuario = campoUsuario.getText().trim();
        String contrasena = new String(campoContrasena.getPassword());
        ResultadoAutenticacion validacion = controlador.validar(usuario, contrasena);
        if (validacion.getError() != null) {
            establecerOcupado(false, validacion.getError());
            mostrarAdvertencia("Datos incompletos", validacion.getError());
            campoUsuario.requestFocusInWindow();
            return;
        }
        int op = idOperacion.incrementAndGet();
        establecerOcupado(true, "Iniciando sesión...");
        iniciarTemporizador(op);

        int puerto = Integer.parseInt(campoPuerto.getText().trim());
        String host = campoHost.getText().trim();

        new SwingWorker<ResultadoAutenticacion, Void>() {
            @Override
            protected ResultadoAutenticacion doInBackground() {
                return controlador.iniciarSesion(host, puerto, usuario, contrasena);
            }

            @Override
            protected void done() {
                finalizarOperacion(op);
                try {
                    controlador.manejarResultadoLogin(get(), VistaLogin.this);
                } catch (Exception ex) {
                    controlador.manejarResultadoLogin(ResultadoAutenticacion.error(controlador.mensajeErrorInesperado(ex)), VistaLogin.this);
                }
            }
        }.execute();
    }

    private void solicitarRegistro() {
        String usuario = campoUsuario.getText().trim();
        String contrasena = new String(campoContrasena.getPassword());
        ResultadoAutenticacion validacion = controlador.validar(usuario, contrasena);
        if (validacion.getError() != null) {
            establecerOcupado(false, validacion.getError());
            mostrarAdvertencia("Datos incompletos", validacion.getError());
            return;
        }
        int op = idOperacion.incrementAndGet();
        establecerOcupado(true, "Registrando cuenta...");
        iniciarTemporizador(op);

        int puerto = Integer.parseInt(campoPuerto.getText().trim());
        String host = campoHost.getText().trim();

        new SwingWorker<ResultadoAutenticacion, Void>() {
            @Override
            protected ResultadoAutenticacion doInBackground() {
                return controlador.registrar(host, puerto, usuario, contrasena);
            }

            @Override
            protected void done() {
                finalizarOperacion(op);
                try {
                    controlador.manejarResultadoRegistro(get(), VistaLogin.this);
                } catch (Exception ex) {
                    establecerOcupado(false, MensajesError.aEspanol(ex));
                    mostrarError("Error", MensajesError.aEspanol(ex));
                }
            }
        }.execute();
    }

    private void iniciarTemporizador(int op) {
        detenerTemporizador();
        temporizadorEspera = new Timer(MS_TEMPORIZADOR_ESPERA, e -> {
            if (idOperacion.get() == op && !botonIniciarSesion.isEnabled()) {
                establecerOcupado(false, "Tiempo de espera. Intente de nuevo.");
                mostrarAdvertencia("Tiempo de espera",
                        "La operación tardó demasiado. Verifique que el servidor esté activo.");
            }
            detenerTemporizador();
        });
        temporizadorEspera.setRepeats(false);
        temporizadorEspera.start();
    }

    private void finalizarOperacion(int op) {
        detenerTemporizador();
        if (idOperacion.get() == op) {
            idOperacion.incrementAndGet();
        }
    }

    private void detenerTemporizador() {
        if (temporizadorEspera != null) {
            temporizadorEspera.stop();
            temporizadorEspera = null;
        }
    }

    @Override
    public void establecerOcupado(boolean ocupado, String textoEstado) {
        botonIniciarSesion.setEnabled(!ocupado);
        botonRegistro.setEnabled(!ocupado);
        campoHost.setEnabled(!ocupado);
        campoPuerto.setEnabled(!ocupado);
        campoUsuario.setEnabled(!ocupado);
        campoContrasena.setEnabled(!ocupado);
        if (casillaMostrarContrasena != null) {
            casillaMostrarContrasena.setEnabled(!ocupado);
        }
        setCursor(ocupado ? Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR) : Cursor.getDefaultCursor());
        etiquetaEstado.setText(textoEstado != null ? textoEstado : " ");
    }

    @Override
    public void mostrarAdvertencia(String titulo, String mensaje) {
        JOptionPane.showMessageDialog(this, mensaje, titulo, JOptionPane.WARNING_MESSAGE);
    }

    @Override
    public void mostrarError(String titulo, String mensaje) {
        JOptionPane.showMessageDialog(this, mensaje, titulo, JOptionPane.ERROR_MESSAGE);
    }

    @Override
    public void mostrarInfo(String titulo, String mensaje) {
        JOptionPane.showMessageDialog(this, mensaje, titulo, JOptionPane.INFORMATION_MESSAGE);
    }

    @Override
    public void onLoginExitoso(SesionCliente sesion) {
        alExitoLogin.accept(sesion);
        dispose();
    }
}
