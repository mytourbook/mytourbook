package net.tourbook.device.garmin.fit.listeners;

public class DataConverters {

    private DataConverters() {
    }

    public static double convertSemicirclesToDegrees(int value) {
	return 180.0d * value / 2147483647;
    }

    public static int convertSpeed(float speed) {
	return Math.round(3.6f * speed * 10.0f);
    }

    public static int convertDistance(float distance) {
	return Math.round(distance);
    }
}
