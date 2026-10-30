package persistencia;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Utilidades compartidas por las suites de integracion contra MySQL.
 * Solo se usan desde los tests: no modifican el codigo de produccion.
 */
final class UtilBD {

	static final int CANTIDAD_SEMILLA = 20;

	private UtilBD() {}

	/**
	 * Deja el Singleton con una conexion abierta y la tabla Asociados con los datos semilla.
	 * Si un test anterior cerro la conexion, se descarta la instancia para que se cree una nueva.
	 */
	static BaseDeDatos prepararBD() throws Exception {
		BaseDeDatos bd = BaseDeDatos.getInstancia();
		if (bd.getConexion() == null || bd.getConexion().isClosed()) {
			resetSingleton();
			bd = BaseDeDatos.getInstancia();
		}
		bd.iniciarBD();
		return bd;
	}

	/** Cierra la conexion actual (si existe) y pone en null la instancia unica por reflexion. */
	static void resetSingleton() throws Exception {
		Field campo = BaseDeDatos.class.getDeclaredField("instancia");
		campo.setAccessible(true);
		BaseDeDatos actual = (BaseDeDatos) campo.get(null);
		if (actual != null && actual.getConexion() != null && !actual.getConexion().isClosed()) {
			actual.getConexion().close();
		}
		campo.set(null, null);
	}

	static int contarFilas(Connection conexion) throws SQLException {
		try (Statement stmt = conexion.createStatement();
			 ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM Asociados")) {
			rs.next();
			return rs.getInt(1);
		}
	}

	/** Devuelve la fila con el dni dado leida por JDBC directo, o null si no existe. */
	static AsociadoDTO buscarPorDni(Connection conexion, String dni) throws SQLException {
		try (PreparedStatement stmt = conexion.prepareStatement("SELECT * FROM Asociados WHERE dni = ?")) {
			stmt.setString(1, dni);
			try (ResultSet rs = stmt.executeQuery()) {
				return rs.next() ? mapear(rs) : null;
			}
		}
	}

	/** Devuelve la fila con el id dado leida por JDBC directo, o null si no existe. */
	static AsociadoDTO buscarPorId(Connection conexion, int id) throws SQLException {
		try (PreparedStatement stmt = conexion.prepareStatement("SELECT * FROM Asociados WHERE id = ?")) {
			stmt.setInt(1, id);
			try (ResultSet rs = stmt.executeQuery()) {
				return rs.next() ? mapear(rs) : null;
			}
		}
	}

	private static AsociadoDTO mapear(ResultSet rs) throws SQLException {
		return new AsociadoDTO(rs.getString("nombre"), rs.getString("apellido"), rs.getString("dni"),
				rs.getString("ciudad"), rs.getString("calle"), rs.getInt("numero"), rs.getString("telefono"));
	}
}
