package me.alenalex.messageforwarder.dto;

import com.squareup.moshi.Json;

public class UpdateResponse {

    @Json(name = "latest_version")
    public String latestVersion;

    @Json(name = "changelog")
    public String changelog;

    @Json(name = "apk_url")
    public String apkUrl;

    public UpdateResponse() {
    }
}