import com.google.gson.Gson;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

@WebServlet(name = "singlegenreservlet", urlPatterns = "/singlegenrepage")
public class singlegenreservlet extends HttpServlet {

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

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        int pageSize = 10; // Default page size
        int pageNumber = 1; // Default page number

        String genreId = request.getParameter("id");
        String sortOption = request.getParameter("sort_option");
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

        if (genreId == null || genreId.isEmpty() || sortOption == null || sortOption.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        try (Connection connection = dataSource.getConnection()) {
            String query = "SELECT m.id, m.title, m.year, m.director, r.rating, GROUP_CONCAT(DISTINCT CONCAT(g.id, ':', g.name) ORDER BY g.name SEPARATOR ',') AS genres, GROUP_CONCAT(DISTINCT CONCAT(s.id, ':', s.name) ORDER BY s.name SEPARATOR ',') AS stars "
                    +
                    "FROM movies m " +
                    "JOIN genres_in_movies gim ON m.id = gim.movieId " +
                    "LEFT JOIN ratings r ON m.id = r.movieId " +
                    "LEFT JOIN genres g ON gim.genreId = g.id " +
                    "LEFT JOIN stars_in_movies sim ON m.id = sim.movieId " +
                    "LEFT JOIN stars s ON sim.starId = s.id " +
                    "WHERE gim.genreId = ? " +
                    "GROUP BY m.id " +
                    "ORDER BY ";

            switch (sortOption) {
                case "title_asc_rating_desc":
                    query += "m.title ASC, r.rating DESC";
                    break;
                case "title_asc_rating_asc":
                    query += "m.title ASC, r.rating ASC";
                    break;
                case "title_desc_rating_desc":
                    query += "m.title DESC, r.rating DESC";
                    break;
                case "title_desc_rating_asc":
                    query += "m.title DESC, r.rating ASC";
                    break;
                case "rating_asc_title_desc":
                    query += "r.rating ASC, m.title DESC";
                    break;
                case "rating_asc_title_asc":
                    query += "r.rating ASC, m.title ASC";
                    break;
                case "rating_desc_title_desc":
                    query += "r.rating DESC, m.title DESC";
                    break;
                case "rating_desc_title_asc":
                    query += "r.rating DESC, m.title ASC";
                    break;
                default:
                    // Default sorting by title ascending, rating descending
                    query += "m.title ASC, r.rating DESC";
                    break;
            }
            // Calculate offset based on page number and page size
            int offset = (pageNumber - 1) * pageSize;
            // Append limit and offset to query
            query += " LIMIT ? OFFSET ?";
            System.out.println(query);
            try (PreparedStatement statement = connection.prepareStatement(query)) {
                statement.setString(1, genreId);
                statement.setInt(2, pageSize);
                statement.setInt(3, offset);
                System.out.println(query);
                try (ResultSet resultSet = statement.executeQuery()) {
                    JsonArray moviesArray = new JsonArray();
                    while (resultSet.next()) {
                        JsonObject movieObject = new JsonObject();
                        movieObject.addProperty("title", resultSet.getString("title"));
                        movieObject.addProperty("year", resultSet.getInt("year"));
                        movieObject.addProperty("director", resultSet.getString("director"));
                        movieObject.addProperty("id", resultSet.getString("id"));
                        movieObject.addProperty("rating", resultSet.getFloat("rating"));

                        String genresString = resultSet.getString("genres");
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
                        movieObject.add("genres", genresArray);

                        // Retrieve stars
                        String starsString = resultSet.getString("stars");
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
                        Collections.sort(starsList, new Comparator<JsonObject>() {
                            @Override
                            public int compare(JsonObject star1, JsonObject star2) {
                                int movieCountComparison = Integer.compare(
                                        getStarMovieCount(star2.get("id").getAsString()),
                                        getStarMovieCount(star1.get("id").getAsString()));
                                if (movieCountComparison != 0) {
                                    return movieCountComparison;
                                }
                                // If movie counts are equal, compare alphabetically
                                return star1.get("name").getAsString().compareTo(star2.get("name").getAsString());
                            }
                        });

                        JsonArray starsArray = new JsonArray();
                        int count = 0;
                        for (JsonObject star : starsList) {
                            if (count < 3) {
                                starsArray.add(star);
                                count++;
                            } else {
                                break;
                            }
                        }
                        movieObject.add("stars", starsArray);
                        moviesArray.add(movieObject);
                    }
                    PrintWriter out = response.getWriter();
                    out.write(new Gson().toJson(moviesArray));
                    response.setContentType("application/json");
                    response.setStatus(HttpServletResponse.SC_OK);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
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