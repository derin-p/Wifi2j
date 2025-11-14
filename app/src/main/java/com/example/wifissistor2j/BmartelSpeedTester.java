package com.example.wifissistor2j;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import fr.bmartel.speedtest.SpeedTestReport;
import fr.bmartel.speedtest.SpeedTestSocket;
import fr.bmartel.speedtest.inter.ISpeedTestListener;
import fr.bmartel.speedtest.model.SpeedTestError;

public class BmartelSpeedTester implements SpeedTester {

    private static final String TAG = "BmartelSpeedTester";
    private static final int UPLOAD_PAYLOAD_SIZE = 25000000;
    private static final int TEST_TIMEOUT_SECONDS = 20;

    private final SpeedTestSocket speedTestSocket = new SpeedTestSocket();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private ScheduledFuture<?> timeoutFuture;

    private int currentServerIndex = 0;
    private final List<SpeedTestServer> servers = new ArrayList<>();
    private boolean serversLoaded = false;
    private boolean testSucceeded = false;

    @Override
    public void startTest(Context context, SpeedTestListener listener) {
        if (!serversLoaded) {
            if (!loadServers(context, listener)) {
                return; // Stop if servers failed to load
            }
            serversLoaded = true;
        }
        currentServerIndex = 0;
        testSucceeded = false;
        tryNextServer(listener);
    }

    @Override
    public void stopTest() {
        if (timeoutFuture != null && !timeoutFuture.isDone()) {
            timeoutFuture.cancel(false);
        }
        speedTestSocket.forceStopTask();
        // The onError will be triggered by forceStopTask, leading to onTestCancelled
    }

    private boolean loadServers(Context context, SpeedTestListener listener) {
        try (InputStream inputStream = context.getResources().openRawResource(R.raw.servers);
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            StringBuilder jsonString = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                jsonString.append(line);
            }
            JSONArray jsonArray = new JSONArray(jsonString.toString());
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject serverObject = jsonArray.getJSONObject(i);
                servers.add(new SpeedTestServer(
                        serverObject.getString("name"),
                        serverObject.getString("downloadUrl"),
                        serverObject.getString("uploadUrl")
                ));
            }
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error loading servers from JSON", e);
            mainHandler.post(() -> listener.onTestFailed(GenericSpeedTestError.CONFIG_ERROR, "Failed to load server list."));
            return false;
        }
    }

    private void tryNextServer(final SpeedTestListener listener) {
        if (currentServerIndex >= servers.size()) {
            if (!testSucceeded) {
                Log.e(TAG, "All servers failed.");
                mainHandler.post(() -> listener.onTestFailed(GenericSpeedTestError.CONNECTION_ERROR, "All speed test servers failed."));
            } else {
                Log.d(TAG, "All servers tested successfully.");
            }
            return;
        }
        SpeedTestServer server = servers.get(currentServerIndex);
        Log.d(TAG, "Attempting test with server: " + server.getName());
        startDownload(server, listener);
    }

    private void startDownload(final SpeedTestServer server, final SpeedTestListener listener) {
        final Runnable timeoutTask = () -> mainHandler.post(() -> {
            Log.e(TAG, "TIMEOUT: Download on server " + server.getName() + " exceeded " + TEST_TIMEOUT_SECONDS + " seconds.");
            speedTestSocket.clearListeners();
            currentServerIndex++;
            tryNextServer(listener);
        });

        speedTestSocket.clearListeners();
        speedTestSocket.addSpeedTestListener(new ISpeedTestListener() {
            @Override
            public void onCompletion(SpeedTestReport report) {
                if (timeoutFuture != null) timeoutFuture.cancel(false);
                mainHandler.post(() -> {
                    listener.onDownloadComplete(adaptReport(report));
                    startUpload(server, listener);
                });
            }

            @Override
            public void onProgress(float percent, SpeedTestReport report) {
                mainHandler.post(() -> listener.onDownloadProgress(percent, adaptReport(report)));
            }

            @Override
            public void onError(SpeedTestError speedTestError, String errorMessage) {
                if (timeoutFuture != null) timeoutFuture.cancel(false);
                if (errorMessage.toLowerCase().contains("socket closed") || errorMessage.toLowerCase().contains("force_stop")) {
                    Log.d(TAG, "Test was manually stopped.");
                    mainHandler.post(listener::onTestCancelled);
                } else {
                    Log.w(TAG, "Download failed on " + server.getName() + ": " + errorMessage + ". Trying next server.");
                    currentServerIndex++;
                    tryNextServer(listener);
                }
            }
        });

        timeoutFuture = scheduler.schedule(timeoutTask, TEST_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        scheduler.execute(() -> speedTestSocket.startDownload(server.getDownloadUrl()));
    }

    private void startUpload(final SpeedTestServer server, final SpeedTestListener listener) {
        final Runnable timeoutTask = () -> mainHandler.post(() -> {
            Log.e(TAG, "TIMEOUT: Upload on server " + server.getName() + " exceeded " + TEST_TIMEOUT_SECONDS + " seconds.");
            speedTestSocket.clearListeners();
            currentServerIndex++;
            tryNextServer(listener);
        });

        speedTestSocket.clearListeners();
        speedTestSocket.addSpeedTestListener(new ISpeedTestListener() {
            @Override
            public void onCompletion(SpeedTestReport report) {
                if (timeoutFuture != null) timeoutFuture.cancel(false);
                testSucceeded = true; // Mark as succeeded only after a full download/upload cycle
                mainHandler.post(() -> {
                    listener.onUploadComplete(adaptReport(report));
                    Log.d(TAG, "Finished test on server: " + server.getName() + ". Moving to next server.");
                    currentServerIndex++;
                    tryNextServer(listener);
                });
            }

            @Override
            public void onProgress(float percent, SpeedTestReport report) {
                mainHandler.post(() -> listener.onUploadProgress(percent, adaptReport(report)));
            }

            @Override
            public void onError(SpeedTestError speedTestError, String errorMessage) {
                if (timeoutFuture != null) timeoutFuture.cancel(false);
                if (errorMessage.toLowerCase().contains("socket closed") || errorMessage.toLowerCase().contains("force_stop")) {
                    Log.d(TAG, "Test was manually stopped.");
                    mainHandler.post(listener::onTestCancelled);
                } else {
                    Log.w(TAG, "Upload failed on " + server.getName() + ": " + errorMessage + ". Trying next server.");
                    currentServerIndex++;
                    tryNextServer(listener);
                }
            }
        });

        timeoutFuture = scheduler.schedule(timeoutTask, TEST_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        scheduler.execute(() -> speedTestSocket.startUpload(server.getUploadUrl(), UPLOAD_PAYLOAD_SIZE));
    }

    private GenericSpeedTestReport adaptReport(SpeedTestReport report) {
        return new GenericSpeedTestReport(report.getTransferRateBit().doubleValue());
    }
}
