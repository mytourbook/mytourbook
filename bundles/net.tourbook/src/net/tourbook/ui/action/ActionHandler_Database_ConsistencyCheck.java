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
package net.tourbook.ui.action;

import java.sql.Connection;
import java.sql.ResultSet;

import net.tourbook.Messages;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.database.TourDatabase;
import net.tourbook.ui.UI;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.widgets.Display;

public class ActionHandler_Database_ConsistencyCheck extends AbstractHandler {

   private void checkConsistency() {

      final String[] resultInfo = new String[1];
      final String[] error = new String[1];

      BusyIndicator.showWhile(Display.getCurrent(), new Runnable() {
         @Override
         public void run() {

            final String sql = UI.EMPTY_STRING

                  + "SELECT" //                                                     //$NON-NLS-1$

                  + " schemaname || '.' || tablename as TableName," //              //$NON-NLS-1$
                  + " SYSCS_UTIL.SYSCS_CHECK_TABLE(schemaname, tablename) AS OK" //$NON-NLS-1$

                  + " FROM sys.sysschemas s, sys.systables t" //                    //$NON-NLS-1$

                  + " WHERE s.schemaid = t.schemaid" //                             //$NON-NLS-1$
                  + "   and t.tabletype = 'T'"; //                                  //$NON-NLS-1$

            try (Connection conn = TourDatabase.getInstance().getConnection()) {

               final StringBuilder sbResult = new StringBuilder();

               sbResult.append(Messages.app_db_consistencyCheck_checkIsOK);

               final ResultSet result = conn.prepareStatement(sql).executeQuery();

               while (result.next()) {
                  sbResult.append("1".equals(result.getString(2)) ? "✓" : "✕"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                  sbResult.append(UI.SPACE2);
                  sbResult.append(result.getString(1));
                  sbResult.append(UI.NEW_LINE);
               }

               resultInfo[0] = sbResult.toString();

            } catch (final Exception e) {

               StatusUtil.log(e);

               String message = e.getMessage();

               final int maxLength = 3000;
               if (message.length() > maxLength) {
                  message = message.substring(0, maxLength) + "\n...\n...\n..."; //$NON-NLS-1$
               }

               error[0] = NLS.bind(Messages.app_db_consistencyCheck_checkFailed, message);
            }
         }
      });

      if (error[0] == null) {

         MessageDialog.openInformation(
               Display.getCurrent().getActiveShell(),
               Messages.app_db_consistencyCheck_dlgTitle,
               resultInfo[0]);
      } else {

         MessageDialog.openError(
               Display.getCurrent().getActiveShell(),
               Messages.app_db_consistencyCheck_dlgTitle,
               error[0]);
      }
   }

   @Override
   public Object execute(final ExecutionEvent event) throws ExecutionException {

      checkConsistency();

      return null;
   }

}
