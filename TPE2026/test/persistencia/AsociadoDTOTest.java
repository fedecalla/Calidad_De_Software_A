package persistencia;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class AsociadoDTOTest {

    private final AsociadoDTO entidad = new AsociadoDTO();

    
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

}
