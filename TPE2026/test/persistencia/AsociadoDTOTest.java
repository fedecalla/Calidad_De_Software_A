package persistencia;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class AsociadoDTOTest {

    private final AsociadoDTO entidad = new AsociadoDTO();

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

    @Test
    public void testGetApellido() {
        entidad.setApellido("Rodriguez");
        assertEquals("Rodriguez", entidad.getApellido());
    }

    @Test(expected = AssertionError.class)
    public void testSetApellidoInvalido() {
        entidad.setApellido(null);
    }

    @Test
    public void testGetDni() {
        entidad.setDni("30123456");
        assertEquals("30123456", entidad.getDni());
    }

    @Test(expected = AssertionError.class)
    public void testSetDniInvalido() {
        entidad.setDni(null);
    }

    @Test
    public void testGetCiudad() {
        entidad.setCiudad("Córdoba");
        assertEquals("Córdoba", entidad.getCiudad());
    }

    @Test(expected = AssertionError.class)
    public void testSetCiudadInvalido() {
        entidad.setCiudad(null);
    }

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
    public void testGetNumero() {
        entidad.setNumero(1234);
        assertEquals(1234, entidad.getNumero());
    }

    @Test(expected = AssertionError.class)
    public void testSetNumeroInvalido() {
        entidad.setNumero(0);
    }

    @Test
    public void testGetTelefono() {
        entidad.setTelefono("3511234567");
        assertEquals("3511234567", entidad.getTelefono());
    }

    @Test(expected = AssertionError.class)
    public void testSetTelefonoInvalido() {
        entidad.setTelefono(null);
    }

}
