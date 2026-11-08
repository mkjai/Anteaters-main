// shows the total, then sends the payment information to the server with POST

const statusBox = document.getElementById("status");
const form = document.getElementById("payment-form");
const errorBox = document.getElementById("payment-error");
const button = document.getElementById("order-button");

function showError(text) {
    errorBox.textContent = text;
    errorBox.hidden = false;
}

async function loadTotal() {
    try {
        const response = await fetch("api/cart");
        const cart = await response.json().catch(function () {
            return null;
        });
        if (!response.ok || cart == null) {
            throw new Error(cart && cart.error ? cart.error : "the server answered " + response.status);
        }
        if (cart.items.length === 0) {
            statusBox.textContent = "Your shopping cart is empty, so there is nothing to pay for.";
            return;
        }
        document.getElementById("payment-total").textContent = money(cart.total);
        statusBox.hidden = true;
        form.hidden = false;
        document.getElementById("order-summary").hidden = false;
    } catch (err) {
        statusBox.textContent = "Could not load the cart: " + err.message;
        statusBox.classList.add("error");
    }
}

form.addEventListener("submit", async function (event) {
    event.preventDefault();          // do not let the browser send the form itself
    errorBox.hidden = true;
    button.disabled = true;          // avoid a double click placing the order twice

    try {
        const response = await fetch("api/place-order", {
            method: "POST",
            headers: {"Content-Type": "application/x-www-form-urlencoded"},
            body: new URLSearchParams({
                firstName: document.getElementById("first-name").value,
                lastName: document.getElementById("last-name").value,
                cardNumber: document.getElementById("card-number").value,
                expiration: document.getElementById("expiration").value
            })
        });
        const result = await response.json().catch(function () {
            return null;
        });

        if (response.ok && result && result.status === "success") {
            window.location.href = "confirmation.html";
            return;
        }
        // wrong payment information
        showError(result && result.message ? result.message : "Could not place the order.");
    } catch (err) {
        showError("Could not reach the server. Please try again.");
    }
    button.disabled = false;
});

loadTotal();