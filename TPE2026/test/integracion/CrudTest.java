package integracion;

import modelo.modeloDominio.clinica.Clinica;
import modelo.modeloDominio.personas.asociado.Asociado;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.Assume;
import persistencia.AsociadoDTO;
import persistencia.DataAccessObject;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

public class CrudTest {

    private static Clinica clinica;
    private static DataAccessObject dao;

    private String dniPrueba;
    private AsociadoDTO datosPrueba;
    private List<String> dnisDePrueba;

    @BeforeClass
    public static void inicializarDependencias() {
        Assume.assumeTrue(
                "La BaseDeDatos descarta y recrea Asociados al inicializarse. "
                        + "Habilite esta suite solo para una base de testing exclusiva.",
                Boolean.getBoolean("clinica.integration.allowDatabaseReset"));
        clinica = Clinica.getInstancia();
        dao = new DataAccessObject();
    }

    @Before
    public void prepararDatosDePrueba() throws SQLException {
        dnisDePrueba = new ArrayList<>();
        dniPrueba = generarDniDePrueba();
        datosPrueba = nuevoAsociado(dniPrueba);
    }

    @After
    public void limpiarDatosDePrueba() throws SQLException {
        if (dnisDePrueba != null) {
            for (String dni : dnisDePrueba) {
                clinica.eliminarAsociado(dni);
                dao.eliminar(dni);
            }
        }
    }

    @Test
    public void altaDesdeClinicaPersisteTodosLosAtributos() throws SQLException {
        clinica.guardarNuevoAsociado(datosPrueba);

        AsociadoDTO persistido = buscarPorDni(dniPrueba);

        assertNotNull("El asociado creado debe estar persistido", persistido);
        assertEquals(datosPrueba.getNombre(), persistido.getNombre());
        assertEquals(datosPrueba.getApellido(), persistido.getApellido());
        assertEquals(datosPrueba.getDni(), persistido.getDni());
        assertEquals(datosPrueba.getCalle(), persistido.getCalle());
        assertEquals(datosPrueba.getNumero(), persistido.getNumero());
        assertEquals(datosPrueba.getCiudad(), persistido.getCiudad());
        assertEquals(datosPrueba.getTelefono(), persistido.getTelefono());
    }

    @Test
    public void consultaDeClinicaIncluyeElAsociadoPersistido() throws SQLException {
        dao.guardar(new Asociado(datosPrueba));

        AsociadoDTO consultado = buscarPorDni(dniPrueba);

        assertNotNull("La consulta debe devolver el DNI insertado", consultado);
        assertEquals(datosPrueba.getNombre(), consultado.getNombre());
        assertEquals(datosPrueba.getApellido(), consultado.getApellido());
        assertEquals(datosPrueba.getCalle(), consultado.getCalle());
        assertEquals(datosPrueba.getNumero(), consultado.getNumero());
        assertEquals(datosPrueba.getCiudad(), consultado.getCiudad());
        assertEquals(datosPrueba.getTelefono(), consultado.getTelefono());
    }

    @Test
    public void consultaDeClinicaNoIncluyeUnDniInexistente() throws SQLException {
        assertNull("No debe encontrarse un asociado con un DNI no insertado",
                buscarPorDni(dniPrueba));
    }

    @Test
    public void altaConDniDuplicadoFallaYConservaElRegistroOriginal() throws SQLException {
        clinica.guardarNuevoAsociado(datosPrueba);
        AsociadoDTO duplicado = nuevoAsociado(dniPrueba);
        duplicado.setNombre("Otro nombre");

        try {
            dao.guardar(new Asociado(duplicado));
            fail("La clave única DNI debe impedir la segunda inserción");
        } catch (SQLException esperada) {
            assertEquals("La violación de unicidad debe ser una restricción de integridad",
                    "23000", esperada.getSQLState());
        }

        AsociadoDTO persistido = buscarPorDni(dniPrueba);
        assertNotNull("El registro original debe seguir presente", persistido);
        assertEquals(datosPrueba.getNombre(), persistido.getNombre());
    }

    @Test
    public void bajaDesdeClinicaEliminaSoloElAsociadoSolicitado() throws SQLException {
        clinica.guardarNuevoAsociado(datosPrueba);

        String dniAjeno = generarDniDePrueba();
        AsociadoDTO datosAjeno = nuevoAsociado(dniAjeno);
        clinica.guardarNuevoAsociado(datosAjeno);

        clinica.eliminarAsociado(dniPrueba);

        assertNull("El asociado eliminado no debe aparecer en la consulta",
                buscarPorDni(dniPrueba));
        assertNotNull("La baja no debe eliminar otros registros",
                buscarPorDni(dniAjeno));
    }

    @Test
    public void bajaDeDniInexistenteNoEliminaRegistros() throws SQLException {
        String dniAjeno = generarDniDePrueba();
        AsociadoDTO datosAjeno = nuevoAsociado(dniAjeno);
        clinica.guardarNuevoAsociado(datosAjeno);

        clinica.eliminarAsociado(dniPrueba);
        assertNotNull("El registro no relacionado debe permanecer",
                buscarPorDni(dniAjeno));
    }

    private static AsociadoDTO nuevoAsociado(String dni) {
        return new AsociadoDTO(
                "Nombre prueba",
                "Apellido prueba",
                dni,
                "Ciudad prueba",
                "Calle prueba",
                123,
                "2230000000");
    }

    private static AsociadoDTO buscarPorDni(String dni) throws SQLException {
        ArrayList<AsociadoDTO> asociados = clinica.getAsociadosDTO();
        AsociadoDTO encontrado = null;
        for (AsociadoDTO asociado : asociados) {
            if (dni.equals(asociado.getDni())) {
                if (encontrado != null) {
                    fail("La consulta devolvió más de un asociado con el DNI " + dni);
                }
                encontrado = asociado;
            }
        }
        return encontrado;
    }

    private String generarDniDePrueba() throws SQLException {
        String dni;
        do {
            dni = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        } while (buscarPorDni(dni) != null);
        dnisDePrueba.add(dni);
        return dni;
    }
}
