# Build the React app, then serve the static files with nginx.
FROM node:20-alpine AS build
WORKDIR /app
# Vite bakes these in at build time. Cloud sets auth mode to keycloak.
ARG VITE_API_BASE_URL=
ARG VITE_AUTH_MODE=jwt
ARG VITE_KEYCLOAK_URL=
ARG VITE_KEYCLOAK_REALM=noor-bank
ARG VITE_KEYCLOAK_CLIENT_ID=noor-bank-frontend
ENV VITE_API_BASE_URL=$VITE_API_BASE_URL \
    VITE_AUTH_MODE=$VITE_AUTH_MODE \
    VITE_KEYCLOAK_URL=$VITE_KEYCLOAK_URL \
    VITE_KEYCLOAK_REALM=$VITE_KEYCLOAK_REALM \
    VITE_KEYCLOAK_CLIENT_ID=$VITE_KEYCLOAK_CLIENT_ID
COPY package*.json ./
RUN npm ci
COPY . .
RUN npm run build

FROM nginx:alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html
EXPOSE 80
