import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import java.util.ArrayList;
import java.util.List;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;

/*
* GET /api/movies
* search / browse: title, director, star, year, genre, startsWith
* list: sort (a key of SORTS), limit (10, 25, 50, 100)
*
* */

@WebServlet(name = "MoviesServlet", urlPatterns = "/api/movies")
public class MoviesServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    public static final String LIST_STATE_KEY = "movieListQuery";

    private static final Map<String, String> SORTS = Map.of(
            "rating_desc_title_asc",  "r.rating DESC, m.title ASC, m.id ASC",
            "rating_desc_title_desc", "r.rating DESC, m.title DESC, m.id ASC",
            "rating_asc_title_asc",   "r.rating ASC, m.title ASC, m.id ASC",
            "rating_asc_title_desc",  "r.rating ASC, m.title DESC, m.id ASC",
            "title_asc_rating_desc",  "m.title ASC, r.rating DESC, m.id ASC",
            "title_asc_rating_asc",   "m.title ASC, r.rating ASC, m.id ASC",
            "title_desc_rating_desc", "m.title DESC, r.rating DESC, m.id ASC",
            "title_desc_rating_asc",  "m.title DESC, r.rating ASC, m.id ASC");

    private static final String DEFAULT_SORT = "rating_desc_title_asc";
    private static final Set<Integer> PAGE_SIZES = Set.of(10, 25, 50, 100);
    private static final int DEFAULT_LIMIT = 25;

    private static final String[] CRITERIA = {"title", "year", "director", "star", "genre", "startsWith"};

    private static final String MOVIES_SELECT =
            "SELECT m.id, m.title, m.year, m.director, r.rating "
                    + "FROM movies m LEFT JOIN ratings r ON r.movie_id = m.id";

    private static final String COUNT_SELECT = "SELECT COUNT(*) FROM movies m";

    private static final Gson GSON = new GsonBuilder().serializeNulls().create();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
        throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();

        List<String> conditions = new ArrayList<>();
        List<Object> values = new ArrayList<>();

        String title = param(request, "title");
        if (title != null) {
            conditions.add("m.title LIKE ?");
            values.add("%" + escapeLike(title) + "%");
        }

        String director = param(request, "director");
        if (director != null) {
            conditions.add("m.director LIKE ?");
            values.add("%" + escapeLike(director) + "%");
        }

        String star = param(request, "star");
        if (star != null) {
            conditions.add("EXISTS (SELECT 1 FROM stars_in_movies sm JOIN stars s ON s.id = sm.star_id "
                    + "WHERE sm.movie_id = m.id AND s.name LIKE ?)");
            values.add("%" + escapeLike(star) + "%");
        }

        String year = param(request, "year");
        if (year != null) {
            try {
                values.add(Integer.parseInt(year));
                conditions.add("m.year = ?");
            } catch (NumberFormatException e) {
                badRequest(response, out, "Year must be a number");
                return;
            }
        }

        String genre = param(request, "genre");
        if (genre != null) {
            try {
                values.add(Integer.parseInt(genre));
                conditions.add("EXISTS (SELECT 1 FROM genres_in_movies gm "
                        + "WHERE gm.movie_id = m.id AND gm.genre_id = ?)");
            } catch (NumberFormatException e) {
                badRequest(response, out, "Genre must be a genre id");
                return;
            }
        }

        String startsWith = param(request, "startsWith");
        if (startsWith != null) {
            if (!startsWith.matches("[0-9A-Za-z]")) {
                badRequest(response, out, "startsWith must be one letter or digit");
                return;
            }
            // LOWER on both sides so it does not matter if the title is upper or lower case
            conditions.add("LOWER(m.title) LIKE ?");
            values.add(startsWith.toLowerCase() + "%");
        }


        String sort = param(request, "sort");
        if (sort == null) {
            sort = DEFAULT_SORT;
        } else if (!SORTS.containsKey(sort)) {
            badRequest(response, out, "Unknown sort order");
            return;
        }

        int limit = DEFAULT_LIMIT;
        String limitParam = param(request, "limit");
        if (limitParam != null) {
            try {
                limit = Integer.parseInt(limitParam);
            } catch (NumberFormatException e) {
                limit = -1;
            }
            if (!PAGE_SIZES.contains(limit)) {
                badRequest(response, out, "limit must be 10, 25, 50 or 100");
                return;
            }
        }

        int page = 1;
        String pageParam = param(request, "page");
        if (pageParam != null) {
            try {
                page = Integer.parseInt(pageParam);
            } catch (NumberFormatException e) {
                page = 0;
            }
            if (page < 1) {
                badRequest(response, out, "page must be a whole number, 1 or more");
                return;
            }
        }

        // remember this list so the single pages can come back to it
        request.getSession().setAttribute(LIST_STATE_KEY, listQuery(request, sort, limit, page));

        String where = conditions.isEmpty() ? "" : " WHERE " + String.join(" AND ", conditions);
        String countSql = COUNT_SELECT + where;
        String moviesSql = MOVIES_SELECT + where + " ORDER BY " + SORTS.get(sort) + " LIMIT ? OFFSET ?";

        // try closes the connection and statements even on errors
        try (Connection conn = Db.getConnection();
             PreparedStatement countStmt = conn.prepareStatement(countSql);
             PreparedStatement moviesStmt = conn.prepareStatement(moviesSql);
             PreparedStatement genresStmt = MovieDetails.prepareGenres(conn, 3);
             PreparedStatement starsStmt = MovieDetails.prepareStars(conn, 3)) {

            for (int i = 0; i < values.size(); i++) {
                countStmt.setObject(i + 1, values.get(i));
                moviesStmt.setObject(i + 1, values.get(i));
            }
            moviesStmt.setInt(values.size() + 1, limit);
            moviesStmt.setLong(values.size() + 2, (long) (page - 1) * limit);

            long total;
            try (ResultSet rs = countStmt.executeQuery()) {
                rs.next();
                total = rs.getLong(1);
            }

            JsonArray movieList = new JsonArray();
            try (ResultSet movies = moviesStmt.executeQuery()) {
                while (movies.next()) {
                    String id = movies.getString("id");

                    JsonObject movie = new JsonObject();
                    movie.addProperty("id", id);
                    movie.addProperty("title", movies.getString("title"));
                    movie.addProperty("year", movies.getInt("year"));
                    movie.addProperty("director", movies.getString("director"));

                    // a movie with no rating has a NULL here, getDouble would turn it into 0
                    double rating = movies.getDouble("rating");
                    if (movies.wasNull()) {
                        movie.add("rating", JsonNull.INSTANCE);
                    } else {
                        movie.addProperty("rating", rating);
                    }

                    movie.add("genres", MovieDetails.genresOf(genresStmt, id));
                    movie.add("stars", MovieDetails.starsOf(starsStmt, id));
                    movieList.add(movie);
                }
            }

            JsonObject result = new JsonObject();
            result.addProperty("page", page);
            result.addProperty("limit", limit);
            result.addProperty("sort", sort);
            result.addProperty("total", total);
            result.add("movies", movieList);
            out.write(GSON.toJson(result));

        } catch (SQLException e) {
            log("Database error while loading the movie list", e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            JsonObject error = new JsonObject();
            error.addProperty("error", "Could not load movies");
            out.write(GSON.toJson(error));
        }
    }

    private static String listQuery(HttpServletRequest request, String sort, int limit, int page) {
        StringBuilder query = new StringBuilder();
        for (String name : CRITERIA) {
            String value = param(request, name);
            if (value != null) {
                appendParam(query, name, value);
            }
        }
        appendParam(query, "sort", sort);
        appendParam(query, "limit", String.valueOf(limit));
        appendParam(query, "page", String.valueOf(page));
        return query.toString();
    }

    private static void appendParam(StringBuilder query, String name, String value) {
        if (query.length() > 0) {
            query.append('&');
        }
        query.append(name).append('=').append(URLEncoder.encode(value, StandardCharsets.UTF_8));
    }

    // the parameter's value without spaces around it, or null if missing or blank
    private static String param(HttpServletRequest request, String name) {
        String value = request.getParameter(name);
        if (value == null) {
            return null;
        }
        value = value.trim();
        return value.isEmpty() ? null : value;
    }

    private static String escapeLike(String text) {
        return text.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    // answers 400 with a json error message
    private static void badRequest(HttpServletResponse response, PrintWriter out, String message) {
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        JsonObject error = new JsonObject();
        error.addProperty("error", message);
        out.write(GSON.toJson(error));
    }





}