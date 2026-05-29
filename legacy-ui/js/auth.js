document.addEventListener("DOMContentLoaded", () => {
  const loginForm = document.getElementById("loginForm");
  const registerForm = document.getElementById("registerForm");
  const logoutBtn = document.getElementById("logoutBtn");

  if (loginForm) {
    loginForm.addEventListener("submit", (event) => {
      event.preventDefault();

      const email = document.getElementById("loginEmail").value.trim();
      const password = document.getElementById("loginPassword").value.trim();

      const user = getUsers().find(
        (item) => item.email === email && item.password === password
      );

      if (!user) {
        alert("Credenciales incorrectas.");
        return;
      }

      setSession(user);
      window.location.href = "dashboard.html";
    });
  }

  if (registerForm) {
    registerForm.addEventListener("submit", (event) => {
      event.preventDefault();

      const name = document.getElementById("registerName").value.trim();
      const email = document.getElementById("registerEmail").value.trim();
      const password = document.getElementById("registerPassword").value.trim();
      const password2 = document.getElementById("registerPassword2").value.trim();

      if (password.length < 6) {
        alert("La contraseña debe tener mínimo 6 caracteres.");
        return;
      }

      if (password !== password2) {
        alert("Las contraseñas no coinciden.");
        return;
      }

      const exists = getUsers().some((user) => user.email === email);

      if (exists) {
        alert("Este correo ya está registrado.");
        return;
      }

      const newUser = {
        name,
        email,
        password,
        role: "usuario",
        notifications: 0,
        routes: 0,
        reports: 0
      };

      saveUser(newUser);
      setSession(newUser);
      window.location.href = "dashboard.html";
    });
  }

  if (document.body.classList.contains("dashboard-page")) {
    const session = getSession();

    if (!session) {
      window.location.href = "login.html";
      return;
    }

    const sidebarName = document.getElementById("sidebarName");

    if (sidebarName) {
      sidebarName.textContent = session.name;
    }

    const welcomeText = document.getElementById("welcomeText");

    if (welcomeText) {
      welcomeText.textContent =
      `Hola, ${session.name}. Esta vista resume la simulación del modelo Usuario, Perfil, Reporte, Incidente, Ruta y Notificaciones.`;
    }

    const statNotifications = document.getElementById("statNotifications");

    if (statNotifications) {
      statNotifications.textContent = session.notifications;
    }

    const statRoutes = document.getElementById("statRoutes");

    if (statRoutes) {
      const storedRoutes = safeParseStorage("saferoute_saved_routes", []);
      const defaultRoutesCount = 2;
      const totalRoutes = defaultRoutesCount + storedRoutes.length;

      statRoutes.textContent = totalRoutes;

      session.routes = totalRoutes;
      setSession(session);
    }

    const statReports = document.getElementById("statReports");

    if (statReports) {
      const storedReports = safeParseStorage("saferoute_reports", []);
      const defaultReportsCount = 3;
      const totalReports = defaultReportsCount + storedReports.length;

      statReports.textContent = totalReports;

      session.reports = totalReports;
      setSession(session);
    }

    const badge = document.getElementById("notifBadge");

    if (badge) {
      const storedAlerts = safeParseStorage("saferoute_alerts", []);

      const pendingCount = storedAlerts.filter((alert) => {
        return alert.status === "pendiente";
      }).length;

      if (pendingCount > 0) {
        badge.textContent = pendingCount;
        badge.style.display = "inline-flex";
      } else {
        badge.textContent = "";
        badge.style.display = "none";
      }

      session.notifications = pendingCount;
      setSession(session);
    }

    const profileConfig = safeParseStorage("saferoute_profile_config", null);

    if (profileConfig) {
      const summaryRisk = document.getElementById("summaryRiskPreference");
      const summaryRadius = document.getElementById("summaryAlertRadius");
      const summaryNotifications = document.getElementById("summaryNotificationsStatus");

      if (summaryRisk) {
        summaryRisk.textContent = profileConfig.riskPreference;
      }

      if (summaryRadius) {
        summaryRadius.textContent = profileConfig.alertRadius + " m";
      }

      if (summaryNotifications) {
        summaryNotifications.textContent = profileConfig.notificationsEnabled
        ? "Activadas"
        : "Desactivadas";
      }
    }
  }
  

  if (logoutBtn) {
    logoutBtn.addEventListener("click", () => {
      clearSession();
    });
  }
});