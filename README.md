# Proyecto Final SO2 — Mensajería Segura Distribuida

Sistema cliente-servidor en **Java 11+** con **Gradle**, **TLS**, **cifrado de extremo a extremo (E2E)**, **BCrypt**, **SQLite**, logs de actividad e **interfaz gráfica Swing**.

---

# 1. Introducción

Este proyecto implementa un sistema de mensajería segura distribuida con:

- Autenticación robusta (BCrypt)
- Cifrado en tránsito (TLS 1.2/1.3)
- Cifrado de extremo a extremo (RSA‑OAEP + AES‑GCM)
- Persistencia con SQLite
- Interfaz gráfica Swing
- Arquitectura cliente‑servidor multihilo

---

# 2. Requisitos

## 2.1 Software necesario

- **JDK 11+** (recomendado JDK 17)
- **Gradle Wrapper** (incluido en el proyecto)
- **VMware Workstation + Ubuntu Desktop** (para entorno distribuido)

---

# 3. Configuración inicial

## 3.1 Generación de certificados TLS

```powershell
cd ruta\a\ProyectoFinalSO2\certificados
.\generar-certificados.ps1
```

Se generan:

- `server.p12`
- `truststore.p12`  
Contraseña: `changeit`

---

## 3.2 Compilación del proyecto

```powershell
cd ruta\a\ProyectoFinalSO2
.\gradlew.bat build
```

---

# 4. Ejecución local (modo demostración)

## 4.1 Iniciar servidor

```powershell
.\iniciar-servidor.bat
```

## 4.2 Iniciar clientes (dos instancias)

```powershell
.\iniciar-cliente.bat
```

### Flujo de uso

1. Registrar usuarios distintos.  
2. Iniciar sesión.  
3. Seleccionar destinatario.  
4. Enviar y recibir mensajes en tiempo real.

## 4.3 Administrador de base de datos

```powershell
.\ver-admin-base-datos.bat
```

Si aparece *ClassNotFoundException*:

```powershell
.\abrir-admin-bd.bat
```

O:

```powershell
.\gradlew.bat :servidor:ejecutarAdminBd
```

---

# 5. Entorno distribuido (VMware + Ubuntu)

Guía detallada: **`documentos/Manuales/Guia_VMware_Ubuntu.docx`**

## 5.1 En la VM servidor

```bash
chmod +x gradlew scripts/ubuntu/*.sh *.sh
./scripts/ubuntu/instalar-requisitos.sh
./iniciar-servidor.sh
```

## 5.2 En cada VM cliente

```bash
./scripts/ubuntu/copiar-certificados-desde-servidor.sh 192.168.56.10 ubuntu
./iniciar-cliente.sh
```

En el login:

- Host: IP del servidor (ej. `192.168.56.10`)
- Puerto: `9443`

---

# 6. Documentación (entrega)

| Archivo | Contenido |
|---------|-----------|
| `documentos/Manuales/Manual de Usuario.pdf` | Guía de uso para el usuario final |
| `documentos/Manuales/Guia_VMware_Ubuntu.docx` | Instalación, VMware/Ubuntu, administración |
| `documentos/Manuales/Informe Final SMCD.docx` | Informe del proyecto (Word) |
| `documentos/Manuales/Informe Final SMCD.pdf` | Informe del proyecto (PDF) |
| `documentos/Manuales/Formato IEEE830 SMCD.pdf` | Especificación de requisitos (IEEE 830) |
| `documentos/Manuales/Presentacion_Mensajeria_Cifrada.pptx` | Presentación del proyecto |
| `documentos/diagramas/` | Diagramas UML y de flujo (`.mmd`, `.png`) |

---

# 7. Estructura del proyecto

```
ProyectoFinalSO2/
├── comun/
│   └── src/main/java/comun/
├── cliente/
│   └── src/main/java/cliente/
│       ├── modelo/
│       ├── servicio/
│       ├── controlador/
│       └── vista/
├── servidor/
│   └── src/main/java/servidor/
│       ├── modelo/
│       ├── controlador/
│       └── vista/
├── certificados/
├── scripts/
│   ├── windows/
│   └── ubuntu/
├── documentos/
│   ├── Manuales/
│   └── diagramas/
├── datos/
└── registros/
```

---

# 8. Ubicación de carpetas MVC

| Módulo | Ruta |
|--------|-------|
| Cliente | `cliente/src/main/java/cliente/...` |
| Servidor | `servidor/src/main/java/servidor/...` |

Ejemplos:

```
ProyectoFinalSO2/cliente/src/main/java/cliente/modelo
ProyectoFinalSO2/cliente/src/main/java/cliente/vista
ProyectoFinalSO2/servidor/src/main/java/servidor/modelo
```

---

# 9. Arquitectura MVC

| Capa | Cliente | Servidor |
|------|---------|----------|
| Modelo | Sesión, bandeja, validación | Usuarios, métricas |
| Vista | Login, Chat (Swing) | Monitor, Admin BD |
| Controlador | Login, Chat | Monitor, ManejadorCliente |
| Servicio | TLS, autenticación, mensajería | SQLite, administración |

### Flujo cliente

```
VistaLogin → ControladorLogin → ServicioAutenticacion
→ VistaChat → ControladorChat → ServicioMensajeria
```

---

# 10. Variables de entorno

| Variable | Descripción | Por defecto |
|----------|-------------|-------------|
| SERVER_PORT | Puerto TLS | 9443 |
| SERVER_HOST | Host del servidor | localhost |
| APP_BASE | Directorio base | . |
| KEYSTORE_PATH | Certificado TLS | certificados/server.p12 |
| HEADLESS | Cliente consola | — |
| CLIENT_USER | Usuario consola | eliseo |
| CLIENT_PEER | Destinatario | — |

---

# 11. Cumplimiento de requisitos

| Requisito | Implementación |
|-----------|----------------|
| Autenticación segura | BCrypt + tokens |
| Cifrado en tránsito | TLS 1.2/1.3 |
| Cifrado E2E | RSA-OAEP + AES-GCM |
| Tiempo real | Push MENSAJE |
| Concurrencia | Pool de hilos |
| Persistencia | SQLite |
| Logs | registros/sistema.log |
| Entorno distribuido | VMware + Ubuntu |

---

# 12. Bibliotecas externas

| Biblioteca | Uso | Licencia |
|------------|-----|----------|
| jBCrypt | Hash seguro | BSD |
| SQLite JDBC | Persistencia | Apache 2.0 |

---

# 13. Autor

**Eliseo Velásquez** — Carné 000132837  
Proyecto Final — Sistemas Operativos 2
