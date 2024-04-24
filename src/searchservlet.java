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

    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        doGet(request, response);
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("application/json");
        PrintWriter out = response.getWriter();

        String title = request.getParameter("title");
        String year = request.getParameter("year");
        String director = request.getParameter("director");
        String star = request.getParameter("star");

        // Construct SQL query
        StringBuilder queryBuilder = new StringBuilder("SELECT * FROM movies WHERE 1=1");
        if (title != null && !title.isEmpty()) {
            queryBuilder.append(" AND title LIKE ?");
        }
        if (year != null && !year.isEmpty()) {
            queryBuilder.append(" AND year = ?");
        }
        if (director != null && !director.isEmpty()) {
            queryBuilder.append(" AND director LIKE ?");
        }
        if (star != null && !star.isEmpty()) {
            queryBuilder.append(" AND star LIKE ?");
        }

        try (Connection conn = dataSource.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(queryBuilder.toString())) {

            // Set parameters
            int paramIndex = 1;
            if (title != null && !title.isEmpty()) {
                pstmt.setString(paramIndex++, "%" + title + "%");
            }
            if (year != null && !year.isEmpty()) {
                pstmt.setString(paramIndex++, year);
            }
            if (director != null && !director.isEmpty()) {
                pstmt.setString(paramIndex++, "%" + director + "%");
            }
            if (star != null && !star.isEmpty()) {
                pstmt.setString(paramIndex++, "%" + star + "%");
            }

            // Execute query
            ResultSet rs = pstmt.executeQuery();

            // Prepare JSON response
            out.print("[");
            boolean first = true;
            while (rs.next()) {
                if (!first) {
                    out.print(",");
                }
                out.print("{");
                out.print("\"title\": \"" + rs.getString("title") + "\",");
                out.print("\"year\": \"" + rs.getString("year") + "\",");
                out.print("\"director\": \"" + rs.getString("director") + "\",");
                out.print("\"star\": \"" + rs.getString("star") + "\"");
                out.print("}");
                first = false;
            }
            out.print("]");
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
