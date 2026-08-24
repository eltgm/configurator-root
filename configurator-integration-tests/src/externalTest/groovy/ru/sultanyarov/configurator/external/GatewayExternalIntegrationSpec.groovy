package ru.sultanyarov.configurator.external

import io.restassured.RestAssured
import io.restassured.response.Response
import spock.lang.Specification

class GatewayExternalIntegrationSpec extends Specification {
    private static final String CSP = "default-src 'self'; base-uri 'self'; object-src 'none'; frame-ancestors 'none'; " +
            "form-action 'self'; connect-src 'self'; img-src 'self' data: blob:; font-src 'self'; " +
            "style-src 'self' 'unsafe-inline'; script-src 'self'"

    final String gatewayOrigin = ExternalTestEnvironment.gatewayOrigin()

    def "serves the SPA root and nested routes with no-cache security headers"() {
        when:
        def root = get("/")
        def deepLink = get("/settings/compatibility/graph")

        then:
        assertSpaResponse(root)
        assertSpaResponse(deepLink)
    }

    def "serves a built asset with immutable caching"() {
        given:
        def root = get("/")
        def matcher = root.asString() =~ /(?:src|href)="(\/assets\/[^"]+)"/

        expect:
        matcher.find()

        when:
        def asset = get(matcher.group(1))

        then:
        asset.statusCode() == 200
        asset.body().asByteArray().length > 0
        asset.header("Cache-Control") == "public, max-age=31536000, immutable"
        assertSecurityHeaders(asset)
    }

    def "keeps API responses outside the SPA fallback"() {
        when:
        def apiDocs = get("/api/v3/api-docs")
        def missingApi = get("/api/gateway-contract-missing")
        def apiRoot = get("/api")

        then:
        apiDocs.statusCode() == 200
        apiDocs.contentType().startsWith("application/json")
        apiDocs.asString().contains('"openapi"')
        assertSecurityHeaders(apiDocs)

        missingApi.statusCode() >= 400
        missingApi.statusCode() < 600
        !missingApi.asString().contains('<div id="root"></div>')
        assertSecurityHeaders(missingApi)

        apiRoot.statusCode() == 404
        apiRoot.contentType().startsWith("application/json")
        apiRoot.asString().contains('"code":"NOT_FOUND"')
        assertSecurityHeaders(apiRoot)
    }

    def "reports gateway liveness without claiming backend readiness"() {
        when:
        def response = get("/healthz")

        then:
        response.statusCode() == 200
        response.contentType().startsWith("application/json")
        response.header("Cache-Control") == "no-store"
        response.jsonPath().getString("status") == "UP"
        assertSecurityHeaders(response)
    }

    private Response get(String path) {
        return RestAssured.given()
                .baseUri(gatewayOrigin)
                .when()
                .get(path)
                .then()
                .extract()
                .response()
    }

    private static void assertSpaResponse(Response response) {
        assert response.statusCode() == 200
        assert response.contentType().startsWith("text/html")
        assert response.header("Cache-Control") == "no-cache"
        assert response.asString().contains('<div id="root"></div>')
        assertSecurityHeaders(response)
    }

    private static void assertSecurityHeaders(Response response) {
        assert response.header("X-Content-Type-Options") == "nosniff"
        assert response.header("X-Frame-Options") == "DENY"
        assert response.header("Referrer-Policy") == "no-referrer"
        assert response.header("Permissions-Policy") ==
                "camera=(), geolocation=(), microphone=(), payment=(), usb=()"
        assert response.header("Content-Security-Policy") == CSP
    }
}
