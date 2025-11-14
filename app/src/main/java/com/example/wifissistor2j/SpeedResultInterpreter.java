package com.example.wifissistor2j;

public class SpeedResultInterpreter {

    /**
     * Interprets a speed value in Mbps and returns a human-readable quality rating.
     *
     * @param speedMbps The speed in Megabits per second.
     * @return A string representing the speed quality.
     */
    public static String interpret(double speedMbps) {
        if (speedMbps >= 100) {
            return "Excellent";
        } else if (speedMbps >= 40) {
            return "Very Good";
        } else if (speedMbps >= 10) {
            return "Good";
        } else if (speedMbps >= 5) {
            return "Fair";
        } else {
            return "Poor";
        }
    }
}
