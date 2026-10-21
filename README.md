# Sistema de Solicitudes — QA Automatizado

Proyecto base **Java** usado como objeto de prueba para la práctica de
**automatización de pruebas con Selenium WebDriver, JUnit Jupiter y Jenkins Pipeline**
(INF-433 · Aseguramiento de Calidad del Software · Unidad II-2).

Es una aplicación web autocontenida (no requiere contenedor de servlets ni framework)
que expone un formulario de **registro de solicitudes de financiamiento** y aplica una
**regla de negocio** validable de forma aislada.

- Licencia: **MIT** (ver [LICENSE](LICENSE))
- Regla de negocio: `ValidadorSolicitud` — el nombre es obligatorio (mín. 3 caracteres),
  el correo debe tener formato válido y el monto debe estar **entre 100.00 y 5000.00**.

---

## 1. Tecnologías y versiones

| Componente | Versión / detalle |
|---|---|
| JDK | 17 o superior (probado con JDK 24.0.1) |
| Maven | 3.9 o superior |
| JUnit Jupiter | 5.10.2 |
| Selenium WebDriver | 4.25.0 (incluye Selenium Manager, descarga el driver automáticamente) |
| Navegador | Google Chrome (modo `--headless=new`) |
| Servidor HTTP | `com.sun.net.httpserver` (incluido en el JDK) |
| Jenkins | LTS con el plugin *Pipeline* |

---

## 2. Estructura del proyecto

```
sistema-solicitudes-qa/
├── Jenkinsfile                     # Pipeline declarativo (código → compilar → JUnit → Selenium → evidencias)
├── LICENSE                         # Licencia MIT
├── pom.xml                         # Configuración Maven (JUnit 5 + Selenium + Surefire)
├── README.md                       # Este archivo
└── src
    ├── main/java/com/utesa/calidad
    │   ├── App.java                        # Punto de entrada de la aplicación
    │   ├── modelo
    │   │   ├── Solicitud.java              # Entidad del dominio
    │   │   └── ResultadoRegistro.java      # Resultado de la operación de registro
    │   ├── repositorio
    │   │   └── SolicitudRepositorio.java   # Persistencia en memoria
    │   ├── servicio
    │   │   ├── ValidadorSolicitud.java     # REGLA DE NEGOCIO (objeto de las pruebas unitarias)
    │   │   └── SolicitudServicio.java      # Orquesta validador + repositorio
    │   └── web
    │       ├── ServidorWeb.java            # Servidor HTTP (rutas / y /registrar)
    │       └── Vistas.java                 # Generación del HTML
    └── test/java/com/utesa/calidad
        ├── unitaria/ValidadorSolicitudTest.java   # 12 pruebas JUnit (válido, límite, inválido)
        └── ui/FlujoWebSeleniumTest.java           # 2 escenarios Selenium (S-01 y S-02)
```

---

## 3. Requisitos previos

1. **JDK 17+** — comprobar con:
   ```
   java -version
   ```
2. **Maven 3.9+** — comprobar con:
   ```
   mvn -version
   ```
   Si no está instalado:
   - Windows (Chocolatey): `choco install maven`
   - Manual: descargar el binario, descomprimir y añadir `<ruta>/apache-maven-3.9.x/bin` al `PATH`.
3. **Google Chrome** instalado (Selenium Manager obtiene el `chromedriver` automáticamente,
   por lo que se requiere conexión a Internet la primera vez).

---

## 4. Ejecutar la aplicación

```powershell
mvn clean package -DskipTests
java -jar target/sistema-solicitudes-qa.jar
```

La consola muestra la URL de acceso (`http://127.0.0.1:8080`). Abrirla en el navegador
para ver el formulario. Para usar otro puerto:

```powershell
java -jar target/sistema-solicitudes-qa.jar 9090
```

También es posible ejecutarla sin empaquetar:

```powershell
mvn compile exec:java
```

---

## 5. Ejecutar las pruebas

### Suite completa

```powershell
mvn test
```

### Solo las pruebas unitarias (JUnit)

```powershell
mvn -Dtest=ValidadorSolicitudTest test
```

### Solo las pruebas de interfaz (Selenium)

```powershell
mvn -Dtest=FlujoWebSeleniumTest test
```

Las capturas de pantalla de los escenarios Selenium se guardan en
`target/screenshots/` y los reportes XML en `target/surefire-reports/`.

---

## 6. Configurar el trabajo en Jenkins

1. Instalar los plugins **Pipeline**, **JUnit**, **Git** y **Workspace Cleanup**.
2. En *Administrar Jenkins → Tools* declarar un **JDK 17+** y un **Maven 3.9+**
   (por ejemplo con los nombres `jdk-17` y `maven-3.9`). Si se declaran, descomentar el
   bloque `tools` del `Jenkinsfile` y ajustar los nombres.
3. En *Nueva tarea → Pipeline* (o *Multibranch Pipeline*):
   - **Definition**: `Pipeline script from SCM`
   - **SCM**: `Git` → URL del repositorio del proyecto
   - **Script Path**: `Jenkinsfile`
4. Ejecutar *Build Now* y revisar *Console Output*, *Test Result* y *Artifacts*.

En el agente de Jenkins deben estar instalados el JDK, Maven y Google Chrome. El
`Jenkinsfile` usa `bat` en Windows y `sh` en Linux/macOS de forma automática.

---

## 7. Correspondencia con las evidencias solicitadas

| Evidencia | De dónde se obtiene |
|---|---|
| E-01 | Proyecto abierto en el IDE y aplicación en ejecución (`java -jar target/sistema-solicitudes-qa.jar`). |
| E-02 | `src/test/java/com/utesa/calidad/ui/FlujoWebSeleniumTest.java` (localizadores, datos, acciones y aserciones). |
| E-03 | `target/screenshots/S-01_01_pagina_inicial.png` y `S-02_01_pagina_inicial.png`. |
| E-04 | `target/screenshots/S-01_02_formulario_completado.png`. |
| E-05 | `target/screenshots/S-01_03_confirmacion.png` y `S-01_04_listado_con_registro.png` + salida de `mvn -Dtest=FlujoWebSeleniumTest test`. |
| E-06 | `target/screenshots/S-02_02_datos_invalidos.png` y `S-02_03_rechazo.png` + salida de la consola. |
| E-07 | `src/test/java/com/utesa/calidad/unitaria/ValidadorSolicitudTest.java` + resultado de `mvn -Dtest=ValidadorSolicitudTest test`. |
| E-08 | `Jenkinsfile` en el repositorio y pantalla de configuración del trabajo en Jenkins. |
| E-09 | *Console Output* de la ejecución del pipeline (etapas y salida de ambas suites). |
| E-10 | *Test Result* del build en Jenkins (pruebas aprobadas/fallidas) y artefactos archivados. |

---

## 8. Licencia

Distribuido bajo la **Licencia MIT**. Ver el archivo [LICENSE](LICENSE).
