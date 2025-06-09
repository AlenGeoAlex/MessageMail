package me.alenalex.messageforwarder.dto;

public abstract class BaseResponse {

    private boolean success;
    private String error;
    private Exception exception;

    public BaseResponse(boolean success, String error) {
        this.success = success;
        this.error = error;
    }

    public BaseResponse(boolean success, String error, Exception exception) {
        this.success = success;
        this.error = error;
        this.exception = exception;
    }

    public BaseResponse() {
        this.success = true;
    }

    public boolean success() {
        return success;
    }

    public String error() {
        return error;
    }

    public Exception exception() {
        return exception;
    }
}
