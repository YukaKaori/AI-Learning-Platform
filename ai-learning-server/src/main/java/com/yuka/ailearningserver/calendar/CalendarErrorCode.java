package com.yuka.ailearningserver.calendar;

import com.yuka.ailearningserver.common.api.ErrorCode;
import org.springframework.http.HttpStatus;

/** Calendar module error codes — reserved range 160000–169999. */
public enum CalendarErrorCode implements ErrorCode {

    SESSION_NOT_FOUND(160000, "Study session not found", HttpStatus.NOT_FOUND),
    SESSION_ACCESS_DENIED(160001, "Study session does not belong to the current user", HttpStatus.FORBIDDEN),
    SESSION_TIME_INVALID(160002, "Study session must end after it starts", HttpStatus.BAD_REQUEST),
    SESSION_WINDOW_INVALID(160003, "Query window requires from < to", HttpStatus.BAD_REQUEST);

    private final int code;
    private final String message;
    private final HttpStatus httpStatus;

    CalendarErrorCode(int code, String message, HttpStatus httpStatus) {
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
