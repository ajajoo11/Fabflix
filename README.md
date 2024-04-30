Team Members - Bhuvan Chandra and Ananya Jajoo

Video URL - Project 1 - https://youtu.be/v9srqGTbPfw
Project 2 - https://youtu.be/gfyWOM3IWo0 

Bhuvan Chandra - Set up the Github structure, AWS instance, set up sql and tomcat on personal computer, coded the commands for sql database creation, worked on the code for the servlets, html, and js files.
Ananya Jajoo - Set up SQL, Tomcat features on the AWS instance, set up sql and tomcat on personal computer created the sql database on AWS instance, worked on coding the servlets, html, and js files.

This is the base query used for searching
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

After this, we include other query parameters based on whether title was chosen etc.
if (title != null && !title.isEmpty()) {
                queryBuilder.append("m.title LIKE ? AND ");
            }

For grouping:
queryBuilder.append(" GROUP BY m.id ");
 switch (sortOption) {
                case "title_asc_rating_desc":
                    queryBuilder.append(" ORDER BY m.title ASC, r.rating DESC");
                    break;

Setting offsets and limit:
 queryBuilder.append(" LIMIT ? OFFSET ?");

For matching
if (title != null && !title.isEmpty()) {
                statement.setString(parameterIndex++, "%" + title + "%");
            }

This way we build the query for searching.



