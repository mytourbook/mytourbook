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

import de.byteholder.geoclipse.preferences.IMappingPreferences;

import gnu.trove.list.array.TIntArrayList;

import java.sql.Connection;
import java.sql.ResultSet;
import java.util.ArrayList;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.font.MTFont;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.database.TourDatabase;
import net.tourbook.ui.UI;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

public class ActionHandler_Database_SizeInfo extends AbstractHandler {

   private static final String NL = UI.NEW_LINE;

   private class MessageDialogLogging extends MessageDialog {

      private String _logText;
      private Font   _monoFont;

      public MessageDialogLogging(final Shell parentShell,
                                  final String dialogTitle,
                                  final String logText) {

         super(parentShell,
               dialogTitle,
               null,
               UI.EMPTY_STRING,
               0, //INFORMATION,
               0,
               new String[] { IDialogConstants.OK_LABEL });

         _logText = logText;

         parentShell.addDisposeListener(new DisposeListener() {

            @Override
            public void widgetDisposed(final DisposeEvent e) {

               if (_monoFont != null) {
                  _monoFont.dispose();
               }
            }
         });

      }

      @Override
      protected Control createCustomArea(final Composite parent) {

         createMonoFont(parent.getDisplay());

         final Text txtLog = new Text(parent, SWT.MULTI);
         txtLog.setFont(_monoFont);
         txtLog.setText(_logText);
         GridDataFactory.fillDefaults().grab(true, true).applyTo(txtLog);

         return txtLog;
      }

      private void createMonoFont(final Display display) {

         final IPreferenceStore prefStore = TourbookPlugin.getPrefStore();

         final String loggingFontPrefs = prefStore.getString(IMappingPreferences.THEME_FONT_LOGGING);
         if (loggingFontPrefs.length() > 0) {
            try {
               _monoFont = new Font(display, new FontData(loggingFontPrefs));
            } catch (final Exception e) {
               // ignore
            }
         }

         if (_monoFont == null) {
            _monoFont = new Font(display, MTFont.DEFAULT_MONO_FONT, 9, SWT.NORMAL);
         }
      }

   }

   private void checkConsistency() {

      final String[] resultInfo = new String[1];
      final String[] error = new String[1];

      BusyIndicator.showWhile(Display.getCurrent(), new Runnable() {
         @Override
         public void run() {

            final String sql = UI.EMPTY_STRING

                  + " SELECT" + NL //                                                              //$NON-NLS-1$
                  + "   T2.CONGLOMERATENAME," + NL //                                           1  //$NON-NLS-1$
                  + "	((NUMALLOCATEDPAGES + NUMFREEPAGES) * PAGESIZE) as USEDSPACE," + NL //  2  //$NON-NLS-1$
                  + "	T2.ESTIMSPACESAVING," + NL //                                           3  //$NON-NLS-1$
                  + "	T2.ISINDEX," + NL //                                                    4  //$NON-NLS-1$
                  + "	T2.NUMALLOCATEDPAGES," + NL //                                          5  //$NON-NLS-1$
                  + "	T2.NUMFREEPAGES," + NL //                                               6  //$NON-NLS-1$
                  + "	T2.NUMUNFILLEDPAGES," + NL //                                           7  //$NON-NLS-1$
                  + "	T2.PAGESIZE" + NL //                                                    8  //$NON-NLS-1$

                  + " FROM" + NL //                                                                //$NON-NLS-1$
                  + "    SYS.SYSTABLES systabs," + NL //                                           //$NON-NLS-1$
                  + "    SYS.SYSSCHEMAS sysschemas," + NL //                                       //$NON-NLS-1$
                  + "    TABLE (SYSCS_DIAG.SPACE_TABLE()) AS T2" + NL //                           //$NON-NLS-1$

                  + " WHERE systabs.tabletype = 'T'" + NL //                                       //$NON-NLS-1$
                  + "    AND sysschemas.schemaid = systabs.schemaid" + NL //                       //$NON-NLS-1$
                  + "    AND systabs.tableid = T2.tableid" + NL //                                 //$NON-NLS-1$

                  + " ORDER BY ISINDEX, CONGLOMERATENAME" + NL //                                  //$NON-NLS-1$
            ;

            try (Connection conn = TourDatabase.getInstance().getConnection()) {

               final ArrayList<String> allNames = new ArrayList<>();
               final TIntArrayList allUsedSpaces = new TIntArrayList();
               final TIntArrayList allSpaceSavings = new TIntArrayList();
               final TIntArrayList allIsIndex = new TIntArrayList();

               final ResultSet result = conn.prepareStatement(sql).executeQuery();

               while (result.next()) {

                  allNames.add(result.getString(1));
                  allUsedSpaces.add(result.getInt(2));
                  allSpaceSavings.add(result.getInt(3));
                  allIsIndex.add(result.getInt(4));
               }

               // get width of the largest name
               int maxNameWidth = 0;
               for (final String name : allNames) {
                  if (name.length() > maxNameWidth) {
                     maxNameWidth = name.length();
                  }
               }

               final int valueWidth = 10;
               final String header1Format = " %-" + (maxNameWidth + 2) + "s" + " %" + valueWidth + "s %" + valueWidth + "s"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
               final String header2Format = " %-" + (maxNameWidth + 2) + "s" + " %" + valueWidth + "s %" + valueWidth + "s"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
               final String valueFormat = " %-" + (maxNameWidth + 2) + "s" + " %" + valueWidth + "d %" + valueWidth + "d"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$

               final StringBuilder sb = new StringBuilder();

               sb.append(NL);
               sb.append(String.format(header1Format,
                     Messages.App_Db_SizeInfo_HeaderLabel_Table,
                     Messages.App_Db_SizeInfo_HeaderLabel_Used,
                     Messages.App_Db_SizeInfo_HeaderLabel_NotUsed));
               sb.append(NL);
               sb.append(NL);

               boolean isIndexTitleDisplayed = false;

               for (int rowIndex = 0; rowIndex < allNames.size(); rowIndex++) {

                  final String name = allNames.get(rowIndex);
                  final int usedSpace = allUsedSpaces.get(rowIndex);
                  final int spaceSavings = allSpaceSavings.get(rowIndex);

                  final boolean dbIsIndex = allIsIndex.get(rowIndex) == 1 ? true : false;

                  if (dbIsIndex && isIndexTitleDisplayed == false) {

                     sb.append(NL);
                     sb.append(NL);
                     sb.append(String.format(header1Format,
                           Messages.App_Db_SizeInfo_HeaderLabel_Index,
                           Messages.App_Db_SizeInfo_HeaderLabel_Used,
                           Messages.App_Db_SizeInfo_HeaderLabel_NotUsed));
                     sb.append(NL);
                     sb.append(NL);

                     isIndexTitleDisplayed = true;
                  }

                  sb.append(String.format(

                        valueFormat,

                        name,
                        usedSpace / 1024,
                        spaceSavings / 1024));

                  sb.append(NL);
               }

               resultInfo[0] = sb.toString();

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

         // no error

//         MessageDialogLogging.openInformation(
//               Display.getCurrent().getActiveShell(),
//               Messages.app_db_consistencyCheck_dlgTitle,
//               resultInfo[0]);

         new MessageDialogLogging(
               Display.getDefault().getActiveShell(),
               Messages.App_Db_SizeInfo_DialogTitle,
               resultInfo[0]).open();

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
