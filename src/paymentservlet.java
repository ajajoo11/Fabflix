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
import java.lang.Iterable;
import java.util.Enumeration;

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

            // Record the transaction in the "sales" table and get the generated sale ID
            int saleId = recordSale(session);

            // Set the sale ID as a session attribute
            session.setAttribute("saleId", saleId);

            String priceUrl = "paymentconfirmation.html?totalprice=" + totalPrice + "&saleid=" + saleId;
            response.sendRedirect(priceUrl);
        } else {
            String errorUrl = "payment.html?error=invalid_credit_card&totalprice=" + totalPrice;
            response.sendRedirect(errorUrl);
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

    private int recordSale(HttpSession session) {

        // Print session attributes
        System.out.println("Session Attributes:");
        Enumeration<String> attributeNames = session.getAttributeNames();
        while (attributeNames.hasMoreElements()) {
            String attributeName = attributeNames.nextElement();
            Object attributeValue = session.getAttribute(attributeName);
            System.out.println(attributeName + ": " + attributeValue);
        }

        int saleId = -1;
        String firstName = (String) session.getAttribute("firstName");
        String lastName = (String) session.getAttribute("lastName");
        String creditCardNumber = (String) session.getAttribute("creditCardNumber");
        String expirationDate = (String) session.getAttribute("expirationDate");
        double totalPrice = (Double) session.getAttribute("totalPrice");
        String customeremail = (String) session.getAttribute("email");
        System.out.println(customeremail);

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = dataSource.getConnection();
            String customerIdQuery = "SELECT id FROM customers WHERE email=?";
            stmt = conn.prepareStatement(customerIdQuery);
            stmt.setString(1, customeremail);
            System.out.println("Executing SQL 1query: " + stmt);
            rs = stmt.executeQuery();
            if (rs.next()) {
                int customerId = rs.getInt("id");
                System.out.println("printing this id here" + customerId);

                String query = "INSERT INTO sales (customerId, movieId, saleDate) VALUES (?, ?, ?)";
                System.out.println("Executing SQL query3: " + query);
                stmt = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
                stmt.setInt(1, customerId);
                stmt.setString(2, "tt0304600");

                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                Date date = new Date();
                stmt.setString(3, sdf.format(date));
                int affectedRows = stmt.executeUpdate();

                if (affectedRows > 0) {
                    rs = stmt.getGeneratedKeys();
                    System.out.println("It is here saleid");
                    if (rs.next()) {
                        saleId = rs.getInt(1);
                        System.out.println("It is here saleid:" + saleId);
                    }
                }
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

        return saleId;
    }

}
