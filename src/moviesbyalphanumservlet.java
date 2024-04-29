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
        int pageSize = 10;
        int pageNumber = 1;

        String pageSizeParam = request.getParameter("pageSize");
        String pageNumberParam = request.getParameter("pageNumber");

        if (pageSizeParam != null) {
            pageSize = Integer.parseInt(pageSizeParam);
        }

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
                jsonObject.add("genres", genresArray);

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
                Collections.sort(starsList, new Comparator<JsonObject>() {
                    @Override
                    public int compare(JsonObject star1, JsonObject star2) {
                        int movieCountComparison = Integer.compare(
                                getStarMovieCount(star2.get("id").getAsString()),
                                getStarMovieCount(star1.get("id").getAsString()));
                        if (movieCountComparison != 0) {
                            return movieCountComparison;
                        }
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
                jsonObject.add("stars", starsArray);

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

    private PreparedStatement buildPreparedStatement(Connection conn, String character, String sortOption, int pageSize,
            int pageNumber) throws Exception {
        String query;
        String orderBy;

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
                    orderBy = "ORDER BY m.title ASC, r.rating DESC";
                    break;
            }
        } else {

            orderBy = "ORDER BY m.title ASC, r.rating DESC";
        }

        if (character.equals("*")) {
            query = "SELECT m.id, m.title, m.year, m.director, r.rating, "
                    + "GROUP_CONCAT(DISTINCT CONCAT(g.id, ':', g.name) ORDER BY g.name SEPARATOR ',') AS genres, "
                    + "GROUP_CONCAT(DISTINCT CONCAT(s.id, ':', s.name) ORDER BY s.name SEPARATOR ',') AS stars "
                    + "FROM movies m "
                    + "LEFT JOIN ratings r ON m.id = r.movieId "
                    + "LEFT JOIN genres_in_movies gim ON m.id = gim.movieId "
                    + "LEFT JOIN genres g ON gim.genreId = g.id "
                    + "LEFT JOIN stars_in_movies sim ON m.id = sim.movieId "
                    + "LEFT JOIN stars s ON sim.starId = s.id "
                    + "WHERE LOWER(m.title) REGEXP '^[^a-z0-9]' "
                    + "GROUP BY m.id "
                    + orderBy;
        } else {
            character = character.toLowerCase() + "%";
            query = "SELECT m.id, m.title, m.year, m.director, r.rating, "
                    + "GROUP_CONCAT(DISTINCT CONCAT(g.id, ':', g.name) ORDER BY g.name SEPARATOR ',') AS genres, "
                    + "GROUP_CONCAT(DISTINCT CONCAT(s.id, ':', s.name) ORDER BY s.name SEPARATOR ',') AS stars "
                    + "FROM movies m "
                    + "LEFT JOIN ratings r ON m.id = r.movieId "
                    + "LEFT JOIN genres_in_movies gim ON m.id = gim.movieId "
                    + "LEFT JOIN genres g ON gim.genreId = g.id "
                    + "LEFT JOIN stars_in_movies sim ON m.id = sim.movieId "
                    + "LEFT JOIN stars s ON sim.starId = s.id "
                    + "WHERE LOWER(m.title) LIKE ? "
                    + "GROUP BY m.id "
                    + orderBy;
        }

        int offset = (pageNumber - 1) * pageSize;

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
