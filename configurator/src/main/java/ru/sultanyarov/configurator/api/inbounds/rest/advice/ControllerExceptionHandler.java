package ru.sultanyarov.configurator.api.inbounds.rest.advice;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.PAYLOAD_TOO_LARGE;
import static org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE;
import static org.springframework.http.HttpStatus.UNSUPPORTED_MEDIA_TYPE;
import static ru.sultanyarov.configurator.api.inbounds.rest.dto.ApiErrorCode.BUSINESS_ERROR;
import static ru.sultanyarov.configurator.api.inbounds.rest.dto.ApiErrorCode.COMPONENT_ARCHIVED;
import static ru.sultanyarov.configurator.api.inbounds.rest.dto.ApiErrorCode.CONFIGURATION_CONFLICT;
import static ru.sultanyarov.configurator.api.inbounds.rest.dto.ApiErrorCode.ENTITY_ALREADY_EXISTS;
import static ru.sultanyarov.configurator.api.inbounds.rest.dto.ApiErrorCode.ENTITY_HAS_RELATED_ENTITIES;
import static ru.sultanyarov.configurator.api.inbounds.rest.dto.ApiErrorCode.EXTERNAL_STORAGE_UNAVAILABLE;
import static ru.sultanyarov.configurator.api.inbounds.rest.dto.ApiErrorCode.IMAGE_TOO_LARGE;
import static ru.sultanyarov.configurator.api.inbounds.rest.dto.ApiErrorCode.INTERNAL_ERROR;
import static ru.sultanyarov.configurator.api.inbounds.rest.dto.ApiErrorCode.NOT_FOUND;
import static ru.sultanyarov.configurator.api.inbounds.rest.dto.ApiErrorCode.UNSUPPORTED_IMAGE_FORMAT;
import static ru.sultanyarov.configurator.api.inbounds.rest.dto.ApiErrorCode.VALIDATION_ERROR;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import ru.sultanyarov.configurator.api.inbounds.rest.dto.ApiErrorCode;
import ru.sultanyarov.configurator.api.inbounds.rest.dto.ApiErrorDetail;
import ru.sultanyarov.configurator.api.inbounds.rest.dto.ErrorResponse;
import ru.sultanyarov.configurator.domain.exception.BusinessException;
import ru.sultanyarov.configurator.domain.exception.ComponentArchivedException;
import ru.sultanyarov.configurator.domain.exception.ConfigurationConflictException;
import ru.sultanyarov.configurator.domain.exception.EntityAlreadyExistsException;
import ru.sultanyarov.configurator.domain.exception.EntityHasRelatedEntitiesException;
import ru.sultanyarov.configurator.domain.exception.ExternalStorageException;
import ru.sultanyarov.configurator.domain.exception.ImageTooLargeException;
import ru.sultanyarov.configurator.domain.exception.NotFoundException;
import ru.sultanyarov.configurator.domain.exception.UnsupportedImageFormatException;
import ru.sultanyarov.configurator.domain.exception.ValidationException;

@RestControllerAdvice
@Slf4j
public class ControllerExceptionHandler {
  @ExceptionHandler(BusinessException.class)
  public ResponseEntity<ErrorResponse> handleBusinessException(
      BusinessException exception, HttpServletRequest request) {
    log.warn("Business operation failed for {}", request.getRequestURI(), exception);
    return getBody(INTERNAL_SERVER_ERROR, BUSINESS_ERROR, exception.getLocalizedMessage(), request);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleUnexpectedException(
      Exception exception, HttpServletRequest request) {
    log.error("Unexpected error while processing {}", request.getRequestURI(), exception);
    return getBody(INTERNAL_SERVER_ERROR, INTERNAL_ERROR, "Internal server error", request);
  }

  @ExceptionHandler(NotFoundException.class)
  public ResponseEntity<ErrorResponse> handleNotFoundException(
      NotFoundException exception, HttpServletRequest request) {
    return getBody(
        org.springframework.http.HttpStatus.NOT_FOUND,
        NOT_FOUND,
        exception.getLocalizedMessage(),
        request);
  }

  @ExceptionHandler({
    EntityAlreadyExistsException.class,
    EntityHasRelatedEntitiesException.class,
    ComponentArchivedException.class,
    ConfigurationConflictException.class
  })
  public ResponseEntity<ErrorResponse> handleEntityAlreadyExistsException(
      Exception exception, HttpServletRequest request) {
    return getBody(CONFLICT, conflictCode(exception), exception.getLocalizedMessage(), request);
  }

  @ExceptionHandler({
    ValidationException.class,
    MethodArgumentNotValidException.class,
    BindException.class,
    ConstraintViolationException.class,
    MissingServletRequestParameterException.class,
    MissingServletRequestPartException.class,
    MethodArgumentTypeMismatchException.class,
    HttpMessageNotReadableException.class
  })
  public ResponseEntity<ErrorResponse> handleValidationException(
      Exception exception, HttpServletRequest request) {
    List<ApiErrorDetail> details = validationDetails(exception);
    return getBody(
        BAD_REQUEST, VALIDATION_ERROR, validationMessage(exception, details), request, details);
  }

  @ExceptionHandler({ImageTooLargeException.class, MaxUploadSizeExceededException.class})
  public ResponseEntity<ErrorResponse> handlePayloadTooLargeException(
      Exception exception, HttpServletRequest request) {
    return getBody(PAYLOAD_TOO_LARGE, IMAGE_TOO_LARGE, exception.getLocalizedMessage(), request);
  }

  @ExceptionHandler(UnsupportedImageFormatException.class)
  public ResponseEntity<ErrorResponse> handleUnsupportedImageFormatException(
      UnsupportedImageFormatException exception, HttpServletRequest request) {
    return getBody(
        UNSUPPORTED_MEDIA_TYPE, UNSUPPORTED_IMAGE_FORMAT, exception.getLocalizedMessage(), request);
  }

  @ExceptionHandler(ExternalStorageException.class)
  public ResponseEntity<ErrorResponse> handleExternalStorageException(
      ExternalStorageException exception, HttpServletRequest request) {
    log.error("External storage failed for {}", request.getRequestURI(), exception);
    return getBody(
        SERVICE_UNAVAILABLE,
        EXTERNAL_STORAGE_UNAVAILABLE,
        "External storage is temporarily unavailable",
        request);
  }

  private static ApiErrorCode conflictCode(Exception exception) {
    if (exception instanceof EntityAlreadyExistsException) {
      return ENTITY_ALREADY_EXISTS;
    }
    if (exception instanceof EntityHasRelatedEntitiesException) {
      return ENTITY_HAS_RELATED_ENTITIES;
    }
    if (exception instanceof ComponentArchivedException) {
      return COMPONENT_ARCHIVED;
    }
    if (exception instanceof ConfigurationConflictException) {
      return CONFIGURATION_CONFLICT;
    }
    throw new IllegalArgumentException("Unsupported conflict exception: " + exception.getClass());
  }

  private static String validationMessage(
      Exception exception, List<ApiErrorDetail> validationDetails) {
    if (exception instanceof BindException) {
      return validationDetails.stream()
          .map(
              detail ->
                  detail.getField() == null
                      ? detail.getMessage()
                      : "%s: %s".formatted(detail.getField(), detail.getMessage()))
          .distinct()
          .sorted()
          .collect(java.util.stream.Collectors.joining("; "));
    }
    if (exception instanceof HttpMessageNotReadableException) {
      return "Malformed request body";
    }
    if (exception instanceof MethodArgumentTypeMismatchException mismatchException) {
      return "Invalid value for parameter '%s'".formatted(mismatchException.getName());
    }
    return exception.getLocalizedMessage();
  }

  private static List<ApiErrorDetail> validationDetails(Exception exception) {
    if (exception instanceof BindException bindException) {
      return bindingDetails(bindException.getBindingResult());
    }
    if (exception instanceof ConstraintViolationException violationException) {
      List<ApiErrorDetail> details =
          violationException.getConstraintViolations().stream()
              .map(
                  violation ->
                      detail(
                          violation.getPropertyPath().toString(),
                          normalizeCode(
                              violation
                                  .getConstraintDescriptor()
                                  .getAnnotation()
                                  .annotationType()
                                  .getSimpleName()),
                          violation.getMessage()))
              .distinct()
              .sorted(DETAIL_COMPARATOR)
              .toList();
      return details.isEmpty()
          ? List.of(detail(null, "INVALID_VALUE", exception.getLocalizedMessage()))
          : details;
    }
    if (exception instanceof MissingServletRequestParameterException missingParameter) {
      return List.of(
          detail(
              missingParameter.getParameterName(),
              "MISSING_PARAMETER",
              missingParameter.getLocalizedMessage()));
    }
    if (exception instanceof MissingServletRequestPartException missingPart) {
      return List.of(
          detail(
              missingPart.getRequestPartName(), "MISSING_PART", missingPart.getLocalizedMessage()));
    }
    if (exception instanceof MethodArgumentTypeMismatchException mismatchException) {
      return List.of(
          detail(
              mismatchException.getName(),
              "TYPE_MISMATCH",
              "Invalid value for parameter '%s'".formatted(mismatchException.getName())));
    }
    if (exception instanceof HttpMessageNotReadableException) {
      return List.of(detail(null, "MALFORMED_REQUEST", "Malformed request body"));
    }
    return List.of(detail(null, "INVALID_VALUE", exception.getLocalizedMessage()));
  }

  private static List<ApiErrorDetail> bindingDetails(BindingResult bindingResult) {
    return bindingResult.getAllErrors().stream()
        .map(ControllerExceptionHandler::bindingDetail)
        .distinct()
        .sorted(DETAIL_COMPARATOR)
        .toList();
  }

  private static ApiErrorDetail bindingDetail(ObjectError error) {
    String field = error instanceof FieldError fieldError ? fieldError.getField() : null;
    return detail(field, normalizeCode(error.getCode()), error.getDefaultMessage());
  }

  private static ApiErrorDetail detail(String field, String code, String message) {
    ApiErrorDetail detail =
        new ApiErrorDetail(
            code == null || code.isBlank() ? "INVALID_VALUE" : code,
            message == null || message.isBlank() ? "Invalid value" : message);
    return field == null || field.isBlank() ? detail : detail.field(field);
  }

  private static String normalizeCode(String code) {
    if (code == null || code.isBlank()) {
      return "INVALID_VALUE";
    }
    return code.replaceAll("([a-z0-9])([A-Z])", "$1_$2").toUpperCase(Locale.ROOT);
  }

  private static ResponseEntity<ErrorResponse> getBody(
      HttpStatus httpStatus, ApiErrorCode code, String message, HttpServletRequest request) {
    return getBody(httpStatus, code, message, request, List.of());
  }

  private static ResponseEntity<ErrorResponse> getBody(
      HttpStatus httpStatus,
      ApiErrorCode code,
      String message,
      HttpServletRequest request,
      List<ApiErrorDetail> details) {
    return ResponseEntity.status(httpStatus)
        .body(
            new ErrorResponse(
                LocalDateTime.now(),
                httpStatus.value(),
                httpStatus.getReasonPhrase(),
                code,
                message,
                request.getRequestURI(),
                details));
  }

  private static final Comparator<ApiErrorDetail> DETAIL_COMPARATOR =
      Comparator.comparing(ApiErrorDetail::getField, Comparator.nullsFirst(String::compareTo))
          .thenComparing(ApiErrorDetail::getCode)
          .thenComparing(ApiErrorDetail::getMessage);
}
