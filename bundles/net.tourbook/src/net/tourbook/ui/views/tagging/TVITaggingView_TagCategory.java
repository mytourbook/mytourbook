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
package net.tourbook.ui.views.tagging;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Set;

import javax.persistence.EntityManager;

import net.tourbook.common.UI;
import net.tourbook.data.TourTag;
import net.tourbook.data.TourTagCategory;
import net.tourbook.database.TourDatabase;

import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.widgets.Tree;

public class TVITaggingView_TagCategory extends TVITaggingView_Item {

   private TourTagCategory _tagCategory;

   int                     numTagCategories;
   int                     numTags;

   public TVITaggingView_TagCategory(final TourTagCategory tagCategory,
                                     final TVITaggingView_Item parentItem,
                                     final TreeViewer treeViewer) {

      super(treeViewer);

      _tagCategory = tagCategory;

      firstColumn = tagCategory.getCategoryName();

      if (UI.IS_SCRAMBLE_DATA) {
         firstColumn = UI.scrambleText(firstColumn);
      }

      setParentItem(parentItem);
   }

   @Override
   protected void fetchChildren() {

      final EntityManager em = TourDatabase.getInstance().getEntityManager();

      if (em == null) {
         return;
      }

      final TreeViewer tagViewer = getTagViewer();

      final TourTagCategory tourTagCategory = em.find(TourTagCategory.class, _tagCategory.getCategoryId());

      // create category items
      final Set<TourTagCategory> lazyTourTagCategories = tourTagCategory.getTagCategories();

      final ArrayList<TourTagCategory> sortedCategories = new ArrayList<>(lazyTourTagCategories);
      Collections.sort(sortedCategories);

      for (final TourTagCategory tagCategory : lazyTourTagCategories) {
         addChild(new TVITaggingView_TagCategory(tagCategory, this, tagViewer));
      }

      // create tag items
      final Set<TourTag> lazyTourTags = tourTagCategory.getTourTags();
      for (final TourTag tourTag : lazyTourTags) {

         final TVITaggingView_Tag tagItem = new TVITaggingView_Tag(tourTag, this, tagViewer);

         readTagTotals(tagItem);

         addChild(tagItem);
      }

      // update number of categories/tags
      _tagCategory.setNumberOfTags(lazyTourTags.size());
      _tagCategory.setNumberOfCategories(lazyTourTagCategories.size());

      em.close();

      /*
       * Show number of tags/categories in the viewer, this must be done after the viewer task is
       * finished
       */
      final Tree tree = tagViewer.getTree();
      tree.getDisplay().asyncExec(() -> {

         if (tree.isDisposed()) {
            return;
         }

         tagViewer.update(TVITaggingView_TagCategory.this, null);
      });
   }

   public TourTagCategory getTourTagCategory() {

      return _tagCategory;
   }

   public void setTagCategory(final TourTagCategory tagCategory) {

      _tagCategory = tagCategory;
   }

   @Override
   public String toString() {

      return UI.EMPTY_STRING

            + "TVITagView_TagCategory " + System.identityHashCode(this) + NL //$NON-NLS-1$

            + "[" + NL //                                               //$NON-NLS-1$

            + _tagCategory

            + NL
            + "  numTours          = " + numTours + NL //               //$NON-NLS-1$
            + "  numTags_NoTours   = " + numTags_NoTours + NL //        //$NON-NLS-1$

            + "]" + NL //                                               //$NON-NLS-1$
      ;
   }
}
