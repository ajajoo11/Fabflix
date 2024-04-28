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
import java.sql.SQLException;

@WebServlet(name = "searchservlet", urlPatterns = "/searchresults")
public class searchservlet extends HttpServlet {
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

        String title = request.getParameter("title");
        String director = request.getParameter("director");
        String year = request.getParameter("year");
        String star = request.getParameter("star");
        String sortOption = request.getParameter("sort_option");
        int pageSize = Integer.parseInt(request.getParameter("pageSize"));
        int pageNumber = Integer.parseInt(request.getParameter("pageNumber"));

        try (Connection conn = dataSource.getConnection()) {
            String query = "SELECT m.id, m.title, m.year, m.director, r.rating " +
                    "FROM movies m " +
                    "LEFT JOIN ratings r ON m.id = r.movieId " +
                    "WHERE m.title LIKE ? " +
                    "AND m.director LIKE ? " +
                    "AND m.year LIKE ? ";
//                    "AND m.star LIKE ? ";

            switch (sortOption) {
                case "title_asc_rating_desc":
                    query += "ORDER BY m.title ASC, r.rating DESC";
                    break;
                case "title_asc_rating_asc":
                    query += "ORDER BY m.title ASC, r.rating ASC";
                    break;
                case "title_desc_rating_desc":
                    query += "ORDER BY m.title DESC, r.rating DESC";
                    break;
                case "title_desc_rating_asc":
                    query += "ORDER BY m.title DESC, r.rating ASC";
                    break;
                case "rating_asc_title_desc":
                    query += "ORDER BY r.rating ASC, m.title DESC";
                    break;
                case "rating_asc_title_asc":
                    query += "ORDER BY r.rating ASC, m.title ASC";
                    break;
                case "rating_desc_title_desc":
                    query += "ORDER BY r.rating DESC, m.title DESC";
                    break;
                case "rating_desc_title_asc":
                    query += "ORDER BY r.rating DESC, m.title ASC";
                    break;
                default:
                    query += "ORDER BY m.title ASC, r.rating DESC"; // Default sorting
                    break;
            }

            query += " LIMIT ? OFFSET ?";

            PreparedStatement pstmt = conn.prepareStatement(query);
            pstmt.setString(1, "%" + title + "%");
            pstmt.setString(2, "%" + director + "%");
            pstmt.setString(3, "%" + year + "%");
//            pstmt.setString(4, "%" + star + "%");
            pstmt.setInt(4, pageSize);
            pstmt.setInt(5, (pageNumber - 1) * pageSize);

            ResultSet rs = pstmt.executeQuery();
            JsonArray searchResultsArray = new JsonArray();

            while (rs.next()) {
                JsonObject resultObject = new JsonObject();
                resultObject.addProperty("id", rs.getString("id"));
                resultObject.addProperty("title", rs.getString("title"));
                resultObject.addProperty("year", rs.getInt("year"));
                resultObject.addProperty("director", rs.getString("director"));
                resultObject.addProperty("rating", rs.getDouble("rating"));
                searchResultsArray.add(resultObject);
            }
            rs.close();
            pstmt.close();

            out.write(searchResultsArray.toString());
            response.setStatus(200);

        } catch (SQLException e) {
            e.printStackTrace();
            JsonObject errorJson = new JsonObject();
            errorJson.addProperty("errorMessage", e.getMessage());
            out.write(errorJson.toString());
            response.setStatus(500);
        } finally {
            out.close();
        }
    }
}
