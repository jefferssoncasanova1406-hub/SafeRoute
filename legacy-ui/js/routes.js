document.addEventListener("DOMContentLoaded", () => {
  const savedRoutesList = document.getElementById("savedRoutesList");
  const routeHistoryList = document.getElementById("routeHistoryList");

  if (!savedRoutesList || !routeHistoryList) return;

  const storedRoutes = safeParseStorage("saferoute_saved_routes", []);

  const defaultRoutes = [
    {
      name: "Ruta iluminada al centro",
      origin: "Plaza San Martín",
      destination: "Parque de la Exposición",
      security: "82/100",
      distance: "1.4 km",
      time: "18 min",
      risk: "Riesgo Bajo",
      createdAt: "16 abr. 2026, 8:00 a. m."
    },
    {
      name: "Ruta comercial alternativa",
      origin: "Plaza San Martín",
      destination: "Parque de la Exposición",
      security: "58/100",
      distance: "1.2 km",
      time: "15 min",
      risk: "Riesgo Medio",
      createdAt: "15 abr. 2026, 7:30 p. m."
    }
  ];

  const allRoutes = [...storedRoutes, ...defaultRoutes];

  savedRoutesList.innerHTML = allRoutes.map((route) => `
    <article class="saved-route-card">
      <div class="saved-route-top">
        <h3>${route.name}</h3>
        <span class="${route.risk === "Riesgo Medio" ? "risk-medium" : "risk-low"}">
          ${route.risk}
        </span>
      </div>

      <p>${route.origin} → ${route.destination}</p>

      <div class="route-stats">
        <div>
          <span>SEGURIDAD</span>
          <strong>${route.security}</strong>
        </div>

        <div>
          <span>DISTANCIA</span>
          <strong>${route.distance}</strong>
        </div>

        <div>
          <span>TIEMPO ESTIMADO</span>
          <strong>${route.time}</strong>
        </div>
      </div>

      <a href="route-detail.html">Ver detalle</a>
    </article>
  `).join("");

  routeHistoryList.innerHTML = allRoutes.map((route) => `
    <article>
      <h3>${route.name}</h3>
      <p>${route.origin} → ${route.destination}</p>
      <small>${route.createdAt}</small>
    </article>
  `).join("");
});