/*function handleStarResult(resultData) {
    let movieTableBodyElement = document.getElementById("movie_table_body");

    resultData.forEach(movie => {
        let row = movieTableBodyElement.insertRow();

        let titleCell = row.insertCell(0);
        let titleLink = document.createElement("a");
        titleLink.href = "singlemovie.html?id=" + encodeURIComponent(movie.title); // Adjust URL as needed
        titleLink.textContent = movie.title;
        titleCell.appendChild(titleLink);

        let yearCell = row.insertCell(1);
        yearCell.textContent = movie.year;

        let directorCell = row.insertCell(2);
        directorCell.textContent = movie.director;

        let genresCell = row.insertCell(3);
        genresCell.textContent = movie.genres;

        let starsCell = row.insertCell(4);
        let starsArray = movie.stars.split(", ");
        starsArray.forEach(star => {
            let starLink = document.createElement("a");
            starLink.href = "singlestar.html?id=" + encodeURIComponent(star);
            starLink.textContent = star;
            starLink.style.display = "block";
            starsCell.appendChild(starLink);
        });

        let ratingCell = row.insertCell(5);
        ratingCell.textContent = movie.rating;
    });
}

// AJAX call to fetch movie data from backend
function fetchMovieData() {
    jQuery.ajax({
        dataType: "json",
        method: "GET",
        url: "movies",
        success: (resultData) => handleStarResult(resultData),
        error: (xhr, status, error) => {
            console.error("AJAX Error:", status, error);
            alert("Failed to fetch movie data. Please try again later.");
        }
    });}*/