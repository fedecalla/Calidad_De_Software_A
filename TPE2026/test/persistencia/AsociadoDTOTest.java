package persistencia;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Suite unitaria de AsociadoDTO (Consigna 1).
 * Se ejecuta de forma aislada: no requiere base de datos ni otros servicios.
 * Las validaciones del DTO son assert de Java, por lo que la JVM debe correr con -ea.
 */
public class AsociadoDTOTest {

    private final AsociadoDTO entidad = new AsociadoDTO();

    @BeforeClass
    public static void verificarAsercionesHabilitadas() {
        assertTrue("Las aserciones estan deshabilitadas: ejecutar la JVM con -ea",
                AsociadoDTO.class.desiredAssertionStatus());
    }

    /** Ejecuta la accion y falla si NO se lanza AssertionError. */
    private static void assertRechaza(String mensaje, Runnable accion) {
        try {
            accion.run();
        } catch (AssertionError e) {
            return;
        }
        fail(mensaje);
    }

    // CONSTRUCTORES

    @Test
    public void testConstructorPorDefecto() {
        AsociadoDTO dto = new AsociadoDTO();
        assertNull(dto.getNombre());
        assertNull(dto.getApellido());
        assertNull(dto.getDni());
        assertNull(dto.getCiudad());
        assertNull(dto.getCalle());
        assertEquals(0, dto.getNumero());
        assertNull(dto.getTelefono());
    }

    @Test
    public void testConstructorParametrizadoValido() {
        AsociadoDTO dto = new AsociadoDTO("Juan", "State", "12345678", "Mar del Plata", "Buenos Aires", 123, "2235551234");
        assertEquals("Juan", dto.getNombre());
        assertEquals("State", dto.getApellido());
        assertEquals("12345678", dto.getDni());
        assertEquals("Mar del Plata", dto.getCiudad());
        assertEquals("Buenos Aires", dto.getCalle());
        assertEquals(123, dto.getNumero());
        assertEquals("2235551234", dto.getTelefono());
    }

    @Test
    public void testConstructorParametrizadoNumeroLimiteCero() {
        AsociadoDTO dto = new AsociadoDTO("Juan", "State", "12345678", "Mar del Plata", "Buenos Aires", 0, "2235551234");
        assertEquals(0, dto.getNumero());
    }

    // El constructor parametrizado debe respetar las mismas reglas del SRS que los setters.

    @Test
    public void testConstructorParametrizadoNombreNulo() {
        assertRechaza("El constructor acepto nombre nulo",
                () -> new AsociadoDTO(null, "State", "12345678", "Mar del Plata", "Buenos Aires", 123, "2235551234"));
    }

    @Test
    public void testConstructorParametrizadoApellidoNulo() {
        assertRechaza("El constructor acepto apellido nulo",
                () -> new AsociadoDTO("Juan", null, "12345678", "Mar del Plata", "Buenos Aires", 123, "2235551234"));
    }

    @Test
    public void testConstructorParametrizadoDniNulo() {
        assertRechaza("El constructor acepto dni nulo",
                () -> new AsociadoDTO("Juan", "State", null, "Mar del Plata", "Buenos Aires", 123, "2235551234"));
    }

    @Test
    public void testConstructorParametrizadoCiudadNula() {
        assertRechaza("El constructor acepto ciudad nula",
                () -> new AsociadoDTO("Juan", "State", "12345678", null, "Buenos Aires", 123, "2235551234"));
    }

    @Test
    public void testConstructorParametrizadoCalleNula() {
        assertRechaza("El constructor acepto calle nula",
                () -> new AsociadoDTO("Juan", "State", "12345678", "Mar del Plata", null, 123, "2235551234"));
    }

    @Test
    public void testConstructorParametrizadoNumeroNegativo() {
        assertRechaza("El constructor acepto numero negativo",
                () -> new AsociadoDTO("Juan", "State", "12345678", "Mar del Plata", "Buenos Aires", -1, "2235551234"));
    }

    @Test
    public void testConstructorParametrizadoTelefonoNulo() {
        assertRechaza("El constructor acepto telefono nulo",
                () -> new AsociadoDTO("Juan", "State", "12345678", "Mar del Plata", "Buenos Aires", 123, null));
    }

    
    // NOMBRE
    
    @Test
    public void testGetNombre() {
        entidad.setNombre("Federico");
        assertEquals("Federico", entidad.getNombre());
    }

    @Test(expected = AssertionError.class)
    public void testSetNombreInvalido() {
        entidad.setNombre(null);
    }

    @Test
    public void testSetNombreValido() {
        entidad.setNombre("Pedro");
        assertEquals("Pedro", entidad.getNombre());
    }
    
    
    // APELLIDO

    @Test
    public void testGetApellido() {
        entidad.setApellido("Perez");
        assertEquals("Perez", entidad.getApellido());
    }

    @Test(expected = AssertionError.class)
    public void testSetApellidoInvalido() {
        entidad.setApellido(null);
    }
    
    @Test
    public void testSetApellidoValido() {
        entidad.setApellido("Perez");
        assertEquals("Perez", entidad.getApellido());
    }
    
    // DNI

    @Test
    public void testGetDni() {
        entidad.setDni("12345678");
        assertEquals("12345678", entidad.getDni());
    }

    @Test(expected = AssertionError.class)
    public void testSetDniInvalido() {
        entidad.setDni(null);
    }
    
    @Test
    public void testSetDniValido() {
        entidad.setDni("12345678");
        assertEquals("12345678", entidad.getDni());
    }
    
    
    // CIUDAD

    @Test
    public void testGetCiudad() {
        entidad.setCiudad("Mar del Plata");
        assertEquals("Mar del Plata", entidad.getCiudad());
    }

    @Test(expected = AssertionError.class)
    public void testSetCiudadInvalido() {
        entidad.setCiudad(null);
    }
    
    @Test
    public void testSetCiudadValido() {
        entidad.setCiudad("Mar del Plata");
        assertEquals("Mar del Plata", entidad.getCiudad());
    }
    
    // CALLE

    @Test
    public void testGetCalle() {
        entidad.setCalle("San Martín");
        assertEquals("San Martín", entidad.getCalle());
    }

    @Test(expected = AssertionError.class)
    public void testSetCalleInvalido() {
        entidad.setCalle(null);
    }

    @Test
    public void testSetCalleValido() {
        entidad.setCalle("San Martín");
        assertEquals("San Martín", entidad.getCalle());
    }
    
    // NUMERO
    
    @Test
    public void testGetNumero() {
        entidad.setNumero(1234);
        assertEquals(1234, entidad.getNumero());
    }

    @Test(expected = AssertionError.class)
    public void testSetNumeroInvalido() {
        entidad.setNumero(-1);
    }
    
    @Test
    public void testSetNumeroValido() {
        entidad.setNumero(0);
        assertEquals(0, entidad.getNumero());
    }

    @Test(expected = AssertionError.class)
    public void testSetNumeroLimiteMinimoEntero() {
        entidad.setNumero(Integer.MIN_VALUE);
    }

    @Test
    public void testSetNumeroLimiteUno() {
        entidad.setNumero(1);
        assertEquals(1, entidad.getNumero());
    }

    @Test
    public void testSetNumeroLimiteMaximoEntero() {
        entidad.setNumero(Integer.MAX_VALUE);
        assertEquals(Integer.MAX_VALUE, entidad.getNumero());
    }

    @Test
    public void testSetNumeroInvalidoNoModificaValorPrevio() {
        entidad.setNumero(10);
        try {
            entidad.setNumero(-1);
        } catch (AssertionError e) {
            // esperado
        }
        assertEquals(10, entidad.getNumero());
    }
    
    // TELEFONO

    @Test
    public void testGetTelefono() {
        entidad.setTelefono("223 457 9863");
        assertEquals("223 457 9863", entidad.getTelefono());
    }

    @Test(expected = AssertionError.class)
    public void testSetTelefonoInvalido() {
        entidad.setTelefono(null);
    }
    
    @Test
    public void testSetTelefonoValido() {
        entidad.setTelefono("223 457 9863");
        assertEquals("223 457 9863", entidad.getTelefono());
    }

    // STRINGS: el SRS solo exige "no nulo", por lo que la cadena vacia pertenece a la clase valida

    @Test
    public void testSettersAceptanCadenaVacia() {
        entidad.setNombre("");
        entidad.setApellido("");
        entidad.setDni("");
        entidad.setCiudad("");
        entidad.setCalle("");
        entidad.setTelefono("");
        assertEquals("", entidad.getNombre());
        assertEquals("", entidad.getApellido());
        assertEquals("", entidad.getDni());
        assertEquals("", entidad.getCiudad());
        assertEquals("", entidad.getCalle());
        assertEquals("", entidad.getTelefono());
    }

    @Test
    public void testSetNombreNuloNoModificaValorPrevio() {
        entidad.setNombre("Federico");
        try {
            entidad.setNombre(null);
        } catch (AssertionError e) {
            // esperado
        }
        assertEquals("Federico", entidad.getNombre());
    }

    // SERIALIZACION

    @Test
    public void testSerializacionConservaLosDatos() throws Exception {
        AsociadoDTO original = new AsociadoDTO("Juan", "State", "12345678", "Mar del Plata", "Buenos Aires", 123, "2235551234");

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bytes)) {
            out.writeObject(original);
        }
        AsociadoDTO copia;
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
            copia = (AsociadoDTO) in.readObject();
        }

        assertNotSame(original, copia);
        assertEquals(original.getNombre(), copia.getNombre());
        assertEquals(original.getApellido(), copia.getApellido());
        assertEquals(original.getDni(), copia.getDni());
        assertEquals(original.getCiudad(), copia.getCiudad());
        assertEquals(original.getCalle(), copia.getCalle());
        assertEquals(original.getNumero(), copia.getNumero());
        assertEquals(original.getTelefono(), copia.getTelefono());
    }

}
