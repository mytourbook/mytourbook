/*******************************************************************************
 * Copyright (C) 2005, 2014  Wolfgang Schramm and Contributors
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

import net.tourbook.Messages;
import net.tourbook.common.util.ITourViewer;
import net.tourbook.common.util.TreeViewerItem;
import net.tourbook.ui.views.tagging.TVITagView_Month;
import net.tourbook.ui.views.tagging.TVITagView_Tag;
import net.tourbook.ui.views.tagging.TVITagView_Tour;
import net.tourbook.ui.views.tagging.TVITagView_Year;

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

public class ActionMenuSetTagStructure extends Action implements IMenuCreator {

	private Menu		_menu;

	private ITourViewer	_tourViewer;

	private class ActionSetTagStructure extends Action {

		private int	__expandType;

		public ActionSetTagStructure(final int expandType, final String name) {

			super(name, AS_CHECK_BOX);

			__expandType = expandType;
		}

		@Override
		public void run() {

			final Runnable runnable = new Runnable() {

				public void run() {

					final StructuredSelection selection = (StructuredSelection) _tourViewer.getViewer().getSelection();

					for (final Object element : selection.toArray()) {

						if (element instanceof TVITagView_Tour) {
							setTagStructure(((TVITagView_Tour) element).getParentItem());
						} else {
							setTagStructure(element);
						}
					}
				}

				private void setTagStructure(final Object element) {

					if (element instanceof TVITagView_Tag) {

						setTagStructure_Item((TVITagView_Tag) element);

					} else if (element instanceof TVITagView_Year) {

						setTagStructure_Item(((TVITagView_Year) element).getTagItem());

					} else if (element instanceof TVITagView_Month) {

						setTagStructure_Item(((TVITagView_Month) element).getYearItem().getTagItem());
					}
				}

				private void setTagStructure_Item(final TVITagView_Tag tagItem) {

					// check if expand type has changed
					if (tagItem.getExpandType() == __expandType) {
						return;
					}

					// remove the children of the tag because another type of children will be displayed
					final ColumnViewer viewer = _tourViewer.getViewer();
					if (viewer instanceof TreeViewer) {

						final TreeViewer treeViewer = (TreeViewer) viewer;

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
							tagItem.setNewExpandType(__expandType);

							tagItem.clearChildren();

							if (isTagExpanded) {
								treeViewer.setExpandedState(tagItem, true);
							}

							// update viewer
							treeViewer.refresh(tagItem);
						}
						tree.setRedraw(true);
					}
				}
			};

			BusyIndicator.showWhile(Display.getCurrent(), runnable);
		}
	}

	public ActionMenuSetTagStructure(final ITourViewer tourViewer) {

		super(Messages.action_tag_set_tag_expand_type, AS_DROP_DOWN_MENU);
		setMenuCreator(this);

		_tourViewer = tourViewer;
	}

	private void addActionToMenu(final Action action) {

		final ActionContributionItem item = new ActionContributionItem(action);
		item.fill(_menu, -1);
	}

	public void dispose() {
		if (_menu != null) {
			_menu.dispose();
			_menu = null;
		}
	}

	public Menu getMenu(final Control parent) {
		return null;
	}

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

					final ActionSetTagStructure actionTagStructure = new ActionSetTagStructure(
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

			if (selection.getFirstElement() instanceof TVITagView_Tag) {
				final TVITagView_Tag itemTag = (TVITagView_Tag) selection.getFirstElement();
				selectedExpandType = itemTag.getExpandType();
			}
		}
		return selectedExpandType;
	}
}
