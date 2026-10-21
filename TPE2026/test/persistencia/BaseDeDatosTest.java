package persistencia;

import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Assume;
import org.junit.Test;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class BaseDeDatosTest {

    private static BaseDeDatos baseDeDatos;
    private static DataAccessObject dao;
    private String dniCreado;

    @BeforeClass
    public static void inicializarBaseDeDatosDePrueba() {
        Assume.assumeTrue(
                "BaseDeDatos.iniciarBD() elimina y recrea Asociados. "
                        + "Habilite la suite solo para una base de testing exclusiva.",
                Boolean.getBoolean("clinica.integration.allowDatabaseReset"));
        baseDeDatos = BaseDeDatos.getInstancia();
        dao = new DataAccessObject();
    }

    @After
    public void limpiarRegistroDePrueba() throws SQLException {
        if (dniCreado != null) {
            dao.eliminar(dniCreado);
        }
    }

    @Test
    public void getInstanciaDevuelveElMismoSingleton() {
        assertSame(baseDeDatos, BaseDeDatos.getInstancia());
    }

    @Test
    public void inicializacionAbreUnaConexionActiva() throws SQLException {
        Connection conexion = baseDeDatos.getConexion();

        assertNotNull("Debe existir una conexión JDBC", conexion);
        assertTrue("La conexión debe estar activa", conexion.isValid(2));
        assertEquals("Grupo_7", conexion.getCatalog());
    }

    @Test
    public void iniciarBDCreaLaTablaYCargaLosDatosSemilla() throws SQLException {
        baseDeDatos.iniciarBD();

        assertEquals("La tabla debe contener los 20 registros semilla",
                20, dao.cargarBD().size());
        assertTrue("Debe estar presente el primer asociado semilla",
                existeAsociadoConDni("12345678"));
        assertTrue("Debe estar presente el último asociado semilla",
                existeAsociadoConDni("28293031"));
    }

    @Test
    public void insertYDeletePersistenParametrosConCaracteresEspeciales() throws SQLException {
        dniCreado = generarDniUnico();
        AsociadoDTO datos = new AsociadoDTO(
                "O'Connor", "Apellido prueba", dniCreado, "Ciudad prueba",
                "Calle 'con comilla'", 25, "2230000000");

        try {
            baseDeDatos.ejecutarInsert(
                    "INSERT INTO Asociados(nombre, apellido, DNI, calle, numero, ciudad, telefono) "
                            + "VALUES (?, ?, ?, ?, ?, ?, ?)", datos);

            AsociadoDTO persistido = buscarPorDni(dniCreado);
            assertNotNull("El insert debe persistir el registro", persistido);
            assertEquals(datos.getNombre(), persistido.getNombre());
            assertEquals(datos.getCalle(), persistido.getCalle());
            assertEquals(datos.getDni(), persistido.getDni());
        } finally {
            baseDeDatos.ejecutarDelete(
                    "DELETE FROM Asociados WHERE DNI = ?", dniCreado);
            dniCreado = null;
        }
    }

    @Test
    public void consultaPropagaErroresSql() throws SQLException {
        try {
            baseDeDatos.ejecutarConsulta(
                    "SELECT * FROM tabla_inexistente_para_test", -1);
            fail("La consulta a una tabla inexistente debe producir SQLException");
        } catch (SQLException esperada) {
            assertNotNull(esperada.getSQLState());
        }
    }

    @Test
    public void iniciarBDEliminaDatosNoSemillaYRestableceSemillas() throws SQLException {
        dniCreado = generarDniUnico();
        dao.guardar(new modelo.modeloDominio.personas.asociado.Asociado(
                new AsociadoDTO("Temporal", "Test", dniCreado, "Ciudad",
                        "Calle", 1, "2230000000")));

        baseDeDatos.iniciarBD();
        dniCreado = null;

        assertEquals("La reinicialización debe restaurar solo los datos semilla",
                20, dao.cargarBD().size());
        assertTrue("La reinicialización debe conservar los datos semilla",
                existeAsociadoConDni("12345678"));
    }

    private boolean existeAsociadoConDni(String dni) throws SQLException {
        return buscarPorDni(dni) != null;
    }

    private AsociadoDTO buscarPorDni(String dni) throws SQLException {
        for (AsociadoDTO asociado : dao.cargarBD()) {
            if (dni.equals(asociado.getDni())) {
                return asociado;
            }
        }
        return null;
    }

    private String generarDniUnico() throws SQLException {
        String dni;
        do {
            dni = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        } while (existeAsociadoConDni(dni));
        return dni;
    }
}
