package ru.sultanyarov.configurator.external

final class ExternalTestEnvironment {
    static final String DEFAULT_GATEWAY_ORIGIN = "http://127.0.0.1:8080"
    static final String DEFAULT_API_BASE_URL = DEFAULT_GATEWAY_ORIGIN + "/api"

    private ExternalTestEnvironment() {
    }

    static String apiBaseUrl() {
        return System.getProperty("test.baseUrl", DEFAULT_API_BASE_URL)
    }

    static String gatewayOrigin() {
        return System.getProperty("test.gatewayUrl", DEFAULT_GATEWAY_ORIGIN)
    }
}
