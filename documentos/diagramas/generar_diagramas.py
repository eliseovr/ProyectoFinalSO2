"""Genera diagramas PNG para el Informe Tecnico."""
from pathlib import Path
from PIL import Image, ImageDraw, ImageFont

OUT = Path(__file__).parent
W, H = 1100, 700
BG = (255, 255, 255)
BOX = (227, 242, 253)
BORDER = (25, 118, 210)
TEXT = (33, 33, 33)
ARROW = (66, 66, 66)


def font(size=16):
    for name in ("arial.ttf", "Arial.ttf", "segoeui.ttf"):
        try:
            return ImageFont.truetype(name, size)
        except OSError:
            pass
    return ImageFont.load_default()


def new_canvas(h=H):
    img = Image.new("RGB", (W, h), BG)
    return img, ImageDraw.Draw(img)


def box(draw, xy, text, fnt, fill=BOX):
    x1, y1, x2, y2 = xy
    draw.rounded_rectangle(xy, radius=12, fill=fill, outline=BORDER, width=2)
    tw = draw.textlength(text, font=fnt)
    th = fnt.size + 4
    draw.text((x1 + (x2 - x1 - tw) / 2, y1 + (y2 - y1 - th) / 2), text, fill=TEXT, font=fnt)


def arrow(draw, a, b):
    draw.line([a, b], fill=ARROW, width=2)
    bx, by = b
    draw.polygon([(bx, by), (bx - 8, by - 5), (bx - 8, by + 5)], fill=ARROW)


def title(draw, text, fnt):
    draw.text((40, 20), text, fill=TEXT, font=fnt)


def save(img, name):
    img.save(OUT / name, "PNG")
    print("OK", name)


def casos_uso():
    img, d = new_canvas(780)
    f, ft = font(18), font(14)
    title(d, "Diagrama de casos de uso", f)
    box(d, (80, 80, 220, 130), "Usuario", ft, (255, 243, 224))
    box(d, (440, 80, 620, 130), "Administrador", ft, (255, 243, 224))
    box(d, (820, 80, 980, 130), "Sistema", ft, (255, 243, 224))
    for i, t in enumerate(["Registrarse", "Iniciar sesion", "Enviar mensaje E2E", "Recibir mensaje"]):
        box(d, (40, 180 + i * 95, 260, 240 + i * 95), t, ft)
        arrow(d, (150, 130), (150, 180 + i * 95))
    for i, t in enumerate(["Ver monitor", "Administrar BD", "Consultas SQL"]):
        box(d, (420, 180 + i * 110, 640, 240 + i * 110), t, ft)
        arrow(d, (530, 130), (530, 180 + i * 110))
    for i, t in enumerate(["Validar TLS", "Persistir mensajes", "Registrar logs"]):
        box(d, (800, 180 + i * 110, 1000, 240 + i * 110), t, ft)
        arrow(d, (900, 130), (900, 180 + i * 110))
    save(img, "01_casos_uso.png")


def clases_cliente():
    img, d = new_canvas(820)
    f, ft = font(18), font(13)
    title(d, "Diagrama de clases - Cliente (MVC)", f)
    rows = [
        ("VistaLogin / VistaChat", 70),
        ("ControladorLogin / ControladorChat", 210),
        ("ServicioAutenticacion / ServicioMensajeria", 350),
        ("SesionCliente / ModeloBandeja", 490),
        ("ConexionServidor (TLS)", 630),
    ]
    for text, y in rows:
        box(d, (250, y, 850, y + 70), text, ft)
    for y in (140, 280, 420, 560):
        arrow(d, (550, y), (550, y + 30))
    save(img, "02_clases_cliente.png")


def clases_servidor():
    img, d = new_canvas(820)
    f, ft = font(18), font(13)
    title(d, "Diagrama de clases - Servidor", f)
    rows = [
        ("ServidorMensajeria", 70),
        ("ManejadorCliente (por conexion)", 190),
        ("BaseDatosSqlite / GestorSesiones", 310),
        ("ControladorMonitor / ModeloMonitor", 430),
        ("MarcoMonitorServidor / MarcoAdminBaseDatos", 550),
        ("RegistroActividad (logs)", 670),
    ]
    for text, y in rows:
        box(d, (180, y, 920, y + 60), text, ft)
    for y in (130, 250, 370, 490, 610):
        arrow(d, (550, y), (550, y + 20))
    save(img, "03_clases_servidor.png")


def flujo_login():
    img, d = new_canvas(900)
    f, ft = font(18), font(13)
    title(d, "Diagrama de flujo - Login del cliente", f)
    steps = [
        "Inicio", "Mostrar VistaLogin", "Ingresar credenciales", "Validar formato",
        "Conectar TLS", "Enviar LOGIN", "BCrypt valido?", "Abrir VistaChat", "Fin",
    ]
    y = 70
    for i, s in enumerate(steps):
        box(d, (350, y, 750, y + 50), s, ft)
        if i < len(steps) - 1:
            arrow(d, (550, y + 50), (550, y + 75))
        y += 75
    save(img, "04_flujo_login.png")


def flujo_mensaje():
    img, d = new_canvas(950)
    f, ft = font(18), font(12)
    title(d, "Diagrama de flujo - Envio de mensaje E2E", f)
    steps = [
        "Usuario escribe mensaje",
        "Cifrar con clave publica del destinatario",
        "Enviar ENVIAR por TLS",
        "Servidor valida sesion",
        "Persistir E2E1 en SQLite",
        "Destinatario online?",
        "Push MENSAJE / guardar historial",
        "Confirmar al remitente",
    ]
    y = 60
    for s in steps:
        box(d, (250, y, 850, y + 55), s, ft)
        arrow(d, (550, y + 55), (550, y + 75))
        y += 80
    save(img, "05_flujo_mensaje.png")


def flujo_servidor():
    img, d = new_canvas(900)
    f, ft = font(18), font(13)
    title(d, "Diagrama de flujo - Servidor concurrente", f)
    steps = [
        "Iniciar ServidorMensajeria",
        "Cargar certificado TLS",
        "Crear SSLServerSocket :9443",
        "Inicializar SQLite y logs",
        "Bucle accept()",
        "Nueva conexion -> ExecutorService",
        "ManejadorCliente procesa paquetes",
        "Cerrar al desconectar",
    ]
    y = 60
    for s in steps:
        box(d, (220, y, 880, y + 52), s, ft)
        if s.startswith("Cerrar"):
            arrow(d, (550, y + 52), (900, y + 52))
            arrow(d, (900, y + 52), (900, 335))
            arrow(d, (900, 335), (550, 335))
        else:
            arrow(d, (550, y + 52), (550, y + 72))
        y += 78
    save(img, "06_flujo_servidor.png")


def entidad_relacion():
    img, d = new_canvas(620)
    f, ft, fs = font(18), font(14), font(12)
    title(d, "Diagrama entidad-relacion (SQLite)", f)
    box(d, (120, 100, 480, 280), "", ft)
    d.text((140, 115), "USUARIOS", fill=TEXT, font=ft)
    for i, col in enumerate(["id (PK)", "usuario (UK)", "hash_contrasena", "creado_en", "clave_publica"]):
        d.text((150, 155 + i * 22), col, fill=TEXT, font=fs)
    box(d, (620, 100, 980, 300), "", ft)
    d.text((640, 115), "MENSAJES", fill=TEXT, font=ft)
    for i, col in enumerate(["id (PK)", "remitente (FK)", "destinatario (FK)", "contenido (E2E)", "enviado_en"]):
        d.text((650, 155 + i * 22), col, fill=TEXT, font=fs)
    arrow(d, (480, 190), (620, 190))
    d.text((500, 165), "1:N remite/recibe", fill=TEXT, font=fs)
    save(img, "07_entidad_relacion.png")


if __name__ == "__main__":
    casos_uso()
    clases_cliente()
    clases_servidor()
    flujo_login()
    flujo_mensaje()
    flujo_servidor()
    entidad_relacion()
