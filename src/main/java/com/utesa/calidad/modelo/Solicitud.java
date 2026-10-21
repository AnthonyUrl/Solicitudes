package com.utesa.calidad.modelo;

/**
 * Representa una solicitud de financiamiento enviada desde el formulario web.
 * Es un objeto inmutable y sin dependencias externas, por lo que puede usarse
 * tanto en la capa web como en las pruebas unitarias sin necesidad de red.
 */
public class Solicitud {

    private final String nombre;
    private final String correo;
    private final double monto;

    public Solicitud(String nombre, String correo, double monto) {
        this.nombre = nombre;
        this.correo = correo;
        this.monto = monto;
    }

    public String getNombre() {
        return nombre;
    }

    public String getCorreo() {
        return correo;
    }

    public double getMonto() {
        return monto;
    }

    @Override
    public String toString() {
        return "Solicitud{nombre='" + nombre + "', correo='" + correo + "', monto=" + monto + '}';
    }
}
