package me.alenalex.messageforwarder;

import com.squareup.moshi.Moshi;

import me.alenalex.messageforwarder.services.api.ApiService;
import me.alenalex.messageforwarder.services.api.UpdateService;
import okhttp3.OkHttpClient;

public class AppContainer {

    private static AppContainer container = null;

    public static AppContainer container(){
        if(container == null)
            container = new AppContainer();

        return container;
    }

    private final Moshi moshi;
    private final ApiService apiService;
    private final UpdateService updateService;
    public AppContainer() {
        this.moshi = new Moshi.Builder().build();
        OkHttpClient client = new OkHttpClient.Builder()
                .followRedirects(true)
                .retryOnConnectionFailure(true)
                .build();

        apiService = new ApiService(client);
        updateService = new UpdateService(client);
    }

    public Moshi moshi() {
        return moshi;
    }

    public ApiService apiService() {
        return apiService;
    }

    public UpdateService updateService() {
        return updateService;
    }

}
