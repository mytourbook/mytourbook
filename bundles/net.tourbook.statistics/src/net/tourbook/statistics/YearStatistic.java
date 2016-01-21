/*******************************************************************************
 * Copyright (C) 2005, 2015 Wolfgang Schramm and Contributors
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
package net.tourbook.statistics;

import java.util.ArrayList;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.chart.Chart;
import net.tourbook.chart.ChartTitleSegmentConfig;
import net.tourbook.data.TourType;
import net.tourbook.database.TourDatabase;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.statistic.IYearStatistic;
import net.tourbook.statistic.TourbookStatistic;
import net.tourbook.ui.TourTypeFilter;
import net.tourbook.ui.UI;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.widgets.Composite;

public abstract class YearStatistic extends TourbookStatistic implements IYearStatistic {

	private final IPreferenceStore	_prefStore					= TourbookPlugin.getDefault().getPreferenceStore();

	protected static final String	MEMENTO_SELECTED_TOUR_ID	= "statistic.selected.tourId";						//$NON-NLS-1$

	private IPropertyChangeListener	_prefChangeListener;

	/**
	 * Add the pref listener which is called when the color was changed
	 * 
	 * @param container
	 */
	private void addPrefListener(final Composite container) {

		// create pref listener
		_prefChangeListener = new IPropertyChangeListener() {
			@Override
			public void propertyChange(final PropertyChangeEvent event) {
				final String property = event.getProperty();

				// test if the color or statistic data have changed
				if (property.equals(ITourbookPreferences.GRAPH_COLORS_HAS_CHANGED)
				//
						|| property.equals(ITourbookPreferences.GRAPH_GRID_HORIZONTAL_DISTANCE)
						|| property.equals(ITourbookPreferences.GRAPH_GRID_VERTICAL_DISTANCE)
						|| property.equals(ITourbookPreferences.GRAPH_GRID_IS_SHOW_HORIZONTAL_GRIDLINES)
						|| property.equals(ITourbookPreferences.GRAPH_GRID_IS_SHOW_VERTICAL_GRIDLINES)
						|| property.equals(ITourbookPreferences.GRAPH_IS_SEGMENT_ALTERNATE_COLOR)
						|| property.equals(ITourbookPreferences.GRAPH_SEGMENT_ALTERNATE_COLOR)
				//
				) {

					// update chart
					preferencesHasChanged();
				}
			}
		};

		// add pref listener
		_prefStore.addPropertyChangeListener(_prefChangeListener);

		// remove pre listener
		container.addDisposeListener(new DisposeListener() {
			@Override
			public void widgetDisposed(final DisposeEvent e) {
				_prefStore.removePropertyChangeListener(_prefChangeListener);
			}
		});
	}

	/**
	 * call super.createControl to initialize the color change listener
	 * 
	 * @see net.tourbook.statistic.TourbookStatistic#createControl(org.eclipse.swt.widgets.Composite)
	 */
	public void createControl(final Composite parent) {
		addPrefListener(parent);
	}

	/**
	 * @param serieIndex
	 * @param activeTourTypeFilter
	 * @return Returns the tour type name for a data serie
	 */
	protected String getTourTypeName(final int serieIndex, final TourTypeFilter activeTourTypeFilter) {

		int colorOffset = 0;
		if (activeTourTypeFilter.showUndefinedTourTypes()) {
			colorOffset = StatisticServices.TOUR_TYPE_COLOR_INDEX_OFFSET;
		}

		if (serieIndex - colorOffset < 0) {
			return Messages.ui_tour_not_defined;
		}

		final ArrayList<TourType> tourTypeList = TourDatabase.getActiveTourTypes();
		final String tourTypeName = TourDatabase
				.getTourTypeName(tourTypeList.get(serieIndex - colorOffset).getTypeId());

		return tourTypeName;
	}

	@Override
	public void restoreState(final IDialogSettings state) {
		// do nothing
	}

	@Override
	public void saveState(final IDialogSettings state) {
		// do nothing
	}

	/**
	 * Set chart properties from the pref store.
	 * 
	 * @param chart
	 */
	protected void setChartProperties(final Chart chart) {

		UI.setChartProperties(chart);

		/*
		 * These settings are currently static, a UI to modify it is not yet implemented.
		 */
		final ChartTitleSegmentConfig ctsConfig = chart.getChartTitleSegmentConfig();

		ctsConfig.isMultipleSegments = true;

		ctsConfig.isShowSegmentBackground = false;
		ctsConfig.isShowSegmentSeparator = true;
		ctsConfig.isShowSegmentTitle = true;
	}
}
