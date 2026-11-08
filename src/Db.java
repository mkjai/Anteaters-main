import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class Db {

    private static final String URL =
            "jdbc:mysql://localhost:3306/moviedb?useSSL=false&allowPublicKeyRetrieval=true";
    private static final String USER = "mytestuser";
    private static final String PASSWORD = "My6$Password";

    static {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");

        } catch (ClassNotFoundException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private Db() {

    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}