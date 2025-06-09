package me.alenalex.messageforwarder.services.api;

import java.io.IOException;

import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public abstract class AbstractNetworkService {

    private final OkHttpClient okHttpClient;

    protected AbstractNetworkService(OkHttpClient okHttpClient) {
        this.okHttpClient = okHttpClient;
    }

    public Response executeRequest(Request request) throws IOException {
        return okHttpClient.newCall(request).execute();
    }

    public void queue(Request request, Callback callback){
        this.okHttpClient.newCall(request).enqueue(callback);
    }

    public Request get(WebRequestOptions options){
        Request.Builder builder = new Request.Builder()
                .url(options.apiUrl())
                .header("User-Agent", options.userAgent());

        if(options.secretKey() != null && !options.secretKey().isEmpty())
            builder = builder.header("x-secret-key", options.secretKey());

        return builder.build();
    }

    public Request post(WebRequestOptions options, String json){
        Request.Builder builder = new Request.Builder()
                .url(options.apiUrl())
                .post(RequestBody.create(json, MediaType.parse("application/json")))
                .header("User-Agent", options.userAgent());

        if(options.secretKey() != null && !options.secretKey().isEmpty())
            builder = builder.header("x-secret-key", options.secretKey());

        return builder.build();
    }

    public OkHttpClient okHttpClient() {
        return okHttpClient;
    }
}
