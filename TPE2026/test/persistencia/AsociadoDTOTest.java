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
                "Federico", "Perez", "12345678", "Mar del Plata",
                "San Martin", 1234, "2234579863");

        assertEquals("Federico", dto.getNombre());
        assertEquals("Perez", dto.getApellido());
        assertEquals("12345678", dto.getDni());
        assertEquals("Mar del Plata", dto.getCiudad());
        assertEquals("San Martin", dto.getCalle());
        assertEquals(1234, dto.getNumero());
        assertEquals("2234579863", dto.getTelefono());
    }

    @Test
    public void settersValidosAsignanLosAtributos() {
        asociadoDTO.setNombre("Federico");
        asociadoDTO.setApellido("Perez");
        asociadoDTO.setDni("12345678");
        asociadoDTO.setCiudad("Mar del Plata");
        asociadoDTO.setCalle("San Martin");
        asociadoDTO.setNumero(1234);
        asociadoDTO.setTelefono("2234579863");

        assertEquals("Federico", asociadoDTO.getNombre());
        assertEquals("Perez", asociadoDTO.getApellido());
        assertEquals("12345678", asociadoDTO.getDni());
        assertEquals("Mar del Plata", asociadoDTO.getCiudad());
        assertEquals("San Martin", asociadoDTO.getCalle());
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
