
// addds <li><a href=""></a></li> to a <ul>
// textContent so a name is always shown as plain text
function addLink(list, text, href) {
    const li = document.createElement("li");
    const a = document.createElement("a");
    a.textContent = text;
    a.href = href;
    li.appendChild(a);
    list.appendChild(li);
}

// browse by title, these are fixed characters, not movie data, so they are built here.
function showTitleLinks() {
    const list = document.getElementById("char-list");
    const characters = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    for (const ch of characters) {
        addLink(list, ch, "index.html?startsWith=" + encodeURIComponent(ch));
    }
}

// browse by genre: the genres come from the database, already sorted by name by the servlet
async function showGenreLinks() {
    const status = document.getElementById("genre-status");
    const list = document.getElementById("genre-list");
    try {
        const response = await fetch("api/genres");
        if (!response.ok) {
            throw new Error("the server answered " + response.status);
        }
        const genres = await response.json();
        genres.forEach(function (genre) {
            addLink(list, genre.name, "index.html?genre=" + encodeURIComponent(genre.id));
        });
        status.hidden = true;
    } catch (err) {
        status.textContent = "Could not load genres: " + err.message;
        status.classList.add("error");
    }
}

showTitleLinks();
showGenreLinks();