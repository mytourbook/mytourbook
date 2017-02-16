/*******************************************************************************
 * Copyright (C) 2005, 2017 Wolfgang Schramm and Contributors
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
package net.tourbook.statistics.graphs;

import net.tourbook.application.TourbookPlugin;
import net.tourbook.chart.ChartDataModel;
import net.tourbook.chart.ChartType;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.statistic.ChartOptions_WeekSummary;
import net.tourbook.statistic.SlideoutStatisticOptions;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IViewSite;

public class StatisticWeek_Summary extends StatisticWeek {

	private final IPreferenceStore	_prefStore	= TourbookPlugin.getPrefStore();

	private IPropertyChangeListener	_statWeek_PrefChangeListener;

	private boolean					_isShowAltitude;
	private boolean					_isShowDistance;
	private boolean					_isShowDuration;
	private boolean					_isShowNumTours;

	private void addPrefListener(final Composite container) {

		// create pref listener
		_statWeek_PrefChangeListener = new IPropertyChangeListener() {
			@Override
			public void propertyChange(final PropertyChangeEvent event) {

				final String property = event.getProperty();

				// observe which data are displayed
				if (property.equals(ITourbookPreferences.STAT_WEEK_CHART_TYPE)
						|| property.equals(ITourbookPreferences.STAT_WEEK_IS_SHOW_ALTITUDE)
						|| property.equals(ITourbookPreferences.STAT_WEEK_IS_SHOW_DISTANCE)
						|| property.equals(ITourbookPreferences.STAT_WEEK_IS_SHOW_DURATION)
						|| property.equals(ITourbookPreferences.STAT_WEEK_IS_SHOW_NUMBER_OF_TOURS)
				//
				) {

					// get the changed preferences
					getPreferences();

					// update chart
					preferencesHasChanged();
				}
			}
		};

		// add pref listener
		_prefStore.addPropertyChangeListener(_statWeek_PrefChangeListener);

		// remove pref listener
		container.addDisposeListener(new DisposeListener() {
			@Override
			public void widgetDisposed(final DisposeEvent e) {
				_prefStore.removePropertyChangeListener(_statWeek_PrefChangeListener);
			}
		});
	}

	@Override
	public void createStatisticUI(final Composite parent, final IViewSite viewSite) {

		super.createStatisticUI(parent, viewSite);

		addPrefListener(parent);
		getPreferences();
	}

	@Override
	ChartDataModel getChartDataModel() {

		final ChartDataModel chartDataModel = new ChartDataModel(ChartType.BAR);

		createXData_Week(chartDataModel);

		if (_isShowDistance) {
			createYData_Distance(chartDataModel);
		}

		if (_isShowAltitude) {
			createYData_Altitude(chartDataModel);
		}

		if (_isShowDuration) {
			createYData_Duration(chartDataModel);
		}

		if (_isShowNumTours) {
			createYData_NumTours(chartDataModel);
		}

		return chartDataModel;
	}

	@Override
	protected String getGridPrefPrefix() {
		return GRID_WEEK_SUMMARY;
	}

	private void getPreferences() {

		_isShowAltitude = _prefStore.getBoolean(ITourbookPreferences.STAT_WEEK_IS_SHOW_ALTITUDE);
		_isShowDistance = _prefStore.getBoolean(ITourbookPreferences.STAT_WEEK_IS_SHOW_DISTANCE);
		_isShowDuration = _prefStore.getBoolean(ITourbookPreferences.STAT_WEEK_IS_SHOW_DURATION);
		_isShowNumTours = _prefStore.getBoolean(ITourbookPreferences.STAT_WEEK_IS_SHOW_NUMBER_OF_TOURS);
	}

	@Override
	protected void setupStatisticSlideout(final SlideoutStatisticOptions slideout) {

		slideout.setStatisticOptions(new ChartOptions_WeekSummary());
	}
}
