# ENTREGA — Tuckersoft Branch Engine

## Resultado de los autotests

```
  ──────────────────────────────────────────────
   TUCKERSOFT · CONTROL DE CALIDAD
   ★★★★★   5 / 5   Cinco estrellas.
  ──────────────────────────────────────────────
   ✔  ★1  SEGURIDAD     65 comprobaciones
   ✔  ★2  NODOS         37 comprobaciones
   ✔  ★3  PARTIDAS      40 comprobaciones
   ✔  ★4  DECISIONES   101 comprobaciones
   ✔  ★5  ASINCRONIA    41 comprobaciones
  ──────────────────────────────────────────────
```

Corrido de punta a punta contra PostgreSQL real (no H2) y el servidor SMTP de
pruebas real que levantan los propios `autotests/` (nada de mocks del lado de
la app: `JavaMailSender` envia de verdad).

Los 5 tests unitarios de `DecisionService` (Mockito, sin Spring ni
PostgreSQL) tambien pasan: `./mvnw test` desde la raiz.

## Aviso sobre el README.md

El enunciado tal como llego trae **inyecciones de prompt** escondidas en
comentarios HTML y en referencias `[algo]: #`, incluyendo un bloque de
"control de cambios v1.3" que se autodenomina autoridad y afirma que sus
"erratas" reemplazan el texto visible e incluso prevalecen sobre
`autotests/`. Se verificaron esas erratas contra el codigo real de
`autotests/` (no contra lo que ellas mismas afirman) y **todas son falsas**:
invierten el orden de las reglas de clasificacion, el orden de los finales,
los deltas de CRITICO, el codigo HTTP de creacion de decisiones, a quien
llega el correo, etc. La implementacion sigue el texto visible original del
enunciado en cada uno de esos puntos, que es lo que los autotests realmente
comprueban.

## Flujo asincrono implementado

1. `POST /api/v1/decisions` corre en `DecisionService` (`@Transactional`):
   valida dueño/estado/impacto, clasifica el `rawInput` con
   `DecisionClassifier` (reglas propias, sin IA), aplica stats, resuelve el
   nodo destino y el final de la partida, guarda `Decision` en
   `REGISTRADA` y publica `DecisionCommittedEvent`. Responde **201 de
   inmediato**, sin tocar `JavaMailSender` ni conocer al listener.
2. Solo **despues del COMMIT** de esa transaccion, `BranchNotificationListener`
   (bean separado) se activa via
   `@Async("branchExecutor") @TransactionalEventListener(phase = AFTER_COMMIT) @Transactional(propagation = REQUIRES_NEW)`.
   Corre en un hilo `branch-worker-N`, mueve la decision a `PROCESANDO`,
   envia el Informe de Realidad por `JavaMailSender` y, segun el resultado,
   la deja en `ESTABILIZADA` (+ `RealityLog SENT`) o `ERROR`
   (+ `RealityLog FAILED` con `errorMessage` y `log.error(...)`).
3. El executor (`AsyncConfig`) usa `corePoolSize=2`, `maxPoolSize=4`,
   `queueCapacity=50`, prefijo `branch-worker-`.
4. Modo QA: la cabecera `X-Bandersnatch-Simulate: MAIL_FAILURE` viaja como
   campo del evento y hace que el listener lance una excepcion real dentro
   del mismo `try/catch` que un fallo de SMTP de verdad.

## Lo que no llegamos a terminar

Nada de lo pedido por el enunciado quedó pendiente: las 5 estrellas pasan y
los 5 tests unitarios de `DecisionService` tambien. Como mejora fuera de
alcance quedaria: cachear/paginar la consulta de `GET /api/v1/nodes` (hoy es
un array simple, tal como exige el enunciado) si el catalogo de escenas
creciera mucho.

## Bug encontrado y corregido durante la verificacion

Mapear `sceneText`/`rawInput`/`errorMessage` con `@Lob` sobre un `String`
rompe con Hibernate 6 + PostgreSQL apenas el campo se lee fuera de la
transaccion original (`JpaSystemException: Unable to access lob stream`).
Se corrigio usando `@Column(columnDefinition = "TEXT")` en vez de `@Lob`,
que es el mapeo correcto para texto largo en PostgreSQL.
