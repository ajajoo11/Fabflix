import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

@WebServlet(name = "ShoppingCartServlet", urlPatterns = "/cart")
public class ShoppingCartServlet extends HttpServlet {

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

        // Retrieve movie details (such as title and price) based on movie IDs or titles stored in the cart
        // Replace this with logic to fetch movie details from the database
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

    // Method to retrieve movie details (such as title and price) based on movie IDs or titles stored in the cart
    // Replace this with logic to fetch movie details from the database
    private Map<String, Double> getMovieDetails(Map<String, Integer> cart) {
        // Dummy implementation - replace with actual database query
        Map<String, Double> movieDetails = new HashMap<>();
        movieDetails.put("Movie Title 1", 10.0); // Example: Movie Title 1 with price $10
        movieDetails.put("Movie Title 2", 15.0); // Example: Movie Title 2 with price $15
        // Add more movie details as needed
        return movieDetails;
    }

    // Method to calculate the total price of items in the cart
    private double calculateTotalPrice(Map<String, Integer> cart, Map<String, Double> movieDetails) {
        double totalPrice = 0.0;
        // Iterate over each item in the cart and calculate its total price
        for (Map.Entry<String, Integer> entry : cart.entrySet()) {
            String movieTitle = entry.getKey();
            int quantity = entry.getValue();
            double price = movieDetails.getOrDefault(movieTitle, 0.0);
            totalPrice += price * quantity;
        }
        return totalPrice;
    }
}
