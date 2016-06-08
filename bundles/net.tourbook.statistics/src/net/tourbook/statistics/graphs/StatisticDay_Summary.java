/*******************************************************************************
 * Copyright (C) 2005, 2016 Wolfgang Schramm and Contributors
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
import net.tourbook.statistic.ChartOptions_DaySummary;
import net.tourbook.statistic.SlideoutStatisticsChartOptions;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.IPostSelectionProvider;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IViewSite;

public class StatisticDay_Summary extends StatisticDay {

	private final IPreferenceStore	_prefStore	= TourbookPlugin.getPrefStore();
	private IPropertyChangeListener	_statDay_PrefChangeListener;

	private boolean					_isShowDistance;
	private boolean					_isShowAltitude;
	private boolean					_isShowDuration;
	private boolean					_isShowAvgPace;
	private boolean					_isShowAvgSpeed;

	private void addPrefListener(final Composite container) {

		// create pref listener
		_statDay_PrefChangeListener = new IPropertyChangeListener() {
			@Override
			public void propertyChange(final PropertyChangeEvent event) {

				final String property = event.getProperty();

				// observe which data are displayed
				if (property.equals(ITourbookPreferences.STAT_DAY_IS_SHOW_ALTITUDE)
						|| property.equals(ITourbookPreferences.STAT_DAY_IS_SHOW_DISTANCE)
						|| property.equals(ITourbookPreferences.STAT_DAY_IS_SHOW_DURATION)
						|| property.equals(ITourbookPreferences.STAT_DAY_IS_SHOW_AVG_PACE)
						|| property.equals(ITourbookPreferences.STAT_DAY_IS_SHOW_AVG_SPEED)) {

					// get the changed preferences
					getPreferences();

					// update chart
					preferencesHasChanged();
				}
			}
		};

		// add pref listener
		_prefStore.addPropertyChangeListener(_statDay_PrefChangeListener);

		// remove pref listener
		container.addDisposeListener(new DisposeListener() {
			@Override
			public void widgetDisposed(final DisposeEvent e) {
				_prefStore.removePropertyChangeListener(_statDay_PrefChangeListener);
			}
		});
	}

	@Override
	public void createStatisticUI(	final Composite parent,
									final IViewSite viewSite,
									final IPostSelectionProvider postSelectionProvider) {

		super.createStatisticUI(parent, viewSite, postSelectionProvider);

		addPrefListener(parent);
		getPreferences();
	}

	@Override
	ChartDataModel getChartDataModel() {

		final ChartDataModel chartDataModel = new ChartDataModel(ChartType.BAR);

		createXDataDay(chartDataModel);

		if (_isShowDistance) {
			createYDataDistance(chartDataModel);
		}

		if (_isShowAltitude) {
			createYDataAltitude(chartDataModel);
		}

		if (_isShowDuration) {
			createYDataDuration(chartDataModel);
		}

		if (_isShowAvgPace) {
			createYDataAvgPace(chartDataModel);
		}

		if (_isShowAvgSpeed) {
			createYDataAvgSpeed(chartDataModel);
		}

		return chartDataModel;
	}

	private void getPreferences() {

		_isShowAltitude = _prefStore.getBoolean(ITourbookPreferences.STAT_DAY_IS_SHOW_ALTITUDE);
		_isShowAvgPace = _prefStore.getBoolean(ITourbookPreferences.STAT_DAY_IS_SHOW_AVG_PACE);
		_isShowAvgSpeed = _prefStore.getBoolean(ITourbookPreferences.STAT_DAY_IS_SHOW_AVG_SPEED);
		_isShowDistance = _prefStore.getBoolean(ITourbookPreferences.STAT_DAY_IS_SHOW_DISTANCE);
		_isShowDuration = _prefStore.getBoolean(ITourbookPreferences.STAT_DAY_IS_SHOW_DURATION);
	}

	@Override
	protected void setChartOptions(final SlideoutStatisticsChartOptions slideout) {

		slideout.setStatisticOptions(new ChartOptions_DaySummary());
	}

}
