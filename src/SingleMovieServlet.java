import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
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
* GET /api/single-movie?id=tt0395642
* returns everything about one movie as JSON, with title, year, director, rating
 * all genres {id, name} sorted by name
 * all stars {id, name} sorted by number of movies played, then name
 * answers 400 if id is missing and 404 if don't exist
* */

@WebServlet(name = "SingleMovieServlet", urlPatterns = "/api/single-movie")
public class SingleMovieServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private static final Gson GSON = new GsonBuilder().serializeNulls().create();

    // left join, movies with no rating still need to show up (?)
    private static final String MOVIE_SQL =
            "SELECT m.id, m.title, m.year, m.director, r.rating "
                    + "FROM movies m LEFT JOIN ratings r ON r.movie_id = m.id "
                    + "WHERE m.id = ?";


    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();

        String id = request.getParameter("id");
        if (id == null || id.trim().isEmpty()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.write(errorJson("movie id missing"));
            return;
        }

        try (Connection conn = Db.getConnection();
             PreparedStatement movieStmt = conn.prepareStatement(MOVIE_SQL);
             PreparedStatement genresStmt = MovieDetails.prepareGenres(conn, 0);
             PreparedStatement starsStmt = MovieDetails.prepareStars(conn, 0)) {

            movieStmt.setString(1, id);
            JsonObject movie = new JsonObject();

            try (ResultSet rs = movieStmt.executeQuery()) {
                if (!rs.next()) {
                    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    out.write(errorJson("Movie not found"));
                    return;
                }
                movie.addProperty("id", rs.getString("id"));
                movie.addProperty("title", rs.getString("title"));
                movie.addProperty("year", rs.getInt("year"));
                movie.addProperty("director", rs.getString("director"));

                double rating = rs.getDouble("rating");
                if (rs.wasNull()) {
                    movie.add("rating", com.google.gson.JsonNull.INSTANCE);
                } else {
                    movie.addProperty("rating", rating);
                }
            }

            movie.add("genres", MovieDetails.genresOf(genresStmt, id));
            movie.add("stars", MovieDetails.starsOf(starsStmt, id));
            out.write(GSON.toJson(movie));

        } catch (SQLException e) {
            log("database error while loading movie " + id, e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.write(errorJson("could not load the movie"));
        }
    }

    private static String errorJson(String message) {
        JsonObject error = new JsonObject();
        error.addProperty("error", message);
        return GSON.toJson(error);
    }

}
