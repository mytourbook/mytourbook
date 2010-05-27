package net.tourbook.printing;

public class PrintSettings {
	
	private PaperSize paperSize;
	private PaperOrientation paperOrientation;
	private String completeFilePath;
	private boolean isPrintMarkers;
	private boolean isPrintDescription;
	private boolean isOverwriteFiles;
	
	
	public PaperSize getPaperSize() {
		return paperSize;
	}
	public void setPaperSize(PaperSize paperSize) {
		this.paperSize = paperSize;
	}
	public PaperOrientation getPaperOrientation() {
		return paperOrientation;
	}
	public void setPaperOrientation(PaperOrientation paperOrientation) {
		this.paperOrientation = paperOrientation;
	}
	public String getCompleteFilePath() {
		return completeFilePath;
	}
	public void setCompleteFilePath(String completeFilePath) {
		this.completeFilePath = completeFilePath;
	}
	public boolean isPrintMarkers() {
		return isPrintMarkers;
	}
	public void setPrintMarkers(boolean isPrintMarkers) {
		this.isPrintMarkers = isPrintMarkers;
	}
	public boolean isPrintDescription() {
		return isPrintDescription;
	}
	public void setPrintDescription(boolean isPrintDescription) {
		this.isPrintDescription = isPrintDescription;
	}
	public boolean isOverwriteFiles() {
		return isOverwriteFiles;
	}
	public void setOverwriteFiles(boolean isOverwriteFiles) {
		this.isOverwriteFiles = isOverwriteFiles;
	}

	
}
