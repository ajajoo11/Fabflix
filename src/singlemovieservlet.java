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

@WebServlet(name = "singlemovieservlet", urlPatterns = "/singlemoviepage")
public class singlemovieservlet extends HttpServlet {
    private static final long serialVersionUID = 3L;

    private DataSource dataSource;

    public void init(ServletConfig config) {
        try {
            dataSource = (DataSource) new InitialContext().lookup("java:comp/env/jdbc/moviedb");
        } catch (NamingException e) {
            e.printStackTrace();
        }
    }

    /**
     * @see HttpServletdoGet(HttpServletRequest request, HttpServletResponse
     *      response)
     */
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {

        response.setContentType("application/json");

        String id = request.getParameter("id");

        request.getServletContext().log("getting id: " + id);

        PrintWriter out = response.getWriter();

        try (Connection conn = dataSource.getConnection()) {

            String query = "SELECT m.title, m.year, m.director, " +
                    "GROUP_CONCAT(DISTINCT g.name ORDER BY g.name SEPARATOR ',') AS genres, " +
                    "GROUP_CONCAT(DISTINCT CONCAT(s.id, ':', s.name) ORDER BY s.name SEPARATOR ',') AS stars, " +
                    "r.rating " +
                    "FROM movies m " +
                    "LEFT JOIN ratings r ON m.id = r.movieId " +
                    "LEFT JOIN genres_in_movies gim ON m.id = gim.movieId " +
                    "LEFT JOIN genres g ON gim.genreId = g.id " +
                    "LEFT JOIN stars_in_movies sim ON m.id = sim.movieId " +
                    "LEFT JOIN stars s ON sim.starId = s.id " +
                    "WHERE m.id = ? " +
                    "GROUP BY m.id";

            PreparedStatement statement = conn.prepareStatement(query);

            statement.setString(1, id);

            ResultSet rs = statement.executeQuery();

            JsonObject movieInfo = new JsonObject();

            if (rs.next()) {

                String title = rs.getString("title");
                int year = rs.getInt("year");
                String director = rs.getString("director");
                String genres = rs.getString("genres");
                String starsString = rs.getString("stars");
                float rating = rs.getFloat("rating");

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

                movieInfo.addProperty("title", title);
                movieInfo.addProperty("year", year);
                movieInfo.addProperty("director", director);
                movieInfo.addProperty("genres", genres);
                movieInfo.add("stars", starsArray);
                movieInfo.addProperty("rating", rating);
            }
            rs.close();
            statement.close();

            out.write(movieInfo.toString());

            response.setStatus(200);

        } catch (Exception e) {

            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("errorMessage", e.getMessage());
            out.write(jsonObject.toString());

            request.getServletContext().log("Error:", e);

            response.setStatus(500);
        } finally {
            out.close();
        }

    }

}