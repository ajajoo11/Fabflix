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
            StringBuilder queryBuilder = new StringBuilder();
            queryBuilder.append("SELECT DISTINCT m.*, r.rating FROM movies m ");
            queryBuilder.append("LEFT JOIN ratings r ON m.id = r.movieId ");
            queryBuilder.append("LEFT JOIN stars_in_movies sm ON m.id = sm.movieId ");
            queryBuilder.append("LEFT JOIN stars s ON sm.starId = s.id ");
            queryBuilder.append("WHERE ");

            if (title != null && !title.isEmpty()) {
                queryBuilder.append("m.title LIKE ? AND ");
            }
            if (director != null && !director.isEmpty()) {
                queryBuilder.append("m.director LIKE ? AND ");
            }
            if (year != null && !year.isEmpty()) {
                queryBuilder.append("m.year = ? AND ");
            }
            if (star != null && !star.isEmpty()) {
                queryBuilder.append("s.name LIKE ? AND ");
            }

            queryBuilder.append("1=1"); // Add a dummy condition to ensure the query is valid

            // Sort options
            switch (sortOption) {
                case "title_asc_rating_desc":
                    queryBuilder.append(" ORDER BY m.title ASC, r.rating DESC");
                    break;
                case "title_asc_rating_asc":
                    queryBuilder.append(" ORDER BY m.title ASC, r.rating ASC");
                    break;
                case "title_desc_rating_desc":
                    queryBuilder.append(" ORDER BY m.title DESC, r.rating DESC");
                    break;
                case "title_desc_rating_asc":
                    queryBuilder.append(" ORDER BY m.title DESC, r.rating ASC");
                    break;
                case "rating_asc_title_desc":
                    queryBuilder.append(" ORDER BY r.rating ASC, m.title DESC");
                    break;
                case "rating_asc_title_asc":
                    queryBuilder.append(" ORDER BY r.rating ASC, m.title ASC");
                    break;
                case "rating_desc_title_desc":
                    queryBuilder.append(" ORDER BY r.rating DESC, m.title DESC");
                    break;
                case "rating_desc_title_asc":
                    queryBuilder.append(" ORDER BY r.rating DESC, m.title ASC");
                    break;
                default:
                    queryBuilder.append(" ORDER BY r.rating DESC"); // Default sorting
                    break;
            }

            queryBuilder.append(" LIMIT ? OFFSET ?");

            PreparedStatement statement = conn.prepareStatement(queryBuilder.toString());
            int parameterIndex = 1;

            if (title != null && !title.isEmpty()) {
                statement.setString(parameterIndex++, "%" + title + "%");
            }
            if (director != null && !director.isEmpty()) {
                statement.setString(parameterIndex++, "%" + director + "%");
            }
            if (year != null && !year.isEmpty()) {
                statement.setInt(parameterIndex++, Integer.parseInt(year));
            }
            if (star != null && !star.isEmpty()) {
                statement.setString(parameterIndex++, "%" + star + "%");
            }
            statement.setInt(parameterIndex++, pageSize);
            statement.setInt(parameterIndex++, (pageNumber - 1) * pageSize);

            ResultSet rs = statement.executeQuery();
            JsonArray searchResultsArray = new JsonArray();

            while (rs.next()) {
                JsonObject resultObject = new JsonObject();
                resultObject.addProperty("id", rs.getString("id"));
                resultObject.addProperty("title", rs.getString("title"));
                resultObject.addProperty("year", rs.getInt("year"));
                resultObject.addProperty("director", rs.getString("director"));
                resultObject.addProperty("rating", rs.getFloat("rating"));
                searchResultsArray.add(resultObject);
            }
            rs.close();
            statement.close();

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