package com.twilight.pointquestbackend.common;

/**
 * Custom exception carrying an error code for unified responses.
 */
public class ServiceException extends RuntimeException {
    private final int code;

    public ServiceException(int code, String message) {
        super(message);
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
