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

        try (Connection conn = dataSource.getConnection()) {
            StringBuilder queryBuilder = new StringBuilder();
            queryBuilder.append("SELECT m.*, r.rating FROM movies m LEFT JOIN ratings r ON m.id = r.movieId WHERE ");

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
                queryBuilder.append("m.star LIKE ? AND ");
            }

            queryBuilder.append("1=1"); // Add a dummy condition to ensure the query is valid
            queryBuilder.append(" ORDER BY r.rating DESC"); // Optionally, you can order the results by rating

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