package Parser;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;

public class MovieXMLParser {

    private static final String JDBC_URL = "jdbc:mysql://localhost:3306/moviedb";
    private static final String DB_USERNAME = "mytestuser";
    private static final String DB_PASSWORD = "My6$Password";

    private static final String STARS_FILE_PATH = "stars.txt";
    private static final String GENRES_FILE_PATH = "genres.txt";
    private static final String MOVIES_FILE_PATH = "movies.txt";
    private static final String STARS_IN_MOVIES_FILE_PATH = "stars_in_movies.txt";
    private static final String GENRES_IN_MOVIES_FILE_PATH = "genres_in_movies.txt";

    private static FileWriter starsWriter;
    private static FileWriter genresWriter;
    private static FileWriter moviesWriter;
    private static FileWriter starsInMoviesWriter;
    private static FileWriter genresInMoviesWriter;

    private static final int BATCH_SIZE = 1000;

    // Initialize lastId with an initial value
    private static String lastId = "tt0000000";

    static {
        try {
            starsWriter = new FileWriter(STARS_FILE_PATH);
            genresWriter = new FileWriter(GENRES_FILE_PATH);
            moviesWriter = new FileWriter(
                    "/Users/ananyajajoo/Desktop/Spring 2024/CS122B/mycs122b-projects/src/Parser/movies.txt");
            starsInMoviesWriter = new FileWriter(STARS_IN_MOVIES_FILE_PATH);
            genresInMoviesWriter = new FileWriter(GENRES_IN_MOVIES_FILE_PATH);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void closeWriters() {
        try {
            starsWriter.close();
            genresWriter.close();
            moviesWriter.close();
            starsInMoviesWriter.close();
            genresInMoviesWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        try {
            SAXParserFactory factory = SAXParserFactory.newInstance();
            SAXParser saxParser = factory.newSAXParser();

            MovieHandler handler = new MovieHandler();
            saxParser.parse("resources/stanford-movies/mains243.xml", handler);

            int totalMoviesInserted = handler.getMovieCount();
            System.out.println("Total movies inserted: " + totalMoviesInserted);

            closeWriters(); // Close all writers after processing
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static class MovieHandler extends DefaultHandler {

        private StringBuilder data;
        private String movieId;
        private String title;
        private int year;
        private String director;
        private int movieCount = 0;

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes)
                throws SAXException {
            data = new StringBuilder();
            if (qName.equalsIgnoreCase("film")) {
                movieId = attributes.getValue("fid");
            }
        }

        @Override
        public void characters(char[] ch, int start, int length) throws SAXException {
            data.append(new String(ch, start, length));
        }

        @Override
        public void endElement(String uri, String localName, String qName) throws SAXException {
            if (qName.equalsIgnoreCase("film")) {
                if (movieId == null || movieId.isEmpty()) {
                    movieId = generateNewId();
                }
                // Update database with movie details
                updateDatabase(movieId, title, year, director);
                movieCount++; // Increment movie count
            } else if (qName.equalsIgnoreCase("t")) {
                title = data.toString().trim();
            } else if (qName.equalsIgnoreCase("year")) {
                try {
                    year = Integer.parseInt(data.toString().trim());
                } catch (NumberFormatException e) {
                    // Handle error if year is not a valid integer
                    System.out.println("Invalid year format for movie: " + title);
                    year = -1; // Set a default value or mark it as invalid
                }
            } else if (qName.equalsIgnoreCase("dirn")) {
                director = data.toString().trim();
            }
        }

        private String generateNewId() {
            try (Connection connection = DriverManager.getConnection(JDBC_URL, DB_USERNAME, DB_PASSWORD)) {
                // Extract the last ID only once
                if (lastId.equals("tt0000000")) {
                    PreparedStatement statement = connection
                            .prepareStatement("SELECT id FROM movies ORDER BY id DESC LIMIT 1");
                    ResultSet resultSet = statement.executeQuery();
                    if (resultSet.next()) {
                        lastId = resultSet.getString("id");
                    }
                }

                // Increment the last ID
                int lastNumeric = Integer.parseInt(lastId.substring(2));
                int newNumeric = lastNumeric + 1;
                String newId = "tt" + String.format("%07d", newNumeric);
                lastId = newId; // Update the last ID

                return newId;
            } catch (SQLException e) {
                e.printStackTrace();
                return null;
            }
        }

        private void updateDatabase(String movieId, String title, int year, String director) {

            try (Connection connection = DriverManager.getConnection(JDBC_URL, DB_USERNAME, DB_PASSWORD)) {
                // System.out.println("Adding movie with movie title: " + title + movieId);
                String sql = "INSERT INTO movies (id, title, year, director) VALUES (?, ?, ?, ?)";
                PreparedStatement preparedStatement = connection.prepareStatement(sql);
                preparedStatement.setString(1, movieId);
                preparedStatement.setString(2, title);
                preparedStatement.setInt(3, year);
                preparedStatement.setString(4, director);
                preparedStatement.executeUpdate();

                // Write data to movies.txt
                moviesWriter.write(movieId + ";" + title + ";" + year + ";" + director + "\n");

            } catch (SQLException | IOException e) {
                // Handle SQL exceptions
                if (e instanceof SQLIntegrityConstraintViolationException) {
                    // Handle duplicate entry error
                    System.out.println("Duplicate entry error for movie: " + title);
                } else {
                    e.printStackTrace();
                }
            }
        }

        public int getMovieCount() {
            return movieCount;
        }
    }
}
