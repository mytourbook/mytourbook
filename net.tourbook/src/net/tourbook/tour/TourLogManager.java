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
package net.tourbook.tour;

import java.util.concurrent.CopyOnWriteArrayList;

import net.tourbook.common.UI;
import net.tourbook.common.util.Util;

public class TourLogManager {

	public static final String							LOG_DELETE_TOUR	= "Tour deleted: %s";			//$NON-NLS-1$
	//
	private static final CopyOnWriteArrayList<TourLog>	_tourLogs		= new CopyOnWriteArrayList<>();

	private static TourLogView							_logView;

	public static void addLog(final TourLogState logState, final String message) {

		final TourLog importLog = new TourLog(logState, message);

		// update model
		_tourLogs.add(importLog);

		// update UI
		if (isTourLogOpen()) {
			_logView.addLog(importLog);
		}
	}

	public static void clear() {

		_logView.clear();
		_tourLogs.clear();
	}

	public static CopyOnWriteArrayList<TourLog> getLogs() {

		return _tourLogs;
	}

	private static boolean isTourLogOpen() {

		final boolean isLogViewOpen = _logView != null && _logView.isDisposed();

		System.out.println((UI.timeStampNano() + " [" + TourLogManager.class.getSimpleName() + "] ")
				+ ("\tisLogViewOpen: " + isLogViewOpen));
		// TODO remove SYSTEM.OUT.PRINTLN

		return isLogViewOpen;
	}

	public static void openLogView() {

		if (_logView == null || _logView.isDisposed()) {

			_logView = (TourLogView) Util.showView(TourLogView.ID, true);
		}
	}

	public static void setLogView(final TourLogView tourLogView) {

		_logView = tourLogView;
	}
}
