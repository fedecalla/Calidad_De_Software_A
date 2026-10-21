package integracion;

import modelo.modeloDominio.clinica.Clinica;
import modelo.modeloDominio.personas.asociado.Asociado;
import modelo.modeloDominio.util.Domicilio;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;

import org.junit.Assert.*;
import org.junit.Test;
import persistencia.AsociadoDTO;
import persistencia.DataAccessObject;

import java.sql.SQLException;
import java.util.*;

public class CrudTest {


    private static Clinica entidad;
    private static DataAccessObject testDao;
    static HashMap<Integer,String> ingresados;
    //CRUD --> traer, eliminar, crear,

    //TODOS LOS METODOS DE CREACION DE LA BASE DE DATOS ESTAN TESTEADOS EN LAS PRUEBAS DE UNIDAD DE PERSISTENCIA


    @BeforeClass
    public static void Init(){

        entidad = Clinica.getInstancia(); //entidad que unifica la integracion de las funcionalidades del sistema
        testDao = new DataAccessObject(); //entidad que me servira para el
        ingresados = new HashMap<>();
                ingresados.put(1, "Juan State");
                ingresados.put(2, "Maria Template");
                ingresados.put(3, "Carlos Observer");
                ingresados.put(4, "Jose Concurrencia");
                        ingresados.put(5, "Xin Dao");
                        ingresados.put(6, "Ana Facade");
                        ingresados.put(7, "Laura Singleton");
                        ingresados.put(8, "Martin Factory");
                        ingresados.put(9, "Sofia Builder");
                        ingresados.put(10, "David Adapter");
                        ingresados.put(11, "Elena Decorator");
                        ingresados.put(12, "Pedro Proxy");
                        ingresados.put(13, "Lucia Strategy");
                        ingresados.put(14, "Diego Command");
                        ingresados.put(15, "Valeria Iterator");
                        ingresados.put(16, "Miguel Mediator");
                        ingresados.put(17, "Clara Memento");
                        ingresados.put(18, "Sergio Visitor");
                        ingresados.put(19, "Paula Bridge");
                        ingresados.put(20, "Adrian Composite");

    }

    @Before
    public void setDependencias(){

    }


    @Test
    public void testGetAsociados() throws SQLException{
        // llamar a la clinica para insertar un asociado
        ArrayList<AsociadoDTO> fetched = entidad.getAsociadosDTO();
        int j =0;
        for(int i = 1; i<21; i++){
            Assert.assertEquals(ingresados.get(i),(fetched.get(j).getNombre())+" "+(fetched.get(j).getApellido()));
            j++;
        }
    }
    @Test
    public void testGuardarAsociado() throws SQLException { // devuelvo la excepcion aca por que no es testeable

        entidad.guardarNuevoAsociado(new AsociadoDTO(
                "Salvador",
                "Munduteguy",
                "47156529",
                "Mar del Plata",
                "Gral. Roca",
                759,
                "2236111529"
        ));
        assertEquals("el sistema no guardo el asociado correcto", "47156529",testDao.cargar(21).getDni());
    }
    @Before
    public void SetUpEliminarAsociado(){

    }

    //TEST PARA LA BAJA DE ASOCIADOS DE LA BASE DE DATOS
    //CORRE A PARTIR DE LA FUNCION DE LA CLASE CLINICA -> Public void eliminarAsociado(String dni) throws SQLException
    @Test
    public void testEliminarAsociado() throws SQLException{

    testDao.guardar(new Asociado("federico", "calla","12345678","2231234567",new Domicilio("paunero",4000,"mar del plata")));



    }



}
