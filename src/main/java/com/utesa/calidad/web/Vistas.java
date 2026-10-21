package com.utesa.calidad.web;

import com.utesa.calidad.modelo.Solicitud;

import java.util.List;
import java.util.Locale;

/**
 * Genera el HTML de la aplicacion. Se mantiene en Java puro (sin motor de
 * plantillas) para que el proyecto no requiera dependencias adicionales.
 */
public final class Vistas {

    private Vistas() {
    }

    /**
     * Construye la pagina principal.
     *
     * @param mensaje     texto del resultado a mostrar (o {@code null} si no hay).
     * @param exito       {@code true} si el mensaje corresponde a un registro exitoso.
     * @param solicitudes solicitudes actualmente registradas.
     */
    public static String paginaPrincipal(String mensaje, boolean exito, List<Solicitud> solicitudes) {
        String bloqueMensaje = construirMensaje(mensaje, exito);
        String filas = construirFilas(solicitudes);

        return "<!DOCTYPE html>\n"
                + "<html lang=\"es\">\n"
                + "<head>\n"
                + "  <meta charset=\"UTF-8\">\n"
                + "  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">\n"
                + "  <title>Sistema de Solicitudes | QA Automatizado</title>\n"
                + "  <style>\n"
                + "    * { box-sizing: border-box; }\n"
                + "    body { margin: 0; font-family: Segoe UI, Arial, sans-serif; background: #eef2f7; color: #1f2937; }\n"
                + "    header { background: #0f3d64; color: #ffffff; padding: 18px 28px; }\n"
                + "    header h1 { margin: 0; font-size: 20px; letter-spacing: .3px; }\n"
                + "    header p { margin: 4px 0 0; font-size: 13px; opacity: .85; }\n"
                + "    main { max-width: 980px; margin: 24px auto 48px; padding: 0 16px; display: grid; gap: 20px; }\n"
                + "    section { background: #ffffff; border-radius: 10px; padding: 20px 22px; box-shadow: 0 1px 3px rgba(15,61,100,.15); }\n"
                + "    h2 { margin: 0 0 14px; font-size: 16px; color: #0f3d64; }\n"
                + "    label { display: block; font-size: 13px; font-weight: 600; margin-bottom: 6px; }\n"
                + "    input { width: 100%; padding: 10px 12px; border: 1px solid #cbd5e1; border-radius: 6px; font-size: 14px; margin-bottom: 14px; }\n"
                + "    input:focus { outline: 2px solid #2563eb; border-color: #2563eb; }\n"
                + "    button { background: #0f3d64; color: #ffffff; border: 0; border-radius: 6px; padding: 11px 20px; font-size: 14px; font-weight: 600; cursor: pointer; }\n"
                + "    button:hover { background: #16548a; }\n"
                + "    .mensaje { border-radius: 6px; padding: 12px 14px; font-size: 14px; border-left: 5px solid; }\n"
                + "    .mensaje.exito { background: #e7f6ec; border-color: #1b8a4b; color: #14532d; }\n"
                + "    .mensaje.error { background: #fdeaea; border-color: #c0392b; color: #7f1d1d; }\n"
                + "    table { width: 100%; border-collapse: collapse; font-size: 14px; }\n"
                + "    th, td { text-align: left; padding: 10px 12px; border-bottom: 1px solid #e2e8f0; }\n"
                + "    th { background: #f1f5f9; font-size: 12px; text-transform: uppercase; letter-spacing: .4px; color: #475569; }\n"
                + "    .vacio { color: #64748b; font-size: 13px; font-style: italic; margin: 0; }\n"
                + "  </style>\n"
                + "</head>\n"
                + "<body>\n"
                + "  <header>\n"
                + "    <h1>Sistema de Solicitudes de Financiamiento</h1>\n"
                + "    <p>Proyecto base para la practica de automatizacion con Selenium, JUnit y Jenkins</p>\n"
                + "  </header>\n"
                + "  <main>\n"
                + "    <section>\n"
                + "      <h2>Nueva solicitud</h2>\n"
                + "      <form id=\"formulario-solicitud\" method=\"post\" action=\"/registrar\">\n"
                + "        <label for=\"nombre\">Nombre completo</label>\n"
                + "        <input id=\"nombre\" name=\"nombre\" type=\"text\" placeholder=\"Ej: Ana Perez\">\n"
                + "        <label for=\"correo\">Correo electronico</label>\n"
                + "        <input id=\"correo\" name=\"correo\" type=\"text\" placeholder=\"ana.perez@ejemplo.com\">\n"
                + "        <label for=\"monto\">Monto solicitado (entre 100.00 y 5000.00)</label>\n"
                + "        <input id=\"monto\" name=\"monto\" type=\"text\" placeholder=\"1500\">\n"
                + "        <button id=\"btn-registrar\" type=\"submit\">Registrar solicitud</button>\n"
                + "      </form>\n"
                + "    </section>\n"
                + "    <section id=\"seccion-resultado\">\n"
                + bloqueMensaje
                + "    </section>\n"
                + "    <section>\n"
                + "      <h2>Solicitudes registradas: <span id=\"contador-solicitudes\">" + solicitudes.size() + "</span></h2>\n"
                + "      <p id=\"estado-vacio\" class=\"vacio\"" + (solicitudes.isEmpty() ? "" : " hidden") + ">Aun no hay solicitudes registradas.</p>\n"
                + "      <table id=\"tabla-solicitudes\">\n"
                + "        <thead>\n"
                + "          <tr><th>#</th><th>Nombre</th><th>Correo</th><th>Monto</th></tr>\n"
                + "        </thead>\n"
                + "        <tbody id=\"cuerpo-solicitudes\">\n"
                + filas
                + "        </tbody>\n"
                + "      </table>\n"
                + "    </section>\n"
                + "  </main>\n"
                + "</body>\n"
                + "</html>\n";
    }

    private static String construirMensaje(String mensaje, boolean exito) {
        if (mensaje == null || mensaje.isEmpty()) {
            return "      <div id=\"mensaje-resultado\" data-tipo=\"ninguno\" class=\"mensaje\" hidden></div>\n";
        }
        String tipo = exito ? "exito" : "error";
        return "      <div id=\"mensaje-resultado\" data-tipo=\"" + tipo + "\" class=\"mensaje " + tipo + "\">"
                + escapar(mensaje) + "</div>\n";
    }

    private static String construirFilas(List<Solicitud> solicitudes) {
        StringBuilder filas = new StringBuilder();
        for (int i = 0; i < solicitudes.size(); i++) {
            Solicitud s = solicitudes.get(i);
            filas.append("          <tr>")
                    .append("<td>").append(i + 1).append("</td>")
                    .append("<td>").append(escapar(s.getNombre())).append("</td>")
                    .append("<td>").append(escapar(s.getCorreo())).append("</td>")
                    .append("<td>").append(String.format(Locale.US, "%.2f", s.getMonto())).append("</td>")
                    .append("</tr>\n");
        }
        return filas.toString();
    }

    private static String escapar(String valor) {
        if (valor == null) {
            return "";
        }
        return valor.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
