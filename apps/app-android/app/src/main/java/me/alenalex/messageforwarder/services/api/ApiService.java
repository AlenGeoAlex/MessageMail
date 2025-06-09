package me.alenalex.messageforwarder.services.api;

import android.util.Log;

import java.io.IOException;
import java.util.Objects;

import me.alenalex.messageforwarder.dto.message.MessageNotificationCommand;
import me.alenalex.messageforwarder.dto.message.MessageNotificationResponse;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class ApiService extends AbstractNetworkService {
    public ApiService(OkHttpClient client) {
        super(client);
    }

    public void ping(WebRequestOptions options, Callback callback){
        Request request = this.post(options, "");
        this.queue(request, callback);
    }

    public MessageNotificationResponse message(WebRequestOptions options, MessageNotificationCommand command) {
        Request request = this.post(options.apiUrl(options.apiUrl()+"/notifications/message"), command.toJson());
        try (Response response = executeRequest(request)) {
            if(response.isSuccessful())
                return new MessageNotificationResponse();

            ResponseBody body = response.body();
            String message = response.message();
            if(body != null)
                message = body.string();

            boolean retry = Objects.equals(response.header("X-Attempt-Retry", "false"), "true");
            return new MessageNotificationResponse(false, message, retry);
        } catch (IOException e) {
            Log.e("ApiService", "Error sending message: " + e.getMessage());
            return new MessageNotificationResponse(false, e.getMessage());
        }
    }
}
