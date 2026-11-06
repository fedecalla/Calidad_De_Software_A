package persistencia;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Suite de pruebas de integración para la clase BaseDeDatos.
 *
 * Estrategia de testing: Integración incremental ascendente (Bottom to Top)
 * 1. Funcionamiento básico del objeto de base de datos (Singleton, conexión, ciclo de vida).
 * 2. Inicialización, creación de esquema y datos semilla.
 * 3. Consultas (GET).
 * 4. Inserción (INSERT).
 * 5. Eliminación (DELETE).
 * 6. Modificación (UPDATE).
 *
 * IMPORTANTE: No se utiliza DataAccessObject (DAO) en esta suite.
 * Todas las operaciones se ejecutan directamente sobre la instancia de BaseDeDatos.
 */
public class BaseDeDatosTest {

    private static BaseDeDatos baseDeDatos;

    private static final String SQL_SELECT_TODOS = "SELECT * FROM Asociados;";
    private static final String SQL_SELECT_POR_ID = "SELECT * FROM Asociados WHERE id = ?;";
    private static final String SQL_INSERT = "INSERT INTO Asociados (nombre, apellido, DNI, calle, numero, ciudad, telefono) VALUES (?, ?, ?, ?, ?, ?, ?);";
    private static final String SQL_DELETE_POR_DNI = "DELETE FROM Asociados WHERE DNI = ?;";
    private static final String SQL_UPDATE = "UPDATE Asociados SET nombre = ?, apellido = ?, DNI = ?, calle = ?, numero = ?, ciudad = ?, telefono = ? WHERE id = ?;";

    @BeforeClass
    public static void setUpClass() {
        baseDeDatos = BaseDeDatos.getInstancia();
    }

    @AfterClass
    public static void tearDownClass() {
        resetearInstanciaSingleton();
    }

    @Before
    public void setUp() throws SQLException {
        // Asegurar que la instancia singleton tenga una conexión activa antes de cada prueba
        if (baseDeDatos == null || baseDeDatos.getConexion() == null || baseDeDatos.getConexion().isClosed()) {
            resetearInstanciaSingleton();
            baseDeDatos = BaseDeDatos.getInstancia();
        }
        // Restaurar el estado conocido de la base de datos (datos semilla) para garantizar aislamiento
        baseDeDatos.iniciarBD();
        System.out.println("Base de datos: " +
                baseDeDatos.getConexion().getCatalog());

        System.out.println("Usuario: " +
                baseDeDatos.getConexion().getMetaData().getUserName());

        System.out.println("URL: " +
                baseDeDatos.getConexion().getMetaData().getURL());
    }

    @After
    public void tearDown() {
        // Limpieza posterior si fuera necesaria
    }

    // =========================================================================
    // 5.1. FUNCIONAMIENTO BÁSICO DEL OBJETO DE BASE DE DATOS (SIN DAO)
    // =========================================================================

    // 5.1.1. Singleton
    @Test
    public void test01_Singleton_DevuelveMismaReferencia() {
        BaseDeDatos instancia1 = BaseDeDatos.getInstancia();
        BaseDeDatos instancia2 = BaseDeDatos.getInstancia();

        assertNotNull("La instancia obtenida no debe ser nula", instancia1);
        assertSame("Múltiples llamadas a getInstancia deben retornar la misma referencia",
                instancia1, instancia2);
    }

    @Test
    public void test02_Singleton_MultiplesLlamadasNoGeneranNuevasInstancias() {
        BaseDeDatos referenciaInicial = BaseDeDatos.getInstancia();
        for (int i = 0; i < 5; i++) {
            BaseDeDatos actual = BaseDeDatos.getInstancia();
            assertSame("La instancia en la iteración " + i + " debe ser la misma",
                    referenciaInicial, actual);
        }
    }

    @Test
    public void test03_Singleton_ConexionInicializadaYActiva() throws SQLException {
        Connection conexion = baseDeDatos.getConexion();

        assertNotNull("Debe existir un objeto Connection inicializado", conexion);
        assertFalse("La conexión a la base de datos debe estar abierta", conexion.isClosed());
    }





    // 5.1.2. Creación de la base de datos
    @Test
    public void test04_CreacionBD_IniciarBDEjecutaSinExcepcionesYTablaExiste() throws SQLException {
        baseDeDatos.iniciarBD();

        ResultSet rs = baseDeDatos.ejecutarConsulta("SELECT 1 FROM Asociados LIMIT 1;", -1);
        assertNotNull("El ResultSet de verificación de tabla no debe ser nulo", rs);
        rs.close();
    }

    @Test
    public void test05_CreacionBD_VerificarEstructuraColumnasTablaAsociados() throws SQLException {
        ResultSet rs = baseDeDatos.ejecutarConsulta("SELECT id, nombre, apellido, DNI, calle, numero, ciudad, telefono FROM Asociados LIMIT 1;", -1);
        assertNotNull("La consulta de columnas no debe ser nula", rs);
        assertTrue("Debe existir al menos una fila para validar las columnas", rs.next());
        // Comprobar que los nombres de columnas son válidos y accesibles
        assertNotNull(rs.getString("nombre"));
        assertNotNull(rs.getString("apellido"));
        assertNotNull(rs.getString("DNI"));
        rs.close();
    }

    // 5.1.3. Datos semilla
    @Test
    public void test06_DatosSemilla_CantidadTotalEsVeinte() throws SQLException {
        int totalRegistros = contarRegistros();
        assertEquals("La inicialización debe insertar exactamente 20 registros semilla", 20, totalRegistros);
    }

    @Test
    public void test07_DatosSemilla_ValidarPrimerAsociado() throws SQLException {
        AsociadoDTO primero = buscarPorId(1);

        assertNotNull("El asociado con ID 1 debe existir", primero);
        assertEquals("Juan", primero.getNombre());
        assertEquals("State", primero.getApellido());
        assertEquals("12345678", primero.getDni());
        assertEquals("Buenos Aires", primero.getCalle());
        assertEquals(123, primero.getNumero());
        assertEquals("Mar del Plata", primero.getCiudad());
        assertEquals("2235551234", primero.getTelefono());
    }

    @Test
    public void test08_DatosSemilla_ValidarUltimoAsociado() throws SQLException {
        AsociadoDTO ultimo = buscarPorId(20);

        assertNotNull("El asociado con ID 20 debe existir", ultimo);
        assertEquals("Adrian", ultimo.getNombre());
        assertEquals("Composite", ultimo.getApellido());
        assertEquals("28293031", ultimo.getDni());
        assertEquals("Olavarria", ultimo.getCalle());
        assertEquals(2700, ultimo.getNumero());
        assertEquals("Mar del Plata", ultimo.getCiudad());
        assertEquals("2235778899", ultimo.getTelefono());
    }

    @Test
    public void test09_DatosSemilla_ValidarAsociadoIntermedio() throws SQLException {
        AsociadoDTO intermedio = buscarPorId(7);
        assertNotNull("El asociado con ID 7 debe existir", intermedio);
        assertEquals("Laura", intermedio.getNombre());
        assertEquals("Singleton", intermedio.getApellido());
        assertEquals("22334455", intermedio.getDni());
        assertEquals("Alem", intermedio.getCalle());
        assertEquals(1450, intermedio.getNumero());
        assertEquals("Mar del Plata", intermedio.getCiudad());
        assertEquals("2236112233", intermedio.getTelefono());
    }

    // 5.1.4. Reseteo de la base de datos
    @Test
    public void test10_ReseteoBD_EliminaDatosAdicionalesYRestableceSemilla() throws SQLException {
        // Insertar registros adicionales
        AsociadoDTO adicional1 = crearAsociadoValido("99000001");
        AsociadoDTO adicional2 = crearAsociadoValido("99000002");
        baseDeDatos.ejecutarInsert(SQL_INSERT, adicional1);
        baseDeDatos.ejecutarInsert(SQL_INSERT, adicional2);

        assertEquals("Debe haber 22 registros luego de agregar los adicionales",
                22, contarRegistros());

        // Ejecutar reseteo
        baseDeDatos.iniciarBD();

        // Verificar que los datos adicionales fueron eliminados
        assertEquals("Luego del reseteo debe haber exactamente 20 registros semilla", 20, contarRegistros());

        // Verificar que los registros semilla siguen presentes sin duplicaciones
        assertNotNull("El registro semilla 1 debe existir tras el reseteo", buscarPorId(1));
        assertNotNull("El registro semilla 20 debe existir tras el reseteo", buscarPorId(20));
    }

    // =========================================================================
    // 5.2. CONSULTAS (GET)
    // =========================================================================

    // 5.2.1. Datos válidos
    @Test
    public void test11_Consulta_GeneralConDatosValidosRecuperaTodosLosSemilla() throws SQLException {
        ResultSet rs = baseDeDatos.ejecutarConsulta(SQL_SELECT_TODOS, -1);
        assertNotNull("El resultado de la consulta general no debe ser nulo", rs);

        int contador = 0;
        while (rs.next()) {
            contador++;
            assertNotNull(rs.getString("nombre"));
            assertNotNull(rs.getString("apellido"));
            assertNotNull(rs.getString("DNI"));
            assertNotNull(rs.getString("calle"));
            assertNotNull(rs.getString("ciudad"));
            assertNotNull(rs.getString("telefono"));
        }
        rs.close();
        assertEquals("La consulta general debe iterar por los 20 registros", 20, contador);
    }

    @Test
    public void test12_Consulta_PorIdExistenteRecuperaAtributosExactos() throws SQLException {
        ResultSet rs = baseDeDatos.ejecutarConsulta(SQL_SELECT_POR_ID, 2);
        assertNotNull("El ResultSet no debe ser nulo", rs);
        assertTrue("Debe existir el registro con ID 2", rs.next());

        assertEquals("Maria", rs.getString("nombre"));
        assertEquals("Template", rs.getString("apellido"));
        assertEquals("87654321", rs.getString("DNI"));
        assertEquals("Libertad", rs.getString("calle"));
        assertEquals(742, rs.getInt("numero"));
        assertEquals("Mar del Plata", rs.getString("ciudad"));
        assertEquals("2235555678", rs.getString("telefono"));

        rs.close();
    }



    @Test
    public void test14_Consulta_TablaInexistenteLanzaSQLException() {
        try {
            baseDeDatos.ejecutarConsulta("SELECT * FROM tabla_inexistente_de_prueba;", -1);
            fail("Consultar una tabla inexistente debe lanzar SQLException");
        } catch (SQLException e) {
            assertNotNull("La excepción debe contener SQLState", e.getSQLState());
        }
    }

    @Test
    public void test15_Consulta_ParametroIncompatibleSinMarcadorLanzaSQLException() {
        // Enviar consulta sin marcador de posición '?' pero pasando id != -1 obliga a stmt.setInt(1, id)
        try {
            baseDeDatos.ejecutarConsulta("SELECT * FROM Asociados;", 5);
            fail("Vincular un parámetro en una consulta sin '?' debe lanzar SQLException");
        } catch (SQLException e) {
            assertNotNull("Debe propagarse SQLException por parámetro fuera de rango", e.getMessage());
        }
    }

    // 5.2.3. Asociados inexistentes
    @Test
    public void test16_Consulta_PorIdInexistenteRetornaVacioSinExcepcion() throws SQLException {
        ResultSet rs = baseDeDatos.ejecutarConsulta(SQL_SELECT_POR_ID, 99999);
        assertNotNull("El ResultSet no debe ser nulo para ID inexistente", rs);
        assertFalse("El ResultSet no debe contener filas para un ID inexistente", rs.next());
        rs.close();
    }


    // =========================================================================
    // 5.3. INSERCIÓN (INSERT)
    // =========================================================================

    // 5.3.1. Datos válidos
    @Test
    public void test19_Insert_DatosCompletamenteValidos() throws SQLException {
        String dniUnico = "44112233";
        AsociadoDTO nuevo = new AsociadoDTO("Roberto", "Gomez", dniUnico, "Miramar", "Calle 12", 456, "2291400000");

        baseDeDatos.ejecutarInsert(SQL_INSERT, nuevo);

        AsociadoDTO recuperado = buscarPorDni(dniUnico);
        assertNotNull("El asociado insertado debe poder ser consultado", recuperado);
        assertEquals(nuevo.getNombre(), recuperado.getNombre());
        assertEquals(nuevo.getApellido(), recuperado.getApellido());
        assertEquals(nuevo.getDni(), recuperado.getDni());
        assertEquals(nuevo.getCiudad(), recuperado.getCiudad());
        assertEquals(nuevo.getCalle(), recuperado.getCalle());
        assertEquals(nuevo.getNumero(), recuperado.getNumero());
        assertEquals(nuevo.getTelefono(), recuperado.getTelefono());
    }



    // 5.3.2. Datos inválidos y estados de atributos
    @Test
    public void test20_Insert_CampoVacioPermitidoPorEsquemaMySQL() throws SQLException {
        String dni = "55000001";
        AsociadoDTO dto = new AsociadoDTO("", "Valido", dni, "Ciudad", "Calle", 100, "123456");

        baseDeDatos.ejecutarInsert(SQL_INSERT, dto);

        AsociadoDTO recuperado = buscarPorDni(dni);
        assertNotNull("El registro con nombre vacío debe almacenarse según el esquema MySQL", recuperado);
        assertEquals("", recuperado.getNombre());
    }

    @Test
    public void test21_Insert_CampoNullPermitidoPorEsquemaMySQL() throws SQLException {
        String dni = "55000002";
        AsociadoDTO dto = new AsociadoDTO(null, "Valido", dni, "Ciudad", "Calle", 100, "123456");

        baseDeDatos.ejecutarInsert(SQL_INSERT, dto);

        AsociadoDTO recuperado = buscarPorDni(dni);
        assertNotNull("El registro con nombre NULL debe almacenarse al no ser columna NOT NULL", recuperado);
        assertNull(recuperado.getNombre());
    }
    @Test
    public void test22_Insert_InsercionDtoNullNoInsertaNada()throws Exception{
        AsociadoDTO dto = null;
        baseDeDatos.ejecutarInsert(SQL_INSERT,dto);
        //si falla con cualquier excepcion, el metodo fallara

    }
    @Test
    public void test30_Insert_ConsultaTablaInexistenteLanzaSQLException() {
        AsociadoDTO dto = crearAsociadoValido("55000010");
        try {
            baseDeDatos.ejecutarInsert("INSERT INTO TablaInexistente VALUES (?, ?, ?, ?, ?, ?, ?);", dto);
            fail("Ejecutar un INSERT sobre tabla inexistente debe lanzar SQLException");
        } catch (SQLException e) {
            assertNotNull(e.getMessage());
        }
    }

    // 5.3.4. Asociados existentes (DNI duplicado)
    @Test
    public void test32_Insert_DniDuplicadoLanzaSQLExceptionYNoModificaBD() throws SQLException {
        // '12345678' pertenece al asociado semilla con ID 1
        String dniExistente = "12345678";
        AsociadoDTO original = buscarPorDni(dniExistente);
        assertNotNull("El registro original debe existir", original);

        AsociadoDTO duplicado = new AsociadoDTO("Otro", "Persona", dniExistente, "OtraCiudad", "OtraCalle", 99, "999999");

        try {
            baseDeDatos.ejecutarInsert(SQL_INSERT, duplicado);
            fail("Insertar un DNI duplicado debe violar la restricción UNIQUE y lanzar SQLException");
        } catch (SQLException e) {
            assertNotNull("Debe informarse un código de error de integridad", e.getSQLState());
        }

        // Verificar que el registro original no fue alterado y la cantidad de registros sigue en 20
        assertEquals("La cantidad total de registros debe mantenerse en 20", 20, contarRegistros());
        AsociadoDTO despuesDelFallo = buscarPorDni(dniExistente);
        assertEquals("Juan", despuesDelFallo.getNombre());
        assertEquals("State", despuesDelFallo.getApellido());
    }

    // =========================================================================
    // 5.4. ELIMINACIÓN (DELETE)
    // =========================================================================

    // 5.4.1. Datos válidos
    @Test
    public void test33_Delete_AsociadoExistenteConDniValido() throws SQLException {
        String dniAEliminar = "12345678";
        assertNotNull("El asociado semilla debe existir antes de eliminarlo", buscarPorDni(dniAEliminar));

        baseDeDatos.ejecutarDelete(SQL_DELETE_POR_DNI, dniAEliminar);

        assertNull("El asociado debe haber sido eliminado de la base de datos", buscarPorDni(dniAEliminar));
        assertEquals("La cantidad total de registros debe ser 19 tras la eliminación", 19, contarRegistros());
    }

    // 5.4.2. Datos inválidos
    @Test
    public void test34_Delete_ConDniVacioNoEliminaRegistrosAccidentalmente() throws SQLException {
        int cantidadInicial = contarRegistros();

        baseDeDatos.ejecutarDelete(SQL_DELETE_POR_DNI, "");

        assertEquals("Eliminar con DNI vacío no debe alterar la cantidad de registros", cantidadInicial, contarRegistros());
    }



    @Test
    public void test35_Delete_ConsultaInvalidaLanzaSQLException() {
        try {
            baseDeDatos.ejecutarDelete("DELETE FROM TablaInexistente WHERE DNI = ?;", "12345678");
            fail("Ejecutar DELETE sobre tabla inexistente debe lanzar SQLException");
        } catch (SQLException e) {
            assertNotNull(e.getMessage());
        }
    }

    // 5.4.3. Asociados inexistentes
    @Test
    public void test36_Delete_AsociadoInexistenteEjecutaCorrectamenteSinExcepcion() throws SQLException {
        int cantidadInicial = contarRegistros();
        String dniInexistente = "99999999";
        assertNull(buscarPorDni(dniInexistente));

        // Debe ejecutar sin propagar excepciones
        baseDeDatos.ejecutarDelete(SQL_DELETE_POR_DNI, dniInexistente);

        assertEquals("Ningún registro debe haber sido eliminado", cantidadInicial, contarRegistros());
    }

    @Test
    public void test37_Delete_ConDniNullNoEliminaRegistrosAccidentalmente() throws SQLException {
        int cantidadInicial = contarRegistros();

        baseDeDatos.ejecutarDelete(SQL_DELETE_POR_DNI, null);

        assertEquals("Eliminar con DNI vacío no debe alterar la cantidad de registros",
                cantidadInicial, contarRegistros());
    }
    // =========================================================================
    // 5.5. MODIFICACIÓN (UPDATE)
    // =========================================================================

    // 5.5.1. Datos válidos
    @Test
    public void test38_Update_AsociadoExistenteConDatosValidos() throws SQLException {
        // En BaseDeDatos.ejecutarActualizacion, el parámetro 8 está cableado a 1 (id = 1)
        AsociadoDTO datosActualizados = new AsociadoDTO("Juan Modificado", "State Nuevo", "12345678",
                "Necochea", "Calle Nueva", 999, "2262112233");

        baseDeDatos.ejecutarActualizacion(SQL_UPDATE, datosActualizados);

        AsociadoDTO persistido = buscarPorId(1);
        assertNotNull("El asociado con ID 1 debe existir tras la actualización", persistido);
        assertEquals("Juan Modificado", persistido.getNombre());
        assertEquals("State Nuevo", persistido.getApellido());
        assertEquals("12345678", persistido.getDni());
        assertEquals("Necochea", persistido.getCiudad());
        assertEquals("Calle Nueva", persistido.getCalle());
        assertEquals(999, persistido.getNumero());
        assertEquals("2262112233", persistido.getTelefono());
    }

    // 5.5.2. Datos inválidos
    @Test
    public void test39_Update_DniDuplicadoLanzaSQLExceptionYNoModificaRegistro() throws SQLException {
        // Intentar actualizar el asociado con ID 1 asignándole el DNI del asociado con ID 2 ('87654321')
        String dniExistenteEnOtroRegistro = "87654321";
        AsociadoDTO dtoInvalido = new AsociadoDTO("Juan", "State", dniExistenteEnOtroRegistro,
                "Ciudad", "Calle", 100, "123456");

        try {
            baseDeDatos.ejecutarActualizacion(SQL_UPDATE, dtoInvalido);
            fail("Actualizar con un DNI existente en otro registro debe violar UNIQUE y lanzar SQLException");
        } catch (SQLException e) {
            assertNotNull(e.getSQLState());
        }

        // Verificar que el registro original permanece intacto
        AsociadoDTO id1 = buscarPorId(1);
        assertEquals("12345678", id1.getDni());
        assertEquals("Buenos Aires", id1.getCalle());
    }

    @Test
    public void test40_Update_NombreExcedeLongitudLanzaSQLException() throws SQLException {
        String nombreLargo = generarCadena(31, 'X');
        AsociadoDTO dto = new AsociadoDTO(nombreLargo, "State", "12345678", "Ciudad", "Calle", 100, "123456");

        try {
            baseDeDatos.ejecutarActualizacion(SQL_UPDATE, dto);
            fail("Actualizar con un nombre mayor a 30 caracteres debe lanzar SQLException");
        } catch (SQLException e) {
            assertNotNull(e.getMessage());
        }

        // Verificar que no hubo modificación parcial
        AsociadoDTO id1 = buscarPorId(1);
        assertEquals("Juan", id1.getNombre());
    }
    // 5.5.3. Asociados inexistentes
    @Test
    public void test42_Update_AsociadoInexistenteEjecutaCorrectamenteSinModificarOtros() throws SQLException {
        // Consulta que apunta a un ID inexistente ligando el parámetro 8 con valor 1
        String sqlUpdateInexistente = "UPDATE Asociados SET nombre = ?, apellido = ?, DNI = ?, calle = ?, numero = ?, ciudad = ?, telefono = ? WHERE id = 99999 AND ? = 1;";
        AsociadoDTO dto = crearAsociadoValido("99999999");

        // No debe propagar excepciones si ninguna fila coincide
        baseDeDatos.ejecutarActualizacion(sqlUpdateInexistente, dto);

        // Verificar que los datos semilla siguen inalterados
        assertEquals(20, contarRegistros());
        AsociadoDTO id1 = buscarPorId(1);
        assertEquals("Juan", id1.getNombre());
    }

    // =========================================================================
    // 5.6. GESTIÓN DEL CICLO DE VIDA DE LA CONEXIÓN
    // =========================================================================

    @Test
    public void test44_CerrarConexion_CierraEfectivamenteLaConexion() throws SQLException {
        Connection conexion = baseDeDatos.getConexion();
        assertNotNull(conexion);
        assertFalse(conexion.isClosed());

        baseDeDatos.cerrarConexion();

        assertTrue("La conexión debe encontrarse cerrada", conexion.isClosed());

        // Restaurar la instancia para las pruebas posteriores
        resetearInstanciaSingleton();
        baseDeDatos = BaseDeDatos.getInstancia();
    }

    // =========================================================================
    // MÉTODOS AUXILIARES PRIVADOS
    // =========================================================================

    private int contarRegistros() throws SQLException {
        ResultSet rs = baseDeDatos.ejecutarConsulta("SELECT COUNT(*) AS total FROM Asociados;", -1);
        int total = 0;
        if (rs.next()) {
            total = rs.getInt("total");
        }
        rs.close();
        return total;
    }

    private AsociadoDTO buscarPorId(int id) throws SQLException {
        ResultSet rs = baseDeDatos.ejecutarConsulta(SQL_SELECT_POR_ID, id);
        AsociadoDTO dto = null;
        if (rs.next()) {
            dto = mapearResultSet(rs);
        }
        rs.close();
        return dto;
    }

    private AsociadoDTO buscarPorDni(String dni) throws SQLException {
        // Se ejecuta consulta sin parámetros vinculados por id (-1) filtrando por DNI
        ResultSet rs = baseDeDatos.ejecutarConsulta("SELECT * FROM Asociados WHERE DNI = '" + dni + "';", -1);
        AsociadoDTO dto = null;
        if (rs.next()) {
            dto = mapearResultSet(rs);
        }
        rs.close();
        return dto;
    }

    private AsociadoDTO mapearResultSet(ResultSet rs) throws SQLException {
        return new AsociadoDTO(
                rs.getString("nombre"),
                rs.getString("apellido"),
                rs.getString("DNI"),
                rs.getString("ciudad"),
                rs.getString("calle"),
                rs.getInt("numero"),
                rs.getString("telefono")
        );
    }

    private AsociadoDTO crearAsociadoValido(String dni) {
        return new AsociadoDTO(
                "Carlos",
                "Perez",
                dni,
                "Mar del Plata",
                "San Martin",
                1234,
                "2235001122"
        );
    }

    private String generarCadena(int longitud, char c) {
        StringBuilder sb = new StringBuilder(longitud);
        for (int i = 0; i < longitud; i++) {
            sb.append(c);
        }
        return sb.toString();
    }

    private static void resetearInstanciaSingleton() {
        try {
            Field campoInstancia = BaseDeDatos.class.getDeclaredField("instancia");
            campoInstancia.setAccessible(true);
            campoInstancia.set(null, null);
        } catch (Exception e) {
            throw new RuntimeException("Error al resetear la instancia singleton de BaseDeDatos", e);
        }
    }
}
