/*******************************************************************************
 * Copyright (C) 2005, 2018 Wolfgang Schramm and Contributors
 * 
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation version 2 of the License.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110, USA
 *******************************************************************************/
package net.tourbook.ui.views.tourCatalog.geo;

import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;

import net.tourbook.application.TourbookPlugin;
import net.tourbook.chart.Chart;
import net.tourbook.chart.ChartDataModel;
import net.tourbook.chart.SelectionChartInfo;
import net.tourbook.chart.SelectionChartXSliderPosition;
import net.tourbook.common.UI;
import net.tourbook.data.NormalizedGeoData;
import net.tourbook.data.TourData;
import net.tourbook.importdata.RawDataManager;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.tour.ITourEventListener;
import net.tourbook.tour.TourEventId;
import net.tourbook.tour.TourManager;
import net.tourbook.ui.tourChart.TourChart;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.wizard.ProgressMonitorPart;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.part.ViewPart;

public class GeoPartView extends ViewPart {

	public static final String		ID				= "net.tourbook.ui.views.tourCatalog.geo.GeoPartView";	//$NON-NLS-1$

//	private static final IDialogSettings	_state		= TourbookPlugin.getState(ID);
	private final IPreferenceStore	_prefStore		= TourbookPlugin.getPrefStore();

	private IPartListener2			_partListener;
	private ITourEventListener		_tourEventListener;
	private SelectionAdapter		_defaultSelectionListener;
	private IPropertyChangeListener	_prefChangeListener;

	private int						_lastSelectionHash;

	private int[]					_geoParts;
	private NormalizedGeoData		_normalizedTourPart;

	private int						_geoPartTours;
	private AtomicInteger			_workedTours	= new AtomicInteger();

	/*
	 * UI controls
	 */
	private Composite				_parent;

	private Label					_lblNumGeoParts;
	private Label					_lblNumSlices;
	private Label					_lblNumTours;
	private Label					_lblSqlRuntime;

	private Button					_chkUseAppFilter;

	private ProgressMonitorPart		_progMonitor;

	private void addPartListener() {

		_partListener = new IPartListener2() {

			@Override
			public void partActivated(final IWorkbenchPartReference partRef) {}

			@Override
			public void partBroughtToTop(final IWorkbenchPartReference partRef) {}

			@Override
			public void partClosed(final IWorkbenchPartReference partRef) {
				if (partRef.getPart(false) == GeoPartView.this) {
					onCancelProgress();
				}
			}

			@Override
			public void partDeactivated(final IWorkbenchPartReference partRef) {}

			@Override
			public void partHidden(final IWorkbenchPartReference partRef) {
				if (partRef.getPart(false) == GeoPartView.this) {}
			}

			@Override
			public void partInputChanged(final IWorkbenchPartReference partRef) {}

			@Override
			public void partOpened(final IWorkbenchPartReference partRef) {}

			@Override
			public void partVisible(final IWorkbenchPartReference partRef) {
				if (partRef.getPart(false) == GeoPartView.this) {}
			}
		};
		getViewSite().getPage().addPartListener(_partListener);
	}

	private void addPrefListener() {

		_prefChangeListener = new IPropertyChangeListener() {
			@Override
			public void propertyChange(final PropertyChangeEvent event) {

				final String property = event.getProperty();

				if (property.equals(ITourbookPreferences.APP_DATA_FILTER_IS_MODIFIED)) {

					onChangeUI();
				}
			}
		};

		// register the listener
		_prefStore.addPropertyChangeListener(_prefChangeListener);
	}

	private void addTourEventListener() {

		_tourEventListener = new ITourEventListener() {
			@Override
			public void tourChanged(final IWorkbenchPart part, final TourEventId eventId, final Object eventData) {

				if (part == GeoPartView.this) {
					return;
				}

				if ((eventId == TourEventId.TOUR_SELECTION) && eventData instanceof ISelection) {

					onSelectionChanged((ISelection) eventData);

				} else if (eventId == TourEventId.SLIDER_POSITION_CHANGED && eventData instanceof ISelection) {

					onSelectionChanged((ISelection) eventData);
				}
			}
		};

		TourManager.getInstance().addTourEventListener(_tourEventListener);
	}

	private void compare_20_LoadGeoParts(final TourData tourData, final int firstIndex, final int lastIndex) {

		// 1. get geo partitions from lat/lon first/last index
		_geoParts = tourData.computeGeo_Partitions(firstIndex, lastIndex);

		if (_geoParts == null) {
			return;
		}

		// update UI
		_lblNumSlices.setText(Integer.toString(lastIndex - firstIndex));
		_lblNumGeoParts.setText(Integer.toString(_geoParts.length));

		/*
		 * Create geo data which should be compared
		 */
		_normalizedTourPart = tourData.computeGeo_NormalizedLatLon(firstIndex, lastIndex);

		// 2. load tour id's in the geo parts
		GeoPartTourLoader.loadToursFromGeoParts(
				_geoParts,
				_normalizedTourPart,
				_chkUseAppFilter.getSelection());
	}

	void compare_30_CompareTours(final GeoPartLoaderItem loaderItem) {

		_geoPartTours = loaderItem.tourIds.length;

		if (_geoPartTours > 0) {
			GeoPartTourComparer.compareGeoTours(loaderItem);
		}

		// update UI
		Display.getDefault().asyncExec(new Runnable() {
			@Override
			public void run() {

				if (_parent.isDisposed()) {
					return;
				}

				if (_geoPartTours > 0) {

					_workedTours.set(0);

					// enable cancel button
					_progMonitor.attachToCancelComponent(null);

					_progMonitor.beginTask(NLS.bind("Comparing tours", _geoPartTours), _geoPartTours);
				}

				updateUI(loaderItem);
			}
		});

	}

	void compare_40_TourIsCompared(final GeoPartComparerItem comparerItem) {

		_workedTours.incrementAndGet();

		Display.getDefault().asyncExec(new Runnable() {
			@Override
			public void run() {

				if (_parent.isDisposed()) {
					return;
				}

				_progMonitor.subTask(NLS.bind("{0} / {1}", _workedTours, _geoPartTours));
				_progMonitor.worked(1);
			}
		});
	}

	@Override
	public void createPartControl(final Composite parent) {

		_parent = parent;

		initUI();

		createUI(parent);

		addPartListener();
		addPrefListener();
		addTourEventListener();

		GeoPartTourLoader.geoPartView = this;
		GeoPartTourComparer.geoPartView = this;
	}

	private void createUI(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
		GridLayoutFactory
				.swtDefaults()//
				.numColumns(2)
				//				.spacing(10, 2)
				.applyTo(container);
		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_LIST_BACKGROUND));
		{
			{
				/*
				 * Show break time values
				 */
				_chkUseAppFilter = new Button(container, SWT.CHECK);
				_chkUseAppFilter.setText("Use app &filter");

				GridDataFactory
						.fillDefaults()//
						.span(2, 1)
						.applyTo(_chkUseAppFilter);

				_chkUseAppFilter.addSelectionListener(_defaultSelectionListener);
			}
			{
				/*
				 * Number of time slices
				 */

				final Label label = new Label(container, SWT.NONE);
				label.setText("Time Slices"); //$NON-NLS-1$
				GridDataFactory.fillDefaults().applyTo(label);

				_lblNumSlices = new Label(container, SWT.NONE);
				_lblNumSlices.setText(UI.EMPTY_STRING);
				GridDataFactory.fillDefaults().grab(true, false).applyTo(_lblNumSlices);
			}
			{
				/*
				 * Number of geo parts
				 */

				final Label label = new Label(container, SWT.NONE);
				label.setText("Geo Parts"); //$NON-NLS-1$
				GridDataFactory.fillDefaults().applyTo(label);

				_lblNumGeoParts = new Label(container, SWT.NONE);
				_lblNumGeoParts.setText(UI.EMPTY_STRING);
				GridDataFactory.fillDefaults().grab(true, false).applyTo(_lblNumGeoParts);
			}
			{
				/*
				 * Number of tours
				 */

				final Label label = new Label(container, SWT.NONE);
				label.setText("Part Tours"); //$NON-NLS-1$
				GridDataFactory.fillDefaults().applyTo(label);

				_lblNumTours = new Label(container, SWT.NONE);
				_lblNumTours.setText(UI.EMPTY_STRING);
				GridDataFactory.fillDefaults().grab(true, false).applyTo(_lblNumTours);
			}
			{
				/*
				 * SQL runtime
				 */

				final Label label = new Label(container, SWT.NONE);
				label.setText("SQL Runtime"); //$NON-NLS-1$
				GridDataFactory.fillDefaults().applyTo(label);

				_lblSqlRuntime = new Label(container, SWT.NONE);
				_lblSqlRuntime.setText(UI.EMPTY_STRING);
				GridDataFactory.fillDefaults().grab(true, false).applyTo(_lblSqlRuntime);
			}
			{
				/*
				 * Progress monitor with cancel button
				 */
				_progMonitor = new ProgressMonitorPart(container, new GridLayout(), true) {

					@Override
					public void setCanceled(final boolean isCanceled) {

						onCancelProgress();

						super.setCanceled(isCanceled);
					}

//					@Override
//					public void done() {
//						super.done();
//					}
				};

				GridDataFactory.fillDefaults().span(2, 1).applyTo(_progMonitor);
			}
		}
	}

	@Override
	public void dispose() {

		getViewSite().getPage().removePartListener(_partListener);
		TourManager.getInstance().removeTourEventListener(_tourEventListener);

		_prefStore.removePropertyChangeListener(_prefChangeListener);

		super.dispose();
	}

	private void initUI() {

		_defaultSelectionListener = new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				onChangeUI();
			}
		};
	}

	private void onCancelProgress() {

		GeoPartTourLoader.stopLoading();
	}

	private void onChangeUI() {

		if (_geoParts != null) {

			GeoPartTourLoader.loadToursFromGeoParts(
					_geoParts,
					_normalizedTourPart,
					_chkUseAppFilter.getSelection());
		}

	}

	private void onMoveSlider(	final TourData tourData,
								final int leftIndex,
								final int rightIndex) {

		final double[] latSerie = tourData.latitudeSerie;
		if (latSerie == null) {
			return;
		}

		/*
		 * Validate first/last indices
		 */
		int firstIndex = leftIndex < rightIndex ? leftIndex : rightIndex;
		int lastIndex = leftIndex > rightIndex ? leftIndex : rightIndex;
		if (firstIndex < 0) {
			firstIndex = 0;
		}
		if (lastIndex > latSerie.length) {
			lastIndex = latSerie.length;
		}

		compare_20_LoadGeoParts(tourData, firstIndex, lastIndex);
	}

	private void onSelectionChanged(final ISelection selection) {

		final int selectionHash = selection.hashCode();
		if (_lastSelectionHash == selectionHash) {

			/*
			 * Last selection has not changed, this can occure when the app lost the focus and got
			 * the focus again.
			 */
			return;
		}

		_lastSelectionHash = selectionHash;

		if (selection instanceof SelectionChartInfo) {

			TourData tourData = null;

			final SelectionChartInfo chartInfo = (SelectionChartInfo) selection;

			final Chart chart = chartInfo.getChart();
			if (chart instanceof TourChart) {

				final TourChart tourChart = (TourChart) chart;
				tourData = tourChart.getTourData();
			}

			if (tourData != null && tourData.isMultipleTours()) {

				// multiple tours are selected

			} else {

				// use old behaviour

				final ChartDataModel chartDataModel = chartInfo.chartDataModel;
				if (chartDataModel != null) {

					final Object tourId = chartDataModel.getCustomData(Chart.CUSTOM_DATA_TOUR_ID);
					if (tourId instanceof Long) {

						tourData = TourManager.getInstance().getTourData((Long) tourId);
						if (tourData == null) {

							// tour is not in the database, try to get it from the raw data manager

							final HashMap<Long, TourData> rawData = RawDataManager.getInstance().getImportedTours();
							tourData = rawData.get(tourId);
						}
					}
				}
			}

			if (tourData != null) {

				onMoveSlider(
						tourData,
						chartInfo.leftSliderValuesIndex,
						chartInfo.rightSliderValuesIndex);
			}

		} else if (selection instanceof SelectionChartXSliderPosition) {

			final SelectionChartXSliderPosition xSliderPos = (SelectionChartXSliderPosition) selection;
			final Chart chart = xSliderPos.getChart();
			if (chart == null) {
				return;
			}

			final ChartDataModel chartDataModel = chart.getChartDataModel();

			final Object tourId = chartDataModel.getCustomData(Chart.CUSTOM_DATA_TOUR_ID);
			if (tourId instanceof Long) {

				final TourData tourData = TourManager.getInstance().getTourData((Long) tourId);
				if (tourData != null) {

					final int leftSliderValueIndex = xSliderPos.getLeftSliderValueIndex();
					int rightSliderValueIndex = xSliderPos.getRightSliderValueIndex();

					rightSliderValueIndex =
							rightSliderValueIndex == SelectionChartXSliderPosition.IGNORE_SLIDER_POSITION
									? leftSliderValueIndex
									: rightSliderValueIndex;

					onMoveSlider(
							tourData,
							leftSliderValueIndex,
							rightSliderValueIndex);
				}
			}

		}
	}

	@Override
	public void setFocus() {}

	private void updateUI(final GeoPartLoaderItem loaderItem) {

		_lblNumTours.setText(Integer.toString(loaderItem.tourIds.length));
		_lblSqlRuntime.setText(Long.toString(loaderItem.sqlRunningTime) + " ms");
	}

}
