package com.ebikes.iam.exceptions.handlers;

import com.ebikes.iam.dtos.responses.api.ErrorResponse;
import com.ebikes.iam.dtos.responses.api.ErrorResponse.ErrorDetail;
import com.ebikes.iam.enums.ResponseCode;
import com.ebikes.iam.exceptions.BaseException;
import com.ebikes.iam.exceptions.ExternalServiceException;
import com.ebikes.iam.exceptions.ValidationException;
import com.ebikes.iam.support.web.ErrorResponseBuilder;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(BaseException.class)
    public ResponseEntity<ErrorResponse> handleBaseException(
            BaseException ex, HttpServletRequest request) {

        logByResponseCode(ex.getResponseCode(), ex.getMessage(), request.getRequestURI(), ex);

        ErrorResponse error =
                ErrorResponseBuilder.buildErrorResponse(
                        ex.getResponseCode().getUserMessage(), request.getRequestURI(), ex.getResponseCode());

        return ErrorResponseBuilder.buildResponse(error, ex.getResponseCode().getHttpStatus());
    }

    @ExceptionHandler(ExternalServiceException.class)
    public ResponseEntity<ErrorResponse> handleExternalServiceException(
            ExternalServiceException ex, HttpServletRequest request) {

        log.error(
                "External service error: endpoint={}, code={}, message={}, path={}",
                ex.getEndpoint(),
                ex.getResponseCode().getCode(),
                ex.getMessage(),
                request.getRequestURI(),
                ex);

        ErrorResponse error =
                ErrorResponseBuilder.buildErrorResponse(
                        ex.getResponseCode().getUserMessage(), request.getRequestURI(), ex.getResponseCode());

        return ErrorResponseBuilder.buildResponse(error, ex.getResponseCode().getHttpStatus());
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            ValidationException ex, HttpServletRequest request) {

        log.warn(
                "Validation error: field={}, rejectedValue={}, message={}, path={}",
                ex.getField(),
                ex.getRejectedValue(),
                ex.getMessage(),
                request.getRequestURI());

        List<ErrorDetail> errors =
                List.of(new ErrorDetail(ex.getField(), ex.getMessage(), ex.getRejectedValue()));

        ErrorResponse error =
                ErrorResponseBuilder.buildErrorResponseWithErrors(
                        ex.getResponseCode().getUserMessage(),
                        errors,
                        request.getRequestURI(),
                        ex.getResponseCode());

        return ErrorResponseBuilder.buildResponse(error, ex.getResponseCode().getHttpStatus());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadableException(
            HttpMessageNotReadableException ex, HttpServletRequest request) {

        log.warn(
                "Malformed request body: message={}, path={}", ex.getMessage(), request.getRequestURI());

        ErrorResponse error =
                ErrorResponseBuilder.buildErrorResponse(
                        ResponseCode.INVALID_FORMAT.getUserMessage(),
                        request.getRequestURI(),
                        ResponseCode.INVALID_FORMAT);

        return ErrorResponseBuilder.buildResponse(error, ResponseCode.INVALID_FORMAT.getHttpStatus());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValidException(
            MethodArgumentNotValidException ex, HttpServletRequest request) {

        List<ErrorDetail> errors =
                ex.getBindingResult().getFieldErrors().stream()
                        .map(
                                fe -> new ErrorDetail(fe.getField(), fe.getDefaultMessage(), fe.getRejectedValue()))
                        .toList();

        log.warn(
                "Argument validation failed: errorCount={}, path={}",
                errors.size(),
                request.getRequestURI());

        ErrorResponse error =
                ErrorResponseBuilder.buildErrorResponseWithErrors(
                        ResponseCode.INVALID_ARGUMENTS.getUserMessage(),
                        errors,
                        request.getRequestURI(),
                        ResponseCode.INVALID_ARGUMENTS);

        return ErrorResponseBuilder.buildResponse(
                error, ResponseCode.INVALID_ARGUMENTS.getHttpStatus());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentTypeMismatchException(
            MethodArgumentTypeMismatchException ex, HttpServletRequest request) {

        log.warn(
                "Type mismatch: parameter={}, value={}, path={}",
                ex.getParameter().getParameterName(),
                ex.getValue(),
                request.getRequestURI());

        List<ErrorDetail> errors =
                List.of(
                        new ErrorDetail(
                                ex.getParameter().getParameterName(),
                                "Invalid value for parameter",
                                ex.getValue()));

        ErrorResponse error =
                ErrorResponseBuilder.buildErrorResponseWithErrors(
                        ResponseCode.INVALID_ARGUMENTS.getUserMessage(),
                        errors,
                        request.getRequestURI(),
                        ResponseCode.INVALID_ARGUMENTS);

        return ErrorResponseBuilder.buildResponse(
                error, ResponseCode.INVALID_ARGUMENTS.getHttpStatus());
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResourceFoundException(
            NoResourceFoundException ex, HttpServletRequest request) {

        log.warn("No resource found: message={}, path={}", ex.getMessage(), request.getRequestURI());

        ErrorResponse error =
                ErrorResponseBuilder.buildErrorResponse(
                        ResponseCode.RESOURCE_NOT_FOUND.getUserMessage(),
                        request.getRequestURI(),
                        ResponseCode.RESOURCE_NOT_FOUND);

        return ErrorResponseBuilder.buildResponse(
                error, ResponseCode.RESOURCE_NOT_FOUND.getHttpStatus());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception ex, HttpServletRequest request) {

        log.error(
                "Unexpected error: message={}, path={}", ex.getMessage(), request.getRequestURI(), ex);

        ErrorResponse error =
                ErrorResponseBuilder.buildErrorResponse(
                        ResponseCode.INTERNAL_SERVER_ERROR.getUserMessage(),
                        request.getRequestURI(),
                        ResponseCode.INTERNAL_SERVER_ERROR);

        return ErrorResponseBuilder.buildResponse(
                error, ResponseCode.INTERNAL_SERVER_ERROR.getHttpStatus());
    }

    private void logByResponseCode(
            ResponseCode responseCode, String message, String path, Exception ex) {
        if (responseCode.getHttpStatus().is5xxServerError()) {
            log.error(
                    "Server error: code={}, message={}, path={}", responseCode.getCode(), message, path, ex);
        } else {
            log.warn("Client error: code={}, message={}, path={}", responseCode.getCode(), message, path);
        }
    }
}
