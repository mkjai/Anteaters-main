// script for cart.html
// shows the cart the server has, and sends every change back to the server

const statusBox = document.getElementById("status");
const table = document.getElementById("cart-table");
const body = document.getElementById("cart-body");
const summary = document.getElementById("cart-summary");
const totalCell = document.getElementById("cart-total");
const payButton = document.getElementById("pay-button");

function showMessage(text, isError) {
  statusBox.textContent = text;
  statusBox.hidden = false;
  statusBox.classList.toggle("error", isError);
}

function makeButton(text, className, onClick) {
  const button = document.createElement("button");
  button.type = "button";
  button.className = className;
  button.textContent = text;
  button.addEventListener("click", onClick);
  return button;
}

// asks the server to change the cart, then draws what comes back
async function update(action, id) {
  try {
    showCart(await changeCart(action, id));
  } catch (err) {
    showMessage("Could not change the cart: " + err.message, true);
  }
}

function makeRow(item) {
  const row = document.createElement("tr");

  const title = document.createElement("td");
  const link = document.createElement("a");
  link.textContent = item.title;
  link.href = "single-movie.html?id=" + encodeURIComponent(item.id);
  title.appendChild(link);

  const price = document.createElement("td");
  price.textContent = money(item.price);

  const quantity = document.createElement("td");
  const minus = makeButton("−", "qty-button", function () {
    update("decrease", item.id);
  });
  minus.title = "One less";
  minus.disabled = item.quantity <= 1;
  const count = document.createElement("span");
  count.className = "qty";
  count.textContent = item.quantity;
  const plus = makeButton("+", "qty-button", function () {
    update("increase", item.id);
  });
  plus.title = "One more";
  quantity.appendChild(minus);
  quantity.appendChild(count);
  quantity.appendChild(plus);

  const subtotal = document.createElement("td");
  subtotal.textContent = money(item.subtotal);

  const remove = document.createElement("td");
  remove.appendChild(
    makeButton("Delete", "delete-button", function () {
      update("remove", item.id);
    })
  );

  [title, price, quantity, subtotal, remove].forEach(function (cell) {
    row.appendChild(cell);
  });
  return row;
}

function showCart(cart) {
  body.replaceChildren();
  if (cart.items.length === 0) {
    table.hidden = true;
    summary.hidden = true;
    showMessage("Your shopping cart is empty.", false);
    return;
  }
  cart.items.forEach(function (item) {
    body.appendChild(makeRow(item));
  });
  totalCell.textContent = money(cart.total);
  statusBox.hidden = true;
  table.hidden = false;
  summary.hidden = false;
}

payButton.addEventListener("click", function () {
  window.location.href = "payment.html";
});

async function loadCart() {
  try {
    const response = await fetch("api/cart");
    const cart = await response.json().catch(function () {
      return null;
    });
    if (!response.ok || cart == null) {
      throw new Error(
        cart && cart.error
          ? cart.error
          : "the server answered " + response.status
      );
    }
    showCart(cart);
  } catch (err) {
    showMessage("Could not load the cart: " + err.message, true);
  }
}

loadCart();
