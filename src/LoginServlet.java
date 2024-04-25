import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import java.io.PrintWriter;
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
import java.io.*;
import java.util.Objects;
import java.io.*;
import java.util.Objects;

@WebServlet(name = "LoginServlet", urlPatterns = "/Fabflix")
@WebServlet(name = "LoginServlet", urlPatterns = "/Fabflix")
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
        PrintWriter out = response.getWriter();
        PrintWriter out = response.getWriter();

        String email = request.getParameter("email");
        String password = request.getParameter("password");
        System.out.println("Authenticating user: " + email); // Debug print
        System.out.println("Authenticating user: " + email); // Debug print

        boolean isValidUser = false;
        HttpSession session = request.getSession();

        try (Connection dbCon = dataSource.getConnection();
             PreparedStatement statement = dbCon
                     .prepareStatement("SELECT * FROM customers WHERE email = ? AND password = ?")) {

            statement.setString(1, email);
            statement.setString(2, password);
            ResultSet rs = statement.executeQuery();

            if (rs.next()) {
                out.println("User authenticated."); // Debug print
                out.println("User authenticated."); // Debug print
                isValidUser = true;
                // HttpSession session = request.getSession();
                response.sendRedirect("/Fabflix/searchandbrowsepage.html");
//                session.setAttribute("customer", new Customer(
//                        rs.getInt("id"),
//                        rs.getString("firstName"),
//                        rs.getString("lastName"),
//                        rs.getString("ccId"),
//                        rs.getString("address"),
//                        rs.getString("email"),
//                        rs.getString("password")));
            } else {
                session.setAttribute("loginError", "Invalid email or password.");
                // Redirect to login.html
               // response.sendRedirect("login.html");
//                request.setAttribute("loginError", "Invalid email or password.");
//                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED); // Set response status to 401 Unauthorized
//                out.println("Authentication failed."); // Debug print
                response.sendRedirect("/Fabflix/login.html");
                // Set error message in request attribute
//                request.setAttribute("loginError", "Invalid email or password.");
                // Forward request to login.html
//                request.getRequestDispatcher("login.html").forward(request, response);
                // out.println("<html><body><h2>Error: Invalid email or password</h2></body></html>");
//                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
//                response.setContentType("application/json");
//                response.setCharacterEncoding("UTF-8");
//                response.getWriter().write("{\"error\": \"Invalid email or password.\"}");
//                return;
                HttpSession session = request.getSession();
                response.sendRedirect("/Fabflix/searchandbrowsepage.html");
                // session.setAttribute("customer", new Customer(
                // rs.getInt("id"),
                // rs.getString("firstName"),
                // rs.getString("lastName"),
                // rs.getString("ccId"),
                // rs.getString("address"),
                // rs.getString("email"),
                // rs.getString("password")));
            } else {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED); // Set response status to 401 Unauthorized
                out.println("Authentication failed."); // Debug print
                response.sendRedirect("/Fabflix/login.html");
                // out.println("<html><body><h2>Error: Invalid email or
                // password</h2></body></html>");
                // response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            }
            rs.close();

        } catch (Exception e) {
            e.printStackTrace();
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Server error: " + e.getMessage());
            return;
        }
//
//        if (isValidUser) {
//            response.sendRedirect("searchandbrowsepage.html");
//        } else {
//            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
//            request.setAttribute("loginError", "Invalid email or password.");
//            request.getRequestDispatcher("login.html").forward(request, response);
//        }
        //
        // if (isValidUser) {
        // response.sendRedirect("searchandbrowsepage.html");
        // } else {
        // response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        // request.setAttribute("loginError", "Invalid email or password.");
        // request.getRequestDispatcher("login.html").forward(request, response);
        // }
    }

}
