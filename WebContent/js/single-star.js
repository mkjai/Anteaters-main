/* Single Star Starter/Skeleton Provided */
function makeLink(text, href) {
    const a = document.createElement("a");
    a.textContent = text;
    a.href = href;
    return a;
}

function makeCell(content) {
    const td = document.createElement("td");
    if (content instanceof Node) {
        td.appendChild(content);
    } else {
        td.textContent = content == null ? "N/A" : content;
    }
    return td;
}

function showMessage(text, isError) {
    const status = document.getElementById("status");
    status.textContent = text;
    status.hidden = false;
    status.classList.toggle("error", isError);
}

async function loadStar() {
    const id = new URLSearchParams(window.location.search).get("id");
    if (!id) {
        showMessage("No star was selected.", true);
        return;
    }

    try {
        const response = await fetch("api/single-star?id=" + encodeURIComponent(id));

        if (response.status === 404) {
            showMessage("Star not found.", true);
            return;
        }
        if (!response.ok) {
            throw new Error("the server answered " + response.status);
        }
        const rows = await response.json();
        const star = rows[0];

        document.title = star.star_name;
        document.getElementById("star-name").textContent = star.star_name;
        document.getElementById("star-birth-year").textContent =
            star.star_dob == null ? "N/A" : star.star_dob;

        // a star with no movies comes back as one row with movie_id null
        const body = document.getElementById("movie-table-body");
        const movies = rows.filter(function (row) {
            return row.movie_id != null;
        });
        movies.forEach(function (row) {
            const tr = document.createElement("tr");
            tr.appendChild(makeCell(makeLink(row.movie_title,
                "single-movie.html?id=" + encodeURIComponent(row.movie_id))));
            tr.appendChild(makeCell(row.movie_year));
            tr.appendChild(makeCell(row.movie_director));
            tr.appendChild(makeCell(makeAddToCartButton(row.movie_id))); // new for cart
            body.appendChild(tr);
        });
        if (movies.length === 0) {
            document.getElementById("movie-table").hidden = true;
            document.getElementById("no-movies").hidden = false;
        }

        document.getElementById("status").hidden = true;
        document.getElementById("star-details").hidden = false;
    } catch (err) {
        showMessage("Could not load the star: " + err.message, true);
    }
}

loadStar();