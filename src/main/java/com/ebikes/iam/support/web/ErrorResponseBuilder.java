package com.ebikes.iam.support.web;

import com.ebikes.iam.constants.ApplicationConstants;
import com.ebikes.iam.constants.MDCKeys;
import com.ebikes.iam.dtos.responses.api.ErrorResponse;
import com.ebikes.iam.dtos.responses.api.ErrorResponse.ErrorDetail;
import com.ebikes.iam.enums.ResponseCode;
import com.ebikes.iam.support.references.ReferenceGenerator;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

public final class ErrorResponseBuilder {

  private ErrorResponseBuilder() {
    // prevent instantiation
  }

  public static ErrorResponse buildErrorResponse(
      String detail, String requestPath, ResponseCode responseCode) {
    String errorReference = ReferenceGenerator.generateErrorReference();
    MDC.put(MDCKeys.ERROR_REFERENCE, errorReference);
    return ErrorResponse.from(detail, errorReference, requestPath, responseCode);
  }

  public static ErrorResponse buildErrorResponseWithErrors(
      String detail, List<ErrorDetail> errors, String requestPath, ResponseCode responseCode) {
    String errorReference = ReferenceGenerator.generateErrorReference();
    MDC.put(MDCKeys.ERROR_REFERENCE, errorReference);
    return ErrorResponse.withErrors(detail, errorReference, errors, requestPath, responseCode);
  }

  public static ResponseEntity<ErrorResponse> buildResponse(
      ErrorResponse errorResponse, HttpStatus status) {
    return ResponseEntity.status(status)
        .header(HttpHeaders.CONTENT_TYPE, ApplicationConstants.PROBLEM_JSON_MEDIA_TYPE)
        .body(errorResponse);
  }
}
