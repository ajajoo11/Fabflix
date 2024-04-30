import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
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
import java.util.AbstractMap;


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
        Map<String, Map.Entry<String, Integer>> cart = (Map<String, Map.Entry<String, Integer>>) session.getAttribute("cart");
        JsonArray cartArray = new JsonArray();

        for (Map.Entry<String, Map.Entry<String, Integer>> entry : cart.entrySet()) {
            String id = entry.getKey();
            String title = entry.getValue().getKey();
            Integer quantity = entry.getValue().getValue();

            BigDecimal price = getPriceFromDatabase(title);

            JsonObject item = new JsonObject();
            item.addProperty("id", id);
            item.addProperty("title", title);
            item.addProperty("quantity", quantity);
            item.addProperty("price", price); // Add the price to the JSON object

            cartArray.add(item);
        }

        response.setContentType("application/json");
        PrintWriter out = response.getWriter();
        out.write(new Gson().toJson(cartArray));
        response.setStatus(HttpServletResponse.SC_OK);
    }

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
        Map<String, Map.Entry<String, Integer>> cart = (Map<String, Map.Entry<String, Integer>>) session.getAttribute("cart");
        if (cart == null) {
            cart = new HashMap<>();
            session.setAttribute("cart", cart);
        }

        String action = request.getParameter("action");
        if ("add".equals(action)) {
            String movieId = request.getParameter("id");
            String movieTitle = request.getParameter("title");
            int quantity = Integer.parseInt(request.getParameter("quantity"));
            cart.put(movieId, new AbstractMap.SimpleEntry<>(movieTitle, quantity));
        } else if ("remove".equals(action)) {
            String movieId = request.getParameter("id");
            cart.remove(movieId);
        }

        session.setAttribute("cart", cart);
        response.sendRedirect(request.getContextPath() + "/cart");
    }
}
