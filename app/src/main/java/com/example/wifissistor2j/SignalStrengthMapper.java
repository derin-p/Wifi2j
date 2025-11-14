package com.example.wifissistor2j;

public class SignalStrengthMapper {

    /**
     * Maps an RSSI value to a human-readable signal strength category.
     *
     * @param rssi The RSSI value in dBm (e.g., -30, -50, -80).
     * @return A string representing the signal quality.
     */
    public static String getSignalStrength(int rssi) {
        if (rssi >= -50) {
            return "Excellent";
        } else if (rssi >= -67) {
            return "Good";
        } else if (rssi >= -70) {
            return "Fair";
        } else {
            return "Weak";
        }
    }

    /**
     * Maps an RSSI value to a game-like, estimated distance category.
     * Note: This is a heuristic and not an accurate measure of physical distance.
     *
     * @param rssi The RSSI value in dBm (e.g., -30, -50, -80).
     * @return A string representing the estimated proximity.
     */
    public static String getSignalStrengthAsDistance(int rssi) {
        if (rssi >= -40) {
            return "You're practically on top of it!";
        } else if (rssi >= -55) {
            return "Very close";
        } else if (rssi >= -65) {
            return "Getting closer";
        } else if (rssi >= -75) {
            return "Distant";
        } else {
            return "Very distant";
        }
    }
}
