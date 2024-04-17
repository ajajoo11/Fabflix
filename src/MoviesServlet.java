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
import java.sql.ResultSet;
import java.sql.Statement;

@WebServlet(name = "MoviesServlet", urlPatterns = "/home")
public class MoviesServlet extends HttpServlet {
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

        try (Connection conn = dataSource.getConnection()) {
            Statement statement = conn.createStatement();
            String query = "SELECT m.id, m.title, m.year, m.director, " +
                    "GROUP_CONCAT(DISTINCT g.name ORDER BY g.name SEPARATOR ',') AS genres, " +
                    "GROUP_CONCAT(DISTINCT s.name ORDER BY s.name SEPARATOR ',') AS stars, " +
                    "r.rating " +
                    "FROM movies m " +
                    "JOIN ratings r ON m.id = r.movieId " +
                    "JOIN genres_in_movies gim ON m.id = gim.movieId " +
                    "JOIN genres g ON gim.genreId = g.id " +
                    "JOIN stars_in_movies sim ON m.id = sim.movieId " +
                    "JOIN stars s ON sim.starId = s.id " +
                    "GROUP BY m.id " +
                    "ORDER BY r.rating DESC " +
                    "LIMIT 20";

            ResultSet rs = statement.executeQuery(query);

            JsonArray jsonArray = new JsonArray();
            while (rs.next()) {
                // Creating JsonObject for each movie...
                JsonObject movieObject = new JsonObject();

                // Adding existing movie details
                movieObject.addProperty("title", rs.getString("title"));
                movieObject.addProperty("id", rs.getString("id"));
                movieObject.addProperty("year", rs.getInt("year"));
                movieObject.addProperty("director", rs.getString("director"));
                movieObject.addProperty("genres", rs.getString("genres"));
                String starsString = rs.getString("stars");
                // movieObject.addProperty("stars", rs.getString("stars")); // Original stars
                // property
                movieObject.addProperty("rating", rs.getDouble("rating"));

                // Parsing stars string and creating JsonArray for parsed stars
                JsonArray starsArray = new JsonArray();
                if (starsString != null) {
                    String[] stars = starsString.split(",");
                    for (String star : stars) {
                        String[] starInfo = star.split(":");
                        JsonObject starObject = new JsonObject();
                        starObject.addProperty("id", starInfo[0]);
                        starObject.addProperty("name", starInfo[1]);
                        starsArray.add(starObject);
                    }
                }
                // Adding parsed stars as a separate property
                movieObject.add("stars", starsArray);

                // Adding movie JsonObject to the jsonArray
                jsonArray.add(movieObject);
            }
            rs.close();
            statement.close();

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