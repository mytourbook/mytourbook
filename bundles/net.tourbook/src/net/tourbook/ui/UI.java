/*******************************************************************************
 * Copyright (C) 2005, 2025 Wolfgang Schramm and Contributors
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
package net.tourbook.ui;

import static net.tourbook.common.UI.EMPTY_STRING;

import de.byteholder.geoclipse.preferences.IMappingPreferences;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Formatter;

import net.tourbook.Images;
import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.chart.Chart;
import net.tourbook.common.color.MapGraphId;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.common.util.Util;
import net.tourbook.data.TourData;
import net.tourbook.data.TourType;
import net.tourbook.database.TourDatabase;
import net.tourbook.photo.IPhotoPreferences;
import net.tourbook.photo.PhotoActivator;
import net.tourbook.photo.PhotoImages;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.preferences.PrefPageViewColors;
import net.tourbook.tour.SelectionTourId;
import net.tourbook.tour.SelectionTourIds;
import net.tourbook.tour.TourEvent;
import net.tourbook.tour.TourManager;
import net.tourbook.tour.photo.TourPhotoLinkView;
import net.tourbook.tourType.TourTypeImage;
import net.tourbook.ui.views.tourDataEditor.TourDataEditorView;
import net.tourbook.web.WEB;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.jface.resource.ColorRegistry;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.ColumnPixelData;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.StyledString.Styler;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.menus.UIElement;

public class UI {

   private static final String           ICONS_PATH                      = "/icons/";                         //$NON-NLS-1$

   public static final String            IS_NOT_INITIALIZED              = "IS NOT INITIALIZED";              //$NON-NLS-1$

   public static final String            GRAPH_ALTIMETER                 = "GRAPH_ALTIMETER";                 //$NON-NLS-1$
   public static final String            GRAPH_ALTITUDE                  = "GRAPH_ALTITUDE";                  //$NON-NLS-1$
   public static final String            GRAPH_CADENCE                   = "GRAPH_CADENCE";                   //$NON-NLS-1$
   public static final String            GRAPH_GRADIENT                  = "GRAPH_GRADIENT";                  //$NON-NLS-1$
   public static final String            GRAPH_PACE                      = "GRAPH_PACE";                      //$NON-NLS-1$
   public static final String            GRAPH_POWER                     = "GRAPH_POWER";                     //$NON-NLS-1$
   public static final String            GRAPH_PULSE                     = "GRAPH_PULSE";                     //$NON-NLS-1$
   public static final String            GRAPH_SPEED                     = "GRAPH_SPEED";                     //$NON-NLS-1$
   public static final String            GRAPH_TEMPERATURE               = "GRAPH_TEMPERATURE";               //$NON-NLS-1$

   /**
    * Content category e.g. tag category
    */
   public static final String            VIEW_COLOR_CONTENT_CATEGORY     = "VIEW_COLOR_CONTENT_CATEGORY";     //$NON-NLS-1$

   /**
    * Cntent subcategory e.g. tag
    */
   public static final String            VIEW_COLOR_CONTENT_SUB_CATEGORY = "VIEW_COLOR_CONTENT_SUB_CATEGORY"; //$NON-NLS-1$

   /**
    * Date category e.g. year
    */
   public static final String            VIEW_COLOR_DATE_CATEGORY        = "VIEW_COLOR_DATE_CATEGORY";        //$NON-NLS-1$

   /**
    * Date subcategory, e.g. month
    */
   public static final String            VIEW_COLOR_DATE_SUB_CATEGORY    = "VIEW_COLOR_DATE_SUB_CATEGORY";    //$NON-NLS-1$

   /**
    * Color for disabled items
    */
   public static final String            VIEW_COLOR_DISABLED             = "VIEW_COLOR_DISABLED";             //$NON-NLS-1$

   /**
    * Color for normal, not categorized tour values
    */
   public static final String            VIEW_COLOR_TOUR                 = "VIEW_COLOR_TOUR";                 //$NON-NLS-1$

   /**
    * Color for totals or number of ...
    */
   public static final String            VIEW_COLOR_TOTAL                = "VIEW_COLOR_TOTAL";                //$NON-NLS-1$

   public static final ImageRegistry     IMAGE_REGISTRY;

   private static final String           PART_NAME_GRAPH_ID              = "graphId-";                        //$NON-NLS-1$
   private static final String           PART_NAME_DISABLED              = "-disabled";                       //$NON-NLS-1$

   public static final String            IMAGE_TOUR_TYPE_FILTER          = "tourType-filter";                 //$NON-NLS-1$
   public static final String            IMAGE_TOUR_TYPE_FILTER_SYSTEM   = "tourType-filter-system";          //$NON-NLS-1$

   private static final IPreferenceStore _prefStore                      = TourbookPlugin.getPrefStore();

   private static StringBuilder          _formatterSB                    = new StringBuilder();
   private static Formatter              _formatter                      = new Formatter(_formatterSB);

   private static DateFormat             _dateFormatterShort;
   private static DateFormat             _timeFormatterShort;

   public static Styler                  CONTENT_SUB_CATEGORY_STYLER;
   public static Styler                  CONTENT_CATEGORY_STYLER;
   public static Styler                  DATE_CATEGORY_STYLER;
   public static Styler                  DISABLED_STYLER;
   public static Styler                  TOUR_STYLER;
   public static Styler                  TOTAL_STYLER;

   private static final String           DEFAULT_MONO_FONT               = "Courier";                         //$NON-NLS-1$

   private static Font                   _fontForLogging;

   static {

      setViewColorsFromState();
      setupFonts();

      /*
       * Load often used images into the image registry
       */
      IMAGE_REGISTRY = TourbookPlugin.getDefault().getImageRegistry();

// SET_FORMATTING_OFF

      /*
       * Chart and map graphs.
       */
      createGraphImageInRegistry(MapGraphId.Altimeter,   Images.Graph_Altimeter);
      createGraphImageInRegistry(MapGraphId.Altitude,    Images.Graph_Elevation);
      createGraphImageInRegistry(MapGraphId.Cadence,     Images.Graph_Cadence);
      createGraphImageInRegistry(MapGraphId.Gradient,    Images.Graph_Gradient);
      createGraphImageInRegistry(MapGraphId.HrZone,      Images.PulseZones);
      createGraphImageInRegistry(MapGraphId.Pace,        Images.Graph_Pace);
      createGraphImageInRegistry(MapGraphId.Power,       Images.Graph_Power);
      createGraphImageInRegistry(MapGraphId.Pulse,       Images.Graph_Heartbeat);
      createGraphImageInRegistry(MapGraphId.Speed,       Images.Graph_Speed);
      createGraphImageInRegistry(MapGraphId.Temperature, Images.Graph_Temperature);

      // tour type images
      IMAGE_REGISTRY.put(IMAGE_TOUR_TYPE_FILTER,               TourbookPlugin.getThemedImageDescriptor(Images.TourType_Filter));
      IMAGE_REGISTRY.put(IMAGE_TOUR_TYPE_FILTER_SYSTEM,        TourbookPlugin.getThemedImageDescriptor(Images.TourType_Filter_System));

      // photo
      IMAGE_REGISTRY.put(TourPhotoLinkView.IMAGE_PHOTO_PHOTO,  TourbookPlugin.getImageDescriptor(Images.PhotoPhotos));

      IMAGE_REGISTRY.put(TourPhotoLinkView.IMAGE_PIC_DIR_VIEW, PhotoActivator.getImageDescriptor(PhotoImages.PhotoDirectoryView));

      /*
       * Set stylers for the view colors, the color is retrieved every time from the color registry
       */
      CONTENT_CATEGORY_STYLER       = StyledString.createColorRegistryStyler(VIEW_COLOR_CONTENT_CATEGORY,      null);
      CONTENT_SUB_CATEGORY_STYLER   = StyledString.createColorRegistryStyler(VIEW_COLOR_CONTENT_SUB_CATEGORY,  null);
      DATE_CATEGORY_STYLER          = StyledString.createColorRegistryStyler(VIEW_COLOR_DATE_CATEGORY,         null);
      DISABLED_STYLER               = StyledString.createColorRegistryStyler(VIEW_COLOR_DISABLED,              null);
      TOUR_STYLER                   = StyledString.createColorRegistryStyler(VIEW_COLOR_TOUR,                  null);
      TOTAL_STYLER                  = StyledString.createColorRegistryStyler(VIEW_COLOR_TOTAL,                 null);

// SET_FORMATTING_ON
   }

   private UI() {}

   /**
    * Change the title for the application
    *
    * @param newTitle
    *           new title for the application or <code>null</code> to set the original title
    */
   public static void changeAppTitle(final String newTitle) {

      final Display display = Display.getDefault();

      if (display != null) {

         // Look at all the shells and pick the first one that is a workbench window.
         final Shell[] shells = display.getShells();
         for (final Shell shell : shells) {

            final Object data = shell.getData();

            // Check whether this shell points to the Application main window's shell:
            if (data instanceof IWorkbenchWindow) {

               String title;
               if (newTitle == null) {
                  title = Messages.App_Title;
               } else {
                  title = newTitle;
               }

               shell.setText(title);
               break;
            }
         }
      }
   }

   /**
    * @param file
    *
    * @return Returns <code>true</code> when the file should be overwritten, otherwise
    *         <code>false</code>
    */
   public static boolean confirmOverwrite(final File file) {

      final Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();

      final MessageDialog dialog = new MessageDialog(
            shell,
            Messages.app_dlg_confirmFileOverwrite_title,
            null,
            NLS.bind(Messages.app_dlg_confirmFileOverwrite_message, file.getPath()),
            MessageDialog.QUESTION,
            new String[] { IDialogConstants.YES_LABEL, IDialogConstants.CANCEL_LABEL },
            0);

      dialog.open();

      return dialog.getReturnCode() == 0;
   }

   public static boolean confirmOverwrite(final FileCollisionBehavior fileCollision, final File file) {

      final boolean[] isOverwrite = { false };

      final int fileCollisionValue = fileCollision.value;

      if (fileCollisionValue == FileCollisionBehavior.REPLACE_ALL) {

         // overwrite is already confirmed
         isOverwrite[0] = true;

      } else if (fileCollisionValue == FileCollisionBehavior.ASK
            || fileCollisionValue == FileCollisionBehavior.REPLACE
            || fileCollisionValue == FileCollisionBehavior.KEEP) {

         Display.getDefault().syncExec(() -> {

            final Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
            final MessageDialog dialog = new MessageDialog(
                  shell,
                  Messages.app_dlg_confirmFileOverwrite_title,
                  null,
                  NLS.bind(Messages.app_dlg_confirmFileOverwrite_message, file.getPath()),
                  MessageDialog.QUESTION,
                  new String[] {
                        IDialogConstants.YES_LABEL,
                        IDialogConstants.YES_TO_ALL_LABEL,
                        IDialogConstants.NO_LABEL,
                        IDialogConstants.NO_TO_ALL_LABEL,
                        IDialogConstants.CANCEL_LABEL },
                  0);
            dialog.open();

            final int returnCode = dialog.getReturnCode();
            switch (returnCode) {

            case -1: // dialog was canceled
            case 4:
               fileCollision.value = FileCollisionBehavior.DIALOG_IS_CANCELED;
               break;

            case 0: // YES
               fileCollision.value = FileCollisionBehavior.REPLACE;
               isOverwrite[0] = true;
               break;

            case 1: // YES_TO_ALL
               fileCollision.value = FileCollisionBehavior.REPLACE_ALL;
               isOverwrite[0] = true;
               break;

            case 2: // NO
               fileCollision.value = FileCollisionBehavior.KEEP;
               break;

            case 3: // NO_TO_ALL
               fileCollision.value = FileCollisionBehavior.KEEP_ALL;
               break;

            default:
               break;
            }
         });

      }

      return isOverwrite[0];
   }

   /**
    * Checks if tour id is contained in the property data
    *
    * @param propertyData
    * @param checkedTourId
    *
    * @return Returns the tour id when it is contained in the property data, otherwise it returns
    *         <code>null</code>
    */
   public static Long containsTourId(final Object propertyData, final long checkedTourId) {

      Long containedTourId = null;

      if (propertyData instanceof SelectionTourId) {

         final Long tourId = ((SelectionTourId) propertyData).getTourId();
         if (checkedTourId == tourId) {
            containedTourId = tourId;
         }

      } else if (propertyData instanceof SelectionTourIds) {

         for (final Long tourId : ((SelectionTourIds) propertyData).getTourIds()) {
            if (checkedTourId == tourId) {
               containedTourId = tourId;
               break;
            }
         }

      } else if (propertyData instanceof TourEvent) {

         final TourEvent tourEvent = (TourEvent) propertyData;
         final ArrayList<TourData> modifiedTours = tourEvent.getModifiedTours();

         if (modifiedTours != null) {
            for (final TourData tourData : modifiedTours) {
               if (tourData.getTourId().longValue() == checkedTourId) {
                  containedTourId = checkedTourId;
                  break;
               }
            }
         }
      }

      return containedTourId;
   }

   /**
    * Get text for the OK button.
    *
    * @param tourData
    *
    * @return
    */
   public static String convertOKtoSaveUpdateButton(final TourData tourData) {

      String okText = null;

      final TourDataEditorView tourDataEditor = TourManager.getTourDataEditor();
      if ((tourDataEditor != null) && tourDataEditor.isDirty() && (tourDataEditor.getTourData() == tourData)) {
         okText = Messages.app_action_update;
      } else {
         okText = Messages.App_Action_Save;
      }

      return okText;
   }

   private static String createGraphImage_Name(final MapGraphId graphId) {
      return PART_NAME_GRAPH_ID + graphId.name();
   }

   private static String createGraphImage_NameDisabled(final MapGraphId graphId) {
      return PART_NAME_GRAPH_ID + graphId.name() + PART_NAME_DISABLED;
   }

   private static void createGraphImageInRegistry(final MapGraphId graphId,
                                                  final String graphImageName) {

      // create enabled image
      IMAGE_REGISTRY.put(createGraphImage_Name(graphId),
            TourbookPlugin.getThemedImageDescriptor(graphImageName));
   }

   /**
    * Disables all controls and their children
    */
   public static void disableAllControls(final Composite container) {

      disableAllControlsInternal(container);

      // !!! force controls (text,combo...) to be updated !!!
      container.update();
   }

   /**
    * !!!!!!!!!!!!!!! RECURSIVE !!!!!!!!!!!!!!!!!!
    */
   private static void disableAllControlsInternal(final Composite container) {

      for (final Control child : container.getChildren()) {

         if (child instanceof Composite) {
            disableAllControlsInternal((Composite) child);
         }

         child.setEnabled(false);
      }
   }

   public static String format_yyyymmdd_hhmmss(final TourData tourData) {

      if (tourData == null) {
         return EMPTY_STRING;
      }

      _formatterSB.setLength(0);

      final ZonedDateTime dt = tourData.getTourStartTime();

      return _formatter.format(

            Messages.Format_yyyymmdd_hhmmss,
            dt.getYear(),
            dt.getMonthValue(),
            dt.getDayOfMonth(),
            dt.getHour(),
            dt.getMinute(),
            dt.getSecond())

            .toString();
   }

   public static ColumnPixelData getColumnPixelWidth(final PixelConverter pixelConverter, final int width) {

      return new ColumnPixelData(pixelConverter.convertWidthInCharsToPixels(width), false);
   }

   /******************************************************************************
    * this method is copied from the following source and was adjusted
    *
    * <pre>
    * Product: Compiere ERP &amp; CRM Smart Business Solution                    *
    * Copyright (C) 1999-2006 ComPiere, Inc. All Rights Reserved.                *
    * This program is free software; you can redistribute it and/or modify it    *
    * under the terms version 2 of the GNU General Public License as published   *
    * by the Free Software Foundation. This program is distributed in the hope   *
    * that it will be useful, but WITHOUT ANY WARRANTY; without even the implied *
    * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.           *
    * See the GNU General Public License for more details.                       *
    * You should have received a copy of the GNU General Public License along    *
    * with this program; if not, write to the Free Software Foundation, Inc.,    *
    * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.                     *
    * For the text or an alternative of this public license, you may reach us    *
    * ComPiere, Inc., 2620 Augustine Dr. #245, Santa Clara, CA 95054, USA        *
    * or via info@compiere.org or http://www.compiere.org/license.html           *
    * </pre>
    *
    * @return date formatter with leading zeros for month and day and 4-digit year
    */
   public static DateFormat getFormatterDateShort() {

      if (_dateFormatterShort == null) {

         final DateFormat dateInstance = DateFormat.getDateInstance(DateFormat.SHORT);
         if (dateInstance instanceof SimpleDateFormat) {

            final SimpleDateFormat sdf = (SimpleDateFormat) (_dateFormatterShort = dateInstance);

            String oldPattern = sdf.toPattern();

            // some short formats have only one M and d (e.g. ths US)
            if (oldPattern.indexOf("MM") == -1 && oldPattern.indexOf("dd") == -1) {//$NON-NLS-1$ //$NON-NLS-2$
               String newPattern = EMPTY_STRING;
               for (int i = 0; i < oldPattern.length(); i++) {
                  if (oldPattern.charAt(i) == 'M') {
                     newPattern += "MM"; //$NON-NLS-1$
                  } else if (oldPattern.charAt(i) == 'd') {
                     newPattern += "dd"; //$NON-NLS-1$
                  } else {
                     newPattern += oldPattern.charAt(i);
                  }
               }
               sdf.applyPattern(newPattern);
            }

            // Unknown short format => use JDBC
            if (sdf.toPattern().length() != 8) {
               sdf.applyPattern("yyyy-MM-dd"); //$NON-NLS-1$
            }

            // 4 digit year
            if (sdf.toPattern().indexOf("yyyy") == -1) { //$NON-NLS-1$
               oldPattern = sdf.toPattern();
               String newPattern = EMPTY_STRING;
               for (int i = 0; i < oldPattern.length(); i++) {
                  if (oldPattern.charAt(i) == 'y') {
                     newPattern += "yy"; //$NON-NLS-1$
                  } else {
                     newPattern += oldPattern.charAt(i);
                  }
               }
               sdf.applyPattern(newPattern);
            }

            sdf.setLenient(true);
         }
      }

      return _dateFormatterShort;
   }

   /******************************************************************************
    * this method is copied from the following source and was adjusted
    *
    * <pre>
    * Product: Compiere ERP &amp; CRM Smart Business Solution                    *
    * Copyright (C) 1999-2006 ComPiere, Inc. All Rights Reserved.                *
    * This program is free software; you can redistribute it and/or modify it    *
    * under the terms version 2 of the GNU General Public License as published   *
    * by the Free Software Foundation. This program is distributed in the hope   *
    * that it will be useful, but WITHOUT ANY WARRANTY; without even the implied *
    * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.           *
    * See the GNU General Public License for more details.                       *
    * You should have received a copy of the GNU General Public License along    *
    * with this program; if not, write to the Free Software Foundation, Inc.,    *
    * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.                     *
    * For the text or an alternative of this public license, you may reach us    *
    * ComPiere, Inc., 2620 Augustine Dr. #245, Santa Clara, CA 95054, USA        *
    * or via info@compiere.org or http://www.compiere.org/license.html           *
    * </pre>
    *
    * @return date formatter with leading zeros for month and day and 4-digit year
    */
   public static DateFormat getFormatterTimeShort() {

      if (_timeFormatterShort == null) {

         final DateFormat timeInstance = DateFormat.getTimeInstance(DateFormat.SHORT);
         if (timeInstance instanceof SimpleDateFormat) {

            final SimpleDateFormat sdf = (SimpleDateFormat) (_timeFormatterShort = timeInstance);

            final String oldPattern = sdf.toPattern();

            // some short formats have only one h (e.g. ths US)
            if (oldPattern.indexOf("hh") == -1) {//$NON-NLS-1$

               String newPattern = EMPTY_STRING;

               for (int i = 0; i < oldPattern.length(); i++) {
                  if (oldPattern.charAt(i) == 'h') {
                     newPattern += "hh"; //$NON-NLS-1$
                  } else {
                     newPattern += oldPattern.charAt(i);
                  }
               }

               sdf.applyPattern(newPattern);
            }

            sdf.setLenient(true);
         }
      }

      return _timeFormatterShort;
   }

   /**
    * @param graphId
    *
    * @return Returns a graph image, this image <b>MUST</b> not be disposed.
    */
   public static Image getGraphImage(final MapGraphId graphId) {

      return IMAGE_REGISTRY.get(createGraphImage_Name(graphId));
   }

   /**
    * @param graphId
    *
    * @return Returns a graph image, this image <b>MUST</b> not be disposed.
    */
   public static Image getGraphImage_Disabled(final MapGraphId graphId) {

      return IMAGE_REGISTRY.get(createGraphImage_NameDisabled(graphId));
   }

   public static ImageDescriptor getGraphImageDescriptor(final MapGraphId graphId) {

      switch (graphId) {
      case Altitude:
         return TourbookPlugin.getThemedImageDescriptor(Images.Graph_Elevation);

      case Gradient:
         return TourbookPlugin.getThemedImageDescriptor(Images.Graph_Gradient);

      case Pace:
         return TourbookPlugin.getThemedImageDescriptor(Images.Graph_Pace);

      case Pulse:
         return TourbookPlugin.getThemedImageDescriptor(Images.Graph_Heartbeat);

      case Speed:
         return TourbookPlugin.getThemedImageDescriptor(Images.Graph_Speed);

      case HrZone:
         return TourbookPlugin.getThemedImageDescriptor(Images.PulseZones);

      default:
         return TourbookPlugin.getThemedImageDescriptor(Images.Graph_Elevation);
      }
   }

   /**
    * @param imageName
    *
    * @return Returns the url for an icon image in the {@link TourbookPlugin} bundle.
    *
    * @throws IOException
    */
   public static String getIconUrl(final String imageName) {

      try {

         final URL bundleUrl = TourbookPlugin.getDefault().getBundle().getEntry(ICONS_PATH + imageName);
         final URL fileUrl = FileLocator.toFileURL(bundleUrl);

         final String encodedFileUrl = WEB.encodeSpace(fileUrl.toExternalForm());

         return encodedFileUrl;

      } catch (final IOException e) {
         StatusUtil.log(e);
      }

      return EMPTY_STRING;
   }

   public static Font getLogFont() {

      return _fontForLogging;
   }

   /**
    * @param tourTypeId
    *
    * @return Returns the {@link TourType} or <code>null</code>.
    */
   public static TourType getTourType(final long tourTypeId) {

      for (final TourType tourType : TourDatabase.getAllTourTypes()) {
         if (tourType.getTypeId() == tourTypeId) {
            return tourType;
         }
      }

      return null;
   }

   /**
    * @param tourTypeId
    *
    * @return Returns the name of a {@link TourType}.
    */
   public static String getTourTypeLabel(final long tourTypeId) {

      for (final TourType tourType : TourDatabase.getAllTourTypes()) {

         if (tourType.getTypeId() == tourTypeId) {

            String tourTypeText = tourType.getName();

            if (net.tourbook.common.UI.IS_SCRAMBLE_DATA) {

               tourTypeText = net.tourbook.common.UI.scrambleText(tourTypeText);
            }

            return tourTypeText;
         }
      }

      return EMPTY_STRING;
   }

   public static ImageData rotate(final ImageData srcData, final int direction) {

      final int bytesPerPixel = srcData.bytesPerLine / srcData.width;
      final int destBytesPerLine = (direction == SWT.DOWN)
            ? srcData.width * bytesPerPixel
            : srcData.height * bytesPerPixel;

      final byte[] newData = new byte[srcData.data.length];
      int width = 0, height = 0;

      for (int srcY = 0; srcY < srcData.height; srcY++) {
         for (int srcX = 0; srcX < srcData.width; srcX++) {

            int destX = 0, destY = 0, destIndex = 0, srcIndex = 0;

            switch (direction) {
            case SWT.LEFT: // left 90 degrees
               destX = srcY;
               destY = srcData.width - srcX - 1;
               width = srcData.height;
               height = srcData.width;
               break;

            case SWT.RIGHT: // right 90 degrees
               destX = srcData.height - srcY - 1;
               destY = srcX;
               width = srcData.height;
               height = srcData.width;
               break;

            case SWT.DOWN: // 180 degrees
               destX = srcData.width - srcX - 1;
               destY = srcData.height - srcY - 1;
               width = srcData.width;
               height = srcData.height;
               break;
            }

            destIndex = (destY * destBytesPerLine) + (destX * bytesPerPixel);
            srcIndex = (srcY * srcData.bytesPerLine) + (srcX * bytesPerPixel);

            System.arraycopy(srcData.data, srcIndex, newData, destIndex, bytesPerPixel);
         }
      }

      // destBytesPerLine is used as scanlinePad to ensure that no padding is required
      return new ImageData(width, height, srcData.depth, srcData.palette, destBytesPerLine, newData);
   }

   /**
    * Set grid layout with no margins for a composite
    *
    * @param composite
    */
   public static void set0GridLayout(final Composite composite) {
      final GridLayout gridLayout = new GridLayout();
      gridLayout.marginHeight = 0;
      gridLayout.marginWidth = 0;
      gridLayout.verticalSpacing = 0;
      composite.setLayout(gridLayout);
   }

   public static void setDefaultColor(final Control control) {
      control.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_WIDGET_FOREGROUND));
      control.setBackground(null);
   }

   public static void setErrorColor(final Text control) {
      control.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
      control.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
   }

   /**
    * Set selection color which is displayed when table item is selected.
    * <p>
    * The code is from here
    * https://www.eclipse.org/articles/article.php?file=Article-CustomDrawingTableAndTreeItems/index.html
    * <p>
    */
   public static void setTableSelectionColor(final Table table) {

      final ColorRegistry colorRegistry = JFaceResources.getColorRegistry();

      final Color bgSelectedColor = colorRegistry.get(IPhotoPreferences.PHOTO_VIEWER_COLOR_SELECTION_BACKGROUND);

      /*
       * NOTE: EraseItem is called repeatedly. Therefore it is critical
       * for performance that this method be as efficient as possible.
       */
      table.addListener(SWT.EraseItem, event -> {

         event.detail &= ~SWT.HOT;

         if ((event.detail & SWT.SELECTED) == 0) {
            // item is not selected
            return;
         }

         final int clientWidth = table.getClientArea().width;
         final GC gc = event.gc;

         final Color oldForeground = gc.getForeground();
         final Color oldBackground = gc.getBackground();
         {
            gc.setBackground(bgSelectedColor);
            gc.fillRectangle(0, event.y, clientWidth, event.height);
         }
         gc.setForeground(oldForeground);
         gc.setBackground(oldBackground);

         event.detail &= ~SWT.SELECTED;
      });
   }

   /**
    * Set the themed image descriptor for a {@link UIElement} with images from the
    * {@link TourbookPlugin} plugin
    *
    * @param uiElement
    * @param icon
    */
   public static void setThemedIcon(final UIElement uiElement, final String icon) {

      uiElement.setIcon(TourbookPlugin.getThemedImageDescriptor(icon));
   }

   private static void setupFonts() {

      final Display display = Display.getCurrent();
      Assert.isNotNull(display);

      // hookup dispose
      display.disposeExec(() -> {
         if (_fontForLogging != null) {
            _fontForLogging.dispose();
         }
      });

      setupFonts_Logging(display);

      // update font after it changed
      _prefStore.addPropertyChangeListener(propertyChangeEvent -> {

         final String property = propertyChangeEvent.getProperty();

         if (property.equals(IMappingPreferences.THEME_FONT_LOGGING)) {

            if (_fontForLogging != null) {

               /**
                * Delay old font disposal because org.eclipse.swt.custom.StyledTextRenderer is
                * using the old font in setFont(...) before the new font is initialized
                * -> really bad behavior !!!
                */
               final Font oldFont = _fontForLogging;

               display.timerExec(10_000, () -> oldFont.dispose());

               _fontForLogging = null;
            }

            setupFonts_Logging(display);

            // fire event after the font is recreated to update the UI
            _prefStore.setValue(ITourbookPreferences.FONT_LOGGING_IS_MODIFIED, Math.random());
         }
      });

   }

   private static void setupFonts_Logging(final Display display) {

      final String prefFontData = _prefStore.getString(IMappingPreferences.THEME_FONT_LOGGING);
      if (prefFontData.length() > 0) {
         try {

            String prefFontDataCleaned = prefFontData;

            if (prefFontData.endsWith(";")) { //$NON-NLS-1$

               // remove ; at the end, after many years, I found that a ; at the end do not set the correct font

               prefFontDataCleaned = prefFontData.substring(0, prefFontData.length() - 1);
            }

            final FontData fontData = new FontData(prefFontDataCleaned);

            _fontForLogging = new Font(display, fontData);

         } catch (final Exception e) {
            // ignore
         }
      }

      if (_fontForLogging == null) {
         _fontForLogging = new Font(display, DEFAULT_MONO_FONT, 9, SWT.NORMAL);
      }
   }

   /**
    * Set tag colors in the JFace color registry from the pref store
    */
   public static void setViewColorsFromState() {

      final ColorRegistry colorRegistry = JFaceResources.getColorRegistry();
      final IDialogSettings state = TourbookPlugin.getState(PrefPageViewColors.ID);

      final boolean isBrightTheme = net.tourbook.common.UI.IS_BRIGHT_THEME;

      // content category e.g. tag category
      colorRegistry.put(VIEW_COLOR_CONTENT_CATEGORY,

            isBrightTheme

                  ? Util.getStateRGB(state,
                        PrefPageViewColors.STATE_VIEW_COLOR_CONTENT_CATEGORY_BRIGHT,
                        PrefPageViewColors.STATE_VIEW_COLOR_CONTENT_CATEGORY_DEFAULT_BRIGHT)

                  : Util.getStateRGB(state,
                        PrefPageViewColors.STATE_VIEW_COLOR_CONTENT_CATEGORY_DARK,
                        PrefPageViewColors.STATE_VIEW_COLOR_CONTENT_CATEGORY_DEFAULT_DARK));

      // content subcategory e.g. tag
      colorRegistry.put(VIEW_COLOR_CONTENT_SUB_CATEGORY,

            isBrightTheme

                  ? Util.getStateRGB(state,
                        PrefPageViewColors.STATE_VIEW_COLOR_CONTENT_SUB_CATEGORY_BRIGHT,
                        PrefPageViewColors.STATE_VIEW_COLOR_CONTENT_SUB_CATEGORY_DEFAULT_BRIGHT)

                  : Util.getStateRGB(state,
                        PrefPageViewColors.STATE_VIEW_COLOR_CONTENT_SUB_CATEGORY_DARK,
                        PrefPageViewColors.STATE_VIEW_COLOR_CONTENT_SUB_CATEGORY_DEFAULT_DARK));

      // date category e.g. year
      colorRegistry.put(VIEW_COLOR_DATE_CATEGORY,

            isBrightTheme

                  ? Util.getStateRGB(state,
                        PrefPageViewColors.STATE_VIEW_COLOR_DATE_CATEGORY_BRIGHT,
                        PrefPageViewColors.STATE_VIEW_COLOR_DATE_CATEGORY_DEFAULT_BRIGHT)

                  : Util.getStateRGB(state,
                        PrefPageViewColors.STATE_VIEW_COLOR_DATE_CATEGORY_DARK,
                        PrefPageViewColors.STATE_VIEW_COLOR_DATE_CATEGORY_DEFAULT_DARK));

      // date subcategory, e.g. month
      colorRegistry.put(VIEW_COLOR_DATE_SUB_CATEGORY,

            isBrightTheme

                  ? Util.getStateRGB(state,
                        PrefPageViewColors.STATE_VIEW_COLOR_DATE_SUB_CATEGORY_BRIGHT,
                        PrefPageViewColors.STATE_VIEW_COLOR_DATE_SUB_CATEGORY_DEFAULT_BRIGHT)

                  : Util.getStateRGB(state,
                        PrefPageViewColors.STATE_VIEW_COLOR_DATE_SUB_CATEGORY_DARK,
                        PrefPageViewColors.STATE_VIEW_COLOR_DATE_SUB_CATEGORY_DEFAULT_DARK));

      // tour
      colorRegistry.put(VIEW_COLOR_TOUR,

            isBrightTheme

                  ? Util.getStateRGB(state,
                        PrefPageViewColors.STATE_VIEW_COLOR_TOUR_BRIGHT,
                        PrefPageViewColors.STATE_VIEW_COLOR_TOUR_DEFAULT_BRIGHT)

                  : Util.getStateRGB(state,
                        PrefPageViewColors.STATE_VIEW_COLOR_TOUR_DARK,
                        PrefPageViewColors.STATE_VIEW_COLOR_TOUR_DEFAULT_DARK));

      // total
      colorRegistry.put(VIEW_COLOR_TOTAL,

            isBrightTheme

                  ? Util.getStateRGB(state,
                        PrefPageViewColors.STATE_VIEW_COLOR_TOTAL_BRIGHT,
                        PrefPageViewColors.STATE_VIEW_COLOR_TOTAL_DEFAULT_BRIGHT)

                  : Util.getStateRGB(state,
                        PrefPageViewColors.STATE_VIEW_COLOR_TOTAL_DARK,
                        PrefPageViewColors.STATE_VIEW_COLOR_TOTAL_DEFAULT_DARK));

      // disabled
      colorRegistry.put(VIEW_COLOR_DISABLED,

            isBrightTheme

                  ? Util.getStateRGB(state,
                        PrefPageViewColors.STATE_VIEW_COLOR_DISABLED_BRIGHT,
                        PrefPageViewColors.STATE_VIEW_COLOR_DISABLED_DEFAULT_BRIGHT)

                  : Util.getStateRGB(state,
                        PrefPageViewColors.STATE_VIEW_COLOR_DISABLED_DARK,
                        PrefPageViewColors.STATE_VIEW_COLOR_DISABLED_DEFAULT_DARK));
   }

   public static GridData setWidth(final Control control, final int width) {

      final GridData gd = new GridData();
      gd.widthHint = width;
      control.setLayoutData(gd);

      return gd;
   }

   public static void showMessageInfo(final String title, final String message) {

      Display.getDefault().asyncExec(() -> MessageDialog.openInformation(

            Display.getDefault().getActiveShell(),
            title,
            message));
   }

   public static void showSQLException(final SQLException ex) {

      /**
       * Redirect to common code, once upon when all old showSQLException are using this from the
       * common code, this method should be deleted
       */
      net.tourbook.common.UI.showSQLException(ex);
   }

   /**
    * Update properties for the chart from the pref store
    *
    * @param chart
    * @param gridPrefix
    *           Pref store prefix for grid preferences
    * @param layoutPrefix
    *           Pref store prefix for layout preferences
    */
   public static void updateChartProperties(final Chart chart,
                                            final String gridPrefix,
                                            final String layoutPrefix) {

      if (chart == null) {
         return;
      }

      chart.updateProperties(

            Util.getPrefixPref_Int(_prefStore, gridPrefix, ITourbookPreferences.CHART_GRID_HORIZONTAL_DISTANCE),
            Util.getPrefixPref_Int(_prefStore, gridPrefix, ITourbookPreferences.CHART_GRID_VERTICAL_DISTANCE),

            Util.getPrefixPref_Boolean(_prefStore, gridPrefix, ITourbookPreferences.CHART_GRID_IS_SHOW_HORIZONTAL_GRIDLINES),
            Util.getPrefixPref_Boolean(_prefStore, gridPrefix, ITourbookPreferences.CHART_GRID_IS_SHOW_VERTICAL_GRIDLINES),

            _prefStore.getBoolean(ITourbookPreferences.GRAPH_IS_SEGMENT_ALTERNATE_COLOR),
            PreferenceConverter.getColor(_prefStore, ITourbookPreferences.GRAPH_SEGMENT_ALTERNATE_COLOR),
            PreferenceConverter.getColor(_prefStore, ITourbookPreferences.GRAPH_SEGMENT_ALTERNATE_COLOR_DARK),

            Util.getPrefixPref_Int(_prefStore, layoutPrefix, ITourbookPreferences.CHART_Y_AXIS_WIDTH)

      );
   }

   /**
    * Sets the tour type image and text into a {@link CLabel}
    *
    * @param tourData
    * @param lblTourType
    * @param isTextDisplayed
    */
   public static void updateUI_TourType(final TourData tourData,
                                        final CLabel lblTourType,
                                        final boolean isTextDisplayed) {

      final TourType tourType = tourData.getTourType();

      // tour type
      if (tourType == null) {
         lblTourType.setText(EMPTY_STRING);
         lblTourType.setImage(TourTypeImage.getTourTypeImage(TourDatabase.ENTITY_IS_NOT_SAVED));
      } else {
         lblTourType.setImage(TourTypeImage.getTourTypeImage(tourType.getTypeId()));
         lblTourType.setText(isTextDisplayed ? tourType.getName() : EMPTY_STRING);
      }

      lblTourType.pack(true);
      lblTourType.redraw(); // display changed tour image
   }
}
