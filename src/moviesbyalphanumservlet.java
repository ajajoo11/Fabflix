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
        int pageSize = 10; // Default page size
        int pageNumber = 1; // Default page number

        String pageSizeParam = request.getParameter("pageSize");
        String pageNumberParam = request.getParameter("pageNumber");

        // Check if pageSize parameter is provided and parse it
        if (pageSizeParam != null) {
            pageSize = Integer.parseInt(pageSizeParam);
        }

        // Check if pageNumber parameter is provided and parse it
        if (pageNumberParam != null) {
            pageNumber = Integer.parseInt(pageNumberParam);
        }

        if (character == null || character.isEmpty() || character.length() > 1) {
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("errorMessage", "Invalid character");
            out.write(jsonObject.toString());
            response.setStatus(400); // Bad Request
            return;
        }

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = buildPreparedStatement(conn, character, sortOption, pageSize, pageNumber);
             ResultSet rs = pstmt.executeQuery()) {

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

    private PreparedStatement buildPreparedStatement(Connection conn, String character, String sortOption, int pageSize, int pageNumber) throws Exception {
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

        // Calculate offset based on page size and page number
        int offset = (pageNumber - 1) * pageSize;

        // Modify the query to include pagination
        query += " LIMIT ? OFFSET ?";

        PreparedStatement pstmt = conn.prepareStatement(query);
        if (!character.equals("*")) {
            pstmt.setString(1, character);
            pstmt.setInt(2, pageSize);
            pstmt.setInt(3, offset);
        } else {
            pstmt.setInt(1, pageSize);
            pstmt.setInt(2, offset);
        }

        return pstmt;
    }
}
