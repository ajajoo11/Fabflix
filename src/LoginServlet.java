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

        String email = request.getParameter("email");
        String password = request.getParameter("password");
        System.out.println("Authenticating user: " + email); // Debug print

        boolean isValidUser = false;

        try (Connection dbCon = dataSource.getConnection();
                PreparedStatement statement = dbCon
                        .prepareStatement("SELECT * FROM customers WHERE email = ?")) {

            statement.setString(1, email);
            ResultSet rs = statement.executeQuery();

            if (rs.next()) {
                // User email exists, check password
                if (Objects.equals(rs.getString("password"), password)) {
                    // Password is correct, redirect to home page
                    isValidUser = true;
                    HttpSession session = request.getSession(true);
                    session.setAttribute("email", email); // Set session attribute
                    System.out.println("User logged in successfully. Email: " + email); // Debug print
                    response.sendRedirect("/Fabflix/searchandbrowsepage.html");
                    return; // Stop further execution
                } else {
                    // Password is incorrect, show password error message
                    String errorMessage = "Invalid password. Please try again.";
                    response.sendRedirect("/Fabflix/login.html?message=" + errorMessage);
                    return; // Stop further execution
                }
            } else {
                // User email doesn't exist, show email error message
                String errorMessage = "Invalid email. Please try again.";
                response.sendRedirect("/Fabflix/login.html?message=" + errorMessage);
                return; // Stop further execution
            }

            // rs.close();

        } catch (Exception e) {
            e.printStackTrace();
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Server error: " + e.getMessage());
            return;
        }
    }

}