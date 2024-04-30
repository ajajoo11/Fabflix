import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;

@WebFilter(filterName = "LoginFilter", urlPatterns = { "/*" })
public class LoginFilter implements Filter {

    public void init(FilterConfig filterConfig) {
    }

    public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain)
            throws ServletException, IOException {
        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) resp;

        HttpSession session = request.getSession(false); // Do not create session if it doesn't exist
        String loginURI = request.getContextPath() + "/login.html";
        String loginServletURI = request.getContextPath() + "/Fabflix";

        System.out.println("Requested URI: " + request.getRequestURI());
        System.out.println("Login URI: " + loginURI);
        System.out.println("Login Servlet URI: " + loginServletURI);
        System.out.println("Context Path : " + request.getContextPath());
        System.out.println(request.getRequestURI().startsWith(loginServletURI));
        String baseURL = request.getContextPath() + "/";
        System.out.println("Base URL: " + baseURL);

        if (request.getRequestURI().equals(baseURL)) {
            // Base URL requested, redirect to login page if not logged in
            if (session == null || session.getAttribute("email") == null) {
                System.out.println("Base URL requested, filtering");
                chain.doFilter(request, response);
                // response.sendRedirect(loginURI);
                return;
            }
        }

        if (request.getRequestURI().equals("/Fabflix/css/login.css")) {
            if (session == null || session.getAttribute("email") == null) {
                System.out.println("CSS href URL requested, filtering");
                chain.doFilter(request, response);
                // response.sendRedirect(loginURI);
                return;
            }
        }

        // Exclude the login servlet URL from filtering
        if (request.getRequestURI().startsWith(loginServletURI) || request.getRequestURI().equals(loginURI)) {
            System.out.println("Request to login servlet or login page, passing through filter.");
            // response.sendRedirect("/Fabflix/login.html");
            chain.doFilter(request, response);
            return;
        }

        // boolean loggedIn = session != null && session.getAttribute("email") != null;
        boolean loggedIn = session != null && session.getAttribute("email") != null;
        // System.out.println("Session attribute 'email': " +
        // session.getAttribute("email"));
        System.out.println(loggedIn);

        if (loggedIn) {
            // User is logged in, proceed
            System.out.println("User is logged in, proceeding to requested page.");
            chain.doFilter(request, response);
        } else {
            // User is not logged in and accessing other URLs, redirect to login page
            System.out.println("User is not logged in, redirecting to login page.");
            response.sendRedirect(loginURI);
        }
    }

    public void destroy() {
    }
}