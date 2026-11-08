// script for confirmation.html
// shows the order that was just placed (the server keeps it in the session)

const statusBox = document.getElementById("status");

function cell(text) {
    const td = document.createElement("td");
    td.textContent = text;
    return td;
}

async function loadOrder() {
    try {
        const response = await fetch("api/place-order");
        if (response.status === 404) {
            statusBox.textContent = "There is no recent order to show.";
            return;
        }
        if (!response.ok) {
            throw new Error("the server answered " + response.status);
        }
        const order = await response.json();

        const body = document.getElementById("order-body");
        order.items.forEach(function (item) {
            const row = document.createElement("tr");
            row.appendChild(cell(item.title));
            row.appendChild(cell(item.quantity));
            row.appendChild(cell(money(item.price)));
            row.appendChild(cell(money(item.subtotal)));
            body.appendChild(row);
        });
        document.getElementById("order-total").textContent = money(order.total);
        document.getElementById("sale-ids").textContent = order.saleIds.join(", ");

        statusBox.hidden = true;
        document.getElementById("order-details").hidden = false;
    } catch (err) {
        statusBox.textContent = "Could not load the order: " + err.message;
        statusBox.classList.add("error");
    }
}

loadOrder();