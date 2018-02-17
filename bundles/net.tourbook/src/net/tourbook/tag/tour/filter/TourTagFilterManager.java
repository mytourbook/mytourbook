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
package net.tourbook.tag.tour.filter;

import net.tourbook.application.ActionTourTagFilter;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.preferences.ITourbookPreferences;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.widgets.Display;
import org.osgi.framework.Bundle;

public class TourTagFilterManager {

	private static final Bundle				_bundle				= TourbookPlugin.getDefault().getBundle();

	private final static IPreferenceStore	_prefStore			= TourbookPlugin.getPrefStore();

	private static boolean					_isTourFilterEnabled;

	private static int[]					_fireEventCounter	= new int[1];

	private static ActionTourTagFilter		_actionTourTagFilter;

	/**
	 * Fire event that the tour filter has changed.
	 */
	static void fireFilterModifyEvent() {

		_fireEventCounter[0]++;

		Display.getDefault().asyncExec(new Runnable() {

			final int __runnableCounter = _fireEventCounter[0];

			@Override
			public void run() {

				// skip all events which has not yet been executed
				if (__runnableCounter != _fireEventCounter[0]) {

					// a new event occured
					return;
				}

				_prefStore.setValue(ITourbookPreferences.APP_DATA_FILTER_IS_MODIFIED, Math.random());
			}
		});

	}

	public static void restoreState() {

		_isTourFilterEnabled = _prefStore.getBoolean(ITourbookPreferences.APP_TOUR_FILTER_IS_SELECTED);

		_actionTourTagFilter.setSelection(_isTourFilterEnabled);

	}

	public static void saveState() {

		_prefStore.setValue(ITourbookPreferences.APP_TOUR_FILTER_IS_SELECTED, _actionTourTagFilter.getSelection());

	}

	/**
	 * Sets the state if the tour filter is active or not.
	 * 
	 * @param isEnabled
	 */
	public static void setFilterEnabled(final boolean isEnabled) {

		_isTourFilterEnabled = isEnabled;

		fireFilterModifyEvent();
	}

	public static void setTourTagFilterAction(final ActionTourTagFilter actionTourTagFilter) {
		_actionTourTagFilter = actionTourTagFilter;
	}

}
