import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import jakarta.servlet.http.HttpSession;

import com.google.gson.JsonObject;

import java.util.HashMap;
import java.util.AbstractMap;

@WebServlet(name = "AddToCartServlet", urlPatterns = "/addToCart")
public class AddToCartServlet extends HttpServlet {
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String movieId = request.getParameter("movieId"); // New
        String movieTitle = request.getParameter("movieTitle");
        System.out.println(
                "Received request to add movie to cart. Movie ID: " + movieId + ", Movie Title: " + movieTitle);

        int quantity = 1;
        String quantityParam = request.getParameter("quantity");
        if (quantityParam != null && !quantityParam.isEmpty()) {
            quantity = Integer.parseInt(quantityParam);
        }

        HttpSession session = request.getSession();
        Map<String, Map.Entry<String, Integer>> cart = (Map<String, Map.Entry<String, Integer>>) session.getAttribute("cart");
        if (cart == null) {
            cart = new HashMap<>();
            session.setAttribute("cart", cart);
        }

        if (cart.containsKey(movieId)) {
            Map.Entry<String, Integer> existingItem = cart.get(movieId);
            int existingQuantity = existingItem.getValue();
            cart.put(movieId, new AbstractMap.SimpleEntry<>(existingItem.getKey(), existingQuantity + quantity));
        } else {
            cart.put(movieId, new AbstractMap.SimpleEntry<>(movieTitle, quantity));
        }

        System.out.println("Movie added to cart. Cart contents: " + cart);

        response.setContentType("application/json");
        response.getWriter().write("{\"success\": true}");
    }
}
