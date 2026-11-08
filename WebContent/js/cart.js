// the cart itself lives in the session on the server, nothing is kept here

function money(amount) {
  return "$" + Number(amount).toFixed(2);
}

// sends one change to the server (add, increase, decrease or remove) and gives back the whole cart
async function changeCart(action, id) {
  const response = await fetch("api/cart", {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body: new URLSearchParams({ action: action, id: id }),
  });
  const cart = await response.json().catch(function () {
    return null;
  });
  if (!response.ok || cart == null) {
    throw new Error(
      cart && cart.error ? cart.error : "the server answered " + response.status
    );
  }
  return cart;
}

// a button that adds one copy of the movie to the shopping cart each time it is clicked
function makeAddToCartButton(movieId) {
  const button = document.createElement("button");
  const label = "Add to Shopping Cart";
  button.type = "button";
  button.className = "add-button";
  button.textContent = label;

  let timer = null;
  button.addEventListener("click", async function () {
    button.disabled = true;
    try {
      const cart = await changeCart("add", movieId);
      const item = cart.items.find(function (line) {
        return line.id === movieId;
      });
      button.textContent = "Added (" + (item ? item.quantity : 1) + " in cart)";
    } catch (err) {
      button.textContent = "Could not add";
      button.title = err.message;
    } finally {
      button.disabled = false;
    }
    // go back to the normal label after a moment
    clearTimeout(timer);
    timer = setTimeout(function () {
      button.textContent = label;
      button.removeAttribute("title");
    }, 2000);
  });
  return button;
}
