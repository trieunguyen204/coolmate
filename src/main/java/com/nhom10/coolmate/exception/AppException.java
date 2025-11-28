package com.nhom10.coolmate.exception;


public class AppException extends RuntimeException {


    public AppException(String message) {
        super(message);
    }

    public AppException(String message, Throwable cause) {
        super(message, cause);
    }
}