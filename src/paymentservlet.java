import java.io.IOException;
import java.sql.*;
import javax.naming.InitialContext;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import javax.sql.DataSource;
import javax.naming.NamingException;
import jakarta.servlet.annotation.WebServlet;
import java.util.Date;
import java.text.SimpleDateFormat;

@WebServlet(name = "paymentservlet", urlPatterns = "/payment")
public class paymentservlet extends HttpServlet {
    private DataSource dataSource;

    @Override
    public void init() throws ServletException {
        try {
            dataSource = (DataSource) new InitialContext().lookup("java:comp/env/jdbc/moviedb");
        } catch (NamingException e) {
            e.printStackTrace();
        }
    }

    public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        HttpSession session = request.getSession();

        String firstName = request.getParameter("firstName");
        String lastName = request.getParameter("lastName");
        String creditCardNumber = request.getParameter("creditCardNumber");
        String expirationDate = request.getParameter("expirationDate");
        double totalPrice = Double.parseDouble(request.getParameter("totalPrice"));

        boolean isCreditCardValid = checkCreditCard(firstName, lastName, creditCardNumber, expirationDate);

        if (isCreditCardValid) {
            session.setAttribute("firstName", firstName);
            session.setAttribute("lastName", lastName);
            session.setAttribute("creditCardNumber", creditCardNumber);
            session.setAttribute("expirationDate", expirationDate);
            session.setAttribute("totalPrice", totalPrice);

            // Record the transaction in the "sales" table
            // recordSale(session);

            response.sendRedirect("paymentconfirmation.html"); // Redirect to payment confirmation page
        } else {
            // If credit card is not valid, redirect back to payment page with an error
            // message
            response.sendRedirect("payment.html?error=invalid_credit_card");
        }
    }

    private boolean checkCreditCard(String firstName, String lastName, String creditCardNumber, String expirationDate) {
        boolean isValid = false;
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = dataSource.getConnection();
            String query = "SELECT id FROM creditcards WHERE id = ? AND expiration = ? AND firstName = ? AND lastName = ?";
            stmt = conn.prepareStatement(query);
            stmt.setString(1, creditCardNumber);
            stmt.setString(2, expirationDate);
            stmt.setString(3, firstName);
            stmt.setString(4, lastName);
            rs = stmt.executeQuery();

            if (rs.next()) {
                isValid = true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            try {
                if (rs != null) {
                    rs.close();
                }
                if (stmt != null) {
                    stmt.close();
                }
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        return isValid;
    }

    // Method to record the transaction in the "sales" table
    // Method to record the transaction in the "sales" table
    private void recordSale(HttpSession session) {
        String firstName = (String) session.getAttribute("firstName");
        String lastName = (String) session.getAttribute("lastName");
        String creditCardNumber = (String) session.getAttribute("creditCardNumber");
        String expirationDate = (String) session.getAttribute("expirationDate");
        double totalPrice = (Double) session.getAttribute("totalPrice");

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = dataSource.getConnection();

            // Query to retrieve customer ID based on credit card details
            String customerIdQuery = "SELECT id FROM customers WHERE ccId = ? AND firstName = ? AND lastName = ?";
            stmt = conn.prepareStatement(customerIdQuery);
            stmt.setString(1, creditCardNumber);
            stmt.setString(2, firstName);
            stmt.setString(3, lastName);
            rs = stmt.executeQuery();

            // If customer ID is found, insert the sale record
            if (rs.next()) {
                int customerId = rs.getInt("id");

                // Insert sale record into the sales table
                String query = "INSERT INTO sales (customerId, movieId, saleDate) VALUES (?, ?, ?)";
                stmt = conn.prepareStatement(query);
                stmt.setInt(1, customerId);
                stmt.setString(2, "12345"); // Assuming a static movie ID for simplicity

                // Get the current date
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                Date date = new Date();
                stmt.setString(3, sdf.format(date));

                // Execute the insertion query
                stmt.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            try {
                if (rs != null) {
                    rs.close();
                }
                if (stmt != null) {
                    stmt.close();
                }
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

}
