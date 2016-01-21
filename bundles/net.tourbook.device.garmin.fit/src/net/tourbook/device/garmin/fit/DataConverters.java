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

	public static double convertSemicirclesToDegrees(final int value) {
		return 180.0d * value / 2147483647;
	}

	public static String convertSoftwareVersion(final int softwareVersion) {
		return BigDecimal.valueOf(softwareVersion, 2).toPlainString();
	}

	/**
	 * Convert m/s -> km/h
	 * 
	 * @param speed
	 * @return
	 */
	public static float convertSpeed(final float speed) {
		return 3.6f * speed;
	}

	/**
	 * @param timestamp
	 * @return Returns timestamp in GARMIN time not in Java time !!!
	 */
	public static long convertTimestamp(final DateTime timestamp) {
		return timestamp.getTimestamp() * 1000L;
	}

}
