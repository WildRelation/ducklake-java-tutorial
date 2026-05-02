# Anslut till DuckLake från Java

En steg-för-steg tutorial för att ansluta till DuckLake via Java på en virtuell Linux-server (Ubuntu) på KTH Cloud.

> **OBS — Linux-distribution:** Det går att välja olika Linux-distributioner när du skapar en deployment på KTH Cloud. Det här tutorialet är skrivet för **Ubuntu** och använder `apt` som pakethanterare. Använder du en annan distro (t.ex. Fedora, Debian, Arch) kan kommandona för att installera paket se annorlunda ut.

## Förutsättningar

- En SSH-nyckel skapad och uppladdad till din profil på [KTH Cloud](https://cloud.cbh.kth.se)
- Credentials från [DuckLake Access Manager](https://ducklake-access-manager.app.cloud.cbh.kth.se/)

---

## Steg 1 — Anslut till servern

```bash
ssh <din deployment>@deploy.cloud.cbh.kth.se
```

---

## Steg 2 — Byt till bash-shell

```bash
bash
```

---

## Steg 3 — Installera Java och Maven

```bash
sudo apt update
sudo apt install -y default-jdk maven
```

Verifiera:

```bash
java -version
mvn -version
```

---

## Steg 4 — Skapa arbetsmapp

```bash
mkdir ~/ducklake-java && cd ~/ducklake-java
```

Kopiera filerna `DuckLakeGeneric.java` och `pom.xml` från detta repo hit. Maven laddar automatiskt ner DuckDB JDBC när du kör projektet.

---

## Steg 5 — Skapa .env-fil med dina credentials

Hämta dina credentials från [DuckLake Access Manager](https://ducklake-access-manager.app.cloud.cbh.kth.se/) och skapa filen:

```bash
nano -w .env
```

Klistra in och fyll i dina värden:

```bash
# Storage (S3/Garage)
AWS_ACCESS_KEY_ID=<DIN_ACCESS_KEY_ID>
AWS_SECRET_ACCESS_KEY=<DIN_SECRET_ACCESS_KEY>
AWS_ENDPOINT_URL=<DIN_ENDPOINT_URL>
AWS_DEFAULT_REGION=<DIN_REGION>
S3_BUCKET=<DITT_BUCKET_NAMN>

# Catalog (Postgres)
PGHOST=<DIN_POSTGRES_HOST>
PGPORT=5432
PGDATABASE=<DITT_DATABAS_NAMN>
PGUSER=<DITT_ANVÄNDARNAMN>
PGPASSWORD=<DITT_LÖSENORD>
```

Spara med `Ctrl+O` → `Enter` → `Ctrl+X`.

---

## Steg 6 — Ladda miljövariabler och kör

Ladda in `.env`-filen i shellet:

```bash
export $(cat .env | grep -v '^#' | grep -v '^\s*$' | xargs)
```

Kompilera och kör med Maven:

```bash
mvn compile exec:java
```

---

## Felsökning

| Problem | Lösning |
|---|---|
| `Permission denied (publickey)` | Din SSH-nyckel är inte uppladdad på KTH Cloud |
| `Miljövariabler saknas` | Kör `export`-kommandot i Steg 6 innan du kör Maven |
| Kompileringsfel med långa rader | Använd `nano -w` för att stänga av automatisk radbrytning |
| Filen försvann efter omstart | Lägg till persistent storage i deploymentet på KTH Cloud |
