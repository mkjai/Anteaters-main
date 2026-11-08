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
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Map;


@WebServlet(name = "PlaceOrderServlet", urlPatterns = "/api/place-order")
public class PlaceOrderServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private static final Gson GSON = new GsonBuilder().serializeNulls().create();

    public static final String LAST_ORDER_KEY = "lastOrder";

    private static final String CARD_SQL =
            "SELECT id FROM credit_cards "
                    + "WHERE id = ? AND first_name = ? AND last_name = ? AND expiration = ?";

    // one row for every copy that is bought
    private static final String SALE_SQL =
            "INSERT INTO sales (customer_id, movie_id, sale_date) VALUES (?, ?, CURDATE())";

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();

        String firstName = param(request, "firstName");
        String lastName = param(request, "lastName");
        String cardNumber = param(request, "cardNumber");
        String expiration = param(request, "expiration");

        if (firstName == null || lastName == null || cardNumber == null || expiration == null) {
            fail(response, out, HttpServletResponse.SC_BAD_REQUEST, "Please fill in every field.");
            return;
        }

        Date expirationDate;
        try {
            expirationDate = Date.valueOf(expiration);
        } catch (IllegalArgumentException e) {
            fail(response, out, HttpServletResponse.SC_BAD_REQUEST,
                    "The expiration date must be a real date, like 2027-05-31.");
            return;
        }

        HttpSession session = request.getSession();
        Object customer = session.getAttribute(LoginServlet.SESSION_KEY);
        if (!(customer instanceof Integer)) {
            fail(response, out, HttpServletResponse.SC_UNAUTHORIZED, "Please log in again.");
            return;
        }
        int customerId = (Integer) customer;

        Map<String, Integer> cart = CartServlet.cartOf(session);

        try (Connection conn = Db.getConnection()) {
            conn.setAutoCommit(false);
            try {
                List<CartServlet.Line> lines = CartServlet.loadLines(conn, CartServlet.snapshot(session));
                if (lines.isEmpty()) {
                    fail(response, out, HttpServletResponse.SC_BAD_REQUEST, "Your shopping cart is empty.");
                    return;
                }

                // the payment information has to match a credit card in the database
                try (PreparedStatement cardStmt = conn.prepareStatement(CARD_SQL)) {
                    cardStmt.setString(1, cardNumber);
                    cardStmt.setString(2, firstName);
                    cardStmt.setString(3, lastName);
                    cardStmt.setDate(4, expirationDate);
                    try (ResultSet rs = cardStmt.executeQuery()) {
                        if (!rs.next()) {
                            fail(response, out, HttpServletResponse.SC_BAD_REQUEST,
                                    "The payment information is not correct. Please check it and try again.");
                            return;
                        }
                    }
                }

                // record the sale, one row per copy
                JsonArray saleIds = new JsonArray();
                try (PreparedStatement saleStmt = conn.prepareStatement(SALE_SQL, Statement.RETURN_GENERATED_KEYS)) {
                    for (CartServlet.Line line : lines) {
                        for (int copy = 0; copy < line.quantity; copy++) {
                            saleStmt.setInt(1, customerId);
                            saleStmt.setString(2, line.id);
                            saleStmt.executeUpdate();
                            try (ResultSet keys = saleStmt.getGeneratedKeys()) {
                                if (keys.next()) {
                                    saleIds.add(keys.getLong(1));
                                }
                            }
                        }
                    }
                }
                conn.commit();

                JsonObject order = CartServlet.toJson(lines);
                order.addProperty("status", "success");
                order.add("saleIds", saleIds);
                String orderJson = GSON.toJson(order);

                // the order is saved, so the cart is empty 
                synchronized (cart) {
                    cart.clear();
                }
                session.setAttribute(LAST_ORDER_KEY, orderJson);
                out.write(orderJson);

            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }

        } catch (SQLException e) {
            log("Database error while placing an order", e);
            fail(response, out, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Could not place the order right now. Please try again.");
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();

        HttpSession session = request.getSession(false);
        Object saved = session == null ? null : session.getAttribute(LAST_ORDER_KEY);
        if (saved instanceof String) {
            out.write((String) saved);
        } else {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            JsonObject error = new JsonObject();
            error.addProperty("error", "There is no recent order.");
            out.write(GSON.toJson(error));
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

    private static void fail(HttpServletResponse response, PrintWriter out, int status, String message) {
        response.setStatus(status);
        JsonObject json = new JsonObject();
        json.addProperty("status", "fail");
        json.addProperty("message", message);
        out.write(GSON.toJson(json));
    }
}