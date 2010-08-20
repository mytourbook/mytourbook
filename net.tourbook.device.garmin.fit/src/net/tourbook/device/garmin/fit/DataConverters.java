package net.tourbook.device.garmin.fit;

import java.math.BigDecimal;

import com.garmin.fit.DateTime;

/**
 * Utility class with various data converters between Garmin and MT format.
 * 
 * @author Marcin Kuthan <marcin.kuthan@gmail.com>
 */
public class DataConverters {

	private DataConverters() {}

	public static double convertSemicirclesToDegrees(int value) {
		return 180.0d * value / 2147483647;
	}

	public static int convertSpeed(float speed) {
		return Math.round(3.6f * speed * 10.0f);
	}

	public static int convertDistance(float distance) {
		return Math.round(distance);
	}

	public static long convertTimestamp(DateTime timestamp) {
		return timestamp.getTimestamp() * 1000L;
	}

	public static String convertSoftwareVersion(int softwareVersion) {
		return BigDecimal.valueOf(softwareVersion, 2).toPlainString();
	}

}
