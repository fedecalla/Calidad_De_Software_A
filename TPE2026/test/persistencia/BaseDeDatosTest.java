package persistencia;

import static org.junit.Assert.*;

import org.junit.Test;

public class BaseDeDatosTest {

	@Test
	public void testGetInstancia() {
		BaseDeDatos instancia1 = BaseDeDatos.getInstancia();
		BaseDeDatos instancia2 = BaseDeDatos.getInstancia();
		
		assertSame(instancia1, instancia2);
	}
	@Test
	public void testCerrarConexion() {
		fail("Not yet implemented");
	}

	@Test
	public void testGetConexion() {
		fail("Not yet implemented");
	}

	@Test
	public void testEjecutarConsulta() {
		fail("Not yet implemented");
	}

	@Test
	public void testEjecutarActualizacion() {
		fail("Not yet implemented");
	}

	@Test
	public void testEjecutarInsert() {
		fail("Not yet implemented");
	}

	@Test
	public void testEjecutarDelete() {
		fail("Not yet implemented");
	}

	@Test
	public void testIniciarBD() {
		fail("Not yet implemented");
	}

}
