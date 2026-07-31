package ru.sultanyarov.configurator.contract

class TestResponse {
    final int status
    final String body
    final Map<String, String> headers

    TestResponse(int status, String body) {
        this(status, body, [:])
    }

    TestResponse(int status, String body, Map<String, String> headers) {
        this.status = status
        this.body = body
        this.headers = headers
    }
}
