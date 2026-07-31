package ru.sultanyarov.configurator.api.inbounds.rest.advice;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.PAYLOAD_TOO_LARGE;
import static org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE;
import static org.springframework.http.HttpStatus.UNSUPPORTED_MEDIA_TYPE;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.time.LocalDateTime;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
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
    return getBody(INTERNAL_SERVER_ERROR, exception.getLocalizedMessage(), request);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleUnexpectedException(
      Exception exception, HttpServletRequest request) {
    log.error("Unexpected error while processing {}", request.getRequestURI(), exception);
    return getBody(INTERNAL_SERVER_ERROR, "Internal server error", request);
  }

  @ExceptionHandler(NotFoundException.class)
  public ResponseEntity<ErrorResponse> handleNotFoundException(
      NotFoundException exception, HttpServletRequest request) {
    return getBody(NOT_FOUND, exception.getLocalizedMessage(), request);
  }

  @ExceptionHandler({
    EntityAlreadyExistsException.class,
    EntityHasRelatedEntitiesException.class,
    ComponentArchivedException.class,
    ConfigurationConflictException.class
  })
  public ResponseEntity<ErrorResponse> handleEntityAlreadyExistsException(
      Exception exception, HttpServletRequest request) {
    return getBody(CONFLICT, exception.getLocalizedMessage(), request);
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
    return getBody(BAD_REQUEST, validationMessage(exception), request);
  }

  @ExceptionHandler({ImageTooLargeException.class, MaxUploadSizeExceededException.class})
  public ResponseEntity<ErrorResponse> handlePayloadTooLargeException(
      Exception exception, HttpServletRequest request) {
    return getBody(PAYLOAD_TOO_LARGE, exception.getLocalizedMessage(), request);
  }

  @ExceptionHandler(UnsupportedImageFormatException.class)
  public ResponseEntity<ErrorResponse> handleUnsupportedImageFormatException(
      UnsupportedImageFormatException exception, HttpServletRequest request) {
    return getBody(UNSUPPORTED_MEDIA_TYPE, exception.getLocalizedMessage(), request);
  }

  @ExceptionHandler(ExternalStorageException.class)
  public ResponseEntity<ErrorResponse> handleExternalStorageException(
      ExternalStorageException exception, HttpServletRequest request) {
    log.error("External storage failed for {}", request.getRequestURI(), exception);
    return getBody(SERVICE_UNAVAILABLE, "External storage is temporarily unavailable", request);
  }

  private static String validationMessage(Exception exception) {
    if (exception instanceof MethodArgumentNotValidException validationException) {
      return validationException.getBindingResult().getFieldErrors().stream()
          .map(error -> "%s: %s".formatted(error.getField(), error.getDefaultMessage()))
          .distinct()
          .sorted()
          .collect(java.util.stream.Collectors.joining("; "));
    }
    if (exception instanceof BindException bindException) {
      return bindException.getBindingResult().getFieldErrors().stream()
          .map(error -> "%s: %s".formatted(error.getField(), error.getDefaultMessage()))
          .distinct()
          .sorted()
          .collect(java.util.stream.Collectors.joining("; "));
    }
    return exception.getLocalizedMessage();
  }

  private static ResponseEntity<ErrorResponse> getBody(
      HttpStatus httpStatus, String message, HttpServletRequest request) {
    return ResponseEntity.status(httpStatus)
        .body(
            new ErrorResponse(
                LocalDateTime.now(),
                httpStatus.value(),
                httpStatus.getReasonPhrase(),
                message,
                request.getRequestURI()));
  }
}
