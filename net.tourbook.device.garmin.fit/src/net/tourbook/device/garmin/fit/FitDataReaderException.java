package net.tourbook.device.garmin.fit;

/**
 * The general exception for Garmin FIT data reader.
 * 
 * @author Marcin Kuthan <marcin.kuthan@gmail.com>
 * 
 */
@SuppressWarnings("serial")
public class FitDataReaderException extends RuntimeException {

    public FitDataReaderException(String message) {
	super(message);
    }

}