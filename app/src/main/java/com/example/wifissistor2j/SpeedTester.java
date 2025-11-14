package com.example.wifissistor2j;

import android.content.Context;

public interface SpeedTester {

    interface SpeedTestListener {
        void onDownloadProgress(float percent, GenericSpeedTestReport report);
        void onDownloadComplete(GenericSpeedTestReport report);
        void onUploadProgress(float percent, GenericSpeedTestReport report);
        void onUploadComplete(GenericSpeedTestReport report);
        void onTestFailed(GenericSpeedTestError error, String message);
        void onTestCancelled();
    }

    void startTest(Context context, SpeedTestListener listener);
    void stopTest();
}
