package com.ebikes.iam.exceptions;

import com.ebikes.iam.enums.ResponseCode;

import java.io.Serial;

public class BusinessRuleException extends BaseException {

    @Serial
    private static final long serialVersionUID = 1L;

    public BusinessRuleException(ResponseCode code, String developerMessage) {
        super(code, developerMessage);
    }

    public BusinessRuleException(ResponseCode code, String developerMessage, Throwable cause) {
        super(code, developerMessage, cause);
    }
}
