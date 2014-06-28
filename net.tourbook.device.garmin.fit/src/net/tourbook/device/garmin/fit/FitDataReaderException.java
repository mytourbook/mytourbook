package net.tourbook.device.garmin.fit;

/**
 * The general exception for Garmin FIT activity reader.
 * 
 * @author Marcin Kuthan <marcin.kuthan@gmail.com>
 */
@SuppressWarnings("serial")
public class FitDataReaderException extends RuntimeException {

	public FitDataReaderException(String message) {
		super(message);
	}

}
