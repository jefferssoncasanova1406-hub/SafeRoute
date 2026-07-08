declare global {
  interface Window {
    __SAFEROUTE_CONFIG__?: Partial<AppRuntimeConfig>;
  }
}

export interface AppRuntimeConfig {
  apiBaseUrl: string;
  mapboxPublicToken: string;
}

const DEFAULT_RUNTIME_CONFIG: AppRuntimeConfig = {
  apiBaseUrl: 'http://localhost:8080',
  mapboxPublicToken:
    'pk.eyJ1IjoiZ3J1cG8zYWF3IiwiYSI6ImNtcWlrbHhhYjA3dzMycnEybHUycjA2ZjcifQ.10yJCzz9-2f5JWDldU17kg',
};

export const appRuntimeConfig: AppRuntimeConfig = {
  ...DEFAULT_RUNTIME_CONFIG,
  ...(typeof window !== 'undefined' ? window.__SAFEROUTE_CONFIG__ : {}),
};
