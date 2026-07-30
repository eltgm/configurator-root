package ru.sultanyarov.configurator.contract

import com.fasterxml.jackson.databind.ObjectMapper

interface ApiTestSupport {
    ObjectMapper getObjectMapper()

    void runSqlScripts(String... scripts)

    TestResponse get(String path)

    TestResponse get(String path, Map<String, ?> queryParams)

    TestResponse post(String path, Object body)

    TestResponse put(String path, Object body)

    TestResponse delete(String path)

    default TestResponse postMultipart(
            String path,
            String filename,
            String contentType,
            byte[] content,
            Integer orderIndex
    ) {
        throw new UnsupportedOperationException("Multipart requests are not supported by this transport")
    }
}
