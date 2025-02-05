/*
 * Copyright 2024 Laszlo Balazs-Csiki and Contributors
 *
 * This file is part of Pixelitor. Pixelitor is free software: you
 * can redistribute it and/or modify it under the terms of the GNU
 * General Public License, version 3 as published by the Free
 * Software Foundation.
 *
 * Pixelitor is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Pixelitor. If not, see <http://www.gnu.org/licenses/>.
 */
package pixelitor.utils;

import static java.lang.String.format;

import java.awt.Component;
import java.io.File;
import java.util.Objects;

/**
 * Centralized message handling for the display of status messages,
 * dialogs, and progress bars through a pluggable message handler.
 */
public class Messages {
   private static MessageHandler msgHandler;

   private Messages() {
      // only static methods, should not be instantiated
   }

   public static void clearStatusBar() {
      showPlainStatusMessage(""); //$NON-NLS-1$
   }

   // Must be set before any other methods are used.
   public static void setHandler(final MessageHandler msgHandler) {
      Messages.msgHandler = Objects.requireNonNull(msgHandler);
   }

   public static void showBulkSaveMessage(final int numFiles, final File dir) {
      assert dir.isDirectory();
      showStatusMessage(numFiles + " files saved to <b>" + dir.getAbsolutePath() + "</b>"); //$NON-NLS-1$ //$NON-NLS-2$
   }

   public static void showError(final String title, final String message) {
      msgHandler.showError(title, message, null);
   }

   public static void showError(final String title, final String message, final Component parent) {
      msgHandler.showError(title, message, parent);
   }

   public static void showException(final Throwable exception) {
      msgHandler.showException(exception);
   }

   public static void showException(final Throwable exception, final Thread srcThread) {
      msgHandler.showException(exception, srcThread);
   }

   public static <T> T showExceptionOnEDT(final Throwable exception) {
      msgHandler.showExceptionOnEDT(exception);
      // Returns a null of the desired type...
      // This way it fits into CompletableFuture.exceptionally.
      return null;
   }

   public static void showFileSavedMessage(final File file) {
      showStatusMessage("<b>" + file.getAbsolutePath() + "</b> was saved."); //$NON-NLS-1$ //$NON-NLS-2$
   }

   public static void showInfo(final String title, final String message) {
      msgHandler.showInfo(title, message, null);
   }

   public static void showInfo(final String title, final String message, final Component parent) {
      msgHandler.showInfo(title, message, parent);
   }

   // Shows a performance timing message in the status bar.
   public static void showPerformanceMessage(final String filterName, final long timeMillis) {
      String msg;
      if (timeMillis < 1000) {
         msg = filterName + " took " + timeMillis + " ms"; //$NON-NLS-1$ //$NON-NLS-2$
      } else {
         final float seconds = timeMillis / 1000.0f;
         msg = format("%s took %.1f s", filterName, seconds); //$NON-NLS-1$
      }
      showPlainStatusMessage(msg);
   }

   /**
    * Shows a non-HTML text message in the status bar.
    */
   public static void showPlainStatusMessage(final String msg) {
      assert !msg.startsWith("<html>"); //$NON-NLS-1$
      msgHandler.showInStatusBar(msg);
   }

   public static boolean showReloadFileQuestion(final File file) {
      final String title = "Reload " + file.getName() + "?"; //$NON-NLS-1$ //$NON-NLS-2$
      final String msg = "<html>The file <b>" + file.getAbsolutePath() //$NON-NLS-1$
            + "</b><br> has been modified by another program." + //$NON-NLS-1$
            "<br><br>Do you want to reload it?"; //$NON-NLS-1$
      return showYesNoQuestion(title, msg);
   }

   public static void showSmartObjectUnsupportedWarning(final String what) {
      msgHandler.showInfo("Feature Not Supported", //$NON-NLS-1$
            what + " isn't yet supported if one of the layers is a smart object.", //$NON-NLS-1$
            null);
   }

   /**
    * Shows an HTML text message in the status bar.
    */
   public static void showStatusMessage(final String msg) {
      assert !msg.startsWith("<html>"); //$NON-NLS-1$
      msgHandler.showInStatusBar("<html>" + msg); //$NON-NLS-1$
   }

   public static void showWarning(final String title, final String message) {
      msgHandler.showWarning(title, message, null);
   }

   public static void showWarning(final String title, final String message, final Component parent) {
      msgHandler.showWarning(title, message, parent);
   }

   public static boolean showYesNoQuestion(final String title, final String msg) {
      return msgHandler.showYesNoQuestion(title, msg);
   }

   public static ProgressHandler startProgress(final String msg, final int max) {
      return msgHandler.startProgress(msg, max);
   }

}
