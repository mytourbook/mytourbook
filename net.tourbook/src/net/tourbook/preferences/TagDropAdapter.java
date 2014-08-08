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
package net.tourbook.preferences;

import java.util.ArrayList;
import java.util.Set;

import javax.persistence.EntityManager;

import net.tourbook.common.util.TreeViewerItem;
import net.tourbook.data.TourTag;
import net.tourbook.data.TourTagCategory;
import net.tourbook.database.TourDatabase;
import net.tourbook.tag.TVIPrefTag;
import net.tourbook.tag.TVIPrefTagCategory;
import net.tourbook.tag.TVIPrefTagRoot;

import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.ViewerDropAdapter;
import org.eclipse.swt.dnd.TransferData;

final class TagDropAdapter extends ViewerDropAdapter {

	private PrefPageTags	_prefPageTags;
	private TreeViewer		_tagViewer;

	TagDropAdapter(final PrefPageTags prefPageTags, final TreeViewer tagViewer) {

		super(tagViewer);

		_prefPageTags = prefPageTags;
		_tagViewer = tagViewer;
	}

	private boolean dropCategory(final TVIPrefTagCategory itemDraggedCategory) {

		final Object hoveredTarget = getCurrentTarget();

		if (hoveredTarget instanceof TVIPrefTagCategory) {

			/*
			 * drop category into another category
			 */

			dropCategory_IntoCategory(itemDraggedCategory, (TVIPrefTagCategory) hoveredTarget);

		} else if (hoveredTarget == null) {

			/*
			 * drop category item into the root
			 */

			dropCategory_IntoRoot(itemDraggedCategory);
		}

		return true;
	}

	private void dropCategory_IntoCategory(	final TVIPrefTagCategory itemDraggedCategory,
											final TVIPrefTagCategory itemTargetCategory) {

//		System.out.println("dropCategoryIntoCategory");
		final TourTagCategory draggedCategory = itemDraggedCategory.getTourTagCategory();
		final TreeViewerItem draggedParentItem = itemDraggedCategory.getParentItem();

		final TourTagCategory targetCategory = itemTargetCategory.getTourTagCategory();
		TVIPrefTagCategory itemDraggedParentCategory = null;

		boolean isUpdateViewer = false;

		if (draggedParentItem instanceof TVIPrefTagCategory) {

			/*
			 * dragged category is from another category
			 */

			final EntityManager em = TourDatabase.getInstance().getEntityManager();
			if (em != null) {

				itemDraggedParentCategory = (TVIPrefTagCategory) draggedParentItem;
				final TourTagCategory draggedParentCategory = itemDraggedParentCategory.getTourTagCategory();

				/*
				 * remove category from old category
				 */

				// remove category from parent item
				itemDraggedParentCategory.removeChild(itemDraggedCategory);

				// remove category from the database
				TourTagCategory updatedEntity = updateModel_RemoveCategory(draggedCategory, draggedParentCategory, em);
				if (updatedEntity != null) {

					// set updated categoy into the item
					itemDraggedParentCategory.setTourTagCategory(updatedEntity);

					/*
					 * add category to the new category (target)
					 */
					itemTargetCategory.addChild(itemDraggedCategory);

					updatedEntity = updateModel_AddCategory(draggedCategory, targetCategory, em);
					itemTargetCategory.setTourTagCategory(updatedEntity);

					isUpdateViewer = true;
				}

				em.close();
			}

		} else if (draggedParentItem instanceof TVIPrefTagRoot) {

			/*
			 * dragged category is a root category
			 */

			final EntityManager em = TourDatabase.getInstance().getEntityManager();
			if (em != null) {

				final TVIPrefTagRoot itemDraggedRoot = (TVIPrefTagRoot) draggedParentItem;

				/*
				 * remove category from old category
				 */

				// remove category from parent item (root)
				itemDraggedRoot.removeChild(itemDraggedCategory);

				// update category in db
				draggedCategory.setRoot(false);
				TourDatabase.saveEntity(draggedCategory, draggedCategory.getCategoryId(), TourTagCategory.class);

				/*
				 * add dragged category to the new parent category (target)
				 */
				itemTargetCategory.addChild(itemDraggedCategory);

				final TourTagCategory updatedEntity = updateModel_AddCategory(draggedCategory, targetCategory, em);
				itemTargetCategory.setTourTagCategory(updatedEntity);

				isUpdateViewer = true;

				em.close();
			}
		}

		if (isUpdateViewer) {

			/*
			 * update tag viewer
			 */

			// move category in the viewer
			_tagViewer.remove(itemDraggedCategory);
			_tagViewer.add(itemTargetCategory, itemDraggedCategory);

			// update parents
			if (itemDraggedParentCategory != null) {
				_tagViewer.update(itemDraggedParentCategory, null);
			}
			_tagViewer.update(itemTargetCategory, null);
		}
	}

	private void dropCategory_IntoRoot(final TVIPrefTagCategory draggedCategoryItem) {

//		System.out.println("dropCategoryIntoRoot");
		final EntityManager em = TourDatabase.getInstance().getEntityManager();
		if (em == null) {
			return;
		}

		final TourTagCategory draggedCategory = draggedCategoryItem.getTourTagCategory();
		final TreeViewerItem draggedCategoryParentItem = draggedCategoryItem.getParentItem();

		TVIPrefTagCategory draggedCategoryParentCategoryItem = null;

		if (draggedCategoryParentItem instanceof TVIPrefTagCategory) {

			/*
			 * parent of the dragged category is a category, remove dragged category from old
			 * category
			 */

			draggedCategoryParentCategoryItem = (TVIPrefTagCategory) draggedCategoryParentItem;
			final TourTagCategory draggedParentCategory = draggedCategoryParentCategoryItem.getTourTagCategory();

			/*
			 * remove category from old category
			 */

			// remove category from dragged parent item
			draggedCategoryParentCategoryItem.removeChild(draggedCategoryItem);

			// remove category from the database
			final TourTagCategory updatedEntity = updateModel_RemoveCategory(draggedCategory, draggedParentCategory, em);
			if (updatedEntity != null) {

				// set updated categoy into the item
				draggedCategoryParentCategoryItem.setTourTagCategory(updatedEntity);
			}
		}

		/*
		 * update category in db
		 */
		draggedCategory.setRoot(true);

		final TourTagCategory savedDraggedCategory = TourDatabase.saveEntity(
				draggedCategory,
				draggedCategory.getCategoryId(),
				TourTagCategory.class);

		/*
		 * update item with the saved category entity
		 */
		if (savedDraggedCategory != null) {
			draggedCategoryItem.setTourTagCategory(savedDraggedCategory);
		}
		em.close();

		/*
		 * add category to the root item (target)
		 */
		final TVIPrefTagRoot rootItem = _prefPageTags.getRootItem();
		rootItem.addChild(draggedCategoryItem);

		/*
		 * update tag viewer
		 */
		_tagViewer.remove(draggedCategoryItem);
		_tagViewer.add(_prefPageTags, draggedCategoryItem);

		if (draggedCategoryParentCategoryItem != null) {
			_tagViewer.update(draggedCategoryParentCategoryItem, null);
		}
	}

	/**
	 * A tag item is dragged and is dropped into a target
	 * 
	 * @param draggedTagItem
	 *            tag tree item which is dragged
	 * @return Returns <code>true</code> when the tag is dropped
	 */
	private boolean dropTag(final TVIPrefTag draggedTagItem) {

		final Object hoveredTarget = getCurrentTarget();
		if (hoveredTarget instanceof TVIPrefTag) {

			final TVIPrefTag targetTagItem = (TVIPrefTag) hoveredTarget;
			final TourTag targetTourTag = targetTagItem.getTourTag();

			if (targetTourTag.isRoot()) {

				/*
				 * drop tag into a root tag
				 */

				dropTag_IntoRoot(draggedTagItem);

			} else {

				/*
				 * drop the dragged tag into the parent category of the hovered tag
				 */

				final TreeViewerItem tagParentItem = targetTagItem.getParentItem();
				if (tagParentItem instanceof TVIPrefTagCategory) {
					dropTag_IntoCategory(draggedTagItem, (TVIPrefTagCategory) tagParentItem);
				}
			}

		} else if (hoveredTarget instanceof TVIPrefTagCategory) {

			/*
			 * drop the dragged tag into the hovered target category
			 */

			dropTag_IntoCategory(draggedTagItem, (TVIPrefTagCategory) hoveredTarget);

		} else if (hoveredTarget == null) {

			/*
			 * drop tag item into the root
			 */

			dropTag_IntoRoot(draggedTagItem);
		}

		return true;
	}

	private void dropTag_IntoCategory(final TVIPrefTag tviDraggedTag, final TVIPrefTagCategory tviTargetCategory) {

		final TourTag draggedTag = tviDraggedTag.getTourTag();
		final TreeViewerItem itemDraggedParent = tviDraggedTag.getParentItem();

		final TourTagCategory targetCategory = tviTargetCategory.getTourTagCategory();
		TVIPrefTagCategory tviDraggedParentCategory = null;

		boolean isUpdateViewer = false;

		if (itemDraggedParent instanceof TVIPrefTagCategory) {

			/*
			 * dragged tag is from a tag category
			 */

			final EntityManager em = TourDatabase.getInstance().getEntityManager();
			if (em != null) {

				tviDraggedParentCategory = (TVIPrefTagCategory) itemDraggedParent;
				final TourTagCategory draggedParentCategory = tviDraggedParentCategory.getTourTagCategory();

				/*
				 * remove tag from old category
				 */
				tviDraggedParentCategory.removeChild(tviDraggedTag);
				tviDraggedParentCategory.setTourTagCategory(//
						updateModel_RemoveTag(draggedTag, draggedParentCategory, em));

				/*
				 * add tag to the new category (target)
				 */
				tviTargetCategory.addChild(tviDraggedTag);
				tviTargetCategory.setTourTagCategory(updateModel_AddTag(draggedTag, targetCategory, em));

				em.close();

				isUpdateViewer = true;
			}

		} else if (itemDraggedParent instanceof TVIPrefTagRoot) {

			/*
			 * dragged tag is a root tag item
			 */

			final EntityManager em = TourDatabase.getInstance().getEntityManager();
			if (em != null) {

				final TVIPrefTagRoot draggedRootItem = (TVIPrefTagRoot) itemDraggedParent;

				/*
				 * remove tag from root item
				 */
				draggedRootItem.removeChild(tviDraggedTag);

				/*
				 * update tag in db
				 */
				draggedTag.setRoot(false);
				TourDatabase.saveEntity(draggedTag, draggedTag.getTagId(), TourTag.class);

				/*
				 * add tag to the new category (target)
				 */
				tviTargetCategory.addChild(tviDraggedTag);
				tviTargetCategory.setTourTagCategory(updateModel_AddTag(draggedTag, targetCategory, em));

				em.close();

				isUpdateViewer = true;
			}
		}

		if (isUpdateViewer) {

			// update tag viewer

			_tagViewer.remove(tviDraggedTag);
			_tagViewer.add(tviTargetCategory, tviDraggedTag);

			if (tviDraggedParentCategory != null) {
				_tagViewer.update(tviDraggedParentCategory, null);
			}
			_tagViewer.update(tviTargetCategory, null);
		}
	}

	private void dropTag_IntoRoot(final TVIPrefTag itemDraggedTag) {

		final EntityManager em = TourDatabase.getInstance().getEntityManager();
		if (em != null) {

			final TourTag draggedTag = itemDraggedTag.getTourTag();
			final TreeViewerItem itemDraggedTagParent = itemDraggedTag.getParentItem();

			TVIPrefTagCategory itemDraggedParentCategory = null;

			if (itemDraggedTagParent instanceof TVIPrefTagCategory) {

				/*
				 * remove tag from old category
				 */

				itemDraggedParentCategory = (TVIPrefTagCategory) itemDraggedTagParent;
				final TourTagCategory draggedParentCategory = itemDraggedParentCategory.getTourTagCategory();

				itemDraggedParentCategory.removeChild(itemDraggedTag);
				itemDraggedParentCategory.setTourTagCategory(updateModel_RemoveTag(
						draggedTag,
						draggedParentCategory,
						em));
			}

			/*
			 * update tag in db
			 */
			draggedTag.setRoot(true);
			TourDatabase.saveEntity(draggedTag, draggedTag.getTagId(), TourTag.class);

			/*
			 * add tag to the root item (target)
			 */
			final TVIPrefTagRoot rootItem = _prefPageTags.getRootItem();
			rootItem.addChild(itemDraggedTag);

			em.close();

			/*
			 * update tag viewer
			 */
			_tagViewer.remove(itemDraggedTag);
			_tagViewer.add(_prefPageTags, itemDraggedTag);

			if (itemDraggedParentCategory != null) {
				_tagViewer.update(itemDraggedParentCategory, null);
			}
		}
	}

	/**
	 * !!! Recursive method !!!<br>
	 * <br>
	 * This will check if the dragged item is dropped in one of it's children
	 * 
	 * @param itemDraggedCategory
	 * @param targetCategoryId
	 * @return Returns <code>true</code> when the dragged category will be dropped on its children
	 */
	private boolean isDraggedIntoChildren(final TVIPrefTagCategory itemDraggedCategory, final long targetCategoryId) {

		final ArrayList<TreeViewerItem> unfetchedChildren = itemDraggedCategory.getUnfetchedChildren();
		if (unfetchedChildren != null) {

			for (final TreeViewerItem treeViewerItem : unfetchedChildren) {
				if (treeViewerItem instanceof TVIPrefTagCategory) {

					final TVIPrefTagCategory itemChildCategory = (TVIPrefTagCategory) treeViewerItem;

//					System.out.println("parent: "
//							+ itemDraggedCategory.getTourTagCategory().getCategoryName()
//							+ "\tchild: "
//							+ itemChildCategory.getTourTagCategory().getCategoryName());

					// check child
					if (itemChildCategory.getTourTagCategory().getCategoryId() == targetCategoryId) {
						return true;
					}

					// check children of the child
					if (isDraggedIntoChildren(itemChildCategory, targetCategoryId)) {
						return true;
					}
				}
			}
		}

		return false;
	}

	@Override
	public boolean performDrop(final Object dropData) {

		boolean returnValue = false;

		/*
		 * check if drag was startet from this tree
		 */
		if (LocalSelectionTransfer.getTransfer().getSelectionSetTime() != _prefPageTags.getDragStartTime()) {
			return false;
		}

		if (dropData instanceof StructuredSelection) {

			final StructuredSelection selection = (StructuredSelection) dropData;
			final Object firstElement = selection.getFirstElement();

			if (selection.size() == 1 && firstElement instanceof TVIPrefTagCategory) {

				/*
				 * drop a category, to avoid confusion only one category is supported to be dragged
				 * & dropped
				 */

				returnValue = dropCategory((TVIPrefTagCategory) firstElement);

			} else {

				// drop all tags, categories will be ignored

				for (final Object element : selection.toList()) {
					if (element instanceof TVIPrefTag) {
						returnValue |= dropTag((TVIPrefTag) element);
					}
				}
			}

			if (returnValue) {
				_prefPageTags.setIsModified();
			}
		}

		return returnValue;
	}

	private TourTagCategory updateModel_AddCategory(final TourTagCategory draggedCategory,
													final TourTagCategory targetCategory,
													final EntityManager em) {

//		System.out.println("updateModelAddCategory - dragged:" + draggedCategory + "\ttarget:" + targetCategory);

		final TourTagCategory lazyTargetCategory = em.find(TourTagCategory.class, targetCategory.getCategoryId());

		// add category to the target
		final Set<TourTagCategory> targetCategories = lazyTargetCategory.getTagCategories();
		targetCategories.add(draggedCategory);

		// update counter
		lazyTargetCategory.setTagCounter(lazyTargetCategory.getTourTags().size());
		lazyTargetCategory.setCategoryCounter(targetCategories.size());

		return TourDatabase.saveEntity(lazyTargetCategory, lazyTargetCategory.getCategoryId(), TourTagCategory.class);
	}

	/**
	 * Add tag to the category
	 * 
	 * @param draggedTag
	 * @param targetCategory
	 * @param em
	 * @return Returns the saved entity
	 */
	private TourTagCategory updateModel_AddTag(	final TourTag draggedTag,
												final TourTagCategory targetCategory,
												final EntityManager em) {

		final TourTagCategory lazyTargetCategory = em.find(TourTagCategory.class, targetCategory.getCategoryId());

		// add new tag to the target category
		final Set<TourTag> targetTags = lazyTargetCategory.getTourTags();
		targetTags.add(draggedTag);

		// update counter
		lazyTargetCategory.setTagCounter(targetTags.size());
		lazyTargetCategory.setCategoryCounter(lazyTargetCategory.getTagCategories().size());

		return TourDatabase.saveEntity(lazyTargetCategory, lazyTargetCategory.getCategoryId(), TourTagCategory.class);
	}

	private TourTagCategory updateModel_RemoveCategory(	final TourTagCategory draggedCategory,
														final TourTagCategory parentCategory,
														final EntityManager em) {

//		System.out.println("updateModelRemoveCategory - dragged:" + draggedCategory + "\tparent:" + parentCategory);

		final TourTagCategory lazyParentCategory = em.find(TourTagCategory.class, parentCategory.getCategoryId());

		// remove category from the parent
		final Set<TourTagCategory> parentChildrenCategories = lazyParentCategory.getTagCategories();

		/*
		 * find the dragged category in the parent children because the object id could (was) be
		 * changed
		 */
		final long draggedCategoryId = draggedCategory.getCategoryId();
		boolean isRemoved = false;
		for (final TourTagCategory tourTagCategory : parentChildrenCategories) {
			if (tourTagCategory.getCategoryId() == draggedCategoryId) {

				final TourTagCategory lazyDraggedCategory = tourTagCategory;
				isRemoved = parentChildrenCategories.remove(lazyDraggedCategory);
				break;
			}
		}

		if (isRemoved) {

			// update counter
			lazyParentCategory.setCategoryCounter(parentChildrenCategories.size());
			lazyParentCategory.setTagCounter(lazyParentCategory.getTourTags().size());

			return TourDatabase.saveEntity(
					lazyParentCategory,
					lazyParentCategory.getCategoryId(),
					TourTagCategory.class);
		} else {
			return null;
		}
	}

	/**
	 * Remove tag from the category
	 * 
	 * @param draggedTag
	 * @param parentCategory
	 * @param em
	 * @return Returns the saved entity
	 */
	private TourTagCategory updateModel_RemoveTag(	final TourTag draggedTag,
													final TourTagCategory parentCategory,
													final EntityManager em) {

		final TourTagCategory lazyCategory = em.find(TourTagCategory.class, parentCategory.getCategoryId());

		// remove tag
		final Set<TourTag> lazyTourTags = lazyCategory.getTourTags();
		lazyTourTags.remove(draggedTag);

		// update counter
		lazyCategory.setTagCounter(lazyTourTags.size());
		lazyCategory.setCategoryCounter(lazyCategory.getTagCategories().size());

		return TourDatabase.saveEntity(lazyCategory, lazyCategory.getCategoryId(), TourTagCategory.class);
	}

	@Override
	public boolean validateDrop(final Object target, final int operation, final TransferData transferType) {

		final LocalSelectionTransfer localTransfer = LocalSelectionTransfer.getTransfer();

		if (localTransfer.isSupportedType(transferType) == false) {
			return false;
		}
		final ISelection selection = localTransfer.getSelection();

		if (selection instanceof StructuredSelection) {

			final Object draggedItem = ((StructuredSelection) selection).getFirstElement();

			if (target == draggedItem) {

				// don't drop on itself

				return false;

			} else {

				if (draggedItem instanceof TVIPrefTagCategory) {

					if (target instanceof TVIPrefTagCategory) {

						/*
						 * check if a dragged category item is dropped on one of it's children, this
						 * is not allowed
						 */

						final TVIPrefTagCategory itemDraggedCategory = (TVIPrefTagCategory) draggedItem;
						final TVIPrefTagCategory itemTargetCategory = (TVIPrefTagCategory) target;

						final long targetCategoryId = itemTargetCategory.getTourTagCategory().getCategoryId();

						if (isDraggedIntoChildren(itemDraggedCategory, targetCategoryId)) {
							return false;
						}

					} else if (target instanceof TVIPrefTag) {
						return false;
					}
				}

				return true;
			}
		}

		return false;
	}
}
