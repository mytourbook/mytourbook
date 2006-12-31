package net.tourbook.ui;

/**
 * The viewer is able to show a tour chart
 */
public interface ITourChartViewer {

	/**
	 * Show the tour chart for the given tour id, when the tour id is -1, the
	 * tour chart should be hidden
	 * 
	 * @param tourId
	 */
	public void showTourChart(long tourId);

	/**
	 * Open the tour in the editor
	 * 
	 * @param tourId
	 */
	public void openTourChart(long tourId);

	/**
	 * Set in the viewer the active year which was selected in the statistic
	 * 
	 * @param activeYear
	 */
	public void setActiveYear(int activeYear);

	/**
	 * show the tour chart container
	 */
	public void showTourChart(boolean isVisible);

}
