package ru.sultanyarov.configurator.contract

class TestResponse {
    final int status
    final String body

    TestResponse(int status, String body) {
        this.status = status
        this.body = body
    }
}
