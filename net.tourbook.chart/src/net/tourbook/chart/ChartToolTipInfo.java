package net.tourbook.chart;

public class ChartToolTipInfo {

	private String	title;
	private String	label;

	private boolean	fIsDisplayed	= false;
	private boolean	fIsReposition	= false;

	public String getLabel() {
		return label;
	}

	public String getTitle() {
		return title;
	}

	boolean isDisplayed() {
		return fIsDisplayed;
	}

	boolean isReposition() {
		return fIsReposition;
	}

	public void setIsDisplayed(boolean isDisplayed) {
		fIsDisplayed = isDisplayed;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public void setReposition(boolean isReposition) {
		fIsReposition = isReposition;
	}

}
