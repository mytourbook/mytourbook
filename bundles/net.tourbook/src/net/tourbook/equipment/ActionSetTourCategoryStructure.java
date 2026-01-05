/*******************************************************************************
 * Copyright (C) 2026 Wolfgang Schramm and Contributors
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
package net.tourbook.equipment;

import java.util.ArrayList;

import javax.persistence.EntityManager;

import net.tourbook.common.util.ITourViewer;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.common.util.TreeViewerItem;
import net.tourbook.data.EquipmentPart;
import net.tourbook.database.TourDatabase;
import net.tourbook.tour.TourManager;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IMenuCreator;
import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.IStructuredSelection;
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

public class ActionSetTourCategoryStructure extends Action implements IMenuCreator {

   private Menu        _menu;

   private ITourViewer _tourViewer;

   private class ActionSetTourCategoryStructure_One extends Action {

      private int __expandType;

      public ActionSetTourCategoryStructure_One(final int expandType, final String name) {

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

               final IStructuredSelection selection = _tourViewer.getViewer().getStructuredSelection();

               for (final Object element : selection.toArray()) {

                  if (element instanceof final TVIEquipmentView_Tour tourItem) {

                     setPartStructure(tourItem.getParentItem());

                  } else {

                     setPartStructure(element);
                  }
               }

               if (_isModified) {
                  EquipmentManager.clearAllEquipmentResourcesAndFireModifyEvent();
               }
            }

            private void setPartStructure(final Object element) {

               if (element instanceof final TVIEquipmentView_Part partItem) {

                  setPartStructure_Item(partItem);

               } else if (element instanceof final TVIEquipmentView_Part_Year yearItem) {

                  setPartStructure_Item(yearItem.getPartItem());

               } else if (element instanceof final TVIEquipmentView_Part_Month monthItem) {

                  setPartStructure_Item(monthItem.getYearItem().getPartItem());
               }
            }

            private void setPartStructure_Item(final TVIEquipmentView_Part partItem) {

               // check if expand type has changed
               if (partItem.getExpandType() == __expandType) {
                  return;
               }

               // remove the children of the tag because another type of children will be displayed
               final ColumnViewer viewer = _tourViewer.getViewer();
               if (viewer instanceof final TreeViewer treeViewer) {

                  final boolean isTagExpanded = treeViewer.getExpandedState(partItem);

                  final Tree tree = treeViewer.getTree();
                  tree.setRedraw(false);
                  {
                     treeViewer.collapseToLevel(partItem, TreeViewer.ALL_LEVELS);

                     final ArrayList<TreeViewerItem> tagUnfetchedChildren = partItem.getUnfetchedChildren();
                     if (tagUnfetchedChildren != null) {
                        treeViewer.remove(tagUnfetchedChildren.toArray());
                     }

                     // set new expand type in the database
                     saveExpandType(__expandType, partItem.getPart());

                     partItem.clearChildren();

                     if (isTagExpanded) {
                        treeViewer.setExpandedState(partItem, true);
                     }

//                     // update viewer
//                     treeViewer.refresh(partItem);
                  }
                  tree.setRedraw(true);

                  _isModified = true;
               }
            }
         };

         BusyIndicator.showWhile(Display.getCurrent(), runnable);
      }
   }

   public ActionSetTourCategoryStructure(final ITourViewer tourViewer) {

      super("Set Part Stru&cture", AS_DROP_DOWN_MENU);

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
             * Create all expand types
             */
            final int selectedExpandType = getSelectedExpandType();
            int typeIndex = 0;
            for (final int expandType : EquipmentManager.EXPAND_TYPES) {

               final ActionSetTourCategoryStructure_One actionTourCategoryStructure = new ActionSetTourCategoryStructure_One(
                     expandType,
                     EquipmentManager.EXPAND_TYPE_NAMES[typeIndex++]);

               // check active expand type
               actionTourCategoryStructure.setChecked(selectedExpandType == expandType);

               addActionToMenu(actionTourCategoryStructure);
            }
         }
      });

      return _menu;
   }

   /**
    * Get expand type from the selected tour category
    *
    * @return
    */
   private int getSelectedExpandType() {

      int selectedExpandType = -1;

      final StructuredSelection selection = (StructuredSelection) _tourViewer.getViewer().getSelection();

      if (selection.size() == 1) {

         // set the expand type when only one tag is selected

         if (selection.getFirstElement() instanceof final TVIEquipmentView_Part partItem) {

            selectedExpandType = partItem.getPart().getExpandType();
         }
      }

      return selectedExpandType;
   }

   /**
    * Set the expand type for the item and save the changed model in the database
    *
    * @param expandType
    * @param part
    */
   private void saveExpandType(final int expandType, final EquipmentPart part) {

      final EntityManager em = TourDatabase.getInstance().getEntityManager();

      try {

         final long partId = part.getPartId();

         final EquipmentPart partInDb = em.find(EquipmentPart.class, partId);

         if (partInDb != null) {

            partInDb.setExpandType(expandType);

            TourDatabase.saveEntity(partInDb, partId, EquipmentPart.class);
         }

      } catch (final Exception e) {

         StatusUtil.log(e);

      } finally {

         em.close();
      }
   }
}
