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
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.UI;
import net.tourbook.common.font.MTFont;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.database.TourDatabase;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

public class ActionHandler_Database_Compress extends AbstractHandler {

   private static final String       HEADER_ALLOCATED_PAGES_1 = "ALLOCATED"; //$NON-NLS-1$
   private static final String       HEADER_ALLOCATED_PAGES_2 = "PAGES";     //$NON-NLS-1$
   private static final String       HEADER_FREE_PAGES_1      = "FREE";      //$NON-NLS-1$
   private static final String       HEADER_FREE_PAGES_2      = "PAGES";     //$NON-NLS-1$
   private static final String       HEADER_PAGE_SIZE_1       = "PAGE";      //$NON-NLS-1$
   private static final String       HEADER_PAGE_SIZE_2       = "SIZE";      //$NON-NLS-1$
   private static final String       HEADER_UNFILLED_PAGES_1  = "UNFILLED";  //$NON-NLS-1$
   private static final String       HEADER_UNFILLED_PAGES_2  = "PAGES";     //$NON-NLS-1$

   private static final String       NL                       = UI.NEW_LINE1;

   private static final NumberFormat _nf0;

   static {

      _nf0 = NumberFormat.getNumberInstance();
      _nf0.setMinimumFractionDigits(0);
      _nf0.setMaximumFractionDigits(0);
   }

   private final IDialogSettings _state = TourbookPlugin.getState("net.tourbook.ui.action.ActionHandler_Database_Compress");//$NON-NLS-1$

   private List<String>          _allTableNames;
   private TIntArrayList         _allUsedSpaces;

   private DialogLogInfo         _dialogLogInfo;

   /*
    * UI controls
    */
   private Shell _dialogShell;

   private class DialogLogInfo extends Dialog {

      private String _logText = UI.EMPTY_STRING;

      /*
       * UI controls
       */
      private Text _txtLog;
      private Font _monoFont;

      DialogLogInfo() {

         super(Display.getDefault().getActiveShell());

         // make dialog resizable
         setShellStyle(getShellStyle() | SWT.RESIZE);
      }

      void appendLogText(final String newText) {

         final String newLogText = _logText.length() == 0
               ? newText
               : NL + NL + newText;

         _txtLog.setText(newLogText);
      }

      @Override
      protected void configureShell(final Shell shell) {

         super.configureShell(shell);

         _dialogShell = shell;

         // set window title
         shell.setText(Messages.App_Db_CompressTables_DialogTitle);

         shell.addDisposeListener(new DisposeListener() {

            @Override
            public void widgetDisposed(final DisposeEvent e) {

               if (_monoFont != null) {
                  _monoFont.dispose();
               }
            }
         });
      }

      @Override
      protected void createButtonsForButtonBar(final Composite parent) {

         {
            /*
             * Button: Compress by copying
             */
            final Button button = createButton(
                  parent,
                  IDialogConstants.CLIENT_ID + 1,
                  Messages.App_Db_CompressTables_Button_CompressByCopying,
                  false);

            button.addSelectionListener(new SelectionAdapter() {
               @Override
               public void widgetSelected(final SelectionEvent e) {
                  onCompress_ByCopying();
               }
            });
         }
         {
            /*
             * Button: Inplace compress
             */
            final Button button = createButton(
                  parent,
                  IDialogConstants.CLIENT_ID + 2,
                  Messages.App_Db_CompressTables_Button_CompressInplace,
                  false);

            button.addSelectionListener(new SelectionAdapter() {
               @Override
               public void widgetSelected(final SelectionEvent e) {
                  onCompress_Inplace();
               }
            });
         }

         // create close button
         createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CLOSE_LABEL, true);
      }

      @Override
      protected Control createDialogArea(final Composite parent) {

         createMonoFont(parent.getDisplay());

         _txtLog = new Text(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
         _txtLog.setFont(_monoFont);
         GridDataFactory.fillDefaults().grab(true, true).applyTo(_txtLog);

         return _txtLog;
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

      @Override
      protected IDialogSettings getDialogBoundsSettings() {

         // keep window size and position
         return _state;
      }
   }

   @Override
   public Object execute(final ExecutionEvent event) throws ExecutionException {

      _dialogLogInfo = new DialogLogInfo();
      _dialogLogInfo.setBlockOnOpen(false);
      _dialogLogInfo.open();

      _allUsedSpaces = new TIntArrayList();
      final String[] resultInfo = new String[1];
      final String[] error = new String[1];

      getDatabaseSize(_allUsedSpaces, resultInfo, error);

      if (error[0] == null) {

         // no error

         _dialogLogInfo.appendLogText(resultInfo[0]);

      } else {

         _dialogLogInfo.appendLogText(error[0]);
      }

      // resize dialog to show all data
      final Point defaultSize = _dialogShell.computeSize(SWT.DEFAULT, SWT.DEFAULT);
      _dialogShell.setSize(defaultSize);

      return null;
   }

   private void getDatabaseSize(final TIntArrayList allUsedSpaces, final String[] resultInfo, final String[] error) {

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

            try (final Connection conn = TourDatabase.getInstance().getConnection()) {

               final ArrayList<String> allNames = new ArrayList<>();

               final TIntArrayList allSpaceSavings = new TIntArrayList();
               final TIntArrayList allIsIndex = new TIntArrayList();

               final TIntArrayList allAllocatedPages = new TIntArrayList();
               final TIntArrayList allFreePages = new TIntArrayList();
               final TIntArrayList allUnfilledPages = new TIntArrayList();
               final TIntArrayList allPageSize = new TIntArrayList();

               _allTableNames = new ArrayList<>();

               final ResultSet result = conn.prepareStatement(sql).executeQuery();

               while (result.next()) {

                  final String name = result.getString(1);
                  allNames.add(name);

                  allUsedSpaces.add(result.getInt(2));
                  allSpaceSavings.add(result.getInt(3));
                  final int dbIsIndex = result.getInt(4);
                  allIsIndex.add(dbIsIndex);

                  allAllocatedPages.add(result.getInt(5));
                  allFreePages.add(result.getInt(6));
                  allUnfilledPages.add(result.getInt(7));
                  allPageSize.add(result.getInt(8));

                  if (dbIsIndex != 1) {

                     // item is a table
                     _allTableNames.add(name);
                  }
               }

               // get width of the largest table/index name
               int maxWidth_Name = 0;
               for (final String name : allNames) {
                  if (name.length() > maxWidth_Name) {
                     maxWidth_Name = name.length();
                  }
               }

// SET_FORMATTING_OFF

               int maxValueWidth = 11; // 123'456'789 kByte
               maxValueWidth = Math.max(maxValueWidth, UI.UNIT_KBYTE.length());

               final int maxWidth_Used             = Math.max(maxValueWidth, Messages.App_Db_CompressTables_HeaderLabel_Used.length());
               final int maxWidth_NotUsed          = Math.max(maxValueWidth, Messages.App_Db_CompressTables_HeaderLabel_NotUsed.length());

               final String lineFormat = UI.EMPTY_STRING

                     + UI.SPACE                          // empty column

                     + "%-"   + maxWidth_Name      + "s" // CONGLOMERATENAME     //$NON-NLS-1$ //$NON-NLS-2$
                     + "  %"  + maxWidth_Used      + "s" // USEDSPACE            //$NON-NLS-1$ //$NON-NLS-2$
                     + "  %"  + maxWidth_NotUsed   + "s" // ESTIMSPACESAVING     //$NON-NLS-1$ //$NON-NLS-2$

                     + "  %"  + maxValueWidth      + "s" // NUMALLOCATEDPAGES    //$NON-NLS-1$ //$NON-NLS-2$
                     + "  %"  + maxValueWidth      + "s" // NUMFREEPAGES         //$NON-NLS-1$ //$NON-NLS-2$
                     + "  %"  + maxValueWidth      + "s" // NUMUNFILLEDPAGES     //$NON-NLS-1$ //$NON-NLS-2$
                     + "  %"  + maxValueWidth      + "s" // PAGESIZE             //$NON-NLS-1$ //$NON-NLS-2$

                     + UI.SPACE                          // empty column

               ;

// SET_FORMATTING_ON

//ISINDEX|CONGLOMERATENAME               |USEDSPACE|ESTIMSPACESAVING|NUMALLOCATEDPAGES|NUMFREEPAGES|NUMUNFILLEDPAGES|PAGESIZE|
//-------|-------------------------------|---------|----------------|-----------------|------------|----------------|--------|
//      0|DBVERSION                      |     4096|               0|                1|           0|               0|    4096|
//      0|TOURBIKE                       |     4096|               0|                1|           0|               0|    4096|
//      0|TOURCOMPARED                   |   491520|               0|              120|           0|               0|    4096|
//      0|TOURDATA                       |667615232|        99418112|            17340|        3034|            2089|   32768|
//      0|TOURDATA_TOURTAG               |   376832|               0|               92|           0|               0|    4096|
//      0|TOURGEOPARTS                   |  7966720|               0|             1945|           0|               1|    4096|
//      0|TOURMARKER                     |  3649536|               0|              891|           0|               1|    4096|
//      0|TOURPERSON                     |    16384|               0|                4|           0|               0|    4096|
//      0|TOURPERSONHRZONE               |    32768|               0|                1|           0|               1|   32768|
//      0|TOURPHOTO                      |  2424832|               0|              592|           0|               0|    4096|
//      0|TOURREFERENCE                  |   102400|           77824|                6|          19|               1|    4096|
//      0|TOURTAG                        |    57344|               0|               14|           0|               0|    4096|
//      0|TOURTAGCATEGORY                |     4096|               0|                1|           0|               1|    4096|
//      0|TOURTAGCATEGORY_TOURTAG        |     4096|               0|                1|           0|               1|    4096|
//      0|TOURTAGCATEGORY_TOURTAGCATEGORY|     4096|               0|                1|           0|               1|    4096|
//      0|TOURTYPE                       |   118784|           40960|               19|          10|               2|    4096|
//      0|TOURWAYPOINT                   |    32768|               0|                1|           0|               1|   32768|

               final StringBuilder sb = new StringBuilder();

               sb.append(NL);
               sb.append(String.format(lineFormat,

                     UI.EMPTY_STRING,
                     Messages.App_Db_CompressTables_HeaderLabel_Used,
                     Messages.App_Db_CompressTables_HeaderLabel_NotUsed,

                     HEADER_ALLOCATED_PAGES_1,
                     HEADER_FREE_PAGES_1,
                     HEADER_UNFILLED_PAGES_1,
                     HEADER_PAGE_SIZE_1

               ));

               sb.append(NL);
               sb.append(String.format(lineFormat,

                     Messages.App_Db_CompressTables_HeaderLabel_Table,
                     UI.UNIT_KBYTE,
                     UI.UNIT_KBYTE,

                     HEADER_ALLOCATED_PAGES_2,
                     HEADER_FREE_PAGES_2,
                     HEADER_UNFILLED_PAGES_2,
                     HEADER_PAGE_SIZE_2

               ));

               sb.append(NL);
               sb.append(NL);

               boolean isIndexTitleDisplayed = false;

               int sumUsedSpaces = 0;
               int sumSpaceSavings = 0;

               for (int rowIndex = 0; rowIndex < allNames.size(); rowIndex++) {

                  final String name = allNames.get(rowIndex);
                  final int usedSpace = allUsedSpaces.get(rowIndex);
                  final int spaceSavings = allSpaceSavings.get(rowIndex);

                  sumUsedSpaces += usedSpace;
                  sumSpaceSavings += spaceSavings;

                  final boolean dbIsIndex = allIsIndex.get(rowIndex) == 1 ? true : false;

                  if (dbIsIndex && isIndexTitleDisplayed == false) {

                     // show index header

                     sb.append(NL);
                     sb.append(NL);
                     sb.append(UI.SPACE + Messages.App_Db_CompressTables_HeaderLabel_Index);
                     sb.append(NL);
                     sb.append(NL);

                     isIndexTitleDisplayed = true;
                  }

                  sb.append(String.format(lineFormat,

                        name,

                        _nf0.format(usedSpace / 1024),
                        _nf0.format(spaceSavings / 1024),

                        _nf0.format(allAllocatedPages.get(rowIndex)),
                        _nf0.format(allFreePages.get(rowIndex)),
                        _nf0.format(allUnfilledPages.get(rowIndex)),
                        _nf0.format(allPageSize.get(rowIndex))

                  ));

                  sb.append(NL);
               }

               /*
                * Show totals
                */
               sb.append(NL);
               sb.append(NL);
               sb.append(String.format(lineFormat,

                     Messages.App_Db_CompressTables_HeaderLabel_Totals,

                     _nf0.format(sumUsedSpaces / 1024),
                     _nf0.format(sumSpaceSavings / 1024),

                     UI.EMPTY_STRING,
                     UI.EMPTY_STRING,
                     UI.EMPTY_STRING,
                     UI.EMPTY_STRING));

               sb.append(NL);

               resultInfo[0] = sb.toString();

            } catch (final Exception e) {

               StatusUtil.log(e);
            }
         }
      });
   }

   private void onCompress_ByCopying() {
      // TODO Auto-generated method stub

   }

   private void onCompress_Inplace() {
      // TODO Auto-generated method stub

   }

}
