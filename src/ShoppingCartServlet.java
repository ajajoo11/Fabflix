import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.math.BigDecimal;


import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

@WebServlet(name = "ShoppingCartServlet", urlPatterns = "/cart")
public class ShoppingCartServlet extends HttpServlet {
    private DataSource dataSource;

    @Override
    public void init() {
        try {
            dataSource = (DataSource) new InitialContext().lookup("java:comp/env/jdbc/moviedb");
            System.out.println("DataSource initialized successfully");
        } catch (NamingException e) {
            e.printStackTrace();
        }
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        HttpSession session = request.getSession();
        Map<String, Integer> cart = (Map<String, Integer>) session.getAttribute("cart");

        // Create a JSON array to hold cart items
        JsonArray cartArray = new JsonArray();

        // Iterate over each item in the cart
        for (Map.Entry<String, Integer> entry : cart.entrySet()) {
            String title = entry.getKey();
            Integer quantity = entry.getValue();

            // Retrieve the price of the movie from the database
            BigDecimal price = getPriceFromDatabase(title);

            // Create a JSON object for each cart item
            JsonObject item = new JsonObject();
            item.addProperty("title", title);
            item.addProperty("quantity", quantity);
            item.addProperty("price", price); // Add the price to the JSON object

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

    // Method to retrieve price from the database based on movie title
    private BigDecimal getPriceFromDatabase(String title) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement("SELECT price FROM movies WHERE title = ?")) {
            pstmt.setString(1, title);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getBigDecimal("price");
                } else {
                    // Handle case where no price is found for the given title
                    return BigDecimal.ZERO; // Default price or you can return null if appropriate
                }
            }
        } catch (SQLException e) {
            e.printStackTrace(); // Log the exception or handle it appropriately
            return null; // Return null or handle the exception accordingly
        }
    }



    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        HttpSession session = request.getSession();
        Map<String, Integer> cart = (Map<String, Integer>) session.getAttribute("cart");
        if (cart == null) {
            cart = new HashMap<>();
        }

        String action = request.getParameter("action");
        if ("add".equals(action)) {
            // Add item to the cart
            String movieTitle = request.getParameter("title");
            int quantity = Integer.parseInt(request.getParameter("quantity"));
            cart.put(movieTitle, quantity);
        } else if ("remove".equals(action)) {
            // Remove item from the cart
            String movieTitle = request.getParameter("title");
            cart.remove(movieTitle);
        }

        session.setAttribute("cart", cart);

        response.sendRedirect(request.getContextPath() + "/cart");
    }
}
