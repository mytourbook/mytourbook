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

import net.tourbook.Messages;
import net.tourbook.common.UI;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.common.util.Util;
import net.tourbook.web.WEB;

import org.eclipse.swt.widgets.Display;

public class TourLogManager {

	public static final String							LOG_TOUR_DELETE_TOURS		= Messages.Log_Tour_DeleteTours;
	public static final String							LOG_TOUR_SAVE_TOURS			= Messages.Log_Tour_SaveTours;
	public static final String							LOG_TOUR_SAVE_TOURS_FILE	= Messages.Log_Tour_SaveTours_File;

	private static final CopyOnWriteArrayList<TourLog>	_tourLogs					= new CopyOnWriteArrayList<>();

	private static TourLogView							_logView;

	private static void addLog(final TourLog tourLog) {

		// update model
		_tourLogs.add(tourLog);

		// update UI
		if (isTourLogOpen()) {
			_logView.addLog(tourLog);
		}
	}

	public static void addLog(final TourLogState logState, final String message) {

		final TourLog tourLog = new TourLog(logState, message);

		addLog(tourLog);
	}

	public static void addLog(final TourLogState logState, final String message, final String css) {

		final TourLog tourLog = new TourLog(logState, message);

		tourLog.css = css;

		addLog(tourLog);
	}

	public static void addSubLog(final TourLogState logState, final String message) {

		final TourLog tourLog = new TourLog(logState, message);

		tourLog.isSubLogItem = true;

		addLog(tourLog);
	}

	public static void clear() {

		_logView.clear();
		_tourLogs.clear();
	}

	public static CopyOnWriteArrayList<TourLog> getLogs() {

		return _tourLogs;
	}

	private static boolean isTourLogOpen() {

		final boolean isLogViewOpen = _logView != null && _logView.isDisposed() == false;

		return isLogViewOpen;
	}

	public static void logError(final String message) {

		final TourLog tourLog = new TourLog(TourLogState.IMPORT_ERROR, message);

		addLog(tourLog);
	}

	public static void logEx(final Exception e) {

		final String stackTrace = Util.getStackTrace(e);

		logException(stackTrace, e);
	}

	public static void logEx(final String message, final Exception e) {

		final String stackTrace = Util.getStackTrace(e);

		logException(message + UI.NEW_LINE + stackTrace, e);
	}

	private static void logException(final String message, final Exception e) {

		final String logMessage = WEB.convertHTML_LineBreaks(message);

		final TourLog tourLog = new TourLog(TourLogState.IMPORT_EXCEPTION, logMessage);

		addLog(tourLog);

		Display.getDefault().asyncExec(new Runnable() {
			@Override
			public void run() {
				openLogView();
			}
		});

		// ensure it is logged when crashing
		StatusUtil.log(e);
	}

	public static void logInfo(final String message) {

		final TourLog tourLog = new TourLog(TourLogState.IMPORT_INFO, message);

		tourLog.css = TourLogView.CSS_LOG_INFO;

		addLog(tourLog);
	}

	public static void logSubInfo(final String message) {

		final TourLog tourLog = new TourLog(TourLogState.IMPORT_INFO, message);

		tourLog.css = TourLogView.CSS_LOG_INFO;
		tourLog.isSubLogItem = true;

		addLog(tourLog);
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
