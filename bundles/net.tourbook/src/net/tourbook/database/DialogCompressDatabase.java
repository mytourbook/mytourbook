/*******************************************************************************
 * Copyright (C) 2005, 2022 Wolfgang Schramm and Contributors
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
package net.tourbook.database;

import de.byteholder.geoclipse.preferences.IMappingPreferences;

import java.lang.reflect.InvocationTargetException;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.UI;
import net.tourbook.common.font.MTFont;
import net.tourbook.common.util.SQL;
import net.tourbook.common.util.StatusUtil;

import org.eclipse.collections.impl.list.mutable.primitive.IntArrayList;
import org.eclipse.collections.impl.list.mutable.primitive.LongArrayList;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.window.Window;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
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

public class DialogCompressDatabase extends Dialog {

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

   private static final IDialogSettings _state   = TourbookPlugin.getState("net.tourbook.database.DialogCompressDatabase");//$NON-NLS-1$

   private Shell                        _dialogShell;

   private List<String>                 _allTableNames;
   private List<String>                 _allConglomerateNames;

   private IntArrayList                 _allIndexFlags;
   private LongArrayList                _allUsedSpaces_BeforeCompress;
   private LongArrayList                _allUsedSpaces_AfterCompress;

   private String                       _logText = UI.EMPTY_STRING;

   /*
    * UI controls
    */
   private Text   _txtLog;
   private Font   _monoFont;

   private Button _btnCompressByCopying;
   private Button _btnCompressInplace;

   public DialogCompressDatabase() {

      super(Display.getDefault().getActiveShell());

      // make dialog resizable
      setShellStyle(getShellStyle() | SWT.RESIZE);
   }

   private void appendLogText(final String newText) {

      _logText = _logText + NL + newText;

      _txtLog.setText(_logText);

      // scroll to the bottom
      _txtLog.setTopIndex(_txtLog.getLineCount() - 1);
   }

   @Override
   protected void configureShell(final Shell shell) {

      super.configureShell(shell);

      _dialogShell = shell;

      // set window title
      shell.setText(Messages.App_Db_Compress_DialogTitle);

      shell.addDisposeListener(e -> {

         if (_monoFont != null) {
            _monoFont.dispose();
         }
      });
   }

   @Override
   protected void createButtonsForButtonBar(final Composite parent) {

      {
         /*
          * Button: Compress by copying
          */
         _btnCompressByCopying = createButton(
               parent,
               IDialogConstants.CLIENT_ID + 1,
               Messages.App_Db_Compress_Button_CompressByCopying,
               false);
         _btnCompressByCopying.setToolTipText(Messages.App_Db_Compress_Button_CompressByCopying_Tooltip);

         _btnCompressByCopying.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
               onCompress(false);
            }
         });
      }
      {
         /*
          * Button: Inplace compress
          */
         _btnCompressInplace = createButton(
               parent,
               IDialogConstants.CLIENT_ID + 2,
               Messages.App_Db_Compress_Button_CompressInplace,
               false);
         _btnCompressInplace.setToolTipText(Messages.App_Db_Compress_Button_CompressInplace_Tooltip);

         _btnCompressInplace.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
               onCompress(true);
            }
         });
      }

      // create close button
      createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CLOSE_LABEL, true);
   }

   @Override
   protected Control createDialogArea(final Composite parent) {

      initUI(parent);

      final Control ui = createUI(parent);

      updateUI_ShowInitialDbSize();

      return ui;
   }

   private String createLog_BeforeCompressed(final LongArrayList allUsedSpaces,
                                             final ArrayList<String> allNames,
                                             final LongArrayList allSpaceSavings,
                                             final LongArrayList allAllocatedPages,
                                             final LongArrayList allFreePages,
                                             final LongArrayList allUnfilledPages,
                                             final LongArrayList allPageSize) {
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

         final int maxWidth_Used             = Math.max(maxValueWidth, Messages.App_Db_Compress_LogLabel_Used.length());
         final int maxWidth_NotUsed          = Math.max(maxValueWidth, Messages.App_Db_Compress_LogLabel_NotUsed.length());

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

//   ISINDEX|CONGLOMERATENAME               |USEDSPACE|ESTIMSPACESAVING|NUMALLOCATEDPAGES|NUMFREEPAGES|NUMUNFILLEDPAGES|PAGESIZE|
//   -------|-------------------------------|---------|----------------|-----------------|------------|----------------|--------|
//         0|DBVERSION                      |     4096|               0|                1|           0|               0|    4096|
//         0|TOURBIKE                       |     4096|               0|                1|           0|               0|    4096|
//         0|TOURCOMPARED                   |   491520|               0|              120|           0|               0|    4096|
//         0|TOURDATA                       |667615232|        99418112|            17340|        3034|            2089|   32768|
//         0|TOURDATA_TOURTAG               |   376832|               0|               92|           0|               0|    4096|
//         0|TOURGEOPARTS                   |  7966720|               0|             1945|           0|               1|    4096|
//         0|TOURMARKER                     |  3649536|               0|              891|           0|               1|    4096|
//         0|TOURPERSON                     |    16384|               0|                4|           0|               0|    4096|
//         0|TOURPERSONHRZONE               |    32768|               0|                1|           0|               1|   32768|
//         0|TOURPHOTO                      |  2424832|               0|              592|           0|               0|    4096|
//         0|TOURREFERENCE                  |   102400|           77824|                6|          19|               1|    4096|
//         0|TOURTAG                        |    57344|               0|               14|           0|               0|    4096|
//         0|TOURTAGCATEGORY                |     4096|               0|                1|           0|               1|    4096|
//         0|TOURTAGCATEGORY_TOURTAG        |     4096|               0|                1|           0|               1|    4096|
//         0|TOURTAGCATEGORY_TOURTAGCATEGORY|     4096|               0|                1|           0|               1|    4096|
//         0|TOURTYPE                       |   118784|           40960|               19|          10|               2|    4096|
//         0|TOURWAYPOINT                   |    32768|               0|                1|           0|               1|   32768|

      final StringBuilder sb = new StringBuilder();

      sb.append(NL);
      sb.append(String.format(lineFormat,

            UI.EMPTY_STRING,
            Messages.App_Db_Compress_LogLabel_Used,
            Messages.App_Db_Compress_LogLabel_NotUsed,

            HEADER_ALLOCATED_PAGES_1,
            HEADER_FREE_PAGES_1,
            HEADER_UNFILLED_PAGES_1,
            HEADER_PAGE_SIZE_1

      ));

      sb.append(NL);
      sb.append(String.format(lineFormat,

            Messages.App_Db_Compress_LogLabel_Table,
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
         final long usedSpace = allUsedSpaces.get(rowIndex);
         final long spaceSavings = allSpaceSavings.get(rowIndex);

         sumUsedSpaces += usedSpace;
         sumSpaceSavings += spaceSavings;

         final boolean dbIsIndex = _allIndexFlags.get(rowIndex) == 1 ? true : false;

         if (dbIsIndex && isIndexTitleDisplayed == false) {

            // show index header

            sb.append(NL);
            sb.append(NL);
            sb.append(UI.SPACE + Messages.App_Db_Compress_LogLabel_Index);
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

            Messages.App_Db_Compress_LogLabel_Totals,

            _nf0.format(sumUsedSpaces / 1024),
            _nf0.format(sumSpaceSavings / 1024),

            UI.EMPTY_STRING,
            UI.EMPTY_STRING,
            UI.EMPTY_STRING,
            UI.EMPTY_STRING));

      sb.append(NL);

      return sb.toString();
   }

   private String createLog_DiffSize() {

      // get width of the largest table/index name
      int maxWidth_Name = 0;
      for (final String name : _allConglomerateNames) {
         if (name.length() > maxWidth_Name) {
            maxWidth_Name = name.length();
         }
      }

// SET_FORMATTING_OFF

         int maxValueWidth = 11; // 123'456'789 kByte
         maxValueWidth = Math.max(maxValueWidth, UI.UNIT_KBYTE.length());

         final int maxWidth_Used             = Math.max(maxValueWidth, Messages.App_Db_Compress_LogLabel_Before.length());
         final int maxWidth_NotUsed          = Math.max(maxValueWidth, Messages.App_Db_Compress_LogLabel_After.length());
         final int maxWidth_Difference       = Math.max(maxValueWidth, Messages.App_Db_Compress_LogLabel_Difference.length());

         final String lineFormat_Header = UI.EMPTY_STRING

               + UI.SPACE                          // empty column

               + "%-"   + maxWidth_Name         + "s" // CONGLOMERATENAME     //$NON-NLS-1$ //$NON-NLS-2$
               + "  %"  + maxWidth_Used         + "s" // before compress      //$NON-NLS-1$ //$NON-NLS-2$
               + "  %"  + maxWidth_NotUsed      + "s" // after compress       //$NON-NLS-1$ //$NON-NLS-2$
               + "  %"  + maxWidth_Difference   + "s" // difference           //$NON-NLS-1$ //$NON-NLS-2$

               + UI.SPACE                          // empty column

         ;

         final String lineFormat_Value = UI.EMPTY_STRING

               + UI.SPACE                          // empty column

               + "%-"   + maxWidth_Name         + "s" // CONGLOMERATENAME     //$NON-NLS-1$ //$NON-NLS-2$
               + "  %"  + maxWidth_Used         + "s" // before compress      //$NON-NLS-1$ //$NON-NLS-2$
               + "  %"  + maxWidth_NotUsed      + "s" // after compress       //$NON-NLS-1$ //$NON-NLS-2$
               + "  %"  + maxWidth_Difference   + "s" // difference           //$NON-NLS-1$ //$NON-NLS-2$

               + UI.SPACE                          // empty column

               ;

// SET_FORMATTING_ON

      final StringBuilder sb = new StringBuilder();

      // header line 1
      sb.append(NL);
      sb.append(String.format(lineFormat_Header,

            UI.EMPTY_STRING,
            Messages.App_Db_Compress_LogLabel_Before,
            Messages.App_Db_Compress_LogLabel_After,
            Messages.App_Db_Compress_LogLabel_Difference

      ));

      // header line 2
      sb.append(NL);
      sb.append(String.format(lineFormat_Header,

            Messages.App_Db_Compress_LogLabel_Table,
            UI.UNIT_KBYTE,
            UI.UNIT_KBYTE,
            UI.UNIT_KBYTE

      ));

      sb.append(NL);
      sb.append(NL);

      boolean isIndexTitleDisplayed = false;

      int sumBefore = 0;
      int sumAfter = 0;

      for (int rowIndex = 0; rowIndex < _allConglomerateNames.size(); rowIndex++) {

         final String name = _allConglomerateNames.get(rowIndex);
         final long usedSpace_BeforeCompress = _allUsedSpaces_BeforeCompress.get(rowIndex);
         final long usedSpace_AfterCompress = _allUsedSpaces_AfterCompress.get(rowIndex);

         sumBefore += usedSpace_BeforeCompress;
         sumAfter += usedSpace_AfterCompress;

         final boolean dbIsIndex = _allIndexFlags.get(rowIndex) == 1 ? true : false;

         if (dbIsIndex && isIndexTitleDisplayed == false) {

            // show index header

            sb.append(NL);
            sb.append(NL);
            sb.append(UI.SPACE + Messages.App_Db_Compress_LogLabel_Index);
            sb.append(NL);
            sb.append(NL);

            isIndexTitleDisplayed = true;
         }

         sb.append(String.format(lineFormat_Value,

               name,

               _nf0.format(usedSpace_BeforeCompress / 1024),
               _nf0.format(usedSpace_AfterCompress / 1024),
               _nf0.format((usedSpace_AfterCompress - usedSpace_BeforeCompress) / 1024)

         ));

         sb.append(NL);
      }

      /*
       * Show totals
       */
      sb.append(NL);
      sb.append(NL);
      sb.append(String.format(lineFormat_Value,

            Messages.App_Db_Compress_LogLabel_Totals,

            _nf0.format(sumBefore / 1024),
            _nf0.format(sumAfter / 1024),
            _nf0.format((sumAfter - sumBefore) / 1024)

      ));

      sb.append(NL);

      return sb.toString();
   }

   private Control createUI(final Composite parent) {

      _txtLog = new Text(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
      _txtLog.setFont(_monoFont);
      GridDataFactory.fillDefaults().grab(true, true).applyTo(_txtLog);

      return _txtLog;
   }

   private String getDatabaseSize(final List<String> allConglomerateNames,
                                  final List<String> allTableNames,
                                  final LongArrayList allUsedSpaces) {

      final String[] returnData = new String[1];

      final boolean[] isSetIndexFlag = { false };

      if (_allIndexFlags == null) {
         _allIndexFlags = new IntArrayList();
         isSetIndexFlag[0] = true;
      }

      BusyIndicator.showWhile(Display.getCurrent(), new Runnable() {
         @Override
         public void run() {

            final String sql = UI.EMPTY_STRING

                  + " SELECT" + NL //                                                              //$NON-NLS-1$
                  + "   T2.CONGLOMERATENAME," + NL //                                           1  //$NON-NLS-1$
                  + "   ((NUMALLOCATEDPAGES + NUMFREEPAGES) * PAGESIZE) as USEDSPACE," + NL //  2  //$NON-NLS-1$
                  + "   T2.ESTIMSPACESAVING," + NL //                                           3  //$NON-NLS-1$
                  + "   T2.ISINDEX," + NL //                                                    4  //$NON-NLS-1$
                  + "   T2.NUMALLOCATEDPAGES," + NL //                                          5  //$NON-NLS-1$
                  + "   T2.NUMFREEPAGES," + NL //                                               6  //$NON-NLS-1$
                  + "   T2.NUMUNFILLEDPAGES," + NL //                                           7  //$NON-NLS-1$
                  + "   T2.PAGESIZE" + NL //                                                    8  //$NON-NLS-1$

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

               final LongArrayList allSpaceSavings = new LongArrayList();

               final LongArrayList allAllocatedPages = new LongArrayList();
               final LongArrayList allFreePages = new LongArrayList();
               final LongArrayList allUnfilledPages = new LongArrayList();
               final LongArrayList allPageSize = new LongArrayList();

               final ResultSet result = conn.prepareStatement(sql).executeQuery();

               while (result.next()) {

                  final String name = result.getString(1);
                  allNames.add(name);

                  allUsedSpaces.add(result.getLong(2));
                  allSpaceSavings.add(result.getLong(3));
                  final int dbIndexFlag = result.getInt(4);
                  allAllocatedPages.add(result.getLong(5));
                  allFreePages.add(result.getLong(6));
                  allUnfilledPages.add(result.getLong(7));
                  allPageSize.add(result.getLong(8));

                  // keep state if it is a table or index
                  if (isSetIndexFlag[0]) {
                     _allIndexFlags.add(dbIndexFlag);
                  }

                  // keep all names
                  if (allConglomerateNames != null) {
                     allConglomerateNames.add(name);
                  }

                  // keep table names separately
                  if (allTableNames != null && dbIndexFlag != 1) {
                     allTableNames.add(name);
                  }
               }

               returnData[0] = createLog_BeforeCompressed(allUsedSpaces,
                     allNames,
                     allSpaceSavings,
                     allAllocatedPages,
                     allFreePages,
                     allUnfilledPages,
                     allPageSize);

            } catch (final SQLException e) {
               SQL.showException(e);
            }
         }
      });

      return returnData[0];
   }

   @Override
   protected IDialogSettings getDialogBoundsSettings() {

      // keep window size and position
      return _state;
   }

   private void initUI(final Composite parent) {

      final IPreferenceStore prefStore = TourbookPlugin.getPrefStore();

      final String loggingFontPrefs = prefStore.getString(IMappingPreferences.THEME_FONT_LOGGING);
      if (loggingFontPrefs.length() > 0) {
         try {
            _monoFont = new Font(parent.getDisplay(), new FontData(loggingFontPrefs));
         } catch (final Exception e) {
            // ignore
         }
      }

      if (_monoFont == null) {
         _monoFont = new Font(parent.getDisplay(), MTFont.DEFAULT_MONO_FONT, 9, SWT.NORMAL);
      }
   }

   private boolean isConfirmCompress() {

      final MessageDialog messageDialog = new MessageDialog(getShell(),

            Messages.App_Db_Compress_DialogTitle,
            null,

            NLS.bind(Messages.App_Db_Compress_Dialog_ConfirmCompress_Message,
                  TourDatabase.getDatabasePath()),

            MessageDialog.QUESTION,

            // define buttons
            new String[] {
                  Messages.App_Db_Compress_Button_CompressDatabase,
                  IDialogConstants.NO_LABEL },

            // default button index
            1);

      return messageDialog.open() == Window.OK;
   }

   private void onCompress(final boolean isInplace) {

      if (isConfirmCompress() == false) {
         return;
      }

      final int numTables = _allTableNames.size();

      final short paramSequenctial = (short) (isInplace

            // compress inplace
            ? 1

            // compress by copying
            : 0);

      try {

         final IRunnableWithProgress compressRunnable = new IRunnableWithProgress() {
            @Override
            public void run(final IProgressMonitor monitor)
                  throws InvocationTargetException, InterruptedException {

               monitor.beginTask(Messages.App_Db_Compress_Monitor_Task, numTables);

               try (final Connection conn = TourDatabase.getInstance().getConnection()) {

                  /*
                   * Get all table name,schema
                   */
                  final String sql = UI.EMPTY_STRING

                        + "SELECT" + NL //$NON-NLS-1$
                        + " SCHEMANAME," + NL //$NON-NLS-1$
                        + " TABLENAME" + NL //$NON-NLS-1$
                        + " FROM Sys.SysSchemas s, Sys.SysTables t" + NL //$NON-NLS-1$
                        + " WHERE s.SchemaId = t.SchemaId AND t.TableType = 'T'" + NL //$NON-NLS-1$
                        + " ORDER BY TABLENAME"; //$NON-NLS-1$

                  final Statement stmt = conn.createStatement();
                  final ResultSet resultSet = stmt.executeQuery(sql);

                  /*
                   * Compress all tables
                   */
                  final CallableStatement callableStmt = conn.prepareCall("CALL SYSCS_UTIL.SYSCS_COMPRESS_TABLE(?, ?, ?)"); //$NON-NLS-1$

                  int numCompressed = 0;

                  // loop all tables
                  while (resultSet.next()) {

                     final String schema = resultSet.getString(1);
                     final String tableName = resultSet.getString(2);

                     monitor.subTask(NLS.bind(Messages.App_Db_Compress_Monitor_SubTask,
                           new Object[] { ++numCompressed, numTables, tableName }));

                     // compress one table
                     callableStmt.setString(1, schema);
                     callableStmt.setString(2, tableName);
                     callableStmt.setShort(3, paramSequenctial);

                     callableStmt.execute();

                     monitor.worked(1);
                  }
               } catch (final SQLException e) {
                  SQL.showException(e);
               } finally {

                  // show updated used space

                  _dialogShell.getDisplay().asyncExec(() -> {

                     _allUsedSpaces_AfterCompress = new LongArrayList();

                     appendLogText(Messages.App_Db_Compress_LogHeader_After);
                     appendLogText(getDatabaseSize(null, null, _allUsedSpaces_AfterCompress));

                     appendLogText(Messages.App_Db_Compress_LogHeader_Difference);
                     appendLogText(createLog_DiffSize());
                  });
               }
            }
         };

         /*
          * Ensure to run in the app shell that a slideoutshell can get hidden without hiding the
          * progress dialog, complicated !
          */
         new ProgressMonitorDialog(TourbookPlugin.getAppShell()).run(true, false, compressRunnable);

      } catch (final InvocationTargetException | InterruptedException e) {
         StatusUtil.showStatus(e);
      }

      // disable compress buttons that it cannot be run twice -> db sizes would be wrong when running more than once
      _btnCompressByCopying.setEnabled(false);
      _btnCompressInplace.setEnabled(false);
   }

   private void updateUI_ShowInitialDbSize() {

      // setup return values
      _allConglomerateNames = new ArrayList<>();
      _allTableNames = new ArrayList<>();

      _allUsedSpaces_BeforeCompress = new LongArrayList();

      appendLogText(Messages.App_Db_Compress_LogHeader_Before);
      appendLogText(getDatabaseSize(_allConglomerateNames, _allTableNames, _allUsedSpaces_BeforeCompress));

      // MUST be run async otherwise it has the wrong location
      _dialogShell.getDisplay().asyncExec(() -> {

         // resize dialog to show all data
         final Point defaultSize = _dialogShell.computeSize(SWT.DEFAULT, SWT.DEFAULT);
         _dialogShell.setSize(defaultSize);
      });

   }
}
