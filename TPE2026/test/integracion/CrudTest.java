package integracion;

import modelo.modeloDominio.clinica.Clinica;
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
    static void Init(){
        //aca deberiamos de iniciar el servidor en el cual corre la base de datos
        entidad = Clinica.getInstancia();
        testDao = new DataAccessObject();
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
        for(int i = 1; i<21; i++){
            Assert.assertEquals(ingresados.get(i),fetched.get(i));
        }
    }
    @Test
    public void testCrearAsociado() throws SQLException { // devuelvo la excepcion aca por que no es testeable

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
    @Test
    public void testEliminarAsociado(){

    }



}
