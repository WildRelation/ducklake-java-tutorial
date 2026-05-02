import java.sql.*;

public class Main {
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
