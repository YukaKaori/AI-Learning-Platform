package com.yuka.ailearningserver.subject;

import com.yuka.ailearningserver.common.api.ErrorCode;
import org.springframework.http.HttpStatus;

/** Subject module error codes — reserved range 110000–119999. */
public enum SubjectErrorCode implements ErrorCode {

    SUBJECT_NOT_FOUND(110000, "Subject not found", HttpStatus.NOT_FOUND),
    SUBJECT_ACCESS_DENIED(110001, "Subject does not belong to the current user", HttpStatus.FORBIDDEN),
    SUBJECT_STATUS_INVALID(110002, "Unknown subject status", HttpStatus.BAD_REQUEST);

    private final int code;
    private final String message;
    private final HttpStatus httpStatus;

    SubjectErrorCode(int code, String message, HttpStatus httpStatus) {
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
    }

    @Override
    public int code() {
        return code;
    }

    @Override
    public String message() {
        return message;
    }

    @Override
    public HttpStatus httpStatus() {
        return httpStatus;
    }
}
