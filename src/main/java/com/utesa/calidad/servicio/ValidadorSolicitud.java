package com.utesa.calidad.servicio;

import com.utesa.calidad.modelo.Solicitud;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Regla de negocio principal de la aplicacion: valida si una solicitud puede
 * registrarse. Es una clase pura (no accede a red ni a repositorios), lo que la
 * hace apta para pruebas unitarias aisladas y repetibles.
 *
 * Reglas:
 *  - El nombre es obligatorio y debe tener al menos 3 caracteres.
 *  - El correo es obligatorio y debe tener formato valido.
 *  - El monto es obligatorio y debe estar entre 100.00 y 5000.00 (inclusive).
 */
public class ValidadorSolicitud {

    public static final double MONTO_MINIMO = 100.00;
    public static final double MONTO_MAXIMO = 5000.00;
    public static final int LONGITUD_MINIMA_NOMBRE = 3;

    /** Umbral a partir del cual la comision baja del 1.5% al 1.0%. */
    public static final double UMBRAL_COMISION = 1000.00;

    private static final Pattern FORMATO_CORREO =
            Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    /**
     * Valida la solicitud y devuelve la lista de errores encontrados.
     * Una lista vacia significa que la solicitud es valida.
     *
     * @throws IllegalArgumentException si la solicitud es nula.
     */
    public List<String> validar(Solicitud solicitud) {
        if (solicitud == null) {
            throw new IllegalArgumentException("La solicitud no puede ser nula");
        }

        List<String> errores = new ArrayList<>();

        validarNombre(solicitud.getNombre(), errores);
        validarCorreo(solicitud.getCorreo(), errores);
        validarMonto(solicitud.getMonto(), errores);

        return List.copyOf(errores);
    }

    public boolean esValida(Solicitud solicitud) {
        return validar(solicitud).isEmpty();
    }

    /**
     * Regla derivada: calcula la comision de apertura de la solicitud.
     * 1.5% si el monto es menor al umbral y 1.0% si es igual o superior.
     *
     * @throws IllegalArgumentException si el monto esta fuera del rango permitido.
     */
    public double calcularComision(double monto) {
        if (Double.isNaN(monto) || monto < MONTO_MINIMO || monto > MONTO_MAXIMO) {
            throw new IllegalArgumentException("El monto " + monto + " esta fuera del rango permitido");
        }
        double tasa = monto < UMBRAL_COMISION ? 0.015 : 0.010;
        return monto * tasa;
    }

    private void validarNombre(String nombre, List<String> errores) {
        if (nombre == null || nombre.trim().isEmpty()) {
            errores.add("El nombre es obligatorio.");
        } else if (nombre.trim().length() < LONGITUD_MINIMA_NOMBRE) {
            errores.add("El nombre debe tener al menos " + LONGITUD_MINIMA_NOMBRE + " caracteres.");
        }
    }

    private void validarCorreo(String correo, List<String> errores) {
        if (correo == null || correo.trim().isEmpty()) {
            errores.add("El correo electronico es obligatorio.");
        } else if (!FORMATO_CORREO.matcher(correo.trim()).matches()) {
            errores.add("El correo electronico no tiene un formato valido.");
        }
    }

    private void validarMonto(double monto, List<String> errores) {
        if (Double.isNaN(monto)) {
            errores.add("El monto es obligatorio y debe ser numerico.");
        } else if (monto < MONTO_MINIMO || monto > MONTO_MAXIMO) {
            errores.add(String.format(Locale.US,
                    "El monto debe estar entre %.2f y %.2f (recibido: %.2f).",
                    MONTO_MINIMO, MONTO_MAXIMO, monto));
        }
    }
}
