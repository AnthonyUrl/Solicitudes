# Informe Técnico — Automatización de pruebas con Selenium, JUnit y Jenkins

**Asignatura:** INF-433 — Aseguramiento de Calidad del Software
**Unidad:** II-2 · Automatización de Pruebas
**Actividad:** Práctica Unidad II-2 — Valor 5.00 puntos
**Modalidad:** Individual
**Autor:** Anthony Ventura
**Institución:** UTESA
**Fecha:** 20 de octubre de 2026

> **Nota sobre las evidencias.** Este documento incluye marcadores `[[CAPTURA: ...]]`
> en las secciones donde corresponde insertar las capturas E-01 … E-10. Todas las
> capturas deben tomarse de la ejecución real del proyecto descrito aquí; no se
> incluyen imágenes inventadas ni de tutoriales.

---

## 1. Portada y contexto

### 1.1 Descripción del proyecto seleccionado

| Elemento | Detalle |
|---|---|
| **Nombre del proyecto** | Sistema de Solicitudes — QA Automatizado (`sistema-solicitudes-qa`) |
| **Propósito** | Aplicación web que registra solicitudes de financiamiento, valida los datos mediante una regla de negocio y muestra el listado de solicitudes aceptadas. |
| **Origen** | Proyecto propio de código abierto, creado para esta práctica y liberado bajo licencia **MIT**. Se optó por un proyecto propio en lugar de uno de terceros para garantizar el acceso completo al código fuente, evitar dependencias externas y disponer de total control sobre la regla de negocio a automatizar. |
| **Repositorio / ruta local** | `sistema-solicitudes-qa/` (incluido en el ZIP de entrega) |
| **Tecnología** | Java 17+ (compilado con JDK 24.0.1), servidor HTTP embebido del JDK (`com.sun.net.httpserver`), sin framework web de terceros. |
| **Gestor de dependencias y construcción** | Maven (JUnit Jupiter 5.10.2, Selenium WebDriver 4.25.0). |
| **Cómo se inicia** | `mvn clean package -DskipTests` y luego `java -jar target/sistema-solicitudes-qa.jar` (puerto por defecto 8080). |
| **Interfaz web** | Formulario HTML con tres campos (`nombre`, `correo`, `monto`), un botón *Registrar solicitud*, una zona de mensajes de resultado y una tabla con las solicitudes registradas. |

### 1.2 Regla de negocio seleccionada

La clase `ValidadorSolicitud` concentra la lógica de negocio y **no** depende de red,
base de datos ni de la interfaz web, lo que la convierte en un candidato ideal para
pruebas unitarias aisladas y deterministas:

1. **Nombre** obligatorio y con una longitud mínima de 3 caracteres.
2. **Correo** obligatorio y con formato de correo válido.
3. **Monto** obligatorio y comprendido **entre 100.00 y 5000.00** (límites inclusivos).

Regla derivada también verificada: la **comisión** es del 1.5 % para montos inferiores
a 1000.00 y del 1.0 % a partir de ese umbral.

### 1.3 Escenarios de interfaz seleccionados

| ID | Tipo | Descripción |
|---|---|---|
| **S-01** | Positivo | Registrar una solicitud con datos válidos → aparece un mensaje de confirmación y la solicitud se incorpora al listado. |
| **S-02** | Negativo | Registrar datos inválidos (nombre omitido y monto fuera de rango) → se muestra el rechazo y **no** se crea ningún registro. |

### 1.4 Correspondencia entre escenarios y requisitos de la actividad

| Requisito de la actividad | Dónde se cumple |
|---|---|
| Flujo web comprobable | Registro de solicitudes S-01 / S-02 |
| Regla de negocio Java apta para pruebas | `ValidadorSolicitud.validar(...)` y `calcularComision(...)` |
| Escenario de éxito | S-01 |
| Escenario alternativo/negativo | S-02 |
| Al menos 3 métodos `@Test` | `ValidadorSolicitudTest` (12 métodos) |
| Casos válido, límite e inválido | Cubiertos explícitamente (ver sección 5) |

[[CAPTURA E-01: Proyecto abierto en el IDE con la estructura de paquetes y, en una segunda ventana/terminal, la aplicación en ejecución en `http://127.0.0.1:8080`.]]

---

## 2. Objetivo y diseño de la automatización

### 2.1 Objetivo

Implementar un proceso de aseguramiento de calidad automatizado y reproducible sobre el
proyecto descrito, integrando tres niveles de verificación en una única canalización:

1. **Pruebas unitarias** de la regla de negocio (JUnit Jupiter 5).
2. **Pruebas de interfaz de usuario** sobre el flujo web real (Selenium WebDriver).
3. **Pipeline de integración continua** en Jenkins que ejecuta ambas suites y publica
   las evidencias como artefactos.

### 2.2 Criterios de selección de los casos automatizables

Para escoger qué automatizar se aplicaron cuatro criterios:

- **Estabilidad de la interfaz:** el formulario es estable y sus campos tienen
  identificadores fijos, por lo que los localizadores no se rompen con cambios menores.
- **Valor de regresión:** la validación del monto y del nombre son reglas que cambian con
  frecuencia (nuevos umbrales de negocio) y son las que más riesgo de regresión aportan.
- **Determinismo:** el repositorio es en memoria y cada prueba arranca su propio servidor
  en un puerto libre, por lo que no hay estado compartido ni dependencias externas.
- **Cobertura del riesgo:** un escenario positivo (camino feliz) y uno negativo (ruta de
  rechazo) cubren las dos ramas de decisión más relevantes del flujo.

### 2.3 Diseño de la pirámide de pruebas aplicada

```
                  ┌─────────────────────────────┐
   Mayor coste →  │  2 pruebas Selenium (UI)    │  Flujo web extremo a extremo
                  ├─────────────────────────────┤
                  │  12 pruebas JUnit (unidad)  │  Regla de negocio aislada
                  └─────────────────────────────┘
                                ↑
                     Pipeline Jenkins que ejecuta ambas
```

Se respeta así la aclaración de la actividad: **una prueba Selenium ejecutada con JUnit no
es una prueba unitaria**. Las pruebas de `ValidadorSolicitudTest` no abren navegador ni
servidor; las de `FlujoWebSeleniumTest` sí, y verifican el sistema completo.

### 2.4 Decisiones de diseño relevantes

| Decisión | Justificación |
|---|---|
| Servidor HTTP del JDK en lugar de Spring Boot / Tomcat | El proyecto es autocontenido: no requiere contenedor externo, la descarga de dependencias es mínima y la canalización de Jenkins se mantiene rápida y reproducible. |
| Puerto `0` (libre) al iniciar el servidor en las pruebas | Evita colisiones de puerto y permite que ambas suites sean paralelizables y repetibles. |
| Chrome en modo `--headless=new` | Necesario para que las pruebas se ejecuten en un agente de Jenkins sin entorno gráfico. |
| Esperas explícitas (`WebDriverWait`) y espera implícita en cero | Elimina el uso de `Thread.sleep` y hace las sincronizaciones deterministas y trazables. |
| Reportes Surefire en carpetas separadas por suite | Permite que Jenkins muestre el resultado de cada suite por separado sin que una ejecución sobrescriba a la otra. |
| Sin datos personales reales | Los datos utilizados (`Ana Perez`, `ana.perez@ejemplo.com`) son ficticios y de uso interno de la prueba. |

### 2.5 Trazabilidad requisito → prueba

| Regla / comportamiento | Caso de prueba | Tipo |
|---|---|---|
| Solicitud con datos correctos se acepta | `solicitudConDatosValidosNoGeneraErrores` | Unitaria (válido) |
| Monto = 100.00 es válido | `montoExactamenteEnElMinimoEsValido` | Unitaria (límite) |
| Monto = 5000.00 es válido | `montoExactamenteEnElMaximoEsValido` | Unitaria (límite) |
| Monto = 99.99 es rechazado | `montoJustoPorDebajoDelMinimoEsRechazado` | Unitaria (límite/inválido) |
| Nombre de 3 caracteres se acepta | `nombreConLongitudMinimaSeAcepta` | Unitaria (límite) |
| Comisión cambia en 1000.00 | `comisionCambiaEnElUmbralDeMil` | Unitaria (límite) |
| Monto > 5000.00 es rechazado | `montoPorEncimaDelMaximoEsRechazado` | Unitaria (inválido) |
| Correo con formato inválido es rechazado | `correoConFormatoInvalidoEsRechazado` | Unitaria (inválido) |
| Nombre vacío es rechazado | `nombreVacioEsRechazado` | Unitaria (inválido) |
| Monto no numérico es rechazado | `montoNoNumericoEsRechazado` | Unitaria (inválido) |
| Solicitud nula lanza excepción | `solicitudNulaLanzaExcepcion` | Unitaria (`assertThrows`) |
| Comisión fuera de rango lanza excepción | `comisionFueraDeRangoLanzaExcepcion` | Unitaria (`assertThrows`) |
| Flujo exitoso de registro web | `s01RegistroValidoMuestraConfirmacion` | Selenium (S-01) |
| Flujo de rechazo web | `s02RegistroInvalidoMuestraErrorYNoRegistra` | Selenium (S-02) |

---

## 3. Ambiente de pruebas (versiones registradas)

| Componente | Versión | Cómo se verificó |
|---|---|---|
| Sistema operativo | Windows 11 (64 bits) | `winver` / `systeminfo` |
| JDK | **24.0.1** (compilación con `--release 17`) | `java -version` |
| Maven | **{{COMPLETAR: p. ej. 3.9.9}}** | `mvn -version` |
| Google Chrome | **{{COMPLETAR: p. ej. 141.0.xxxx}}** | `chrome://version` |
| ChromeDriver | Gestionado automáticamente por Selenium Manager | Se muestra en el log de la primera ejecución |
| Selenium WebDriver | **4.25.0** | `pom.xml` |
| JUnit Jupiter | **5.10.2** | `pom.xml` |
| Jenkins | **{{COMPLETAR: p. ej. 2.4xx LTS}}** | *Administrar Jenkins → Acerca de Jenkins* |
| Plugins de Jenkins | Pipeline, JUnit, Git, Workspace Cleanup | *Administrar Jenkins → Plugins* |
| Git | **2.49.0** | `git --version` |

> Sustituya los valores entre `{{ }}` por los de su equipo. La tarea exige registrar las
> versiones reales del entorno donde se ejecutaron las pruebas.

[[CAPTURA: salida de `java -version`, `mvn -version` y `chrome://version` en una sola imagen o en tres imágenes consecutivas.]]

---

## 4. Pruebas de interfaz web con Selenium (S-01 y S-02)

### 4.1 Clase de prueba

Archivo: `src/test/java/com/utesa/calidad/ui/FlujoWebSeleniumTest.java`.

Cada prueba ejecuta el siguiente ciclo de vida, gestionando explícitamente la aplicación
bajo prueba y el navegador:

```java
@BeforeEach
void abrirEscenario() throws IOException {
    // 1) Aplicacion bajo prueba: servidor real en un puerto libre.
    servidor = ServidorWeb.iniciar(0);
    urlBase = servidor.getUrlBase();

    // 2) Navegador: Chrome en modo headless para que funcione tambien en Jenkins.
    ChromeOptions opciones = new ChromeOptions();
    opciones.addArguments("--headless=new", "--no-sandbox", "--disable-dev-shm-usage",
            "--window-size=1280,900", "--remote-allow-origins=*");
    navegador = new ChromeDriver(opciones);

    // Se usan esperas explicitas; la espera implicita se deja en cero.
    navegador.manage().timeouts().implicitlyWait(Duration.ZERO);
    espera = new WebDriverWait(navegador, ESPERA);

    // Precondicion: la pagina inicial carga correctamente.
    navegador.get(urlBase + "/");
}

@AfterEach
void cerrarEscenario() {
    if (navegador != null) navegador.quit();
    if (servidor != null) servidor.detener();
}
```

### 4.2 Localizadores empleados y su justificación

| Localizador | Elemento | Justificación |
|---|---|---|
| `By.id("nombre")` | Campo de nombre | `id` es el localizador más rápido y estable; los identificadores del formulario están fijados en la vista y no dependen del estilo ni del orden del DOM. |
| `By.id("correo")` | Campo de correo | Igual que el anterior. |
| `By.id("monto")` | Campo de monto | Igual que el anterior. |
| `By.id("btn-registrar")` | Botón de envío | Identificador semántico y único. |
| `By.id("mensaje-resultado")` | Zona de mensajes | Es el elemento cuya visibilidad y texto constituyen la verificación principal. |
| `By.id("contador-solicitudes")` | Contador del listado | Permite verificaciones numéricas estables mediante `ExpectedConditions.textToBe`. |
| `By.cssSelector("#cuerpo-solicitudes tr")` | Filas del listado | Selector CSS que localiza una colección; se usa para comprobar la presencia o ausencia de registros. |

Se evitó el uso de XPath absolutos y de localizadores basados en texto visible, ya que
son frágiles ante cambios de maquetación o traducción.

### 4.3 Escenario S-01 (positivo)

```java
@Test
void s01RegistroValidoMuestraConfirmacion() {
    // Precondicion: no existe ninguna solicitud registrada
    assertEquals("0", texto(By.id("contador-solicitudes")));
    capturar("S-01_01_pagina_inicial");

    // Datos de entrada (ficticios, no son datos personales reales)
    escribir(By.id("nombre"), "Ana Perez");
    escribir(By.id("correo"), "ana.perez@ejemplo.com");
    escribir(By.id("monto"), "1500");
    capturar("S-01_02_formulario_completado");

    // Accion
    navegador.findElement(By.id("btn-registrar")).click();

    // Verificacion 1: mensaje de confirmacion (espera explicita)
    WebElement mensaje = espera.until(
            ExpectedConditions.visibilityOfElementLocated(By.id("mensaje-resultado")));
    assertEquals("exito", mensaje.getAttribute("data-tipo"));
    assertTrue(mensaje.getText().contains("Solicitud registrada correctamente"));
    assertTrue(mensaje.getText().contains("Ana Perez"));
    capturar("S-01_03_confirmacion");

    // Verificacion 2: la operacion es observable en el listado
    espera.until(ExpectedConditions.textToBe(By.id("contador-solicitudes"), "1"));
    List<WebElement> filas = navegador.findElements(By.cssSelector("#cuerpo-solicitudes tr"));
    assertEquals(1, filas.size());
    assertTrue(filas.get(0).getText().contains("Ana Perez"));
    assertTrue(filas.get(0).getText().contains("ana.perez@ejemplo.com"));
    assertTrue(filas.get(0).getText().contains("1500.00"));
    capturar("S-01_04_listado_con_registro");
}
```

**Precondición:** servidor recién iniciado y listado vacío.
**Acción:** completar los tres campos con datos válidos y pulsar *Registrar solicitud*.
**Espera:** `WebDriverWait` de 15 s sobre la visibilidad del mensaje y sobre el texto del
contador; la espera implícita está en cero.
**Aserciones:** el atributo `data-tipo` vale `exito`, el mensaje contiene el texto de
confirmación y el nombre del solicitante, el contador pasa a `1` y existe exactamente una
fila con los datos enviados. No basta con que el mensaje aparezca: también se verifica el
efecto observable (persistencia en el listado).

[[CAPTURA E-02: fragmento del código de la prueba Selenium con los localizadores, los datos y las aserciones resaltados.]]
[[CAPTURA E-03: página inicial antes de la interacción, con el contador en `0` (`S-01_01_pagina_inicial.png`).]]
[[CAPTURA E-04: formulario completado con los datos de la prueba (`S-01_02_formulario_completado.png`).]]
[[CAPTURA E-05: resultado del escenario positivo: mensaje de confirmación y fila creada (`S-01_03_confirmacion.png`, `S-01_04_listado_con_registro.png`).]]

### 4.4 Escenario S-02 (negativo)

```java
@Test
void s02RegistroInvalidoMuestraErrorYNoRegistra() {
    assertEquals("0", texto(By.id("contador-solicitudes")));
    capturar("S-02_01_pagina_inicial");

    // Datos invalidos: se omite el nombre obligatorio y el monto esta fuera de rango
    escribir(By.id("correo"), "ana@ejemplo.com");
    escribir(By.id("monto"), "50");
    capturar("S-02_02_datos_invalidos");

    navegador.findElement(By.id("btn-registrar")).click();

    WebElement mensaje = espera.until(
            ExpectedConditions.visibilityOfElementLocated(By.id("mensaje-resultado")));
    assertEquals("error", mensaje.getAttribute("data-tipo"));
    assertTrue(mensaje.getText().contains("No se pudo registrar la solicitud"));
    assertTrue(mensaje.getText().contains("El nombre es obligatorio"));
    assertTrue(mensaje.getText().contains("El monto debe estar entre"));
    capturar("S-02_03_rechazo");

    // Ausencia de efectos: nada se registro
    assertEquals("0", texto(By.id("contador-solicitudes")));
    assertEquals(0, navegador.findElements(By.cssSelector("#cuerpo-solicitudes tr")).size());
    assertTrue(navegador.findElement(By.id("estado-vacio")).isDisplayed());
}
```

**Precondición:** servidor recién iniciado y listado vacío.
**Acción:** enviar el formulario omitiendo el nombre y con un monto de `50` (por debajo del
mínimo de 100.00).
**Aserciones:** el resultado se marca como `error`, el texto informa de los dos defectos
detectados, el contador permanece en `0`, **no existe ninguna fila** en el listado y se
muestra el estado «listado vacío».

Nota: los campos del formulario son de tipo `text` y no llevan el atributo `required`, de
modo que la validación la realiza el servidor y no el navegador. Esto es intencional: si se
usara validación HTML5, el navegador bloquearía el envío y la prueba nunca llegaría a
ejercitar la regla de negocio.

[[CAPTURA E-06: escenario negativo: datos inválidos introducidos y resultado con el mensaje de rechazo y el listado vacío.]]

### 4.5 Identificación de defectos encontrados durante la automatización

| Hallazgo | Impacto | Acción tomada |
|---|---|---|
| `Thread.sleep` producía resultados intermitentes al comprobar el mensaje | Prueba inestable (*flaky*) | Se sustituyó por `WebDriverWait` con `visibilityOfElementLocated` y `textToBe`. |
| El primer diseño usaba `type="number"` y `type="email"` en el formulario | El navegador bloqueaba el envío y el escenario negativo no llegaba al servidor | Se cambiaron a `type="text"` para que la validación sea responsabilidad del servidor. |
| El puerto fijo 8080 provocaba fallos si ya estaba ocupado | Prueba no reproducible | El servidor de prueba se inicia en el puerto `0` y se lee el puerto real asignado. |
| Navegador con estado entre pruebas | Contaminación de datos entre escenarios | `navegador.quit()` en `@AfterEach` y servidor con repositorio nuevo por escenario. |

---

## 5. Pruebas unitarias con JUnit Jupiter

### 5.1 Objeto de prueba

`ValidadorSolicitud` es una clase pura: no accede a la red ni al repositorio, por lo que
las pruebas no necesitan sustituir dependencias (no hay *mocks*). Cada prueba crea una
instancia nueva en `@BeforeEach`, garantizando el aislamiento.

### 5.2 Métodos `@Test` implementados (12 en total)

| # | Método | Tipo de caso | Aserción principal |
|---|---|---|---|
| 1 | `solicitudConDatosValidosNoGeneraErrores` | Válido | `assertEquals(0, errores.size())` |
| 2 | `montoExactamenteEnElMinimoEsValido` | Límite | `assertEquals(0, errores.size())` |
| 3 | `montoExactamenteEnElMaximoEsValido` | Límite | `assertEquals(0, errores.size())` |
| 4 | `montoJustoPorDebajoDelMinimoEsRechazado` | Límite / inválido | `assertEquals(1, …)` + `assertTrue(startsWith(...))` |
| 5 | `nombreConLongitudMinimaSeAcepta` | Límite | `assertTrue(esValida(...))` |
| 6 | `comisionCambiaEnElUmbralDeMil` | Límite | `assertEquals(…, …, delta)` |
| 7 | `montoPorEncimaDelMaximoEsRechazado` | Inválido | `assertTrue(contains("7500.00"))` |
| 8 | `correoConFormatoInvalidoEsRechazado` | Inválido | `assertEquals("El correo…", errores.get(0))` |
| 9 | `nombreVacioEsRechazado` | Inválido | `assertEquals("El nombre es obligatorio.", …)` |
| 10 | `montoNoNumericoEsRechazado` | Inválido | `assertEquals("El monto es obligatorio…", …)` |
| 11 | `solicitudNulaLanzaExcepcion` | Inválido | `assertThrows(IllegalArgumentException.class, …)` |
| 12 | `comisionFueraDeRangoLanzaExcepcion` | Inválido | `assertThrows(IllegalArgumentException.class, …)` |

### 5.3 Fragmentos de código representativos

Caso **válido**:

```java
@Test
void solicitudConDatosValidosNoGeneraErrores() {
    Solicitud solicitud = new Solicitud("Ana Perez", "ana.perez@ejemplo.com", 1500.00);

    List<String> errores = validador.validar(solicitud);

    assertEquals(0, errores.size(), "No deberia reportar errores para datos validos");
    assertTrue(validador.esValida(solicitud), "La solicitud debe considerarse valida");
}
```

Casos **límite** (el límite es inclusivo, y el cambio de comisión ocurre exactamente en el umbral):

```java
@Test
void montoExactamenteEnElMinimoEsValido() {
    Solicitud solicitud = new Solicitud("Luis Gomez", "luis@ejemplo.com",
            ValidadorSolicitud.MONTO_MINIMO);          // 100.00
    assertEquals(0, validador.validar(solicitud).size(), "El limite inferior es inclusivo");
}

@Test
void montoJustoPorDebajoDelMinimoEsRechazado() {
    Solicitud solicitud = new Solicitud("Luis Gomez", "luis@ejemplo.com",
            ValidadorSolicitud.MONTO_MINIMO - 0.01);   // 99.99
    List<String> errores = validador.validar(solicitud);
    assertEquals(1, errores.size(), "Debe reportar exactamente un error de monto");
    assertTrue(errores.get(0).startsWith("El monto debe estar entre"));
}

@Test
void comisionCambiaEnElUmbralDeMil() {
    assertEquals(14.99985, validador.calcularComision(999.99), 0.00001);
    assertEquals(10.00000, validador.calcularComision(1000.00), 0.00001);
}
```

Casos **inválidos** con `assertThrows`:

```java
@Test
void solicitudNulaLanzaExcepcion() {
    IllegalArgumentException excepcion = assertThrows(IllegalArgumentException.class,
            () -> validador.validar(null));
    assertEquals("La solicitud no puede ser nula", excepcion.getMessage());
}
```

### 5.4 Ejecución y reporte

```
mvn -Dtest=ValidadorSolicitudTest test
```

Surefire genera un reporte XML por clase en `target/surefire-reports/` (o en la carpeta
indicada con `-Dsurefire.reports.dir`), que es el mismo formato que Jenkins consume para
construir la vista *Test Result*.

[[CAPTURA E-07: código de los tres métodos representativos de JUnit (válido, límite e inválido) junto con la salida de `mvn -Dtest=ValidadorSolicitudTest test` mostrando «Tests run: 12, Failures: 0, Errors: 0, Skipped: 0».]]

---

## 6. Pipeline de Jenkins

### 6.1 Flujo de integración

```
Checkout  →  Build  →  Unit tests (12 JUnit)  →  UI tests (2 Selenium)  →  Reportes y evidencias
```

| Etapa | Acción | Resultado esperado |
|---|---|---|
| `Codigo` | `checkout scm` | Código fuente descargado del repositorio |
| `Compilar` | `mvn -B clean package -DskipTests` | Proyecto compilado y empaquetado |
| `JUnit` | `mvn -B -Dtest=ValidadorSolicitudTest -Dsurefire.reports.dir=surefire-reports-junit test` | 12 pruebas unitarias ejecutadas |
| `Selenium` | `mvn -B -Dtest=FlujoWebSeleniumTest -Dsurefire.reports.dir=surefire-reports-selenium test` | 2 escenarios de interfaz ejecutados |
| `Evidencias` | `archiveArtifacts` de los XML y de `target/screenshots/**/*.png` | Reportes y capturas adjuntos al build |
| `post { always }` | `junit testResults: 'target/surefire-reports-*/**/*.xml'` | Reporte consolidado aprobado/fallido |

### 6.2 Jenkinsfile entregado

```groovy
pipeline {
    agent any
    options { timestamps(); skipDefaultCheckout(true); disableConcurrentBuilds() }

    stages {
        stage('Codigo')   { steps { checkout scm } }
        stage('Compilar') { steps { mvn('clean package -DskipTests') } }
        stage('JUnit') {
            steps { mvn('-Dtest=ValidadorSolicitudTest -Dsurefire.reports.dir=surefire-reports-junit test') }
        }
        stage('Selenium') {
            steps { mvn('-Dtest=FlujoWebSeleniumTest -Dsurefire.reports.dir=surefire-reports-selenium test') }
        }
        stage('Evidencias') {
            steps {
                archiveArtifacts artifacts: 'target/surefire-reports-*/*.xml', allowEmptyArchive: true, fingerprint: true
                archiveArtifacts artifacts: 'target/screenshots/**/*.png', allowEmptyArchive: true
            }
        }
    }

    post {
        always {
            junit testResults: 'target/surefire-reports-*/**/*.xml', allowEmptyResults: true
        }
    }
}

def mvn(String argumentos) {
    if (isUnix()) { sh "mvn -B ${argumentos}" } else { bat "mvn -B ${argumentos}" }
}
```

**Adaptación para Windows:** la función `mvn` del final del archivo detecta el sistema
operativo del agente y usa `bat` en Windows y `sh` en Linux/macOS, por lo que el mismo
`Jenkinsfile` funciona en ambos entornos (la plantilla de referencia de la actividad usaba
`sh` de forma fija).

**Separación de reportes:** cada suite escribe sus reportes XML en una carpeta distinta
gracias a la propiedad `surefire.reports.dir` definida en el `pom.xml`. Sin esta
separación, la segunda ejecución de Maven sobrescribiría los reportes de la primera y el
build de Jenkins solo mostraría una de las dos suites.

### 6.3 Configuración del trabajo

1. *Administrar Jenkins → Tools*: declarar un JDK 17+ y un Maven 3.9+.
2. *Nueva tarea → Pipeline*: `Pipeline script from SCM`, SCM `Git` apuntando al
   repositorio del proyecto y `Script Path` = `Jenkinsfile`.
3. Asegurar que el agente tenga instalado **Google Chrome** (las pruebas corren en modo
   headless, por lo que no se necesita entorno gráfico).

[[CAPTURA E-08: archivo `Jenkinsfile` en el proyecto y pantalla de configuración del trabajo en Jenkins (definición del pipeline, repositorio y script path).]]
[[CAPTURA E-09: vista *Stage View* / *Console Output* del build con las etapas ejecutadas y la salida de ambas suites («Tests run: …»).]]
[[CAPTURA E-10: reporte *Test Result* de Jenkins con las pruebas aprobadas/fallidas y la lista de artefactos archivados.]]

---

## 7. Análisis crítico: decisiones, errores y limitaciones

### 7.1 Decisiones de diseño

- **Proyecto propio en lugar de uno de terceros.** Se gana control total sobre la regla de
  negocio y se elimina el riesgo de que la interfaz de un proyecto ajeno cambie y rompa los
  localizadores. A cambio, se pierde la validación frente a código «del mundo real».
- **Regla de negocio pura.** Al no depender del repositorio ni de la web, las pruebas
  unitarias se ejecutan en milisegundos y son totalmente deterministas, lo que las hace
  adecuadas para ejecutarse en cada *commit*.
- **Un servidor por escenario, en puerto libre.** Se gana aislamiento y se pierde,
  mínimamente, tiempo de arranque (despreciable frente a la apertura del navegador).
- **Carpetas de reporte separadas por suite.** Permite que Jenkins muestre la
  contribución de cada suite sin mezclar resultados.

### 7.2 Errores y dificultades encontradas

| # | Síntoma | Causa raíz | Solución aplicada |
|---|---|---|---|
| 1 | La verificación del mensaje fallaba de forma intermitente | Uso de `Thread.sleep` insuficiente frente a los tiempos del navegador | Esperas explícitas con `WebDriverWait` (`visibilityOfElementLocated` y `textToBe`) |
| 2 | El escenario negativo nunca llegaba al servidor | El formulario usaba `type="number"`/`type="email"` y el navegador bloqueaba el envío | Cambio a `type="text"` sin `required`, delegando la validación al servidor |
| 3 | `Address already in use` al repetir las pruebas | Puerto fijo 8080 | Arranque del servidor en el puerto `0` y lectura del puerto real |
| 4 | El build de Jenkins solo mostraba una suite | La segunda ejecución de Maven borraba los XML de la primera | Propiedad `surefire.reports.dir` distinta por etapa |
| 5 | El proceso de Maven no terminaba tras las pruebas | Hilos del ejecutor HTTP no *daemon* | Hilos del *pool* marcados como *daemon* y `shutdownNow()` en `detener()` |
| 6 | Datos de la prueba visible tras reiniciar | Estado estático compartido entre pruebas | Repositorio instanciado por servidor y servidor nuevo por escenario |

### 7.3 Limitaciones reconocidas

1. **Sin validación cruzada de navegadores.** Solo se ejecuta sobre Chrome; no se probó en
   Firefox, Edge ni Safari, por lo que no puede afirmarse compatibilidad multiplataforma.
2. **Sin persistencia real.** El repositorio es en memoria: al reiniciar la aplicación se
   pierden los datos. Esto simplifica las pruebas, pero aleja el escenario de un sistema en
   producción.
3. **Sin pruebas de carga, estrés ni concurrencia.** El objetivo era funcional; no se midió
   el rendimiento ni la consistencia bajo accesos simultáneos.
4. **Cobertura parcial.** Se cubre la validación y el flujo de registro, pero no todos los
   caminos del código (por ejemplo, la ruta 404 o los errores de formato en la lectura del
   formulario). No se configuró un umbral mínimo de cobertura con JaCoCo.
5. **Capturas como evidencia visual.** Una captura sin su aserción correspondiente no
   demuestra la aprobación; por eso cada captura de los escenarios va acompañada del
   fragmento de código con su `assert` y de la salida de la consola.
6. **Dependencia de Chrome y de la red la primera vez.** Selenium Manager requiere conexión
   a Internet para resolver y descargar el `chromedriver` si no está en caché.

### 7.4 Mejoras propuestas

- Añadir **JaCoCo** y exigir un mínimo de cobertura en el pipeline.
- Ejecutar la suite de Selenium en **matriz de navegadores** con Selenium Grid o Docker.
- Incorporar **capturas automáticas al fallo** (`WebDriverListener` / `TestWatcher`) además
  de las capturas de evidencia en pasos concretos.
- Migrar los datos a una base de datos embebida y añadir pruebas de integración de la capa
  de persistencia.
- Añadir un *gate* de calidad con SonarQube o Checkstyle en la etapa de compilación.

### 7.5 Conclusión

La práctica permitió comprobar que un proceso de QA automatizado reproducible requiere algo
más que «escribir pruebas»: exige aislar la lógica de los efectos secundarios, sincronizar
correctamente con la interfaz, separar las suites para que sus resultados sean auditables y
publicar las evidencias de forma automática. El pipeline construido ejecuta las dos suites
en cada build y deja en Jenkins el resultado por suite y las capturas correspondientes, que
es exactamente el criterio de trazabilidad exigido por la actividad.

---

## 8. Referencias

1. Selenium. *Waiting Strategies / Waits*. Selenium Documentation.
   <https://www.selenium.dev/documentation/webdriver/waits/>
2. JUnit Team. *JUnit 5 User Guide*. <https://junit.org/junit5/docs/current/user-guide/>
3. Jenkins Project. *Pipeline — Jenkinsfile*. <https://www.jenkins.io/doc/book/pipeline/jenkinsfile/>
4. Jenkins Project. *Build a Java app with Maven*. <https://www.jenkins.io/doc/tutorials/build-a-java-app-with-maven/>
5. Apache Maven. *Maven Surefire Plugin*. <https://maven.apache.org/surefire/maven-surefire-plugin/>
6. Selenium. *Selenium Manager*. <https://www.selenium.dev/documentation/selenium_manager/>

---

## Anexo A — Guía de evidencias E-01 … E-10

| Evidencia | Qué debe mostrar | Dónde obtenerla | Explicación técnica a redactar (2–4 líneas) |
|---|---|---|---|
| **E-01** | Proyecto original en el IDE/repositorio **y** la aplicación en ejecución | IDE + `java -jar target/sistema-solicitudes-qa.jar` con el navegador abierto | Describir el propósito del proyecto, su tecnología y cómo se inicia; señalar que es un proyecto propio bajo licencia MIT. |
| **E-02** | Código Selenium: localizadores, datos, acciones y aserción | `FlujoWebSeleniumTest.java` | Explicar por qué se usan `By.id` y selectores CSS, qué datos se introducen y qué aserciones verifican el resultado. |
| **E-03** | Página al inicio del escenario y estado previo de los datos | `target/screenshots/S-01_01_pagina_inicial.png` / `S-02_01_…` | Indicar que la precondición es un repositorio vacío y que el contador en `0` lo confirma. |
| **E-04** | Interacción automatizada: campos completados y acción ejecutada | `target/screenshots/S-01_02_formulario_completado.png` | Señalar los datos de entrada y que el envío se realiza con un clic automatizado sobre `#btn-registrar`. |
| **E-05** | Resultado visible del escenario positivo y comprobación automatizada | `S-01_03_confirmacion.png`, `S-01_04_listado_con_registro.png` + consola | Relacionar el mensaje de confirmación con la aserción que lo verifica y con la fila creada en el listado. |
| **E-06** | Escenario negativo y su resultado/validación | `S-02_02_datos_invalidos.png`, `S-02_03_rechazo.png` + consola | Explicar que se omitió el nombre y se usó un monto fuera de rango, y que la aserción comprueba el rechazo **y** la ausencia de registros. |
| **E-07** | Código de los 3 métodos JUnit y resultado de ejecución | `ValidadorSolicitudTest.java` + `mvn -Dtest=ValidadorSolicitudTest test` | Mostrar un caso válido, uno límite y uno inválido, y el resumen de Surefire con `Failures: 0`. |
| **E-08** | `Jenkinsfile` en el proyecto y configuración del trabajo | Repositorio + Jenkins | Describir las etapas del pipeline y la configuración del trabajo (Pipeline script from SCM). |
| **E-09** | Pipeline ejecutado y consola/log de ambas suites | *Stage View* / *Console Output* | Identificar cada etapa y localizar en el log el número de pruebas ejecutadas por suite. |
| **E-10** | Reportes finales en Jenkins (aprobadas/fallidas) | *Test Result* + artefactos | Mostrar el total de pruebas aprobadas y los artefactos (XML y capturas) archivados por el build. |

## Anexo B — Comandos de referencia

```powershell
# Compilar y empaquetar
mvn clean package -DskipTests

# Ejecutar la aplicacion
java -jar target/sistema-solicitudes-qa.jar

# Suite completa
mvn test

# Solo unitarias
mvn -Dtest=ValidadorSolicitudTest test

# Solo interfaz (Selenium)
mvn -Dtest=FlujoWebSeleniumTest test

# Simular localmente lo que hace cada etapa de Jenkins
mvn -B -Dtest=ValidadorSolicitudTest -Dsurefire.reports.dir=surefire-reports-junit test
mvn -B -Dtest=FlujoWebSeleniumTest -Dsurefire.reports.dir=surefire-reports-selenium test
```
