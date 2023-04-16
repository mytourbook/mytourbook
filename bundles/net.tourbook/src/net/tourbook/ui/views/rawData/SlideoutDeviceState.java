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
import net.tourbook.web.WEB;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.ToolItem;

/**
 * Slideout for map models
 */
public class SlideoutDeviceState extends AdvancedSlideout {

   private RawDataView    _rawDataView;
   private ToolItem       _toolItem;

   private PixelConverter _pc;

   /*
    * UI controls
    */
   private Composite _parent;

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

      setTitleText(rawDataView.getEasyConfig().getActiveImportConfig().name);

      // prevent that the opened slideout is partly hidden
      setIsForceBoundsToBeInsideOfViewport(true);
   }

   @Override
   protected void createSlideoutContent(final Composite parent) {

      _parent = parent;

      initUI(parent);

      createUI(parent);

      restoreState();
   }

   private void createUI(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, true).applyTo(container);
      GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
//      container.setBackground(UI.SYS_COLOR_GREEN);
      {
         createUI_10_DeviceFolder(container);
      }
   }

   private void createUI_10_DeviceFolder(final Composite parent) {

      final EasyConfig easyConfig = _rawDataView.getEasyConfig();

      final ImportConfig importConfig = easyConfig.getActiveImportConfig();

      final String deviceOSFolder = importConfig.getDeviceOSFolder();
      final ArrayList<OSFile> notImportedFiles = easyConfig.notImportedFiles;

      final int numDeviceFiles = easyConfig.numDeviceFiles;
      final int numMovedFiles = easyConfig.movedFiles.size();
      final int numNotImportedFiles = notImportedFiles.size();
      final int numAllFiles = numDeviceFiles + numMovedFiles;

      final boolean isDeviceFolderOK = _rawDataView.isOSFolderValid(deviceOSFolder);
      boolean isFolderOK = true;

      {
         /*
          * Backup folder
          */
         final boolean isCreateBackup = importConfig.isCreateBackup;
         if (isCreateBackup) {

            // check OS folder
            final String backupOSFolder = importConfig.getBackupOSFolder();
            final boolean isBackupFolderOK = _rawDataView.isOSFolderValid(backupOSFolder);
            isFolderOK &= isBackupFolderOK;

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

                  importConfig.getBackupFolder(), //           folderLocation
                  isBackupFolderOK, //                         isOSFolderValid
                  false, //                                    isTopMargin
                  Messages.Import_Data_HTML_Title_Backup, //   folderTitle
                  folderInfo //                                folderInfo
            );

         }
      }

//      /*
//       * Device folder
//       */
//      {
//         final String htmlDeviceFolder = UI.replaceHTML_BackSlash(importConfig.getDeviceFolder());
//
//         final boolean isTopMargin = importConfig.isCreateBackup;
//
//         final String folderTitle = Messages.Import_Data_HTML_Title_Device;
//         final String folderInfo = numNotImportedFiles == 0
//               ? NLS.bind(Messages.Import_Data_HTML_AllFilesAreImported, numAllFiles)
//               : NLS.bind(Messages.Import_Data_HTML_NotImportedFiles, numNotImportedFiles, numAllFiles);
//
//         xcreateHTML_56_FolderState(
//               sb,
//               htmlDeviceFolder,
//               isDeviceFolderOK,
//               isTopMargin,
//               folderTitle,
//               folderInfo);
//
//         isFolderOK &= isDeviceFolderOK;
//      }
//
//      /*
//       * Moved files
//       */
//      if (numMovedFiles > 0) {
//
//         sb.append(HTML_TR);
//
//         sb.append(HTML_TD_SPACE + HTML_STYLE_TITLE_VERTICAL_PADDING + " class='folderTitle'>"); //$NON-NLS-1$
//         sb.append(Messages.Import_Data_HTML_Title_Moved);
//         sb.append(HTML_TD_END);
//
//         sb.append(HTML_TD_SPACE + HTML_STYLE_TITLE_VERTICAL_PADDING + " class='folderLocation'>"); //$NON-NLS-1$
//         sb.append(NLS.bind(Messages.Import_Data_HTML_MovedFiles, numMovedFiles));
//         sb.append(HTML_TD_END);
//
//         sb.append(HTML_TR_END);
//      }
//
//      /*
//       * Device files
//       */
//      {
//         final String deviceFiles = importConfig.fileGlobPattern.trim().length() == 0
//               ? ImportConfig.DEVICE_FILES_DEFAULT
//               : importConfig.fileGlobPattern;
//
//         sb.append(HTML_TR);
//
//         sb.append(HTML_TD_SPACE + HTML_STYLE_TITLE_VERTICAL_PADDING + " class='folderTitle'>"); //$NON-NLS-1$
//         sb.append(Messages.Import_Data_HTML_Title_Files);
//         sb.append(HTML_TD_END);
//
//         sb.append(HTML_TD_SPACE + HTML_STYLE_TITLE_VERTICAL_PADDING + " class='folderLocation'>"); //$NON-NLS-1$
//         sb.append(deviceFiles);
//         sb.append(HTML_TD_END);
//
//         sb.append(HTML_TR_END);
//      }
//
//      /*
//       * 100. Delete device files
//       */
//      {
//         final boolean isDeleteDeviceFiles = importConfig.isDeleteDeviceFiles;
//
//         final String deleteFiles = isDeleteDeviceFiles
//               ? Messages.Import_Data_HTML_DeleteFilesYES
//               : Messages.Import_Data_HTML_DeleteFilesNO;
//
//         sb.append(HTML_TR);
//
//         sb.append(HTML_TD_SPACE + HTML_STYLE_TITLE_VERTICAL_PADDING + " class='folderTitle'>"); //$NON-NLS-1$
//         sb.append(Messages.Import_Data_HTML_Title_Delete);
//         sb.append(HTML_TD_END);
//
//         sb.append(HTML_TD_SPACE + HTML_STYLE_TITLE_VERTICAL_PADDING + " class='folderLocation'>"); //$NON-NLS-1$
//         sb.append(deleteFiles);
//         sb.append(HTML_TD_END);
//
//         sb.append(HTML_TR_END);
//      }
//
//      /*
//       * 101. Turn off device watching
//       */
//      {
//         final boolean isWatchingOff = importConfig.isTurnOffWatching;
//
//         final String watchingText = isWatchingOff
//               ? Messages.Import_Data_HTML_WatchingOff
//               : Messages.Import_Data_HTML_WatchingOn;
//
//         // show red image when off
//         final String imageUrl = isWatchingOff
//               ? _imageUrl_Device_TurnOff
//               : _imageUrl_Device_TurnOn;
//
//         final String onOffImage = createHTML_BgImage(imageUrl);
//
//         sb.append(HTML_TR);
//
//         sb.append(HTML_TD_SPACE + HTML_STYLE_TITLE_VERTICAL_PADDING + ">"); //$NON-NLS-1$
//         sb.append("   <div class='action-button-25' style='" + onOffImage + "'></div>"); //$NON-NLS-1$ //$NON-NLS-2$
//         sb.append(HTML_TD_END);
//         sb.append("<td class='folderInfo' " + HTML_STYLE_TITLE_VERTICAL_PADDING + ">" + watchingText + HTML_TD_END); //$NON-NLS-1$ //$NON-NLS-2$
//
//         sb.append(HTML_TR_END);
//      }
//
//      sb.append("</tbody></table>"); //$NON-NLS-1$
//
//      /*
//       * Import files
//       */
//      if (numNotImportedFiles > 0) {
//         xcreateHTML_58_NotImportedFiles(sb, notImportedFiles);
//      }
   }

   /**
    * @param parent
    * @param folderLocation
    * @param isOSFolderValid
    * @param isTopMargin
    * @param folderTitle
    * @param folderInfo
    */
   private void createUI_12_FolderState(final Composite parent,
                                        final String folderLocation,
                                        final boolean isOSFolderValid,
                                        final boolean isTopMargin,
                                        final String folderTitle,
                                        final String folderInfo) {

      String htmlErrorState;
      String htmlFolderInfo;

      if (isOSFolderValid) {

         htmlErrorState = UI.EMPTY_STRING;
         htmlFolderInfo = folderInfo == null
               ? UI.EMPTY_STRING
               : "<span class='folderInfo'>" + folderInfo + "</span>"; //$NON-NLS-1$ //$NON-NLS-2$

      } else {

         htmlErrorState = "<div class='folderError'>" + Messages.Import_Data_HTML_FolderIsNotAvailable + "</div>"; //$NON-NLS-1$ //$NON-NLS-2$
         htmlFolderInfo = UI.EMPTY_STRING;
      }

      final String paddingTop = isTopMargin
            ? HTML_STYLE_TITLE_VERTICAL_PADDING
            : UI.EMPTY_STRING;

      final String imageUrl = isOSFolderValid
            ? _imageUrl_State_OK
            : _imageUrl_State_Error;

      final String folderStateIcon = "<img src='" //$NON-NLS-1$
            + imageUrl
            + "' style='padding-left:5px; vertical-align:text-bottom;'>"; //$NON-NLS-1$

      UI.createLabel(parent, folderTitle);

      sb.append(HTML_TR);
      sb.append(HTML_TD_SPACE + paddingTop + " class='folderTitle'>" + folderTitle + HTML_TD_END); //$NON-NLS-1$
      sb.append(HTML_TD_SPACE + paddingTop + " class='folderLocation'>" + folderLocation + folderStateIcon); //$NON-NLS-1$
      sb.append(htmlErrorState);
      sb.append(HTML_TD_END);
      sb.append(HTML_TR_END);

      sb.append(HTML_TR);
      sb.append("<td></td>"); //$NON-NLS-1$
      sb.append(HTML_TD + htmlFolderInfo + HTML_TD_END);
      sb.append(HTML_TR_END);

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

   private void xcreateHTML_58_NotImportedFiles(final StringBuilder sb, final ArrayList<OSFile> notImportedFiles) {

      sb.append("<table border=0 class='deviceList'><tbody>"); //$NON-NLS-1$

      final EasyConfig easyConfig = getEasyConfig();

      for (final OSFile deviceFile : notImportedFiles) {

         final String fileMoveState = deviceFile.isBackupImportFile
               ? Messages.Import_Data_HTML_Title_Moved_State
               : UI.EMPTY_STRING;

         String filePathName = UI.replaceHTML_BackSlash(deviceFile.getPath().getParent().toString());
         final ZonedDateTime modifiedTime = TimeTools.getZonedDateTime(deviceFile.modifiedTime);

         // am/pm contains a space which can break the line
         final String nbspTime = modifiedTime.format(TimeTools.Formatter_Time_S).replace(UI.SPACE1, WEB.NONE_BREAKING_SPACE);
         final String nbspFileName = deviceFile.getFileName().replace(UI.SPACE1, WEB.NONE_BREAKING_SPACE);

         sb.append(HTML_TR);

         sb.append("<td width=1 class='column'>"); //$NON-NLS-1$
         sb.append(fileMoveState);
         sb.append(HTML_TD_END);

         sb.append("<td class='column content'>"); //$NON-NLS-1$
         sb.append(nbspFileName);
         sb.append(HTML_TD_END);

         sb.append("<td class='column right'>"); //$NON-NLS-1$
         sb.append(modifiedTime.format(TimeTools.Formatter_Date_S));
         sb.append(HTML_TD_END);

         sb.append("<td class='column right'>"); //$NON-NLS-1$
         sb.append(nbspTime);
         sb.append(HTML_TD_END);

         sb.append("<td class='right'>"); //$NON-NLS-1$
         sb.append(deviceFile.size);
         sb.append(HTML_TD_END);

         // this is for debugging
         if (easyConfig.stateToolTipDisplayAbsoluteFilePath) {

            if (NIO.isTourBookFileSystem(filePathName)) {

               final TourbookFileSystem tourbookFileSystem = FileSystemManager.getTourbookFileSystem(filePathName);

               filePathName = filePathName.replace(tourbookFileSystem.getId(), tourbookFileSystem.getDisplayId());
            }

            final String nbspFilePathName = UI.EMPTY_STRING

                  // add additonal space before the text otherwise it is too narrow to the previous column
                  + WEB.NONE_BREAKING_SPACE
                  + WEB.NONE_BREAKING_SPACE

                  + filePathName.replace(UI.SPACE1, WEB.NONE_BREAKING_SPACE);

            sb.append("<td class='column content'>"); //$NON-NLS-1$
            sb.append(nbspFilePathName);
            sb.append(HTML_TD_END);
         }

         sb.append(HTML_TR_END);
      }

      sb.append("</tbody></table>"); //$NON-NLS-1$
   }

}
