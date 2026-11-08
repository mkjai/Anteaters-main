// Script for login.html


const form = document.getElementById("login-form");
const errorBox = document.getElementById("login-error");
const button = document.getElementById("login-button");

function showError(text) {
    errorBox.textContent = text;
    errorBox.hidden = false;
}

form.addEventListener("submit", async function (event) {
    event.preventDefault();          // do not let the browser reload the page
    errorBox.hidden = true;
    button.disabled = true;          // avoid a double click sending two requests

    try {
        const response = await fetch("api/login", {
            method: "POST",
            headers: {"Content-Type": "application/x-www-form-urlencoded"},
            body: new URLSearchParams({
                email: document.getElementById("email").value,
                password: document.getElementById("password").value,
                "g-recaptcha-response": grecaptcha.getResponse()
            })
        });

        const result = await response.json();

        if (response.ok && result.status === "success") {
            window.location.href = "main.html";
        } else {
            showError(result.message || "Could not log in.");
        }
    } catch (err) {
        showError("Could not reach the server. Please try again.");
    } finally {
        button.disabled = false;
    }
});