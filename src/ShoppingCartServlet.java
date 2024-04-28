import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

@WebServlet(name = "ShoppingCartServlet", urlPatterns = "/cart")
public class ShoppingCartServlet extends HttpServlet {

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        HttpSession session = request.getSession();
        Map<String, Integer> cart = (Map<String, Integer>) session.getAttribute("cart");

        // Create a JSON array to hold cart items
        JsonArray cartArray = new JsonArray();

        // Iterate over each item in the cart
        for (Map.Entry<String, Integer> entry : cart.entrySet()) {
            String title = entry.getKey();
            Integer quantity = entry.getValue();

            // Create a JSON object for each cart item
            JsonObject item = new JsonObject();
            item.addProperty("title", title);
            item.addProperty("quantity", quantity);

            // Add the item to the cart array
            cartArray.add(item);
        }

        // Set response content type
        response.setContentType("application/json");

        // Get the response writer
        PrintWriter out = response.getWriter();

        // Write the JSON array to the response
        out.write(new Gson().toJson(cartArray));

        // Set response status
        response.setStatus(HttpServletResponse.SC_OK);
    }


    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        HttpSession session = request.getSession();
        Map<String, Integer> cart = (Map<String, Integer>) session.getAttribute("cart");
        if (cart == null) {
            cart = new HashMap<>();
        }

        String movieTitle = request.getParameter("title");
        int quantity = Integer.parseInt(request.getParameter("quantity"));

        cart.put(movieTitle, quantity);
        session.setAttribute("cart", cart);

        response.sendRedirect(request.getContextPath() + "/cart");
    }
}
