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
package net.tourbook.sign;


public class ActionMenuSetSignStructure_DISABLED /* extends Action implements IMenuCreator */{

//	private Menu		fMenu;
//
//	private ITourViewer	fTourViewer;
//
//	private class ActionSetTagStructure extends Action {
//
//		private int	fExpandType;
//
//		public ActionSetTagStructure(final int expandType, final String name) {
//
//			super(name, AS_CHECK_BOX);
//			fExpandType = expandType;
//		}
//
//		@Override
//		public void run() {
//
//			final Runnable runnable = new Runnable() {
//
//				public void run() {
//
//					final StructuredSelection selection = (StructuredSelection) fTourViewer.getViewer().getSelection();
//
//					for (final Object element : selection.toArray()) {
//
//						if (element instanceof TVITagView_Tour) {
//							setTagStructure(((TVITagView_Tour) element).getParentItem());
//						} else {
//							setTagStructure(element);
//						}
//					}
//				}
//
//				private void setTagStructure(final Object element) {
//
//					if (element instanceof TVITagView_Tag) {
//
//						setTagStructure2((TVITagView_Tag) element);
//
//					} else if (element instanceof TVITagView_Year) {
//
//						setTagStructure2(((TVITagView_Year) element).getTagItem());
//
//					} else if (element instanceof TVITagView_Month) {
//
//						setTagStructure2(((TVITagView_Month) element).getYearItem().getTagItem());
//					}
//				}
//
//				private void setTagStructure2(final TVITagView_Tag tagItem) {
//
//					// check if expand type has changed
//					if (tagItem.getExpandType() == fExpandType) {
//						return;
//					}
//
//					// remove the children of the tag because another type of children will be displayed
//					final ColumnViewer viewer = fTourViewer.getViewer();
//					if (viewer instanceof TreeViewer) {
//
//						final TreeViewer treeViewer = (TreeViewer) viewer;
//
//						final boolean isTagExpanded = treeViewer.getExpandedState(tagItem);
//
//						final Tree tree = treeViewer.getTree();
//						tree.setRedraw(false);
//						{
//							treeViewer.collapseToLevel(tagItem, TreeViewer.ALL_LEVELS);
//							final ArrayList<TreeViewerItem> tagUnfetchedChildren = tagItem.getUnfetchedChildren();
//							if (tagUnfetchedChildren != null) {
//								treeViewer.remove(tagUnfetchedChildren.toArray());
//							}
//						}
//						tree.setRedraw(true);
//
//						// set new expand type in the database
//						tagItem.setNewExpandType(fExpandType);
//
//						tagItem.clearChildren();
//
//						if (isTagExpanded) {
//							treeViewer.setExpandedState(tagItem, true);
//						}
//
//						// update viewer
//						treeViewer.refresh(tagItem);
//					}
//				}
//			};
//
//			BusyIndicator.showWhile(Display.getCurrent(), runnable);
//		}
//	}
//
//	public ActionMenuSetSignStructure(final ITourViewer tourViewer) {
//
//		super(Messages.action_tag_set_tag_expand_type, AS_DROP_DOWN_MENU);
//		setMenuCreator(this);
//
//		fTourViewer = tourViewer;
//	}
//
//	private void addActionToMenu(final Action action) {
//
//		final ActionContributionItem item = new ActionContributionItem(action);
//		item.fill(fMenu, -1);
//	}
//
//	public void dispose() {
//		if (fMenu != null) {
//			fMenu.dispose();
//			fMenu = null;
//		}
//	}
//
//	public Menu getMenu(final Control parent) {
//		return null;
//	}
//
//	public Menu getMenu(final Menu parent) {
//
//		dispose();
//		fMenu = new Menu(parent);
//
//		// Add listener to repopulate the menu each time
//		fMenu.addMenuListener(new MenuAdapter() {
//			@Override
//			public void menuShown(final MenuEvent e) {
//
//				final Menu menu = (Menu) e.widget;
//
//				// dispose old items
//				for (final MenuItem menuItem : menu.getItems()) {
//					menuItem.dispose();
//				}
//
//				/*
//				 * create all expand types
//				 */
//				final int selectedExpandType = getSelectedExpandType();
//				int typeIndex = 0;
//				for (final int expandType : SignManager.EXPAND_TYPES) {
//
//					final ActionSetTagStructure actionTagStructure = new ActionSetTagStructure(expandType,
//							SignManager.EXPAND_TYPE_NAMES[typeIndex++]);
//
//					// check active expand type
//					actionTagStructure.setChecked(selectedExpandType == expandType);
//
//					addActionToMenu(actionTagStructure);
//				}
//			}
//		});
//
//		return fMenu;
//	}
//
//	/**
//	 * get expand type from the selected tag
//	 *
//	 * @return
//	 */
//	private int getSelectedExpandType() {
//
//		int selectedExpandType = -1;
//		final StructuredSelection selection = (StructuredSelection) fTourViewer.getViewer().getSelection();
//
//		if (selection.size() == 1) {
//
//			// set the expand type when only one tag is selected
//
//			if (selection.getFirstElement() instanceof TVITagView_Tag) {
//				final TVITagView_Tag itemTag = (TVITagView_Tag) selection.getFirstElement();
//				selectedExpandType = itemTag.getExpandType();
//			}
//		}
//		return selectedExpandType;
//	}
}
