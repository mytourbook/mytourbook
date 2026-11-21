/*******************************************************************************
 * Copyright (C) 2005, 2026 Wolfgang Schramm and Contributors
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

import java.util.List;

import net.tourbook.common.UI;
import net.tourbook.data.TourTag;
import net.tourbook.data.TourTagCategory;
import net.tourbook.database.TourDatabase;

import org.eclipse.jface.viewers.TreeViewer;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;

public class TVIPrefTagRoot extends TVIPrefTagItem {

   private boolean _isTreeLayoutHierarchical;

   /**
    * @param tagViewer
    * @param isTreeLayoutHierarchical
    *           Is <code>true</code> when the tree is displayed hierarchical, otherwise the tree
    *           items are displayed flat
    */
   public TVIPrefTagRoot(final TreeViewer tagViewer, final boolean isTreeLayoutHierarchical) {

      super(tagViewer);

      _isTreeLayoutHierarchical = isTreeLayoutHierarchical;
   }

   @Override
   protected void fetchChildren() {

      try (final EntityManager em = TourDatabase.getInstance().getEntityManager()) {

         if (_isTreeLayoutHierarchical) {

            getItemsHierarchical(em);

         } else {

            getItemsFlat(em);
         }
      }
   }

   private void getItemsFlat(final EntityManager em) {

      /*
       * Read tour tags from db
       */
      final String sql = UI.EMPTY_STRING

            + "SELECT emTag" //$NON-NLS-1$
            + " FROM " + TourTag.class.getSimpleName() + " AS emTag "; //$NON-NLS-1$ //$NON-NLS-2$

      final TypedQuery<TourTag> emQuery = em.createQuery(sql, TourTag.class);

      final List<TourTag> allTourTags = emQuery.getResultList();

      for (final TourTag tourTag : allTourTags) {

         final TVIPrefTag tagItem = new TVIPrefTag(getTagViewer(), tourTag);
         addChild(tagItem);
      }
   }

   private void getItemsHierarchical(final EntityManager em) {

      /*
       * Read tour tags from db
       */
      String sql = UI.EMPTY_STRING

            + "SELECT emTag" + NL //                                            //$NON-NLS-1$
            + " FROM " + TourTag.class.getSimpleName() + " AS emTag " + NL //   //$NON-NLS-1$ //$NON-NLS-2$
            + " WHERE emTag.isRoot = 1" + NL; //                                //$NON-NLS-1$

      final TypedQuery<TourTag> emTagQuery = em.createQuery(sql, TourTag.class);

      final List<TourTag> allTourTags = emTagQuery.getResultList();

      for (final TourTag tourTag : allTourTags) {

         final TVIPrefTag tagItem = new TVIPrefTag(getTagViewer(), tourTag);
         addChild(tagItem);
      }

      /*
       * Read tag categories from db
       */
      sql = UI.EMPTY_STRING

            + "SELECT emTagCategory" //$NON-NLS-1$
            + " FROM " + TourTagCategory.class.getSimpleName() + " AS emTagCategory" //$NON-NLS-1$ //$NON-NLS-2$
            + " WHERE emTagCategory.isRoot = 1"; //$NON-NLS-1$

      final TypedQuery<TourTagCategory> emTagCatQuery = em.createQuery(sql, TourTagCategory.class);

      final List<TourTagCategory> allTourTagCategories = emTagCatQuery.getResultList();

      for (final TourTagCategory tourTagCategory : allTourTagCategories) {

         final TVIPrefTagCategory categoryItem = new TVIPrefTagCategory(getTagViewer(), tourTagCategory);
         addChild(categoryItem);
      }
   }
}
