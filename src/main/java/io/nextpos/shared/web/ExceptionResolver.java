package io.nextpos.shared.web;

import io.nextpos.shared.exception.ConfigurationException;
import io.nextpos.shared.exception.ObjectNotFoundException;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ExceptionResolver {

    @ExceptionHandler(ObjectNotFoundException.class)
    @ResponseStatus(code = HttpStatus.NOT_FOUND)
    public ErrorResponse handleObjectNotFound(ObjectNotFoundException exception) {

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
        return new ErrorResponse("Request cannot be completed due to some constraint violation.",
                exception.getConstraintName());
    }


    @Data
    @AllArgsConstructor
    private static class ErrorResponse {

        private String message;

        private String details;

        static ErrorResponse simpleErrorResponse(String message) {
            return new ErrorResponse(message, "NA");
        }
    }
}
