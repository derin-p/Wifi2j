package com.example.wifissistor2j;

public class SpeedTestServer {
    private final String name;
    private final String downloadUrl;
    private final String uploadUrl;

    public SpeedTestServer(String name, String downloadUrl, String uploadUrl) {
        this.name = name;
        this.downloadUrl = downloadUrl;
        this.uploadUrl = uploadUrl;
    }

    public String getName() {
        return name;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }

    public String getUploadUrl() {
        return uploadUrl;
    }
}
