package com.utesa.calidad.unitaria;

import com.utesa.calidad.modelo.Solicitud;
import com.utesa.calidad.servicio.ValidadorSolicitud;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas unitarias de la regla de negocio {@link ValidadorSolicitud}.
 *
 * Caracteristicas exigidas por la practica:
 *  - Se ejecutan sobre metodos reales del proyecto (no sobre Selenium).
 *  - Probar la logica aislada: el validador no usa red ni repositorio.
 *  - Son deterministas: mismos datos de entrada, mismo resultado.
 *  - Cubren los tres tipos de caso: valido, limite e invalido.
 */
@DisplayName("Regla de negocio: validacion de solicitudes de financiamiento")
class ValidadorSolicitudTest {

    private ValidadorSolicitud validador;

    @BeforeEach
    void prepararValidador() {
        // Se crea una instancia nueva antes de cada prueba para garantizar aislamiento.
        validador = new ValidadorSolicitud();
    }

    // ---------------------------------------------------------------------
    // Caso VALIDO
    // ---------------------------------------------------------------------

    @Test
    @DisplayName("Una solicitud con datos correctos no genera errores")
    void solicitudConDatosValidosNoGeneraErrores() {
        Solicitud solicitud = new Solicitud("Ana Perez", "ana.perez@ejemplo.com", 1500.00);

        List<String> errores = validador.validar(solicitud);

        assertEquals(0, errores.size(), "No deberia reportar errores para datos validos");
        assertTrue(validador.esValida(solicitud), "La solicitud debe considerarse valida");
    }

    // ---------------------------------------------------------------------
    // Casos LIMITE
    // ---------------------------------------------------------------------

    @Test
    @DisplayName("Un monto exactamente igual al minimo (100.00) es valido")
    void montoExactamenteEnElMinimoEsValido() {
        Solicitud solicitud = new Solicitud("Luis Gomez", "luis@ejemplo.com", ValidadorSolicitud.MONTO_MINIMO);

        List<String> errores = validador.validar(solicitud);

        assertEquals(0, errores.size(), "El limite inferior es inclusivo");
    }

    @Test
    @DisplayName("Un monto exactamente igual al maximo (5000.00) es valido")
    void montoExactamenteEnElMaximoEsValido() {
        Solicitud solicitud = new Solicitud("Luis Gomez", "luis@ejemplo.com", ValidadorSolicitud.MONTO_MAXIMO);

        List<String> errores = validador.validar(solicitud);

        assertEquals(0, errores.size(), "El limite superior es inclusivo");
    }

    @Test
    @DisplayName("Un monto una centesima por debajo del minimo es invalido")
    void montoJustoPorDebajoDelMinimoEsRechazado() {
        Solicitud solicitud = new Solicitud("Luis Gomez", "luis@ejemplo.com",
                ValidadorSolicitud.MONTO_MINIMO - 0.01);

        List<String> errores = validador.validar(solicitud);

        assertEquals(1, errores.size(), "Debe reportar exactamente un error de monto");
        assertTrue(errores.get(0).startsWith("El monto debe estar entre"), "Mensaje de error inesperado: " + errores);
    }

    @Test
    @DisplayName("Un nombre de exactamente 3 caracteres se acepta (limite inferior de longitud)")
    void nombreConLongitudMinimaSeAcepta() {
        Solicitud solicitud = new Solicitud("Ana", "ana@ejemplo.com", 500.00);

        assertTrue(validador.esValida(solicitud), "Tres caracteres es la longitud minima permitida");
    }

    @Test
    @DisplayName("La comision cambia de 1.5% a 1.0% justo en el umbral de 1000.00")
    void comisionCambiaEnElUmbralDeMil() {
        double debajoDelUmbral = validador.calcularComision(999.99);
        double enElUmbral = validador.calcularComision(ValidadorSolicitud.UMBRAL_COMISION);

        assertEquals(14.99985, debajoDelUmbral, 0.00001, "Debajo del umbral la tasa es 1.5%");
        assertEquals(10.00000, enElUmbral, 0.00001, "En el umbral la tasa baja a 1.0%");
    }

    // ---------------------------------------------------------------------
    // Casos INVALIDOS
    // ---------------------------------------------------------------------

    @Test
    @DisplayName("Un monto por encima del maximo es rechazado")
    void montoPorEncimaDelMaximoEsRechazado() {
        Solicitud solicitud = new Solicitud("Carlos Ruiz", "carlos@ejemplo.com", 7500.00);

        List<String> errores = validador.validar(solicitud);

        assertEquals(1, errores.size());
        assertTrue(errores.get(0).contains("7500.00"), "El mensaje debe indicar el valor recibido");
    }

    @Test
    @DisplayName("Un correo con formato invalido es rechazado")
    void correoConFormatoInvalidoEsRechazado() {
        Solicitud solicitud = new Solicitud("Carlos Ruiz", "carlos-arroba-ejemplo", 800.00);

        List<String> errores = validador.validar(solicitud);

        assertEquals(1, errores.size());
        assertEquals("El correo electronico no tiene un formato valido.", errores.get(0));
    }

    @Test
    @DisplayName("Un nombre vacio es rechazado")
    void nombreVacioEsRechazado() {
        Solicitud solicitud = new Solicitud("   ", "carlos@ejemplo.com", 800.00);

        List<String> errores = validador.validar(solicitud);

        assertEquals(1, errores.size());
        assertEquals("El nombre es obligatorio.", errores.get(0));
        assertFalse(validador.esValida(solicitud));
    }

    @Test
    @DisplayName("Un monto no numerico (NaN) es rechazado")
    void montoNoNumericoEsRechazado() {
        Solicitud solicitud = new Solicitud("Carlos Ruiz", "carlos@ejemplo.com", Double.NaN);

        List<String> errores = validador.validar(solicitud);

        assertEquals(1, errores.size());
        assertEquals("El monto es obligatorio y debe ser numerico.", errores.get(0));
    }

    @Test
    @DisplayName("Una solicitud nula lanza IllegalArgumentException")
    void solicitudNulaLanzaExcepcion() {
        IllegalArgumentException excepcion = assertThrows(IllegalArgumentException.class,
                () -> validador.validar(null));

        assertEquals("La solicitud no puede ser nula", excepcion.getMessage());
    }

    @Test
    @DisplayName("Calcular la comision de un monto fuera de rango lanza IllegalArgumentException")
    void comisionFueraDeRangoLanzaExcepcion() {
        assertThrows(IllegalArgumentException.class, () -> validador.calcularComision(50.00));
    }
}
