package com.utesa.calidad.ui;

import com.utesa.calidad.web.ServidorWeb;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas de interfaz web con Selenium WebDriver sobre la aplicacion real.
 *
 * Cada prueba arranca su propio servidor (puerto libre y repositorio vacio) y
 * abre su propio navegador, por lo que los escenarios son independientes y
 * repetibles.
 *
 * Escenarios:
 *   S-01 (positivo):   registrar una solicitud valida -> mensaje de confirmacion
 *                      y la solicitud visible en el listado.
 *   S-02 (negativo):   registrar datos invalidos -> mensaje de rechazo y
 *                      ausencia total de registros.
 */
@DisplayName("Flujo web de registro de solicitudes (Selenium WebDriver)")
class FlujoWebSeleniumTest {

    private static final String CARPETA_CAPTURAS = "target/screenshots";
    private static final Duration ESPERA = Duration.ofSeconds(15);

    private ServidorWeb servidor;
    private WebDriver navegador;
    private WebDriverWait espera;
    private String urlBase;

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
        if (navegador != null) {
            navegador.quit();
        }
        if (servidor != null) {
            servidor.detener();
        }
    }

    @Test
    @DisplayName("S-01: registrar una solicitud valida muestra la confirmacion y crea el registro")
    void s01RegistroValidoMuestraConfirmacion() {
        // --- Precondicion: no existe ninguna solicitud registrada ---
        assertEquals("0", texto(By.id("contador-solicitudes")), "El listado debe iniciar vacio");
        capturar("S-01_01_pagina_inicial");

        // --- Datos de entrada (locales: no se usan datos personales reales) ---
        escribir(By.id("nombre"), "Ana Perez");
        escribir(By.id("correo"), "ana.perez@ejemplo.com");
        escribir(By.id("monto"), "1500");
        capturar("S-01_02_formulario_completado");

        // --- Accion: enviar el formulario ---
        navegador.findElement(By.id("btn-registrar")).click();

        // --- Verificacion 1: mensaje de confirmacion visible (espera explicita) ---
        WebElement mensaje = espera.until(
                ExpectedConditions.visibilityOfElementLocated(By.id("mensaje-resultado")));
        assertEquals("exito", mensaje.getAttribute("data-tipo"), "El resultado debe marcarse como exitoso");
        assertTrue(mensaje.getText().contains("Solicitud registrada correctamente"),
                "El mensaje debe confirmar el registro. Texto real: " + mensaje.getText());
        assertTrue(mensaje.getText().contains("Ana Perez"), "El mensaje debe identificar al solicitante");
        capturar("S-01_03_confirmacion");

        // --- Verificacion 2: la operacion es observable en el listado ---
        espera.until(ExpectedConditions.textToBe(By.id("contador-solicitudes"), "1"));
        List<WebElement> filas = navegador.findElements(By.cssSelector("#cuerpo-solicitudes tr"));
        assertEquals(1, filas.size(), "Debe existir exactamente una fila en el listado");
        assertTrue(filas.get(0).getText().contains("Ana Perez"), "La fila debe mostrar el nombre registrado");
        assertTrue(filas.get(0).getText().contains("ana.perez@ejemplo.com"), "La fila debe mostrar el correo");
        assertTrue(filas.get(0).getText().contains("1500.00"), "La fila debe mostrar el monto");
        capturar("S-01_04_listado_con_registro");
    }

    @Test
    @DisplayName("S-02: registrar datos invalidos muestra el rechazo y no crea ningun registro")
    void s02RegistroInvalidoMuestraErrorYNoRegistra() {
        // --- Precondicion: repositorio vacio ---
        assertEquals("0", texto(By.id("contador-solicitudes")), "El listado debe iniciar vacio");
        capturar("S-02_01_pagina_inicial");

        // --- Datos invalidos: se omite el nombre obligatorio y el monto esta fuera de rango ---
        escribir(By.id("correo"), "ana@ejemplo.com");
        escribir(By.id("monto"), "50");
        capturar("S-02_02_datos_invalidos");

        // --- Accion: enviar el formulario ---
        navegador.findElement(By.id("btn-registrar")).click();

        // --- Verificacion 1: mensaje de rechazo visible (espera explicita) ---
        WebElement mensaje = espera.until(
                ExpectedConditions.visibilityOfElementLocated(By.id("mensaje-resultado")));
        assertEquals("error", mensaje.getAttribute("data-tipo"), "El resultado debe marcarse como error");
        assertTrue(mensaje.getText().contains("No se pudo registrar la solicitud"),
                "El mensaje debe indicar el rechazo. Texto real: " + mensaje.getText());
        assertTrue(mensaje.getText().contains("El nombre es obligatorio"), "Debe informar el nombre faltante");
        assertTrue(mensaje.getText().contains("El monto debe estar entre"), "Debe informar el monto invalido");
        capturar("S-02_03_rechazo");

        // --- Verificacion 2: no se creo ningun registro ---
        assertEquals("0", texto(By.id("contador-solicitudes")), "El contador no debe incrementarse");
        assertEquals(0, navegador.findElements(By.cssSelector("#cuerpo-solicitudes tr")).size(),
                "No debe existir ninguna fila en el listado");
        assertTrue(navegador.findElement(By.id("estado-vacio")).isDisplayed(),
                "Debe mostrarse el estado de listado vacio");
    }

    // ------------------------------------------------------------------
    // Utilidades de apoyo
    // ------------------------------------------------------------------

    private void escribir(By localizador, String valor) {
        WebElement campo = espera.until(ExpectedConditions.elementToBeClickable(localizador));
        campo.clear();
        if (campo.getAttribute("value") != null && !campo.getAttribute("value").isEmpty()) {
            campo.clear();
        }
        campo.sendKeys(valor);
    }

    private String texto(By localizador) {
        return espera.until(ExpectedConditions.visibilityOfElementLocated(localizador)).getText().trim();
    }

    /** Guarda una captura de pantalla en target/screenshots (evidencia E-03..E-06). */
    private void capturar(String nombreArchivo) {
        try {
            File carpeta = new File(CARPETA_CAPTURAS);
            Files.createDirectories(carpeta.toPath());
            File origen = ((TakesScreenshot) navegador).getScreenshotAs(OutputType.FILE);
            Files.copy(origen.toPath(),
                    new File(carpeta, nombreArchivo + ".png").toPath(),
                    StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException excepcion) {
            throw new UncheckedIOException("No se pudo guardar la captura " + nombreArchivo, excepcion);
        }
    }
}
