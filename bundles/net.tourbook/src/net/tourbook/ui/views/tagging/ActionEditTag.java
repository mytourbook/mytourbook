/*******************************************************************************
 * Copyright (C) 2005, 2019 Wolfgang Schramm and Contributors
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
package net.tourbook.ui.views.tagging;

import java.util.HashMap;

import net.tourbook.Messages;
import net.tourbook.common.util.ITourViewer;
import net.tourbook.data.TourTag;
import net.tourbook.data.TourTagCategory;
import net.tourbook.database.TourDatabase;
import net.tourbook.tag.Dialog_TourTag;
import net.tourbook.tag.Dialog_TourTag_Category;
import net.tourbook.tag.TagMenuManager;
import net.tourbook.tour.TourEventId;
import net.tourbook.tour.TourManager;
import net.tourbook.ui.UI;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.widgets.Display;

/**
 * Action to edit {@link TourTag} or {@link TourTagCategory}
 */
public class ActionEditTag extends Action {

   private ITourViewer _tourViewer;

   public ActionEditTag(final ITourViewer tourViewer) {

      super(Messages.Action_Tag_Edit, AS_PUSH_BUTTON);

      _tourViewer = tourViewer;
   }

   void editTag(final Object viewerCellData) {

      String dlgMessage = UI.EMPTY_STRING;

      final TourTag[] finalTourTag = { null };
      final TourTagCategory[] finalTagCategory = { null };

      /*
       * Open dialog
       */
      if (viewerCellData instanceof TVITagView_Tag) {

         final TVITagView_Tag tourTagItem = ((TVITagView_Tag) viewerCellData);

         final HashMap<Long, TourTag> allTourTags = TourDatabase.getAllTourTags();

         final TourTag tourTag = finalTourTag[0] = allTourTags.get(tourTagItem.getTagId());
         final String tagName = tourTag.getTagName();

         dlgMessage = tagName == null ? UI.EMPTY_STRING : tagName;

         if (new Dialog_TourTag(
               Display.getCurrent().getActiveShell(),
               dlgMessage,
               tourTag).open() != Window.OK) {

            return;
         }

      } else if (viewerCellData instanceof TVITagView_TagCategory) {

         final TVITagView_TagCategory tagCategoryItem = (TVITagView_TagCategory) viewerCellData;

         final HashMap<Long, TourTagCategory> allTourTagCategories = TourDatabase.getAllTourTagCategories();
         final TourTagCategory tagCategory = finalTagCategory[0] = allTourTagCategories.get(tagCategoryItem.getCategoryId());

         final String tagCategoryName = tagCategory.getCategoryName();

         dlgMessage = tagCategoryName == null ? UI.EMPTY_STRING : tagCategoryName;

         if (new Dialog_TourTag_Category(
               Display.getCurrent().getActiveShell(),
               dlgMessage,
               tagCategory).open() != Window.OK) {

            return;
         }

      } else {

         return;
      }

      /*
       * Update UI/model
       */
      BusyIndicator.showWhile(Display.getCurrent(), () -> {

         final ColumnViewer tagViewer = _tourViewer.getViewer();

         if (viewerCellData instanceof TVITagView_Tag) {

            // update model
            final TourTag tourTag = finalTourTag[0];
            TourDatabase.saveEntity(tourTag, tourTag.getTagId(), TourTag.class);

            // update UI
            final TVITagView_Tag tourTagItem = ((TVITagView_Tag) viewerCellData);
            tagViewer.update(tourTagItem, null);

         } else if (viewerCellData instanceof TVITagView_TagCategory) {

            // update model
            final TourTagCategory tagCategory = finalTagCategory[0];
            TourDatabase.saveEntity(tagCategory, tagCategory.getCategoryId(), TourTagCategory.class);

            // update UI
            final TVITagView_TagCategory tagCategory_Item = ((TVITagView_TagCategory) viewerCellData);
            tagViewer.update(tagCategory_Item, null);
         }

         // remove old tags from internal list
         TourDatabase.clearTourTags();
         TagMenuManager.updateRecentTagNames();

         TourManager.getInstance().clearTourDataCache();

         // fire modify event
         TourManager.fireEvent(TourEventId.TAG_STRUCTURE_CHANGED);
      });
   }

   /**
    * Edit selected tag/category
    */
   @Override
   public void run() {

      final ColumnViewer tagViewer = _tourViewer.getViewer();

      final Object firstElement = tagViewer.getStructuredSelection().getFirstElement();

      editTag(firstElement);
   }
}
