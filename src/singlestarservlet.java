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

// Declaring a WebServlet called SingleStarServlet, which maps to url "/api/single-star"
@WebServlet(name = "singlestarservlet", urlPatterns = "/single-star")
public class singlestarservlet extends HttpServlet {
    private static final long serialVersionUID = 2L;

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
/* protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {

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
         // Construct a query to get star information and movies they acted in
         String query = "SELECT s.name AS star_name, s.birthYear, m.id AS movie_id, m.title AS movie_title " +
                 "FROM stars AS s " +
                 "JOIN stars_in_movies AS sim ON s.id = sim.starId " +
                 "JOIN movies AS m ON sim.movieId = m.id " +
                 "WHERE s.id = ?";

         // Declare our statement
         PreparedStatement statement = conn.prepareStatement(query);

         // Set the parameter represented by "?" in the query to the id we get from url
         statement.setString(1, id);

         // Perform the query
         ResultSet rs = statement.executeQuery();

         JsonObject starInfo = new JsonObject();
         JsonArray moviesArray = new JsonArray();

         // Iterate through each row of rs
         while (rs.next()) {
             // Retrieve star information
             String starName = rs.getString("star_name");
             String birthYear = rs.getString("birthYear");
             String movieId = rs.getString("movie_id");
             String movieTitle = rs.getString("movie_title");

             // Add movie information to movies array
             JsonObject movieObject = new JsonObject();
             movieObject.addProperty("movie_id", movieId);
             movieObject.addProperty("movie_title", movieTitle);
             moviesArray.add(movieObject);

             // If birthYear is null, set it to "N/A"
             if (birthYear == null) {
                 birthYear = "N/A";
             }

             // Populate star information object
             starInfo.addProperty("star_name", starName);
             starInfo.addProperty("birth_year", birthYear);
         }
         rs.close();
         statement.close();

         // Add movies array to star information object
         starInfo.add("movies", moviesArray);

         // Write JSON string to output
         out.write(starInfo.toString());
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

@WebServlet(name = "singlestarservlet", urlPatterns = "/singlestar")
public class singlestarservlet extends HttpServlet {
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

        String starId = request.getParameter("id");

        try (Connection conn = dataSource.getConnection()) {
            String query = "SELECT name, birthYear FROM stars WHERE id = ?";
            PreparedStatement statement = conn.prepareStatement(query);
            statement.setString(1, starId);
            ResultSet rs = statement.executeQuery();

            JsonObject starJson = new JsonObject();

            if (rs.next()) {
                starJson.addProperty("name", rs.getString("name"));
                int birthYear = rs.getInt("birthYear");
                starJson.addProperty("birthYear", birthYear);

                query = "SELECT m.id, m.title FROM movies m JOIN stars_in_movies sm ON m.id = sm.movieId WHERE sm.starId = ?";
                statement = conn.prepareStatement(query);
                statement.setString(1, starId);
                rs = statement.executeQuery();

                JsonArray moviesArray = new JsonArray();
                while (rs.next()) {
                    JsonObject movieJson = new JsonObject();
                    movieJson.addProperty("id", rs.getString("id"));
                    movieJson.addProperty("title", rs.getString("title"));
                    moviesArray.add(movieJson);
                }
                starJson.add("movies", moviesArray);

                out.write(starJson.toString());
                response.setStatus(200);
            } else {
                JsonObject errorJson = new JsonObject();
                errorJson.addProperty("error", "Star not found");
                out.write(errorJson.toString());
                response.setStatus(404);
            }

            rs.close();
            statement.close();

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
