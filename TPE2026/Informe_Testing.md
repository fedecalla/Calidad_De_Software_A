# Informe de Testing: Aplicación Clínica

Trabajo Práctico Final: Estrategia de Testing (Calidad de Software A)

## 1. Alcance y estrategia

| Nivel | Componentes bajo prueba | Suite | Recursos externos |
|---|---|---|---|
| Unitario (atómico) | `AsociadoDTO` | `test/persistencia/AsociadoDTOTest.java` | Ninguno |
| Integración con BD | `AsociadoDTO` + `DataAccessObject` + `BaseDeDatos` | `test/persistencia/BaseDeDatosTest.java`, `test/persistencia/DataAccessObjectTest.java` (helper `UtilBD.java`) | MySQL (`localhost:3306`, `progra_c`/`progra_c`) |
| Unitario con mocks | `Ambulancia`, `State` y estados concretos, `RetornoAutomatico` | `test/modelo/modeloDominio/ambulancia/AmbulanciaMockTest.java` | Ninguno (Mockito) |
| Integración y concurrencia | `Ambulancia` + estados reales + `RetornoAutomatico` + hilos | `test/modelo/modeloDominio/ambulancia/AmbulanciaIntegracionTest.java` | Ninguno |

Criterios aplicados:

- **Oráculo:** el SRS (`SRS_Persistencia_Clinica.pdf`) y los contratos Javadoc (pre/post) del código. Cuando el código contradice la especificación, el test sigue a la especificación y queda en rojo.
- **Aislamiento:** la suite de `AsociadoDTO` no usa ningún otro componente. La de mocks reemplaza `State` (o `Ambulancia`) por dobles de prueba, de modo que cada falla se atribuye a una sola clase.
- **Repetibilidad:** antes de cada test de BD se ejecuta `iniciarBD()`, que recrea la tabla con los 20 registros semilla. Los tests de concurrencia usan hilos propios, sin `TiempoMuerto`, con `join`/`timeout` acotados, y no dependen del azar.
- **Aserciones Java:** las validaciones del sistema son `assert`, así que la JVM tiene que ejecutarse con `-ea` (está configurado en el `pom.xml`). Cada suite lo verifica en `@BeforeClass`.

## 2. Diseño de casos: clases de equivalencia y valores límite

### 2.1 `AsociadoDTO` (consigna 1)

| Atributo | Clase válida | Clase inválida | Valores límite probados |
|---|---|---|---|
| nombre, apellido, dni, ciudad, calle, telefono (`String`) | CV1: cualquier cadena no nula (incluye `""`) | CI1: `null` | `""` (válido), `null` (inválido) |
| numero (`int`) | CV2: `numero >= 0` | CI2: `numero < 0` | `Integer.MIN_VALUE`, `-1` (inválidos); `0`, `1`, `Integer.MAX_VALUE` (válidos) |

| Id | Caso | Clase / límite | Entrada | Resultado esperado | Obtenido |
|---|---|---|---|---|---|
| DTO-01..07 | Setter + getter de cada campo | CV1 / CV2 | valores típicos | se recupera el mismo valor | ✅ |
| DTO-08..14 | Setter con `null` / `-1` | CI1 / CI2 | `null`, `-1` | `AssertionError` | ✅ |
| DTO-15 | `setNumero` | límite inferior válido | `0` | se acepta | ✅ |
| DTO-16 | `setNumero` | límite | `Integer.MIN_VALUE` | `AssertionError` | ✅ |
| DTO-17 | `setNumero` | límite | `1`, `Integer.MAX_VALUE` | se acepta | ✅ |
| DTO-18 | Un setter inválido no altera el estado | CI1 / CI2 | valor previo + `null`/`-1` | se conserva el valor previo | ✅ |
| DTO-19 | Cadenas vacías | CV1 (límite) | `""` | se acepta | ✅ |
| DTO-20 | Constructor por defecto | n/a | n/a | campos en `null`/`0` | ✅ |
| DTO-21 | Constructor parametrizado válido | CV1 + CV2 | 7 valores válidos (`numero = 0` y `123`) | mapeo 1 a 1 | ✅ |
| DTO-22..28 | Constructor parametrizado con un `null` o `numero = -1` | CI1 / CI2 | un campo inválido por caso | `AssertionError` | ❌ **(D1)** |
| DTO-29 | Serialización ida y vuelta | n/a | DTO completo | copia distinta con iguales valores | ✅ |

### 2.2 `BaseDeDatos` (consigna 2)

| Id | Escenario | Clase / límite | Resultado esperado | Obtenido* |
|---|---|---|---|---|
| BD-01 | Singleton secuencial | n/a | misma instancia | ✅ esperado |
| BD-02 | Singleton con 10 hilos simultáneos | concurrencia | una única instancia | ❌ esperado **(B1)** |
| BD-03 | Conexión válida y esquema `Grupo_7` | n/a | `isValid`, `DATABASE() = Grupo_7` | ✅ esperado |
| BD-04 | `cerrarConexion` | n/a | conexión cerrada | ✅ esperado |
| BD-05 | Uso del Singleton después de `cerrarConexion` | n/a | conexión utilizable | ❌ esperado **(B3)** |
| BD-06 | `iniciarBD` crea la tabla y 20 registros (ids 1..20) | n/a | 20 filas | ✅ esperado |
| BD-07 | Primer registro semilla completo | n/a | Juan State… | ✅ esperado |
| BD-08 | Consistencia de ciudades semilla | n/a | todas `"Mar del Plata"` | ❌ esperado **(B4)** |
| BD-09 | `iniciarBD` es idempotente | n/a | vuelve a 20 filas | ✅ esperado |
| BD-10 | DNI único | DNI repetido | `SQLIntegrityConstraintViolationException` | ✅ esperado |
| BD-11 | `ejecutarConsulta` con parámetro / sin parámetro / id inexistente | id válido, `-1`, `999` | 1 / 20 / 0 filas | ✅ esperado |
| BD-12 | Consulta con `?` sin valor, SQL inválido, consulta `null` | inválidas | `SQLException` / `AssertionError` | ✅ esperado |
| BD-13 | `ejecutarInsert` válido | válida | fila persistida con todos los campos | ✅ esperado |
| BD-14 | `ejecutarInsert` con DNI de 10 y 11 caracteres | límite `VARCHAR(10)` | 10 se acepta; 11 da `SQLException` (modo estricto) | ✅ esperado |
| BD-15 | `ejecutarInsert` con argumentos `null` | inválida | `AssertionError` | ✅ esperado |
| BD-16 | `ejecutarActualizacion` sobre el registro id 5 | válida | cambia la fila 5; la fila 1 queda intacta | ❌ esperado **(B2)** |
| BD-17 | `ejecutarActualizacion` con DNI nuevo | válida | no pisa la fila 1 | ❌ esperado **(B2)** |
| BD-18 | `ejecutarDelete` con DNI existente / inexistente | válida | 19 / 20 filas | ✅ esperado |
| BD-19 | `ejecutarDelete` con `' OR '1'='1` | inyección SQL | no borra nada (PreparedStatement) | ✅ esperado |

### 2.3 `DataAccessObject` (consigna 2)

| Id | Escenario | Clase / límite | Resultado esperado | Obtenido* |
|---|---|---|---|---|
| DAO-01 | El DAO usa el Singleton | n/a | `bd == BaseDeDatos.getInstancia()` | ✅ esperado |
| DAO-02 | `guardar` válido | válida | 21 filas, datos correctos | ✅ esperado |
| DAO-03 | `guardar(null)` / DNI duplicado | inválidas | `AssertionError` / `SQLException` | ✅ esperado |
| DAO-04 | `cargarConLimite` | límites `-1`, `0` | `AssertionError` | ✅ esperado |
| DAO-05 | `cargarConLimite` | límites `1`, `5`, `20`, `21` | 1, 5, 20, 20 elementos | ✅ esperado |
| DAO-06 | `cargar(1)`, `cargar(20)` | ids límite válidos | DTO con los datos | ❌ esperado **(O1)** |
| DAO-07 | `cargar(999)` | id inexistente | `null` | ❌ esperado **(O1)** |
| DAO-08 | `cargar(0)`, `cargar(-1)` | inválidas | `AssertionError` | ✅ esperado |
| DAO-09 | `cargarBD` (completa, con un registro nuevo, con la tabla vacía) | n/a | 20 / 21 / 0 elementos, columnas sin cruzar | ✅ esperado |
| DAO-10 | `eliminar` con DNI existente / inexistente / `""` | n/a | 19 / 20 / `AssertionError` | ✅ esperado |
| DAO-11 | `eliminar(null)` validado por el propio DAO | inválida | `AssertionError("El DNI no puede ser nulo")` | ❌ esperado **(O2)** |
| DAO-12 | `actualizar` del asociado id 5 | válida | cambia la fila 5 | ❌ esperado **(B2)** |
| DAO-13 | `actualizar` del asociado id 1 | válida | cambia la fila 1 | ✅ esperado (funciona por coincidencia) |
| DAO-14 | `CrearDTO(Asociado)` / `CrearDTO(int, …)` | válidas e inválidas | mapeo correcto / `AssertionError` | ✅ esperado |
| DAO-15 | `new Asociado(dto)` con `numero = 0` | límite válido según el SRS | se construye | ❌ esperado **(O4)** |
| DAO-16 | `guardar` y luego `cargarBD`, comparando | n/a | asociado equivalente | ✅ esperado |

\* Los tests de BD no se pudieron ejecutar en el entorno donde se escribieron porque no había un servidor MySQL. La columna indica el resultado **esperado** según el análisis del código. Hay que completarla con la ejecución real y la captura correspondiente.

### 2.4 `Ambulancia` / `State` / `RetornoAutomatico` (consigna 3)

**Tabla de transiciones usada como oráculo** (de los Javadoc de cada estado):

| Estado \ evento | Traslado | Domicilio | Mantenimiento | RetornoClinica |
|---|---|---|---|---|
| Disponible | Trasladando | AtendiendoDomicilio | EnTaller | (no cambia) |
| AtendiendoDomicilio | (rechaza) | (rechaza) | (rechaza) | RegresandoSinPaciente |
| Trasladando | (rechaza) | (rechaza) | (rechaza) | Disponible / RegresandoSinPaciente |
| EnTaller | (rechaza) | (rechaza) | RegresandoDelTaller | (rechaza) |
| RegresandoDelTaller | (rechaza) | (rechaza) | (rechaza) | Disponible |
| RegresandoSinPaciente | Trasladando | AtendiendoDomicilio | (rechaza) | Disponible |

**Con mocks (`AmbulanciaMockTest`):**

| Id | Escenario | Doble | Resultado esperado | Obtenido |
|---|---|---|---|---|
| MK-01..06 | Cada operación de `Ambulancia` delega en el método correcto de `State`, una sola vez, y actualiza `ocupado` y las notificaciones | `State` mock | `verify(...)` correcto | ✅ |
| MK-07 | Simulación inactiva | `State` mock | `verifyNoInteractions` | ✅ |
| MK-08..10 | Argumentos `null` | n/a | `AssertionError` | ✅ |
| MK-11 | Solicitud con la ambulancia ocupada: el hilo queda `WAITING`, se notifica `NO_AMBULANCIA` y avanza al liberarse | `State` mock | hilo bloqueado y luego atendido | ✅ |
| MK-12 | Dos hilos esperando y una sola liberación | `State` mock | avanza exactamente uno | ✅ |
| MK-13 | Fin de simulación con hilos esperando | `State` mock | todos terminan sin tocar el estado | ✅ |
| MK-14 | Interrupción de un hilo en espera | `State` mock | termina limpiamente | ✅ |
| MK-15..30 | Cada celda de la tabla de transiciones, con el estado real y `Ambulancia` mock (`ArgumentCaptor` sobre `setState`) | `Ambulancia` mock | transición de la tabla | ✅ salvo MK-22 |
| MK-22 | Trasladando + RetornoClinica | `Ambulancia` mock | Disponible / RegresandoSinPaciente | ❌ **(A2)** |
| MK-31 | `RetornoAutomatico` sigue vivo con la simulación activa | `State` mock | hilo vivo a los 500 ms | ❌ **(A1)** |
| MK-32 | `RetornoAutomatico` invoca `RetornoClinica` (intervalo de 10 s) | `State` mock | invocado en ≤ 12 s | ❌ **(A1)** |
| MK-33 | `RetornoAutomatico` con la simulación inactiva | `State` mock | termina enseguida | ❌ **(A1)** (duerme 10 s antes de salir) |
| MK-34 | `RetornoAutomatico` interrumpido | `State` mock | termina | ✅ |

**Integración y concurrencia (`AmbulanciaIntegracionTest`):**

| Id | Escenario | Resultado esperado | Obtenido |
|---|---|---|---|
| IN-01 | Estado inicial | Disponible, libre, simulación activa | ✅ |
| IN-02 | Flujo domicilio → regreso → retorno | Atendiendo → RegresandoSinPaciente → Disponible | ✅ |
| IN-03 | Flujo traslado → regreso | RegresandoSinPaciente / Disponible | ❌ **(A2)** |
| IN-04 | Flujo taller → vuelta → retorno | sale del taller, libre, luego Disponible | ✅ |
| IN-05 | Solicitud después de volver del taller | estado AtendiendoDomicilio | ❌ **(A3)** |
| IN-06 | Solicitud inválida en EnTaller | el estado no cambia | ✅ |
| IN-07 | 5 hilos × 20 servicios concurrentes | exclusión mutua (máximo 1 en servicio), 100 servicios, 100 notificaciones OK, sin deadlock | ✅ |
| IN-08 | 2 hilos alternando domicilio/traslado | el estado coincide con el servicio aceptado | ❌ **(A2 + A3)** |
| IN-09 | Fin de simulación con 4 hilos esperando | todos terminan; ninguna notificación OK | ✅ |
| IN-10/11 | `verificarYFinalizarSimulacion` con asociados terminados / pendientes | finaliza e informa / sigue activa | ✅ |
| IN-12 | `RetornoAutomatico` real con la ambulancia en domicilio | RegresandoSinPaciente en ≤ 12 s | ❌ **(A1)** |

## 3. Errores detectados en el código provisto

| Id | Severidad | Ubicación | Descripción | Detectado por |
|---|---|---|---|---|
| C1 | Bloqueante | `ambulancia/EnTallerState.java:1` | Declara `package model.modeloDominio.ambulancia` en lugar de `modelo…`: el proyecto no compila. | Compilación |
| C2 | Bloqueante | `ambulancia/TrasladandoPacienteClinicasState.java` | El archivo no coincide con la clase pública `TrasladandoPacienteClinicaState`: el proyecto no compila. | Compilación |
| D1 | Alta | `AsociadoDTO(String…, int, String)` | El constructor asigna sin validar: admite `null` y `numero < 0`, violando el SRS 3.1. | DTO-22..28 |
| B1 | Media | `BaseDeDatos.getInstancia()` | El método no es `synchronized`, así que varios hilos pueden crear varias instancias (y conexiones). | BD-02 |
| B2 | Alta | `BaseDeDatos.ejecutarActualizacion` | `stmt.setInt(8, 1)`: el `WHERE id = ?` siempre usa 1, así que todo UPDATE pisa el registro 1 (y falla por UNIQUE de DNI si el DNI pertenece a otro). | BD-16, BD-17, DAO-12 |
| B3 | Media | `BaseDeDatos.cerrarConexion` | Cierra la conexión pero el Singleton sigue devolviendo esa conexión cerrada. | BD-05 |
| B4 | Baja | `BaseDeDatos.iniciarBD` | El registro semilla id 4 tiene la ciudad `' Mar del Plata'` (con un espacio adelante). | BD-08 |
| B5 | Baja | `BaseDeDatos.ejecutarConsulta` | El `PreparedStatement` no se cierra nunca (fuga de recursos). | Inspección |
| O1 | Alta | `DataAccessObject.cargar(int)` | No llama a `resultado.next()` antes de leer, así que siempre lanza `SQLException` (y para un id inexistente no devuelve `null`). | DAO-06, DAO-07 |
| O2 | Baja | `DataAccessObject.eliminar` | `assert !Objects.equals(dni, "")` no detecta `null`, aunque el mensaje dice "no puede ser nulo". | DAO-11 |
| O3 | Baja | `DataAccessObject.CrearDTO(int id, …)` | El parámetro `id` se ignora porque el DTO no tiene ese campo, y el identificador se pierde en el mapeo. | Inspección |
| O4 | Media | `Domicilio` vs `AsociadoDTO` | El SRS y el DTO aceptan `numero = 0`, pero `Domicilio` exige `> 0`: un DTO válido no se puede convertir en `Asociado`. | DAO-15 |
| A1 | Alta | `RetornoAutomatico.run` | Condición invertida `while (!ambulancia.isSimulacionActiva())`: con la simulación activa el hilo termina sin hacer nada, y con la simulación inactiva queda dormido 10 s. | MK-31..33, IN-12 |
| A2 | Alta | `TrasladandoPacienteClinicaState.RetornoClinica` | Pasa a `RegresandoDelTallerState` en vez de a Disponible o RegresandoSinPaciente. Además hace `Thread.sleep(1000)` dentro del lock de `Ambulancia`. | MK-22, IN-03, IN-08 |
| A3 | Alta | `Ambulancia.volviendoDelTaller` / `RegresandoDelTallerState` | Libera la ambulancia (`ocupado = false`) dejándola en RegresandoDelTaller, que rechaza los pedidos. La solicitud siguiente se notifica como "OK" pero el estado no cambia: la atención se pierde. | IN-05, IN-08 |
| A4 | Baja | `Ambulancia.getEstadoActual()` | Lee `estadoActual` sin sincronizar (no garantiza visibilidad entre hilos). | Inspección |
| A5 | Baja | Estados concretos | Llaman a `notifyObservers` sin `setChanged()`, por lo que los mensajes de rechazo nunca llegan a los observadores. | Inspección |

## 4. Resultados

### 4.1 Ejecución

- El código provisto **no compila** (C1, C2), así que ninguna suite se puede ejecutar sin corregir esos dos archivos. Salida de `javac`:
  ```
  TrasladandoPacienteClinicasState.java:9: error: class TrasladandoPacienteClinicaState is public, should be declared in a file named TrasladandoPacienteClinicaState.java
  EnTallerState.java:9: error: cannot find symbol
  DisponibleState.java:44: error: cannot find symbol
  ...
  ```
- Con solo C1 y C2 corregidos, las suites sin BD dan **84 tests, 15 fallas**: DTO-22..28 (D1), MK-22 (A2), MK-31..33 (A1), IN-03 (A2), IN-05 (A3), IN-08 (A2/A3), IN-12 (A1).
- Como control de la propia suite, se corrigieron D1, A1, A2 y A3 en una copia del código y las mismas suites pasaron **84/84 en verde** (3 corridas consecutivas de las suites de Ambulancia, sin fallas intermitentes). Esto muestra que cada rojo corresponde a un error del código y no a un test mal planteado.
- Las suites de BD (`BaseDeDatosTest`, `DataAccessObjectTest`) compilan y quedan pendientes de ejecución contra MySQL. Rojos esperados: BD-02 (puede ser intermitente porque depende de la carrera), BD-05, BD-08, BD-16, BD-17, DAO-06, DAO-07, DAO-11, DAO-12, DAO-15.

### 4.2 Capturas

> _Pegar acá las capturas de la ejecución en rojo (código provisto) y en verde (código corregido) de cada suite._

## 5. Cómo ejecutar

1. Corregir C1 y C2, porque sin esto no compila nada.
2. Tener MySQL 8 en `localhost:3306` con un usuario `progra_c` / `progra_c` que pueda crear bases (`CREATE DATABASE`). En modo SQL estricto (el valor por defecto de MySQL 8), BD-14 funciona como se describe.
3. `mvn test` (el `pom.xml` ya incluye JUnit 4, Mockito 4.11 y `-ea`). Desde el IDE: habilitar `-ea` en la configuración de ejecución de JUnit. Con JDK 21 o posterior, agregar también `-Dnet.bytebuddy.experimental=true` si Mockito lo pide.
4. Las suites de Ambulancia tardan unos 30 s, porque los casos de `RetornoAutomatico` esperan el intervalo real de 10 s.

## 6. Conclusiones

- **Pruebas unitarias del DTO:** ejecutadas de forma totalmente aislada, alcanzaron para mostrar que la validación de integridad está solo en los setters. El constructor parametrizado, que es justamente el que usa la capa de dominio, la saltea (D1). Aplicar valores límite sobre `numero` confirmó que la regla `>= 0` del SRS está bien implementada en el setter.
- **Integración con BD:** los errores más graves solo aparecen al acoplar los componentes con el motor real: el UPDATE con el id fijo (B2), la lectura sin posicionar el cursor (O1) y el Singleton que no se recupera de un cierre (B3). Ninguno se puede detectar con pruebas unitarias del DTO. También se verificó que el uso de `PreparedStatement` protege contra la inyección SQL.
- **Mocks:** el diseño basado en la interfaz `State` permitió probar `Ambulancia` sin depender de los estados concretos, y probar cada estado sin depender de `Ambulancia`. Así, cada falla de la máquina de estados quedó atribuida a una sola clase (A2 en `TrasladandoPacienteClinicaState`, A1 en `RetornoAutomatico`).
- **Concurrencia:** la exclusión mutua basada en `ocupado` + `wait`/`notifyAll` funciona: sin carreras ni deadlocks en 100 servicios concurrentes, y el fin de simulación libera a todos los hilos. Las fallas están en la **coherencia** entre el flag `ocupado` y el estado (A3): la ambulancia figura libre en un estado que rechaza pedidos, y esto solo se vio en el test de integración con varios hilos.
- **Recomendaciones:** validar en el constructor del DTO (o delegar en los setters), recibir el id como parámetro en `ejecutarActualizacion`, llamar a `next()` en `cargar`, hacer `synchronized` `getInstancia()` y reabrir la conexión si está cerrada, corregir la condición de `RetornoAutomatico` y la transición de `TrasladandoPacienteClinicaState`, y alinear `ocupado` con el estado al volver del taller.
