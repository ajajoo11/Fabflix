/*import com.google.gson.JsonArray;
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

// Declaring a WebServlet called SingleMovieServlet, which maps to url "/api/single-movie"
@WebServlet(name = "singlemovieservlet", urlPatterns = "/single-movie")
public class singlemovieservlet extends HttpServlet {
    private static final long serialVersionUID = 3L;

    // Create a dataSource which registered in web.xml
    private DataSource dataSource;

    public void init(ServletConfig config) {
        try {
            dataSource = (DataSource) new InitialContext().lookup("java:comp/env/jdbc/moviedb");
        } catch (NamingException e) {
            e.printStackTrace();
        }
    }

    /**
     * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
     *      response)
     */
/*protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {

    response.setContentType("application/json"); // Response mime type

    // Retrieve parameter id from url request.
    String id = request.getParameter("id");

    // The log message can be found in localhost log
    request.getServletContext().log("getting id: " + id);

    // Output stream to STDOUT
    PrintWriter out = response.getWriter();

    // Get a connection from dataSource and let resource manager close the
    // connection after usage.
    try (Connection conn = dataSource.getConnection()) {
        // Construct a query to get movie information, genres, and stars
        String query = "SELECT m.title, m.year, m.director, " +
                "GROUP_CONCAT(DISTINCT g.name ORDER BY g.name SEPARATOR ',') AS genres, " +
                "GROUP_CONCAT(DISTINCT s.name ORDER BY s.name SEPARATOR ',') AS stars, " +
                "r.rating " +
                "FROM movies m " +
                "LEFT JOIN ratings r ON m.id = r.movieId " +
                "LEFT JOIN genres_in_movies gim ON m.id = gim.movieId " +
                "LEFT JOIN genres g ON gim.genreId = g.id " +
                "LEFT JOIN stars_in_movies sim ON m.id = sim.movieId " +
                "LEFT JOIN stars s ON sim.starId = s.id " +
                "WHERE m.id = ? " +
                "GROUP BY m.id";

        // Declare our statement
        PreparedStatement statement = conn.prepareStatement(query);

        // Set the parameter represented by "?" in the query to the id we get from url
        statement.setString(1, id);

        // Perform the query
        ResultSet rs = statement.executeQuery();

        JsonObject movieInfo = new JsonObject();

        // Iterate through each row of rs
        if (rs.next()) {
            // Retrieve movie information
            String title = rs.getString("title");
            int year = rs.getInt("year");
            String director = rs.getString("director");
            String genres = rs.getString("genres");
            String stars = rs.getString("stars");
            float rating = rs.getFloat("rating");

            // Populate movie information object
            movieInfo.addProperty("title", title);
            movieInfo.addProperty("year", year);
            movieInfo.addProperty("director", director);
            movieInfo.addProperty("genres", genres);
            movieInfo.addProperty("stars", stars);
            movieInfo.addProperty("rating", rating);
        }
        rs.close();
        statement.close();

        // Write JSON string to output
        out.write(movieInfo.toString());
        // Set response status to 200 (OK)
        response.setStatus(200);

    } catch (Exception e) {
        // Write error message JSON object to output
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("errorMessage", e.getMessage());
        out.write(jsonObject.toString());

        // Log error to localhost log
        request.getServletContext().log("Error:", e);
        // Set response status to 500 (Internal Server Error)
        response.setStatus(500);
    } finally {
        out.close();
    }

    // Always remember to close db connection after usage. Here it's done by
    // try-with-resources

}

}*/

import com.google.gson.JsonObject;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

@WebServlet(name = "singlermovieservlet", urlPatterns = "/singlemovie")
public class singlemovieservlet extends HttpServlet {
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
        if (title == null || title.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            JsonObject errorObject = new JsonObject();
            errorObject.addProperty("error", "Title parameter is required.");
            out.write(errorObject.toString());
            return;
        }

        try (Connection conn = dataSource.getConnection()) {
            String query = "SELECT m.title, m.year, m.director, " +
                    "GROUP_CONCAT(DISTINCT g.name ORDER BY g.name SEPARATOR ',') AS genres, " +
                    "GROUP_CONCAT(DISTINCT s.name ORDER BY s.name SEPARATOR ',') AS stars, " +
                    "r.rating " +
                    "FROM movies m " +
                    "JOIN ratings r ON m.id = r.movieId " +
                    "JOIN genres_in_movies gim ON m.id = gim.movieId " +
                    "JOIN genres g ON gim.genreId = g.id " +
                    "JOIN stars_in_movies sim ON m.id = sim.movieId " +
                    "JOIN stars s ON sim.starId = s.id " +
                    "WHERE m.title = ? " +
                    "GROUP BY m.id";

            PreparedStatement statement = conn.prepareStatement(query);
            statement.setString(1, title);
            ResultSet rs = statement.executeQuery();

            JsonObject jsonObject = new JsonObject();
            if (rs.next()) {
                jsonObject.addProperty("title", rs.getString("title"));
                jsonObject.addProperty("year", rs.getInt("year"));
                jsonObject.addProperty("director", rs.getString("director"));
                jsonObject.addProperty("genres", rs.getString("genres"));
                jsonObject.addProperty("stars", rs.getString("stars"));
                jsonObject.addProperty("rating", rs.getDouble("rating"));
            }

            rs.close();
            statement.close();

            out.write(jsonObject.toString());
            response.setStatus(HttpServletResponse.SC_OK);

        } catch (Exception e) {
            JsonObject errorObject = new JsonObject();
            errorObject.addProperty("error", e.getMessage());
            out.write(errorObject.toString());
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        } finally {
            out.close();
        }
    }
}
