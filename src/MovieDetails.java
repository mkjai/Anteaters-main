import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

final class MovieDetails{
    private static final String GENRES_SQL =
            "SELECT DISTINCT g.id, g.name "
                    + "FROM genres g JOIN genres_in_movies gm ON gm.genre_id = g.id "
                    + "WHERE gm.movie_id = ? "
                    + "ORDER BY g.name, g.id";

    private static final String STARS_SQL =
            "SELECT s.id, s.name, COUNT(DISTINCT all_sm.movie_id) AS movie_count "
                    + "FROM stars_in_movies sm "
                    + "JOIN stars s ON s.id = sm.star_id "
                    + "JOIN stars_in_movies all_sm ON all_sm.star_id = s.id "
                    + "WHERE sm.movie_id = ? "
                    + "GROUP BY s.id, s.name "
                    + "ORDER BY movie_count DESC, s.name ASC, s.id ASC";

    private MovieDetails() {
    }

    static PreparedStatement prepareGenres(Connection conn, int limit) throws SQLException {
        return conn.prepareStatement(limit > 0 ? GENRES_SQL + " LIMIT " + limit : GENRES_SQL);
    }

    static PreparedStatement prepareStars(Connection conn, int limit) throws SQLException {
        return conn.prepareStatement(limit > 0 ? STARS_SQL + " LIMIT " + limit : STARS_SQL);
    }

    static JsonArray genresOf(PreparedStatement stmt, String movieId) throws SQLException {
        stmt.setString(1, movieId);
        JsonArray genres = new JsonArray();
        try (ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                JsonObject genre = new JsonObject();
                genre.addProperty("id", rs.getInt("id"));
                genre.addProperty("name", rs.getString("name"));
                genres.add(genre);
            }
        }
        return genres;
    }

    static JsonArray starsOf(PreparedStatement stmt, String movieId) throws SQLException {
        stmt.setString(1, movieId);
        JsonArray stars = new JsonArray();
        try (ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                JsonObject star = new JsonObject();
                star.addProperty("id", rs.getString("id"));
                star.addProperty("name", rs.getString("name"));
                stars.add(star);
            }
        }
        return stars;
    }
}