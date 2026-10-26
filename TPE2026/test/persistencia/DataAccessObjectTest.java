package persistencia;

import static org.junit.Assert.*;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import modelo.modeloDominio.personas.asociado.Asociado;
import modelo.modeloDominio.util.Domicilio;

/**
 * Suite de integracion AsociadoDTO + DataAccessObject + BaseDeDatos contra MySQL (Consigna 2).
 * Requiere un servidor MySQL en localhost:3306 con usuario/clave progra_c.
 * Antes de cada test se recrea la tabla Asociados con los 20 registros semilla.
 */
public class DataAccessObjectTest {

	private DataAccessObject dao;
	private Connection conexion;

	@BeforeClass
	public static void verificarServidor() {
		assertTrue("Ejecutar la JVM con -ea", DataAccessObject.class.desiredAssertionStatus());
		assertNotNull("MySQL no disponible: no se pudo abrir la conexion (localhost:3306, progra_c)",
				BaseDeDatos.getInstancia().getConexion());
	}

	@Before
	public void setUp() throws Exception {
		conexion = UtilBD.prepararBD().getConexion();
		dao = new DataAccessObject();
	}

	@AfterClass
	public static void tearDown() throws Exception {
		UtilBD.resetSingleton();
	}

	private static Asociado nuevoAsociado(String dni) {
		return new Asociado("Ana", "Prueba", dni, "2235000000", new Domicilio("Colon", 1500, "Mar del Plata"));
	}

	private static Asociado asociadoSemillaId5ConTelefono(String telefono) {
		return new Asociado("Xin", "Dao", "33445566", telefono, new Domicilio("Corrientes", 789, "Mar del Plata"));
	}

	// CONSTRUCTOR

	@Test
	public void testDataAccessObject() throws Exception {
		Field campo = DataAccessObject.class.getDeclaredField("bd");
		campo.setAccessible(true);
		assertSame("El DAO debe usar la instancia unica de BaseDeDatos", BaseDeDatos.getInstancia(), campo.get(dao));
	}

	// GUARDAR

	@Test
	public void testGuardar() throws SQLException {
		dao.guardar(nuevoAsociado("40111222"));

		assertEquals(UtilBD.CANTIDAD_SEMILLA + 1, UtilBD.contarFilas(conexion));
		AsociadoDTO fila = UtilBD.buscarPorDni(conexion, "40111222");
		assertNotNull(fila);
		assertEquals("Ana", fila.getNombre());
		assertEquals("Prueba", fila.getApellido());
		assertEquals("Colon", fila.getCalle());
		assertEquals(1500, fila.getNumero());
		assertEquals("Mar del Plata", fila.getCiudad());
		assertEquals("2235000000", fila.getTelefono());
	}

	@Test(expected = AssertionError.class)
	public void testGuardarNulo() throws SQLException {
		dao.guardar(null);
	}

	@Test(expected = SQLException.class)
	public void testGuardarDniDuplicado() throws SQLException {
		dao.guardar(nuevoAsociado("12345678"));
	}

	// CARGAR CON LIMITE (valores limite: -1, 0 | 1, 5, 20 | 21)

	@Test(expected = AssertionError.class)
	public void testCargarConLimiteNegativo() throws SQLException {
		dao.cargarConLimite(-1);
	}

	@Test(expected = AssertionError.class)
	public void testCargarConLimiteCero() throws SQLException {
		dao.cargarConLimite(0);
	}

	@Test
	public void testCargarConLimiteUno() throws SQLException {
		ArrayList<AsociadoDTO> lista = dao.cargarConLimite(1);
		assertEquals(1, lista.size());
		assertEquals("12345678", lista.get(0).getDni());
	}

	@Test
	public void testCargarConLimite() throws SQLException {
		assertEquals(5, dao.cargarConLimite(5).size());
	}

	@Test
	public void testCargarConLimiteIgualACantidadDeRegistros() throws SQLException {
		assertEquals(UtilBD.CANTIDAD_SEMILLA, dao.cargarConLimite(UtilBD.CANTIDAD_SEMILLA).size());
	}

	@Test
	public void testCargarConLimiteMayorACantidadDeRegistros() throws SQLException {
		assertEquals(UtilBD.CANTIDAD_SEMILLA, dao.cargarConLimite(UtilBD.CANTIDAD_SEMILLA + 1).size());
	}

	// CARGAR POR ID

	@Test
	public void testCargar() {
		AsociadoDTO dto = null;
		try {
			dto = dao.cargar(1);
		} catch (SQLException e) {
			fail("cargar(1) lanzo SQLException (no posiciona el cursor del ResultSet): " + e.getMessage());
		}
		assertNotNull(dto);
		assertEquals("Juan", dto.getNombre());
		assertEquals("State", dto.getApellido());
		assertEquals("12345678", dto.getDni());
		assertEquals("Buenos Aires", dto.getCalle());
		assertEquals(123, dto.getNumero());
		assertEquals("Mar del Plata", dto.getCiudad());
		assertEquals("2235551234", dto.getTelefono());
	}

	@Test
	public void testCargarUltimoId() {
		try {
			assertEquals("28293031", dao.cargar(UtilBD.CANTIDAD_SEMILLA).getDni());
		} catch (SQLException e) {
			fail("cargar(20) lanzo SQLException: " + e.getMessage());
		}
	}

	@Test
	public void testCargarIdInexistente() {
		try {
			assertNull(dao.cargar(999));
		} catch (SQLException e) {
			fail("cargar de un id inexistente deberia devolver null y lanzo: " + e.getMessage());
		}
	}

	@Test(expected = AssertionError.class)
	public void testCargarIdCero() throws SQLException {
		dao.cargar(0);
	}

	@Test(expected = AssertionError.class)
	public void testCargarIdNegativo() throws SQLException {
		dao.cargar(-1);
	}

	// CARGAR TODO

	@Test
	public void testCargarBD() throws SQLException {
		ArrayList<AsociadoDTO> lista = dao.cargarBD();
		assertEquals(UtilBD.CANTIDAD_SEMILLA, lista.size());

		AsociadoDTO primero = lista.get(0);
		assertEquals("Juan", primero.getNombre());
		assertEquals("Buenos Aires", primero.getCalle());
		assertEquals(123, primero.getNumero());
		assertEquals("Mar del Plata", primero.getCiudad());
		assertEquals("2235551234", primero.getTelefono());
	}

	@Test
	public void testCargarBDIncluyeRegistroGuardado() throws SQLException {
		dao.guardar(nuevoAsociado("40111222"));
		ArrayList<AsociadoDTO> lista = dao.cargarBD();
		assertEquals(UtilBD.CANTIDAD_SEMILLA + 1, lista.size());
		assertEquals("40111222", lista.get(lista.size() - 1).getDni());
	}

	@Test
	public void testCargarBDTablaVacia() throws SQLException {
		for (AsociadoDTO dto : dao.cargarBD()) {
			dao.eliminar(dto.getDni());
		}
		assertTrue(dao.cargarBD().isEmpty());
	}

	// ELIMINAR

	@Test
	public void testEliminar() throws SQLException {
		dao.eliminar("12345678");
		assertEquals(UtilBD.CANTIDAD_SEMILLA - 1, UtilBD.contarFilas(conexion));
		assertNull(UtilBD.buscarPorDni(conexion, "12345678"));
	}

	@Test
	public void testEliminarDniInexistente() throws SQLException {
		dao.eliminar("00000000");
		assertEquals(UtilBD.CANTIDAD_SEMILLA, UtilBD.contarFilas(conexion));
	}

	@Test(expected = AssertionError.class)
	public void testEliminarDniVacio() throws SQLException {
		dao.eliminar("");
	}

	@Test
	public void testEliminarDniNuloRechazadoPorElDAO() throws SQLException {
		try {
			dao.eliminar(null);
			fail("Se acepto un DNI nulo");
		} catch (AssertionError e) {
			// La precondicion la debe validar el DAO (con su mensaje), no la capa inferior
			assertEquals("El DNI no puede ser nulo", e.getMessage());
		}
	}

	// ACTUALIZAR

	@Test
	public void testActualizar() {
		try {
			dao.actualizar(asociadoSemillaId5ConTelefono("2230000000"));
		} catch (SQLException e) {
			fail("actualizar del asociado id=5 lanzo SQLException: " + e.getMessage());
		}
		try {
			assertEquals("2230000000", UtilBD.buscarPorDni(conexion, "33445566").getTelefono());
			assertEquals("Se modifico el registro id=1", "12345678", UtilBD.buscarPorId(conexion, 1).getDni());
		} catch (SQLException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void testActualizarPrimerRegistro() throws SQLException {
		Asociado juan = new Asociado("Juan", "State", "12345678", "2230000000", new Domicilio("Buenos Aires", 123, "Mar del Plata"));
		dao.actualizar(juan);
		assertEquals("2230000000", UtilBD.buscarPorId(conexion, 1).getTelefono());
	}

	@Test(expected = AssertionError.class)
	public void testActualizarNulo() throws SQLException {
		dao.actualizar(null);
	}

	// MAPEO Asociado <-> AsociadoDTO

	@Test
	public void testCrearDTOAsociado() {
		AsociadoDTO dto = dao.CrearDTO(nuevoAsociado("40111222"));
		assertEquals("Ana", dto.getNombre());
		assertEquals("Prueba", dto.getApellido());
		assertEquals("40111222", dto.getDni());
		assertEquals("Colon", dto.getCalle());
		assertEquals(1500, dto.getNumero());
		assertEquals("Mar del Plata", dto.getCiudad());
		assertEquals("2235000000", dto.getTelefono());
	}

	@Test(expected = AssertionError.class)
	public void testCrearDTOAsociadoNulo() {
		dao.CrearDTO(null);
	}

	@Test
	public void testCrearDTOIntStringStringStringStringIntStringString() {
		AsociadoDTO dto = dao.CrearDTO(7, "Laura", "Singleton", "22334455", "Alem", 1450, "Mar del Plata", "2236112233");
		assertEquals("Laura", dto.getNombre());
		assertEquals("Singleton", dto.getApellido());
		assertEquals("22334455", dto.getDni());
		assertEquals("Alem", dto.getCalle());
		assertEquals(1450, dto.getNumero());
		assertEquals("Mar del Plata", dto.getCiudad());
		assertEquals("2236112233", dto.getTelefono());
	}

	@Test(expected = AssertionError.class)
	public void testCrearDTOIntStringConNombreNulo() {
		dao.CrearDTO(7, null, "Singleton", "22334455", "Alem", 1450, "Mar del Plata", "2236112233");
	}

	@Test(expected = AssertionError.class)
	public void testCrearDTOIntStringConNumeroNegativo() {
		dao.CrearDTO(7, "Laura", "Singleton", "22334455", "Alem", -1, "Mar del Plata", "2236112233");
	}

	@Test
	public void testAsociadoDesdeDTOConNumeroCero() {
		// numero = 0 es valido para el DTO segun el SRS (>= 0): el dominio deberia poder construirse con el
		AsociadoDTO dto = new AsociadoDTO("Ana", "Prueba", "40111222", "Mar del Plata", "Colon", 0, "2235000000");
		try {
			Asociado asociado = new Asociado(dto);
			assertEquals(0, asociado.getDomicilio().getNumero());
		} catch (AssertionError e) {
			fail("Un AsociadoDTO valido (numero = 0) no puede convertirse en Asociado: Domicilio exige numero > 0");
		}
	}

	@Test
	public void testIdaYVueltaGuardarYCargar() throws SQLException {
		Asociado original = nuevoAsociado("40111222");
		dao.guardar(original);
		AsociadoDTO leido = null;
		for (AsociadoDTO dto : dao.cargarBD()) {
			if ("40111222".equals(dto.getDni())) {
				leido = dto;
			}
		}
		assertNotNull(leido);
		assertEquals(original, new Asociado(leido));
	}

	// CERRAR CONEXION

	@Test
	public void testCerrarConexion() throws Exception {
		try {
			dao.cerrarConexion();
			assertTrue(BaseDeDatos.getInstancia().getConexion().isClosed());
		} finally {
			UtilBD.resetSingleton();
		}
	}

}
