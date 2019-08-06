package io.nextpos.shared.web;

import io.nextpos.shared.exception.ConfigurationException;
import io.nextpos.shared.exception.GeneralApplicationException;
import io.nextpos.shared.exception.ObjectAlreadyExistsException;
import io.nextpos.shared.exception.ObjectNotFoundException;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.hibernate.exception.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.AbstractMap;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class ExceptionResolver {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExceptionResolver.class);


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

    @ExceptionHandler(GeneralApplicationException.class)
    @ResponseStatus(code = HttpStatus.BAD_REQUEST)
    public ErrorResponse handleGeneralApplicationException(GeneralApplicationException exception) {

        return ErrorResponse.simpleErrorResponse(exception.getMessage());
    }

    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(code = HttpStatus.BAD_REQUEST)
    public ErrorResponse handleConstraintViolationException(ConstraintViolationException exception) {

        LOGGER.error("{}", exception.getMessage(), exception);
        final ErrorResponse errorResponse = ErrorResponse.simpleErrorResponse("Request cannot be completed. Please check your payload or provide x-request-id to support team for further diagnosis.");
        errorResponse.setDetails(exception.getSQLException().getMessage());

        return errorResponse;
    }

    /**
     * https://www.baeldung.com/spring-boot-bean-validation
     *
     * Object level error is detected and set on the details section of error response.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleValidationExceptions(MethodArgumentNotValidException ex) {

        final BindingResult bindingResult = ex.getBindingResult();
        final HashMap<String, String> fieldErrors = bindingResult.getAllErrors().stream()
                .filter(error -> error instanceof FieldError)
                .map(error -> new AbstractMap.SimpleEntry<>(((FieldError) error).getField(), error.getDefaultMessage()))
                .collect(HashMap::new,
                        (map, entry) -> map.put(entry.getKey(), entry.getValue()),
                        HashMap::putAll
                );

        StringBuilder details = new StringBuilder();
        bindingResult.getAllErrors().stream()
                .filter(error -> !(error instanceof FieldError))
                .findFirst().ifPresent(error -> details.append(error.getDefaultMessage()));

        final String errorMessage = "Validation failed for object='" + bindingResult.getObjectName() + "'. Error count: " + bindingResult.getErrorCount();

        return new ErrorResponse(errorMessage, fieldErrors, details.toString(), Instant.now());
    }


    @Data
    @AllArgsConstructor
    private static class ErrorResponse {

        private String message;

        private Map<String, String> fieldErrors;

        private String details;

        private Instant timestamp;

        static ErrorResponse simpleErrorResponse(String message) {
            return new ErrorResponse(message, Collections.emptyMap(), "NA", Instant.now());
        }


    }
}
