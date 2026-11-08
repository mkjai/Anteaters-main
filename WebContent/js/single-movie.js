// reads ?id= of the addres
// then asks the server for movie and fills in the page
// nothing about the movie is stored here


// creates <a> link element with the given text and destination
// textContent is for characters like < or & or etc in a name to be shown as plain text
// instead of reading as html
function makeLink(text, href) {
    const a = document.createElement("a");
    a.textContent = text;
    a.href = href;
    return a;
}


// shows a message in the #status paragraph
// used for not found and other errors
// the error CSS class turns the text read when isError is true
function showMessage(text, isError) {
    const status = document.getElementById("status");
    status.textContent = text;
    status.hidden = false;
    status.classList.toggle("error", isError);
}


// fetche smovie from the server and fills in the page
async function loadMovie() {
    // returns null if the address has no ?id= part
    // get the id parameter from the address
    const id = new URLSearchParams(window.location.search).get("id");
    if (!id) {
        showMessage("No movie was selected.", true);
        return;
    }


    // ask the servlet for the movie
    try {
        // make sure special characters in the id are safe in URL
        const response = await fetch("api/single-movie?id=" + encodeURIComponent(id));

        // answers 404 when no movie has the id
        if (response.status === 404) {
            showMessage("Movie not found.", true);
            return;
        }

        // any other answers that don't work
        if (!response.ok) {
            throw new Error("the server answered " + response.status);
        }
        const movie = await response.json();


        // fill the page, the browser tab title becomes the movie title too to look nicer
        document.title = movie.title;
        document.getElementById("movie-title").textContent = movie.title;
        document.getElementById("movie-year").textContent = movie.year;
        document.getElementById("movie-director").textContent = movie.director;
        document.getElementById("movie-rating").textContent =
            movie.rating == null ? "N/A" : movie.rating;

        const genres = document.getElementById("movie-genres");
        movie.genres.forEach(function (genre, i) {
            if (i > 0) {
                genres.appendChild(document.createTextNode(", "));
            }
            genres.appendChild(makeLink(genre.name, "index.html?genre=" + encodeURIComponent(genre.id)));
        });

        // one link per star
        // each linke goes to the single star page for that star's id
        const stars = document.getElementById("movie-stars");
        movie.stars.forEach(function (star, i) {
            if (i > 0) {
                stars.appendChild(document.createTextNode(", "));
            }
            stars.appendChild(makeLink(star.name, "single-star.html?id=" + encodeURIComponent(star.id)));
        });

        // new for cart
        document.getElementById("cart-action").appendChild(makeAddToCartButton(movie.id));

        document.getElementById("status").hidden = true;
        document.getElementById("movie-details").hidden = false;
    } catch (err) {
        showMessage("Could not load the movie: " + err.message, true);
    }
}

loadMovie();