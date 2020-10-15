package io.nextpos.shared.web;

import io.nextpos.shared.exception.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.hibernate.exception.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.sql.SQLIntegrityConstraintViolationException;
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
    @ResponseStatus(code = HttpStatus.CONFLICT)
    public ErrorResponse handleObjectAlreadyExist(ObjectAlreadyExistsException exception) {

        return ErrorResponse.simpleErrorResponse("message.alreadyExists", exception.getMessage());
    }

    @ExceptionHandler(ConfigurationException.class)
    @ResponseStatus(code = HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorResponse handleConfigurationException(ConfigurationException exception) {

        return ErrorResponse.simpleErrorResponse(exception.getMessage());
    }

    @ExceptionHandler({GeneralApplicationException.class, ClientAccountException.class, ClientOwnershipViolationException.class})
    @ResponseStatus(code = HttpStatus.BAD_REQUEST)
    public ErrorResponse handleGeneralApplicationException(Exception exception) {

        return ErrorResponse.simpleErrorResponse(exception.getMessage());
    }

    @ExceptionHandler(BusinessLogicException.class)
    @ResponseStatus(code = HttpStatus.PRECONDITION_FAILED)
    public ErrorResponse handleBusinessLogicException(BusinessLogicException exception) {

        return ErrorResponse.simpleErrorResponse(exception.getLocalizedMessageKey(), exception.getMessage());
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolationException(ConstraintViolationException exception) {

        LOGGER.error("{}", exception.getMessage(), exception);

        if (exception.getCause() instanceof SQLIntegrityConstraintViolationException) {
            final ErrorResponse errorResponse = ErrorResponse.simpleErrorResponse("message.alreadyExists", "Object with name already exists");
            errorResponse.setDetails(exception.getSQLException().getMessage());

            return new ResponseEntity<>(errorResponse, HttpStatus.CONFLICT);

        } else {
            final ErrorResponse errorResponse = ErrorResponse.simpleErrorResponse("Database constraint violation.");
            errorResponse.setDetails(exception.getSQLException().getMessage());

            return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * https://www.baeldung.com/spring-boot-bean-validation
     *
     * Object level error is detected and set on the details section of error response.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleValidationExceptions(MethodArgumentNotValidException ex) {

        LOGGER.error("{}", ex.getMessage(), ex);

        final BindingResult bindingResult = ex.getBindingResult();
        final HashMap<String, ErrorResponse.FieldLevelError> fieldErrors = bindingResult.getAllErrors().stream()
                .filter(error -> error instanceof FieldError)
                .map(error -> {
                    final String fieldName = ((FieldError) error).getField();
                    return new AbstractMap.SimpleEntry<>(fieldName, new ErrorResponse.FieldLevelError("message." + fieldName, error.getDefaultMessage()));
                })
                .collect(HashMap::new,
                        (map, entry) -> map.put(entry.getKey(), entry.getValue()),
                        HashMap::putAll
                );

        StringBuilder details = new StringBuilder();
        bindingResult.getAllErrors().stream()
                .filter(error -> !(error instanceof FieldError))
                .findFirst().ifPresent(error -> details.append(error.getDefaultMessage()));

        final String errorMessage = "Validation failed for object='" + bindingResult.getObjectName() + "'. Error count: " + bindingResult.getErrorCount();

        return new ErrorResponse(errorMessage, null, fieldErrors, details.toString(), Instant.now());
    }


    @Data
    @AllArgsConstructor
    private static class ErrorResponse {

        private String message;

        private String localizedMessageKey;

        private Map<String, FieldLevelError> fieldErrors;

        private String details;

        private Instant timestamp;

        static ErrorResponse simpleErrorResponse(String message) {
            return new ErrorResponse(message, null, Collections.emptyMap(), "NA", Instant.now());
        }

        static ErrorResponse simpleErrorResponse(String localizedMessageKey, String message) {
            return new ErrorResponse(message, localizedMessageKey, Collections.emptyMap(), "NA", Instant.now());
        }

        @Data
        @AllArgsConstructor
        private static class FieldLevelError {

            private String localizedMessageKey;
            
            private String message;
        }
    }
}
