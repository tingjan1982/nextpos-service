package io.nextpos.shared.web;

import io.nextpos.shared.exception.ObjectNotFoundException;
import lombok.AllArgsConstructor;
import lombok.Data;
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
