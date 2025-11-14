package com.example.wifissistor2j;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

public class SpeedTestManager implements SpeedTester.SpeedTestListener {

    private static SpeedTestManager instance;
    private final SpeedTester speedTester;
    private final Context context;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    // The listener is now volatile and can be swapped out
    private volatile SpeedTester.SpeedTestListener uiListener;

    private SpeedTestManager(Context context) {
        this.context = context.getApplicationContext();
        this.speedTester = new BmartelSpeedTester();
    }

    public static synchronized SpeedTestManager getInstance(Context context) {
        if (instance == null) {
            instance = new SpeedTestManager(context);
        }
        return instance;
    }

    public void setListener(SpeedTester.SpeedTestListener listener) {
        this.uiListener = listener;
    }

    public void removeListener() {
        this.uiListener = null;
    }

    public void startTest() {
        // The manager is now the direct listener
        speedTester.startTest(context, this);
    }

    public void stopTest() {
        speedTester.stopTest();
    }

    // --- SpeedTestListener Implementation ---
    // These methods now safely delegate to the UI listener if it's present.

    @Override
    public void onDownloadProgress(float percent, GenericSpeedTestReport report) {
        if (uiListener != null) {
            mainHandler.post(() -> uiListener.onDownloadProgress(percent, report));
        }
    }

    @Override
    public void onDownloadComplete(GenericSpeedTestReport report) {
        if (uiListener != null) {
            mainHandler.post(() -> uiListener.onDownloadComplete(report));
        }
    }

    @Override
    public void onUploadProgress(float percent, GenericSpeedTestReport report) {
        if (uiListener != null) {
            mainHandler.post(() -> uiListener.onUploadProgress(percent, report));
        }
    }

    @Override
    public void onUploadComplete(GenericSpeedTestReport report) {
        if (uiListener != null) {
            mainHandler.post(() -> uiListener.onUploadComplete(report));
        }
    }

    @Override
    public void onTestFailed(GenericSpeedTestError error, String message) {
        if (uiListener != null) {
            mainHandler.post(() -> uiListener.onTestFailed(error, message));
        }
    }

    @Override
    public void onTestCancelled() {
        if (uiListener != null) {
            mainHandler.post(() -> uiListener.onTestCancelled());
        }
    }
}
