package com.utesa.calidad.servicio;

import com.utesa.calidad.modelo.ResultadoRegistro;
import com.utesa.calidad.modelo.Solicitud;
import com.utesa.calidad.repositorio.SolicitudRepositorio;

import java.util.List;

/**
 * Orquesta la regla de negocio ({@link ValidadorSolicitud}) y la persistencia
 * ({@link SolicitudRepositorio}). Es la unica capa que conoce a ambas.
 */
public class SolicitudServicio {

    private final ValidadorSolicitud validador;
    private final SolicitudRepositorio repositorio;

    public SolicitudServicio(ValidadorSolicitud validador, SolicitudRepositorio repositorio) {
        this.validador = validador;
        this.repositorio = repositorio;
    }

    /** Crea un servicio con sus dependencias por defecto (repositorio vacio en memoria). */
    public static SolicitudServicio crearPorDefecto() {
        return new SolicitudServicio(new ValidadorSolicitud(), new SolicitudRepositorio());
    }

    /**
     * Intenta registrar la solicitud. Si la validacion falla, no se persiste
     * ningun dato y se devuelve el detalle de los errores.
     */
    public ResultadoRegistro registrar(Solicitud solicitud) {
        List<String> errores = validador.validar(solicitud);

        if (!errores.isEmpty()) {
            return new ResultadoRegistro(false,
                    "No se pudo registrar la solicitud. " + String.join(" ", errores));
        }

        repositorio.guardar(solicitud);
        return new ResultadoRegistro(true,
                "Solicitud registrada correctamente para " + solicitud.getNombre() + ".");
    }

    public List<Solicitud> listar() {
        return repositorio.listar();
    }

    public int cantidad() {
        return repositorio.cantidad();
    }
}
