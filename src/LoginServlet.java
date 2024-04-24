import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

@WebServlet(name = "LoginServlet", urlPatterns = "/Fablix")
public class LoginServlet extends HttpServlet {

    private DataSource dataSource;

    public void init(ServletConfig config) {
        try {
            dataSource = (DataSource) new InitialContext().lookup("java:comp/env/jdbc/moviedb");
        } catch (NamingException e) {
            e.printStackTrace();
        }
    }

    public void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {

        String email = request.getParameter("email");
        String password = request.getParameter("password");
        // Inside the doPost method

        System.out.println(email + " " + password);
        boolean isValidUser = false;

        try (Connection dbCon = dataSource.getConnection();
                PreparedStatement statement = dbCon
                        .prepareStatement("SELECT * FROM customers WHERE email = ? AND password = ?")) {

            statement.setString(1, email);
            statement.setString(2, password);
            ResultSet rs = statement.executeQuery();

            if (rs.next()) {
                Customer customer = new Customer(
                        rs.getInt("id"),
                        rs.getString("firstName"),
                        rs.getString("lastName"),
                        rs.getString("ccId"),
                        rs.getString("address"),
                        rs.getString("email"),
                        rs.getString("password"));
                isValidUser = true;
                HttpSession session = request.getSession();
                session.setAttribute("customer", customer); // or store user object
            }
            rs.close();
            response.setStatus(200);

        } catch (Exception e) {
            e.printStackTrace();
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Server error: " + e.getMessage());
            return;
        }

        if (isValidUser) {
            // Inside the doPost method, after successful authentication
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write("{\"message\": \"Credentials verified\"}");

            response.sendRedirect("searchandbrowsepage.html"); // Redirect to the main page
        } else {
            request.setAttribute("loginError", "Invalid email or password.");
            request.getRequestDispatcher("login.html").forward(request, response);
        }
    }
}
