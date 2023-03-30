/*******************************************************************************
 * Copyright (C) 2005, 2023 Wolfgang Schramm and Contributors
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
package net.tourbook.tag;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.UI;
import net.tourbook.common.util.SQL;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.common.util.StringUtils;
import net.tourbook.common.util.Util;
import net.tourbook.data.TourData;
import net.tourbook.data.TourTag;
import net.tourbook.database.TourDatabase;
import net.tourbook.photo.ImageUtils;
import net.tourbook.tag.tour.filter.TourTagFilterManager;
import net.tourbook.tag.tour.filter.TourTagFilterProfile;
import net.tourbook.tour.TourEventId;
import net.tourbook.tour.TourLogManager;
import net.tourbook.tour.TourManager;
import net.tourbook.ui.views.tourDataEditor.TourDataEditorView;

import org.apache.commons.imaging.ImageReadException;
import org.apache.commons.imaging.Imaging;
import org.apache.commons.imaging.common.ImageMetadata;
import org.apache.commons.imaging.formats.jpeg.JpegImageMetadata;
import org.apache.commons.imaging.formats.tiff.TiffField;
import org.apache.commons.imaging.formats.tiff.constants.TiffTagConstants;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.window.Window;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.imgscalr.Scalr.Rotation;

public class TagManager {

   private static final char       NL                  = UI.NEW_LINE;

   protected static final String[] EXPAND_TYPE_NAMES   = {

         Messages.app_action_expand_type_flat,
         Messages.app_action_expand_type_year_day,
         Messages.app_action_expand_type_year_month_day
   };

   protected static final int[]    EXPAND_TYPES        = {

         TourTag.EXPAND_TYPE_FLAT,
         TourTag.EXPAND_TYPE_YEAR_DAY,
         TourTag.EXPAND_TYPE_YEAR_MONTH_DAY
   };

   private static final String     PARAMETER_FIRST     = "?";        //$NON-NLS-1$
   private static final String     PARAMETER_FOLLOWING = ", ?";      //$NON-NLS-1$

   private static TagContentLayout _tagContentLayout;
   private static int              _tagNumContentColumns;
   private static int              _tagImageSize;
   private static int              _tagTextWidth;

   static {

      restoreTagContentValues();
   }

   private static final Map<String, Image>      _tagImagesCache        = new HashMap<>();

   private static final ArrayList<TagUIContent> _allTagUIContainer     = new ArrayList<>();

   public static final TagContentLayoutItem[]   ALL_TAG_CONTENT_LAYOUT = {

         new TagContentLayoutItem(Messages.Tag_ContentLayout_SimpleText, TagContentLayout.SIMPLE_TEXT),
         new TagContentLayoutItem(Messages.Tag_ContentLayout_ImageAndData, TagContentLayout.IMAGE_AND_DATA),
   };

   public static class TagContentLayoutItem {

      public String           label;
      public TagContentLayout tagContentLayout;

      public TagContentLayoutItem(final String label, final TagContentLayout legendUnitLayout) {

         this.label = label;
         this.tagContentLayout = legendUnitLayout;
      }
   }

   private static class TagUIContent {

      Composite container;

      Label     label1;
      Label     label2;
   }

   private static boolean canDeleteTourTagCategory(final long categoryId, final String categoryName) {

      // Category -> Tag
      final String sql_Category_Tags = UI.EMPTY_STRING

            + "SELECT" + NL //                                                               //$NON-NLS-1$
            + " COUNT(TOURTAGCATEGORY_TAGCATEGORYID)" + NL //                                //$NON-NLS-1$
            + " FROM " + TourDatabase.JOINTABLE__TOURTAGCATEGORY_TOURTAG + NL //             //$NON-NLS-1$
            + " WHERE TOURTAGCATEGORY_TAGCATEGORYID = " + categoryId + NL //                 //$NON-NLS-1$
      ;

      // Category -> Category
      final String sql_Category_Categories = UI.EMPTY_STRING

            + "SELECT" + NL //                                                               //$NON-NLS-1$
            + " COUNT(TOURTAGCATEGORY_TAGCATEGORYID1)" + NL //                               //$NON-NLS-1$
            + " FROM " + TourDatabase.JOINTABLE__TOURTAGCATEGORY_TOURTAGCATEGORY + NL //     //$NON-NLS-1$
            + " WHERE TOURTAGCATEGORY_TAGCATEGORYID1 = " + categoryId + NL //                //$NON-NLS-1$
      ;

      try (Connection conn = TourDatabase.getInstance().getConnection()) {

         final long numCategory_Tags = getNumberOfItems(conn, sql_Category_Tags);
         if (numCategory_Tags > 0) {

            MessageDialog.openError(Display.getDefault().getActiveShell(),
                  Messages.Tag_Manager_Dialog_DeleteCategory_Title,
                  NLS.bind(Messages.Tag_Manager_Dialog_DeleteCategory_Tags_Message,
                        new Object[] { categoryName, numCategory_Tags }));

            return false;
         }

         final long numCategory_Categories = getNumberOfItems(conn, sql_Category_Categories);
         if (numCategory_Categories > 0) {

            MessageDialog.openError(Display.getDefault().getActiveShell(),
                  Messages.Tag_Manager_Dialog_DeleteCategory_Title,
                  NLS.bind(Messages.Tag_Manager_Dialog_DeleteCategory_Categories_Message,
                        new Object[] { categoryName, numCategory_Categories }));

            return false;
         }

         return true;

      } catch (final SQLException e) {
         net.tourbook.ui.UI.showSQLException(e);
      }

      return false;
   }

   /**
    * Deletes a tour tag from all contained tours and in the tag structure. This event
    * {@link TourEventId#TAG_STRUCTURE_CHANGED} is fired when done.
    *
    * @param allTags
    * @return Returns <code>true</code> when deletion was successful
    */
   public static boolean deleteTourTag(final List<TourTag> allTags) {

      // ensure that a tour is NOT modified in the tour editor
      if (TourManager.isTourEditorModified(false)) {
         return false;
      }

      String dialogMessage;
      String actionDeleteTags;

      final ArrayList<Long> allTourIds = getTaggedTours(allTags);

      if (allTags.size() == 1) {

         // remove one tag

         dialogMessage = NLS.bind(Messages.Tag_Manager_Dialog_DeleteTag_Message, allTags.get(0).getTagName(), allTourIds.size());
         actionDeleteTags = Messages.Tag_Manager_Action_DeleteTag;

      } else {

         // remove multiple tags

         dialogMessage = NLS.bind(Messages.Tag_Manager_Dialog_DeleteTag_Multiple_Message, allTags.size(), allTourIds.size());
         actionDeleteTags = Messages.Tag_Manager_Action_DeleteTags;
      }

      final Display display = Display.getDefault();

      // confirm deletion, show tag name and number of tours which contain a tag
      final MessageDialog dialog = new MessageDialog(
            display.getActiveShell(),
            Messages.Tag_Manager_Dialog_DeleteTag_Title,
            null,
            dialogMessage,
            MessageDialog.QUESTION,
            new String[] {
                  actionDeleteTags,
                  IDialogConstants.CANCEL_LABEL },
            1);

      final boolean[] returnValue = { false };

      if (dialog.open() == Window.OK) {

         BusyIndicator.showWhile(display, () -> {

            if (deleteTourTag_10(allTags)) {

               fireChangeEvent();

               updateTourTagFilterProfiles(allTags);

               returnValue[0] = true;
            }
         });
      }

      return returnValue[0];
   }

   private static boolean deleteTourTag_10(final List<TourTag> allTags) {

      boolean returnResult = false;

      String sql;

      PreparedStatement prepStmt_TagCategory = null;
      PreparedStatement prepStmt_TourData = null;
      PreparedStatement prepStmt_TourTag = null;

      try (Connection conn = TourDatabase.getInstance().getConnection()) {

         // remove tag from TOURDATA_TOURTAG
         sql = "DELETE" //                                                        //$NON-NLS-1$
               + " FROM " + TourDatabase.JOINTABLE__TOURDATA__TOURTAG //          //$NON-NLS-1$
               + " WHERE " + TourDatabase.KEY_TAG + "=?"; //                      //$NON-NLS-1$ //$NON-NLS-2$
         prepStmt_TourData = conn.prepareStatement(sql);

         // remove tag from TOURTAGCATEGORY_TOURTAG
         sql = "DELETE" //                                                       //$NON-NLS-1$
               + " FROM " + TourDatabase.JOINTABLE__TOURTAGCATEGORY_TOURTAG //   //$NON-NLS-1$
               + " WHERE " + TourDatabase.KEY_TAG + "=?"; //                     //$NON-NLS-1$ //$NON-NLS-2$
         prepStmt_TagCategory = conn.prepareStatement(sql);

         // remove tag from TOURTAG
         sql = "DELETE" //                                                       //$NON-NLS-1$
               + " FROM " + TourDatabase.TABLE_TOUR_TAG //                       //$NON-NLS-1$
               + " WHERE " + TourDatabase.ENTITY_ID_TAG + "=?"; //               //$NON-NLS-1$ //$NON-NLS-2$
         prepStmt_TourTag = conn.prepareStatement(sql);

         int[] returnValue_TourData;
         int[] returnValue_TagCategory;
         int[] returnValue_TourTag;

         conn.setAutoCommit(false);
         {
            for (final TourTag tourTag : allTags) {

               final long tagId = tourTag.getTagId();

               prepStmt_TourData.setLong(1, tagId);
               prepStmt_TourData.addBatch();

               prepStmt_TagCategory.setLong(1, tagId);
               prepStmt_TagCategory.addBatch();

               prepStmt_TourTag.setLong(1, tagId);
               prepStmt_TourTag.addBatch();
            }

            returnValue_TourData = prepStmt_TourData.executeBatch();
            returnValue_TagCategory = prepStmt_TagCategory.executeBatch();
            returnValue_TourTag = prepStmt_TourTag.executeBatch();
         }
         conn.commit();

         // log result
         TourLogManager.showLogView();

         for (int tagIndex = 0; tagIndex < allTags.size(); tagIndex++) {

            TourLogManager.log_INFO(String.format(Messages.Tag_Manager_LogInfo_DeletedTags,
                  returnValue_TourData[tagIndex],
                  returnValue_TagCategory[tagIndex],
                  returnValue_TourTag[tagIndex],
                  allTags.get(tagIndex).getTagName()));
         }

         returnResult = true;

      } catch (final SQLException e) {

         net.tourbook.ui.UI.showSQLException(e);

      } finally {

         Util.closeSql(prepStmt_TourData);
         Util.closeSql(prepStmt_TagCategory);
         Util.closeSql(prepStmt_TourTag);
      }

      return returnResult;
   }

   /**
    * @param categoryId
    * @param categoryName
    * @return Returns <code>true</code> when tag category is deleted.
    */
   public static boolean deleteTourTagCategory(final long categoryId, final String categoryName) {

      // ensure that a tour is NOT modified in the tour editor
      if (TourManager.isTourEditorModified(false)) {
         return false;
      }

      if (canDeleteTourTagCategory(categoryId, categoryName) == false) {
         return false;
      }

      final Display display = Display.getDefault();

      // confirm deletion, show tag name and number of tours which contain a tag
      final MessageDialog dialog = new MessageDialog(
            display.getActiveShell(),
            Messages.Tag_Manager_Dialog_DeleteCategory_Title,
            null,
            NLS.bind(Messages.Tag_Manager_Dialog_DeleteCategory_Message, categoryName),
            MessageDialog.QUESTION,
            new String[] {
                  Messages.Tag_Manager_Action_DeleteCategory,
                  IDialogConstants.CANCEL_LABEL },
            1);

      final boolean[] returnValue = { false };

      if (dialog.open() == Window.OK) {

         BusyIndicator.showWhile(display, () -> {

            if (deleteTourTagCategory_10(categoryId, categoryName)) {

               fireChangeEvent();

               returnValue[0] = true;
            }
         });
      }

      return returnValue[0];
   }

   private static boolean deleteTourTagCategory_10(final long categoryId, final String categoryName) {

      boolean returnResult = false;

      String sql;

      PreparedStatement prepStmt_CategoryCategory = null;
      PreparedStatement prepStmt_TagCategory = null;

      try (Connection conn = TourDatabase.getInstance().getConnection()) {

         // remove category from: TOURTAGCATEGORY_TOURTAGCATEGORY
         sql = "DELETE" + NL //                                                                 //$NON-NLS-1$
               + " FROM " + TourDatabase.JOINTABLE__TOURTAGCATEGORY_TOURTAGCATEGORY + NL //     //$NON-NLS-1$
               + " WHERE TOURTAGCATEGORY_TAGCATEGORYID2 =?"; //                                 //$NON-NLS-1$
         prepStmt_CategoryCategory = conn.prepareStatement(sql);

         // remove category from TOURTAGCATEGORY
         sql = "DELETE" //                                                                      //$NON-NLS-1$
               + " FROM " + TourDatabase.TABLE_TOUR_TAG_CATEGORY //                             //$NON-NLS-1$
               + " WHERE " + TourDatabase.ENTITY_ID_TAG_CATEGORY + "=?"; //                     //$NON-NLS-1$ //$NON-NLS-2$
         prepStmt_TagCategory = conn.prepareStatement(sql);

         int[] returnValue_CategoryCategory;
         int[] returnValue_TagCategory;

         conn.setAutoCommit(false);
         {
            prepStmt_CategoryCategory.setLong(1, categoryId);
            prepStmt_CategoryCategory.addBatch();

            prepStmt_TagCategory.setLong(1, categoryId);
            prepStmt_TagCategory.addBatch();

            returnValue_CategoryCategory = prepStmt_CategoryCategory.executeBatch();
            returnValue_TagCategory = prepStmt_TagCategory.executeBatch();
         }
         conn.commit();

         // log result
         TourLogManager.showLogView();
         TourLogManager.log_INFO(String.format(Messages.Tag_Manager_LogInfo_DeletedTagCategory,
               returnValue_CategoryCategory[0],
               returnValue_TagCategory[0],
               categoryName));

         returnResult = true;

      } catch (final SQLException e) {

         net.tourbook.ui.UI.showSQLException(e);

      } finally {

         Util.closeSql(prepStmt_CategoryCategory);
         Util.closeSql(prepStmt_TagCategory);
      }

      return returnResult;
   }

   /**
    * Dispose images
    */
   public static void disposeTagImages() {

      _tagImagesCache.values().forEach(UI::disposeResource);

      _tagImagesCache.clear();
   }

   public static void disposeTagUIContent() {

      _allTagUIContainer.forEach(tagUIContent -> tagUIContent.container.dispose());

      _allTagUIContainer.clear();
   }

   public static Map<Long, String> fetchTourTagsAccumulatedValues() {

      final String sqlQuery = UI.EMPTY_STRING

            + "SELECT" + NL //                                                               //$NON-NLS-1$

            + "jTdataTtag.TOURTAG_TAGID," + NL //                                      1     //$NON-NLS-1$
            + "SUM(tourData.TOURDISTANCE) AS TOTALDISTANCE," + NL //                   2     //$NON-NLS-1$
            + "SUM(tourData.TOURDEVICETIME_RECORDED) AS TOTALRECORDEDTIME" + NL //     3     //$NON-NLS-1$

            + "FROM " + TourDatabase.JOINTABLE__TOURDATA__TOURTAG + " jTdataTtag" + NL //    //$NON-NLS-1$ //$NON-NLS-2$
            + "INNER JOIN " + TourDatabase.TABLE_TOUR_DATA + NL //                           //$NON-NLS-1$
            + "ON jTdataTtag.TOURDATA_TOURID = tourData.TOURID" + NL //                      //$NON-NLS-1$

            + "GROUP BY jTdataTtag.TOURTAG_TAGID" //                                         //$NON-NLS-1$
      ;

      final Map<Long, String> tourTagsAccumulatedValues = new HashMap<>();

      try (Connection connection = TourDatabase.getInstance().getConnection();
            final PreparedStatement preparedStatement = connection.prepareStatement(sqlQuery)) {

         final ResultSet result = preparedStatement.executeQuery();

         while (result.next()) {

            final long tourTagId = result.getLong(1);
            float usedMiles = result.getLong(2);
            final long usedHours = result.getLong(3);

            final StringBuilder tagAccumulatedValues = new StringBuilder();

            tagAccumulatedValues.append(Math.round(usedHours / 3600f));
            tagAccumulatedValues.append(UI.SPACE + net.tourbook.common.UI.UNIT_LABEL_TIME);

            tagAccumulatedValues.append(NL);

            usedMiles = usedMiles / 1000 / net.tourbook.common.UI.UNIT_VALUE_DISTANCE;
            tagAccumulatedValues.append(Math.round(usedMiles));
            tagAccumulatedValues.append(UI.SPACE + net.tourbook.common.UI.UNIT_LABEL_DISTANCE);

            tourTagsAccumulatedValues.put(tourTagId, tagAccumulatedValues.toString());
         }

      } catch (final SQLException e) {

         SQL.showException(e, sqlQuery);
      }

      return tourTagsAccumulatedValues;
   }

   private static void fireChangeEvent() {

      // remove old tags from cached tours
      TourDatabase.clearTourTags();

      TagMenuManager.clearRecentTags();

      TourManager.getInstance().clearTourDataCache();

      // fire modify event
      TourManager.fireEvent(TourEventId.TAG_STRUCTURE_CHANGED);
   }

   private static long getNumberOfItems(final Connection conn, final String sql) {

      long numItems = 0;

      try (PreparedStatement stmt = conn.prepareStatement(sql)) {

         final ResultSet result = stmt.executeQuery();
         while (result.next()) {
            numItems = result.getLong(1);
            break;
         }

      } catch (final SQLException e) {

         StatusUtil.logError(sql);
         net.tourbook.ui.UI.showSQLException(e);
      }

      return numItems;
   }

   public static int getNumberOfTagContentColumns() {

      return _tagNumContentColumns;
   }

   public static TagContentLayout getTagContentLayout() {

      return _tagContentLayout;
   }

   /**
    * Get all tours for a tag id.
    */
   /**
    * @param allTags
    * @return Returns a list with all tour id's which contain the tour tag.
    */
   private static ArrayList<Long> getTaggedTours(final List<TourTag> allTags) {

      final ArrayList<Long> allTourIds = new ArrayList<>();

      final ArrayList<Long> sqlParameters = new ArrayList<>();
      final StringBuilder sqlParameterPlaceholder = new StringBuilder();

      boolean isFirst = true;

      for (final TourTag tagTag : allTags) {

         if (isFirst) {
            isFirst = false;
            sqlParameterPlaceholder.append(PARAMETER_FIRST);
         } else {
            sqlParameterPlaceholder.append(PARAMETER_FOLLOWING);
         }

         sqlParameters.add(tagTag.getTagId());
      }

      final String sql = UI.EMPTY_STRING

            + "SELECT\n" //                                                                           //$NON-NLS-1$

            + " DISTINCT TourData.tourId\n" //                                                        //$NON-NLS-1$

            + " FROM " + TourDatabase.JOINTABLE__TOURDATA__TOURTAG + " jTdataTtag \n" //              //$NON-NLS-1$ //$NON-NLS-2$

            // get all tours for current tag
            + " LEFT OUTER JOIN " + TourDatabase.TABLE_TOUR_DATA + " TourData" //                     //$NON-NLS-1$ //$NON-NLS-2$
            + " ON jTdataTtag.TourData_tourId = TourData.tourId \n" //                                //$NON-NLS-1$

            + " WHERE jTdataTtag.TourTag_TagId IN (" + sqlParameterPlaceholder.toString() + ")\n" //  //$NON-NLS-1$ //$NON-NLS-2$

            + " ORDER BY tourId\n"; //                                                 //$NON-NLS-1$

      PreparedStatement statement = null;

      try (Connection conn = TourDatabase.getInstance().getConnection()) {

         statement = conn.prepareStatement(sql);

         // fillup parameter
         for (int parameterIndex = 0; parameterIndex < sqlParameters.size(); parameterIndex++) {
            statement.setLong(parameterIndex + 1, sqlParameters.get(parameterIndex));
         }

         final ResultSet result = statement.executeQuery();
         while (result.next()) {
            allTourIds.add(result.getLong(1));
         }

      } catch (final SQLException e) {

         StatusUtil.logError(sql);
         net.tourbook.ui.UI.showSQLException(e);

      } finally {
         Util.closeSql(statement);
      }

      return allTourIds;
   }

   /**
    * For a given image file path, try to retrieve the already created
    * Image resource from the cache.
    * Otherwise, create an image resource, and put it in the cache
    *
    * @param tag
    * @return Return the tag image or <code>null</code> when not available
    */
   public static Image getTagImage(final TourTag tag) {

      final String imageFilePath = tag.getImageFilePath();

      if (StringUtils.isNullOrEmpty(imageFilePath)) {
         return null;
      }

      Image tagImage = _tagImagesCache.get(imageFilePath);

      if (tagImage == null) {

         tagImage = prepareTagImage(imageFilePath);

         if (tagImage != null) {
            _tagImagesCache.put(imageFilePath, tagImage);
         }
      }

      return tagImage;
   }

   public static int getTagImageSize() {

      return _tagImageSize;
   }

   public static Image prepareTagImage(final String imageFilePath) {

      if (StringUtils.isNullOrEmpty(imageFilePath)
            || new File(imageFilePath).exists() == false) {

         return null;
      }

      final Image image = new Image(Display.getDefault(), imageFilePath);

      Rotation rotation = null;
      try {

         final ImageMetadata imageMetadata = Imaging.getMetadata(new File(imageFilePath), null);
         if (imageMetadata instanceof JpegImageMetadata) {

            final JpegImageMetadata jpegMetadata = (JpegImageMetadata) imageMetadata;
            final TiffField field = jpegMetadata.findEXIFValueWithExactMatch(TiffTagConstants.TIFF_TAG_ORIENTATION);

            if (field != null) {

               final int orientation = field.getIntValue();

               if (orientation == 6) {

                  rotation = Rotation.CW_90;

               } else if (orientation == 3) {

                  rotation = Rotation.CW_180;

               } else if (orientation == 8) {

                  rotation = Rotation.CW_270;
               }
            }
         }
      } catch (ImageReadException | IOException e) {
         StatusUtil.log(e);
      }

      final int imageWidth = image.getBounds().width;
      final int imageHeight = image.getBounds().height;

      int newimageWidth = _tagImageSize;
      int newimageHeight = _tagImageSize;

      if (UI.IS_4K_DISPLAY) {

         // increase image size for 4k displays

         newimageWidth *= 2.0f;
         newimageHeight *= 2.0f;
      }

      if (imageWidth > imageHeight) {

         /**
          * math floor or - 0.5f is necessary that the resized image is not smaller than the image
          * canvas which could result in a vertical 1 pixel white line
          * <p>
          * https://github.com/mytourbook/mytourbook/issues/1001
          */
         newimageHeight = (int) Math.floor(newimageWidth * imageHeight / (imageWidth * 1f));

      } else if (imageWidth < imageHeight) {

         newimageWidth = (int) Math.floor(newimageHeight * imageWidth / (imageHeight * 1f));
      }

      final Image resizedImage = ImageUtils.resize(Display.getDefault(),
            image,
            newimageWidth,
            newimageHeight,
            SWT.ON,
            SWT.HIGH,
            rotation);

      net.tourbook.common.UI.disposeResource(image);

      return resizedImage;
   }

   private static void restoreTagContentValues() {

      final IDialogSettings state = TourbookPlugin.getState(TourDataEditorView.ID);

      _tagContentLayout = (TagContentLayout) Util.getStateEnum(state,
            TourDataEditorView.STATE_TAG_CONTENT_LAYOUT,
            TourDataEditorView.STATE_TAG_CONTENT_LAYOUT_DEFAULT);

      _tagTextWidth = Util.getStateInt(state,
            TourDataEditorView.STATE_TAG_TEXT_WIDTH,
            TourDataEditorView.STATE_TAG_TEXT_WIDTH_DEFAULT,
            TourDataEditorView.STATE_TAG_TEXT_WIDTH_MIN,
            TourDataEditorView.STATE_TAG_TEXT_WIDTH_MAX);

      _tagImageSize = Util.getStateInt(state,
            TourDataEditorView.STATE_TAG_IMAGE_SIZE,
            TourDataEditorView.STATE_TAG_IMAGE_SIZE_DEFAULT,
            TourDataEditorView.STATE_TAG_IMAGE_SIZE_MIN,
            TourDataEditorView.STATE_TAG_IMAGE_SIZE_MAX);

      _tagNumContentColumns = Util.getStateInt(state,
            TourDataEditorView.STATE_TAG_NUM_CONTENT_COLUMNS,
            TourDataEditorView.STATE_TAG_NUM_CONTENT_COLUMNS_DEFAULT,
            TourDataEditorView.STATE_TAG_NUM_CONTENT_COLUMNS_MIN,
            TourDataEditorView.STATE_TAG_NUM_CONTENT_COLUMNS_MAX);
   }

   public static void updateTagContent() {

      // get old values
      final TagContentLayout tagContentLayout = _tagContentLayout;
      final int tagTextWidth = _tagTextWidth;
      final int tagImageSize = _tagImageSize;
      final int tagNumContentColumns = _tagNumContentColumns;

      // update values from the state
      restoreTagContentValues();

      // check if values are modified
      if (tagContentLayout == _tagContentLayout
            && tagImageSize == _tagImageSize
            && tagTextWidth == _tagTextWidth
            && tagNumContentColumns == _tagNumContentColumns) {

         // tag content is not modified -> nothing to do

         return;
      }

      // dispose tag content
      disposeTagImages();
      disposeTagUIContent();

      // fire event that the tag content is redisplayed
      TourManager.fireEvent(TourEventId.TAG_CONTENT_CHANGED);
   }

   /**
    * Updates the tag list in each tour tag filter profile as one or several tags were just deleted.
    *
    * @param allDeletedTags
    *           An array containing the Tag Id's of the tour tags just deleted
    */
   private static void updateTourTagFilterProfiles(final List<TourTag> allDeletedTags) {

      final ArrayList<TourTagFilterProfile> allProfiles = TourTagFilterManager.getProfiles();

      for (final TourTagFilterProfile profile : allProfiles) {

         for (final TourTag tourTag : allDeletedTags) {

            if (profile.tagFilterIds.contains(tourTag.getTagId())) {
               profile.tagFilterIds.remove(tourTag.getTagId());
            }
         }
      }
   }

   public static void updateUI_Tags(final TourData tourData, final Label tourTagLabel) {

      updateUI_Tags(tourData, tourTagLabel, false);
   }

   /**
    * @param tourData
    * @param tourTagLabel
    * @param isVertical
    *           When <code>true</code> the tags are displayed as a list, otherwise horizontally
    */
   public static void updateUI_Tags(final TourData tourData, final Label tourTagLabel, final boolean isVertical) {

      // tour tags
      final Set<TourTag> tourTags = tourData.getTourTags();

      if (tourTags == null || tourTags.isEmpty()) {

         tourTagLabel.setText(UI.EMPTY_STRING);

      } else {

         final String tagLabels = TourDatabase.getTagNames(tourTags, isVertical);

         tourTagLabel.setText(tagLabels);
         tourTagLabel.setToolTipText(tagLabels);
      }
   }

   public static void updateUI_TagsWithImage(final PixelConverter pc,
                                             final Set<TourTag> tourTags,
                                             final Composite tagContentContainer) {

      final int numTags = tourTags.size();

      if (numTags == 0) {
         return;
      }

      final TourTag[] allTags = tourTags.toArray(new TourTag[numTags]);

      // sort tags by name
      Arrays.sort(allTags);

      // update number of tag content columns
      ((GridLayout) tagContentContainer.getLayout()).numColumns = _tagNumContentColumns;

      // create missing tag UI container
      updateUI_TagsWithImages_CreateUIContainer(pc, tagContentContainer, numTags);

      /*
       * Check if any tag images are available
       */
      boolean isAnyTagImageAvailable = false;

      for (final TourTag tag : allTags) {

         final Image tagImage = getTagImage(tag);

         if (tagImage != null) {

            isAnyTagImageAvailable = true;

            break;
         }
      }

      /*
       * Fill tag content
       */
      final Map<Long, String> tourTagsAccumulatedValues = fetchTourTagsAccumulatedValues();
      final ArrayList<TagUIContent> notNeededTags = new ArrayList<>();

      final GridDataFactory gd = GridDataFactory.fillDefaults();

      for (int tagIndex = 0; tagIndex < _allTagUIContainer.size(); tagIndex++) {

         final TagUIContent tagUIContent = _allTagUIContainer.get(tagIndex);

         if (tagIndex < numTags) {

            final TourTag tag = allTags[tagIndex];
            final long tagId = tag.getTagId();

            final String tagText = tag.getTagName() + UI.NEW_LINE + tourTagsAccumulatedValues.get(tagId);

            final Label label1 = tagUIContent.label1;
            final Label label2 = tagUIContent.label2;

            if (isAnyTagImageAvailable) {

               // 1st label shows the tag image
               // 2nd label shows the tag text

               final Image tagImage = getTagImage(tag);

               label1.setText(UI.EMPTY_STRING);

               // !!! IMPORTANT: image must be set AFTER the text, otherwise the image is not displayed !!!
               label1.setImage(tagImage);

               label2.setVisible(true);
               label2.setText(tagText);

               gd.grab(false, false).hint(_tagImageSize, SWT.DEFAULT).applyTo(label1);
               gd.grab(true, false).applyTo(label2);

            } else {

               // 1st label shows the tag text
               // 2nd label is hidden

               label1.setText(tagText);
               label1.setImage(null);

               label2.setVisible(false);
               label2.setText(UI.EMPTY_STRING);

               gd.grab(true, false).hint(SWT.DEFAULT, SWT.DEFAULT).applyTo(label1);
               gd.grab(false, false).applyTo(label2);
            }

         } else {

            // there are no more tags -> dispose remaining UI container

            notNeededTags.add(tagUIContent);
         }

//         tagUIContent.container.setBackground(net.tourbook.common.UI.SYS_COLOR_CYAN);
      }

      /*
       * Not used tag UI container must be disposed and removed otherwise they still occupy UI space
       * :-(
       */
      notNeededTags.forEach(tagUIContent -> {
         tagUIContent.container.dispose();
      });

      _allTagUIContainer.removeAll(notNeededTags);
   }

   /**
    * Create missing tag UI container
    *
    * @param pc
    * @param tourTagsContainer
    * @param numTags
    */
   private static void updateUI_TagsWithImages_CreateUIContainer(final PixelConverter pc,
                                                                 final Composite tourTagsContainer,
                                                                 final int numTags) {

      final int numMissingUIContainer = numTags - _allTagUIContainer.size();

      if (numMissingUIContainer > 0) {

         final int tagContentWidth = _tagImageSize + _tagTextWidth;

         final Color backgroundColor = tourTagsContainer.getBackground();

         final GridDataFactory gdContainer = GridDataFactory.fillDefaults().hint(tagContentWidth, SWT.DEFAULT);

         for (int numCreated = 0; numCreated < numMissingUIContainer; numCreated++) {

            Label label1;
            Label label2;

            final TagUIContent tagUIContent = new TagUIContent();

            final Composite container = new Composite(tourTagsContainer, SWT.NONE);
            gdContainer.applyTo(container);
            GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
            {
               label1 = new Label(container, SWT.WRAP);
               GridDataFactory.fillDefaults().hint(_tagImageSize, SWT.DEFAULT).applyTo(label1);

               label2 = new Label(container, SWT.WRAP);
               GridDataFactory.fillDefaults().grab(true, false).applyTo(label2);

               tagUIContent.container = container;
               tagUIContent.label1 = label1;
               tagUIContent.label2 = label2;
            }

            container.setBackground(backgroundColor);
            label1.setBackground(backgroundColor);
            label2.setBackground(backgroundColor);

//            container.setBackground(net.tourbook.common.UI.SYS_COLOR_RED);
//            label1.setBackground(net.tourbook.common.UI.SYS_COLOR_GREEN);
//            label2.setBackground(net.tourbook.common.UI.SYS_COLOR_YELLOW);

            _allTagUIContainer.add(tagUIContent);
         }
      }
   }

}
