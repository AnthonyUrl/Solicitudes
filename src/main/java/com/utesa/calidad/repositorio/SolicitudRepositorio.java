package com.utesa.calidad.repositorio;

import com.utesa.calidad.modelo.Solicitud;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Repositorio en memoria de solicitudes.
 * Se mantiene deliberadamente simple (sin base de datos ni red) para que la
 * aplicacion sea autocontenida y para que las pruebas sean deterministas.
 */
public class SolicitudRepositorio {

    private final List<Solicitud> solicitudes = new CopyOnWriteArrayList<>();

    public void guardar(Solicitud solicitud) {
        solicitudes.add(solicitud);
    }

    public List<Solicitud> listar() {
        return List.copyOf(solicitudes);
    }

    public int cantidad() {
        return solicitudes.size();
    }

    public void limpiar() {
        solicitudes.clear();
    }
}
