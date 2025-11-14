package com.example.wifissistor2j;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.util.Arrays;
import java.util.List;

import fr.bmartel.speedtest.SpeedTestReport;
import fr.bmartel.speedtest.SpeedTestSocket;
import fr.bmartel.speedtest.inter.ISpeedTestListener;
import fr.bmartel.speedtest.model.SpeedTestError;

public class SpeedTestManager {

    private static final String TAG = "SpeedTestManager";
    private static final int UPLOAD_PAYLOAD_SIZE = 25000000; // 25 MB

    private final SpeedTestSocket speedTestSocket = new SpeedTestSocket();
    private final Handler handler = new Handler(Looper.getMainLooper());

    private final List<SpeedTestServer> servers = Arrays.asList(
            new SpeedTestServer("OVH", "http://proof.ovh.net/files/100Mio.dat", "http://proof.ovh.net/"),
            new SpeedTestServer("Tele2", "http://speedtest.tele2.net/100MB.zip", "http://speedtest.tele2.net/"),
            new SpeedTestServer("Hetzner", "http://speed.hetzner.de/100MB.bin", "http://speed.hetzner.de/")
    );

    public interface SpeedTestListener {
        void onDownloadProgress(float percent, SpeedTestReport report);
        void onDownloadComplete(SpeedTestReport report);
        void onUploadProgress(float percent, SpeedTestReport report);
        void onUploadComplete(SpeedTestReport report);
        void onTestFailed(SpeedTestError error, String message);
    }

    public void startTest(SpeedTestListener listener) {
        // For simplicity, we'll continue to use a random server.
        // A more robust implementation would ping each server and select the one with the lowest latency.
        SpeedTestServer server = servers.get((int) (Math.random() * servers.size()));
        Log.d(TAG, "Using server: " + server.getName());

        startDownload(server, listener);
    }

    private void startDownload(SpeedTestServer server, SpeedTestListener listener) {
        speedTestSocket.clearListeners();
        speedTestSocket.addSpeedTestListener(new ISpeedTestListener() {
            @Override
            public void onCompletion(SpeedTestReport report) {
                handler.post(() -> {
                    listener.onDownloadComplete(report);
                    // Start upload after download is complete
                    startUpload(server, listener);
                });
            }

            @Override
            public void onProgress(float percent, SpeedTestReport report) {
                handler.post(() -> listener.onDownloadProgress(percent, report));
            }

            @Override
            public void onError(SpeedTestError speedTestError, String errorMessage) {
                handler.post(() -> listener.onTestFailed(speedTestError, "Download failed: " + errorMessage));
            }
        });
        new Thread(() -> speedTestSocket.startDownload(server.getDownloadUrl())).start();
    }

    private void startUpload(SpeedTestServer server, SpeedTestListener listener) {
        speedTestSocket.clearListeners();
        speedTestSocket.addSpeedTestListener(new ISpeedTestListener() {
            @Override
            public void onCompletion(SpeedTestReport report) {
                handler.post(() -> listener.onUploadComplete(report));
            }

            @Override
            public void onProgress(float percent, SpeedTestReport report) {
                handler.post(() -> listener.onUploadProgress(percent, report));
            }

            @Override
            public void onError(SpeedTestError speedTestError, String errorMessage) {
                handler.post(() -> listener.onTestFailed(speedTestError, "Upload failed: " + errorMessage));
            }
        });
        new Thread(() -> speedTestSocket.startUpload(server.getUploadUrl(), UPLOAD_PAYLOAD_SIZE)).start();
    }
}
