package integracion;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import persistencia.BaseDeDatosTest;
import persistencia.DataAccessObjectTest;

@RunWith(Suite.class)
@Suite.SuiteClasses({
        BaseDeDatosTest.class,
        DataAccessObjectTest.class,
        CrudTest.class
})
public class IntegrationSuite {
}
