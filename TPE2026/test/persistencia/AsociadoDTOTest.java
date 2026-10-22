package persistencia;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class AsociadoDTOTest {

    private AsociadoDTO asociadoDTO;

    @Before
    public void setUp() {
        asociadoDTO = new AsociadoDTO();
    }

    @Test
    public void constructorParametrizadoInicializaTodosLosAtributos() {
        AsociadoDTO dto = new AsociadoDTO(
                "Pedro", "Perez", "12345678", "Mar del Plata",
                "Paunero", 1234, "2234579863");

        assertEquals("Pedro", dto.getNombre());
        assertEquals("Perez", dto.getApellido());
        assertEquals("12345678", dto.getDni());
        assertEquals("Mar del Plata", dto.getCiudad());
        assertEquals("Paunero", dto.getCalle());
        assertEquals(1234, dto.getNumero());
        assertEquals("2234579863", dto.getTelefono());
    }

    @Test
    public void settersValidosAsignanLosAtributos() {
        asociadoDTO.setNombre("Pedro");
        asociadoDTO.setApellido("Perez");
        asociadoDTO.setDni("12345678");
        asociadoDTO.setCiudad("Mar del Plata");
        asociadoDTO.setCalle("Paunero");
        asociadoDTO.setNumero(1234);
        asociadoDTO.setTelefono("2234579863");

        assertEquals("Pedro", asociadoDTO.getNombre());
        assertEquals("Perez", asociadoDTO.getApellido());
        assertEquals("12345678", asociadoDTO.getDni());
        assertEquals("Mar del Plata", asociadoDTO.getCiudad());
        assertEquals("Paunero", asociadoDTO.getCalle());
        assertEquals(1234, asociadoDTO.getNumero());
        assertEquals("2234579863", asociadoDTO.getTelefono());
    }

    @Test
    public void numeroCeroEsValido() {
        asociadoDTO.setNumero(0);

        assertEquals(0, asociadoDTO.getNumero());
    }

    @Test(expected = AssertionError.class)
    public void nombreNuloEsRechazado() {
        asociadoDTO.setNombre(null);
    }

    @Test(expected = AssertionError.class)
    public void apellidoNuloEsRechazado() {
        asociadoDTO.setApellido(null);
    }

    @Test(expected = AssertionError.class)
    public void dniNuloEsRechazado() {
        asociadoDTO.setDni(null);
    }

    @Test(expected = AssertionError.class)
    public void ciudadNulaEsRechazada() {
        asociadoDTO.setCiudad(null);
    }

    @Test(expected = AssertionError.class)
    public void calleNulaEsRechazada() {
        asociadoDTO.setCalle(null);
    }

    @Test(expected = AssertionError.class)
    public void telefonoNuloEsRechazado() {
        asociadoDTO.setTelefono(null);
    }

    @Test(expected = AssertionError.class)
    public void numeroNegativoEsRechazado() {
        asociadoDTO.setNumero(-1);
    }
}
