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

package pixelitor.gui;

import java.awt.Component;
import java.awt.EventQueue;

import pixelitor.utils.MessageHandler;
import pixelitor.utils.ProgressHandler;

/**
 * GUI-based implementation of MessageHandler.
 * This is used normally (except in unit-testing code).
 */
public class GUIMessageHandler implements MessageHandler {
   public GUIMessageHandler() {}

   @Override
   public void showError(final String title, final String msg, final Component parent) {
//        Dialogs.showErrorDialog(parent, title, msg);
   }

   @Override
   public void showException(final Throwable exception) {
//        Dialogs.showExceptionDialog(exception);
   }

   @Override
   public void showException(final Throwable exception, final Thread srcThread) {
//        Dialogs.showExceptionDialog(exception, srcThread);
   }

   @Override
   public void showExceptionOnEDT(final Throwable exception) {
      EventQueue.invokeLater(() -> showException(exception));
   }

   @Override
   public void showInfo(final String title, final String msg, final Component parent) {
//        Dialogs.showInfoDialog(parent, title, msg);
   }

   @Override
   public void showInStatusBar(final String msg) {

//        assert calledOnEDT() : threadInfo();
//
//        StatusBar.get().updateMessage(msg);
   }

   @Override
   public void showWarning(final String title, final String msg, final Component parent) {
//        Dialogs.showWarningDialog(parent, title, msg);
   }

   @Override
   public boolean showYesNoQuestion(final String title, final String msg) {

//        return Dialogs.showYesNoQuestionDialog(title, msg);

      return false;
   }

   @Override
   public ProgressHandler startProgress(final String msg, final int maxValue) {

//       assert calledOnEDT() : threadInfo();
//
      return StatusBar.get().startProgress(msg, maxValue);
   }
}
