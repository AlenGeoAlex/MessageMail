package me.alenalex.messageforwarder.services.api;

import android.content.Context;
import android.content.SharedPreferences;
import android.webkit.WebSettings;

import me.alenalex.messageforwarder.constants.SharedPrefConstants;

public class WebRequestOptions {

    private final String apiUrl;
    private final String userAgent;
    private final String secretKey;

    public WebRequestOptions(Context context){
        SharedPreferences sharedPreferences = context.getSharedPreferences(SharedPrefConstants.SHARED_PREF, Context.MODE_PRIVATE);
        this.apiUrl = sharedPreferences.getString(SharedPrefConstants.API_URL, null);
        this.secretKey = sharedPreferences.getString(SharedPrefConstants.SECRET_KEY, null);
        this.userAgent = WebSettings.getDefaultUserAgent(context);
    }

    public WebRequestOptions(String apiUrl, Context context){
        SharedPreferences sharedPreferences = context.getSharedPreferences(SharedPrefConstants.SHARED_PREF, Context.MODE_PRIVATE);
        this.apiUrl = apiUrl;
        this.secretKey = sharedPreferences.getString(SharedPrefConstants.SECRET_KEY, null);
        this.userAgent = WebSettings.getDefaultUserAgent(context);
    }

    public WebRequestOptions(String apiUrl, String userAgent, String secretKey) {
        this.apiUrl = apiUrl;
        this.userAgent = userAgent;
        this.secretKey = secretKey;
    }

    public WebRequestOptions(String userAgent, String apiUrl) {
        this(userAgent, apiUrl, null);
    }

    public String apiUrl() {
        return apiUrl;
    }

    public String userAgent() {
        return userAgent;
    }

    public String secretKey() {
        return secretKey;
    }

    public WebRequestOptions apiUrl(String apiUrl){
        return new WebRequestOptions(apiUrl, this.userAgent, this.secretKey);
    }

    public WebRequestOptions userAgent(String userAgent){
        return new WebRequestOptions(this.apiUrl, userAgent, this.secretKey);
    }

    public WebRequestOptions secretKey(String secretKey){
        return new WebRequestOptions(this.apiUrl, this.userAgent, secretKey);
    }

}
