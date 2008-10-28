/**
 * 
 */
package net.tourbook.ui.tourChart;

import java.util.ArrayList;

import net.tourbook.chart.ChartXSlider;
import net.tourbook.chart.IChartContextProvider;
import net.tourbook.data.TourData;
import net.tourbook.tour.ActionEditQuick;
import net.tourbook.tour.ActionEditTour;
import net.tourbook.ui.ITourProvider;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;

class TourChartContextProvicer implements IChartContextProvider, ITourProvider {

	/**
	 * 
	 */
	private final TourChartView	fTourChartView;

	private ActionEditQuick		fActionQuickEdit;
	private ActionEditTour		fActionEditTour;

	/**
	 * @param tourChartView
	 */
	TourChartContextProvicer(final TourChartView tourChartView) {
		fTourChartView = tourChartView;

		fActionQuickEdit = new ActionEditQuick(fTourChartView);
		fActionEditTour = new ActionEditTour(fTourChartView);
	}

	public void fillBarChartContextMenu(final IMenuManager menuMgr,
										final int hoveredBarSerieIndex,
										final int hoveredBarValueIndex) {}

	public void fillContextMenu(final IMenuManager menuMgr) {

		menuMgr.add(fActionQuickEdit);
		menuMgr.add(fActionEditTour);

		menuMgr.add(new Separator());

		/*
		 * enable actions
		 */
		final boolean isDataAvailable = fTourChartView.fTourData != null
				&& fTourChartView.fTourData.getTourPerson() != null;

		fActionQuickEdit.setEnabled(isDataAvailable);
		fActionEditTour.setEnabled(isDataAvailable);
	}

	public void fillXSliderContextMenu(	final IMenuManager menuMgr,
										final ChartXSlider leftSlider,
										final ChartXSlider rightSlider) {}

	public ArrayList<TourData> getSelectedTours() {

		final ArrayList<TourData> tourList = new ArrayList<TourData>();
		tourList.add(fTourChartView.fTourData);

		return tourList;
	}
}
