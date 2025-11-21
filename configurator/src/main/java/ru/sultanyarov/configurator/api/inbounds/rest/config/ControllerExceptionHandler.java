package ru.sultanyarov.configurator.api.inbounds.rest.config;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import ru.sultanyarov.configurator.domain.dto.ErrorResponse;
import ru.sultanyarov.configurator.domain.exception.BusinessException;
import ru.sultanyarov.configurator.domain.exception.EntityAlreadyExistsException;
import ru.sultanyarov.configurator.domain.exception.NotFoundException;
import ru.sultanyarov.configurator.domain.exception.ValidationException;

import java.time.OffsetDateTime;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@RestControllerAdvice
public class ControllerExceptionHandler {
    @ExceptionHandler({BusinessException.class, Exception.class})
    public ResponseEntity<?> handleBusinessException(Exception exception) {
        return getBody(INTERNAL_SERVER_ERROR, exception);
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<?> handleNotFoundException(NotFoundException exception) {
        return getBody(NOT_FOUND, exception);
    }

    @ExceptionHandler(EntityAlreadyExistsException.class)
    public ResponseEntity<?> handleEntityAlreadyExistsException(EntityAlreadyExistsException exception) {
        return getBody(CONFLICT, exception);
    }

    @ExceptionHandler({ValidationException.class, MethodArgumentNotValidException.class})
    public ResponseEntity<?> handleValidationException(Exception exception) {
        return getBody(BAD_REQUEST, exception);
    }


    private static ResponseEntity<ErrorResponse> getBody(HttpStatus httpStatus, Exception exception) {
        return ResponseEntity.status(httpStatus)
                .body(
                        new ErrorResponse(
                                OffsetDateTime.now(),
                                httpStatus.value(),
                                httpStatus.getReasonPhrase(),
                                exception.getLocalizedMessage(),
                                null
                        )
                );
    }
}
