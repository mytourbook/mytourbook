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
package net.tourbook.tag;

import java.util.HashMap;

import net.tourbook.Messages;
import net.tourbook.common.util.ITourViewer;
import net.tourbook.data.TourTag;
import net.tourbook.data.TourTagCategory;
import net.tourbook.database.TourDatabase;
import net.tourbook.tour.TourEventId;
import net.tourbook.tour.TourManager;
import net.tourbook.ui.UI;
import net.tourbook.ui.views.tagging.TVITagView_Tag;
import net.tourbook.ui.views.tagging.TVITagView_TagCategory;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.window.Window;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.widgets.Display;

public class ActionRenameTag extends Action {

   private ITourViewer _tourViewer;

   public ActionRenameTag(final ITourViewer tourViewer) {

      super(Messages.action_tag_rename_tag, AS_PUSH_BUTTON);

      _tourViewer = tourViewer;
   }

   /**
    * Rename selected tag/category
    */
   @Override
   public void run() {

      final ColumnViewer tagViewer = _tourViewer.getViewer();

      final Object firstElement = tagViewer.getStructuredSelection().getFirstElement();

      String dlgTitle = UI.EMPTY_STRING;
      String dlgMessage = UI.EMPTY_STRING;

      final TourTag[] finalTourTag = { null };
      final TourTagCategory[] finalTagCategory = { null };

      /*
       * Open dialog
       */
      if (firstElement instanceof TVITagView_Tag) {

         final TVITagView_Tag tourTagItem = ((TVITagView_Tag) firstElement);

         final HashMap<Long, TourTag> allTourTags = TourDatabase.getAllTourTags();

         final TourTag tourTag = finalTourTag[0] = allTourTags.get(tourTagItem.getTagId());

         dlgTitle = Messages.pref_tourtag_dlg_rename_title;
         dlgMessage = NLS.bind(Messages.Dialog_TourTag_Message_RenameTag, tourTag.getTagName());

         if (new Dialog_TourTag(Display.getCurrent().getActiveShell(), dlgTitle, dlgMessage, tourTag).open() != Window.OK) {
            return;
         }

      } else if (firstElement instanceof TVITagView_TagCategory) {

         final TVITagView_TagCategory tagCategoryItem = (TVITagView_TagCategory) firstElement;

         final HashMap<Long, TourTagCategory> allTourTagCategories = TourDatabase.getAllTourTagCategories();
         final TourTagCategory tagCategory = finalTagCategory[0] = allTourTagCategories.get(tagCategoryItem.getCategoryId());

         dlgTitle = Messages.pref_tourtag_dlg_rename_title_category;
         dlgMessage = NLS.bind(Messages.Dialog_TourTagCategory_Message_RenameCategory, tagCategory.getCategoryName());

         if (new Dialog_TourTag_Category(Display.getCurrent().getActiveShell(), dlgTitle, dlgMessage, tagCategory).open() != Window.OK) {
            return;
         }
      }

      /*
       * Update UI/model
       */
      BusyIndicator.showWhile(Display.getCurrent(), () -> {

         if (firstElement instanceof TVITagView_Tag) {

            // update model
            final TourTag tourTag = finalTourTag[0];
            TourDatabase.saveEntity(tourTag, tourTag.getTagId(), TourTag.class);

            // update UI
            final TVITagView_Tag tourTagItem = ((TVITagView_Tag) firstElement);
            tagViewer.update(tourTagItem, null);

         } else if (firstElement instanceof TVITagView_TagCategory) {

            // update model
            final TourTagCategory tagCategory = finalTagCategory[0];
            TourDatabase.saveEntity(tagCategory, tagCategory.getCategoryId(), TourTagCategory.class);

            // update UI
            final TVITagView_TagCategory tagCategory_Item = ((TVITagView_TagCategory) firstElement);
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
}
