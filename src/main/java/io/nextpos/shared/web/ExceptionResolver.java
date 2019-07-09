package io.nextpos.shared.web;

import io.nextpos.shared.exception.ConfigurationException;
import io.nextpos.shared.exception.ObjectAlreadyExistsException;
import io.nextpos.shared.exception.ObjectNotFoundException;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class ExceptionResolver {

    @ExceptionHandler(ObjectNotFoundException.class)
    @ResponseStatus(code = HttpStatus.NOT_FOUND)
    public ErrorResponse handleObjectNotFound(ObjectNotFoundException exception) {

        return ErrorResponse.simpleErrorResponse(exception.getMessage());
    }

    @ExceptionHandler(ObjectAlreadyExistsException.class)
    @ResponseStatus(code = HttpStatus.NOT_FOUND)
    public ErrorResponse handleObjectAlreadyExist(ObjectAlreadyExistsException exception) {

        return ErrorResponse.simpleErrorResponse(exception.getMessage());
    }

    @ExceptionHandler(ConfigurationException.class)
    @ResponseStatus(code = HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorResponse handleConfigurationException(ConfigurationException exception) {

        return ErrorResponse.simpleErrorResponse(exception.getMessage());
    }

    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(code = HttpStatus.BAD_REQUEST)
    public ErrorResponse handleConstraintViolationException(ConstraintViolationException exception) {
        return ErrorResponse.simpleErrorResponse("Request cannot be completed due to some constraint violation: " + exception.getConstraintName());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleValidationExceptions(MethodArgumentNotValidException ex) {

        final BindingResult bindingResult = ex.getBindingResult();
        final HashMap<String, String> fieldErrors = bindingResult.getAllErrors().stream()
                .map(error -> new AbstractMap.SimpleEntry<>(((FieldError) error).getField(), error.getDefaultMessage()))
                .collect(HashMap::new,
                        (map, entry) -> map.put(entry.getKey(), entry.getValue()),
                        HashMap::putAll
                );

        final String errorMessage = "Validation failed for object='" + bindingResult.getObjectName() + "'. Error count: " + bindingResult.getErrorCount();

        return new ErrorResponse(errorMessage, fieldErrors, null, Instant.now());
    }


    @Data
    @AllArgsConstructor
    private static class ErrorResponse {

        private String message;

        private Map<String, String> fieldErrors;

        private String details;

        private Instant timestamp;

        static ErrorResponse simpleErrorResponse(String message) {
            return new ErrorResponse(message, null, "NA", Instant.now());
        }


    }
}
