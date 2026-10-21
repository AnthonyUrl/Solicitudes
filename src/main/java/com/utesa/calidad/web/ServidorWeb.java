package com.utesa.calidad.web;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import com.utesa.calidad.modelo.ResultadoRegistro;
import com.utesa.calidad.modelo.Solicitud;
import com.utesa.calidad.servicio.SolicitudServicio;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Servidor HTTP de la aplicacion, construido sobre {@code com.sun.net.httpserver}
 * (incluido en el JDK). No requiere contenedor de servlets ni framework web.
 *
 * Rutas:
 *   GET  /           formulario de registro + listado de solicitudes
 *   POST /registrar  procesa el formulario y devuelve el resultado
 */
public class ServidorWeb implements AutoCloseable {

    private static final String HOST = "127.0.0.1";

    private final HttpServer servidor;
    private final ExecutorService ejecutor;
    private final SolicitudServicio servicio;

    private ServidorWeb(HttpServer servidor, ExecutorService ejecutor, SolicitudServicio servicio) {
        this.servidor = servidor;
        this.ejecutor = ejecutor;
        this.servicio = servicio;
    }

    /**
     * Inicia el servidor en el puerto indicado. Usar {@code 0} para obtener un
     * puerto libre asignado por el sistema operativo (util en pruebas).
     */
    public static ServidorWeb iniciar(int puerto) throws IOException {
        SolicitudServicio servicio = SolicitudServicio.crearPorDefecto();
        HttpServer servidor = HttpServer.create(new InetSocketAddress(HOST, puerto), 0);

        ExecutorService ejecutor = Executors.newFixedThreadPool(4, tarea -> {
            Thread hilo = new Thread(tarea, "solicitudes-http");
            hilo.setDaemon(true);
            return hilo;
        });
        servidor.setExecutor(ejecutor);

        ServidorWeb web = new ServidorWeb(servidor, ejecutor, servicio);
        servidor.createContext("/", web::manejar);
        servidor.start();
        return web;
    }

    public int getPuerto() {
        return servidor.getAddress().getPort();
    }

    public String getUrlBase() {
        return "http://" + HOST + ":" + getPuerto();
    }

    public SolicitudServicio getServicio() {
        return servicio;
    }

    public void detener() {
        servidor.stop(0);
        ejecutor.shutdownNow();
    }

    @Override
    public void close() {
        detener();
    }

    private void manejar(HttpExchange intercambio) throws IOException {
        String ruta = intercambio.getRequestURI().getPath();
        String metodo = intercambio.getRequestMethod();

        try {
            if ("/".equals(ruta) && "GET".equals(metodo)) {
                responderHtml(intercambio, 200, Vistas.paginaPrincipal(null, false, servicio.listar()));
            } else if ("/registrar".equals(ruta) && "POST".equals(metodo)) {
                procesarRegistro(intercambio);
            } else {
                responderHtml(intercambio, 404, "<h1>404 - Recurso no encontrado</h1>");
            }
        } catch (RuntimeException excepcion) {
            responderHtml(intercambio, 500, "<h1>500 - Error interno de la aplicacion</h1>");
        } finally {
            intercambio.close();
        }
    }

    private void procesarRegistro(HttpExchange intercambio) throws IOException {
        Map<String, String> parametros = leerFormulario(intercambio);

        Solicitud solicitud = new Solicitud(
                parametros.get("nombre"),
                parametros.get("correo"),
                parsearMonto(parametros.get("monto")));

        ResultadoRegistro resultado = servicio.registrar(solicitud);

        responderHtml(intercambio, 200,
                Vistas.paginaPrincipal(resultado.getMensaje(), resultado.isExitoso(), servicio.listar()));
    }

    private Map<String, String> leerFormulario(HttpExchange intercambio) throws IOException {
        String cuerpo = new String(intercambio.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        Map<String, String> parametros = new HashMap<>();

        for (String par : cuerpo.split("&")) {
            if (par.isEmpty()) {
                continue;
            }
            String[] partes = par.split("=", 2);
            String clave = URLDecoder.decode(partes[0], StandardCharsets.UTF_8);
            String valor = partes.length > 1 ? URLDecoder.decode(partes[1], StandardCharsets.UTF_8) : "";
            parametros.put(clave, valor);
        }
        return parametros;
    }

    /** Devuelve {@code Double.NaN} cuando el monto falta o no es numerico; el validador lo rechaza. */
    private double parsearMonto(String valor) {
        if (valor == null || valor.trim().isEmpty()) {
            return Double.NaN;
        }
        try {
            return Double.parseDouble(valor.trim().replace(',', '.'));
        } catch (NumberFormatException excepcion) {
            return Double.NaN;
        }
    }

    private void responderHtml(HttpExchange intercambio, int codigo, String html) throws IOException {
        byte[] cuerpo = html.getBytes(StandardCharsets.UTF_8);
        intercambio.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
        intercambio.sendResponseHeaders(codigo, cuerpo.length);
        try (OutputStream salida = intercambio.getResponseBody()) {
            salida.write(cuerpo);
        }
    }
}
