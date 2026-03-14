package backend.academy.linktracker.scrapper.exception.handler;

import backend.academy.linktracker.contract.dto.error.ApiErrorResponse;
import backend.academy.linktracker.scrapper.exception.chat.TelegramChatAlreadyExistsException;
import backend.academy.linktracker.scrapper.exception.chat.TelegramChatNotFoundException;
import backend.academy.linktracker.scrapper.exception.client.RepositoryPollingException;
import backend.academy.linktracker.scrapper.exception.link.TrackingStateAlreadyExistsException;
import backend.academy.linktracker.scrapper.exception.link.TrackingStateNotFoundException;
import backend.academy.linktracker.scrapper.exception.link.UnsupportedLinkException;
import backend.academy.linktracker.scrapper.exception.subscription.SubscriptionAlreadyExistsException;
import backend.academy.linktracker.scrapper.exception.subscription.SubscriptionNotFoundException;
import jakarta.validation.ConstraintViolationException;
import java.util.Arrays;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler({
        MethodArgumentNotValidException.class,
        HandlerMethodValidationException.class,
        ConstraintViolationException.class,
        HttpMessageNotReadableException.class,
        MethodArgumentTypeMismatchException.class,
        MissingRequestHeaderException.class,
        BindException.class,
        IllegalArgumentException.class,
        UnsupportedLinkException.class,
    })
    public ResponseEntity<ApiErrorResponse> handleBadRequest(Exception ex) {
        return build(HttpStatus.BAD_REQUEST, "Некорректные параметры запроса", ex);
    }

    @ExceptionHandler({
        TelegramChatNotFoundException.class,
        SubscriptionNotFoundException.class,
        TrackingStateNotFoundException.class
    })
    public ResponseEntity<ApiErrorResponse> handleNotFound(RuntimeException ex) {
        return build(HttpStatus.NOT_FOUND, "Ресурс не найден", ex);
    }

    @ExceptionHandler({
        TelegramChatAlreadyExistsException.class,
        SubscriptionAlreadyExistsException.class,
        TrackingStateAlreadyExistsException.class
    })
    public ResponseEntity<ApiErrorResponse> handleConflict(RuntimeException ex) {
        return build(HttpStatus.CONFLICT, "Конфликт состояния ресурса", ex);
    }

    @ExceptionHandler(RepositoryPollingException.class)
    public ResponseEntity<ApiErrorResponse> handleRepositoryPollingException(RepositoryPollingException ex) {
        ApiErrorResponse response = new ApiErrorResponse(
                "Repository polling failed",
                "Failed to poll external repository",
                ex.getClass().getSimpleName(),
                ex.getMessage(),
                List.of());

        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpected(Exception ex) {
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Внутренняя ошибка сервиса", ex);
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
