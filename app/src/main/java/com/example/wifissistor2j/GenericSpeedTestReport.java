package com.example.wifissistor2j;

/**
 * A generic data class for speed test results.
 */
public class GenericSpeedTestReport {
    private final double transferRateBit;

    public GenericSpeedTestReport(double transferRateBit) {
        this.transferRateBit = transferRateBit;
    }

    public double getTransferRateBit() {
        return transferRateBit;
    }
}
