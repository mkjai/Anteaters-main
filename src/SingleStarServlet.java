import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonNull;
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


/*
 * GET /api/single-star?id=nm0000123
 * returns JSON array -> one object per movie that the star acted in
 * [{ star_id, star_name, star_dob, movie_id, movie_title, movie_year, movie_director }, ...]
 */

@WebServlet(name = "SingleStarServlet", urlPatterns = "/api/single-star")
public class SingleStarServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private static final Gson GSON = new GsonBuilder().serializeNulls().create();

    // left join so a star with no movies still comes back
    private static final String STAR_SQL =
            "SELECT DISTINCT s.id AS star_id, s.name AS star_name, s.birth_year AS star_dob, "
                    + "m.id AS movie_id, m.title AS movie_title, m.year AS movie_year, m.director AS movie_director "
                    + "FROM stars s "
                    + "LEFT JOIN stars_in_movies sm ON sm.star_id = s.id "
                    + "LEFT JOIN movies m ON m.id = sm.movie_id "
                    + "WHERE s.id = ? "
                    + "ORDER BY movie_year DESC, movie_title ASC, movie_id ASC";


    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();

        String id = request.getParameter("id");
        if (id == null || id.trim().isEmpty()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.write(errorJson("star id missing"));
            return;
        }

        try (Connection conn = Db.getConnection();
             PreparedStatement stmt = conn.prepareStatement(STAR_SQL)) {

            stmt.setString(1, id);
            JsonArray rows = new JsonArray();

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    JsonObject row = new JsonObject();
                    row.addProperty("star_id", rs.getString("star_id"));
                    row.addProperty("star_name", rs.getString("star_name"));

                    int dob = rs.getInt("star_dob");
                    if (rs.wasNull()) {
                        row.add("star_dob", JsonNull.INSTANCE);
                    } else {
                        row.addProperty("star_dob", dob);
                    }

                    row.addProperty("movie_id", rs.getString("movie_id"));
                    row.addProperty("movie_title", rs.getString("movie_title"));

                    int year = rs.getInt("movie_year");
                    if (rs.wasNull()) {
                        row.add("movie_year", JsonNull.INSTANCE);
                    } else {
                        row.addProperty("movie_year", year);
                    }

                    row.addProperty("movie_director", rs.getString("movie_director"));
                    rows.add(row);
                }
            }

            if (rows.isEmpty()) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                out.write(errorJson("Star not found"));
                return;
            }

            out.write(GSON.toJson(rows));

        } catch (SQLException e) {
            log("database error while loading star " + id, e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.write(errorJson("could not load the star"));
        }
    }

    private static String errorJson(String message) {
        JsonObject error = new JsonObject();
        error.addProperty("error", message);
        return GSON.toJson(error);
    }
}
