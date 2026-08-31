FROM node:24.20.0-alpine3.23@sha256:0388af2af070cd4736a1567cfed02469ba117848845b4165d87a333edb53d2ca AS build

WORKDIR /workspace

COPY package.json package-lock.json ./
RUN npm ci

COPY . ./
RUN npm run build

FROM nginxinc/nginx-unprivileged:1.31.3-alpine3.24@sha256:f972e5322b9797dc2a6b830030094426437b1ae7032e4644496395336ac6fdac

ARG OCI_CREATED=""
ARG OCI_REVISION="unknown"
ARG OCI_VERSION="0.0.0-dev"

LABEL org.opencontainers.image.title="Configurator Web Gateway" \
      org.opencontainers.image.description="Unprivileged NGINX gateway for the Configurator SPA and API" \
      org.opencontainers.image.source="https://github.com/eltgm/configurator-root" \
      org.opencontainers.image.revision="${OCI_REVISION}" \
      org.opencontainers.image.version="${OCI_VERSION}" \
      org.opencontainers.image.created="${OCI_CREATED}" \
      org.opencontainers.image.licenses="MIT"

COPY --chown=nginx:nginx nginx/default.conf /etc/nginx/conf.d/default.conf
COPY --from=build --chown=nginx:nginx /workspace/dist/ /usr/share/nginx/html/

EXPOSE 8080

HEALTHCHECK --interval=10s --timeout=3s --start-period=5s --retries=5 \
  CMD wget --quiet --tries=1 --spider http://127.0.0.1:8080/healthz || exit 1
