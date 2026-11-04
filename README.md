# Booking API

Ett REST API för bordsbokningar. Systemet kör tre instanser av samma Spring Boot-applikation bakom Nginx. Alla instanser använder en gemensam PostgreSQL-databas.

## Teknik

- Java 21 och Spring Boot
- PostgreSQL 17
- Docker och Docker Compose
- Nginx som reverse proxy och load balancer

## Arkitektur

Klienten skickar requests till Nginx på http://localhost:8080.

Nginx fördelar requests mellan api1, api2 och api3. Alla tre instanser läser och skriver bokningar i samma PostgreSQL-databas.

## Förutsättningar

Docker Desktop med Docker Compose ska vara installerat och startat.

Java och Maven behöver inte installeras lokalt eftersom applikationen byggs i Docker.

Kommandona nedan är avsedda för Bash eller Zsh.

## Konfiguration

Klona projektet och gå till projektmappen:

```bash
git clone https://github.com/TonySkengman/booking-api.git
cd booking-api
```

Skapa den lokala konfigurationsfilen:

```bash
cp .env.example .env
```

Ange ett lokalt databaslösenord i `.env`.

| Variabel | Beskrivning |
|---|---|
| DB_NAME | Databasens namn |
| DB_USER | Databasanvändare |
| DB_PASSWORD | Databaslösenord |
| DB_HOST | Databasadress vid lokal körning, normalt localhost |
| DB_PORT | Databasport vid lokal körning, normalt 5432 |

Docker Compose läser DB_NAME, DB_USER och DB_PASSWORD från `.env`. För API-containrarna anges DB_HOST till `db` och DB_PORT till `5432` i `compose.yaml`.

INSTANCE_NAME och PORT anges också i Compose-filen. Instanserna heter api-1, api-2 och api-3 och använder port 8080 internt.

`.env` ignoreras av Git. `.env.example` innehåller bara exempelvärden.

## Starta systemet

Vid första starten, starta databasen och den första API-instansen:

```bash
docker compose up -d --build api1
docker compose logs -f api1
```

Vänta tills loggen visar `Started BookingApiApplication`. Avsluta loggvisningen med Ctrl+C. Containern fortsätter köra.

Detta låter den första instansen skapa databastabellerna innan övriga instanser startas.

Starta sedan resten av systemet:

```bash
docker compose up -d --build
```

Vänta tills alla API-instanser har startat. Nginx kan tillfälligt svara med 502 medan applikationerna startar.

Kontrollera tjänsterna:

```bash
docker compose ps
```

Följande tjänster ska köra:

- nginx
- db
- api1
- api2
- api3

API:et nås genom Nginx på:

http://localhost:8080

API-instanserna och databasen har inga direkt publicerade portar.

Vid senare starter:

```bash
docker compose up -d
```

Efter ändringar i Java-koden:

```bash
docker compose up -d --build
```

## REST API

| Metod | Endpoint | Funktion |
|---|---|---|
| GET | /bookings | Hämta alla bokningar |
| GET | /bookings/{id} | Hämta en bokning |
| POST | /bookings | Skapa en bokning |
| PUT | /bookings/{id} | Uppdatera en bokning |
| DELETE | /bookings/{id} | Ta bort en bokning |
| GET | /instance | Visa vilken instans som hanterade requesten |

En bokning innehåller namn, e-postadress, datum och tid, antal personer samt status.

Tillåtna statusvärden är PENDING, CONFIRMED och CANCELLED.

## Testa load balancing

```bash
for i in {1..9}; do
  curl -sS http://localhost:8080/instance
  echo
done
```

Svaren ska innehålla alla tre instansnamnen, exempelvis:

```json
{"instance":"api-1"}
{"instance":"api-2"}
{"instance":"api-3"}
```

## Testa bokningar

Skapa en bokning:

```bash
curl -i -X POST http://localhost:8080/bookings -H "Content-Type: application/json" -d '{"name":"Testperson","email":"test@example.com","dateTime":"2026-12-10T18:00:00","numberOfPeople":4,"status":"PENDING"}'
```

Förväntat: 201 och en bokning med ett genererat id.

Spara id-värdet och ersätt DITT-ID i kommande kommandon.

Hämta alla bokningar:

```bash
curl -i http://localhost:8080/bookings
```

Hämta bokningen:

```bash
curl -i http://localhost:8080/bookings/DITT-ID
```

Förväntat: 200 och bokningen.

Uppdatera bokningen:

```bash
curl -i -X PUT http://localhost:8080/bookings/DITT-ID -H "Content-Type: application/json" -d '{"name":"Testperson","email":"test@example.com","dateTime":"2026-12-10T18:00:00","numberOfPeople":6,"status":"CONFIRMED"}'
```

Förväntat: 200 och de uppdaterade värdena.

## Testa en stoppad instans

Behåll testbokningen under detta test.

Stoppa api2:

```bash
docker compose stop api2
docker compose ps -a
```

Kör load balancing-testet igen. Requests ska fortfarande lyckas, men svaren ska bara innehålla api-1 och api-3.

Hämta bokningen genom Nginx:

```bash
curl -i http://localhost:8080/bookings/DITT-ID
```

Förväntat: 200 och samma bokning.

Kontrollera bokningarna direkt i PostgreSQL:

```bash
docker compose exec db sh -c 'psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -c "SELECT * FROM bookings;"'
```

Starta instansen igen:

```bash
docker compose start api2
```

När applikationen har startat kan det dröja ungefär tio sekunder innan Nginx använder instansen igen.

## Testa persistent lagring

Med testbokningen kvar, ta ner systemet:

```bash
docker compose down
```

Starta igen:

```bash
docker compose up -d
```

Vänta tills applikationerna har startat och hämta bokningen:

```bash
curl -i http://localhost:8080/bookings/DITT-ID
```

Bokningen och dess uppdaterade värden ska finnas kvar. PostgreSQL sparar data i Docker-volymen `postgres_data`.

Använd inte `docker compose down -v` om data ska behållas. Flaggan `-v` tar även bort databasvolymen.

## Testa borttagning

Ta bort testbokningen efter övriga tester:

```bash
curl -i -X DELETE http://localhost:8080/bookings/DITT-ID
```

Förväntat: 204 utan svarsinnehåll.

Hämta samma id igen:

```bash
curl -i http://localhost:8080/bookings/DITT-ID
```

Förväntat: 404.

## Loggar och felsökning

Visa loggar:

```bash
docker compose logs --tail=50
```

Kontrollera Nginx-konfigurationen:

```bash
docker compose exec nginx nginx -t
```

Efter ändringar i nginx.conf, kontrollera konfigurationen och ladda sedan om den:

```bash
docker compose exec nginx nginx -s reload
```

Port 8080 på datorn måste vara ledig när systemet startas.

## Stoppa systemet

```bash
docker compose down
```

Databasens data behålls i Docker-volymen.