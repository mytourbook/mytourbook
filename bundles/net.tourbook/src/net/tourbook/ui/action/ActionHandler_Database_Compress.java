/*******************************************************************************
 * Copyright (C) 2005, 2026 Wolfgang Schramm and Contributors
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
package net.tourbook.ui.action;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.database.DialogCompressH2Database;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.tour.TourManager;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.widgets.Display;

public class ActionHandler_Database_Compress extends AbstractHandler {

   private static final IPreferenceStore _prefStore = TourbookPlugin.getPrefStore();

   @Override
   public Object execute(final ExecutionEvent event) throws ExecutionException {

      if (TourManager.isTourModified()) {
         return null;
      }

      final boolean isExternalDatabase = _prefStore.getBoolean(ITourbookPreferences.TOUR_DATABASE_IS_EXTERNAL);

      if (isExternalDatabase) {

         MessageDialog.openInformation(

               Display.getDefault().getActiveShell(),
               Messages.App_Db_Compress_DialogTitle,
               "This feature is currently only available for the internal tour database, however the external database is selected.");

         return null;
      }

      new DialogCompressH2Database().open();

      return null;
   }

}
