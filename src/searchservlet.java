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

        String searchTerm = request.getParameter("query");
        String[] searchCriteria = request.getParameterValues("criteria[]");

        try (Connection conn = dataSource.getConnection()) {
            // Construct SQL query based on search criteria
            StringBuilder queryBuilder = new StringBuilder();
            queryBuilder.append("SELECT m.*, r.rating FROM movies m LEFT JOIN ratings r ON m.id = r.movieId WHERE ");
            for (int i = 0; i < searchCriteria.length; i++) {
                queryBuilder.append(searchCriteria[i]).append(" LIKE ? ");
                if (i < searchCriteria.length - 1) {
                    queryBuilder.append("AND ");
                }
            }
            queryBuilder.append("ORDER BY r.rating DESC");

            // Prepare statement
            PreparedStatement statement = conn.prepareStatement(queryBuilder.toString());
            System.out.println("SQL Query: " + queryBuilder.toString());

            // Set parameters
            for (int i = 1; i <= searchCriteria.length; i++) {
                statement.setString(i, "%" + searchTerm + "%");
            }

            // Execute query
            ResultSet rs = statement.executeQuery();

            // Prepare JSON object to store search results
            JsonArray searchResultsArray = new JsonArray();

            // Process results
            while (rs.next()) {
                JsonObject resultObject = new JsonObject();
                resultObject.addProperty("id", rs.getString("id"));
                resultObject.addProperty("title", rs.getString("title"));
                resultObject.addProperty("year", rs.getInt("year"));
                resultObject.addProperty("director", rs.getString("director"));
                resultObject.addProperty("rating", rs.getFloat("rating")); // Add rating to the JSON object
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
