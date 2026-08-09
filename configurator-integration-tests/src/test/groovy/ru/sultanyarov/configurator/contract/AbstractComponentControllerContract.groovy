package ru.sultanyarov.configurator.contract


import ru.sultanyarov.configurator.api.inbounds.rest.dto.AttributeValueInput
import ru.sultanyarov.configurator.api.inbounds.rest.dto.Component
import ru.sultanyarov.configurator.api.inbounds.rest.dto.ComponentImage
import ru.sultanyarov.configurator.api.inbounds.rest.dto.ComponentPage
import ru.sultanyarov.configurator.api.inbounds.rest.dto.CreateComponentRequest
import ru.sultanyarov.configurator.api.inbounds.rest.dto.ErrorResponse
import ru.sultanyarov.configurator.api.inbounds.rest.dto.UpdateComponentRequest
import spock.lang.Specification

abstract class AbstractComponentControllerContract extends Specification implements ApiTestSupport {

    def "should create component successfully"() {
        given:
        runSqlScripts(
                "/sql/clear-db.sql",
                "/sql/insert-test-domain.sql",
                "/sql/insert-test-component-type.sql",
                "/sql/insert-test-component-creation-attributes.sql"
        )
        def createRequest = new CreateComponentRequest()
                .componentTypeId(1L)
                .name("Keychron Q1")
                .brand("Keychron")
                .description("75% keyboard kit")
                .attributes([
                        new AttributeValueInput(101L, "ANSI"),
                        new AttributeValueInput(102L, "75%")
                ])

        when:
        def result = post("/components", createRequest)

        then:
        result.status == 201
        def responseBody = objectMapper.readValue(result.body, Component)
        responseBody.getId() != null
        responseBody.getComponentTypeId() == 1L
        responseBody.getName() == "Keychron Q1"
        responseBody.getBrand() == "Keychron"
        responseBody.getDescription() == "75% keyboard kit"
        responseBody.getArchived() == false
        responseBody.getCreatedAt() != null
        responseBody.getImages() != null
        responseBody.getImages().isEmpty()
        responseBody.getAttributes().size() == 2
        responseBody.getAttributes()*.getAttributeDefinitionId().containsAll([101L, 102L])
        responseBody.getAttributes()*.getName().containsAll(["layout", "form_factor"])
        responseBody.getAttributes()*.getLabel().containsAll(["Layout", "Form factor"])
        responseBody.getAttributes()*.getValue().containsAll(["ANSI", "75%"])
    }

    def "should return bad request when creating component with blank name"() {
        given:
        runSqlScripts("/sql/clear-db.sql", "/sql/insert-test-domain.sql", "/sql/insert-test-component-type.sql")
        def createRequest = new CreateComponentRequest()
                .componentTypeId(1L)
                .name("   ")

        when:
        def result = post("/components", createRequest)

        then:
        result.status == 400
        def errorResponse = objectMapper.readValue(result.body, ErrorResponse)
        errorResponse.getMessage() != null
    }

    def "should return not found when creating component for non-existent component type"() {
        given:
        runSqlScripts("/sql/clear-db.sql")
        def createRequest = new CreateComponentRequest()
                .componentTypeId(999999L)
                .name("Keychron Q1")

        when:
        def result = post("/components", createRequest)

        then:
        result.status == 404
    }

    def "should return conflict when creating component with duplicate name in same component type"() {
        given:
        runSqlScripts(
                "/sql/clear-db.sql",
                "/sql/insert-test-domain.sql",
                "/sql/insert-test-component-type.sql",
                "/sql/insert-test-component.sql"
        )
        def createRequest = new CreateComponentRequest()
                .componentTypeId(1L)
                .name("Keychron Q1")

        when:
        def result = post("/components", createRequest)

        then:
        result.status == 409
    }

    def "should return bad request when request contains duplicate attribute ids"() {
        given:
        runSqlScripts(
                "/sql/clear-db.sql",
                "/sql/insert-test-domain.sql",
                "/sql/insert-test-component-type.sql",
                "/sql/insert-test-component-creation-attributes.sql"
        )
        def createRequest = new CreateComponentRequest()
                .componentTypeId(1L)
                .name("Keychron Q1")
                .attributes([
                        new AttributeValueInput(101L, "ANSI"),
                        new AttributeValueInput(101L, "ISO")
                ])

        when:
        def result = post("/components", createRequest)

        then:
        result.status == 400
    }

    def "should return bad request when request contains invalid attribute value"() {
        given:
        runSqlScripts(
                "/sql/clear-db.sql",
                "/sql/insert-test-domain.sql",
                "/sql/insert-test-component-type.sql",
                "/sql/insert-test-component-creation-attributes.sql"
        )
        def createRequest = new CreateComponentRequest()
                .componentTypeId(1L)
                .name("Keychron Q1")
                .attributes([
                        new AttributeValueInput(101L, "INVALID"),
                        new AttributeValueInput(102L, "75%")
                ])

        when:
        def result = post("/components", createRequest)

        then:
        result.status == 400
    }

    def "should return bad request when required attributes are missing"() {
        given:
        runSqlScripts(
                "/sql/clear-db.sql",
                "/sql/insert-test-domain.sql",
                "/sql/insert-test-component-type.sql",
                "/sql/insert-test-component-creation-attributes.sql"
        )
        def createRequest = new CreateComponentRequest()
                .componentTypeId(1L)
                .name("Keychron Q1")
                .attributes([
                        new AttributeValueInput(101L, "ANSI")
                ])

        when:
        def result = post("/components", createRequest)

        then:
        result.status == 400
    }

    def "should return not found when request contains non-existent attribute"() {
        given:
        runSqlScripts(
                "/sql/clear-db.sql",
                "/sql/insert-test-domain.sql",
                "/sql/insert-test-component-type.sql",
                "/sql/insert-test-component-creation-attributes.sql"
        )
        def createRequest = new CreateComponentRequest()
                .componentTypeId(1L)
                .name("Keychron Q1")
                .attributes([
                        new AttributeValueInput(999999L, "ANSI"),
                        new AttributeValueInput(102L, "75%")
                ])

        when:
        def result = post("/components", createRequest)

        then:
        result.status == 400
    }

    def "should return bad request when attribute belongs to another component type"() {
        given:
        runSqlScripts(
                "/sql/clear-db.sql",
                "/sql/insert-test-domain.sql",
                "/sql/insert-test-component-type.sql",
                "/sql/insert-second-test-component-type.sql",
                "/sql/insert-test-component-creation-attributes.sql",
                "/sql/insert-second-test-attribute-definition.sql"
        )
        def createRequest = new CreateComponentRequest()
                .componentTypeId(1L)
                .name("Keychron Q1")
                .attributes([
                        new AttributeValueInput(101L, "ANSI"),
                        new AttributeValueInput(102L, "75%"),
                        new AttributeValueInput(201L, "Linear")
                ])

        when:
        def result = post("/components", createRequest)

        then:
        result.status == 400
    }

    def "should fully update component and replace attribute values while preserving images"() {
        given:
        prepareComponentUpdateData()
        def updateRequest = new UpdateComponentRequest(
                1L,
                "Keychron Q1 Pro",
                [
                        new AttributeValueInput(101L, "ISO"),
                        new AttributeValueInput(102L, "TKL")
                ]
        )
                .brand("Updated brand")
                .description("Updated description")

        when:
        def result = put("/components/1", updateRequest)

        then:
        result.status == 200
        def responseBody = objectMapper.readValue(result.body, Component)
        responseBody.getId() == 1L
        responseBody.getComponentTypeId() == 1L
        responseBody.getName() == "Keychron Q1 Pro"
        responseBody.getBrand() == "Updated brand"
        responseBody.getDescription() == "Updated description"
        responseBody.getArchived() == false
        responseBody.getCreatedAt() != null
        responseBody.getAttributes().size() == 2
        responseBody.getAttributes()*.getAttributeDefinitionId().toSet() == [101L, 102L].toSet()
        responseBody.getAttributes()*.getValue().toSet() == ["ISO", "TKL"].toSet()
        responseBody.getImages().size() == 1
        responseBody.getImages().first().getId() == 501L
        responseBody.getImages().first().getUrl() == "/component-images/501/content"
    }

    def "should clear nullable fields when updating component"() {
        given:
        prepareComponentUpdateData()
        def updateRequest = new UpdateComponentRequest(
                1L,
                "Keychron Q1",
                [
                        new AttributeValueInput(101L, "ANSI"),
                        new AttributeValueInput(102L, "75%")
                ]
        )

        when:
        def result = put("/components/1", updateRequest)

        then:
        result.status == 200
        def responseBody = objectMapper.readValue(result.body, Component)
        responseBody.getBrand() == null
        responseBody.getDescription() == null
    }

    def "should trim component name when updating"() {
        given:
        prepareComponentUpdateData()
        def updateRequest = new UpdateComponentRequest(
                1L,
                "Updated component ",
                [
                        new AttributeValueInput(101L, "ANSI"),
                        new AttributeValueInput(102L, "75%")
                ]
        )

        when:
        def result = put("/components/1", updateRequest)

        then:
        result.status == 200
        objectMapper.readValue(result.body, Component).getName() == "Updated component"
    }

    def "should return not found when updating non-existent component"() {
        given:
        prepareComponentUpdateData()
        def updateRequest = validUpdateRequest()

        expect:
        put("/components/999999", updateRequest).status == 404
    }

    def "should return bad request when changing component type"() {
        given:
        prepareComponentUpdateData()
        def updateRequest = validUpdateRequest().componentTypeId(2L)

        when:
        def result = put("/components/1", updateRequest)

        then:
        result.status == 400
        objectMapper.readValue(result.body, ErrorResponse).getMessage() != null
    }

    def "should return conflict when updated name belongs to another component of the same type"() {
        given:
        prepareComponentUpdateData()
        def updateRequest = validUpdateRequest().name("Existing component name")

        expect:
        put("/components/1", updateRequest).status == 409
    }

    def "should return bad request when update contains duplicate attribute ids"() {
        given:
        prepareComponentUpdateData()
        def updateRequest = validUpdateRequest().attributes([
                new AttributeValueInput(101L, "ANSI"),
                new AttributeValueInput(101L, "ISO"),
                new AttributeValueInput(102L, "75%")
        ])

        expect:
        put("/components/1", updateRequest).status == 400
    }

    def "should return bad request when update contains invalid attribute value"() {
        given:
        prepareComponentUpdateData()
        def updateRequest = validUpdateRequest().attributes([
                new AttributeValueInput(101L, "INVALID"),
                new AttributeValueInput(102L, "75%")
        ])

        expect:
        put("/components/1", updateRequest).status == 400
    }

    def "should return bad request when update misses required attributes"() {
        given:
        prepareComponentUpdateData()
        def updateRequest = validUpdateRequest().attributes([
                new AttributeValueInput(101L, "ANSI")
        ])

        expect:
        put("/components/1", updateRequest).status == 400
    }

    def "should return bad request when update contains attribute of another component type"() {
        given:
        prepareComponentUpdateData()
        def updateRequest = validUpdateRequest().attributes([
                new AttributeValueInput(101L, "ANSI"),
                new AttributeValueInput(102L, "75%"),
                new AttributeValueInput(201L, "Linear")
        ])

        expect:
        put("/components/1", updateRequest).status == 400
    }

    def "should return bad request when update attributes are omitted"() {
        given:
        prepareComponentUpdateData()
        def updateRequest = [
                componentTypeId: 1L,
                name: "Updated component"
        ]

        expect:
        put("/components/1", updateRequest).status == 400
    }

    def "should return bad request when updated name is blank"() {
        given:
        prepareComponentUpdateData()
        def updateRequest = validUpdateRequest().name("   ")

        expect:
        put("/components/1", updateRequest).status == 400
    }

    def "should get component by id successfully"() {
        given:
        runSqlScripts(
                "/sql/clear-db.sql",
                "/sql/insert-test-domain.sql",
                "/sql/insert-test-component-type.sql",
                "/sql/insert-test-component.sql"
        )

        when:
        def result = get("/components/1")

        then:
        result.status == 200
        def responseBody = objectMapper.readValue(result.body, Component)
        responseBody.getId() == 1L
        responseBody.getComponentTypeId() == 1L
        responseBody.getName() == "Keychron Q1"
        responseBody.getBrand() == "Keychron"
        responseBody.getDescription() == "Existing component"
        responseBody.getArchived() == false
        responseBody.getCreatedAt() != null
    }

    def "should return not found when getting non-existent component"() {
        given:
        runSqlScripts("/sql/clear-db.sql")

        when:
        def result = get("/components/999999")

        then:
        result.status == 404
        def errorResponse = objectMapper.readValue(result.body, ErrorResponse)
        errorResponse.getMessage() == "Component with id 999999 not found"
    }

    def "should archive component without deleting attributes or images"() {
        given:
        prepareComponentUpdateData()

        when:
        def deleteResult = delete("/components/1")

        then:
        deleteResult.status == 204
        deleteResult.body.isEmpty()

        when:
        def getResult = get("/components/1")

        then:
        getResult.status == 200
        def responseBody = objectMapper.readValue(getResult.body, Component)
        responseBody.getArchived()
        responseBody.getAttributes().size() == 3
        responseBody.getAttributes()*.getAttributeDefinitionId().containsAll([101L, 102L, 103L])
        responseBody.getImages().size() == 1
        responseBody.getImages().first().getId() == 501L
        responseBody.getImages().first().getUrl() == "/component-images/501/content"
    }

    def "should return no content when archiving component repeatedly"() {
        given:
        prepareComponentUpdateData()

        expect:
        delete("/components/1").status == 204
        delete("/components/1").status == 204
    }

    def "should return not found when archiving non-existent component"() {
        given:
        runSqlScripts("/sql/clear-db.sql")

        when:
        def result = delete("/components/999999")

        then:
        result.status == 404
        def errorResponse = objectMapper.readValue(result.body, ErrorResponse)
        errorResponse.getMessage() == "Component with id 999999 not found"
    }

    def "should return bad request when component id for archiving is not positive"() {
        given:
        runSqlScripts("/sql/clear-db.sql")

        expect:
        delete("/components/0").status == 400
    }

    def "should upload supported #contentType component image"() {
        given:
        prepareComponentImageData()

        when:
        def result = postMultipart(
                "/components/1/images",
                filename,
                contentType,
                content,
                3
        )

        then:
        result.status == 201
        def responseBody = objectMapper.readValue(result.body, ru.sultanyarov.configurator.api.inbounds.rest.dto.ComponentImage)
        responseBody.getId() != null
        responseBody.getOrderIndex() == 3
        responseBody.getUrl() == "/component-images/${responseBody.getId()}/content"

        and:
        def componentResult = get("/components/1")
        componentResult.status == 200
        def component = objectMapper.readValue(componentResult.body, Component)
        component.getImages().size() == 1
        component.getImages().first().getId() == responseBody.getId()
        component.getImages().first().getUrl() == responseBody.getUrl()
        component.getImages().first().getOrderIndex() == 3

        where:
        filename     | contentType  | content
        "image.jpg"  | "image/jpeg" | jpegBytes()
        "image.png"  | "image/png"  | pngBytes()
        "image.webp" | "image/webp" | webpBytes()
    }

    def "should return original uploaded component image content"() {
        given:
        prepareComponentImageData()
        def content = pngBytes()
        def uploadResult = postMultipart(
                "/components/1/images",
                "image.png",
                "image/png",
                content,
                0
        )
        def image = objectMapper.readValue(uploadResult.body, ComponentImage)

        when:
        def result = getBinary(image.getUrl())

        then:
        uploadResult.status == 201
        result.status == 200
        result.body == content
        result.headers["Content-Type"] == "image/png"
        result.headers["Content-Length"] == String.valueOf(content.length)
        result.headers["Cache-Control"] == "private, no-cache"
        result.headers["Content-Disposition"] == "inline"
    }

    def "should return image content after component is archived"() {
        given:
        prepareComponentImageData()
        def uploadResult = postMultipart(
                "/components/1/images",
                "image.webp",
                "image/webp",
                webpBytes(),
                null
        )
        def image = objectMapper.readValue(uploadResult.body, ComponentImage)
        delete("/components/1").status == 204

        expect:
        getBinary(image.getUrl()).status == 200
    }

    def "should return not found for missing component image content"() {
        given:
        runSqlScripts("/sql/clear-db.sql")

        expect:
        getBinary("/component-images/999999/content").status == 404
    }

    def "should return service unavailable when component image object is missing from storage"() {
        given:
        prepareComponentUpdateData()

        expect:
        getBinary("/component-images/501/content").status == 503
    }

    def "should return bad request for non-positive component image id"() {
        given:
        runSqlScripts("/sql/clear-db.sql")

        expect:
        getBinary("/component-images/0/content").status == 400
    }

    def "should assign next image order index when it is omitted"() {
        given:
        prepareComponentImageData()

        when:
        def firstResult = postMultipart(
                "/components/1/images",
                "first.png",
                "image/png",
                pngBytes(),
                null
        )
        def secondResult = postMultipart(
                "/components/1/images",
                "second.png",
                "image/png",
                pngBytes(),
                null
        )

        then:
        firstResult.status == 201
        secondResult.status == 201
        objectMapper.readValue(
                firstResult.body,
                ru.sultanyarov.configurator.api.inbounds.rest.dto.ComponentImage
        ).getOrderIndex() == 0
        objectMapper.readValue(
                secondResult.body,
                ru.sultanyarov.configurator.api.inbounds.rest.dto.ComponentImage
        ).getOrderIndex() == 1
    }

    def "should return not found when uploading image for non-existent component"() {
        given:
        runSqlScripts("/sql/clear-db.sql")

        expect:
        postMultipart(
                "/components/999999/images",
                "image.png",
                "image/png",
                pngBytes(),
                null
        ).status == 404
    }

    def "should return conflict when uploading image for archived component"() {
        given:
        prepareComponentImageData()
        delete("/components/1").status == 204

        expect:
        postMultipart(
                "/components/1/images",
                "image.png",
                "image/png",
                pngBytes(),
                null
        ).status == 409
    }

    def "should return bad request when image file is missing"() {
        given:
        prepareComponentImageData()

        expect:
        postMultipart(
                "/components/1/images",
                "image.png",
                "image/png",
                null,
                0
        ).status == 400
    }

    def "should return bad request when image file is empty"() {
        given:
        prepareComponentImageData()

        expect:
        postMultipart(
                "/components/1/images",
                "image.png",
                "image/png",
                new byte[0],
                null
        ).status == 400
    }

    def "should return bad request when image order index is negative"() {
        given:
        prepareComponentImageData()

        expect:
        postMultipart(
                "/components/1/images",
                "image.png",
                "image/png",
                pngBytes(),
                -1
        ).status == 400
    }

    def "should return bad request when component id for image upload is not positive"() {
        given:
        prepareComponentImageData()

        expect:
        postMultipart(
                "/components/0/images",
                "image.png",
                "image/png",
                pngBytes(),
                null
        ).status == 400
    }

    def "should return unsupported media type for unsupported image format"() {
        given:
        prepareComponentImageData()

        expect:
        postMultipart(
                "/components/1/images",
                "image.gif",
                "image/gif",
                new byte[]{0x47, 0x49, 0x46},
                null
        ).status == 415
    }

    def "should return unsupported media type when content does not match declared image format"() {
        given:
        prepareComponentImageData()

        expect:
        postMultipart(
                "/components/1/images",
                "fake.jpg",
                "image/jpeg",
                new byte[]{1, 2, 3},
                null
        ).status == 415
    }

    def "should return payload too large when image exceeds ten mebibytes"() {
        given:
        prepareComponentImageData()

        expect:
        postMultipart(
                "/components/1/images",
                "large.png",
                "image/png",
                oversizedPng(),
                null
        ).status == 413
    }

    def "should get component images in deterministic display order"() {
        given:
        prepareComponentImagesReadData()

        when:
        def result = get("/components/1/images")

        then:
        result.status == 200
        List<ComponentImage> responseBody = objectMapper.readerForListOf(ComponentImage)
                .readValue(result.body)
        responseBody*.getId() == [602L, 603L, 604L, 601L]
        responseBody*.getOrderIndex() == [0, 2, 2, null]
        responseBody*.getUrl() == [
                "/component-images/602/content",
                "/component-images/603/content",
                "/component-images/604/content",
                "/component-images/601/content"
        ]
    }

    def "should return empty array when component has no images"() {
        given:
        prepareComponentImageData()

        when:
        def result = get("/components/1/images")

        then:
        result.status == 200
        objectMapper.readerForListOf(ComponentImage).readValue(result.body).isEmpty()
    }

    def "should get images of archived component"() {
        given:
        prepareComponentImagesReadData()
        delete("/components/1").status == 204

        when:
        def result = get("/components/1/images")

        then:
        result.status == 200
        List<ComponentImage> responseBody = objectMapper.readerForListOf(ComponentImage)
                .readValue(result.body)
        responseBody*.getId() == [602L, 603L, 604L, 601L]
    }

    def "should return not found when getting images of non-existent component"() {
        given:
        runSqlScripts("/sql/clear-db.sql")

        when:
        def result = get("/components/999999/images")

        then:
        result.status == 404
        objectMapper.readValue(result.body, ErrorResponse).getMessage() ==
                "Component with id 999999 not found"
    }

    def "should return bad request when component id for image retrieval is not positive"() {
        given:
        runSqlScripts("/sql/clear-db.sql")

        expect:
        get("/components/0/images").status == 400
    }

    def "should get components by domain with pagination"() {
        given:
        prepareComponentSearchData()

        when:
        def result = get("/domains/1/components", [page: 1, size: 2])

        then:
        result.status == 200
        def responseBody = objectMapper.readValue(result.body, ComponentPage)
        responseBody.getPage() == 1
        responseBody.getSize() == 2
        responseBody.getTotalItems() == 3
        responseBody.getItems()*.getId() == [3L]
        responseBody.getItems().every { it.getComponentTypeId() in [1L, 2L] }
    }

    def "should use default pagination when component page parameters are omitted"() {
        given:
        prepareComponentSearchData()

        when:
        def result = get("/domains/1/components")

        then:
        result.status == 200
        def responseBody = objectMapper.readValue(result.body, ComponentPage)
        responseBody.getPage() == 0
        responseBody.getSize() == 10
        responseBody.getTotalItems() == 3
        responseBody.getItems()*.getId() == [1L, 2L, 3L]
    }

    def "should filter domain components by component type"() {
        given:
        prepareComponentSearchData()

        when:
        def result = get("/domains/1/components", [componentTypeId: 1, page: 0, size: 10])

        then:
        result.status == 200
        def responseBody = objectMapper.readValue(result.body, ComponentPage)
        responseBody.getTotalItems() == 2
        responseBody.getItems()*.getId() == [1L, 2L]
        responseBody.getItems().every { it.getComponentTypeId() == 1L }
    }

    def "should filter domain components by exact name"() {
        given:
        prepareComponentSearchData()

        when:
        def result = get("/domains/1/components", [name: "Keychron K2", page: 0, size: 10])

        then:
        result.status == 200
        def responseBody = objectMapper.readValue(result.body, ComponentPage)
        responseBody.getTotalItems() == 1
        responseBody.getItems().size() == 1
        responseBody.getItems().first().getId() == 2L
        responseBody.getItems().first().getName() == "Keychron K2"
    }

    def "should return not found when getting components for non-existent domain"() {
        given:
        runSqlScripts("/sql/clear-db.sql")

        expect:
        get("/domains/999999/components", [page: 0, size: 10]).status == 404
    }

    def "should return bad request when component type belongs to another domain"() {
        given:
        prepareComponentSearchData()

        expect:
        get("/domains/1/components", [componentTypeId: 3, page: 0, size: 10]).status == 400
    }

    private void prepareComponentSearchData() {
        runSqlScripts(
                "/sql/clear-db.sql",
                "/sql/insert-test-domain.sql",
                "/sql/insert-test-component-type.sql",
                "/sql/insert-second-test-component-type.sql",
                "/sql/insert-test-component.sql",
                "/sql/insert-component-search-data.sql"
        )
    }

    private void prepareComponentImageData() {
        runSqlScripts(
                "/sql/clear-db.sql",
                "/sql/insert-test-domain.sql",
                "/sql/insert-test-component-type.sql",
                "/sql/insert-test-component.sql"
        )
    }

    private void prepareComponentImagesReadData() {
        runSqlScripts(
                "/sql/clear-db.sql",
                "/sql/insert-test-domain.sql",
                "/sql/insert-test-component-type.sql",
                "/sql/insert-test-component.sql",
                "/sql/insert-test-component-images.sql"
        )
    }

    private void prepareComponentUpdateData() {
        runSqlScripts(
                "/sql/clear-db.sql",
                "/sql/insert-test-domain.sql",
                "/sql/insert-test-component-type.sql",
                "/sql/insert-second-test-component-type.sql",
                "/sql/insert-test-component-creation-attributes.sql",
                "/sql/insert-second-test-attribute-definition.sql",
                "/sql/insert-test-component.sql",
                "/sql/insert-test-component-update-data.sql"
        )
    }

    private static UpdateComponentRequest validUpdateRequest() {
        return new UpdateComponentRequest(
                1L,
                "Updated component",
                [
                        new AttributeValueInput(101L, "ANSI"),
                        new AttributeValueInput(102L, "75%")
                ]
        )
    }

    private static byte[] jpegBytes() {
        return new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x00}
    }

    private static byte[] pngBytes() {
        return new byte[]{
                (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A
        }
    }

    private static byte[] webpBytes() {
        return new byte[]{
                0x52, 0x49, 0x46, 0x46,
                0, 0, 0, 0,
                0x57, 0x45, 0x42, 0x50
        }
    }

    private static byte[] oversizedPng() {
        def content = new byte[10 * 1024 * 1024 + 1]
        def signature = pngBytes()
        System.arraycopy(signature, 0, content, 0, signature.length)
        return content
    }
}
