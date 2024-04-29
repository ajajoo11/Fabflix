import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;

import com.google.gson.JsonObject;

import java.util.HashMap;

@WebServlet(name = "AddToCartServlet", urlPatterns = "/addToCart")
public class AddToCartServlet extends HttpServlet {
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String movieId = request.getParameter("movieId");
        String movieTitle = request.getParameter("movieTitle");
        System.out.println(
                "Received request to add movie to cart. Movie ID: " + movieId + ", Movie Title: " + movieTitle); // Debug
                                                                                                                 // statement

        int quantity = 1;
        String quantityParam = request.getParameter("quantity");
        if (quantityParam != null && !quantityParam.isEmpty()) {
            quantity = Integer.parseInt(quantityParam);
        }
        System.out.println("Quantity: " + quantity);

        Map<String, Integer> cart = (Map<String, Integer>) request.getSession().getAttribute("cart");
        if (cart == null) {
            cart = new HashMap<>();
            request.getSession().setAttribute("cart", cart);
        }
        if (cart.containsKey(movieTitle)) {

            int existingQuantity = cart.get(movieTitle);
            cart.put(movieTitle, existingQuantity + quantity);

        } else {

            cart.put(movieTitle, quantity);
        }
        System.out.println("Movie added to cart. Cart contents: " + cart);

        response.setContentType("application/json");
        response.getWriter().write("{\"success\": true}");
    }

}
