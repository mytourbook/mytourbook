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
package net.tourbook.statistic;

import java.util.ArrayList;

import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.util.StringToArrayConverter;
import net.tourbook.preferences.ITourbookPreferences;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.preference.IPreferenceStore;

public class StatisticManager {

	private static ArrayList<TourbookStatistic>	_statisticExtensionPoints;

	/**
	 * This method is synchronized to conform to FindBugs
	 * 
	 * @return Returns statistics from the extension registry in the sort order of the registry
	 */
	public static synchronized ArrayList<TourbookStatistic> getStatisticExtensionPoints() {

		if (_statisticExtensionPoints != null) {
			return _statisticExtensionPoints;
		}

		_statisticExtensionPoints = new ArrayList<TourbookStatistic>();

		final IExtensionPoint extPoint = Platform.getExtensionRegistry().getExtensionPoint(
				TourbookPlugin.PLUGIN_ID,
				TourbookPlugin.EXT_POINT_STATISTIC_YEAR);

		if (extPoint != null) {

			for (final IExtension extension : extPoint.getExtensions()) {

				for (final IConfigurationElement configElement : extension.getConfigurationElements()) {

					if (configElement.getName().equalsIgnoreCase("statistic")) { //$NON-NLS-1$

						Object object;
						try {
							object = configElement.createExecutableExtension("class"); //$NON-NLS-1$
							if (object instanceof TourbookStatistic) {

								final TourbookStatistic statisticItem = (TourbookStatistic) object;

								statisticItem.visibleName = configElement.getAttribute("name"); //$NON-NLS-1$
								statisticItem.statisticId = configElement.getAttribute("id"); //$NON-NLS-1$

								_statisticExtensionPoints.add(statisticItem);
							}
						} catch (final CoreException e) {
							e.printStackTrace();
						}
					}
				}
			}
		}

		return _statisticExtensionPoints;
	}

	/**
	 * @return Returns statistic providers with the custom sort order
	 */
	public static ArrayList<TourbookStatistic> getStatisticProviders() {

		final ArrayList<TourbookStatistic> availableStatistics = getStatisticExtensionPoints();
		final ArrayList<TourbookStatistic> visibleStatistics = new ArrayList<TourbookStatistic>();

		final IPreferenceStore prefStore = TourbookPlugin.getPrefStore();
		final String providerIds = prefStore.getString(ITourbookPreferences.STATISTICS_STATISTIC_PROVIDER_IDS);

		final String[] prefStoreStatisticIds = StringToArrayConverter.convertStringToArray(providerIds);

		// get all statistics which are saved in the pref store
		for (final String statisticId : prefStoreStatisticIds) {

			// get statistic item from the id
			for (final TourbookStatistic tourbookStatistic : availableStatistics) {
				if (statisticId.equals(tourbookStatistic.statisticId)) {
					visibleStatistics.add(tourbookStatistic);
					break;
				}
			}
		}

		// get statistics which are available but not saved in the prefstore
		for (final TourbookStatistic availableStatistic : availableStatistics) {

			if (visibleStatistics.contains(availableStatistic) == false) {
				visibleStatistics.add(availableStatistic);
			}
		}

		return visibleStatistics;
	}
}
