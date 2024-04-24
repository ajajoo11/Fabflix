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
import java.sql.Statement;

@WebServlet(name = "singlegenreservlet", urlPatterns = "/singlegenrepage")
public class singlegenreservlet extends HttpServlet {

    private DataSource dataSource;

    @Override
    public void init() {
        try {
            dataSource = (DataSource) new InitialContext().lookup("java:comp/env/jdbc/moviedb");
        } catch (NamingException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String genreId = request.getParameter("id");
        if (genreId == null || genreId.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        try (Connection connection = dataSource.getConnection()) {
            String query = "SELECT m.id, m.title, m.year, m.director " +
                    "FROM movies m " +
                    "JOIN genres_in_movies gim ON m.id = gim.movieId " +
                    "WHERE gim.genreId = ?";
            try (PreparedStatement statement = connection.prepareStatement(query)) {
                statement.setString(1, genreId);
                try (ResultSet resultSet = statement.executeQuery()) {
                    JsonArray moviesArray = new JsonArray();
                    while (resultSet.next()) {
                        JsonObject movieObject = new JsonObject();
                        movieObject.addProperty("title", resultSet.getString("title"));
                        movieObject.addProperty("year", resultSet.getInt("year"));
                        movieObject.addProperty("director", resultSet.getString("director"));
                        movieObject.addProperty("id", resultSet.getString("id"));

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
}
