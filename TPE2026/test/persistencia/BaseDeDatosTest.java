package persistencia;

import static org.junit.Assert.*;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Suite de integracion de BaseDeDatos contra MySQL (Consigna 2).
 * Requiere un servidor MySQL en localhost:3306 con usuario/clave progra_c.
 * Antes de cada test se recrea la tabla Asociados con los 20 registros semilla.
 */
public class BaseDeDatosTest {

	private static final String INSERT = "INSERT INTO Asociados(nombre, apellido, DNI, calle, numero, ciudad, telefono) VALUES (?, ?, ?, ?, ?, ?, ?);";
	private static final String UPDATE = "UPDATE Asociados SET nombre = ?, apellido = ?, dni = ?, calle = ?, numero = ?, ciudad = ?, telefono = ? WHERE id = ? ;";
	private static final String DELETE = "DELETE FROM Asociados WHERE dni = ? ;";

	private BaseDeDatos bd;
	private Connection conexion;

	@BeforeClass
	public static void verificarServidor() {
		assertTrue("Ejecutar la JVM con -ea", BaseDeDatos.class.desiredAssertionStatus());
		assertNotNull("MySQL no disponible: no se pudo abrir la conexion (localhost:3306, progra_c)",
				BaseDeDatos.getInstancia().getConexion());
	}

	@Before
	public void setUp() throws Exception {
		bd = UtilBD.prepararBD();
		conexion = bd.getConexion();
	}

	@AfterClass
	public static void tearDown() throws Exception {
		UtilBD.resetSingleton();
	}

	private static AsociadoDTO nuevoDTO(String dni) {
		return new AsociadoDTO("Ana", "Prueba", dni, "Mar del Plata", "Colon", 1500, "2235000000");
	}

	// SINGLETON

	@Test
	public void testGetInstancia() {
		BaseDeDatos instancia1 = BaseDeDatos.getInstancia();
		BaseDeDatos instancia2 = BaseDeDatos.getInstancia();

		assertSame(instancia1, instancia2);
	}

	@Test
	public void testGetInstanciaConcurrente() throws Exception {
		UtilBD.resetSingleton();
		final int hilos = 10;
		final CountDownLatch largada = new CountDownLatch(1);
		final CountDownLatch fin = new CountDownLatch(hilos);
		final List<BaseDeDatos> obtenidas = Collections.synchronizedList(new ArrayList<BaseDeDatos>());

		for (int i = 0; i < hilos; i++) {
			new Thread(() -> {
				try {
					largada.await();
					obtenidas.add(BaseDeDatos.getInstancia());
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				} finally {
					fin.countDown();
				}
			}).start();
		}
		largada.countDown();
		assertTrue("Los hilos no terminaron", fin.await(30, TimeUnit.SECONDS));

		Set<BaseDeDatos> distintas = Collections.newSetFromMap(new IdentityHashMap<BaseDeDatos, Boolean>());
		distintas.addAll(obtenidas);
		// cerrar las conexiones de las instancias "perdidas" para no dejar conexiones abiertas
		for (BaseDeDatos b : distintas) {
			if (b != BaseDeDatos.getInstancia() && b.getConexion() != null) {
				b.getConexion().close();
			}
		}
		assertEquals("getInstancia() no es thread-safe: se crearon " + distintas.size() + " instancias",
				1, distintas.size());
	}

	// CONEXION

	@Test
	public void testGetConexion() throws SQLException {
		assertNotNull(conexion);
		assertFalse(conexion.isClosed());
		assertTrue(conexion.isValid(2));
	}

	@Test
	public void testConexionApuntaAlEsquemaGrupo7() throws SQLException {
		try (Statement stmt = conexion.createStatement();
			 ResultSet rs = stmt.executeQuery("SELECT DATABASE()")) {
			assertTrue(rs.next());
			assertTrue("Esquema actual: " + rs.getString(1), "Grupo_7".equalsIgnoreCase(rs.getString(1)));
		}
	}

	@Test
	public void testCerrarConexion() throws Exception {
		bd.cerrarConexion();
		assertTrue(bd.getConexion().isClosed());
		UtilBD.resetSingleton();
	}

	@Test
	public void testInstanciaUtilizableDespuesDeCerrarConexion() throws Exception {
		bd.cerrarConexion();
		try {
			Connection c = BaseDeDatos.getInstancia().getConexion();
			assertFalse("El Singleton sigue entregando la conexion cerrada y no la reabre", c.isClosed());
		} finally {
			UtilBD.resetSingleton();
		}
	}

	// INICIALIZACION (iniciarBD)

	@Test
	public void testIniciarBDCreaTablaAsociados() throws SQLException {
		String sql = "SELECT COUNT(*) FROM information_schema.tables "
				+ "WHERE table_schema = DATABASE() AND LOWER(table_name) = 'asociados'";
		try (Statement stmt = conexion.createStatement();
			 ResultSet rs = stmt.executeQuery(sql)) {
			assertTrue(rs.next());
			assertEquals("No existe la tabla Asociados", 1, rs.getInt(1));
		}
	}

	@Test
	public void testIniciarBDCargaDatosSemilla() throws SQLException {
		assertEquals(UtilBD.CANTIDAD_SEMILLA, UtilBD.contarFilas(conexion));
		for (int id = 1; id <= UtilBD.CANTIDAD_SEMILLA; id++) {
			assertNotNull("Falta el registro semilla id=" + id, UtilBD.buscarPorId(conexion, id));
		}
	}

	@Test
	public void testIniciarBDPrimerRegistroSemilla() throws SQLException {
		AsociadoDTO fila = UtilBD.buscarPorId(conexion, 1);
		assertEquals("Juan", fila.getNombre());
		assertEquals("State", fila.getApellido());
		assertEquals("12345678", fila.getDni());
		assertEquals("Buenos Aires", fila.getCalle());
		assertEquals(123, fila.getNumero());
		assertEquals("Mar del Plata", fila.getCiudad());
		assertEquals("2235551234", fila.getTelefono());
	}

	@Test
	public void testIniciarBDCiudadesSemillaConsistentes() throws SQLException {
		for (int id = 1; id <= UtilBD.CANTIDAD_SEMILLA; id++) {
			assertEquals("Ciudad inconsistente en el registro semilla id=" + id,
					"Mar del Plata", UtilBD.buscarPorId(conexion, id).getCiudad());
		}
	}

	@Test
	public void testIniciarBDEsIdempotente() throws SQLException {
		bd.ejecutarInsert(INSERT, nuevoDTO("40111222"));
		bd.iniciarBD();
		assertEquals(UtilBD.CANTIDAD_SEMILLA, UtilBD.contarFilas(conexion));
		assertNull(UtilBD.buscarPorDni(conexion, "40111222"));
	}

	@Test(expected = SQLIntegrityConstraintViolationException.class)
	public void testDniEsUnico() throws SQLException {
		bd.ejecutarInsert(INSERT, nuevoDTO("12345678"));
	}

	// CONSULTAS

	@Test
	public void testEjecutarConsulta() throws SQLException {
		ResultSet rs = bd.ejecutarConsulta("SELECT * FROM Asociados WHERE id = ? ;", 5);
		assertTrue(rs.next());
		assertEquals("Xin", rs.getString("nombre"));
		assertEquals("33445566", rs.getString("dni"));
		assertFalse(rs.next());
		rs.close();
	}

	@Test
	public void testEjecutarConsultaSinParametro() throws SQLException {
		ResultSet rs = bd.ejecutarConsulta("SELECT * FROM Asociados ;", -1);
		int filas = 0;
		while (rs.next()) {
			filas++;
		}
		rs.close();
		assertEquals(UtilBD.CANTIDAD_SEMILLA, filas);
	}

	@Test
	public void testEjecutarConsultaIdInexistente() throws SQLException {
		ResultSet rs = bd.ejecutarConsulta("SELECT * FROM Asociados WHERE id = ? ;", 999);
		assertFalse(rs.next());
		rs.close();
	}

	@Test(expected = SQLException.class)
	public void testEjecutarConsultaParametroSinValor() throws SQLException {
		bd.ejecutarConsulta("SELECT * FROM Asociados WHERE id = ? ;", -1);
	}

	@Test(expected = SQLException.class)
	public void testEjecutarConsultaSQLInvalido() throws SQLException {
		bd.ejecutarConsulta("SELECT * FROM TablaInexistente ;", -1);
	}

	@Test(expected = AssertionError.class)
	public void testEjecutarConsultaNula() throws SQLException {
		bd.ejecutarConsulta(null, -1);
	}

	// INSERT

	@Test
	public void testEjecutarInsert() throws SQLException {
		bd.ejecutarInsert(INSERT, nuevoDTO("40111222"));

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
	public void testEjecutarInsertDTONulo() throws SQLException {
		bd.ejecutarInsert(INSERT, null);
	}

	@Test(expected = AssertionError.class)
	public void testEjecutarInsertSentenciaNula() throws SQLException {
		bd.ejecutarInsert(null, nuevoDTO("40111222"));
	}

	@Test(expected = SQLException.class)
	public void testEjecutarInsertDniExcedeLongitud() throws SQLException {
		// DNI VARCHAR(10): 11 caracteres es el primer valor fuera del limite
		bd.ejecutarInsert(INSERT, nuevoDTO("12345678901"));
	}

	@Test
	public void testEjecutarInsertDniLongitudMaxima() throws SQLException {
		bd.ejecutarInsert(INSERT, nuevoDTO("1234567890"));
		assertNotNull(UtilBD.buscarPorDni(conexion, "1234567890"));
	}

	// UPDATE

	@Test
	public void testEjecutarActualizacionDelRegistroId5() throws SQLException {
		// Se actualiza el telefono del asociado id=5 (dni 33445566) usando la sentencia del DAO
		AsociadoDTO cambios = new AsociadoDTO("Xin", "Dao", "33445566", "Mar del Plata", "Corrientes", 789, "2230000000");
		try {
			bd.ejecutarActualizacion(UPDATE, cambios);
		} catch (SQLException e) {
			fail("La actualizacion no se aplico sobre el registro id=5: " + e.getMessage());
		}

		assertEquals("2230000000", UtilBD.buscarPorId(conexion, 5).getTelefono());
		assertEquals("El UPDATE modifico el registro id=1", "12345678", UtilBD.buscarPorId(conexion, 1).getDni());
	}

	@Test
	public void testEjecutarActualizacionNoModificaOtrosRegistros() throws SQLException {
		// DNI nuevo: no hay conflicto de UNIQUE, se observa que fila termina modificada
		AsociadoDTO cambios = new AsociadoDTO("Nuevo", "Asociado", "40111222", "Mar del Plata", "Colon", 1, "2230000000");
		bd.ejecutarActualizacion("UPDATE Asociados SET nombre = ?, apellido = ?, dni = ?, calle = ?, numero = ?, ciudad = ?, telefono = ? WHERE id = ? ;", cambios);

		AsociadoDTO fila1 = UtilBD.buscarPorId(conexion, 1);
		assertEquals("El UPDATE piso el registro id=1, que no fue el solicitado", "Juan", fila1.getNombre());
	}

	@Test(expected = AssertionError.class)
	public void testEjecutarActualizacionDTONulo() throws SQLException {
		bd.ejecutarActualizacion(UPDATE, null);
	}

	// DELETE

	@Test
	public void testEjecutarDelete() throws SQLException {
		bd.ejecutarDelete(DELETE, "12345678");
		assertEquals(UtilBD.CANTIDAD_SEMILLA - 1, UtilBD.contarFilas(conexion));
		assertNull(UtilBD.buscarPorDni(conexion, "12345678"));
	}

	@Test
	public void testEjecutarDeleteDniInexistente() throws SQLException {
		bd.ejecutarDelete(DELETE, "00000000");
		assertEquals(UtilBD.CANTIDAD_SEMILLA, UtilBD.contarFilas(conexion));
	}

	@Test
	public void testEjecutarDeleteResisteInyeccionSQL() throws SQLException {
		bd.ejecutarDelete(DELETE, "' OR '1'='1");
		assertEquals(UtilBD.CANTIDAD_SEMILLA, UtilBD.contarFilas(conexion));
	}

	@Test(expected = AssertionError.class)
	public void testEjecutarDeleteDniNulo() throws SQLException {
		bd.ejecutarDelete(DELETE, null);
	}

}
