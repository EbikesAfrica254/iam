package com.ebikes.iam.exceptions;

import com.ebikes.iam.enums.ResponseCode;

import java.io.Serial;

public class DuplicateResourceException extends BaseException {

  @Serial private static final long serialVersionUID = 1L;

  public DuplicateResourceException(ResponseCode code, String developerMessage) {
    super(code, developerMessage);
  }
}
