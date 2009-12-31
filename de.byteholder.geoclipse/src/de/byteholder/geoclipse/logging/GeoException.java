package de.byteholder.geoclipse.logging;

public class GeoException extends Exception {

	private static final long	serialVersionUID	= -4461244721669720910L;

	public GeoException() {}

	public GeoException(final Exception e) {
		super(e);
	}

	public GeoException(final String message) {
		super(message);
	}

	public GeoException(final String message, final Throwable cause) {
		super(message, cause);
	}
}
