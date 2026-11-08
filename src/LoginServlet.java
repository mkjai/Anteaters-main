import com.google.gson.Gson;
import com.google.gson.JsonObject;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/*
 * POST /api/login
 * fields: email and password
 *
 * checks email and password against customers table
 * stores the customer's id in the session if success
 * wrong email or password should be 401 and the fail message
 *
 * only POST is implemented so the password never appears in the URL
 */
@WebServlet(name = "LoginServlet", urlPatterns = "/api/login")
public class LoginServlet extends HttpServlet {

    private static final long servialVersionUID = 1L;
    private static final Gson GSON = new Gson();

    public static final String SESSION_KEY = "customerId";

    private static final String LOGIN_SQL =
            "SELECT id FROM customers WHERE email = ? AND password = ?";

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();

        String email = request.getParameter("email");
        String password = request.getParameter("password");

        if (email == null || email.trim().isEmpty() || password == null || password.trim().isEmpty() || password == null || password.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);

            out.write(reply("fail", "Please enter your email and password"));
            return;

        }

        // need to add reCAPTCHA stuff here
        try {
            if (!RecaptchaVerifier.verify(request.getParameter("g-recaptcha-response"))) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                out.write(reply("fail", "Invalid Recaptcha Response"));
                return;
            }
        } catch (IOException e) {
            log("Could not do reCAPTCHA", e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.write(reply("fail", "Could not do reCAPTCHA right now. Please try again."));
            return;
        }

        try (Connection conn = Db.getConnection();
             PreparedStatement stmt = conn.prepareStatement(LOGIN_SQL)) {

            stmt.setString(1, email.trim());
            stmt.setString(2, password);

            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    // Same message for "no such email" and "wrong password", so the
                    // page does not reveal which emails exist.
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    out.write(reply("fail", "Incorrect email or password."));
                    return;
                }

                // Drop any old session first so a new login always gets a fresh one.
                HttpSession old = request.getSession(false);
                if (old != null) {
                    old.invalidate();
                }
                HttpSession session = request.getSession(true);
                session.setAttribute(SESSION_KEY, rs.getInt("id"));
            }

            out.write(reply("success", "Logged in."));

        } catch (SQLException e) {
            log("Database error during login", e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.write(reply("fail", "Could not log in right now. Please try again."));
        }
    }

    private static String reply(String code, String message) {

        JsonObject json = new JsonObject();
        json.addProperty("status", code);
        json.addProperty("message", message);
        return GSON.toJson(json);
    }

}

