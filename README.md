# LipariBank Day 08 — Error Handling Globale & Logging

Benvenuto nel progetto **liparibank-day08-broken**.

Applicazione Spring Boot 3.3.4 (Java 21) con sistema di error handling basato su
`@RestControllerAdvice`, `ProblemDetail` RFC 7807, gerarchia di eccezioni di dominio sealed,
`MdcLoggingFilter` per il request tracing, e `logback-spring.xml` con profili.

Il progetto contiene **3 bug intenzionali** da trovare e correggere.

---

## Come avviare

```bash
./mvnw spring-boot:run
```

Su Windows:
```cmd
mvnw.cmd spring-boot:run
```

Swagger UI disponibile su: `http://localhost:8080/swagger-ui.html`
H2 Console: `http://localhost:8080/h2-console` (JDBC URL: `jdbc:h2:mem:liparibankdb`)

---

## Curl di test per ogni scenario

### Setup: crea un conto

```bash
curl -s -X POST http://localhost:8080/api/v1/accounts \
  -H "Content-Type: application/json" \
  -d '{"iban":"IT60X0542811101000000123456","balance":1000.00,"ownerName":"Mario Rossi"}' | jq .
```

### Scenario Missione 1 — conto non trovato

```bash
curl -sv http://localhost:8080/api/v1/accounts/999 2>&1 | grep -E "< HTTP|^\{"
```

Osserva il codice HTTP nella risposta. Confronta con il campo `status` nel body JSON.

### Scenario Missione 2 — MDC non pulito

```bash
# Avvia l'app con un endpoint che lancia eccezione interna
# Poi fai una richiesta normale successiva e osserva il requestId nei log
curl -s http://localhost:8080/api/v1/accounts/abc  # Causa errore di tipo conversione
curl -s http://localhost:8080/api/v1/accounts       # Richiesta normale successiva
# Verifica nei log: il requestId della seconda richiesta è corretto?
```

### Scenario Missione 3 — IBAN duplicato

```bash
# Prima inserzione
curl -s -X POST http://localhost:8080/api/v1/accounts \
  -H "Content-Type: application/json" \
  -d '{"iban":"IT60X0542811101000000999999","balance":500.00,"ownerName":"Luca Bianchi"}' | jq .

# Seconda inserzione con lo stesso IBAN
curl -sv -X POST http://localhost:8080/api/v1/accounts \
  -H "Content-Type: application/json" \
  -d '{"iban":"IT60X0542811101000000999999","balance":100.00,"ownerName":"Anna Verdi"}' 2>&1 | grep -E "< HTTP|status"
```

Osserva il codice HTTP restituito. Confronta con quello atteso.

### Scenario validazione (senza bug)

```bash
curl -s -X POST http://localhost:8080/api/v1/accounts \
  -H "Content-Type: application/json" \
  -d '{"iban":"","balance":-50,"ownerName":""}' | jq .
```

---

## Le 3 Missioni

### Missione 1 — La risposta di errore ha il codice HTTP sbagliato

Quando si chiama `GET /api/v1/accounts/999` e il conto non esiste, il sistema
risponde con un body JSON che indica `"status": 404` — ma il codice HTTP effettivo
della risposta è diverso da quello atteso.

**Sintomo:** `curl -I` mostra un codice HTTP inatteso. Il corpo della risposta
contiene `ProblemDetail` con i campi corretti ma il protocollo HTTP dice altro.

---

### Missione 2 — I log mostrano `requestId` errato in alcune richieste

In modo intermittente, alcune richieste mostrano nei log il `requestId` di una
richiesta precedente invece del proprio. Il fenomeno si amplifica sotto carico.

**Sintomo:** nel log si vedono righe con `[requestId]` che non corrispondono alla
richiesta in corso. Più evidente quando una richiesta precedente ha generato
un'eccezione non gestita nel filter chain.

**Come riprodurre:** invia una richiesta che causa un errore (es. `GET /api/v1/accounts/abc`
che genera un'eccezione di conversione di tipo), poi immediatamente una richiesta
normale (`GET /api/v1/accounts`). Confronta il `requestId` nei log: la seconda
richiesta potrebbe mostrare il `requestId` della prima.

---

### Missione 3 — IBAN duplicato restituisce HTTP 500 invece di 409

Quando si tenta di inserire un conto con un IBAN già esistente, il sistema dovrebbe
riconoscere il conflitto e rispondere con HTTP 409. Invece risponde con HTTP 500
e nel log si vede un messaggio di errore interno.

**Sintomo:** `POST /api/v1/accounts` con IBAN già presente → HTTP 500 con
`"errorCode":"INTERNAL_SERVER_ERROR"` invece di HTTP 409 con
`"errorCode":"DATA_INTEGRITY_VIOLATION"`.

---

## Struttura del progetto

```
src/main/java/com/lipari/bank/
├── LipariBankApplication.java
├── account/
│   ├── model/Account.java
│   ├── dto/AccountRequest.java
│   ├── AccountRepository.java
│   ├── AccountService.java
│   └── AccountController.java
└── shared/
    ├── config/
    │   ├── SecurityConfig.java
    │   └── OpenApiConfig.java
    ├── exception/
    │   ├── LipariBankException.java     ← sealed class
    │   ├── ResourceNotFoundException.java
    │   ├── BusinessException.java
    │   ├── ConflictException.java
    │   ├── GlobalExceptionHandler.java  ← @RestControllerAdvice
    │   └── DatabaseExceptionHandler.java← @RestControllerAdvice separato
    └── filter/
        └── MdcLoggingFilter.java        ← OncePerRequestFilter
src/main/resources/
├── application.yml
└── logback-spring.xml
```

---

## Bonus Mission — Feature da Implementare (opzionale, ~1 ora)

Una volta risolti i 3 bug, implementa la seguente feature per consolidare i concetti del giorno.

### Filter di audit per tempo di risposta e status HTTP

Il `MdcLoggingFilter` aggiunge il `requestId` al contesto MDC, ma non esiste nessun componente che registri sistematicamente il tempo impiegato da ogni richiesta e il codice di risposta HTTP prodotto.

**Cosa implementare:**

Crea un nuovo filtro `RequestAuditFilter` che esegue una volta sola per ogni richiesta HTTP (come `MdcLoggingFilter`). Il filtro deve:

1. Misurare il tempo trascorso dall'inizio alla fine dell'elaborazione della richiesta (in millisecondi).
2. Dopo che la risposta è stata generata, emettere un'unica riga di log con: metodo HTTP, URI della richiesta, codice di stato HTTP della risposta, e durata in ms.
3. Includere nella riga di log il `requestId` già presente nel contesto MDC, impostato da `MdcLoggingFilter`.

Registra il filtro garantendo che esegua **dopo** `MdcLoggingFilter` (così il `requestId` è già disponibile nel MDC quando viene scritto il log di audit).

**Criteri di accettazione:**

- Per ogni richiesta HTTP, nei log appare una riga con metodo, URI, status e durata.
- La riga include il `requestId` corretto (non `NO-REQ-ID`).
- Il filtro funziona sia per richieste andate a buon fine sia per richieste che generano errori gestiti da `GlobalExceptionHandler`.
- Il `requestId` nel log di audit corrisponde a quello nelle righe di log applicative della stessa richiesta.
- Nessun test esistente è rotto dopo l'aggiunta del filtro.

---

*LipariBank Prompt Bootcamp — Error Handling Globale & Logging — Day 08*
