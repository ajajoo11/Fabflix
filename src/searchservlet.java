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
            queryBuilder.append(
                    "SELECT DISTINCT m.*, r.rating, " +
                            "GROUP_CONCAT(DISTINCT CONCAT(g.id, ':', g.name) ORDER BY g.name SEPARATOR ',') AS genres, "
                            +
                            "GROUP_CONCAT(DISTINCT CONCAT(s.id, ':', s.name) ORDER BY s.name SEPARATOR ',') AS stars " +
                            "FROM movies m " +
                            "LEFT JOIN ratings r ON m.id = r.movieId " +
                            "LEFT JOIN stars_in_movies sm ON m.id = sm.movieId " +
                            "LEFT JOIN stars s ON sm.starId = s.id " +
                            "LEFT JOIN genres_in_movies gm ON m.id = gm.movieId " +
                            "LEFT JOIN genres g ON gm.genreId = g.id " +
                            "WHERE ");

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
            queryBuilder.append(" GROUP BY m.id ");
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
            System.out.println("Generated SQL Query: " + queryBuilder.toString());

            ResultSet rs = statement.executeQuery();
            JsonArray searchResultsArray = new JsonArray();

            while (rs.next()) {
                JsonObject resultObject = new JsonObject();
                resultObject.addProperty("id", rs.getString("id"));
                resultObject.addProperty("title", rs.getString("title"));
                resultObject.addProperty("year", rs.getInt("year"));
                resultObject.addProperty("director", rs.getString("director"));
                resultObject.addProperty("rating", rs.getFloat("rating"));

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
                resultObject.add("genres", genresArray);

                // Retrieve stars
                String starsString = rs.getString("stars");
                List<JsonObject> starsList = new ArrayList<>();
                if (starsString != null) {
                    String[] stars = starsString.split(",");
                    for (String star4 : stars) {
                        String[] starInfo = star4.split(":");
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
                for (JsonObject star3 : starsList) {
                    if (count < 3) {
                        starsArray.add(star3);
                        count++;
                    } else {
                        break; // Exit loop once three stars are added
                    }
                }
                resultObject.add("stars", starsArray);
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