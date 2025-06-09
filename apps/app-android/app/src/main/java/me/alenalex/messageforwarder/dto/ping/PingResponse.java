package me.alenalex.messageforwarder.dto.ping;

import me.alenalex.messageforwarder.dto.BaseResponse;

public class PingResponse extends BaseResponse {

    public PingResponse(boolean success, String error) {
        super(success, error);
    }

    public PingResponse(boolean success, String error, Exception exception) {
        super(success, error, exception);
    }

    public PingResponse() {
    }
}
