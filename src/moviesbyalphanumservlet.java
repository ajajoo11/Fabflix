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
        String query;

        if (character == null || character.isEmpty() || character.length() > 1) {
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("errorMessage", "Invalid character");
            out.write(jsonObject.toString());
            response.setStatus(400); // Bad Request
            return;
        }

        try (Connection conn = dataSource.getConnection()) {
            if (character.equals("*")) {
                query = "SELECT m.id, m.title, m.year, m.director, r.rating " +
                        "FROM movies m " +
                        "LEFT JOIN ratings r ON m.id = r.movieId " +
                        "WHERE LOWER(m.title) REGEXP '^[^a-z0-9]' " +
                        "ORDER BY r.rating DESC";
            } else {
                character = character.toLowerCase() + "%";
                query = "SELECT m.id, m.title, m.year, m.director, r.rating " +
                        "FROM movies m " +
                        "LEFT JOIN ratings r ON m.id = r.movieId " +
                        "WHERE LOWER(m.title) LIKE ? " +
                        "ORDER BY r.rating DESC ";
            }

            PreparedStatement pstmt = conn.prepareStatement(query);
            if (!character.equals("*")) {
                pstmt.setString(1, character);
            }

            ResultSet rs = pstmt.executeQuery();

            JsonArray jsonArray = new JsonArray();
            while (rs.next()) {
                JsonObject jsonObject = new JsonObject();
                jsonObject.addProperty("title", rs.getString("title"));
                jsonObject.addProperty("id", rs.getString("id"));
                jsonObject.addProperty("year", rs.getInt("year"));
                jsonObject.addProperty("director", rs.getString("director"));
                jsonObject.addProperty("rating", rs.getDouble("rating"));

                jsonArray.add(jsonObject);
            }
            rs.close();
            pstmt.close();

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
