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

import java.util.ArrayList;

import javax.persistence.EntityManager;

import net.tourbook.Messages;
import net.tourbook.common.util.ITourViewer;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.common.util.TreeViewerItem;
import net.tourbook.data.TourTag;
import net.tourbook.database.TourDatabase;
import net.tourbook.tour.TourManager;
import net.tourbook.ui.views.tagging.TVITaggingView_Month;
import net.tourbook.ui.views.tagging.TVITaggingView_Tag;
import net.tourbook.ui.views.tagging.TVITaggingView_Tour;
import net.tourbook.ui.views.tagging.TVITaggingView_Year;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IMenuCreator;
import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.events.MenuAdapter;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Tree;

public class ActionSetTagStructure extends Action implements IMenuCreator {

   private Menu        _menu;

   private ITourViewer _tourViewer;

   private class ActionSetTagStructure_One extends Action {

      private int __expandType;

      public ActionSetTagStructure_One(final int expandType, final String name) {

         super(name, AS_CHECK_BOX);

         __expandType = expandType;
      }

      @Override
      public void run() {

         final Runnable runnable = new Runnable() {

            private boolean _isModified;

            @Override
            public void run() {

               // check if the tour editor contains a modified tour
               if (TourManager.isTourEditorModified()) {
                  return;
               }

               final StructuredSelection selection = (StructuredSelection) _tourViewer.getViewer().getSelection();

               for (final Object element : selection.toArray()) {

                  if (element instanceof final TVITaggingView_Tour tourItem) {

                     setTagStructure(tourItem.getParentItem());

                  } else {

                     setTagStructure(element);
                  }
               }

               if (_isModified) {
                  TagManager.clearAllTagResourcesAndFireModifyEvent();
               }
            }

            private void setTagStructure(final Object element) {

               if (element instanceof final TVITaggingView_Tag tagItem) {

                  setTagStructure_Item(tagItem);

               } else if (element instanceof final TVITaggingView_Year yearItem) {

                  setTagStructure_Item(yearItem.getTagItem());

               } else if (element instanceof final TVITaggingView_Month monthItem) {

                  setTagStructure_Item(monthItem.getYearItem().getTagItem());
               }
            }

            private void setTagStructure_Item(final TVITaggingView_Tag tagItem) {

               // check if expand type has changed
               if (tagItem.getExpandType() == __expandType) {
                  return;
               }

               // remove the children of the tag because another type of children will be displayed
               final ColumnViewer viewer = _tourViewer.getViewer();
               if (viewer instanceof final TreeViewer treeViewer) {

                  final boolean isTagExpanded = treeViewer.getExpandedState(tagItem);

                  final Tree tree = treeViewer.getTree();
                  tree.setRedraw(false);
                  {
                     treeViewer.collapseToLevel(tagItem, TreeViewer.ALL_LEVELS);

                     final ArrayList<TreeViewerItem> tagUnfetchedChildren = tagItem.getUnfetchedChildren();
                     if (tagUnfetchedChildren != null) {
                        treeViewer.remove(tagUnfetchedChildren.toArray());
                     }

                     // set new expand type in the database
                     saveExpandType(__expandType, tagItem.getTourTag());

                     tagItem.clearChildren();

                     if (isTagExpanded) {
                        treeViewer.setExpandedState(tagItem, true);
                     }

//                     // update viewer
//                     treeViewer.refresh(tagItem);
                  }
                  tree.setRedraw(true);

                  _isModified = true;
               }
            }
         };

         BusyIndicator.showWhile(Display.getCurrent(), runnable);
      }
   }

   public ActionSetTagStructure(final ITourViewer tourViewer) {

      super(Messages.action_tag_set_tag_expand_type, AS_DROP_DOWN_MENU);

      setMenuCreator(this);

      _tourViewer = tourViewer;
   }

   private void addActionToMenu(final Action action) {

      final ActionContributionItem item = new ActionContributionItem(action);
      item.fill(_menu, -1);
   }

   @Override
   public void dispose() {
      if (_menu != null) {
         _menu.dispose();
         _menu = null;
      }
   }

   @Override
   public Menu getMenu(final Control parent) {
      return null;
   }

   @Override
   public Menu getMenu(final Menu parent) {

      dispose();
      _menu = new Menu(parent);

      // Add listener to repopulate the menu each time
      _menu.addMenuListener(new MenuAdapter() {
         @Override
         public void menuShown(final MenuEvent e) {

            final Menu menu = (Menu) e.widget;

            // dispose old items
            for (final MenuItem menuItem : menu.getItems()) {
               menuItem.dispose();
            }

            /*
             * create all expand types
             */
            final int selectedExpandType = getSelectedExpandType();
            int typeIndex = 0;
            for (final int expandType : TagManager.EXPAND_TYPES) {

               final ActionSetTagStructure_One actionTagStructure = new ActionSetTagStructure_One(
                     expandType,
                     TagManager.EXPAND_TYPE_NAMES[typeIndex++]);

               // check active expand type
               actionTagStructure.setChecked(selectedExpandType == expandType);

               addActionToMenu(actionTagStructure);
            }
         }
      });

      return _menu;
   }

   /**
    * get expand type from the selected tag
    *
    * @return
    */
   private int getSelectedExpandType() {

      int selectedExpandType = -1;
      final StructuredSelection selection = (StructuredSelection) _tourViewer.getViewer().getSelection();

      if (selection.size() == 1) {

         // set the expand type when only one tag is selected

         if (selection.getFirstElement() instanceof TVITaggingView_Tag) {
            final TVITaggingView_Tag itemTag = (TVITaggingView_Tag) selection.getFirstElement();
            selectedExpandType = itemTag.getExpandType();
         }
      }
      return selectedExpandType;
   }

   /**
    * Set the expand type for the item and save the changed model in the database
    *
    * @param expandType
    * @param tourTag
    */
   private void saveExpandType(final int expandType, final TourTag tourTag) {

      final EntityManager em = TourDatabase.getInstance().getEntityManager();

      try {

         final long tagId = tourTag.getTagId();

         final TourTag tagInDb = em.find(TourTag.class, tagId);

         if (tagInDb != null) {

            tagInDb.setExpandType(expandType);

            TourDatabase.saveEntity(tagInDb, tagId, TourTag.class);
         }

      } catch (final Exception e) {

         StatusUtil.log(e);

      } finally {

         em.close();
      }
   }
}
