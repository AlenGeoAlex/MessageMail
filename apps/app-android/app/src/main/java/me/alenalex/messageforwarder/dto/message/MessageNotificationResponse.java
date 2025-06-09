package me.alenalex.messageforwarder.dto.message;

import me.alenalex.messageforwarder.dto.BaseResponse;

public class MessageNotificationResponse extends BaseResponse {

    private boolean shouldRetry = false;

    public MessageNotificationResponse(boolean success, String error, boolean shouldRetry) {
        super(success, error);
        this.shouldRetry = shouldRetry;
    }

    public MessageNotificationResponse(boolean success, String error) {
        super(success, error);
    }

    public MessageNotificationResponse(boolean success, String error, Exception exception) {
        super(success, error, exception);
    }

    public MessageNotificationResponse() {
    }

    public boolean shouldRetry() {
        return shouldRetry;
    }
}
