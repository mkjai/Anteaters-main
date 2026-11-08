const CRITERIA = ["title", "year", "director", "star", "genre", "startsWith"];

function makeLink(text, href) {
  const a = document.createElement("a");
  a.textContent = text;
  a.href = href;
  return a;
}

// build "a, b, c", each name is a link. used for stars
function makeLinkList(items, hrefFor) {
  const span = document.createElement("span");
  items.forEach(function (item, i) {
    if (i > 0) {
      span.appendChild(document.createTextNode(", "));
    }
    span.appendChild(makeLink(item.name, hrefFor(item)));
  });
  return span;
}

function makeRow(movie) {
  const row = document.createElement("tr");

  const title = document.createElement("td");
  title.appendChild(
    makeLink(
      movie.title,
      "single-movie.html?id=" + encodeURIComponent(movie.id)
    )
  );
  const year = document.createElement("td");
  year.textContent = movie.year;
  const director = document.createElement("td");
  director.textContent = movie.director;
  const genres = document.createElement("td");
  genres.appendChild(
    makeLinkList(movie.genres, function (genre) {
      return "index.html?genre=" + encodeURIComponent(genre.id);
    })
  );
  const stars = document.createElement("td");
  stars.appendChild(
    makeLinkList(movie.stars, function (star) {
      return "single-star.html?id=" + encodeURIComponent(star.id);
    })
  );
  const rating = document.createElement("td");
  rating.textContent = movie.rating == null ? "N/A" : movie.rating;

  // new for cart page
  const cart = document.createElement("td");
  cart.appendChild(makeAddToCartButton(movie.id));

  [title, year, director, genres, stars, rating, cart].forEach(function (cell) {
    row.appendChild(cell);
  });
  return row;
}

// true if the address has at least one search/brose parameter that is not blank
function hasCriteria() {
  const params = new URLSearchParams(window.location.search);
  return CRITERIA.some(function (name) {
    return (params.get(name) || "").trim() !== "";
  });
}

function goTo(changes) {
  const params = new URLSearchParams(window.location.search);
  Object.keys(changes).forEach(function (name) {
    params.set(name, changes[name]);
  });
  window.location.search = params.toString();
}

function setUpControls(result) {
  const sortSelect = document.getElementById("sort-select");
  const limitSelect = document.getElementById("limit-select");
  sortSelect.value = result.sort;
  limitSelect.value = String(result.limit);

  sortSelect.addEventListener("change", function () {
    goTo({ sort: sortSelect.value, page: 1 });
  });
  limitSelect.addEventListener("change", function () {
    goTo({ limit: limitSelect.value, page: 1 });
  });
  document.getElementById("list-controls").hidden = false;
}

function setUpPager(result) {
  const lastPage = Math.max(1, Math.ceil(result.total / result.limit));
  const prev = document.getElementById("prev-button");
  const next = document.getElementById("next-button");

  prev.disabled = result.page <= 1;
  next.disabled = result.page >= lastPage;
  prev.addEventListener("click", function () {
    goTo({ page: Math.min(result.page - 1, lastPage) });
  });
  next.addEventListener("click", function () {
    goTo({ page: result.page + 1 });
  });

  document.getElementById("page-info").textContent =
    "Page " + result.page + " of " + lastPage;
  document.getElementById("pager").hidden = false;
}

async function loadMovies() {
  const status = document.getElementById("status");
  const table = document.getElementById("movie-table");
  const body = document.getElementById("movie-body");

  document.getElementById("page-title").textContent = hasCriteria()
    ? "Search Results"
    : "Movies";
  try {
    const response = await fetch("api/movies" + window.location.search);
    const result = await response.json().catch(function () {
      return null;
    });
    if (!response.ok || result == null) {
      throw new Error(
        result && result.error
          ? result.error
          : "the server answered " + response.status
      );
    }

    setUpControls(result);
    if (result.total === 0) {
      status.textContent = "No movies found.";
      return;
    }

    setUpPager(result);
    if (result.movies.length === 0) {
      status.textContent =
        "There are no movies on this page. Use Previous to go back.";
      return;
    }
    result.movies.forEach(function (movie) {
      body.appendChild(makeRow(movie));
    });
    table.hidden = false;

    const first = (result.page - 1) * result.limit + 1;
    const last = first + result.movies.length - 1;
    status.textContent =
      "Showing " +
      first +
      "–" +
      last +
      " of " +
      result.total +
      (result.total === 1 ? " movie." : " movies.");
  } catch (err) {
    status.textContent = "Could not load movies: " + err.message;
    status.classList.add("error");
  }
}

loadMovies();
