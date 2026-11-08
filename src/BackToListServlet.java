import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;


@WebServlet(name = "BackToListServlet", urlPatterns = "/api/back-to-list")
public class BackToListServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        String target = request.getContextPath() + "/index.html";

        HttpSession session = request.getSession(false);
        if (session != null) {
            Object saved = session.getAttribute(MoviesServlet.LIST_STATE_KEY);
            if (saved instanceof String && !((String) saved).isEmpty()) {
                target += "?" + saved;
            }
        }
        response.sendRedirect(target);
    }
}