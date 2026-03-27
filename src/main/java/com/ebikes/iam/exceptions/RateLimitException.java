package com.ebikes.iam.exceptions;

import java.io.Serial;

import com.ebikes.iam.enums.ResponseCode;

public class RateLimitException extends BaseException {

  @Serial private static final long serialVersionUID = 1L;

  public RateLimitException(ResponseCode code, String developerMessage) {
    super(code, developerMessage);
  }
}
