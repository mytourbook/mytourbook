/*******************************************************************************
 * Copyright (C) 2023 Wolfgang Schramm and Contributors
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
package net.tourbook.ui.views.rawData;

import static net.tourbook.ui.UI.getColumnPixelWidth;

import java.time.ZonedDateTime;
import java.util.ArrayList;

import net.tourbook.Messages;
import net.tourbook.common.FileSystemManager;
import net.tourbook.common.NIO;
import net.tourbook.common.TourbookFileSystem;
import net.tourbook.common.UI;
import net.tourbook.common.time.TimeTools;
import net.tourbook.common.tooltip.AdvancedSlideout;
import net.tourbook.importdata.EasyConfig;
import net.tourbook.importdata.ImportConfig;
import net.tourbook.importdata.OSFile;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.ToolItem;

/**
 * Slideout for map models
 */
public class SlideoutDeviceState extends AdvancedSlideout {

   private static final int HTML_STYLE_TITLE_VERTICAL_PADDING = 10;

   private RawDataView      _rawDataView;
   private ToolItem         _toolItem;

   private PixelConverter   _pc;

   private TableViewer      _osFileViewer;

   /**
    * Sort files by date/time
    */
   private static class MarkerViewerComparator extends ViewerComparator {

      @Override
      public int compare(final Viewer viewer, final Object obj1, final Object obj2) {

         final String fileName1 = ((OSFile) (obj1)).getFileName();
         final String fileName2 = ((OSFile) (obj2)).getFileName();

         return fileName1.compareTo(fileName2);
      }
   }

   private class MarkerViewerContentProvider implements IStructuredContentProvider {

      @Override
      public void dispose() {}

      @Override
      public Object[] getElements(final Object inputElement) {

         final EasyConfig easyConfig = _rawDataView.getEasyConfig();
         final ArrayList<OSFile> notImportedFiles = easyConfig.notImportedFiles;

         return notImportedFiles.toArray();
      }

      @Override
      public void inputChanged(final Viewer viewer, final Object oldInput, final Object newInput) {}
   }

   /**
    * @param ownerControl
    * @param toolBar
    * @param state
    * @param rawDataView
    */
   public SlideoutDeviceState(final ToolItem toolItem,
                              final IDialogSettings state,
                              final RawDataView rawDataView) {

      super(toolItem.getParent(), state, new int[] { 300, 200 });

      _toolItem = toolItem;
      _rawDataView = rawDataView;

      // prevent that the opened slideout is partly hidden
      setIsForceBoundsToBeInsideOfViewport(true);
   }

   @Override
   protected void createSlideoutContent(final Composite parent) {

      initUI(parent);

      createUI(parent);

      restoreState();

      // load viewer content
      if (_osFileViewer != null && _osFileViewer.getTable().isDisposed() == false) {
         _osFileViewer.setInput(this);
      }
   }

   private void createUI(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, true).applyTo(container);
      GridLayoutFactory.fillDefaults().numColumns(2).spacing(20, 5).applyTo(container);
//      container.setBackground(UI.SYS_COLOR_GREEN);
      {
         createUI_10_DeviceFolder(container);
      }
   }

   private void createUI_10_DeviceFolder(final Composite parent) {

      final EasyConfig easyConfig = _rawDataView.getEasyConfig();
      final ImportConfig activeConfig = easyConfig.getActiveImportConfig();

      // set slideout title
      updateTitleText(activeConfig.name);

      final String deviceOSFolder = activeConfig.getDeviceOSFolder();
      final ArrayList<OSFile> notImportedFiles = easyConfig.notImportedFiles;

      final int numDeviceFiles = easyConfig.numDeviceFiles;
      final int numMovedFiles = easyConfig.movedFiles.size();
      final int numNotImportedFiles = notImportedFiles.size();
      final int numAllFiles = numDeviceFiles + numMovedFiles;

      final boolean isDeviceFolderOK = _rawDataView.isOSFolderValid(deviceOSFolder);

      final int paddingTop = HTML_STYLE_TITLE_VERTICAL_PADDING;
      final GridDataFactory gd = GridDataFactory.fillDefaults().indent(0, paddingTop);

      final ImageRegistry images = _rawDataView.getImages();

      {
         /*
          * Backup folder
          */
         final boolean isCreateBackup = activeConfig.isCreateBackup;
         if (isCreateBackup) {

            // check OS folder
            final String backupOSFolder = activeConfig.getBackupOSFolder();
            final boolean isBackupFolderOK = _rawDataView.isOSFolderValid(backupOSFolder);

            String folderInfo = null;

            /*
             * Show back folder info only when device folder is OK because they are related
             * together.
             */
            if (isDeviceFolderOK) {

               final int numNotBackedUpFiles = easyConfig.notBackedUpFiles.size();

               folderInfo = numNotBackedUpFiles == 0
                     ? NLS.bind(Messages.Import_Data_HTML_AllFilesAreBackedUp, numDeviceFiles)
                     : NLS.bind(Messages.Import_Data_HTML_NotBackedUpFiles, numNotBackedUpFiles, numDeviceFiles);

            }

            createUI_12_FolderState(parent,

                  activeConfig.getBackupFolder(), //           folderLocation
                  isBackupFolderOK, //                         isOSFolderValid
                  true, //                                     isTopMargin
                  Messages.Import_Data_HTML_Title_Backup, //   folderTitle
                  folderInfo //                                folderInfo
            );
         }
      }
      {
         /*
          * Device folder
          */
         final boolean isTopMargin = activeConfig.isCreateBackup;

         final String folderInfo = numNotImportedFiles == 0
               ? NLS.bind(Messages.Import_Data_HTML_AllFilesAreImported, numAllFiles)
               : NLS.bind(Messages.Import_Data_HTML_NotImportedFiles, numNotImportedFiles, numAllFiles);

         createUI_12_FolderState(parent,

               activeConfig.getDeviceFolder(),
               isDeviceFolderOK,
               isTopMargin,
               Messages.Import_Data_HTML_Title_Device,
               folderInfo);

      }
      {
         /*
          * Moved files
          */
         if (numMovedFiles > 0) {

            // label
            Label label = UI.createLabel(parent, Messages.Import_Data_HTML_Title_Moved);
            gd.applyTo(label);

            // state
            label = UI.createLabel(parent, NLS.bind(Messages.Import_Data_HTML_MovedFiles, numMovedFiles));
            gd.applyTo(label);
         }
      }
      {
         /*
          * Device files
          */
         final String deviceFiles = activeConfig.fileGlobPattern.trim().length() == 0
               ? ImportConfig.DEVICE_FILES_DEFAULT
               : activeConfig.fileGlobPattern;

         // label
         Label label = UI.createLabel(parent, Messages.Import_Data_HTML_Title_Files);
         gd.applyTo(label);

         // state
         label = UI.createLabel(parent, deviceFiles);
         gd.applyTo(label);
      }
      {
         /*
          * 100. Delete device files
          */
         final boolean isDeleteDeviceFiles = activeConfig.isDeleteDeviceFiles;

         final String deleteFiles = isDeleteDeviceFiles
               ? Messages.Import_Data_HTML_DeleteFilesYES_NoHTML
               : Messages.Import_Data_HTML_DeleteFilesNO;

         // label
         Label label = UI.createLabel(parent, Messages.Import_Data_HTML_Title_Delete);
         gd.applyTo(label);

         // state
         label = UI.createLabel(parent, deleteFiles);
         gd.applyTo(label);
      }
      {
         /*
          * 101. Turn off device watching
          */
         final boolean isWatchingOff = activeConfig.isTurnOffWatching;

         final String watchingText = isWatchingOff
               ? Messages.Import_Data_HTML_WatchingOff
               : Messages.Import_Data_HTML_WatchingOn;

         // show red image when off
         final Image onOffImage = isWatchingOff
               ? images.get(RawDataView.IMAGE_DEVICE_TURN_OFF)
               : images.get(RawDataView.IMAGE_DEVICE_TURN_ON);

         // on/off icon
         final Label labelStateIcon = UI.createLabel(parent, UI.EMPTY_STRING);
         labelStateIcon.setImage(onOffImage);
         gd.applyTo(labelStateIcon);

         // state
         final Label label = UI.createLabel(parent, watchingText);
         gd.align(SWT.BEGINNING, SWT.CENTER).applyTo(label);
      }
      {
         /*
          * Import files
          */
         if (numNotImportedFiles > 0) {
            createUI_14_NotImportedFiles(parent, notImportedFiles);
         }
      }
   }

   /**
    * @param parent
    * @param folderLocation
    * @param isOSFolderValid
    * @param isTopMargin
    * @param folderName
    * @param folderInfo
    */
   private void createUI_12_FolderState(final Composite parent,
                                        final String folderLocation,
                                        final boolean isOSFolderValid,
                                        final boolean isTopMargin,
                                        final String folderName,
                                        final String folderInfo) {

      String folderMessage = null;
      String errorMessage = null;

      final ImageRegistry images = _rawDataView.getImages();
      final Image stateImage;

      if (isOSFolderValid) {

         stateImage = images.get(RawDataView.IMAGE_STATE_OK);

         folderMessage = folderInfo;

      } else {

         stateImage = images.get(RawDataView.IMAGE_STATE_ERROR);

         errorMessage = Messages.Import_Data_HTML_FolderIsNotAvailable;
      }

      final int paddingTop = isTopMargin
            ? HTML_STYLE_TITLE_VERTICAL_PADDING
            : 0;

      final GridDataFactory gd = GridDataFactory.fillDefaults().indent(0, paddingTop);

      /*
       * Folder location
       */
      final Label labelName = UI.createLabel(parent, folderName);
      gd.applyTo(labelName);

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
      GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
      {
         // label: folder location
         final Label labelLocation = UI.createLabel(container, folderLocation);
         gd.applyTo(labelLocation);

         // state icon
         final Label labelStateIcon = UI.createLabel(container, UI.EMPTY_STRING);
         labelStateIcon.setImage(stateImage);
         gd.applyTo(labelStateIcon);
      }

      /*
       * Folder error
       */
      if (errorMessage != null) {

         UI.createSpacer_Horizontal(parent, 1);

         final Label label = UI.createLabel(parent, errorMessage);

         label.getDisplay().asyncExec(() -> {

            final Color errorColor = UI.IS_DARK_THEME
                  ? UI.SYS_COLOR_YELLOW
                  : UI.SYS_COLOR_RED;

            label.setForeground(errorColor);
         });
      }

      /*
       * Folder info
       */
      if (folderMessage != null) {

         UI.createSpacer_Horizontal(parent, 1);

         UI.createLabel(parent, folderMessage);
      }
   }

   private void createUI_14_NotImportedFiles(final Composite parent, final ArrayList<OSFile> notImportedFiles) {

      final Composite layoutContainer = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults()
            .grab(true, true)
            .span(2, 1)
            .indent(0, 10)
            .applyTo(layoutContainer);

      final TableColumnLayout tableLayout = new TableColumnLayout();
      layoutContainer.setLayout(tableLayout);

      /*
       * Create table
       */
      final Table table = new Table(layoutContainer, SWT.FULL_SELECTION);

//      if (UI.IS_BRIGHT_THEME) {
//
//         parent.getDisplay().asyncExec(() -> {
//
//            // !!! background color is reverted with the dark theme when this dialog gains the focus !!!
//
//            table.setBackground(ThemeUtil.getDefaultBackgroundColor_Shell());
//         });
//      }

//      table.addFocusListener(FocusListener.focusGainedAdapter(focusEvent -> {
//
//         // !!! background color is reverted with the dark theme when this dialog gains the focus !!!
//
//         table.setBackground(ThemeUtil.getDefaultBackgroundColor_Shell());
//      }));

      table.setLayout(new TableLayout());

      _osFileViewer = new TableViewer(table);

      /*
       * Create columns
       */
      defineColumn_10_MoveState(tableLayout);
      defineColumn_20_Filename(tableLayout);
      defineColumn_30_Date(tableLayout);
      defineColumn_40_Time(tableLayout);
      defineColumn_50_FileSize(tableLayout);

      if (_rawDataView.getEasyConfig().stateToolTipDisplayAbsoluteFilePath) {

         defineColumn_60_FilePathname(tableLayout);
      }

      /*
       * Create table viewer
       */
      _osFileViewer.setContentProvider(new MarkerViewerContentProvider());
      _osFileViewer.setComparator(new MarkerViewerComparator());
   }

   /**
    * Column: Moving state
    */
   private void defineColumn_10_MoveState(final TableColumnLayout tableLayout) {

      final TableViewerColumn tvc = new TableViewerColumn(_osFileViewer, SWT.CENTER);
      final TableColumn tc = tvc.getColumn();

      tvc.setLabelProvider(new CellLabelProvider() {

         @Override
         public void update(final ViewerCell cell) {

            final OSFile osFile = (OSFile) cell.getElement();

            final String fileMoveState = osFile.isBackupImportFile
                  ? Messages.Import_Data_HTML_Title_Moved_State
                  : UI.EMPTY_STRING;

            cell.setText(fileMoveState);
         }
      });

      tableLayout.setColumnData(tc, getColumnPixelWidth(_pc, 4));
   }

   /**
    * Column: Filename
    */
   private void defineColumn_20_Filename(final TableColumnLayout tableLayout) {

      final TableViewerColumn tvc = new TableViewerColumn(_osFileViewer, SWT.LEFT);
      final TableColumn tc = tvc.getColumn();

      tvc.setLabelProvider(new CellLabelProvider() {

         @Override
         public void update(final ViewerCell cell) {

            final OSFile osFile = (OSFile) cell.getElement();

            cell.setText(osFile.getFileName());
         }
      });

      tableLayout.setColumnData(tc, new ColumnWeightData(1, true));
   }

   /**
    * Column: Date
    */
   private void defineColumn_30_Date(final TableColumnLayout tableLayout) {

      final TableViewerColumn tvc = new TableViewerColumn(_osFileViewer, SWT.RIGHT);
      final TableColumn tc = tvc.getColumn();

      tvc.setLabelProvider(new CellLabelProvider() {

         @Override
         public void update(final ViewerCell cell) {

            final OSFile osFile = (OSFile) cell.getElement();

            final ZonedDateTime modifiedTime = TimeTools.getZonedDateTime(osFile.modifiedTime);

            cell.setText(modifiedTime.format(TimeTools.Formatter_Date_S));
         }
      });

      tableLayout.setColumnData(tc, getColumnPixelWidth(_pc, 12));
   }

   /**
    * Column: Time
    */
   private void defineColumn_40_Time(final TableColumnLayout tableLayout) {

      final TableViewerColumn tvc = new TableViewerColumn(_osFileViewer, SWT.RIGHT);
      final TableColumn tc = tvc.getColumn();

      tvc.setLabelProvider(new CellLabelProvider() {

         @Override
         public void update(final ViewerCell cell) {

            final OSFile osFile = (OSFile) cell.getElement();

            final ZonedDateTime modifiedTime = TimeTools.getZonedDateTime(osFile.modifiedTime);

            cell.setText(modifiedTime.format(TimeTools.Formatter_Time_S));
         }
      });

      tableLayout.setColumnData(tc, getColumnPixelWidth(_pc, 12));
   }

   /**
    * Column: File size
    */
   private void defineColumn_50_FileSize(final TableColumnLayout tableLayout) {

      final TableViewerColumn tvc = new TableViewerColumn(_osFileViewer, SWT.RIGHT);
      final TableColumn tc = tvc.getColumn();

      tvc.setLabelProvider(new CellLabelProvider() {

         @Override
         public void update(final ViewerCell cell) {

            final OSFile osFile = (OSFile) cell.getElement();

            cell.setText(Long.toString(osFile.size));
         }
      });

      tableLayout.setColumnData(tc, getColumnPixelWidth(_pc, 12));
   }

   /**
    * Column: Filepathname
    */
   private void defineColumn_60_FilePathname(final TableColumnLayout tableLayout) {

      final TableViewerColumn tvc = new TableViewerColumn(_osFileViewer, SWT.LEFT);
      final TableColumn tc = tvc.getColumn();

      tvc.setLabelProvider(new CellLabelProvider() {

         @Override
         public void update(final ViewerCell cell) {

            final OSFile osFile = (OSFile) cell.getElement();

            String filePathName = osFile.getPath().getParent().toString();

            if (NIO.isTourBookFileSystem(filePathName)) {

               final TourbookFileSystem tourbookFileSystem = FileSystemManager.getTourbookFileSystem(filePathName);

               filePathName = filePathName.replace(tourbookFileSystem.getId(), tourbookFileSystem.getDisplayId());
            }

            cell.setText(filePathName);
         }
      });

      tableLayout.setColumnData(tc, new ColumnWeightData(1, true));
   }

   private void enableActions() {

   }

   @Override
   protected Rectangle getParentBounds() {

      final Rectangle itemBounds = _toolItem.getBounds();
      final Point itemDisplayPosition = _toolItem.getParent().toDisplay(itemBounds.x, itemBounds.y);

      itemBounds.x = itemDisplayPosition.x;
      itemBounds.y = itemDisplayPosition.y;

      return itemBounds;
   }

   private void initUI(final Composite parent) {

      _pc = new PixelConverter(parent);
   }

   @Override
   protected void onDispose() {

      super.onDispose();
   }

   @Override
   protected void onFocus() {

   }

   private void restoreState() {

      enableActions();
   }

   @Override
   protected void saveState() {

      // save slideout position/size
      super.saveState();
   }

}
