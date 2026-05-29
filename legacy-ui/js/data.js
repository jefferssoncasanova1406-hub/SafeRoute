const demoUsers = [
  {
    name: "Luis Caminante",
    email: "luis@saferoute.pe",
    password: "123456",
    role: "usuario",
    notifications: 2,
    routes: 2,
    reports: 3
  },
  {
    name: "Administrador",
    email: "admin@saferoute.pe",
    password: "admin123",
    role: "admin",
    notifications: 4,
    routes: 0,
    reports: 8
  }
];

function getUsers() {
  const storedUsers = safeParseStorage("saferoute_users", []);
  return [...demoUsers, ...storedUsers];
}

function saveUser(user) {
  const storedUsers = safeParseStorage("saferoute_users", []);
  storedUsers.push(user);
  localStorage.setItem("saferoute_users", JSON.stringify(storedUsers));
}

function setSession(user) {
  const session = {
    name: user.name,
    email: user.email,
    role: user.role,
    notifications: user.notifications || 0,
    routes: user.routes || 0,
    reports: user.reports || 0
  };

  localStorage.setItem("saferoute_session", JSON.stringify(session));
}

function getSession() {
  return safeParseStorage("saferoute_session", null);
}

function clearSession() {
  localStorage.removeItem("saferoute_session");
}

function safeParseStorage(key, fallback = null) {
  try {
    return JSON.parse(localStorage.getItem(key)) || fallback;
  } catch (error) {
    return fallback;
  }
}