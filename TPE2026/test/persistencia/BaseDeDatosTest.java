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
        assertTrue("La tabla Asociados debe existir y tener al menos un registro", rs.next());
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
        assertEquals("La inicialización debe insertar exactamente 20 registros semilla",
                20, totalRegistros);
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
        assertEquals("Luego del reseteo debe haber exactamente 20 registros semilla",
                20, contarRegistros());
        assertNull("El registro adicional 1 debe haber sido eliminado", buscarPorDni("99000001"));
        assertNull("El registro adicional 2 debe haber sido eliminado", buscarPorDni("99000002"));

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

    // 5.2.2. Datos inválidos
    @Test
    public void test13_Consulta_SintaxisSqlInvalidaLanzaSQLException() {
        try {
            baseDeDatos.ejecutarConsulta("SELECT FROM Asociados WHERE;", -1);
            fail("Una consulta con sintaxis inválida debe lanzar SQLException");
        } catch (SQLException e) {
            assertNotNull("La excepción debe contener SQLState", e.getSQLState());
        }
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

    @Test
    public void test17_Consulta_PorIdNegativoInexistenteRetornaVacioSinExcepcion() throws SQLException {
        // ID negativo (diferente de -1) se vincula como parámetro
        ResultSet rs = baseDeDatos.ejecutarConsulta(SQL_SELECT_POR_ID, -50);
        assertNotNull("El ResultSet no debe ser nulo para ID negativo", rs);
        assertFalse("El ResultSet no debe contener filas para un ID negativo", rs.next());
        rs.close();
    }

    // 5.2.4. Asociados existentes
    @Test
    public void test18_Consulta_AsociadoExistenteVerificaIntegridadCompleta() throws SQLException {
        AsociadoDTO asociado = buscarPorId(6);

        assertNotNull("El asociado con ID 6 debe existir", asociado);
        assertEquals("Ana", asociado.getNombre());
        assertEquals("Facade", asociado.getApellido());
        assertEquals("66554433", asociado.getDni());
        assertEquals("San Martin", asociado.getCalle());
        assertEquals(321, asociado.getNumero());
        assertEquals("Mar del Plata", asociado.getCiudad());
        assertEquals("2235556789", asociado.getTelefono());
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
    public void test20_Insert_NombreVacioPermitidoPorEsquemaMySQL() throws SQLException {
        String dni = "55000001";
        AsociadoDTO dto = new AsociadoDTO("", "Valido", dni, "Ciudad", "Calle", 100, "123456");

        baseDeDatos.ejecutarInsert(SQL_INSERT, dto);

        AsociadoDTO recuperado = buscarPorDni(dni);
        assertNotNull("El registro con nombre vacío debe almacenarse según el esquema MySQL", recuperado);
        assertEquals("", recuperado.getNombre());
    }

    @Test
    public void test21_Insert_NombreNullPermitidoPorEsquemaMySQL() throws SQLException {
        String dni = "55000002";
        AsociadoDTO dto = new AsociadoDTO(null, "Valido", dni, "Ciudad", "Calle", 100, "123456");

        baseDeDatos.ejecutarInsert(SQL_INSERT, dto);

        AsociadoDTO recuperado = buscarPorDni(dni);
        assertNotNull("El registro con nombre NULL debe almacenarse al no ser columna NOT NULL", recuperado);
        assertNull(recuperado.getNombre());
    }

    @Test
    public void test22_Insert_NombreExcedeLongitudLanzaSQLException() {
        String nombreLargo = generarCadena(31, 'A'); // Límite es VARCHAR(30)
        AsociadoDTO dto = new AsociadoDTO(nombreLargo, "Valido", "55000003", "Ciudad", "Calle", 100, "123456");

        try {
            baseDeDatos.ejecutarInsert(SQL_INSERT, dto);
            fail("Insertar un nombre que exceda VARCHAR(30) debe lanzar SQLException por Data Truncation");
        } catch (SQLException e) {
            assertNotNull(e.getMessage());
        }
    }

    @Test
    public void test23_Insert_ApellidoVacioPermitidoPorEsquemaMySQL() throws SQLException {
        String dni = "55000004";
        AsociadoDTO dto = new AsociadoDTO("Valido", "", dni, "Ciudad", "Calle", 100, "123456");

        baseDeDatos.ejecutarInsert(SQL_INSERT, dto);

        AsociadoDTO recuperado = buscarPorDni(dni);
        assertNotNull(recuperado);
        assertEquals("", recuperado.getApellido());
    }

    @Test
    public void test24_Insert_ApellidoNullPermitidoPorEsquemaMySQL() throws SQLException {
        String dni = "55000005";
        AsociadoDTO dto = new AsociadoDTO("Valido", null, dni, "Ciudad", "Calle", 100, "123456");

        baseDeDatos.ejecutarInsert(SQL_INSERT, dto);

        AsociadoDTO recuperado = buscarPorDni(dni);
        assertNotNull(recuperado);
        assertNull(recuperado.getApellido());
    }

    @Test
    public void test25_Insert_ApellidoExcedeLongitudLanzaSQLException() {
        String apellidoLargo = generarCadena(31, 'B'); // Límite es VARCHAR(30)
        AsociadoDTO dto = new AsociadoDTO("Valido", apellidoLargo, "55000006", "Ciudad", "Calle", 100, "123456");

        try {
            baseDeDatos.ejecutarInsert(SQL_INSERT, dto);
            fail("Insertar un apellido que exceda VARCHAR(30) debe lanzar SQLException");
        } catch (SQLException e) {
            assertNotNull(e.getMessage());
        }
    }

    @Test
    public void test26_Insert_DniExcedeLongitudLanzaSQLException() {
        String dniLargo = generarCadena(11, '9'); // Límite es VARCHAR(10)
        AsociadoDTO dto = new AsociadoDTO("Valido", "Valido", dniLargo, "Ciudad", "Calle", 100, "123456");

        try {
            baseDeDatos.ejecutarInsert(SQL_INSERT, dto);
            fail("Insertar un DNI que exceda VARCHAR(10) debe lanzar SQLException");
        } catch (SQLException e) {
            assertNotNull(e.getMessage());
        }
    }

    @Test
    public void test27_Insert_CalleExcedeLongitudLanzaSQLException() {
        String calleLarga = generarCadena(31, 'C'); // Límite es VARCHAR(30)
        AsociadoDTO dto = new AsociadoDTO("Valido", "Valido", "55000007", "Ciudad", calleLarga, 100, "123456");

        try {
            baseDeDatos.ejecutarInsert(SQL_INSERT, dto);
            fail("Insertar una calle que exceda VARCHAR(30) debe lanzar SQLException");
        } catch (SQLException e) {
            assertNotNull(e.getMessage());
        }
    }

    @Test
    public void test28_Insert_CiudadExcedeLongitudLanzaSQLException() {
        String ciudadLarga = generarCadena(36, 'D'); // Límite es VARCHAR(35)
        AsociadoDTO dto = new AsociadoDTO("Valido", "Valido", "55000008", ciudadLarga, "Calle", 100, "123456");

        try {
            baseDeDatos.ejecutarInsert(SQL_INSERT, dto);
            fail("Insertar una ciudad que exceda VARCHAR(35) debe lanzar SQLException");
        } catch (SQLException e) {
            assertNotNull(e.getMessage());
        }
    }

    @Test
    public void test29_Insert_TelefonoExcedeLongitudLanzaSQLException() {
        String telefonoLargo = generarCadena(16, '5'); // Límite es VARCHAR(15)
        AsociadoDTO dto = new AsociadoDTO("Valido", "Valido", "55000009", "Ciudad", "Calle", 100, telefonoLargo);

        try {
            baseDeDatos.ejecutarInsert(SQL_INSERT, dto);
            fail("Insertar un teléfono que exceda VARCHAR(15) debe lanzar SQLException");
        } catch (SQLException e) {
            assertNotNull(e.getMessage());
        }
    }

    @Test
    public void test30_Insert_ConsultaInvalidaLanzaSQLException() {
        AsociadoDTO dto = crearAsociadoValido("55000010");
        try {
            baseDeDatos.ejecutarInsert("INSERT INTO TablaInexistente VALUES (?, ?, ?, ?, ?, ?, ?);", dto);
            fail("Ejecutar un INSERT sobre tabla inexistente debe lanzar SQLException");
        } catch (SQLException e) {
            assertNotNull(e.getMessage());
        }
    }

    // 5.3.3. Asociados inexistentes
    @Test
    public void test31_Insert_AsociadoInexistenteQuedaAlmacenadoCorrectamente() throws SQLException {
        String dni = "77112233";
        assertNull("El asociado no debe existir previamente", buscarPorDni(dni));

        AsociadoDTO nuevo = crearAsociadoValido(dni);
        baseDeDatos.ejecutarInsert(SQL_INSERT, nuevo);

        assertEquals("La cantidad total de registros debe incrementarse a 21",
                21, contarRegistros());
        assertNotNull("El nuevo asociado debe estar persistido", buscarPorDni(dni));
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
        assertEquals("La cantidad total de registros debe ser 19 tras la eliminación",
                19, contarRegistros());
    }

    // 5.4.2. Datos inválidos
    @Test
    public void test34_Delete_ConDniVacioNoEliminaRegistrosAccidentalmente() throws SQLException {
        int cantidadInicial = contarRegistros();

        baseDeDatos.ejecutarDelete(SQL_DELETE_POR_DNI, "");

        assertEquals("Eliminar con DNI vacío no debe alterar la cantidad de registros",
                cantidadInicial, contarRegistros());
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

        assertEquals("Ningún registro debe haber sido eliminado",
                cantidadInicial, contarRegistros());
    }

    // 5.4.4. Asociados existentes
    @Test
    public void test37_Delete_AsociadoPreviamenteInsertado() throws SQLException {
        String dni = "88998899";
        AsociadoDTO dto = crearAsociadoValido(dni);
        baseDeDatos.ejecutarInsert(SQL_INSERT, dto);
        assertEquals(21, contarRegistros());

        baseDeDatos.ejecutarDelete(SQL_DELETE_POR_DNI, dni);

        assertNull("El registro debe haber sido eliminado", buscarPorDni(dni));
        assertEquals(20, contarRegistros());
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

    @Test
    public void test41_Update_DniExcedeLongitudLanzaSQLException() throws SQLException {
        String dniLargo = generarCadena(11, '8');
        AsociadoDTO dto = new AsociadoDTO("Juan", "State", dniLargo, "Ciudad", "Calle", 100, "123456");

        try {
            baseDeDatos.ejecutarActualizacion(SQL_UPDATE, dto);
            fail("Actualizar con un DNI mayor a 10 caracteres debe lanzar SQLException");
        } catch (SQLException e) {
            assertNotNull(e.getMessage());
        }

        AsociadoDTO id1 = buscarPorId(1);
        assertEquals("12345678", id1.getDni());
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

    // 5.5.4. Asociados existentes
    @Test
    public void test43_Update_ModificaExclusivamenteElRegistroIndicado() throws SQLException {
        // Modificar ID 1
        AsociadoDTO nuevosDatos = new AsociadoDTO("NuevoNombre", "NuevoApellido", "12345678",
                "Ciudad", "Calle", 50, "111222");
        baseDeDatos.ejecutarActualizacion(SQL_UPDATE, nuevosDatos);

        // Verificar que ID 1 cambió
        AsociadoDTO id1 = buscarPorId(1);
        assertEquals("NuevoNombre", id1.getNombre());

        // Verificar que ID 2 ("Maria Template") permanece inalterado
        AsociadoDTO id2 = buscarPorId(2);
        assertEquals("Maria", id2.getNombre());
        assertEquals("Template", id2.getApellido());
        assertEquals("87654321", id2.getDni());
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
