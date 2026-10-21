package persistencia;

import modelo.modeloDominio.personas.asociado.Asociado;
import modelo.modeloDominio.util.Domicilio;
import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

public class DataAccessObjectTest {

    private static DataAccessObject dao;
    private static BaseDeDatos baseDeDatos;

    private List<String> dnisDePrueba;
    private String dniPrueba;
    private boolean actualizarPuedeHaberModificadoSemilla;

    @BeforeClass
    public static void inicializarDependencias() {
        Assume.assumeTrue(
                "BaseDeDatos.iniciarBD() elimina y recrea Asociados. "
                        + "Habilite la suite solo para una base de testing exclusiva.",
                Boolean.getBoolean("clinica.integration.allowDatabaseReset"));
        baseDeDatos = BaseDeDatos.getInstancia();
        dao = new DataAccessObject();
    }

    @Before
    public void prepararDatos() throws SQLException {
        dnisDePrueba = new ArrayList<>();
        dniPrueba = generarDniUnico();
    }

    @After
    public void limpiarDatos() throws SQLException {
        if (actualizarPuedeHaberModificadoSemilla) {
            baseDeDatos.iniciarBD();
        } else {
            for (String dni : dnisDePrueba) {
                dao.eliminar(dni);
            }
        }
    }

    @Test
    public void guardarPersisteTodosLosAtributosDelAsociado() throws SQLException {
        AsociadoDTO datos = nuevoAsociado(dniPrueba, "Nombre O'Connor");

        dao.guardar(new Asociado(datos));

        assertAsociadoIgual(datos, buscarPorDni(dniPrueba));
    }

    @Test
    public void cargarBDDevuelveElAsociadoGuardado() throws SQLException {
        AsociadoDTO datos = nuevoAsociado(dniPrueba, "Nombre persistido");
        dao.guardar(new Asociado(datos));

        AsociadoDTO recuperado = buscarPorDni(dniPrueba);

        assertAsociadoIgual(datos, recuperado);
    }

    @Test
    public void cargarConLimiteRespetaElLimiteSolicitado() throws SQLException {
        ArrayList<AsociadoDTO> resultado = dao.cargarConLimite(1);

        assertEquals("Debe devolver como máximo el límite solicitado",
                1, resultado.size());
    }

    @Test
    public void cargarPorIdRecuperaElRegistroPersistido() throws SQLException {
        AsociadoDTO datos = nuevoAsociado(dniPrueba, "Carga por ID");
        dao.guardar(new Asociado(datos));
        int idGenerado = obtenerIdPorDni(dniPrueba);

        AsociadoDTO recuperado = dao.cargar(idGenerado);

        assertNotNull("Debe recuperar el registro existente por su ID", recuperado);
        assertEquals(datos.getNombre(), recuperado.getNombre());
        assertEquals(datos.getApellido(), recuperado.getApellido());
        assertEquals(datos.getDni(), recuperado.getDni());
        assertEquals(datos.getCalle(), recuperado.getCalle());
        assertEquals(datos.getNumero(), recuperado.getNumero());
        assertEquals(datos.getCiudad(), recuperado.getCiudad());
        assertEquals(datos.getTelefono(), recuperado.getTelefono());
    }

    @Test
    public void eliminarQuitaSoloElRegistroSolicitado() throws SQLException {
        String dniAjeno = generarDniUnico();
        dao.guardar(new Asociado(nuevoAsociado(dniPrueba, "A eliminar")));
        dao.guardar(new Asociado(nuevoAsociado(dniAjeno, "Debe permanecer")));

        dao.eliminar(dniPrueba);
        dnisDePrueba.remove(dniPrueba);

        assertNull("El registro seleccionado debe desaparecer",
                buscarPorDni(dniPrueba));
        assertNotNull("El registro ajeno debe permanecer",
                buscarPorDni(dniAjeno));
    }

    @Test
    public void actualizarModificaElRegistroConElMismoDni() throws SQLException {
        AsociadoDTO original = nuevoAsociado(dniPrueba, "Nombre original");
        dao.guardar(new Asociado(original));
        AsociadoDTO modificado = nuevoAsociado(dniPrueba, "Nombre modificado");
        modificado.setApellido("Apellido modificado");
        modificado.setCalle("Calle modificada");
        modificado.setNumero(456);
        modificado.setCiudad("Ciudad modificada");
        modificado.setTelefono("2231111111");

        actualizarPuedeHaberModificadoSemilla = true;
        dao.actualizar(new Asociado(modificado));

        assertAsociadoIgual(modificado, buscarPorDni(dniPrueba));
    }

    @Test
    public void guardarDniDuplicadoPropagaLaViolacionDeUnicidad() throws SQLException {
        AsociadoDTO datos = nuevoAsociado(dniPrueba, "Registro original");
        dao.guardar(new Asociado(datos));

        try {
            dao.guardar(new Asociado(nuevoAsociado(dniPrueba, "Registro duplicado")));
            fail("DNI es único y la segunda inserción debe fallar");
        } catch (SQLException esperada) {
            assertEquals("MySQL debe informar una violación de integridad",
                    "23000", esperada.getSQLState());
        }

        assertAsociadoIgual(datos, buscarPorDni(dniPrueba));
    }

    @Test
    public void crearDTODesdeAsociadoMapeaLosAtributos() {
        Asociado asociado = new Asociado(
                "Nombre", "Apellido", "12345670", "2231234567",
                new Domicilio("Calle", 42, "Ciudad"));

        AsociadoDTO dto = dao.CrearDTO(asociado);

        assertEquals("Nombre", dto.getNombre());
        assertEquals("Apellido", dto.getApellido());
        assertEquals("12345670", dto.getDni());
        assertEquals("Calle", dto.getCalle());
        assertEquals(42, dto.getNumero());
        assertEquals("Ciudad", dto.getCiudad());
        assertEquals("2231234567", dto.getTelefono());
    }

    private static AsociadoDTO nuevoAsociado(String dni, String nombre) {
        return new AsociadoDTO(nombre, "Apellido prueba", dni, "Ciudad prueba",
                "Calle prueba", 123, "2230000000");
    }

    private static void assertAsociadoIgual(AsociadoDTO esperado, AsociadoDTO actual) {
        assertNotNull("El asociado debe existir", actual);
        assertEquals(esperado.getNombre(), actual.getNombre());
        assertEquals(esperado.getApellido(), actual.getApellido());
        assertEquals(esperado.getDni(), actual.getDni());
        assertEquals(esperado.getCalle(), actual.getCalle());
        assertEquals(esperado.getNumero(), actual.getNumero());
        assertEquals(esperado.getCiudad(), actual.getCiudad());
        assertEquals(esperado.getTelefono(), actual.getTelefono());
    }

    private AsociadoDTO buscarPorDni(String dni) throws SQLException {
        for (AsociadoDTO asociado : dao.cargarBD()) {
            if (dni.equals(asociado.getDni())) {
                return asociado;
            }
        }
        return null;
    }

    private int obtenerIdPorDni(String dni) throws SQLException {
        String query = "SELECT id FROM Asociados WHERE DNI = ?";
        try (PreparedStatement statement = baseDeDatos.getConexion().prepareStatement(query)) {
            statement.setString(1, dni);
            try (ResultSet resultado = statement.executeQuery()) {
                if (!resultado.next()) {
                    throw new SQLException("No se encontró el asociado de prueba");
                }
                return resultado.getInt("id");
            }
        }
    }

    private String generarDniUnico() throws SQLException {
        String dni;
        do {
            dni = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        } while (buscarPorDni(dni) != null);
        dnisDePrueba.add(dni);
        return dni;
    }
}
