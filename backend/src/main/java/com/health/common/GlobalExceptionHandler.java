package com.health.common;

import java.util.stream.Stream;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(BadRequestException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> badRequest(BadRequestException exception) {
        return ApiResponse.error(firstNonBlank(Stream.of(exception.getMessage())));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> validation(MethodArgumentNotValidException exception) {
        return ApiResponse.error(firstNonBlank(exception.getBindingResult().getFieldErrors().stream()
                .map(MessageSourceResolvable::getDefaultMessage)));
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> methodValidation(HandlerMethodValidationException exception) {
        return ApiResponse.error(firstNonBlank(exception.getAllValidationResults().stream()
                .flatMap(result -> result.getResolvableErrors().stream())
                .map(MessageSourceResolvable::getDefaultMessage)));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> constraintViolation(ConstraintViolationException exception) {
        return ApiResponse.error(firstNonBlank(exception.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)));
    }

    private static String firstNonBlank(Stream<String> messages) {
        return messages
                .filter(StringUtils::hasText)
                .findFirst()
                .orElse("请求参数不合法");
    }
}
