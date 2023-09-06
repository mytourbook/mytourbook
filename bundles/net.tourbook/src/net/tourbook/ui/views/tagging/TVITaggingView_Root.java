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

import javax.persistence.EntityManager;
import javax.persistence.Query;

import net.tourbook.common.UI;
import net.tourbook.data.TourTag;
import net.tourbook.data.TourTagCategory;
import net.tourbook.database.TourDatabase;

import org.eclipse.jface.viewers.TreeViewer;

/**
 * Root item for the tagging view
 */
public class TVITaggingView_Root extends TVITaggingView_Item {

   private boolean _isTreeLayoutHierarchical;

   public TVITaggingView_Root(final TreeViewer tagViewer, final boolean isTreeLayoutHierarchical) {

      super(tagViewer);

      _isTreeLayoutHierarchical = isTreeLayoutHierarchical;
   }

   @Override
   protected void fetchChildren() {

      final EntityManager em = TourDatabase.getInstance().getEntityManager();

      if (em == null) {
         return;
      }

      if (_isTreeLayoutHierarchical) {
         getItemsHierarchical(em);
      } else {
         getItemsFlat(em);
      }

      em.close();
   }

   @SuppressWarnings("unchecked")
   private void getItemsFlat(final EntityManager em) {

      /*
       * Read tour tags from db
       */
      final Query query = em.createQuery(UI.EMPTY_STRING

            + "SELECT" //                                                  //$NON-NLS-1$
            + " Tag" //                                                    //$NON-NLS-1$
            + " FROM " + TourTag.class.getSimpleName() + " AS tag " //     //$NON-NLS-1$ //$NON-NLS-2$
            + " ORDER by name" //                                          //$NON-NLS-1$
      );

      final ArrayList<TourTag> allTags = (ArrayList<TourTag>) query.getResultList();

      for (final TourTag tourTag : allTags) {

         final TVITaggingView_Tag tagItem = new TVITaggingView_Tag(tourTag, this, getTagViewer());

         readTagTotals(tagItem);

         addChild(tagItem);
      }
   }

   @SuppressWarnings("unchecked")
   private void getItemsHierarchical(final EntityManager em) {

      /*
       * Read tag categories from db
       */
      Query query = em.createQuery(UI.EMPTY_STRING

            + "SELECT tagCategory" //                                                        //$NON-NLS-1$
            + " FROM " + TourTagCategory.class.getSimpleName() + " AS tagCategory" //        //$NON-NLS-1$ //$NON-NLS-2$
            + " WHERE tagCategory.isRoot = 1" //                                             //$NON-NLS-1$
            + " ORDER by name" //                                                            //$NON-NLS-1$
      );

      final ArrayList<TourTagCategory> allTagCategories = (ArrayList<TourTagCategory>) query.getResultList();

      for (final TourTagCategory tagCategory : allTagCategories) {

         addChild(new TVITaggingView_TagCategory(tagCategory, this, getTagViewer()));
      }

      /*
       * Read tour tags from db
       */
      query = em.createQuery(UI.EMPTY_STRING

            + "SELECT tag" //                                              //$NON-NLS-1$
            + " FROM " + TourTag.class.getSimpleName() + " AS tag " //     //$NON-NLS-1$ //$NON-NLS-2$
            + " WHERE tag.isRoot = 1" //                                   //$NON-NLS-1$
            + " ORDER by name" //                                          //$NON-NLS-1$
      );

      final ArrayList<TourTag> allTags = (ArrayList<TourTag>) query.getResultList();

      for (final TourTag tourTag : allTags) {

         final TVITaggingView_Tag tagItem = new TVITaggingView_Tag(tourTag, this, getTagViewer());

         readTagTotals(tagItem);

         addChild(tagItem);
      }
   }

   @Override
   public String toString() {

      return UI.EMPTY_STRING

            + "TVITaggingView_Root" + NL //                                         //$NON-NLS-1$

            + "[" + NL //                                                           //$NON-NLS-1$

            + "  _isTreeLayoutHierarchical=" + _isTreeLayoutHierarchical + NL //     //$NON-NLS-1$

            + "]" + NL //                                                           //$NON-NLS-1$
      ;
   }
}
