package net.tourbook.export.openlayers;

/**
 * Value object for velocity context containing track layer info
 */
public class LayerContext {
	private String	trackName;
	private String	fileName;

	public String getTrackName() {
		return trackName;
	}

	public void setTrackName(String trackName) {
		this.trackName = trackName;
	}

	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}
}