/*******************************************************************************
 * Copyright (C) 2005, 2021 Wolfgang Schramm and Contributors
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
import net.tourbook.common.util.StatusUtil;
import net.tourbook.common.util.Util;
import net.tourbook.web.WEB;

import org.eclipse.swt.widgets.Display;

public class TourLogManager {

   private static final CopyOnWriteArrayList<TourLog> _allTourLogs = new CopyOnWriteArrayList<>();

   private static TourLogView                         _logView;

   private static void addLog(final TourLog tourLog) {

      // update model
      _allTourLogs.add(tourLog);

      // update UI
      if (isTourLogOpen()) {
         _logView.addLog(tourLog);
      }
   }

   public static void addLog(final TourLogState logState, final String message, final String css) {

      final TourLog tourLog = new TourLog(logState, message);

      tourLog.css = css;

      addLog(tourLog);
   }

   public static void addSubLog(final TourLogState tourLogState, final String message) {

      final TourLog tourLog = new TourLog(tourLogState, message);

      tourLog.isSubLogItem = true;

      addLog(tourLog);
   }

   public static void clear() {

      _logView.clear();
      _allTourLogs.clear();

      TourLog.clear();
   }

   public static CopyOnWriteArrayList<TourLog> getLogs() {

      return _allTourLogs;
   }

   private static boolean isTourLogOpen() {

      final boolean isLogViewOpen = _logView != null && _logView.isDisposed() == false;

      return isLogViewOpen;
   }

   /**
    * Log message and do not show an icon
    *
    * @param message
    */
   public static void log_DEFAULT(final String message) {

      final String logMessage = WEB.convertHTML_LineBreaks(message);

      final TourLog tourLog = new TourLog(TourLogState.DEFAULT, logMessage);

      addLog(tourLog);
   }

   public static void log_ERROR(final String message) {

      final TourLog tourLog = new TourLog(TourLogState.ERROR, message);

      addLog(tourLog);
   }

   public static void log_ERROR_CannotReadDataFile(final String importFilePath, final Exception e) {

      log_EXCEPTION_WithStacktrace(String.format("Could not read data file '%s'", importFilePath), e); //$NON-NLS-1$
   }

   private static void log_EXCEPTION(final String message, final Exception e) {

      final String logMessage = WEB.convertHTML_LineBreaks(message);

      final TourLog tourLog = new TourLog(TourLogState.EXCEPTION, logMessage);

      addLog(tourLog);

      Display.getDefault().syncExec(new Runnable() {
         @Override
         public void run() {
            showLogView();
         }
      });

      // ensure it is logged when crashing
      StatusUtil.log(e);
   }

   public static void log_EXCEPTION_WithStacktrace(final Exception e) {

      final String stackTrace = Util.getStackTrace(e);

      log_EXCEPTION(stackTrace, e);
   }

   public static void log_EXCEPTION_WithStacktrace(final String message, final Exception e) {

      final String stackTrace = Util.getStackTrace(e);

      log_EXCEPTION(message + UI.NEW_LINE + stackTrace, e);
   }

   public static void log_INFO(final String message) {

      final String logMessage = WEB.convertHTML_LineBreaks(message);

      final TourLog tourLog = new TourLog(TourLogState.INFO, logMessage);

      tourLog.css = TourLogView.CSS_LOG_INFO;

      addLog(tourLog);
   }

   /**
    * Log message and show OK icon
    *
    * @param message
    */
   public static void log_OK(final String message) {

      final String logMessage = WEB.convertHTML_LineBreaks(message);

      final TourLog tourLog = new TourLog(TourLogState.OK, logMessage);

      addLog(tourLog);
   }

   public static void log_TITLE(final String message) {

      final String logMessage = WEB.convertHTML_LineBreaks(message);

      final TourLog tourLog = new TourLog(TourLogState.DEFAULT, logMessage);

      tourLog.css = TourLogView.CSS_LOG_TITLE;

      addLog(tourLog);
   }

   public static void setLogView(final TourLogView tourLogView) {

      _logView = tourLogView;
   }

   public static void showLogView() {

      _logView = (TourLogView) Util.showView(TourLogView.ID,

            // !!! log view MUST NOT be activated, otherwise events (e.g. deletion) are NOT fired from the source view !!!

            false);
   }

   /**
    * Indent log message and do not show an icon
    *
    * @param message
    */
   public static void subLog_DEFAULT(final String message) {

      final String logMessage = WEB.convertHTML_LineBreaks(message);

      final TourLog tourLog = new TourLog(TourLogState.DEFAULT, logMessage);
      tourLog.isSubLogItem = true;

      addLog(tourLog);
   }

   /**
    * Indent log message and show error icon
    *
    * @param message
    */
   public static void subLog_ERROR(final String message) {

      final String logMessage = WEB.convertHTML_LineBreaks(message);

      final TourLog tourLog = new TourLog(TourLogState.ERROR, logMessage);
      tourLog.isSubLogItem = true;

      addLog(tourLog);
   }

   /**
    * Indent log message and show info icon
    *
    * @param message
    */
   public static void subLog_INFO(final String message) {

      final String logMessage = WEB.convertHTML_LineBreaks(message);

      final TourLog tourLog = new TourLog(TourLogState.INFO, logMessage);

      tourLog.css = TourLogView.CSS_LOG_INFO;
      tourLog.isSubLogItem = true;

      addLog(tourLog);
   }

   /**
    * Indent log message and show OK icon
    *
    * @param message
    */
   public static void subLog_OK(final String message) {

      final String logMessage = WEB.convertHTML_LineBreaks(message);

      final TourLog tourLog = new TourLog(TourLogState.OK, logMessage);
      tourLog.isSubLogItem = true;

      addLog(tourLog);
   }
}
