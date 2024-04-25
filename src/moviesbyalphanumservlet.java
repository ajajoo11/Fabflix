import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import javax.sql.DataSource;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

@WebServlet(name = "moviesbyalphanumservlet", urlPatterns = "/moviesbyalphanum")
public class moviesbyalphanumservlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private DataSource dataSource;

    public void init(ServletConfig config) {
        try {
            dataSource = (DataSource) new InitialContext().lookup("java:comp/env/jdbc/moviedb");
        } catch (NamingException e) {
            e.printStackTrace();
        }
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json");
        PrintWriter out = response.getWriter();

        String character = request.getParameter("charac");
        String sortOption = request.getParameter("sort_option");

        if (character == null || character.isEmpty() || character.length() > 1) {
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("errorMessage", "Invalid character");
            out.write(jsonObject.toString());
            response.setStatus(400); // Bad Request
            return;
        }

        try (Connection conn = dataSource.getConnection()) {
            String query;
            String orderBy;

            // Determine the sorting order based on the selected sort option
            if (sortOption != null) {
                switch (sortOption) {
                    case "title_asc_rating_desc":
                        orderBy = "ORDER BY m.title ASC, r.rating DESC";
                        break;
                    case "title_asc_rating_asc":
                        orderBy = "ORDER BY m.title ASC, r.rating ASC";
                        break;
                    case "title_desc_rating_desc":
                        orderBy = "ORDER BY m.title DESC, r.rating DESC";
                        break;
                    case "title_desc_rating_asc":
                        orderBy = "ORDER BY m.title DESC, r.rating ASC";
                        break;
                    case "rating_asc_title_desc":
                        orderBy = "ORDER BY r.rating ASC, m.title DESC";
                        break;
                    case "rating_asc_title_asc":
                        orderBy = "ORDER BY r.rating ASC, m.title ASC";
                        break;
                    case "rating_desc_title_desc":
                        orderBy = "ORDER BY r.rating DESC, m.title DESC";
                        break;
                    case "rating_desc_title_asc":
                        orderBy = "ORDER BY r.rating DESC, m.title ASC";
                        break;
                    default:
                        orderBy = "ORDER BY m.title ASC, r.rating DESC"; // Default sorting
                        break;
                }
            } else {
                // Default sorting
                orderBy = "ORDER BY m.title ASC, r.rating DESC";
            }

            if (character.equals("*")) {
                query = "SELECT m.id, m.title, m.year, m.director, r.rating " +
                        "FROM movies m " +
                        "LEFT JOIN ratings r ON m.id = r.movieId " +
                        "WHERE LOWER(m.title) REGEXP '^[^a-z0-9]' " +
                        orderBy;
            } else {
                character = character.toLowerCase() + "%";
                query = "SELECT m.id, m.title, m.year, m.director, r.rating " +
                        "FROM movies m " +
                        "LEFT JOIN ratings r ON m.id = r.movieId " +
                        "WHERE LOWER(m.title) LIKE ? " +
                        orderBy;
            }

            PreparedStatement pstmt = conn.prepareStatement(query);
            if (!character.equals("*")) {
                pstmt.setString(1, character);
            }

            ResultSet rs = pstmt.executeQuery();

            JsonArray jsonArray = new JsonArray();
            while (rs.next()) {
                JsonObject jsonObject = new JsonObject();
                jsonObject.addProperty("title", rs.getString("title"));
                jsonObject.addProperty("id", rs.getString("id"));
                jsonObject.addProperty("year", rs.getInt("year"));
                jsonObject.addProperty("director", rs.getString("director"));
                jsonObject.addProperty("rating", rs.getDouble("rating"));

                jsonArray.add(jsonObject);
            }
            rs.close();
            pstmt.close();

            System.out.println(jsonArray.toString());

            request.getServletContext().log("getting " + jsonArray.size() + " results");

            out.write(jsonArray.toString());
            response.setStatus(200);

        } catch (Exception e) {
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("errorMessage", e.getMessage());
            out.write(jsonObject.toString());
            response.setStatus(500);
        } finally {
            out.close();
        }
    }
}
