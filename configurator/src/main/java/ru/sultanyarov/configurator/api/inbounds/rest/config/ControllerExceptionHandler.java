package ru.sultanyarov.configurator.api.inbounds.rest.config;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import ru.sultanyarov.configurator.domain.dto.ErrorResponse;
import ru.sultanyarov.configurator.domain.exception.BusinessException;

import java.time.OffsetDateTime;

import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;

@RestControllerAdvice
public class ControllerExceptionHandler {
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<?> handleBusinessException(BusinessException exception) {
        return ResponseEntity.status(INTERNAL_SERVER_ERROR)
                .body(
                        new ErrorResponse(
                                OffsetDateTime.now(),
                                INTERNAL_SERVER_ERROR.value(),
                                INTERNAL_SERVER_ERROR.getReasonPhrase(),
                                exception.getLocalizedMessage(),
                                null
                        )
                );
    }
}
