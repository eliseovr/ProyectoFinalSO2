package comun;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;

/**
 * Paleta de colores y utilidades de estilo para la interfaz Swing del proyecto.
 * Inspirado en un diseño minimalista tipo Facebook / Messenger.
 */
public final class TemaVisual {
    /** Color de fondo general de ventanas y paneles principales. */
    public static final Color FONDO = new Color(240, 242, 245);
    /** Fondo de tarjetas y paneles elevados. */
    public static final Color TARJETA = Color.WHITE;
    /** Color primario de acciones y acentos. */
    public static final Color PRIMARIO = new Color(24, 119, 242);
    /** Variante del primario para estados hover. */
    public static final Color PRIMARIO_HOVER = new Color(22, 111, 229);
    /** Fondo secundario para barras y encabezados de tabla. */
    public static final Color FONDO_SECUNDARIO = new Color(228, 230, 235);
    /** Color de texto principal. */
    public static final Color TEXTO = new Color(5, 5, 5);
    /** Color de texto secundario o atenuado. */
    public static final Color TEXTO_TENUE = new Color(101, 103, 107);
    /** Color de bordes y separadores. */
    public static final Color BORDE = new Color(206, 208, 212);
    /** Fondo del área de conversación en el chat. */
    public static final Color FONDO_CHAT = new Color(229, 221, 213);
    /** Fondo de burbujas de mensajes enviados por el usuario local. */
    public static final Color BURBUJA_SALIENTE = new Color(217, 253, 211);
    /** Fondo de burbujas de mensajes recibidos. */
    public static final Color BURBUJA_ENTRANTE = Color.WHITE;
    /** Color para estados o indicadores de éxito. */
    public static final Color EXITO = new Color(54, 164, 32);
    /** Color para advertencias. */
    public static final Color ADVERTENCIA = new Color(247, 185, 40);
    /** Color para errores o acciones destructivas. */
    public static final Color PELIGRO = new Color(250, 70, 22);
    /** Fondo de barras de encabezado y pie. */
    public static final Color ENCABEZADO = Color.WHITE;

    private TemaVisual() {
    }

    /**
     * Aplica el color de fondo estándar a la raíz de un marco o panel principal.
     *
     * @param raiz componente raíz del marco
     */
    public static void aplicarEstiloMarco(JComponent raiz) {
        raiz.setBackground(FONDO);
    }

    /**
     * Devuelve el borde estándar de una tarjeta con relleno interior.
     *
     * @return borde compuesto para paneles tipo tarjeta
     */
    public static Border bordeTarjeta() {
        return BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDE),
                BorderFactory.createEmptyBorder(16, 18, 16, 18));
    }

    /**
     * Devuelve el borde estándar de campos de texto y áreas editables.
     *
     * @return borde compuesto para entradas de formulario
     */
    public static Border bordeCampo() {
        return BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDE),
                BorderFactory.createEmptyBorder(10, 12, 10, 12));
    }

    /**
     * Aplica estilo visual a un campo de texto de una sola línea.
     *
     * @param campo campo de texto a estilizar
     */
    public static void aplicarEstiloCampoTexto(JTextField campo) {
        campo.setBorder(bordeCampo());
        campo.setBackground(TARJETA);
        campo.setForeground(TEXTO);
        campo.setFont(campo.getFont().deriveFont(Font.PLAIN, 14f));
        campo.setCaretColor(PRIMARIO);
    }

    /**
     * Aplica estilo visual a un campo de contraseña.
     *
     * @param campo campo de contraseña a estilizar
     */
    public static void aplicarEstiloCampoContrasena(JPasswordField campo) {
        aplicarEstiloCampoTexto(campo);
    }

    /**
     * Aplica estilo visual a un área de texto multilínea.
     *
     * @param area área de texto a estilizar
     */
    public static void aplicarEstiloAreaTexto(JTextArea area) {
        area.setBackground(TARJETA);
        area.setForeground(TEXTO);
        area.setCaretColor(PRIMARIO);
        area.setBorder(bordeCampo());
        area.setFont(area.getFont().deriveFont(Font.PLAIN, 13f));
    }

    /**
     * Aplica estilo visual a un cuadro combinado (combo box).
     *
     * @param combo lista desplegable a estilizar
     */
    public static void aplicarEstiloCombo(JComboBox<?> combo) {
        combo.setBackground(TARJETA);
        combo.setForeground(TEXTO);
        combo.setFont(combo.getFont().deriveFont(Font.PLAIN, 13f));
    }

    /**
     * Aplica estilo visual a una casilla de verificación.
     *
     * @param casilla casilla a estilizar
     */
    public static void aplicarEstiloCasilla(JCheckBox casilla) {
        casilla.setForeground(TEXTO);
        casilla.setOpaque(false);
        casilla.setFont(casilla.getFont().deriveFont(Font.PLAIN, 13f));
    }

    /**
     * Aplica estilo visual a un panel de pestañas.
     *
     * @param pestanas panel con pestañas a estilizar
     */
    public static void aplicarEstiloPestanas(JTabbedPane pestanas) {
        pestanas.setBackground(FONDO);
        pestanas.setForeground(TEXTO);
        pestanas.setFont(pestanas.getFont().deriveFont(Font.BOLD, 13f));
        pestanas.setBorder(BorderFactory.createEmptyBorder(8, 16, 12, 16));
    }

    /**
     * Aplica estilo visual a una tabla de datos.
     *
     * @param tabla tabla a estilizar
     */
    public static void aplicarEstiloTabla(JTable tabla) {
        tabla.setBackground(TARJETA);
        tabla.setForeground(TEXTO);
        tabla.setGridColor(BORDE);
        tabla.setRowHeight(32);
        tabla.setShowVerticalLines(false);
        tabla.setIntercellSpacing(new Dimension(0, 0));
        tabla.setSelectionBackground(new Color(227, 242, 253));
        tabla.setSelectionForeground(TEXTO);
        tabla.setFont(tabla.getFont().deriveFont(Font.PLAIN, 13f));
        if (tabla.getTableHeader() != null) {
            tabla.getTableHeader().setBackground(FONDO_SECUNDARIO);
            tabla.getTableHeader().setForeground(TEXTO);
            tabla.getTableHeader().setFont(tabla.getTableHeader().getFont().deriveFont(Font.BOLD, 12f));
            tabla.getTableHeader().setReorderingAllowed(false);
            tabla.getTableHeader().setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, BORDE));
        }
    }

    /**
     * Aplica estilo visual a una lista de elementos.
     *
     * @param lista lista a estilizar
     */
    public static void aplicarEstiloLista(JList<?> lista) {
        lista.setBackground(TARJETA);
        lista.setForeground(TEXTO);
        lista.setSelectionBackground(new Color(227, 242, 253));
        lista.setSelectionForeground(TEXTO);
        lista.setFixedCellHeight(44);
        lista.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
        lista.setFont(lista.getFont().deriveFont(Font.PLAIN, 13f));
    }

    /**
     * Crea un panel de desplazamiento con borde estándar alrededor de la vista.
     *
     * @param vista componente a envolver
     * @return panel con barras de desplazamiento
     */
    public static JScrollPane scrollConBorde(JComponent vista) {
        JScrollPane scroll = new JScrollPane(vista);
        scroll.setBorder(BorderFactory.createLineBorder(BORDE));
        scroll.getViewport().setBackground(TARJETA);
        scroll.setBackground(FONDO);
        return scroll;
    }

    /**
     * Crea un panel con apariencia de tarjeta (fondo blanco y borde estándar).
     *
     * @return panel estilizado como tarjeta
     */
    public static JPanel panelTarjeta() {
        JPanel tarjeta = new JPanel();
        tarjeta.setBackground(TARJETA);
        tarjeta.setBorder(bordeTarjeta());
        return tarjeta;
    }

    /**
     * Crea una etiqueta en negrita para formularios.
     *
     * @param texto texto visible de la etiqueta
     * @return etiqueta estilizada
     */
    public static JLabel etiquetaFormulario(String texto) {
        JLabel etiqueta = new JLabel(texto);
        etiqueta.setForeground(TEXTO);
        etiqueta.setFont(etiqueta.getFont().deriveFont(Font.BOLD, 13f));
        return etiqueta;
    }

    /**
     * Crea una barra de estado con contenido central y acciones a la derecha.
     *
     * @param centro contenido central (puede ser {@code null})
     * @param derecha acciones o información a la derecha (puede ser {@code null})
     * @return panel de barra inferior
     */
    public static JPanel barraEstado(JComponent centro, JComponent derecha) {
        JPanel barra = new JPanel(new java.awt.BorderLayout(12, 0));
        barra.setBackground(ENCABEZADO);
        barra.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, BORDE),
                BorderFactory.createEmptyBorder(10, 16, 10, 16)));
        if (centro != null) {
            barra.add(centro, java.awt.BorderLayout.CENTER);
        }
        if (derecha != null) {
            barra.add(derecha, java.awt.BorderLayout.EAST);
        }
        return barra;
    }

    /**
     * Aplica estilo al botón primario de acción principal.
     *
     * @param boton botón a estilizar
     */
    public static void aplicarEstiloBotonPrimario(JButton boton) {
        boton.setBackground(PRIMARIO);
        boton.setForeground(Color.WHITE);
        boton.setFocusPainted(false);
        boton.setBorderPainted(false);
        boton.setOpaque(true);
        boton.setFont(boton.getFont().deriveFont(Font.BOLD, 14f));
        boton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        boton.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
    }

    /**
     * Aplica estilo al botón secundario de acciones complementarias.
     *
     * @param boton botón a estilizar
     */
    public static void aplicarEstiloBotonSecundario(JButton boton) {
        boton.setBackground(FONDO_SECUNDARIO);
        boton.setForeground(TEXTO);
        boton.setFocusPainted(false);
        boton.setBorderPainted(false);
        boton.setOpaque(true);
        boton.setFont(boton.getFont().deriveFont(Font.BOLD, 13f));
        boton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        boton.setBorder(BorderFactory.createEmptyBorder(10, 18, 10, 18));
    }

    /**
     * Aplica estilo al botón fantasma (contorno sin relleno fuerte).
     *
     * @param boton botón a estilizar
     */
    public static void aplicarEstiloBotonFantasma(JButton boton) {
        boton.setBackground(TARJETA);
        boton.setForeground(PRIMARIO);
        boton.setFocusPainted(false);
        boton.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDE),
                BorderFactory.createEmptyBorder(8, 14, 8, 14)));
        boton.setOpaque(true);
        boton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    /**
     * Crea una tarjeta compacta para mostrar una estadística con acento lateral.
     *
     * @param titulo etiqueta descriptiva
     * @param valor valor numérico o texto destacado
     * @param acento color de la franja lateral
     * @return panel con la tarjeta de estadística
     */
    public static JPanel crearTarjetaEstadistica(String titulo, String valor, Color acento) {
        JPanel tarjeta = new JPanel(new java.awt.BorderLayout(0, 4));
        tarjeta.setBackground(TARJETA);
        tarjeta.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 0, 4, acento),
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(BORDE),
                        BorderFactory.createEmptyBorder(10, 12, 10, 12))));

        JLabel etiquetaTitulo = new JLabel(titulo);
        etiquetaTitulo.setForeground(TEXTO_TENUE);
        etiquetaTitulo.setFont(etiquetaTitulo.getFont().deriveFont(Font.PLAIN, 11f));

        JLabel etiquetaValor = new JLabel(valor);
        etiquetaValor.setForeground(TEXTO);
        etiquetaValor.setFont(etiquetaValor.getFont().deriveFont(Font.BOLD, 20f));

        tarjeta.add(etiquetaTitulo, java.awt.BorderLayout.NORTH);
        tarjeta.add(etiquetaValor, java.awt.BorderLayout.CENTER);
        return tarjeta;
    }

    /**
     * Organiza varias tarjetas de estadística en una fila horizontal.
     *
     * @param tarjetas tarjetas a disponer en fila
     * @return panel contenedor de la fila
     */
    public static JPanel crearFilaEstadisticas(JPanel... tarjetas) {
        JPanel fila = new JPanel(new GridLayout(1, tarjetas.length, 10, 0));
        fila.setOpaque(false);
        for (JPanel tarjeta : tarjetas) {
            fila.add(tarjeta);
        }
        return fila;
    }

    /**
     * Crea una etiqueta con texto atenuado (secundario).
     *
     * @param texto texto visible
     * @return etiqueta con color tenue
     */
    public static JLabel etiquetaTenue(String texto) {
        JLabel etiqueta = new JLabel(texto);
        etiqueta.setForeground(TEXTO_TENUE);
        return etiqueta;
    }

    /**
     * Crea una etiqueta de título principal en negrita.
     *
     * @param texto texto del título
     * @return etiqueta de título
     */
    public static JLabel etiquetaTitulo(String texto) {
        JLabel etiqueta = new JLabel(texto);
        etiqueta.setForeground(TEXTO);
        etiqueta.setFont(etiqueta.getFont().deriveFont(Font.BOLD, 20f));
        return etiqueta;
    }

    /**
     * Crea una barra de encabezado con hasta tres zonas: oeste, centro y este.
     *
     * @param partes componentes en orden oeste, centro y este (algunos pueden ser {@code null})
     * @return panel de encabezado
     */
    public static JPanel barraEncabezado(JComponent... partes) {
        JPanel barra = new JPanel(new java.awt.BorderLayout(12, 0));
        barra.setBackground(ENCABEZADO);
        barra.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, BORDE),
                BorderFactory.createEmptyBorder(12, 16, 12, 16)));
        if (partes.length > 0 && partes[0] != null) {
            barra.add(partes[0], java.awt.BorderLayout.WEST);
        }
        if (partes.length > 1 && partes[1] != null) {
            barra.add(partes[1], java.awt.BorderLayout.CENTER);
        }
        if (partes.length > 2 && partes[2] != null) {
            barra.add(partes[2], java.awt.BorderLayout.EAST);
        }
        return barra;
    }

    /**
     * Crea un borde con título para delimitar secciones de un formulario.
     *
     * @param titulo texto del título de la sección
     * @return borde compuesto con título
     */
    public static CompoundBorder tituloSeccion(String titulo) {
        return BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(
                        BorderFactory.createLineBorder(BORDE),
                        titulo,
                        javax.swing.border.TitledBorder.LEFT,
                        javax.swing.border.TitledBorder.TOP,
                        new Font("Segoe UI", Font.BOLD, 12),
                        TEXTO_TENUE),
                BorderFactory.createEmptyBorder(8, 10, 10, 10));
    }

    /**
     * Devuelve una dimensión preferida para botones.
     *
     * @param ancho ancho en píxeles
     * @param alto alto en píxeles
     * @return dimensión del botón
     */
    public static Dimension tamanoBoton(int ancho, int alto) {
        return new Dimension(ancho, alto);
    }
}
