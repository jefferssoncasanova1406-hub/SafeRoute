document.addEventListener("DOMContentLoaded", () => {
  const riskPreference = document.getElementById("riskPreference");
  const alertRadius = document.getElementById("alertRadius");
  const alertRadiusValue = document.getElementById("alertRadiusValue");
  const notificationsEnabled = document.getElementById("notificationsEnabled");
  const saveConfigBtn = document.getElementById("saveConfigBtn");
  const configMessage = document.getElementById("configMessage");

  const savedConfig = safeParseStorage("saferoute_profile_config", {
    riskPreference: "Medio",
    alertRadius: 1200,
    notificationsEnabled: true
  });

  if (riskPreference) {
    riskPreference.value = savedConfig.riskPreference;
  }

  if (alertRadius && alertRadiusValue) {
    alertRadius.value = savedConfig.alertRadius;
    alertRadiusValue.textContent = savedConfig.alertRadius;

    alertRadius.addEventListener("input", () => {
      alertRadiusValue.textContent = alertRadius.value;
    });
  }

  if (notificationsEnabled) {
    notificationsEnabled.checked = savedConfig.notificationsEnabled;
  }

  if (saveConfigBtn) {
    saveConfigBtn.addEventListener("click", () => {
      const profileConfig = {
        riskPreference: riskPreference.value,
        alertRadius: Number(alertRadius.value),
        notificationsEnabled: notificationsEnabled.checked
      };

      localStorage.setItem("saferoute_profile_config", JSON.stringify(profileConfig));

      if (configMessage) {
        configMessage.textContent = "Perfil actualizado dentro de la simulación.";
        configMessage.classList.remove("hidden");
      }
    });
  }
});