package ru.sultanyarov.configurator.api.inbounds.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import jakarta.validation.ConstraintViolationException;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import ru.sultanyarov.configurator.api.inbounds.rest.advice.ControllerExceptionHandler;
import ru.sultanyarov.configurator.api.inbounds.rest.dto.ErrorResponse;
import ru.sultanyarov.configurator.domain.exception.BusinessException;
import ru.sultanyarov.configurator.domain.exception.ComponentArchivedException;
import ru.sultanyarov.configurator.domain.exception.EntityAlreadyExistsException;
import ru.sultanyarov.configurator.domain.exception.EntityHasRelatedEntitiesException;
import ru.sultanyarov.configurator.domain.exception.ExternalStorageException;
import ru.sultanyarov.configurator.domain.exception.ImageTooLargeException;
import ru.sultanyarov.configurator.domain.exception.NotFoundException;
import ru.sultanyarov.configurator.domain.exception.UnsupportedImageFormatException;
import ru.sultanyarov.configurator.domain.exception.ValidationException;

class ControllerExceptionHandlerTest {

  private final ControllerExceptionHandler handler = new ControllerExceptionHandler();
  private final MockHttpServletRequest request = request();

  @Test
  void handleBusinessException_shouldReturnInternalServerError() {
    assertErrorResponse(
        handler.handleBusinessException(new BusinessException("boom"), request),
        HttpStatus.INTERNAL_SERVER_ERROR,
        "boom");
    assertErrorResponse(
        handler.handleUnexpectedException(new Exception("sensitive"), request),
        HttpStatus.INTERNAL_SERVER_ERROR,
        "Internal server error");
  }

  @Test
  void handleNotFoundException_shouldReturnNotFound() {
    assertErrorResponse(
        handler.handleNotFoundException(new NotFoundException("missing"), request),
        HttpStatus.NOT_FOUND,
        "missing");
  }

  @Test
  void handleEntityAlreadyExistsException_shouldReturnConflict() {
    assertErrorResponse(
        handler.handleEntityAlreadyExistsException(
            new EntityAlreadyExistsException("exists"), request),
        HttpStatus.CONFLICT,
        "exists");
    assertErrorResponse(
        handler.handleEntityAlreadyExistsException(
            new EntityHasRelatedEntitiesException("related"), request),
        HttpStatus.CONFLICT,
        "related");
    assertErrorResponse(
        handler.handleEntityAlreadyExistsException(
            new ComponentArchivedException("archived"), request),
        HttpStatus.CONFLICT,
        "archived");
  }

  @Test
  void handleValidationException_shouldReturnBadRequest() {
    assertErrorResponse(
        handler.handleValidationException(new ValidationException("bad"), request),
        HttpStatus.BAD_REQUEST,
        "bad");
    MethodArgumentNotValidException validationException =
        mock(MethodArgumentNotValidException.class);
    BeanPropertyBindingResult bindingResult =
        new BeanPropertyBindingResult(new Object(), "request");
    bindingResult.addError(new FieldError("request", "name", "must not be blank"));
    when(validationException.getBindingResult()).thenReturn(bindingResult);
    assertErrorResponse(
        handler.handleValidationException(validationException, request),
        HttpStatus.BAD_REQUEST,
        "name: must not be blank");
    ConstraintViolationException constraintViolationException =
        new ConstraintViolationException("must be positive", Set.of());
    assertErrorResponse(
        handler.handleValidationException(constraintViolationException, request),
        HttpStatus.BAD_REQUEST,
        "must be positive");
    MissingServletRequestPartException missingPartException =
        new MissingServletRequestPartException("file");
    assertErrorResponse(
        handler.handleValidationException(missingPartException, request),
        HttpStatus.BAD_REQUEST,
        missingPartException.getLocalizedMessage());
    MissingServletRequestParameterException missingParameterException =
        new MissingServletRequestParameterException("componentId", "Long");
    assertErrorResponse(
        handler.handleValidationException(missingParameterException, request),
        HttpStatus.BAD_REQUEST,
        missingParameterException.getLocalizedMessage());
  }

  @Test
  void handlePayloadTooLargeException_shouldReturnPayloadTooLarge() {
    assertErrorResponse(
        handler.handlePayloadTooLargeException(new ImageTooLargeException("too large"), request),
        HttpStatus.PAYLOAD_TOO_LARGE,
        "too large");
    MaxUploadSizeExceededException multipartException = new MaxUploadSizeExceededException(10L);
    assertErrorResponse(
        handler.handlePayloadTooLargeException(multipartException, request),
        HttpStatus.PAYLOAD_TOO_LARGE,
        multipartException.getLocalizedMessage());
  }

  @Test
  void handleUnsupportedImageFormatException_shouldReturnUnsupportedMediaType() {
    assertErrorResponse(
        handler.handleUnsupportedImageFormatException(
            new UnsupportedImageFormatException("unsupported"), request),
        HttpStatus.UNSUPPORTED_MEDIA_TYPE,
        "unsupported");
  }

  @Test
  void handleExternalStorageException_shouldReturnServiceUnavailable() {
    assertErrorResponse(
        handler.handleExternalStorageException(
            new ExternalStorageException(new IllegalStateException(), "unavailable"), request),
        HttpStatus.SERVICE_UNAVAILABLE,
        "External storage is temporarily unavailable");
  }

  private static void assertErrorResponse(
      ResponseEntity<?> response, HttpStatus expectedStatus, String expectedMessage) {
    assertThat(response.getStatusCode()).isEqualTo(expectedStatus);
    assertThat(response.getBody()).isInstanceOf(ErrorResponse.class);
    ErrorResponse errorResponse = (ErrorResponse) response.getBody();
    assertThat(errorResponse.getStatus()).isEqualTo(expectedStatus.value());
    assertThat(errorResponse.getError()).isEqualTo(expectedStatus.getReasonPhrase());
    assertThat(errorResponse.getMessage()).isEqualTo(expectedMessage);
    assertThat(errorResponse.getTimestamp()).isNotNull();
    assertThat(errorResponse.getPath()).isEqualTo("/test");
  }

  private static MockHttpServletRequest request() {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setRequestURI("/test");
    return request;
  }
}
