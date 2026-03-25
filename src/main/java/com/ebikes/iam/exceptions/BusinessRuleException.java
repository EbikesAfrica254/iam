package com.ebikes.iam.exceptions;

import java.io.Serial;

import com.ebikes.iam.enums.ResponseCode;

public class BusinessRuleException extends BaseException {

  @Serial private static final long serialVersionUID = 1L;

  public BusinessRuleException(ResponseCode code, String developerMessage) {
    super(code, developerMessage);
  }
}
