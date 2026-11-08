import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

@WebServlet(name = "GenresServlet", urlPatterns = "/api/genres")
public class GenresServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private static final Gson GSON = new Gson();

    private static final String GENRES_SQL = "SELECT id, name FROM genres ORDER BY name";

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();

        try (Connection conn = Db.getConnection();
             PreparedStatement stmt = conn.prepareStatement(GENRES_SQL);
             ResultSet rs = stmt.executeQuery()) {

            JsonArray genres = new JsonArray();
            while (rs.next()) {
                JsonObject genre = new JsonObject();
                genre.addProperty("id", rs.getInt("id"));
                genre.addProperty("name", rs.getString("name"));
                genres.add(genre);
            }
            out.write(GSON.toJson(genres));

        } catch (SQLException e) {
            log("Database error while loading genres", e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            JsonObject error = new JsonObject();
            error.addProperty("error", "Could not load genres");
            out.write(GSON.toJson(error));
        }
    }
}