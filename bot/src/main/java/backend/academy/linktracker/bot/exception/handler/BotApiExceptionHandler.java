package backend.academy.linktracker.bot.exception.handler;

import backend.academy.linktracker.contract.dto.error.ApiErrorResponse;
import jakarta.validation.ConstraintViolationException;
import java.util.Arrays;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class BotApiExceptionHandler {
    @ExceptionHandler({
        ConstraintViolationException.class,
        IllegalArgumentException.class,
        MethodArgumentNotValidException.class,
        HttpMessageNotReadableException.class
    })
    public ResponseEntity<ApiErrorResponse> handleConstraintViolation(Exception ex) {
        return build(HttpStatus.BAD_REQUEST, "Incorrect request parameters", ex);
    }

    private ResponseEntity<ApiErrorResponse> build(HttpStatus status, String description, Exception ex) {
        ApiErrorResponse response = new ApiErrorResponse(
                description,
                String.valueOf(status.value()),
                ex.getClass().getSimpleName(),
                ex.getMessage(),
                Arrays.stream(ex.getStackTrace())
                        .map(StackTraceElement::toString)
                        .toList());

        return ResponseEntity.status(status).body(response);
    }
}
