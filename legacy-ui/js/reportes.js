const incidentTypes = [
  "Robo / Arrebato",
  "Actividad sospechosa",
  "Iluminación defectuosa",
  "Vía bloqueada / accidente",
  "Acoso callejero"
];

const reportAddresses = [
  {
    address: "Av. Arequipa 2450, Lince, Lima",
    district: "Lince, Lima"
  },
  {
    address: "Av. Larco 345, Miraflores, Lima",
    district: "Miraflores, Lima"
  },
  {
    address: "Av. Javier Prado Este 4200, San Borja, Lima",
    district: "San Borja, Lima"
  },
  {
    address: "Parque Kennedy",
    district: "Miraflores, Lima"
  },
  {
    address: "Av. Grau Cdra 5",
    district: "Cercado de Lima, Lima"
  }
];

const baseReports = [
  {
    status: "validado",
    title: "Robo reportado",
    location: "Cruce Av. Javier Prado con Arequipa",
    risk: "Riesgo Bajo",
    date: "17 abr. 2026, 9:10 a. m.",
    createdAt: "17 abr. 2026, 9:00 a. m.",
    description: "Arrebato en paradero con dos personas huyendo hacia Arequipa."
  },
  {
    status: "validado",
    title: "Iluminación defectuosa",
    location: "Parque Kennedy",
    risk: "Riesgo Bajo",
    date: "17 abr. 2026, 7:45 a. m.",
    createdAt: "17 abr. 2026, 7:30 a. m.",
    description: "Parque con alumbrado apagado en el lado norte."
  },
  {
    status: "rechazado",
    title: "Vía bloqueada",
    location: "Av. Grau Cdra 5",
    risk: "Riesgo Alto",
    date: "16 abr. 2026, 6:30 p. m.",
    createdAt: "16 abr. 2026, 6:10 p. m.",
    description: "Cruce peatonal bloqueado por choque. Tránsito desordenado."
  }
];

document.addEventListener("DOMContentLoaded", () => {
  createReportModal();
  setupReportButtons();
  renderReports();
});

function getStoredReports() {
  return safeParseStorage("saferoute_reports", []);
}

function saveReport(report) {
  const reports = getStoredReports();
  reports.unshift(report);
  localStorage.setItem("saferoute_reports", JSON.stringify(reports));

  const session = getSession();

  if (session) {
    session.reports = (session.reports || 0) + 1;
    setSession(session);
  }
}

function createReportModal() {
  if (document.getElementById("reportModal")) return;

  document.body.insertAdjacentHTML("beforeend", `
    <div class="report-modal-overlay hidden" id="reportModal">
      <section class="report-modal">
        <div class="report-modal-header">
          <h2>
            <i data-lucide="alert-triangle"></i>
            Nuevo reporte vinculado a la BD simulada
          </h2>

          <button type="button" id="closeReportModalBtn">×</button>
        </div>

        <form id="reportForm" class="report-form">
          <label>
            Tipo de incidente
            <select id="reportType" required>
              ${incidentTypes.map(type => `<option value="${type}">${type}</option>`).join("")}
            </select>
          </label>

          <label class="report-location-field">
            Ubicación del incidente
            <input 
              type="text" 
              id="reportLocation" 
              placeholder="Busca una calle o intersección" 
              autocomplete="off"
              required
            />
            <div class="report-autocomplete-list" id="reportLocationList"></div>
            <small>
              Demo navegable: las coincidencias se simulan localmente y al elegir una se cargan calle, distrito, ciudad y coordenadas.
            </small>
          </label>

          <label>
            <div class="risk-label-row">
              <span>Nivel de riesgo asociado a la zona</span>
              <strong id="riskValue">60 / Medio</strong>
            </div>

            <input type="range" id="reportRisk" min="0" max="100" value="60" />
            <small>Este valor alimenta la simulación de Zona_Riesgo para esa ubicación.</small>
          </label>

          <label>
            Descripción breve
            <textarea 
              id="reportDescription" 
              placeholder="Describe qué pasó y por qué debería tenerse en cuenta."
              required
            ></textarea>
          </label>

          <div class="report-modal-actions">
            <button type="button" id="cancelReportBtn">Cancelar</button>
            <button type="submit">Registrar reporte</button>
          </div>
        </form>
      </section>
    </div>
  `);

  setupReportModalEvents();

  if (window.lucide) {
    lucide.createIcons();
  }
}

function setupReportButtons() {
  const buttons = document.querySelectorAll("#openReportModalBtn, .create-report-btn, .dark-action-btn");

  buttons.forEach((button) => {
    button.addEventListener("click", () => {
      openReportModal();
    });
  });
}

function setupReportModalEvents() {
  const modal = document.getElementById("reportModal");
  const closeBtn = document.getElementById("closeReportModalBtn");
  const cancelBtn = document.getElementById("cancelReportBtn");
  const form = document.getElementById("reportForm");
  const riskInput = document.getElementById("reportRisk");
  const riskValue = document.getElementById("riskValue");
  const locationInput = document.getElementById("reportLocation");
  const locationList = document.getElementById("reportLocationList");

  closeBtn.addEventListener("click", closeReportModal);
  cancelBtn.addEventListener("click", closeReportModal);

  modal.addEventListener("click", (event) => {
    if (event.target === modal) closeReportModal();
  });

  riskInput.addEventListener("input", () => {
    riskValue.textContent = `${riskInput.value} / ${getRiskLevel(Number(riskInput.value))}`;
  });

  locationInput.addEventListener("focus", () => {
    renderAddressOptions(reportAddresses);
  });

  locationInput.addEventListener("input", () => {
    const search = locationInput.value.toLowerCase().trim();

    const filtered = reportAddresses.filter((item) =>
      item.address.toLowerCase().includes(search) ||
      item.district.toLowerCase().includes(search)
    );

    renderAddressOptions(filtered);
  });

  document.addEventListener("click", (event) => {
    if (!locationInput.parentElement.contains(event.target)) {
      locationList.classList.remove("show");
    }
  });

  form.addEventListener("submit", (event) => {
    event.preventDefault();

    const type = document.getElementById("reportType").value;
    const location = locationInput.value.trim();
    const riskNumber = Number(riskInput.value);
    const description = document.getElementById("reportDescription").value.trim();

    if (!location || !description) {
      alert("Completa la ubicación y la descripción del reporte.");
      return;
    }

    const report = {
      status: "pendiente",
      title: type,
      location,
      risk: `Riesgo ${getRiskLevel(riskNumber)}`,
      date: getCurrentDateText(),
      createdAt: getCurrentDateText(),
      description
    };

    saveReport(report);
    renderReports();
    closeReportModal();
    showReportToast();

    form.reset();
    riskInput.value = 60;
    riskValue.textContent = "60 / Medio";
  });

  function renderAddressOptions(addresses) {
    locationList.innerHTML = "";

    if (!addresses.length) {
      locationList.innerHTML = `<div class="report-autocomplete-empty">No se encontraron coincidencias</div>`;
      locationList.classList.add("show");
      return;
    }

    addresses.forEach((item) => {
      const option = document.createElement("button");
      option.type = "button";
      option.innerHTML = `
        <strong>${item.address}</strong>
        <span>${item.district}</span>
      `;

      option.addEventListener("click", () => {
        locationInput.value = item.address;
        locationList.classList.remove("show");
      });

      locationList.appendChild(option);
    });

    locationList.classList.add("show");
  }
}

function openReportModal() {
  document.getElementById("reportModal").classList.remove("hidden");
}

function closeReportModal() {
  document.getElementById("reportModal").classList.add("hidden");
}

function renderReports() {
  const reportsGrid = document.getElementById("reportsGrid");

  if (!reportsGrid) return;

  const reports = [...getStoredReports(), ...baseReports];

  reportsGrid.innerHTML = reports.map((report) => {
    return `
      <article class="report-card">
        <div class="report-top">
          <span class="status ${report.status}">${report.status.toUpperCase()}</span>
          <small>${report.createdAt}</small>
        </div>

        <h3>${report.title}</h3>

        <p class="report-location">
          <i data-lucide="map-pin"></i>
          ${report.location}
        </p>

        <div class="report-meta">
          <span>${report.risk}</span>
          <span>
            <i data-lucide="clock"></i>
            ${report.date}
          </span>
        </div>

        <p class="report-description">${report.description}</p>
      </article>
    `;
  }).join("");

  if (window.lucide) {
    lucide.createIcons();
  }
}

function getRiskLevel(value) {
  if (value >= 75) return "Alto";
  if (value >= 40) return "Medio";
  return "Bajo";
}

function getCurrentDateText() {
  const now = new Date();

  return now.toLocaleString("es-PE", {
    day: "2-digit",
    month: "short",
    year: "numeric",
    hour: "numeric",
    minute: "2-digit"
  });
}

function showReportToast() {
  const toast = document.createElement("div");
  toast.className = "report-toast";
  toast.innerHTML = `
    <i data-lucide="check-circle"></i>
    Reporte registrado. Quedó pendiente de validación en la simulación.
  `;

  document.body.appendChild(toast);

  if (window.lucide) {
    lucide.createIcons();
  }

  setTimeout(() => {
    toast.remove();
  }, 3500);
}