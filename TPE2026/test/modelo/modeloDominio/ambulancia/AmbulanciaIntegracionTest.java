package modelo.modeloDominio.ambulancia;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import modelo.modeloDominio.ambulancia.AmbulanciaMockTest.Recolector;
import modelo.modeloDominio.personas.asociado.Asociado;
import modelo.modeloDominio.personas.operario.Operario;
import modelo.modeloDominio.util.Domicilio;

/**
 * Suite de integracion y concurrencia (Consigna 3).
 * Ambulancia + implementaciones concretas de State + RetornoAutomatico, sin dobles de prueba.
 * Los hilos se crean en el test (sin TiempoMuerto) para que las pruebas sean rapidas y reproducibles.
 */
public class AmbulanciaIntegracionTest {

	private static final long ESPERA_MAX_MS = 10000;

	private Ambulancia ambulancia;
	private Asociado asociado;
	private Operario operario;
	private Recolector recolector;
	private final List<Thread> hilos = new ArrayList<>();

	@BeforeClass
	public static void verificarAsercionesHabilitadas() {
		assertTrue("Ejecutar la JVM con -ea", Ambulancia.class.desiredAssertionStatus());
	}

	@Before
	public void setUp() {
		ambulancia = new Ambulancia();
		recolector = new Recolector();
		ambulancia.addObserver(recolector);
		Domicilio domicilio = new Domicilio("Buenos Aires", 123, "Mar del Plata");
		asociado = new Asociado("Juan", "State", "12345678", "2235551234", domicilio);
		operario = new Operario("Jhon", "Operario", "00000001", "5550001", domicilio, ambulancia);
	}

	@After
	public void tearDown() throws InterruptedException {
		ambulancia.setSimulacionActiva(false);
		for (Thread t : hilos) {
			t.interrupt();
			t.join(ESPERA_MAX_MS);
		}
	}

	private Thread iniciar(Runnable r) {
		Thread t = new Thread(r);
		hilos.add(t);
		t.start();
		return t;
	}

	private void joinTodos() throws InterruptedException {
		for (Thread t : hilos) {
			t.join(ESPERA_MAX_MS);
			assertFalse("Un hilo no termino: posible deadlock o espera sin notificacion", t.isAlive());
		}
	}

	// ESTADO INICIAL

	@Test
	public void testEstadoInicial() {
		assertTrue(ambulancia.getEstadoActual() instanceof DisponibleState);
		assertFalse(ambulancia.isOcupado());
		assertTrue(ambulancia.isSimulacionActiva());
	}

	// FLUJOS COMPLETOS DE LA MAQUINA DE ESTADOS

	@Test
	public void testFlujoAtencionADomicilio() {
		ambulancia.atenderDomicilio(asociado);
		assertTrue(ambulancia.getEstadoActual() instanceof AtendiendoEnDomicilioState);
		assertTrue(ambulancia.isOcupado());

		ambulancia.regresarSinPaciente();
		assertTrue(ambulancia.getEstadoActual() instanceof RegresandoClinicaSinPacienteState);
		assertFalse(ambulancia.isOcupado());

		ambulancia.retornoAutomatico(new RetornoAutomatico(ambulancia));
		assertTrue(ambulancia.getEstadoActual() instanceof DisponibleState);
	}

	@Test
	public void testFlujoTrasladoALaClinica() {
		ambulancia.trasladarALaClinica(asociado);
		assertTrue(ambulancia.getEstadoActual() instanceof TrasladandoPacienteClinicaState);
		assertTrue(ambulancia.isOcupado());

		ambulancia.regresarSinPaciente();
		State siguiente = ambulancia.getEstadoActual();
		assertTrue("Tras el traslado la ambulancia quedo en " + siguiente.getClass().getSimpleName(),
				siguiente instanceof RegresandoClinicaSinPacienteState || siguiente instanceof DisponibleState);
		assertFalse(ambulancia.isOcupado());
	}

	@Test
	public void testFlujoMantenimiento() {
		ambulancia.solicitarMantenimiento(operario);
		assertTrue(ambulancia.getEstadoActual() instanceof EnTallerState);
		assertTrue(ambulancia.isOcupado());

		ambulancia.volviendoDelTaller();
		assertFalse("La ambulancia sigue en el taller", ambulancia.getEstadoActual() instanceof EnTallerState);
		assertFalse(ambulancia.isOcupado());

		ambulancia.retornoAutomatico(new RetornoAutomatico(ambulancia));
		assertTrue(ambulancia.getEstadoActual() instanceof DisponibleState);
	}

	@Test
	public void testSolicitudAceptadaDespuesDelTallerCambiaElEstado() {
		ambulancia.solicitarMantenimiento(operario);
		ambulancia.volviendoDelTaller();
		assertFalse("La ambulancia figura libre", ambulancia.isOcupado());

		ambulancia.atenderDomicilio(asociado);

		assertEquals("Se notifico la atencion como aceptada", 2, recolector.contar("OK"));
		assertTrue("La solicitud se acepto pero el estado quedo en "
						+ ambulancia.getEstadoActual().getClass().getSimpleName() + ": la atencion se pierde",
				ambulancia.getEstadoActual() instanceof AtendiendoEnDomicilioState);
	}

	@Test
	public void testEstadoNoCambiaAnteSolicitudInvalida() {
		ambulancia.setState(new EnTallerState(ambulancia));
		ambulancia.trasladarALaClinica(asociado);
		assertTrue(ambulancia.getEstadoActual() instanceof EnTallerState);
	}

	// CONCURRENCIA

	@Test(timeout = 30000)
	public void testExclusionMutuaEntreSolicitantes() throws InterruptedException {
		final int cantHilos = 5;
		final int ciclos = 20;
		final AtomicInteger enServicio = new AtomicInteger();
		final AtomicInteger maximoSimultaneo = new AtomicInteger();
		final AtomicInteger servicios = new AtomicInteger();

		for (int i = 0; i < cantHilos; i++) {
			iniciar(() -> {
				for (int c = 0; c < ciclos; c++) {
					ambulancia.atenderDomicilio(asociado);
					int actuales = enServicio.incrementAndGet();
					maximoSimultaneo.accumulateAndGet(actuales, Math::max);
					Thread.yield();
					servicios.incrementAndGet();
					enServicio.decrementAndGet();
					ambulancia.regresarSinPaciente();
				}
			});
		}
		joinTodos();

		assertEquals("Mas de un solicitante uso la ambulancia a la vez", 1, maximoSimultaneo.get());
		assertEquals(cantHilos * ciclos, servicios.get());
		assertEquals(cantHilos * ciclos, recolector.contar("OK"));
		assertFalse(ambulancia.isOcupado());
	}

	@Test(timeout = 30000)
	public void testEstadoCoherenteConElServicioBajoConcurrencia() throws InterruptedException {
		final List<String> inconsistencias = Collections.synchronizedList(new ArrayList<String>());

		for (int i = 0; i < 2; i++) {
			iniciar(() -> {
				for (int c = 0; c < 4; c++) {
					boolean domicilio = c % 2 == 0;
					if (domicilio) {
						ambulancia.atenderDomicilio(asociado);
					} else {
						ambulancia.trasladarALaClinica(asociado);
					}
					// mientras ocupado == true ningun otro hilo puede cambiar el estado
					State actual = ambulancia.getEstadoActual();
					boolean coherente = domicilio ? actual instanceof AtendiendoEnDomicilioState
							: actual instanceof TrasladandoPacienteClinicaState;
					if (!coherente) {
						inconsistencias.add((domicilio ? "domicilio" : "traslado") + " -> " + actual.getClass().getSimpleName());
					}
					ambulancia.regresarSinPaciente();
				}
			});
		}
		joinTodos();

		assertTrue("Servicios aceptados con un estado incoherente: " + inconsistencias, inconsistencias.isEmpty());
	}

	@Test
	public void testFinDeSimulacionLiberaATodosLosQueEsperan() throws InterruptedException {
		ambulancia.setOcupado(true);
		for (int i = 0; i < 3; i++) {
			iniciar(() -> ambulancia.atenderDomicilio(asociado));
		}
		iniciar(() -> ambulancia.solicitarMantenimiento(operario));
		Thread.sleep(300);

		ambulancia.setSimulacionActiva(false);

		joinTodos();
		assertTrue(ambulancia.getEstadoActual() instanceof DisponibleState);
		assertEquals(0, recolector.contar("OK"));
	}

	@Test
	public void testVerificarYFinalizarSimulacionConAsociadosTerminados() {
		Domicilio domicilio = new Domicilio("Colon", 456, "Mar del Plata");
		new Asociado("Carlos", "Observer", "11223344", "2235558765", domicilio, 0, ambulancia);
		new Asociado("Xin", "Dao", "33445566", "2235554321", domicilio, 0, ambulancia);

		ambulancia.verificarYFinalizarSimulacion();

		assertFalse(ambulancia.isSimulacionActiva());
		assertEquals(1, recolector.contar("INFO"));
	}

	@Test
	public void testVerificarYFinalizarSimulacionConAsociadosPendientes() {
		Domicilio domicilio = new Domicilio("Colon", 456, "Mar del Plata");
		new Asociado("Carlos", "Observer", "11223344", "2235558765", domicilio, 0, ambulancia);
		new Asociado("Xin", "Dao", "33445566", "2235554321", domicilio, 1, ambulancia);

		ambulancia.verificarYFinalizarSimulacion();

		assertTrue(ambulancia.isSimulacionActiva());
		assertEquals(0, recolector.contar("INFO"));
	}

	// RETORNO AUTOMATICO (hilo independiente)

	@Test(timeout = 15000)
	public void testRetornoAutomaticoDevuelveLaAmbulanciaALaClinica() throws InterruptedException {
		ambulancia.atenderDomicilio(asociado);
		iniciar(new RetornoAutomatico(ambulancia));

		// INTERVALO_RETORNO = 10 s
		long limite = System.currentTimeMillis() + 12000;
		while (!(ambulancia.getEstadoActual() instanceof RegresandoClinicaSinPacienteState)
				&& System.currentTimeMillis() < limite) {
			Thread.sleep(100);
		}
		assertTrue("El retorno automatico no cambio el estado; quedo en "
						+ ambulancia.getEstadoActual().getClass().getSimpleName(),
				ambulancia.getEstadoActual() instanceof RegresandoClinicaSinPacienteState);
	}

}
