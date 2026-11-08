import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


@WebServlet(name = "CartServlet", urlPatterns = "/api/cart")
public class CartServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private static final Gson GSON = new GsonBuilder().serializeNulls().create();

    public static final String CART_KEY = "cart";
    private static final int MAX_QUANTITY = 99;

    // Every movie gets a price between 5.00 and 19.99
    // "random" but always the same for one movie, on every computer and after every reload of the data
    static final String PRICE_SQL = "ROUND(5 + (CRC32(m.id) MOD 1500) / 100, 2)";

    private static final String MOVIE_SQL =
            "SELECT m.title, " + PRICE_SQL + " AS price FROM movies m WHERE m.id = ?";

    // one line of the cart
    static final class Line {
        final String id;
        final String title;
        final BigDecimal price;
        final int quantity;

        Line(String id, String title, BigDecimal price, int quantity) {
            this.id = id;
            this.title = title;
            this.price = price;
            this.quantity = quantity;
        }

        BigDecimal subtotal() {
            return price.multiply(BigDecimal.valueOf(quantity));
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();

        try (Connection conn = Db.getConnection()) {
            out.write(GSON.toJson(toJson(loadLines(conn, snapshot(request.getSession())))));
        } catch (SQLException e) {
            log("Database error while loading the cart", e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.write(errorJson("Could not load the shopping cart"));
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();

        String action = param(request, "action");
        String id = param(request, "id");
        if (action == null || id == null) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.write(errorJson("action and id are needed"));
            return;
        }

        HttpSession session = request.getSession();
        Map<String, Integer> cart = cartOf(session);

        try (Connection conn = Db.getConnection()) {
            switch (action) {
                case "add":
                    // only real movies can go in the cart
                    if (!movieExists(conn, id)) {
                        response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                        out.write(errorJson("Movie not found"));
                        return;
                    }
                    synchronized (cart) {
                        int now = cart.getOrDefault(id, 0);
                        cart.put(id, Math.min(now + 1, MAX_QUANTITY));
                    }
                    break;

                case "increase":
                    synchronized (cart) {
                        if (cart.containsKey(id)) {
                            cart.put(id, Math.min(cart.get(id) + 1, MAX_QUANTITY));
                        }
                    }
                    break;

                case "decrease":
                    // stops at 1, the delete button is for taking a movie out
                    synchronized (cart) {
                        if (cart.containsKey(id)) {
                            cart.put(id, Math.max(cart.get(id) - 1, 1));
                        }
                    }
                    break;

                case "remove":
                    synchronized (cart) {
                        cart.remove(id);
                    }
                    break;

                default:
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    out.write(errorJson("Unknown action"));
                    return;
            }

            out.write(GSON.toJson(toJson(loadLines(conn, snapshot(session)))));

        } catch (SQLException e) {
            log("Database error while changing the cart", e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.write(errorJson("Could not change the shopping cart"));
        }
    }

    // the cart of this session, a new empty one the first time
    @SuppressWarnings("unchecked")
    static Map<String, Integer> cartOf(HttpSession session) {
        synchronized (session) {
            Object saved = session.getAttribute(CART_KEY);
            if (saved instanceof Map) {
                return (Map<String, Integer>) saved;
            }
            Map<String, Integer> cart = new LinkedHashMap<>();
            session.setAttribute(CART_KEY, cart);
            return cart;
        }
    }

    // a copy, so it can be read while another request changes the real cart
    static Map<String, Integer> snapshot(HttpSession session) {
        Map<String, Integer> cart = cartOf(session);
        synchronized (cart) {
            return new LinkedHashMap<>(cart);
        }
    }

    // title and price of each movie in the cart, from the database. Movies that no longer exist are skipped
    static List<Line> loadLines(Connection conn, Map<String, Integer> cart) throws SQLException {
        List<Line> lines = new ArrayList<>();
        try (PreparedStatement stmt = conn.prepareStatement(MOVIE_SQL)) {
            for (Map.Entry<String, Integer> entry : cart.entrySet()) {
                stmt.setString(1, entry.getKey());
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        lines.add(new Line(entry.getKey(), rs.getString("title"),
                                rs.getBigDecimal("price"), entry.getValue()));
                    }
                }
            }
        }
        return lines;
    }

    static BigDecimal totalOf(List<Line> lines) {
        BigDecimal total = BigDecimal.ZERO;
        for (Line line : lines) {
            total = total.add(line.subtotal());
        }
        return total;
    }

    static JsonObject toJson(List<Line> lines) {
        JsonArray items = new JsonArray();
        int count = 0;
        for (Line line : lines) {
            JsonObject item = new JsonObject();
            item.addProperty("id", line.id);
            item.addProperty("title", line.title);
            item.addProperty("price", line.price);
            item.addProperty("quantity", line.quantity);
            item.addProperty("subtotal", line.subtotal());
            items.add(item);
            count += line.quantity;
        }
        JsonObject cart = new JsonObject();
        cart.add("items", items);
        cart.addProperty("count", count);
        cart.addProperty("total", totalOf(lines));
        return cart;
    }

    private static boolean movieExists(Connection conn, String id) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement("SELECT 1 FROM movies WHERE id = ?")) {
            stmt.setString(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        }
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

    private static String errorJson(String message) {
        JsonObject error = new JsonObject();
        error.addProperty("error", message);
        return GSON.toJson(error);
    }
}