package ru.sultanyarov.configurator.api.inbounds.rest;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import ru.sultanyarov.configurator.api.inbounds.rest.advice.ControllerExceptionHandler;
import ru.sultanyarov.configurator.api.inbounds.rest.dto.ErrorResponse;
import ru.sultanyarov.configurator.domain.exception.BusinessException;
import ru.sultanyarov.configurator.domain.exception.EntityAlreadyExistsException;
import ru.sultanyarov.configurator.domain.exception.EntityHasRelatedEntitiesException;
import ru.sultanyarov.configurator.domain.exception.NotFoundException;
import ru.sultanyarov.configurator.domain.exception.ValidationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class ControllerExceptionHandlerTest {

    private final ControllerExceptionHandler handler = new ControllerExceptionHandler();

    @Test
    void handleBusinessException_shouldReturnInternalServerError() {
        assertErrorResponse(handler.handleBusinessException(new BusinessException("boom")), HttpStatus.INTERNAL_SERVER_ERROR, "boom");
        assertErrorResponse(handler.handleBusinessException(new Exception("fail")), HttpStatus.INTERNAL_SERVER_ERROR, "fail");
    }

    @Test
    void handleNotFoundException_shouldReturnNotFound() {
        assertErrorResponse(handler.handleNotFoundException(new NotFoundException("missing")), HttpStatus.NOT_FOUND, "missing");
    }

    @Test
    void handleEntityAlreadyExistsException_shouldReturnConflict() {
        assertErrorResponse(handler.handleEntityAlreadyExistsException(new EntityAlreadyExistsException("exists")), HttpStatus.CONFLICT, "exists");
        assertErrorResponse(handler.handleEntityAlreadyExistsException(new EntityHasRelatedEntitiesException("related")), HttpStatus.CONFLICT, "related");
    }

    @Test
    void handleValidationException_shouldReturnBadRequest() {
        assertErrorResponse(handler.handleValidationException(new ValidationException("bad")), HttpStatus.BAD_REQUEST, "bad");
        MethodArgumentNotValidException validationException = mock(MethodArgumentNotValidException.class);
        assertErrorResponse(handler.handleValidationException(validationException), HttpStatus.BAD_REQUEST, validationException.getLocalizedMessage());
    }

    private static void assertErrorResponse(ResponseEntity<?> response, HttpStatus expectedStatus, String expectedMessage) {
        assertThat(response.getStatusCode()).isEqualTo(expectedStatus);
        assertThat(response.getBody()).isInstanceOf(ErrorResponse.class);
        ErrorResponse errorResponse = (ErrorResponse) response.getBody();
        assertThat(errorResponse.getStatus()).isEqualTo(expectedStatus.value());
        assertThat(errorResponse.getError()).isEqualTo(expectedStatus.getReasonPhrase());
        assertThat(errorResponse.getMessage()).isEqualTo(expectedMessage);
        assertThat(errorResponse.getTimestamp()).isNotNull();
    }
}
