package com.utesa.calidad;

import com.utesa.calidad.web.ServidorWeb;

import java.io.IOException;

/**
 * Punto de entrada de la aplicacion.
 * Uso: {@code java -jar target/sistema-solicitudes-qa.jar [puerto]}
 * Puerto por defecto: 8080.
 */
public final class App {

    private static final int PUERTO_POR_DEFECTO = 8080;

    private App() {
    }

    public static void main(String[] args) throws IOException {
        int puerto = args.length > 0 ? Integer.parseInt(args[0]) : PUERTO_POR_DEFECTO;

        ServidorWeb servidor = ServidorWeb.iniciar(puerto);
        Runtime.getRuntime().addShutdownHook(new Thread(servidor::detener));

        System.out.println("=====================================================");
        System.out.println(" Sistema de Solicitudes - QA Automatizado");
        System.out.println(" Aplicacion disponible en: " + servidor.getUrlBase());
        System.out.println(" Presione Ctrl+C para detener el servidor.");
        System.out.println("=====================================================");
    }
}
