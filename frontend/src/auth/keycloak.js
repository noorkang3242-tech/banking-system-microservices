import Keycloak from 'keycloak-js'

// "jwt" = custom auth-service login (local default); "keycloak" = OIDC redirect (cloud).
export const AUTH_MODE = import.meta.env.VITE_AUTH_MODE || 'jwt'

let keycloak = null

// Lazily create the singleton Keycloak instance (only in keycloak mode).
export function getKeycloak() {
  if (!keycloak && AUTH_MODE === 'keycloak') {
    keycloak = new Keycloak({
      url: import.meta.env.VITE_KEYCLOAK_URL || 'http://localhost:8180/auth',
      realm: import.meta.env.VITE_KEYCLOAK_REALM || 'noor-bank',
      clientId: import.meta.env.VITE_KEYCLOAK_CLIENT_ID || 'noor-bank-frontend',
    })
  }
  return keycloak
}

// Map Keycloak realm roles to the single role the app/services use.
export function roleFromToken(parsed) {
  const roles = (parsed && parsed.realm_access && parsed.realm_access.roles) || []
  if (roles.includes('ADMIN')) return 'ADMIN'
  if (roles.includes('MANAGER')) return 'MANAGER'
  return 'CUSTOMER'
}
