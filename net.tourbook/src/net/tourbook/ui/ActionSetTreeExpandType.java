/*******************************************************************************
 * Copyright (C) 2005, 2008  Wolfgang Schramm and Contributors
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

import java.util.ArrayList;

import net.tourbook.Messages;
import net.tourbook.data.TourTag;
import net.tourbook.tour.TreeViewerItem;
import net.tourbook.ui.views.tourTag.TVITagViewTag;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IMenuCreator;
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

public class ActionSetTreeExpandType extends Action implements IMenuCreator {

	private Menu					fMenu;

	private ITourViewer				fTourViewer;

	private static final String[]	fExpandTypeNames	= {
			Messages.app_action_expand_type_flat,
			Messages.app_action_expand_type_year_day,
			Messages.app_action_expand_type_year_month_day };

	private static final int[]		fExpandTypes		= {
			TourTag.EXPAND_TYPE_FLAT,
			TourTag.EXPAND_TYPE_YEAR_DAY,
			TourTag.EXPAND_TYPE_YEAR_MONTH_DAY			};

	private class ActionTagStructure extends Action {

		private int	fExpandType;

		public ActionTagStructure(final int expandType, final String name) {

			super(name, AS_CHECK_BOX);
			fExpandType = expandType;
		}

		@Override
		public void run() {

			final Runnable runnable = new Runnable() {

				public void run() {

					final StructuredSelection selection = (StructuredSelection) fTourViewer.getTreeViewer()
							.getSelection();
					if (selection.getFirstElement() instanceof TVITagViewTag) {

						final TVITagViewTag tagItem = (TVITagViewTag) selection.getFirstElement();

						// check if expand type has changed
						if (tagItem.getExpandType() == fExpandType) {
							return;
						}

						// remove the children of the tag because another type of children will be displayed
						final TreeViewer treeViewer = fTourViewer.getTreeViewer();

						final boolean isTagExpanded = treeViewer.getExpandedState(tagItem);

						final Tree tree = treeViewer.getTree();
						tree.setRedraw(false);
						{
							treeViewer.collapseToLevel(tagItem, TreeViewer.ALL_LEVELS);
							final ArrayList<TreeViewerItem> tagUnfetchedChildren = tagItem.getUnfetchedChildren();
							if (tagUnfetchedChildren != null) {
								treeViewer.remove(tagUnfetchedChildren.toArray());
							}
						}
						tree.setRedraw(true);

						// set new expand type
						tagItem.setNewExpandType(fExpandType);

						tagItem.clearChildren();

						if (isTagExpanded) {
							treeViewer.setExpandedState(tagItem, true);
						}

						treeViewer.refresh(tagItem);
					}
				}

			};
			BusyIndicator.showWhile(Display.getCurrent(), runnable);
		}
	}

	public ActionSetTreeExpandType(final ITourViewer tourViewer) {

		super(Messages.app_action_set_tour_tag_tree_expand_type, AS_DROP_DOWN_MENU);
		setMenuCreator(this);

		fTourViewer = tourViewer;
	}

	private void addActionToMenu(final Action action) {

		final ActionContributionItem item = new ActionContributionItem(action);
		item.fill(fMenu, -1);
	}

	public void dispose() {
		if (fMenu != null) {
			fMenu.dispose();
			fMenu = null;
		}
	}

	public Menu getMenu(final Control parent) {
		return null;
	}

	public Menu getMenu(final Menu parent) {

		dispose();
		fMenu = new Menu(parent);

		// Add listener to repopulate the menu each time
		fMenu.addMenuListener(new MenuAdapter() {
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
				for (final int expandType : fExpandTypes) {

					final ActionTagStructure actionTagStructure = new ActionTagStructure(expandType,
							fExpandTypeNames[typeIndex++]);

					// check active expand type
					actionTagStructure.setChecked(selectedExpandType == expandType);

					addActionToMenu(actionTagStructure);
				}
			}
		});

		return fMenu;
	}

	/**
	 * get expand type from the selected tag
	 * 
	 * @return
	 */
	private int getSelectedExpandType() {
		int selectedExpandType = -1;
		final StructuredSelection selection = (StructuredSelection) fTourViewer.getTreeViewer().getSelection();
		if (selection.getFirstElement() instanceof TVITagViewTag) {
			final TVITagViewTag itemTag = (TVITagViewTag) selection.getFirstElement();
			selectedExpandType = itemTag.getExpandType();
		}
		return selectedExpandType;
	}
}
