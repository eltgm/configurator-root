package ru.sultanyarov.configurator.contract

class BinaryTestResponse {
    final int status
    final byte[] body
    final Map<String, String> headers

    BinaryTestResponse(int status, byte[] body, Map<String, String> headers) {
        this.status = status
        this.body = body
        this.headers = headers
    }
}
