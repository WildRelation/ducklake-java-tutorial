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

## Steg 4 — Skapa arbetsmapp och Java-fil

```bash
mkdir -p ~/ducklake-java/src/main/java && cd ~/ducklake-java
```

Kopiera `pom.xml` till `~/ducklake-java/` och skapa sedan Java-filen:

```bash
nano -w src/main/java/DuckLakeGeneric.java
```

Klistra in följande kod:

```java
import java.sql.*;

public class DuckLakeGeneric {
    public static void main(String[] args) {
        String s3Key      = System.getenv("AWS_ACCESS_KEY_ID");
        String s3Secret   = System.getenv("AWS_SECRET_ACCESS_KEY");
        String s3Endpoint = System.getenv("AWS_ENDPOINT_URL");
        String s3Region   = System.getenv("AWS_DEFAULT_REGION");
        String s3Bucket   = System.getenv("S3_BUCKET");

        String pgHost     = System.getenv("PGHOST");
        String pgPort     = System.getenv("PGPORT");
        String pgDatabase = System.getenv("PGDATABASE");
        String pgUser     = System.getenv("PGUSER");
        String pgPassword = System.getenv("PGPASSWORD");

        if (s3Key == null || pgPassword == null) {
            System.err.println("Fel: Miljövariabler saknas! Kör: export $(cat .env | grep -v '^#' | xargs)");
            return;
        }

        try {
            Class.forName("org.duckdb.DuckDBDriver");
            Connection con = DriverManager.getConnection("jdbc:duckdb:");
            Statement stmt = con.createStatement();

            stmt.execute("INSTALL ducklake");
            stmt.execute("LOAD ducklake");
            stmt.execute("INSTALL postgres");
            stmt.execute("LOAD postgres");

            stmt.execute(String.format(
                "CREATE OR REPLACE SECRET garage_secret (" +
                "TYPE s3, PROVIDER config, KEY_ID '%s', SECRET '%s', " +
                "REGION '%s', ENDPOINT '%s', URL_STYLE 'path', USE_SSL false)",
                s3Key, s3Secret, s3Region, s3Endpoint
            ));
            System.out.println("✓ S3/Garage secret configured");

            stmt.execute(String.format(
                "ATTACH 'ducklake:postgres:dbname=%s host=%s port=%s user=%s password=%s' AS my_data " +
                "(DATA_PATH 's3://%s/')",
                pgDatabase, pgHost, pgPort, pgUser, pgPassword, s3Bucket
            ));
            System.out.println("✓ DuckLake attached");

            System.out.println("\nTabeller i databasen:");
            ResultSet rs = stmt.executeQuery("SHOW ALL TABLES");
            while (rs.next()) System.out.println("- " + rs.getString("name"));
            rs.close();

            stmt.close();
            con.close();
            System.out.println("\n✓ Anslutning stängd");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
```

Spara med `Ctrl+O` → `Enter` → `Ctrl+X`.

Maven laddar automatiskt ner DuckDB JDBC när du kör projektet.

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
