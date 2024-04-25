import javax.naming.InitialContext;
import javax.naming.NamingException;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
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
import java.util.List;

@WebServlet(name = "searchservlet", urlPatterns = "/search")
public class searchservlet extends HttpServlet {
    private DataSource dataSource;

    public void init(ServletConfig config) {
        try {
            dataSource = (DataSource) new InitialContext().lookup("java:comp/env/jdbc/moviedb");
        } catch (NamingException e) {
            e.printStackTrace(); // Log the error for diagnostic purposes
            // You might want to set a flag here to indicate that the data source is not
            // available
        }
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("application/json");
        PrintWriter out = response.getWriter();

        String searchTerm = request.getParameter("query");
        String[] searchCriteria = request.getParameterValues("criteria");

        // Construct SQL query
        String query = "SELECT * FROM movies WHERE ";
        List<String> conditions = new ArrayList<>();

        for (String criteria : searchCriteria) {
            switch (criteria) {
                case "title":
                    conditions.add("title LIKE ?");
                    break;
                case "year":
                    conditions.add("year = ?");
                    break;
                case "director":
                    conditions.add("director LIKE ?");
                    break;
                case "star":
                    conditions.add("star LIKE ?");
                    break;
            }
        }

        if (!conditions.isEmpty()) {
            query += String.join(" OR ", conditions);
        } else {
            // No criteria selected, return empty result set
            out.print("[]");
            response.setStatus(HttpServletResponse.SC_OK);
            out.close();
            return;
        }

        try (Connection conn = dataSource.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(query)) {

            // Set parameters
            for (int i = 0; i < searchCriteria.length; i++) {
                pstmt.setString(i + 1, "%" + searchTerm + "%");
            }

            // Execute query
            ResultSet rs = pstmt.executeQuery();

            // Prepare JSON response
            List<String> resultJSON = new ArrayList<>();
            while (rs.next()) {
                String movieJSON = "{" +
                        "\"title\": \"" + rs.getString("title") + "\"," +
                        "\"year\": \"" + rs.getString("year") + "\"," +
                        "\"director\": \"" + rs.getString("director") + "\"," +
                        "\"star\": \"" + rs.getString("star") + "\"" +
                        "}";
                resultJSON.add(movieJSON);
            }

            out.print("[" + String.join(",", resultJSON) + "]");
            response.setStatus(HttpServletResponse.SC_OK);
        } catch (SQLException e) {
            e.printStackTrace(); // Log the error for diagnostic purposes
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print("{\"error\": \"Internal server error\"}");
        } finally {
            out.close();
        }
    }
}
