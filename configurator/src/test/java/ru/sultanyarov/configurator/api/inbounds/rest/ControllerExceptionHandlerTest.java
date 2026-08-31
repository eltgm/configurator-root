package ru.sultanyarov.configurator.api.inbounds.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import jakarta.validation.constraints.Positive;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import ru.sultanyarov.configurator.api.inbounds.rest.advice.ControllerExceptionHandler;
import ru.sultanyarov.configurator.api.inbounds.rest.dto.ApiErrorCode;
import ru.sultanyarov.configurator.api.inbounds.rest.dto.ApiErrorDetail;
import ru.sultanyarov.configurator.api.inbounds.rest.dto.ErrorResponse;
import ru.sultanyarov.configurator.domain.exception.AttributeNameConflictException;
import ru.sultanyarov.configurator.domain.exception.BusinessException;
import ru.sultanyarov.configurator.domain.exception.ComponentArchivedException;
import ru.sultanyarov.configurator.domain.exception.ConfigurationConflictException;
import ru.sultanyarov.configurator.domain.exception.DomainHasConfigurationsException;
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
        ApiErrorCode.BUSINESS_ERROR,
        "boom");
    assertErrorResponse(
        handler.handleUnexpectedException(new Exception("sensitive"), request),
        HttpStatus.INTERNAL_SERVER_ERROR,
        ApiErrorCode.INTERNAL_ERROR,
        "Internal server error");
  }

  @Test
  void handleNotFoundException_shouldReturnNotFound() {
    assertErrorResponse(
        handler.handleNotFoundException(new NotFoundException("missing"), request),
        HttpStatus.NOT_FOUND,
        ApiErrorCode.NOT_FOUND,
        "missing");
  }

  @Test
  void handleEntityAlreadyExistsException_shouldReturnConflict() {
    assertErrorResponse(
        handler.handleEntityAlreadyExistsException(
            new EntityAlreadyExistsException("exists"), request),
        HttpStatus.CONFLICT,
        ApiErrorCode.ENTITY_ALREADY_EXISTS,
        "exists");
    assertErrorResponse(
        handler.handleEntityAlreadyExistsException(
            new EntityHasRelatedEntitiesException("related"), request),
        HttpStatus.CONFLICT,
        ApiErrorCode.ENTITY_HAS_RELATED_ENTITIES,
        "related");
    assertErrorResponse(
        handler.handleEntityAlreadyExistsException(
            new ComponentArchivedException("archived"), request),
        HttpStatus.CONFLICT,
        ApiErrorCode.COMPONENT_ARCHIVED,
        "archived");
    assertErrorResponse(
        handler.handleEntityAlreadyExistsException(
            new ConfigurationConflictException("incompatible"), request),
        HttpStatus.CONFLICT,
        ApiErrorCode.CONFIGURATION_CONFLICT,
        "incompatible");
  }

  @Test
  void domainWithConfigurations_shouldReturnSpecificConflict() {
    var exception = new DomainHasConfigurationsException(1L);
    assertErrorResponse(
        handler.handleEntityAlreadyExistsException(exception, request),
        HttpStatus.CONFLICT,
        ApiErrorCode.DOMAIN_HAS_CONFIGURATIONS,
        exception.getMessage());
  }

  @Test
  void attributeNameConflict_shouldIdentifyTheNameField() {
    var exception = new AttributeNameConflictException(1L, "socket");
    assertErrorResponse(
        handler.handleEntityAlreadyExistsException(exception, request),
        HttpStatus.CONFLICT,
        ApiErrorCode.ENTITY_ALREADY_EXISTS,
        exception.getMessage(),
        List.of(new ApiErrorDetail("ENTITY_ALREADY_EXISTS", exception.getMessage()).field("name")));
  }

  @Test
  void handleValidationException_shouldReturnBadRequest() {
    assertErrorResponse(
        handler.handleValidationException(new ValidationException("bad"), request),
        HttpStatus.BAD_REQUEST,
        ApiErrorCode.VALIDATION_ERROR,
        "bad",
        List.of(new ApiErrorDetail("INVALID_VALUE", "bad")));
    MethodArgumentNotValidException validationException =
        mock(MethodArgumentNotValidException.class);
    BeanPropertyBindingResult bindingResult =
        new BeanPropertyBindingResult(new Object(), "request");
    bindingResult.addError(
        new FieldError(
            "request", "name", null, false, new String[] {"NotBlank"}, null, "must not be blank"));
    bindingResult.addError(
        new ObjectError("request", new String[] {"ValidRequest"}, null, "invalid"));
    when(validationException.getBindingResult()).thenReturn(bindingResult);
    assertErrorResponse(
        handler.handleValidationException(validationException, request),
        HttpStatus.BAD_REQUEST,
        ApiErrorCode.VALIDATION_ERROR,
        "invalid; name: must not be blank",
        List.of(
            new ApiErrorDetail("VALID_REQUEST", "invalid"),
            new ApiErrorDetail("NOT_BLANK", "must not be blank").field("name")));
    ConstraintViolationException constraintViolationException =
        new ConstraintViolationException("must be positive", Set.of());
    assertErrorResponse(
        handler.handleValidationException(constraintViolationException, request),
        HttpStatus.BAD_REQUEST,
        ApiErrorCode.VALIDATION_ERROR,
        "must be positive",
        List.of(new ApiErrorDetail("INVALID_VALUE", "must be positive")));
    MissingServletRequestPartException missingPartException =
        new MissingServletRequestPartException("file");
    assertErrorResponse(
        handler.handleValidationException(missingPartException, request),
        HttpStatus.BAD_REQUEST,
        ApiErrorCode.VALIDATION_ERROR,
        missingPartException.getLocalizedMessage(),
        List.of(
            new ApiErrorDetail("MISSING_PART", missingPartException.getLocalizedMessage())
                .field("file")));
    MissingServletRequestParameterException missingParameterException =
        new MissingServletRequestParameterException("componentId", "Long");
    assertErrorResponse(
        handler.handleValidationException(missingParameterException, request),
        HttpStatus.BAD_REQUEST,
        ApiErrorCode.VALIDATION_ERROR,
        missingParameterException.getLocalizedMessage(),
        List.of(
            new ApiErrorDetail("MISSING_PARAMETER", missingParameterException.getLocalizedMessage())
                .field("componentId")));
  }

  @Test
  void handleValidationException_shouldReturnConstraintAndTransportDetails() {
    try (ValidatorFactory validatorFactory = Validation.buildDefaultValidatorFactory()) {
      Set<ConstraintViolation<PositiveRequest>> violations =
          validatorFactory.getValidator().validate(new PositiveRequest(0L));
      ConstraintViolationException constraintViolationException =
          new ConstraintViolationException(violations);
      ErrorResponse response =
          handler.handleValidationException(constraintViolationException, request).getBody();
      assertThat(response.getDetails()).hasSize(1);
      assertThat(response.getDetails().getFirst().getField()).isEqualTo("id");
      assertThat(response.getDetails().getFirst().getCode()).isEqualTo("POSITIVE");
      assertThat(response.getDetails().getFirst().getMessage()).isNotBlank();
    }

    MethodArgumentTypeMismatchException mismatchException =
        new MethodArgumentTypeMismatchException("secret", Long.class, "componentId", null, null);
    assertErrorResponse(
        handler.handleValidationException(mismatchException, request),
        HttpStatus.BAD_REQUEST,
        ApiErrorCode.VALIDATION_ERROR,
        "Invalid value for parameter 'componentId'",
        List.of(
            new ApiErrorDetail("TYPE_MISMATCH", "Invalid value for parameter 'componentId'")
                .field("componentId")));

    HttpMessageNotReadableException malformedException =
        new HttpMessageNotReadableException(
            "sensitive parser diagnostics", new MockHttpInputMessage(new byte[0]));
    assertErrorResponse(
        handler.handleValidationException(malformedException, request),
        HttpStatus.BAD_REQUEST,
        ApiErrorCode.VALIDATION_ERROR,
        "Malformed request body",
        List.of(new ApiErrorDetail("MALFORMED_REQUEST", "Malformed request body")));
  }

  @Test
  void handlePayloadTooLargeException_shouldReturnPayloadTooLarge() {
    assertErrorResponse(
        handler.handlePayloadTooLargeException(new ImageTooLargeException("too large"), request),
        HttpStatus.PAYLOAD_TOO_LARGE,
        ApiErrorCode.IMAGE_TOO_LARGE,
        "too large");
    MaxUploadSizeExceededException multipartException = new MaxUploadSizeExceededException(10L);
    assertErrorResponse(
        handler.handlePayloadTooLargeException(multipartException, request),
        HttpStatus.PAYLOAD_TOO_LARGE,
        ApiErrorCode.IMAGE_TOO_LARGE,
        multipartException.getLocalizedMessage());
  }

  @Test
  void handleUnsupportedImageFormatException_shouldReturnUnsupportedMediaType() {
    assertErrorResponse(
        handler.handleUnsupportedImageFormatException(
            new UnsupportedImageFormatException("unsupported"), request),
        HttpStatus.UNSUPPORTED_MEDIA_TYPE,
        ApiErrorCode.UNSUPPORTED_IMAGE_FORMAT,
        "unsupported");
  }

  @Test
  void handleExternalStorageException_shouldReturnServiceUnavailable() {
    assertErrorResponse(
        handler.handleExternalStorageException(
            new ExternalStorageException(new IllegalStateException(), "unavailable"), request),
        HttpStatus.SERVICE_UNAVAILABLE,
        ApiErrorCode.EXTERNAL_STORAGE_UNAVAILABLE,
        "External storage is temporarily unavailable");
  }

  private static void assertErrorResponse(
      ResponseEntity<?> response,
      HttpStatus expectedStatus,
      ApiErrorCode expectedCode,
      String expectedMessage) {
    assertErrorResponse(response, expectedStatus, expectedCode, expectedMessage, List.of());
  }

  private static void assertErrorResponse(
      ResponseEntity<?> response,
      HttpStatus expectedStatus,
      ApiErrorCode expectedCode,
      String expectedMessage,
      List<ApiErrorDetail> expectedDetails) {
    assertThat(response.getStatusCode()).isEqualTo(expectedStatus);
    assertThat(response.getBody()).isInstanceOf(ErrorResponse.class);
    ErrorResponse errorResponse = (ErrorResponse) response.getBody();
    assertThat(errorResponse.getStatus()).isEqualTo(expectedStatus.value());
    assertThat(errorResponse.getError()).isEqualTo(expectedStatus.getReasonPhrase());
    assertThat(errorResponse.getCode()).isEqualTo(expectedCode);
    assertThat(errorResponse.getMessage()).isEqualTo(expectedMessage);
    assertThat(errorResponse.getDetails()).containsExactlyElementsOf(expectedDetails);
    assertThat(errorResponse.getTimestamp()).isNotNull();
    assertThat(errorResponse.getPath()).isEqualTo("/test");
  }

  private record PositiveRequest(@Positive Long id) {}

  private static MockHttpServletRequest request() {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setRequestURI("/test");
    return request;
  }
}
