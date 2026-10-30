package modelo.modeloDominio.ambulancia;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import modelo.modeloAplicacion.NotificacionSimulacion;
import modelo.modeloDominio.personas.asociado.Asociado;
import modelo.modeloDominio.personas.operario.Operario;
import modelo.modeloDominio.util.Domicilio;

/**
 * Suite con dobles de prueba (Consigna 3 - Testing con Mocks).
 * <ul>
 * <li>Ambulancia real + State simulado: se verifica que la Ambulancia delega en su estado
 * y administra correctamente el recurso compartido (ocupado / wait / notifyAll).</li>
 * <li>Estado concreto real + Ambulancia simulada: se verifica cada transicion de forma atomica.</li>
 * <li>RetornoAutomatico con un State simulado: se verifica la interaccion del hilo con la ambulancia.</li>
 * </ul>
 */
public class AmbulanciaMockTest {

	private static final long ESPERA_MAX_MS = 2000;

	private Ambulancia ambulancia;
	private State estado;
	private Asociado asociado;
	private Operario operario;
	private Recolector recolector;
	private final List<Thread> hilos = new ArrayList<>();

	/** Observer que junta las notificaciones emitidas por la ambulancia. */
	static class Recolector implements Observer {
		final List<NotificacionSimulacion> notificaciones = Collections.synchronizedList(new ArrayList<NotificacionSimulacion>());

		@Override
		public void update(Observable o, Object arg) {
			if (arg instanceof NotificacionSimulacion) {
				notificaciones.add((NotificacionSimulacion) arg);
			}
		}

		long contar(String tipo) {
			synchronized (notificaciones) {
				return notificaciones.stream().filter(n -> tipo.equals(n.getTipo())).count();
			}
		}
	}

	@BeforeClass
	public static void verificarAsercionesHabilitadas() {
		assertTrue("Ejecutar la JVM con -ea", Ambulancia.class.desiredAssertionStatus());
	}

	@Before
	public void setUp() {
		ambulancia = new Ambulancia();
		estado = mock(State.class);
		ambulancia.setState(estado);
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

	private static void esperarEstado(Thread t, Thread.State esperado) throws InterruptedException {
		long limite = System.currentTimeMillis() + ESPERA_MAX_MS;
		while (t.getState() != esperado && System.currentTimeMillis() < limite) {
			Thread.sleep(10);
		}
		assertEquals("El hilo no llego al estado " + esperado, esperado, t.getState());
	}

	// AMBULANCIA REAL + STATE SIMULADO: delegacion

	@Test
	public void testAtenderDomicilioDelegaEnElEstado() {
		ambulancia.atenderDomicilio(asociado);

		verify(estado, times(1)).SolicitudDeAtencionDomicilio();
		verifyNoMoreInteractions(estado);
		assertTrue(ambulancia.isOcupado());
		assertEquals(1, recolector.contar("OK"));
	}

	@Test
	public void testTrasladarALaClinicaDelegaEnElEstado() {
		ambulancia.trasladarALaClinica(asociado);

		verify(estado, times(1)).SolicitudDeTraslado();
		verifyNoMoreInteractions(estado);
		assertTrue(ambulancia.isOcupado());
		assertEquals(1, recolector.contar("OK"));
	}

	@Test
	public void testSolicitarMantenimientoDelegaEnElEstado() {
		ambulancia.solicitarMantenimiento(operario);

		verify(estado, times(1)).SolicitudMantenimiento();
		verifyNoMoreInteractions(estado);
		assertTrue(ambulancia.isOcupado());
		assertEquals(1, recolector.contar("OK"));
	}

	@Test
	public void testVolviendoDelTallerLiberaLaAmbulancia() {
		ambulancia.setOcupado(true);
		ambulancia.volviendoDelTaller();

		verify(estado, times(1)).SolicitudMantenimiento();
		assertFalse(ambulancia.isOcupado());
		assertEquals(1, recolector.contar("INFO"));
	}

	@Test
	public void testRegresarSinPacienteLiberaLaAmbulancia() {
		ambulancia.setOcupado(true);
		ambulancia.regresarSinPaciente();

		verify(estado, times(1)).RetornoClinica();
		assertFalse(ambulancia.isOcupado());
		assertEquals(1, recolector.contar("INFO"));
	}

	@Test
	public void testRetornoAutomaticoDelegaEnElEstado() {
		ambulancia.retornoAutomatico(new RetornoAutomatico(ambulancia));
		verify(estado, times(1)).RetornoClinica();
	}

	@Test
	public void testSimulacionInactivaNoInvocaAlEstado() {
		ambulancia.setSimulacionActiva(false);

		ambulancia.atenderDomicilio(asociado);
		ambulancia.trasladarALaClinica(asociado);
		ambulancia.solicitarMantenimiento(operario);

		verifyNoInteractions(estado);
		assertFalse(ambulancia.isOcupado());
	}

	@Test(expected = AssertionError.class)
	public void testSetStateNulo() {
		ambulancia.setState(null);
	}

	@Test(expected = AssertionError.class)
	public void testAtenderDomicilioAsociadoNulo() {
		ambulancia.atenderDomicilio(null);
	}

	@Test(expected = AssertionError.class)
	public void testSolicitarMantenimientoOperarioNulo() {
		ambulancia.solicitarMantenimiento(null);
	}

	// AMBULANCIA REAL + STATE SIMULADO: recurso compartido y sincronizacion

	@Test
	public void testSolicitudEsperaMientrasLaAmbulanciaEstaOcupada() throws InterruptedException {
		ambulancia.setOcupado(true);
		Thread solicitante = iniciar(() -> ambulancia.atenderDomicilio(asociado));

		esperarEstado(solicitante, Thread.State.WAITING);
		verify(estado, never()).SolicitudDeAtencionDomicilio();
		assertEquals(1, recolector.contar("NO_AMBULANCIA"));

		ambulancia.regresarSinPaciente(); // libera y despierta al solicitante

		verify(estado, timeout(ESPERA_MAX_MS)).SolicitudDeAtencionDomicilio();
		solicitante.join(ESPERA_MAX_MS);
		assertFalse(solicitante.isAlive());
		assertTrue("Quien obtuvo la ambulancia debe dejarla ocupada", ambulancia.isOcupado());
	}

	@Test
	public void testSoloUnSolicitanteObtieneLaAmbulanciaLiberada() throws InterruptedException {
		ambulancia.setOcupado(true);
		Thread s1 = iniciar(() -> ambulancia.atenderDomicilio(asociado));
		Thread s2 = iniciar(() -> ambulancia.trasladarALaClinica(asociado));
		esperarEstado(s1, Thread.State.WAITING);
		esperarEstado(s2, Thread.State.WAITING);

		ambulancia.regresarSinPaciente();
		Thread.sleep(300);

		int atendidos = mockingDetails(estado).getInvocations().stream()
				.mapToInt(i -> i.getMethod().getName().startsWith("Solicitud") ? 1 : 0).sum();
		assertEquals("Con una sola liberacion solo un solicitante debe avanzar", 1, atendidos);
		assertTrue(s1.isAlive() ^ s2.isAlive());
	}

	@Test
	public void testFinDeSimulacionDespiertaALosQueEsperan() throws InterruptedException {
		ambulancia.setOcupado(true);
		Thread solicitante = iniciar(() -> ambulancia.atenderDomicilio(asociado));
		Thread mantenimiento = iniciar(() -> ambulancia.solicitarMantenimiento(operario));
		esperarEstado(solicitante, Thread.State.WAITING);
		esperarEstado(mantenimiento, Thread.State.WAITING);

		ambulancia.setSimulacionActiva(false);

		solicitante.join(ESPERA_MAX_MS);
		mantenimiento.join(ESPERA_MAX_MS);
		assertFalse(solicitante.isAlive());
		assertFalse(mantenimiento.isAlive());
		verifyNoInteractions(estado);
	}

	@Test
	public void testInterrupcionDeUnHiloEnEspera() throws InterruptedException {
		ambulancia.setOcupado(true);
		Thread solicitante = iniciar(() -> ambulancia.atenderDomicilio(asociado));
		esperarEstado(solicitante, Thread.State.WAITING);

		solicitante.interrupt();

		solicitante.join(ESPERA_MAX_MS);
		assertFalse(solicitante.isAlive());
		verifyNoInteractions(estado);
	}

	// ESTADOS CONCRETOS REALES + AMBULANCIA SIMULADA: transiciones atomicas

	private static Ambulancia ambulanciaMock() {
		return mock(Ambulancia.class);
	}

	private static void assertTransicion(Ambulancia amb, Class<? extends State> esperado) {
		ArgumentCaptor<State> captor = ArgumentCaptor.forClass(State.class);
		verify(amb, times(1)).setState(captor.capture());
		assertEquals(esperado, captor.getValue().getClass());
	}

	private static void assertSinTransicion(Ambulancia amb) {
		verify(amb, never()).setState(any(State.class));
	}

	@Test
	public void testDisponibleAnteTraslado() {
		Ambulancia amb = ambulanciaMock();
		new DisponibleState(amb).SolicitudDeTraslado();
		assertTransicion(amb, TrasladandoPacienteClinicaState.class);
	}

	@Test
	public void testDisponibleAnteDomicilio() {
		Ambulancia amb = ambulanciaMock();
		new DisponibleState(amb).SolicitudDeAtencionDomicilio();
		assertTransicion(amb, AtendiendoEnDomicilioState.class);
	}

	@Test
	public void testDisponibleAnteMantenimiento() {
		Ambulancia amb = ambulanciaMock();
		new DisponibleState(amb).SolicitudMantenimiento();
		assertTransicion(amb, EnTallerState.class);
	}

	@Test
	public void testDisponibleAnteRetorno() {
		Ambulancia amb = ambulanciaMock();
		new DisponibleState(amb).RetornoClinica();
		assertSinTransicion(amb);
	}

	@Test
	public void testAtendiendoEnDomicilioRechazaSolicitudes() {
		Ambulancia amb = ambulanciaMock();
		State s = new AtendiendoEnDomicilioState(amb);
		s.SolicitudDeTraslado();
		s.SolicitudDeAtencionDomicilio();
		s.SolicitudMantenimiento();
		assertSinTransicion(amb);
	}

	@Test
	public void testAtendiendoEnDomicilioAnteRetorno() {
		Ambulancia amb = ambulanciaMock();
		new AtendiendoEnDomicilioState(amb).RetornoClinica();
		assertTransicion(amb, RegresandoClinicaSinPacienteState.class);
	}

	@Test
	public void testTrasladandoRechazaSolicitudes() {
		Ambulancia amb = ambulanciaMock();
		State s = new TrasladandoPacienteClinicaState(amb);
		s.SolicitudDeTraslado();
		s.SolicitudDeAtencionDomicilio();
		s.SolicitudMantenimiento();
		assertSinTransicion(amb);
	}

	@Test
	public void testTrasladandoAnteRetorno() {
		Ambulancia amb = ambulanciaMock();
		new TrasladandoPacienteClinicaState(amb).RetornoClinica();

		ArgumentCaptor<State> captor = ArgumentCaptor.forClass(State.class);
		verify(amb, times(1)).setState(captor.capture());
		State siguiente = captor.getValue();
		assertTrue("Tras dejar al paciente la ambulancia no vuelve del taller; paso a " + siguiente.getClass().getSimpleName(),
				siguiente instanceof DisponibleState || siguiente instanceof RegresandoClinicaSinPacienteState);
	}

	@Test
	public void testEnTallerRechazaSolicitudes() {
		Ambulancia amb = ambulanciaMock();
		State s = new EnTallerState(amb);
		s.SolicitudDeTraslado();
		s.SolicitudDeAtencionDomicilio();
		s.RetornoClinica();
		assertSinTransicion(amb);
	}

	@Test
	public void testEnTallerAnteMantenimiento() {
		Ambulancia amb = ambulanciaMock();
		new EnTallerState(amb).SolicitudMantenimiento();
		assertTransicion(amb, RegresandoDelTallerState.class);
	}

	@Test
	public void testRegresandoDelTallerRechazaSolicitudes() {
		Ambulancia amb = ambulanciaMock();
		State s = new RegresandoDelTallerState(amb);
		s.SolicitudDeTraslado();
		s.SolicitudDeAtencionDomicilio();
		s.SolicitudMantenimiento();
		assertSinTransicion(amb);
	}

	@Test
	public void testRegresandoDelTallerAnteRetorno() {
		Ambulancia amb = ambulanciaMock();
		new RegresandoDelTallerState(amb).RetornoClinica();
		assertTransicion(amb, DisponibleState.class);
	}

	@Test
	public void testRegresandoSinPacienteAnteTraslado() {
		Ambulancia amb = ambulanciaMock();
		new RegresandoClinicaSinPacienteState(amb).SolicitudDeTraslado();
		assertTransicion(amb, TrasladandoPacienteClinicaState.class);
	}

	@Test
	public void testRegresandoSinPacienteAnteDomicilio() {
		Ambulancia amb = ambulanciaMock();
		new RegresandoClinicaSinPacienteState(amb).SolicitudDeAtencionDomicilio();
		assertTransicion(amb, AtendiendoEnDomicilioState.class);
	}

	@Test
	public void testRegresandoSinPacienteRechazaMantenimiento() {
		Ambulancia amb = ambulanciaMock();
		new RegresandoClinicaSinPacienteState(amb).SolicitudMantenimiento();
		assertSinTransicion(amb);
	}

	@Test
	public void testRegresandoSinPacienteAnteRetorno() {
		Ambulancia amb = ambulanciaMock();
		new RegresandoClinicaSinPacienteState(amb).RetornoClinica();
		assertTransicion(amb, DisponibleState.class);
	}

	// RETORNO AUTOMATICO + STATE SIMULADO

	@Test
	public void testRetornoAutomaticoSigueActivoDuranteLaSimulacion() throws InterruptedException {
		Thread retorno = iniciar(new RetornoAutomatico(ambulancia));
		Thread.sleep(500);
		assertTrue("El hilo de retorno automatico termino con la simulacion todavia activa", retorno.isAlive());
	}

	@Test(timeout = 15000)
	public void testRetornoAutomaticoInvocaRetornoClinica() {
		iniciar(new RetornoAutomatico(ambulancia));
		// INTERVALO_RETORNO = 10 s
		verify(estado, timeout(12000).atLeastOnce()).RetornoClinica();
	}

	@Test
	public void testRetornoAutomaticoTerminaSiLaSimulacionNoEstaActiva() throws InterruptedException {
		ambulancia.setSimulacionActiva(false);
		Thread retorno = iniciar(new RetornoAutomatico(ambulancia));
		retorno.join(ESPERA_MAX_MS);
		assertFalse("Con la simulacion finalizada el hilo no debe seguir ejecutandose", retorno.isAlive());
		verifyNoInteractions(estado);
	}

	@Test
	public void testRetornoAutomaticoTerminaAlSerInterrumpido() throws InterruptedException {
		Thread retorno = iniciar(new RetornoAutomatico(ambulancia));
		Thread.sleep(200);
		retorno.interrupt();
		retorno.join(ESPERA_MAX_MS);
		assertFalse(retorno.isAlive());
	}

}
