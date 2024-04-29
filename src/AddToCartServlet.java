import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import java.util.HashMap;

@WebServlet(name = "AddToCartServlet", urlPatterns = "/addToCart")
public class AddToCartServlet extends HttpServlet {
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        // Extract movie details from request parameters
        String movieId = request.getParameter("movieId");
        String movieTitle = request.getParameter("movieTitle");
        System.out.println(
                "Received request to add movie to cart. Movie ID: " + movieId + ", Movie Title: " + movieTitle); // Debug
                                                                                                                 // statement

        // Default quantity is 1 if not provided in the request
        int quantity = 1;
        String quantityParam = request.getParameter("quantity");
        if (quantityParam != null && !quantityParam.isEmpty()) {
            quantity = Integer.parseInt(quantityParam);
        }
        System.out.println("Quantity: " + quantity); // Debug statement

        // Retrieve the user's shopping cart from the session or create a new cart if it
        // doesn't exist
        Map<String, Integer> cart = (Map<String, Integer>) request.getSession().getAttribute("cart");
        if (cart == null) {
            cart = new HashMap<>();
            request.getSession().setAttribute("cart", cart);
        }
        if (cart.containsKey(movieTitle)) {
            // If the movie already exists, update the quantity
            int existingQuantity = cart.get(movieTitle);
            cart.put(movieTitle, existingQuantity + quantity);
        } else {
            // If the movie doesn't exist, add it to the cart
            cart.put(movieTitle, quantity);
        }
        System.out.println("Movie added to cart. Cart contents: " + cart); // Debug statement

        // Send success response
        response.setContentType("application/json");
        response.getWriter().write("{\"success\": true}");
    }

}
