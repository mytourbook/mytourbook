package net.tourbook.ui.views;

import net.tourbook.data.TourData;
import net.tourbook.database.TourDatabase;
import net.tourbook.plugin.TourbookPlugin;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.tour.ITourPropertyListener;
import net.tourbook.tour.TourChart;
import net.tourbook.tour.TourChartConfiguration;
import net.tourbook.tour.TourManager;
import net.tourbook.util.PostSelectionProvider;

import org.eclipse.core.runtime.Preferences;
import org.eclipse.core.runtime.Preferences.IPropertyChangeListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IPropertyListener;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.part.ViewPart;

/**
 * Provides a skeleton for a view which displays a tour chart
 */
public abstract class TourChartViewPart extends ViewPart {

	protected TourChart					fCompareTourChart;
	protected TourData					fTourData;
	protected TourChartConfiguration	fTourChartConfig;

	protected PostSelectionProvider		fPostSelectionProvider;

	private IPropertyChangeListener		fPrefChangeListener;
	private IPropertyListener			fTourDbListener;
	private ITourPropertyListener		fTourPropertyListener;
	private ISelectionListener			fPostSelectionListener;

	private void addPrefListener() {

		fPrefChangeListener = new Preferences.IPropertyChangeListener() {
			public void propertyChange(final Preferences.PropertyChangeEvent event) {

				final String property = event.getProperty();

				/*
				 * set a new chart configuration when the preferences has changed
				 */
				if (property.equals(ITourbookPreferences.GRAPH_VISIBLE)
						|| property.equals(ITourbookPreferences.GRAPH_X_AXIS)
						|| property.equals(ITourbookPreferences.GRAPH_X_AXIS_STARTTIME)) {

					fTourChartConfig = TourManager.createTourChartConfiguration();

					if (fCompareTourChart != null) {
						fCompareTourChart.updateTourChart(fTourData, fTourChartConfig, false);
					}
				}
			}
		};

		TourbookPlugin.getDefault()
				.getPluginPreferences()
				.addPropertyChangeListener(fPrefChangeListener);
	}

	/**
	 * listen for events when a tour is selected
	 */
	private void addSelectionListener() {

		fPostSelectionListener = new ISelectionListener() {
			public void selectionChanged(IWorkbenchPart part, ISelection selection) {
				onSelectionChanged(selection);
			}
		};
		getSite().getPage().addPostSelectionListener(fPostSelectionListener);
	}

	private void addTourDbListener() {

		fTourDbListener = new IPropertyListener() {
			public void propertyChanged(Object source, int propId) {
				if (propId == TourDatabase.TOUR_IS_CHANGED_AND_PERSISTED) {

					if (fTourData == null) {
						return;
					}

					// reload data from the database
					fTourData = TourDatabase.getTourData(fTourData.getTourId());

					updateChart();

				} else if (propId == TourDatabase.TOUR_IS_CHANGED) {

					updateChart();
				}
			}
		};

		TourDatabase.getInstance().addPropertyListener(fTourDbListener);
	}

	private void addTourPropertyListener() {

		fTourPropertyListener = new ITourPropertyListener() {
			public void propertyChanged(int propertyId, Object propertyData) {

				if (propertyId == TourManager.TOUR_PROPERTY_SEGMENT_LAYER_CHANGED) {
					fCompareTourChart.updateSegmentLayer((Boolean) propertyData);

				} else if (propertyId == TourManager.TOUR_PROPERTY_CHART_IS_MODIFIED) {
					fCompareTourChart.updateTourChart(true, true);
				}
			}
		};

		TourManager.getInstance().addPropertyListener(fTourPropertyListener);
	}

	@Override
	public void createPartControl(Composite parent) {

		addPrefListener();
		addTourDbListener();
		addTourPropertyListener();
		addSelectionListener();

		// set this part as selection provider
		getSite().setSelectionProvider(fPostSelectionProvider = new PostSelectionProvider());

	}

	@Override
	public void dispose() {

		getSite().getPage().removePostSelectionListener(fPostSelectionListener);

		TourDatabase.getInstance().removePropertyListener(fTourDbListener);
		TourManager.getInstance().removePropertyListener(fTourPropertyListener);

		TourbookPlugin.getDefault()
				.getPluginPreferences()
				.removePropertyChangeListener(fPrefChangeListener);

		super.dispose();
	}

	/**
	 * A post selection event was received by the selection listener
	 * 
	 * @param selection
	 */
	protected abstract void onSelectionChanged(ISelection selection);

	/**
	 * Update the chart after the tour data was modified
	 */
	protected abstract void updateChart();

}
