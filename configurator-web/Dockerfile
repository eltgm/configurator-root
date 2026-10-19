FROM node:24.13.0-alpine3.23@sha256:cd6fb7efa6490f039f3471a189214d5f548c11df1ff9e5b181aa49e22c14383e AS build

WORKDIR /workspace

COPY package.json package-lock.json ./
RUN npm ci

COPY . ./
RUN npm run build

FROM nginxinc/nginx-unprivileged:1.31.6-alpine3.24@sha256:884e0b7f42f303641b0f4e78755aea647521dca0b89ca3be7992ad78b5c07434

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
