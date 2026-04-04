package backend.academy.linktracker.scrapper.exception.handler;

import backend.academy.linktracker.contract.dto.error.ApiErrorResponse;
import backend.academy.linktracker.scrapper.exception.chat.TelegramChatAlreadyExistsException;
import backend.academy.linktracker.scrapper.exception.chat.TelegramChatNotFoundException;
import backend.academy.linktracker.scrapper.exception.client.RepositoryPollingException;
import backend.academy.linktracker.scrapper.exception.link.UnsupportedLinkException;
import backend.academy.linktracker.scrapper.exception.subscription.SubscriptionAlreadyExistsException;
import backend.academy.linktracker.scrapper.exception.subscription.SubscriptionNotFoundException;
import backend.academy.linktracker.scrapper.logging.LogEvent;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.util.Arrays;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
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

    @ExceptionHandler({SubscriptionNotFoundException.class})
    public ResponseEntity<ApiErrorResponse> handleNotFound(RuntimeException ex) {
        return build(HttpStatus.NOT_FOUND, "Ресурс не найден", ex);
    }

    @ExceptionHandler(TelegramChatNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleTelegramChatNotFound(
            TelegramChatNotFoundException ex, HttpServletRequest request) {
        if (isTelegramChatEndpoint(request)) {
            log.atInfo()
                    .addKeyValue("event", LogEvent.TELEGRAM_CHAT_UNREGISTER_FAILED)
                    .addKeyValue("reason", "chat_not_found")
                    .addKeyValue("exception", ex.getClass().getSimpleName())
                    .addKeyValue("path", request.getRequestURI())
                    .log("Telegram chat unregistration rejected");
        }
        return build(HttpStatus.NOT_FOUND, "Ресурс не найден", ex);
    }

    @ExceptionHandler({SubscriptionAlreadyExistsException.class})
    public ResponseEntity<ApiErrorResponse> handleConflict(RuntimeException ex) {
        return build(HttpStatus.CONFLICT, "Конфликт состояния ресурса", ex);
    }

    @ExceptionHandler(TelegramChatAlreadyExistsException.class)
    public ResponseEntity<ApiErrorResponse> handleTelegramChatAlreadyExists(
            TelegramChatAlreadyExistsException ex, HttpServletRequest request) {
        if (isTelegramChatEndpoint(request)) {
            log.atInfo()
                    .addKeyValue("event", LogEvent.TELEGRAM_CHAT_REGISTER_FAILED)
                    .addKeyValue("reason", "chat_already_exists")
                    .addKeyValue("exception", ex.getClass().getSimpleName())
                    .addKeyValue("path", request.getRequestURI())
                    .log("Telegram chat registration rejected");
        }
        return build(HttpStatus.CONFLICT, "Конфликт состояния ресурса", ex);
    }

    @ExceptionHandler(RepositoryPollingException.class)
    public ResponseEntity<ApiErrorResponse> handleRepositoryPollingException(
            RepositoryPollingException ex, HttpServletRequest request) {
        log.atWarn()
                .setCause(ex)
                .addKeyValue("event", LogEvent.REPOSITORY_POLLING_FAILED)
                .addKeyValue("method", request.getMethod())
                .addKeyValue("path", request.getRequestURI())
                .addKeyValue("status", HttpStatus.BAD_GATEWAY.value())
                .addKeyValue("exception", ex.getClass().getSimpleName())
                .log("Repository polling failed while handling request");

        ApiErrorResponse response = new ApiErrorResponse(
                "Repository polling failed",
                "Failed to poll external repository",
                ex.getClass().getSimpleName(),
                ex.getMessage(),
                List.of());

        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpected(Exception ex, HttpServletRequest request) {
        log.atError()
                .setCause(ex)
                .addKeyValue("event", LogEvent.REQUEST_PROCESSING_FAILED)
                .addKeyValue("method", request.getMethod())
                .addKeyValue("path", request.getRequestURI())
                .addKeyValue("status", HttpStatus.INTERNAL_SERVER_ERROR.value())
                .addKeyValue("exception", ex.getClass().getSimpleName())
                .log("Unexpected error while handling request");

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

    private boolean isTelegramChatEndpoint(HttpServletRequest request) {
        return request.getRequestURI().startsWith("/tg-chat/");
    }
}
