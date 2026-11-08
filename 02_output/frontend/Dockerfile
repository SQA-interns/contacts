# Frontend image (ADR-15): static files behind nginx, non-root, one image for every environment.
FROM node:22.20.0-bookworm-slim AS build
WORKDIR /build
COPY package.json package-lock.json .npmrc ./
RUN npm ci
COPY . .
RUN npm run build

FROM nginx:1.29.2-alpine
COPY nginx.conf /etc/nginx/nginx.conf
COPY docker-entrypoint.sh /docker-entrypoint-app.sh
COPY --from=build /build/dist /usr/share/nginx/html
RUN chmod 0555 /docker-entrypoint-app.sh && chown -R nginx:nginx /usr/share/nginx/html
USER nginx
EXPOSE 8080
HEALTHCHECK --interval=15s --timeout=3s --start-period=5s --retries=5 \
  CMD wget -q -O /dev/null http://127.0.0.1:8080/healthz || exit 1
ENTRYPOINT ["/docker-entrypoint-app.sh"]
CMD ["nginx", "-g", "daemon off;"]
