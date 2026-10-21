package com.utesa.calidad.modelo;

/**
 * Resultado de intentar registrar una solicitud.
 * {@code exitoso} indica si la regla de negocio acepto los datos y
 * {@code mensaje} es el texto que la interfaz web muestra al usuario.
 */
public class ResultadoRegistro {

    private final boolean exitoso;
    private final String mensaje;

    public ResultadoRegistro(boolean exitoso, String mensaje) {
        this.exitoso = exitoso;
        this.mensaje = mensaje;
    }

    public boolean isExitoso() {
        return exitoso;
    }

    public String getMensaje() {
        return mensaje;
    }
}
