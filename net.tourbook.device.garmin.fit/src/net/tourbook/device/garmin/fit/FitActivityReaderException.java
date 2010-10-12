package net.tourbook.device.garmin.fit;

/**
 * The general exception for Garmin FIT activity reader.
 * 
 * @author Marcin Kuthan <marcin.kuthan@gmail.com>
 */
@SuppressWarnings("serial")
public class FitActivityReaderException extends RuntimeException {

	public FitActivityReaderException(String message) {
		super(message);
	}

}
