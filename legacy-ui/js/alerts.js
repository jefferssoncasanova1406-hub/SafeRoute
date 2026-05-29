const baseAlerts = [
  {
    id: 1,
    title: "Robo reportado",
    message: "Nuevo incidente validado cerca de San Isidro dentro de tu radio de alerta.",
    date: "17 abr. 2026, 9:15 a. m.",
    status: "pendiente",
    level: "Bajo",
    description: "Se reportó un arrebato en el paradero. Se recomienda evitar la esquina durante la hora punta.",
    location: "Cruce Av. Javier Prado con Arequipa",
    district: "San Isidro, Lima",
    register: "17 abr. 2026, 9:10 a. m.",
    registerText: "Reporte ciudadano validado"
  },
  {
    id: 2,
    title: "Iluminación defectuosa",
    message: "Iluminación defectuosa registrada en Miraflores. Revisa rutas alternativas.",
    date: "17 abr. 2026, 7:50 a. m.",
    status: "leida",
    level: "Bajo",
    description: "Faroles apagados en la zona central del parque, lo que reduce la visibilidad peatonal.",
    location: "Parque Kennedy",
    district: "Miraflores, Lima",
    register: "17 abr. 2026, 7:45 a. m.",
    registerText: "Reporte ciudadano validado"
  },
  {
    id: 3,
    title: "Actividad sospechosa",
    message: "Actividad sospechosa detectada en el centro. Mantén precaución.",
    date: "16 abr. 2026, 9:20 p. m.",
    status: "pendiente",
    level: "Bajo",
    description: "Se detectó presencia sospechosa cerca de comercios cerrados. Conviene transitar en grupo.",
    location: "Cercado de Lima",
    district: "Lima, Perú",
    register: "16 abr. 2026, 9:15 p. m.",
    registerText: "Reporte ciudadano validado"
  }
];

document.addEventListener("DOMContentLoaded", () => {
  initAlerts();
});

function initAlerts() {
  if (!localStorage.getItem("saferoute_alerts")) {
    localStorage.setItem("saferoute_alerts", JSON.stringify(baseAlerts));
  }

  renderAlerts();
  renderAlertDetail(getAlerts()[0]);
  updateBadge();
}

function getAlerts() {
  return safeParseStorage("saferoute_alerts", []);
}

function saveAlerts(alerts) {
  localStorage.setItem("saferoute_alerts", JSON.stringify(alerts));
}

function renderAlerts() {
  const list = document.getElementById("alertsList");
  const markAllBtn = document.getElementById("markAllBtn");

  if (!list) return;

  const alerts = getAlerts();

  list.innerHTML = alerts.map((alert, index) => `
    <article class="alert-item ${index === 0 ? "active" : ""}" data-id="${alert.id}">
      <div>
        <h3>${alert.message}</h3>
        <small>${alert.date}</small>
      </div>
      ${alert.status === "pendiente" ? "<span></span>" : ""}
    </article>
  `).join("");

  document.querySelectorAll(".alert-item").forEach((item) => {
    item.addEventListener("click", () => {
      document.querySelectorAll(".alert-item").forEach((el) => el.classList.remove("active"));
      item.classList.add("active");

      const selected = getAlerts().find((alert) => alert.id === Number(item.dataset.id));
      renderAlertDetail(selected);
    });
  });

  if (markAllBtn) {
    markAllBtn.addEventListener("click", markAllAsRead);
  }

  if (window.lucide) lucide.createIcons();
}

function renderAlertDetail(alert) {
  const detail = document.getElementById("alertDetail");
  if (!detail || !alert) return;

  detail.innerHTML = `
    <article class="alert-detail-card">
      <header>
        <span>NOTIFICACIÓN SELECCIONADA</span>
        <h2>${alert.title}</h2>
        <p>${alert.message}</p>
      </header>

      <div class="alert-detail-body">
        <div class="alert-stats">
          <div>
            <span>ESTADO</span>
            <strong>${alert.status === "pendiente" ? "Pendiente" : "Leída"}</strong>
          </div>

          <div>
            <span>NIVEL ASOCIADO</span>
            <strong>${alert.level}</strong>
          </div>

          <div>
            <span>FECHA DE ENVÍO</span>
            <strong>${alert.date}</strong>
          </div>
        </div>

        <section class="alert-description">
          <h3>Descripción del incidente</h3>
          <p>${alert.description}</p>
        </section>

        <div class="alert-extra-grid">
          <div>
            <h4><i data-lucide="map-pin"></i> UBICACIÓN</h4>
            <p>${alert.location}</p>
            <small>${alert.district}</small>
          </div>

          <div>
            <h4><i data-lucide="clock"></i> REGISTRO</h4>
            <p>${alert.register}</p>
            <small>${alert.registerText}</small>
          </div>
        </div>

        ${
          alert.status === "pendiente"
            ? `<button type="button" class="mark-read-btn" id="markReadBtn">
                <i data-lucide="check-circle"></i>
                Marcar como leída
              </button>`
            : ""
        }
      </div>
    </article>
  `;

  const markReadBtn = document.getElementById("markReadBtn");

  if (markReadBtn) {
    markReadBtn.addEventListener("click", () => {
      markAlertAsRead(alert.id);
    });
  }

  if (window.lucide) lucide.createIcons();
}

function markAlertAsRead(id) {
  const alerts = getAlerts().map((alert) => {
    if (alert.id === id) {
      return { ...alert, status: "leida" };
    }

    return alert;
  });

  saveAlerts(alerts);

  const selected = alerts.find((alert) => alert.id === id);

  renderAlerts();
  renderAlertDetail(selected);
  updateBadge();
}

function markAllAsRead() {
  const alerts = getAlerts().map((alert) => ({
    ...alert,
    status: "leida"
  }));

  saveAlerts(alerts);

  renderAlerts();
  renderAlertDetail(alerts[0]);
  updateBadge();
}

function updateBadge() {
  const pendingCount = getAlerts().filter((alert) => alert.status === "pendiente").length;
  const badge = document.getElementById("notifBadge");

  if (badge) {
    if (pendingCount > 0) {
      badge.textContent = pendingCount;
      badge.style.display = "inline-flex";
    } else {
      badge.style.display = "none";
    }
  }

  const session = getSession();

  if (session) {
    session.notifications = pendingCount;
    setSession(session);
  }
}