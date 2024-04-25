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
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.ArrayList;

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
                    "GROUP_CONCAT(DISTINCT CONCAT(s.id, ':', s.name) ORDER BY s.name SEPARATOR ',') AS stars, " +
                    "r.rating, " +
                    "GROUP_CONCAT(DISTINCT CONCAT(g.id, ':', g.name) ORDER BY g.name SEPARATOR ',') AS genres " +
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
                float rating = rs.getFloat("rating");

                // Fetching stars
                String starsString = rs.getString("stars");
                List<JsonObject> starsList = new ArrayList<>();
                if (starsString != null) {
                    String[] stars = starsString.split(",");
                    for (String star : stars) {
                        String[] starInfo = star.split(":");
                        JsonObject starObject = new JsonObject();
                        starObject.addProperty("id", starInfo[0]);
                        starObject.addProperty("name", starInfo[1]);
                        starsList.add(starObject);
                    }
                }
                // Sorting stars by the number of movies they've played and then alphabetically
                Collections.sort(starsList, new Comparator<JsonObject>() {
                    @Override
                    public int compare(JsonObject star1, JsonObject star2) {
                        int movieCountComparison = Integer.compare(getStarMovieCount(star2.get("id").getAsString()),
                                getStarMovieCount(star1.get("id").getAsString()));
                        if (movieCountComparison != 0) {
                            return movieCountComparison;
                        }
                        // If movie counts are equal, compare alphabetically
                        return star1.get("name").getAsString().compareTo(star2.get("name").getAsString());
                    }
                });

                JsonArray starsArray = new JsonArray();
                for (JsonObject star : starsList) {
                    starsArray.add(star);
                }

                // Fetching genres
                String genresString = rs.getString("genres");
                JsonArray genresArray = new JsonArray();
                if (genresString != null) {
                    String[] genres = genresString.split(",");
                    for (String genre : genres) {
                        String[] genreInfo = genre.split(":");
                        JsonObject genreObject = new JsonObject();
                        genreObject.addProperty("id", genreInfo[0]);
                        genreObject.addProperty("name", genreInfo[1]);
                        genresArray.add(genreObject);
                    }
                }

                movieInfo.addProperty("title", title);
                movieInfo.addProperty("year", year);
                movieInfo.addProperty("director", director);
                movieInfo.add("genres", genresArray);
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

    private int getStarMovieCount(String starId) {
        try (Connection conn = dataSource.getConnection()) {
            String query = "SELECT COUNT(*) AS movieCount FROM stars_in_movies WHERE starId = ?";
            PreparedStatement statement = conn.prepareStatement(query);
            statement.setString(1, starId);
            ResultSet rs = statement.executeQuery();
            if (rs.next()) {
                return rs.getInt("movieCount");
            }
            return 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return 0;
        }
    }

}
