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
}
