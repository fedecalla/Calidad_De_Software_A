package persistencia;

import static org.junit.Assert.*;

import org.junit.Test;

public class AsociadoDTOTest {
	
	private AsociadoDTO entidad = new AsociadoDTO();

	//Habria que probar los constructores ?
	
	@Test
	public void testGetNombre() {
		entidad.setNombre("Federico");
		assertEquals("Federico", entidad.getNombre());
	}

	@Test(expected = AssertionError.class)
	public void testSetNombre_invalido() {
		entidad.setNombre(null);
	}
	
	@Test
	public void testSetNombre_valido() {
		entidad.setNombre("Pedro");
		assertEquals("Pedro", entidad.getNombre());
	}

	@Test
	public void testGetApellido() {
		fail("Not yet implemented");
	}

	@Test
	public void testSetApellido() {
		fail("Not yet implemented");
	}

	@Test
	public void testGetDni() {
		fail("Not yet implemented");
	}

	@Test
	public void testSetDni() {
		fail("Not yet implemented");
	}

	@Test
	public void testGetCiudad() {
		fail("Not yet implemented");
	}

	@Test
	public void testSetCiudad() {
		fail("Not yet implemented");
	}

	@Test
	public void testGetCalle() {
		fail("Not yet implemented");
	}

	@Test
	public void testSetCalle() {
		fail("Not yet implemented");
	}

	@Test
	public void testGetNumero() {
		fail("Not yet implemented");
	}

	@Test
	public void testSetNumero() {
		fail("Not yet implemented");
	}

	@Test
	public void testGetTelefono() {
		fail("Not yet implemented");
	}

	@Test
	public void testSetTelefono() {
		fail("Not yet implemented");
	}

}
