import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import javax.sql.DataSource;

@WebServlet(name = "ShoppingCartServlet", urlPatterns = "/cart")
public class ShoppingCartServlet extends HttpServlet {

    private DataSource dataSource;

    @Override
    public void init() throws ServletException {
        super.init();
        try {
            Context initContext = new InitialContext();
            Context envContext = (Context) initContext.lookup("java:/comp/env");
            dataSource = (DataSource) envContext.lookup("jdbc/moviedb"); // Replace with your DataSource name
        } catch (NamingException e) {
            e.printStackTrace();
            throw new ServletException("Error initializing DataSource", e);
        }
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("text/html");
        PrintWriter out = response.getWriter();

        // Retrieve the user's shopping cart from the session
        Map<String, Integer> cart = (Map<String, Integer>) request.getSession().getAttribute("cart");

        // Check if the cart is empty
        if (cart == null || cart.isEmpty()) {
            out.println("<h1>Your shopping cart is empty</h1>");
            return;
        }

        // Display the contents of the shopping cart
        out.println("<h1>Shopping Cart</h1>");
        out.println("<table border=\"1\">");
        out.println("<tr><th>Movie Title</th><th>Quantity</th><th>Price</th></tr>");

        // Retrieve movie details (such as title and price) based on movie IDs or titles
        // stored in the cart
        Map<String, Double> movieDetails = getMovieDetails(cart);

        // Iterate over each item in the cart and display its details
        for (Map.Entry<String, Integer> entry : cart.entrySet()) {
            String movieTitle = entry.getKey();
            int quantity = entry.getValue();
            double price = movieDetails.getOrDefault(movieTitle, 0.0);

            out.println("<tr>");
            out.println("<td>" + movieTitle + "</td>");
            out.println("<td>" + quantity + "</td>");
            out.println("<td>$" + price + "</td>");
            out.println("</tr>");
        }

        out.println("</table>");

        // Calculate and display the total price
        double totalPrice = calculateTotalPrice(cart, movieDetails);
        out.println("<h2>Total Price: $" + totalPrice + "</h2>");

        out.close();
    }

    private Map<String, Double> getMovieDetails(Map<String, Integer> cart) {
        Map<String, Double> movieDetails = new HashMap<>();
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            conn = dataSource.getConnection();

            // Prepare SQL query to fetch movie prices based on movie titles
            StringBuilder sql = new StringBuilder();
            sql.append("SELECT title, price FROM movies WHERE title IN (");
            for (String movieTitle : cart.keySet()) {
                sql.append("?,");
            }
            sql.deleteCharAt(sql.length() - 1); // Remove the last comma
            sql.append(")");

            pstmt = conn.prepareStatement(sql.toString());

            // Set movie titles as parameters for the prepared statement
            int parameterIndex = 1;
            for (String movieTitle : cart.keySet()) {
                pstmt.setString(parameterIndex++, movieTitle);
            }

            // Execute the query
            rs = pstmt.executeQuery();

            // Populate movie details map with retrieved prices
            while (rs.next()) {
                String title = rs.getString("title");
                double price = rs.getDouble("price");
                movieDetails.put(title, price);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            try {
                if (rs != null) {
                    rs.close();
                }
                if (pstmt != null) {
                    pstmt.close();
                }
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        return movieDetails;
    }

    private double calculateTotalPrice(Map<String, Integer> cart, Map<String, Double> movieDetails) {
        double totalPrice = 0.0;
        for (Map.Entry<String, Integer> entry : cart.entrySet()) {
            String movieTitle = entry.getKey();
            int quantity = entry.getValue();
            double price = movieDetails.getOrDefault(movieTitle, 0.0);
            totalPrice += price * quantity;
        }
        return totalPrice;
    }
}
