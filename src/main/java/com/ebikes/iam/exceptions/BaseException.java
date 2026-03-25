package com.ebikes.iam.exceptions;

import com.ebikes.iam.enums.ResponseCode;
import lombok.Getter;

import java.io.Serial;

@Getter
public abstract class BaseException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    private final ResponseCode responseCode;

    protected BaseException(ResponseCode responseCode, String developerMessage) {
        super(developerMessage);
        this.responseCode = responseCode;
    }

    protected BaseException(ResponseCode responseCode, String developerMessage, Throwable cause) {
        super(developerMessage, cause);
        this.responseCode = responseCode;
    }
}
