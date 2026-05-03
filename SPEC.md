# SPEC.md - Spins Backend

**Projekt:** Java 2 semestrální projekt
**Inspirace:** Pragmatic Play - Gates of Olympus
**Repozitář:** Spring Boot backend pro slot machine hru
**Stav:** backend MVP hotový, frontend je další samostatný krok

---

## 1. Přehled

Tento projekt je backend pro webovou slot machine aplikaci inspirovanou Gates of Olympus. Backend generuje 6x5 grid symbolů, počítá výhry, zpracovává tumble sekvence, multiplikátory, free spins, buy free spins, balance hráče a historii spinů.

Backend je autoritativní zdroj pravdy. Frontend pouze posílá požadavky, zobrazuje balance a animuje grid podle výsledků vrácených z API.

### 1.1 Architektura

```text
Svelte/Vite frontend
  -> HTTP/JSON
  -> Spring Boot REST API
  -> Spring Data JPA
  -> H2 in-memory databáze
```

Aktuální backend používá H2 databázi v paměti. Konfigurace je v `src/main/resources/application.properties` a H2 běží v PostgreSQL compatibility mode.

### 1.2 Aktuální rozsah

Hotovo:

- 6x5 grid.
- Scatter pays výplaty pro 8+ stejných regular symbolů.
- Tumble mechanika s odstraněním výherních regular symbolů, gravitací a doplněním nových symbolů.
- Náhodné multiplier symboly s váženými hodnotami.
- Base spin endpoint.
- Buy Free Spins endpoint.
- Free Spin session endpoint.
- Akumulační multiplikátor ve free spinech.
- Retrigger free spinů.
- Perzistence spinů a tumble kroků.
- Historie hráče.
- Detail spinu pro replay.
- Seed data pro jednoho hráče a jednu paytable.
- Jednotný JSON formát chyb.
- Integrační testy přes MockMvc.

Mimo aktuální rozsah:

- Reálný frontend v tomto repo.
- Více hráčů a autentizace.
- Reálné platby.
- PostgreSQL runtime profil.
- Admin CRUD pro paytables.
- RTP simulace/statistiky.
- Produkční casino-grade matematická verifikace.

---

## 2. Spuštění

### 2.1 Požadavky

- Java 26.
- Maven.

### 2.2 Příkazy

```bash
mvn spring-boot:run
```

```bash
mvn test
```

### 2.3 Lokální URL

- API: `http://localhost:8080/api`
- H2 console: `http://localhost:8080/h2-console`
- Swagger UI: `http://localhost:8080/swagger-ui.html`

H2 připojení:

- JDBC URL: `jdbc:h2:mem:spins-backend;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE`
- User: `sa`
- Password: prázdné

---

## 3. Technický Stack

| Oblast      | Použito                            |
| ----------- | ---------------------------------- |
| Framework   | Spring Boot 4.0.5                  |
| Jazyk       | Java 26                            |
| Build       | Maven                              |
| REST        | Spring WebMVC                      |
| ORM         | Spring Data JPA + Hibernate        |
| Databáze    | H2 in-memory, PostgreSQL mode      |
| Validace    | Jakarta Validation                 |
| DTO JSON    | Jackson                            |
| Boilerplate | Lombok                             |
| API docs    | springdoc-openapi 3.0.2            |
| Testy       | JUnit 5, Spring Boot Test, MockMvc |

Hlavní package:

```text
com.adamekka.spins_backend
```

---

## 4. Tok Backendem

Base spin:

```text
POST /api/spin
  -> SpinController
  -> SpinService
  -> PlayerService / PlayerRepository
  -> PaytableRepository
  -> TumbleEngine
  -> PayoutCalculator
  -> SpinRepository
  -> SpinResponse
```

Free spin:

```text
POST /api/spin/free
  -> SpinController
  -> SpinService.freeSpin
  -> SpinRepository.findSessionByIdForUpdate
  -> TumbleEngine
  -> PayoutCalculator
  -> SpinRepository.save
  -> SpinResponse
```

Buy free spins:

```text
POST /api/spin/buy
  -> SpinController
  -> SpinService.buyFreeSpins
  -> PlayerService / PlayerRepository
  -> PaytableRepository
  -> SpinRepository.save
  -> BuyFreeSpinsResponse
```

Role hlavních tříd:

| Třída                 | Role                                                          |
| --------------------- | ------------------------------------------------------------- |
| `SpinController`      | HTTP endpointy pro spin, free spin, buy a detail.             |
| `SpinService`         | Business logika, transakce, balance, validace, persistence.   |
| `TumbleEngine`        | RNG gridu, multiplier hodnoty, tumble loop, gravitace/refill. |
| `PayoutCalculator`    | Výpočet výher pro jeden grid.                                 |
| `PlayerService`       | Načtení a reset seed hráče.                                   |
| `SeedDataInitializer` | Vytvoří hráče a paytable při startu.                          |
| `ApiExceptionHandler` | Převod výjimek na jednotný JSON error response.               |

---

## 5. Herní Mechaniky

### 5.1 Grid

- Grid má 6 sloupců a 5 řádků.
- Každá buňka obsahuje `code` symbolu.
- Multiplier buňka obsahuje také `multiplierValue`.
- Backend vrací celý seznam tumble kroků najednou, aby frontend mohl sekvenci přehrát animací.

Příklad jedné buňky:

```json
{ "code": "CROWN" }
```

Příklad multiplier buňky:

```json
{ "code": "MULTIPLIER", "multiplierValue": 5 }
```

### 5.2 Symboly

Seed paytable se jmenuje `Gates of Olympus`.

| Kategorie | Symbol           | Kód          | Váha | 8-9    | 10-11  | 12+    |
| --------- | ---------------- | ------------ | ---- | ------ | ------ | ------ |
| High      | Koruna           | `CROWN`      | 25   | 10.00x | 25.00x | 50.00x |
| High      | Prsten           | `RING`       | 30   | 5.00x  | 15.00x | 25.00x |
| High      | Pohár            | `CUP`        | 35   | 4.00x  | 10.00x | 20.00x |
| High      | Přesýpací hodiny | `HOURGLASS`  | 40   | 2.50x  | 5.00x  | 10.00x |
| Low       | Modrý drahokam   | `GEM_BLUE`   | 50   | 1.00x  | 2.00x  | 5.00x  |
| Low       | Zelený drahokam  | `GEM_GREEN`  | 55   | 0.80x  | 1.80x  | 4.00x  |
| Low       | Fialový drahokam | `GEM_PURPLE` | 60   | 0.50x  | 1.00x  | 2.50x  |
| Low       | Žlutý drahokam   | `GEM_YELLOW` | 65   | 0.40x  | 0.90x  | 2.00x  |
| Special   | Scatter          | `SCATTER`    | 8    | -      | -      | -      |
| Special   | Multiplikátor    | `MULTIPLIER` | 4    | -      | -      | -      |

### 5.3 Výplaty

Regular symboly:

- 8-9 stejných symbolů: `payoutLow * bet`.
- 10-11 stejných symbolů: `payoutMid * bet`.
- 12+ stejných symbolů: `payoutHigh * bet`.

Scatter symboly:

- 4 scatter: `2.00 * bet`.
- 5 scatter: `5.00 * bet`.
- 6+ scatter: `100.00 * bet`.

`PayoutCalculator` vyhodnotí aktuální grid. Regular výherní symboly pokračují do tumble mechaniky. Scatter výplata se přičte podle počtu scatterů v aktuálně vyhodnoceném gridu.

### 5.4 Tumble

`TumbleEngine.process(paytable, bet)` dělá tento tok:

```text
1. Vygeneruje počáteční grid podle vah symbolů.
2. Spočítá výhru přes PayoutCalculator.
3. Uloží aktuální grid jako TumbleOutcome.
4. Pokud nejsou žádné výherní regular symboly, spin končí.
5. Odstraní výherní regular symboly.
6. Multiplier symboly zůstanou ukotvené na své pozici.
7. Ostatní symboly ve sloupci propadnou dolů.
8. Prázdná místa se doplní novými náhodnými symboly.
9. Opakuje od kroku 2.
```

### 5.5 Multiplikátory

Multiplier symbol má váženou hodnotu:

| Hodnota | Váha | Hodnota | Váha |
| ------- | ---- | ------- | ---- |
| 2x      | 300  | 20x     | 15   |
| 3x      | 200  | 25x     | 10   |
| 4x      | 150  | 50x     | 7    |
| 5x      | 100  | 100x    | 5    |
| 6x      | 70   | 250x    | 2    |
| 8x      | 50   | 500x    | 1    |
| 10x     | 40   |         |      |
| 12x     | 30   |         |      |
| 15x     | 20   |         |      |

Base spin výpočet:

```text
baseWin = součet výher všech tumble kroků
finalMultiplier = součet multiplier hodnot na finálním gridu

pokud baseWin > 0 a finalMultiplier > 0:
    totalWin = baseWin * finalMultiplier
jinak:
    totalWin = baseWin
```

### 5.6 Free Spins

Base spin trigger:

- Pokud `maxScatterCount >= paytable.scatterTriggerCount`, tedy seed default `4`, base spin vytvoří session s `remainingFreeSpins = 15`.

Free spin endpoint:

- Klient volá `POST /api/spin/free` s `parentSpinId` session spinu.
- Free spin má `bet = 0.00`, ale výhra se počítá podle původní sázky z parent spinu.
- Parent spin drží `remainingFreeSpins` a `accumulatedMultiplier`.
- Každý free spin vytvoří nový child `Spin` typu `FREE_SPIN`.

Free spin výpočet:

```text
accumulatedMultiplier += outcome.finalMultiplier

pokud outcome.baseWin > 0:
    totalWin = outcome.baseWin * max(accumulatedMultiplier, 1)
jinak:
    totalWin = 0.00
```

Retrigger:

- Ve free spinu stačí `3+` scatterů.
- Retrigger přidá `paytable.freeSpinsRetrigger`, seed default `5` free spinů.

### 5.7 Buy Free Spins

Buy endpoint:

- Cena je `bet * paytable.buyFreeSpinsMultiplier`, seed default `100`.
- Balance se sníží okamžitě.
- Vytvoří se parent session spin typu `PURCHASED_FREE_SPINS`.
- Free spiny se potom konzumují přes `POST /api/spin/free`.

---

## 6. Datový Model

Projekt má 5 JPA entit a několik 1:N vazeb.

### 6.1 Entity

#### `Player`

Tabulka `players`.

| Field       | Typ             | Poznámka                      |
| ----------- | --------------- | ----------------------------- |
| `id`        | `Long`          | PK                            |
| `username`  | `String`        | unique, seed hodnota `player` |
| `balance`   | `BigDecimal`    | precision 19, scale 2         |
| `createdAt` | `LocalDateTime` | čas vytvoření                 |
| `spins`     | `List<Spin>`    | 1:N                           |

#### `Paytable`

Tabulka `paytables`.

| Field                    | Typ                  | Poznámka                                |
| ------------------------ | -------------------- | --------------------------------------- |
| `id`                     | `Long`               | PK                                      |
| `name`                   | `String`             | unique, seed hodnota `Gates of Olympus` |
| `reelCount`              | `int`                | seed `6`                                |
| `rowCount`               | `int`                | seed `5`                                |
| `minMatchCount`          | `int`                | seed `8`                                |
| `scatterTriggerCount`    | `int`                | seed `4`                                |
| `freeSpinsAwarded`       | `int`                | seed `15`                               |
| `freeSpinsRetrigger`     | `int`                | seed `5`                                |
| `buyFreeSpinsMultiplier` | `int`                | seed `100`                              |
| `symbols`                | `List<SymbolConfig>` | 1:N                                     |
| `spins`                  | `List<Spin>`         | 1:N                                     |

#### `SymbolConfig`

Tabulka `symbol_configs`.

| Field        | Typ          | Poznámka                           |
| ------------ | ------------ | ---------------------------------- |
| `id`         | `Long`       | PK                                 |
| `paytable`   | `Paytable`   | N:1                                |
| `code`       | `String`     | např. `CROWN`                      |
| `weight`     | `int`        | váha pro RNG                       |
| `symbolType` | `SymbolType` | `REGULAR`, `SCATTER`, `MULTIPLIER` |
| `payoutLow`  | `BigDecimal` | výplata 8-9                        |
| `payoutMid`  | `BigDecimal` | výplata 10-11                      |
| `payoutHigh` | `BigDecimal` | výplata 12+                        |

#### `Spin`

Tabulka `spins`.

| Field                   | Typ             | Poznámka                                    |
| ----------------------- | --------------- | ------------------------------------------- |
| `id`                    | `Long`          | PK                                          |
| `player`                | `Player`        | N:1                                         |
| `paytable`              | `Paytable`      | N:1                                         |
| `bet`                   | `BigDecimal`    | base bet nebo původní bet session           |
| `totalWin`              | `BigDecimal`    | výsledná výhra                              |
| `spinType`              | `SpinType`      | `BASE`, `FREE_SPIN`, `PURCHASED_FREE_SPINS` |
| `parentSpin`            | `Spin`          | nullable self-reference                     |
| `childSpins`            | `List<Spin>`    | 1:N self-reference                          |
| `accumulatedMultiplier` | `int`           | session state pro free spins                |
| `remainingFreeSpins`    | `int`           | session state pro free spins                |
| `spunAt`                | `LocalDateTime` | čas spinu                                   |
| `tumbles`               | `List<Tumble>`  | 1:N                                         |

#### `Tumble`

Tabulka `tumbles`.

| Field              | Typ          | Poznámka                                             |
| ------------------ | ------------ | ---------------------------------------------------- |
| `id`               | `Long`       | PK                                                   |
| `spin`             | `Spin`       | N:1                                                  |
| `sequenceIndex`    | `int`        | 0, 1, 2, ...                                         |
| `gridState`        | `String`     | JSON gridu pro replay                                |
| `winAmount`        | `BigDecimal` | výhra tumble kroku před finálním multiplier výpočtem |
| `multiplierOnGrid` | `int`        | součet multiplier hodnot na gridu v daném kroku      |

### 6.2 Vazby

| Zdroj      | Cíl            | Kardinalita | Implementace                          |
| ---------- | -------------- | ----------- | ------------------------------------- |
| `Player`   | `Spin`         | 1:N         | `Spin.player`                         |
| `Paytable` | `Spin`         | 1:N         | `Spin.paytable`                       |
| `Paytable` | `SymbolConfig` | 1:N         | `SymbolConfig.paytable`               |
| `Spin`     | `Tumble`       | 1:N         | `Tumble.spin`                         |
| `Spin`     | `Spin`         | 1:N         | `Spin.parentSpin` / `Spin.childSpins` |

---

## 7. REST API

Base URL:

```text
/api
```

Content type:

```text
application/json
```

CORS:

```text
http://localhost:5173
```

### 7.1 `GET /api/player`

Vrátí seed hráče.

Response 200:

```json
{
  "id": 1,
  "username": "player",
  "balance": "1000.00"
}
```

### 7.2 `POST /api/player/reset`

Resetuje balance seed hráče na `1000.00` a ukončí aktivní free spin session tím, že nastaví zbývající free spiny na `0`.

Response 200:

```json
{
  "id": 1,
  "username": "player",
  "balance": "1000.00"
}
```

### 7.3 `GET /api/paytables`

Vrátí dostupné paytables.

Response 200:

```json
[
  {
    "id": 1,
    "name": "Gates of Olympus",
    "reelCount": 6,
    "rowCount": 5,
    "symbols": [
      {
        "code": "CROWN",
        "symbolType": "REGULAR",
        "payouts": {
          "low": "10.00",
          "mid": "25.00",
          "high": "50.00"
        }
      }
    ]
  }
]
```

Poznámka: response obsahuje všech 10 symbolů. Příklad je zkrácený.

### 7.4 `POST /api/spin`

Spustí base spin.

Request:

```json
{
  "paytableId": 1,
  "bet": "1.00"
}
```

Response 200:

```json
{
  "spinId": 42,
  "spinType": "BASE",
  "bet": "1.00",
  "totalWin": "5.00",
  "newBalance": "1004.00",
  "tumbles": [
    {
      "sequenceIndex": 0,
      "grid": [
        [
          { "code": "GEM_BLUE" },
          { "code": "CROWN" },
          { "code": "MULTIPLIER", "multiplierValue": 5 }
        ]
      ],
      "winAmount": "1.00",
      "multiplierOnGrid": 5
    }
  ],
  "freeSpinsTriggered": false,
  "freeSpinsAwarded": 0
}
```

Poznámka: skutečný grid má vždy 5 řádků a každý řádek má 6 buněk. Příklad je zkrácený.

### 7.5 `POST /api/spin/buy`

Koupí free spin session.

Request:

```json
{
  "paytableId": 1,
  "bet": "1.00"
}
```

Response 200:

```json
{
  "spinId": 100,
  "spinType": "PURCHASED_FREE_SPINS",
  "cost": "100.00",
  "newBalance": "900.00",
  "freeSpinsAwarded": 15,
  "parentSpinId": 100
}
```

### 7.6 `POST /api/spin/free`

Spotřebuje jeden free spin z parent session.

Request:

```json
{
  "parentSpinId": 100
}
```

Response 200:

```json
{
  "spinId": 101,
  "spinType": "FREE_SPIN",
  "bet": "0.00",
  "totalWin": "48.00",
  "newBalance": "948.00",
  "tumbles": [
    {
      "sequenceIndex": 0,
      "grid": [
        [{ "code": "GEM_BLUE" }, { "code": "MULTIPLIER", "multiplierValue": 8 }]
      ],
      "winAmount": "6.00",
      "multiplierOnGrid": 8
    }
  ],
  "accumulatedMultiplier": 8,
  "remainingFreeSpins": 14,
  "retriggered": false,
  "retriggerAwarded": 0,
  "sessionComplete": false
}
```

### 7.7 `GET /api/player/history?limit=20`

Vrátí poslední spiny seed hráče. `limit` se normalizuje na rozsah `1..100`.

Response 200:

```json
{
  "spins": [
    {
      "id": 42,
      "spinType": "BASE",
      "bet": "1.00",
      "totalWin": "5.00",
      "spunAt": "2026-04-24T10:15:30",
      "tumbleCount": 2
    }
  ]
}
```

### 7.8 `GET /api/spin/{id}`

Vrátí detail spinu včetně tumble gridů.

Response 200:

```json
{
  "id": 42,
  "spinType": "BASE",
  "bet": "1.00",
  "totalWin": "5.00",
  "spunAt": "2026-04-24T10:15:30",
  "accumulatedMultiplier": 0,
  "remainingFreeSpins": 0,
  "tumbles": [
    {
      "sequenceIndex": 0,
      "grid": [
        [{ "code": "GEM_BLUE" }, { "code": "MULTIPLIER", "multiplierValue": 5 }]
      ],
      "winAmount": "1.00",
      "multiplierOnGrid": 5
    }
  ]
}
```

U child free spinu obsahuje response také `parentSpinId`.
Grid je v příkladu zkrácený; reálný grid má 5 řádků a 6 buněk na řádek.

---

## 8. Chyby

Všechny očekávané aplikační chyby mají tvar:

```json
{
  "error": "INSUFFICIENT_BALANCE",
  "message": "Balance 0.50 is less than bet 1.00"
}
```

Používané chyby:

| HTTP | `error`                    | Kdy vznikne                                                |
| ---- | -------------------------- | ---------------------------------------------------------- |
| 400  | `INVALID_REQUEST`          | Chybějící/malformed JSON nebo validační chyba requestu.    |
| 400  | `INVALID_BET`              | Bet je `null`, `<= 0`, nebo má více než 2 desetinná místa. |
| 402  | `INSUFFICIENT_BALANCE`     | Hráč nemá dost balance na bet nebo buy cost.               |
| 404  | `PAYTABLE_NOT_FOUND`       | `paytableId` neexistuje.                                   |
| 404  | `SPIN_NOT_FOUND`           | `spinId` nebo `parentSpinId` neexistuje.                   |
| 409  | `INVALID_FREE_SPIN_PARENT` | Klient použije child `FREE_SPIN` jako parent session.      |
| 409  | `FREE_SPINS_EXHAUSTED`     | Session nemá žádné zbývající free spiny.                   |

---

## 9. Seed Data

Při startu aplikace `SeedDataInitializer` vytvoří:

- `Player(username = "player", balance = 1000.00)`.
- `Paytable(name = "Gates of Olympus", reelCount = 6, rowCount = 5, minMatchCount = 8, scatterTriggerCount = 4, freeSpinsAwarded = 15, freeSpinsRetrigger = 5, buyFreeSpinsMultiplier = 100)`.
- 10 `SymbolConfig` záznamů podle tabulky symbolů v této specifikaci.

Seed data se nevytváří znovu, pokud už existují.

---

## 10. Testy

Testovací soubor:

```text
src/test/java/com/adamekka/spins_backend/SpinsBackendApplicationTests.java
```

Pokryté scénáře:

- Spring context load.
- `GET /api/player` vrací seed hráče.
- `POST /api/player/reset` obnoví balance.
- `GET /api/paytables` vrací seeded paytable.
- `POST /api/spin` vytvoří spin a tumbles.
- `GET /api/spin/{id}` vrací detail.
- `GET /api/player/history` vrací historii.
- `POST /api/spin/buy` vytvoří free spin session.
- `POST /api/spin/free` spotřebuje session.
- Child free spin nejde použít jako parent.
- Invalid bet scale vrací `INVALID_BET`.
- Nedostatečný balance vrací `INSUFFICIENT_BALANCE`.

Aktuálně ověřeno:

```text
mvn test
```

---
