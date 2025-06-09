package me.alenalex.messageforwarder.services.api;

import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;

public class UpdateService extends AbstractNetworkService {

    private static final String API_URL = "https://raw.githubusercontent.com/AlenGeoAlex/MessageMail/refs/heads/master/.version.json";

    public UpdateService(OkHttpClient okHttpClient) {
        super(okHttpClient);
    }

    public void checkUpdate(Callback callback){
        Request build = new Request.Builder()
                .url(API_URL)
                .get()
                .build();

        okHttpClient().newCall(build).enqueue(callback);
    }

}
