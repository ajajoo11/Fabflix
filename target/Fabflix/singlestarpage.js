/**
 * Retrieve parameter from request URL, matching by parameter name
 * @param target String
 * @returns {*}
 */
/*function getParameterByName(target) {
    // Get request URL
    let url = window.location.href;
    // Encode target parameter name to URL encoding
    target = target.replace(/[\[\]]/g, "\\$&");

    // Use regular expression to find matched parameter value
    let regex = new RegExp("[?&]" + target + "(=([^&#]*)|&|#|$)"),
        results = regex.exec(url);
    if (!results) return null;
    if (!results[2]) return '';

    // Return the decoded parameter value
    return decodeURIComponent(results[2].replace(/\+/g, " "));
}*/

/**
 * Handles the data returned by the API, read the jsonObject and populate data into HTML elements
 * @param resultData jsonObject
 */
/*function handleResult(resultData) {
    console.log("handleResult: populating star info from resultData");

    // Populate the star info
    const starInfoElement = document.getElementById('starInfo');
    starInfoElement.innerHTML = `
        <p><strong>Name:</strong> ${resultData.star_name}</p>
        <p><strong>Year of Birth:</strong> ${resultData.birth_year}</p>
        <p><strong>Movies:</strong></p>
        <ul>
            ${resultData.movies.map(movie => `<li><a href="/single-movie?id=${movie.movie_id}">${movie.movie_title}</a></li>`).join('')}
        </ul>
    `;
}
*/
/**
 * Fetch data from the servlet and display star information
 */
/*function fetchStarData() {
    // Get star ID from URL parameter
    const starId = getParameterByName('id');

    // Make HTTP GET request to servlet endpoint
    fetch(`/single-starpage?id=${starId}`)
        .then(response => response.json())
        .then(data => {
            // Call handleResult function to populate data into HTML elements
            handleResult(data);
        })
        .catch(error => console.error('Error fetching star information:', error));
}

// Call fetchStarData function once the page is loaded
window.onload = fetchStarData;*/
