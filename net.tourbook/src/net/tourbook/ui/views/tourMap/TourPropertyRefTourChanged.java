package net.tourbook.ui.views.tourMap;

import net.tourbook.tour.TourChart;

public class TourPropertyRefTourChanged {

	long		refId;
	int			xMarkerValue;
	TourChart	refTourChart;

	/**
	 * @param refTourChart
	 *        reference tour chart
	 * @param refId
	 *        reference id
	 * @param refTourXMarkerValue
	 *        value difference in the reference tour
	 */
	public TourPropertyRefTourChanged(TourChart refTourChart, long refId, int refTourXMarkerValue) {
		this.refTourChart = refTourChart;
		this.refId = refId;
		this.xMarkerValue = refTourXMarkerValue;
	}

}
