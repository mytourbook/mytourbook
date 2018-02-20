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

import java.util.ArrayList;
import java.util.Set;

import net.tourbook.application.ActionTourTagFilter;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.util.Util;
import net.tourbook.data.TourTag;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.tour.filter.TourFilterSQLData;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.widgets.Display;

public class TourTagFilterManager {

	private final static IPreferenceStore	_prefStore					= TourbookPlugin.getPrefStore();
	private static final IDialogSettings	_state						= TourbookPlugin.getState(
			"TourTagFilterSlideout");																	//$NON-NLS-1$

	private static final String				STATE_TOUR_TAG_FILTER_IDS	= "STATE_TOUR_TAG_FILTER_IDS";

	private static boolean					_isTourTagFilterEnabled;

	private static int[]					_fireEventCounter			= new int[1];

	private static ActionTourTagFilter		_actionTourTagFilter;

	/**
	 * Contains all tour tag id's which are filtering the tours
	 */
	private static long[]					_tourTagFilterIds			= new long[0];

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

	/**
	 * @return Returns sql where part for the tag filter or <code>null</code> when tag filter is
	 *         disabled.
	 */
	public static TourFilterSQLData getSQL() {

		if (_isTourTagFilterEnabled == false || _tourTagFilterIds.length == 0) {

			// tour tag filter is not enabled

			return null;
		}

		final ArrayList<Object> sqlParameters = new ArrayList<>();

		boolean isFirst = true;
		final StringBuilder sb = new StringBuilder();

		for (final long tourTagId : _tourTagFilterIds) {

			if (isFirst) {
				isFirst = false;
				sb.append(" ?"); //$NON-NLS-1$
			} else {
				sb.append(", ?"); //$NON-NLS-1$
			}

			sqlParameters.add(tourTagId);
		}

		final String sqlWhere = " AND jTdataTtag.TourTag_tagId IN (" + sb.toString() + ") \n"; //$NON-NLS-1$ //$NON-NLS-2$

		final TourFilterSQLData tourFilterSQLData = new TourFilterSQLData(sqlWhere, sqlParameters);

		return tourFilterSQLData;
	}

	/**
	 * @return Returns all tour tag id's which are filtering the tours
	 */
	static long[] getTourTagFilterIds() {
		return _tourTagFilterIds;
	}

	public static void restoreState() {

		// is filter enabled
		_isTourTagFilterEnabled = _prefStore.getBoolean(ITourbookPreferences.APP_TOUR_TAG_FILTER_IS_SELECTED);
		_actionTourTagFilter.setSelection(_isTourTagFilterEnabled);

		// tour tag id's
		final long[] tagFilterIds = Util.getStateLongArray(_state, STATE_TOUR_TAG_FILTER_IDS, null);
		if (tagFilterIds != null) {
			_tourTagFilterIds = tagFilterIds;
		}
	}

	public static void saveState() {

		_prefStore.setValue(ITourbookPreferences.APP_TOUR_TAG_FILTER_IS_SELECTED, _actionTourTagFilter.getSelection());

		Util.setState(_state, STATE_TOUR_TAG_FILTER_IDS, _tourTagFilterIds);

	}

	/**
	 * Sets the state if the tour filter is active or not.
	 * 
	 * @param isEnabled
	 */
	public static void setFilterEnabled(final boolean isEnabled) {

		_isTourTagFilterEnabled = isEnabled;

		fireFilterModifyEvent();
	}

	public static void setTourTagFilterAction(final ActionTourTagFilter actionTourTagFilter) {

		_actionTourTagFilter = actionTourTagFilter;
	}

	static void updateTourTagFilter(final Set<TourTag> tourTags) {

		_tourTagFilterIds = new long[tourTags.size()];

		int tagIndex = 0;

		for (final TourTag tourTag : tourTags) {
			_tourTagFilterIds[tagIndex++] = tourTag.getTagId();
		}
	}

}
