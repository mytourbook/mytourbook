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
package net.tourbook.ui.views.tourCatalog;

import java.text.NumberFormat;
import java.util.HashMap;

import net.tourbook.application.TourbookPlugin;
import net.tourbook.chart.Chart;
import net.tourbook.chart.ChartDataModel;
import net.tourbook.chart.SelectionChartInfo;
import net.tourbook.chart.SelectionChartXSliderPosition;
import net.tourbook.common.UI;
import net.tourbook.data.TourData;
import net.tourbook.importdata.RawDataManager;
import net.tourbook.tour.ITourEventListener;
import net.tourbook.tour.TourEventId;
import net.tourbook.tour.TourManager;
import net.tourbook.ui.tourChart.TourChart;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.part.ViewPart;

public class GeoPartsView extends ViewPart {

	public static final String				ID		= "net.tourbook.ui.views.tourCatalog.GeoPartsView";	//$NON-NLS-1$

	private static final IDialogSettings	_state	= TourbookPlugin.getState(ID);

	private final static NumberFormat		_nf2	= NumberFormat.getNumberInstance();
	{
		_nf2.setMinimumIntegerDigits(2);
		_nf2.setMaximumFractionDigits(2);
	}

	private IPartListener2		_partListener;
	private ITourEventListener	_tourEventListener;

	private int					_lastSelectionHash;

	/*
	 * UI controls
	 */
	private Label				_lblNumGeoParts;
	private Label				_lblNumSlices;
	private Label				_lblNumTours;

	private void addPartListener() {

		_partListener = new IPartListener2() {

			@Override
			public void partActivated(final IWorkbenchPartReference partRef) {}

			@Override
			public void partBroughtToTop(final IWorkbenchPartReference partRef) {}

			@Override
			public void partClosed(final IWorkbenchPartReference partRef) {
				if (partRef.getPart(false) == GeoPartsView.this) {}
			}

			@Override
			public void partDeactivated(final IWorkbenchPartReference partRef) {}

			@Override
			public void partHidden(final IWorkbenchPartReference partRef) {
				if (partRef.getPart(false) == GeoPartsView.this) {}
			}

			@Override
			public void partInputChanged(final IWorkbenchPartReference partRef) {}

			@Override
			public void partOpened(final IWorkbenchPartReference partRef) {}

			@Override
			public void partVisible(final IWorkbenchPartReference partRef) {
				if (partRef.getPart(false) == GeoPartsView.this) {}
			}
		};
		getViewSite().getPage().addPartListener(_partListener);
	}

	private void addTourEventListener() {

		_tourEventListener = new ITourEventListener() {
			@Override
			public void tourChanged(final IWorkbenchPart part, final TourEventId eventId, final Object eventData) {

				if (part == GeoPartsView.this) {
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

	@Override
	public void createPartControl(final Composite parent) {

		createUI(parent);

		addPartListener();
		addTourEventListener();
	}

	private void createUI(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
		GridLayoutFactory
				.swtDefaults()//
				.numColumns(2)
				.spacing(10, 2)
				.applyTo(container);
		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_LIST_BACKGROUND));
		{
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
				label.setText("Tours <="); //$NON-NLS-1$
				GridDataFactory.fillDefaults().applyTo(label);

				_lblNumTours = new Label(container, SWT.NONE);
				_lblNumTours.setText(UI.EMPTY_STRING);
				GridDataFactory.fillDefaults().grab(true, false).applyTo(_lblNumTours);
			}
		}
	}

	@Override
	public void dispose() {

		getViewSite().getPage().removePartListener(_partListener);
		TourManager.getInstance().removeTourEventListener(_tourEventListener);

		super.dispose();
	}

	private void onMoveSlider(	final TourData tourData,
								final int leftIndex,
								final int rightIndex) {

		if (tourData.latitudeSerie == null) {
			return;
		}

		final int firstIndex = leftIndex < rightIndex ? leftIndex : rightIndex;
		final int lastIndex = leftIndex > rightIndex ? leftIndex : rightIndex;

		final int[] geoPartitions = tourData.computeGeo_Partitions(firstIndex, lastIndex);

		if (geoPartitions == null) {
			return;
		}

		_lblNumSlices.setText(Integer.toString(lastIndex - firstIndex));
		_lblNumGeoParts.setText(Integer.toString(geoPartitions.length));
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

}
