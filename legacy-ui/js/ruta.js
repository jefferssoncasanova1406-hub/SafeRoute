const demoAddresses = [
  {
    address: "Av. Arequipa 2450, Lince, Lima",
    district: "Lince",
    city: "Lima",
    lat: -12.0852,
    lng: -77.0336
  },
  {
    address: "Av. Larco 345, Miraflores, Lima",
    district: "Miraflores",
    city: "Lima",
    lat: -12.1219,
    lng: -77.0297
  },
  {
    address: "Av. Javier Prado Este 4200, San Borja, Lima",
    district: "San Borja",
    city: "Lima",
    lat: -12.0937,
    lng: -76.9942
  },
  {
    address: "Av. Brasil 1980, Jesús María, Lima",
    district: "Jesús María",
    city: "Lima",
    lat: -12.0748,
    lng: -77.0509
  },
  {
    address: "Av. Salaverry 3100, San Isidro, Lima",
    district: "San Isidro",
    city: "Lima",
    lat: -12.0977,
    lng: -77.0535
  },
  {
    address: "Jirón de la Unión 870, Cercado de Lima, Lima",
    district: "Cercado de Lima",
    city: "Lima",
    lat: -12.0464,
    lng: -77.0329
  },
  {
    address: "Parque Kennedy, Miraflores, Lima",
    district: "Miraflores",
    city: "Lima",
    lat: -12.1215,
    lng: -77.0305
  },
  {
    address: "Plaza San Martín, Cercado de Lima, Lima",
    district: "Cercado de Lima",
    city: "Lima",
    lat: -12.0515,
    lng: -77.0346
  },
  {
    address: "Parque de la Exposición, Cercado de Lima, Lima",
    district: "Cercado de Lima",
    city: "Lima",
    lat: -12.0601,
    lng: -77.0371
  }
];

document.addEventListener("DOMContentLoaded", () => {
  const originInput = document.getElementById("routeOrigin");
  const destinationInput = document.getElementById("routeDestination");

  setupAutocomplete(originInput);
  setupAutocomplete(destinationInput);
});

function setupAutocomplete(input) {
  if (!input) return;

  const wrapper = input.closest(".route-field");
  wrapper.classList.add("autocomplete-wrapper");

  const list = document.createElement("div");
  list.className = "autocomplete-list";
  wrapper.appendChild(list);

  input.addEventListener("focus", () => {
    renderOptions(list, input, demoAddresses);
  });

  input.addEventListener("input", () => {
    const search = input.value.toLowerCase().trim();

    const filtered = demoAddresses.filter((item) => {
      return (
        item.address.toLowerCase().includes(search) ||
        item.district.toLowerCase().includes(search) ||
        item.city.toLowerCase().includes(search)
      );
    });

    renderOptions(list, input, filtered);
  });

  document.addEventListener("click", (event) => {
    if (!wrapper.contains(event.target)) {
      list.innerHTML = "";
      list.classList.remove("show");
    }
  });
}

function renderOptions(list, input, addresses) {
  list.innerHTML = "";

  if (!addresses.length) {
    list.innerHTML = `<div class="autocomplete-empty">No se encontraron coincidencias</div>`;
    list.classList.add("show");
    return;
  }

  addresses.forEach((item) => {
    const option = document.createElement("button");
    option.type = "button";
    option.className = "autocomplete-item";

    option.innerHTML = `
      <strong>${item.address}</strong>
      <span>${item.district}, ${item.city}</span>
    `;

    option.addEventListener("click", () => {
      input.value = item.address;
      input.dataset.lat = item.lat;
      input.dataset.lng = item.lng;
      input.dataset.district = item.district;
      input.dataset.city = item.city;

      list.innerHTML = "";
      list.classList.remove("show");
    });

    list.appendChild(option);
  });

  list.classList.add("show");
}
document.addEventListener("DOMContentLoaded", () => {
  const simulateBtn = document.querySelector(".simulate-route-btn");
  const results = document.getElementById("routeResults");
  const originInput = document.getElementById("routeOrigin");
  const destinationInput = document.getElementById("routeDestination");

  const routeCards = document.querySelectorAll(".route-option");

  const routeData = [
    {
      title: "Ruta recomendada por avenidas",
      distance: "4.8 km",
      time: "60 min",
      security: "92/100",
      risk: "Bajo",
      zones: "0"
    },
    {
      title: "Ruta comercial intermedia",
      distance: "4.3 km",
      time: "54 min",
      security: "87/100",
      risk: "Bajo",
      zones: "0"
    },
    {
      title: "Ruta corta con mayor exposición",
      distance: "3.9 km",
      time: "49 min",
      security: "77/100",
      risk: "Bajo",
      zones: "0"
    }
  ];

  function updateSelectedRoute(index) {
  
    const selectedRoute = routeData[index];
 
    routeCards.forEach((card) => {
    card.classList.remove("active");
    });

    routeCards[index].classList.add("active");

    const globalLevel = document.querySelector(".summary-box strong");
    const riskValue = document.querySelectorAll(".summary-box strong")[1];
    const zonesValue = document.querySelectorAll(".summary-box strong")[2];
    const previewTitle = document.querySelector(".route-preview-info h3");
  
    if (globalLevel) globalLevel.textContent = selectedRoute.security;
    if (riskValue) riskValue.textContent = selectedRoute.risk;
    if (zonesValue) zonesValue.textContent = selectedRoute.zones;
    if (previewTitle) previewTitle.textContent = selectedRoute.title;
  }

  routeCards.forEach((card, index) => {
    card.style.cursor = "pointer";

    card.addEventListener("click", () => {
      updateSelectedRoute(index);
    });
  });

  if (!simulateBtn || !results) return;

  simulateBtn.addEventListener("click", () => {
    const origin = originInput.value.trim();
    const destination = destinationInput.value.trim();

    if (!origin || !destination) {
      alert("Selecciona un origen y un destino para simular la ruta.");
      return;
    }

    document.getElementById("previewOrigin").textContent = origin;
    document.getElementById("previewDestination").textContent = destination;

    results.classList.remove("hidden");

    updateSelectedRoute(0);

    results.scrollIntoView({
      behavior: "smooth",
      block: "nearest"
    });
  });
});

const saveAndStartBtn = document.getElementById("saveAndStartBtn");

if (saveAndStartBtn) {
  saveAndStartBtn.addEventListener("click", () => {
    const storedRoutes = safeParseStorage("saferoute_saved_routes", []);

    const newRoute = {
      id: Date.now(),
      name: "Ruta recomendada por avenidas",
      origin: "Av. Arequipa 2450, Lince, Lima",
      destination: "Av. Javier Prado Este 4200, San Borja, Lima",
      security: "82/100",
      distance: "5.1 km",
      time: "64 min",
      risk: "Riesgo Bajo",
      createdAt: new Date().toLocaleString()
    };

    storedRoutes.push(newRoute);

    localStorage.setItem("saferoute_saved_routes", JSON.stringify(storedRoutes));

    // 🔥 REDIRECCIÓN
    window.location.href = `route-detail.html?routeId=${newRoute.id}`;
  });
}