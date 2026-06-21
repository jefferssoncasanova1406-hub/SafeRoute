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
    '',
};

export const appRuntimeConfig: AppRuntimeConfig = {
  ...DEFAULT_RUNTIME_CONFIG,
  ...(typeof window !== 'undefined' ? window.__SAFEROUTE_CONFIG__ : {}),
};
