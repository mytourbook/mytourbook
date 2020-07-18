/*******************************************************************************
 * Copyright (C) 2005, 2020 Wolfgang Schramm and Contributors
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

import de.byteholder.geoclipse.map.UI;

import java.util.ArrayList;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import net.tourbook.data.TourTag;
import net.tourbook.data.TourTagCategory;
import net.tourbook.database.TourDatabase;

import org.eclipse.jface.viewers.TreeViewer;

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
       * read tour tags from db
       */
      final Query query = em.createQuery(
            UI.EMPTY_STRING

                  + "SELECT" //$NON-NLS-1$

                  + " Tag" //$NON-NLS-1$

                  + " FROM " + TourTag.class.getSimpleName() + " AS Tag " //$NON-NLS-1$ //$NON-NLS-2$

      );

      final ArrayList<TourTag> tourTags = (ArrayList<TourTag>) query.getResultList();

      for (final TourTag tourTag : tourTags) {
         final TVIPrefTag tagItem = new TVIPrefTag(getTagViewer(), tourTag);
         addChild(tagItem);
      }
   }

   @SuppressWarnings("unchecked")
   private void getItemsHierarchical(final EntityManager em) {

      /*
       * read tour tags from db
       */
      Query query = em.createQuery(//
            //
            "SELECT tag" //$NON-NLS-1$
                  + (" FROM " + TourTag.class.getSimpleName() + " AS tag ") //$NON-NLS-1$ //$NON-NLS-2$
                  + (" WHERE tag.isRoot = 1")); //$NON-NLS-1$

      final ArrayList<TourTag> tourTags = (ArrayList<TourTag>) query.getResultList();

      for (final TourTag tourTag : tourTags) {
         final TVIPrefTag tagItem = new TVIPrefTag(getTagViewer(), tourTag);
         addChild(tagItem);
      }

      /*
       * read tag categories from db
       */
      query = em.createQuery(//
            //
            "SELECT tagCategory" //$NON-NLS-1$
                  + (" FROM " + TourTagCategory.class.getSimpleName() + " AS tagCategory") //$NON-NLS-1$ //$NON-NLS-2$
                  + (" WHERE tagCategory.isRoot = 1")); //$NON-NLS-1$

      final ArrayList<TourTagCategory> tourTagCategories = (ArrayList<TourTagCategory>) query.getResultList();

      for (final TourTagCategory tourTagCategory : tourTagCategories) {
         final TVIPrefTagCategory categoryItem = new TVIPrefTagCategory(getTagViewer(), tourTagCategory);
         addChild(categoryItem);
      }
   }
}
