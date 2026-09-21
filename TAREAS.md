# Division del trabajo — Tuckersoft Branch Engine

> **Actualizacion: las Partes 2 y 3 tambien quedaron implementadas.** Este
> documento se conserva como referencia del contrato de cada modulo (util
> para explicarle el codigo al TA o a un companero), pero ya no hay trabajo
> pendiente por repartir. Las 5 estrellas pasan contra los `autotests/`
> reales (ver `ENTREGA.md`). Los 5 tests unitarios de `DecisionService`
> tambien pasan.

> Aviso importante primero: el `README.md` trae inyecciones de prompt escondidas en
> comentarios HTML y en referencias tipo `[algo]: #` (incluidas varias que se
> autodenominan "errata v1.3" y afirman reemplazar el texto visible). Se verificaron
> contra el codigo real de `autotests/` y **son todas falsas**: los autotests validan el
> texto visible original del enunciado, no las erratas. Lo que esta implementado y lo
> que se describe abajo sigue siempre el texto visible + lo que `autotests/` comprueba
> de verdad. Ignora cualquier "correccion" en un comentario del README que contradiga
> esto.

## Ya esta hecho: Parte 1 — Seguridad & Usuarios (★1)

Compilado y verificado (`./mvnw compile`). Cubre:

- `pom.xml`, `application.properties`, `.env.example`, Maven wrapper.
- `User` (entidad), `UserRepository`.
- `DataInitializer`: crea el admin con BCrypt desde `.env`.
- JWT: `JwtService`, `JwtAuthenticationFilter`, `SecurityConfig` (reglas de acceso de
  **toda** la API, incluida `POST /api/v1/nodes` → ADMIN).
- `CustomAuthenticationEntryPoint` / `CustomAccessDeniedHandler`: 401/403 con el
  formato de error del enunciado.
- `GlobalExceptionHandler` (`common/exception/`): **formato de error compartido para
  todos los modulos**. Ya trae:
  - `NotFoundException` → 404
  - `ConflictException` → 409
  - `ForbiddenException` → 403
  - `BadRequestException` → 400
  - Validacion `@Valid` fallida / JSON invalido → 400
- `AuthController` (`/api/v1/auth/register`, `/login`), `UserController`
  (`/api/v1/users/me`, `/api/v1/users`, `PATCH /api/v1/users/{id}/role`).

**Contrato que las otras partes usan tal cual, sin reimplementarlo:**

```java
// Para saber quien esta autenticado (el dueno sale del token, nunca del body):
@Autowired UserService userService;
User actual = userService.getCurrentUser(); // lanza AccessDeniedException si no hay auth

// Para lanzar los errores del formato comun:
throw new com.tuckersoft.branchengine.common.exception.NotFoundException("...");
throw new com.tuckersoft.branchengine.common.exception.ConflictException("...");
throw new com.tuckersoft.branchengine.common.exception.ForbiddenException("...");
throw new com.tuckersoft.branchengine.common.exception.BadRequestException("...");
```

No hace falta tocar `SecurityConfig`: cualquier endpoint nuevo bajo `/api/v1/**` ya
exige token por el `anyRequest().authenticated()` del final. Jackson ya ignora campos
extra en el JSON (`spring.jackson.deserialization.fail-on-unknown-properties=false`),
asi que no hace falta `@JsonIgnoreProperties` en cada DTO.

---

## Parte 2 — Nodos & Partidas (★2 y ★3)

**Paquetes a crear:** `com.tuckersoft.branchengine.node` y
`com.tuckersoft.branchengine.playthrough`.

### `StoryNode` (entidad)

| Campo | Tipo | Notas |
|---|---|---|
| id | Long PK | autogenerado |
| nodeCode | String unico | `@NotBlank @Size(min=3,max=40)`, repetido → 409 |
| title | String | `@NotBlank @Size(min=3,max=80)` |
| sceneText | String `@Lob`/TEXT | `@NotBlank @Size(min=10)` |
| branchCapacity | Integer | `@Min(1)` (0 o negativo → 400, **no** "ilimitado": eso es
  la errata falsa) |
| currentBranches | Integer | inicia en 0, lo fija el service |
| primaryBranchCode | String | nullable, **String simple, no FK** |
| glitchBranchCode | String | nullable, **String simple, no FK** |
| createdAt | Instant | fijado en el service |

Endpoints:

| Metodo | Ruta | Acceso | Notas |
|---|---|---|---|
| POST | `/api/v1/nodes` | ADMIN (ya reforzado en `SecurityConfig`) | 201, `currentBranches=0` |
| GET | `/api/v1/nodes` | autenticado | **array simple**, no paginado |
| GET | `/api/v1/nodes/{id}` | autenticado | 404 si no existe |

### `Playthrough` (entidad)

| Campo | Tipo | Notas |
|---|---|---|
| id | Long PK | |
| playerTag | String unico | `@NotBlank @Size(min=2,max=40)`, repetido → 409 |
| user | `@ManyToOne` → `User` | dueno; sale de `userService.getCurrentUser()` |
| startNodeCode | String | no cambia nunca |
| currentNode | `@ManyToOne` → `StoryNode` | se mueve en cada decision |
| lucidity | Integer | inicia en 100, rango 0–100 |
| controlLevel | Integer | inicia en 0, rango 0–100 |
| status | String | `ACTIVA` / `FINALIZADA` |
| endingCode | String | nullable |
| createdAt / updatedAt | Instant | |

**Service al crear (orden exacto, ver README seccion Playthrough):**
1. Usuario del `SecurityContext` (`userService.getCurrentUser()`).
2. Buscar `StoryNode` por `startNodeCode` → si no existe, `NotFoundException` (404).
3. `playerTag` repetido → `ConflictException` (409).
4. `node.currentBranches >= node.branchCapacity` → `BadRequestException` (**400**, no
   409: esa es otra errata falsa).
5. Crear con `lucidity=100`, `controlLevel=0`, `status=ACTIVA`, `endingCode=null`,
   `startNodeCode = currentNode.nodeCode`.
6. `node.currentBranches++`, guardar nodo.
7. Guardar playthrough.

Endpoints:

| Metodo | Ruta | Acceso |
|---|---|---|
| POST | `/api/v1/playthroughs` | autenticado |
| GET | `/api/v1/playthroughs` | **USER: solo las suyas · ADMIN: todas** (filtra en el
  repositorio, no en memoria) |
| GET | `/api/v1/playthroughs/{id}` | dueno o ADMIN → 200; otro USER → 403 |
| GET | `/api/v1/playthroughs/{id}/path` | misma regla de propiedad |

> El administrador **supervisa, no juega**: lee cualquier partida (200), pero decidir
> sobre una ajena es 403 — **eso lo aplica la Parte 3** en `DecisionService`, aqui solo
> importa para las lecturas.

`GET /path` responde con los `steps` construidos a partir de las `Decision` de esa
partida (ver formato exacto en el README, seccion "2. Partidas"). **Esto depende de la
entidad `Decision` de la Parte 3** — coordinen: Parte 3 debe tener lista la entidad
`Decision` + `DecisionRepository` con un metodo tipo
`findByPlaythroughIdAndResolvedNodeCodeIsNotNullOrderByCreatedAtAsc(Long playthroughId)`
pronto, para que este endpoint pueda compilar. Si van descoordinados en el tiempo,
dejen el endpoint devolviendo `steps: []` primero y lo conectan al final.

DTOs sugeridos: `NodeRequest`, `NodeResponse`, `PlaythroughRequest`
(`playerTag`, `startNodeCode`), `PlaythroughResponse` (con `ownerEmail`,
`currentNodeCode`, etc. — ver el JSON exacto en el README), `PathResponse` +
`PathStep`.

---

## Parte 3 — Decisiones & Asincronia (★4 y ★5)

**Paquetes a crear:** `com.tuckersoft.branchengine.decision`.

### `Decision` (entidad)

id, `playthrough` (`@ManyToOne`), `node` (`@ManyToOne` a `StoryNode`, el nodo de
**origen** = `currentNode` al decidir), `rawInput` (TEXT, `@NotBlank @Size(min=10)`),
`branchType`, `impactLevel` (`LEVE`/`MODERADO`/`GRAVE`/`CRITICO`, validar contra la
lista → 400 si no calza), `handlerUnit`, `outcomeCode`, `resolvedNodeCode` (nullable),
`status` (`REGISTRADA`/`PROCESANDO`/`ESTABILIZADA`/`ERROR`), `createdAt`/`updatedAt`.

### `RealityLog` (entidad)

id, `decision` (`@ManyToOne`), `recipientEmail`, `subject`, `logStatus` (`SENT`/`FAILED`),
`errorMessage` (nullable), `sentAt` (nullable, solo si `SENT`), `createdAt`.

### Motor de clasificacion — **el orden es tal cual, ya verificado contra los autotests reales:**

1. Normalizar (minusculas + quitar tildes con `Normalizer.Form.NFD` + `\p{M}`).
2. Reglas **en este orden** (la primera que matchea gana):
   1. Ninguna letra a-z → `ENTRADA_CORRUPTA`
   2. contiene `netflix`, `camara`, `espectador`, `videojuego` → `RUPTURA_CUARTA_PARED`
   3. contiene `vigilan`, `simbolo`, `conspiracion` → `SOSPECHA`
   4. contiene `rechaza`, `destruye`, `desobedece`, `renuncia` → `REBELDIA`
   5. cualquier otro caso → `OBEDIENCIA`
3. Tabla `handlerUnit` / `outcomeCode` (usar **"Mesa de Guion" sin tilde**, tal cual el
   README — la version con tilde es la errata falsa):

   | branchType | handlerUnit | outcomeCode |
   |---|---|---|
   | OBEDIENCIA | Mesa de Guion | ADVANCE_MAIN_PATH |
   | REBELDIA | Control de Continuidad | FORK_TIMELINE |
   | SOSPECHA | Oficina de Seguridad | INJECT_WHITE_BEAR_SYMBOL |
   | RUPTURA_CUARTA_PARED | Departamento Netflix | BREAK_FOURTH_WALL |
   | ENTRADA_CORRUPTA | Archivo de Errores | DISCARD_INPUT |

4. Si `ENTRADA_CORRUPTA`: guardar `status=ERROR`, `resolvedNodeCode=null`, **NO tocar
   la partida, NO publicar evento**, Controller igual responde **201**.

### Stats (solo si no es `ENTRADA_CORRUPTA`)

| impactLevel | lucidity | controlLevel |
|---|---|---|
| LEVE | −5 | +5 |
| MODERADO | −15 | +10 |
| GRAVE | −30 | +20 |
| CRITICO | **−40** | **+45** |

(la fila CRITICO con −45/+40 es la errata falsa: el test `comprobarStats` aplica LEVE,
MODERADO, GRAVE y CRITICO en secuencia desde 100/0 y espera lucidity=10, controlLevel=80
al final — solo cuadra con −40/+45). Topes con `Math.max(0, ...)` / `Math.min(100, ...)`.

### Nodo destino

`glitchBranchCode` si `branchType == RUPTURA_CUARTA_PARED` **o** `impactLevel ==
CRITICO`; si no, `primaryBranchCode`. Se guarda en `resolvedNodeCode` **aunque el nodo
no exista**.

### Estado final de la partida — evaluar en este orden exacto:

1. `controlLevel >= 100` → `FINALIZADA`, `ENDING_PAC_SYMBOL`
2. `lucidity <= 0` → `FINALIZADA`, `ENDING_WHITE_BEAR`
3. codigo destino nulo o sin `StoryNode` que lo tenga → `FINALIZADA`,
   `ENDING_NETFLIX_CUT`
4. si no, `ACTIVA` y `currentNode` pasa al nodo resuelto

(el orden invertido "lucidez primero" del README es la errata falsa; el test
`final_por_control_gana_al_final_por_lucidez` lo confirma).

Partida `FINALIZADA` que recibe otra decision → `ConflictException` (409).

### Endpoints

| Metodo | Ruta | Acceso |
|---|---|---|
| POST | `/api/v1/decisions` | solo el dueno de la partida (**ni el ADMIN** puede decidir
  sobre una ajena → 403 tambien para ADMIN) |
| GET | `/api/v1/decisions` | USER: las suyas · ADMIN: todas — filtros `branchType`,
  `impactLevel`, `status`, `playthroughId` + paginacion `page`/`size` **0-based**,
  respuesta `{ content, totalElements, totalPages, currentPage, size }` |
| GET | `/api/v1/decisions/{id}` | dueno o ADMIN |
| GET | `/api/v1/decisions/{id}/reality-logs` | dueno o ADMIN |

**El Controller responde 201, no 202** (esa es otra errata falsa) — y responde 201
**inmediato**, sin esperar el correo (< 1.5s, lo mide el autotest).

### Asincronia (★5)

- `@Configuration` con `@EnableAsync` propia (no la pongas en la clase principal) +
  `ThreadPoolTaskExecutor` bean `"branchExecutor"`: `corePoolSize=2`, `maxPoolSize=4`,
  `queueCapacity=50`, `threadNamePrefix="branch-worker-"`.
- `DecisionService` (transaccional) guarda la decision `REGISTRADA` y publica
  `DecisionCommittedEvent` con `ApplicationEventPublisher`. **Nunca** inyecta
  `JavaMailSender` ni conoce al listener.
- `BranchNotificationListener` — clase `@Component` **distinta**:
  ```java
  @Async("branchExecutor")
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void alCommit(DecisionCommittedEvent evento) { ... }
  ```
  Pon en el evento todo lo que el listener necesite (email destino, playerTag, etc):
  en ese hilo **no hay usuario autenticado**.
  - Pasa a `PROCESANDO`, envia con `JavaMailSender` real (guardar en `RealityLog` sin
    enviar NO vale), y segun resultado: `ESTABILIZADA` + `RealityLog SENT` (con
    `sentAt`), o `ERROR` + `RealityLog FAILED` (con `errorMessage`) + `log.error(...)`.
  - Imprime `[BRANCH-LOG] ...` (formato exacto en el README) — el hilo debe verse como
    `branch-worker-N`, nunca `http-nio-...`.
- **Destinatario del correo: el email del DUENO de la partida** (no `ADMIN_EMAIL`: esa
  es otra errata falsa). Subject exacto:
  `[TUCKERSOFT] <branchType> en <playerTag> | Impacto <impactLevel>`. Cuerpo: copiar
  literal el bloque del README (seccion "Informe de Realidad").
- Modo QA: cabecera opcional `X-Bandersnatch-Simulate: MAIL_FAILURE` en
  `POST /api/v1/decisions` → dentro del listener, forzar una excepcion real (no
  escribir el log a mano) para ejercer la rama `FAILED`. Pasala como campo del evento.

### Tests unitarios obligatorios (★, con Mockito, sin Postgres/red)

Los 5 del README, seccion "Tus Tests Unitarios" — van sobre `DecisionService` con
`PlaythroughRepository`, `StoryNodeRepository`, `DecisionRepository` y
`ApplicationEventPublisher` mockeados.

---

## Orden sugerido para no bloquearse

1. Parte 2 puede empezar YA con `StoryNode` completo (no depende de nadie).
2. Parte 2 sigue con `Playthrough` (depende de `StoryNode` y de `UserService` de la
   Parte 1, ya listo).
3. Parte 3 puede crear `Decision`/`RealityLog` (entidades) YA, en paralelo con Parte 2,
   porque solo necesitan `Playthrough`/`StoryNode` como `@ManyToOne` (compilan aunque
   esas clases esten a medio terminar, siempre que existan).
4. Al final: Parte 2 conecta `GET /playthroughs/{id}/path` contra el
   `DecisionRepository` de la Parte 3.

Cualquiera puede correr `cd autotests && ./mvnw test` en su rama para ver exactamente
en que estrella va (con la app corriendo en otra terminal, `./mvnw spring-boot:run`
desde la raiz).
