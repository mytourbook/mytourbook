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

package net.tourbook.preferences;

import java.util.Set;

import javax.persistence.EntityManager;

import net.tourbook.data.TourTag;
import net.tourbook.data.TourTagCategory;
import net.tourbook.database.TourDatabase;
import net.tourbook.tag.TVIRootItem;
import net.tourbook.tag.TVITourTag;
import net.tourbook.tag.TVITourTagCategory;
import net.tourbook.tour.TreeViewerItem;

import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.ViewerDropAdapter;
import org.eclipse.swt.dnd.TransferData;

final class TagDropAdapter extends ViewerDropAdapter {

	private PrefPageTags	fPrefPageTags;
	private TreeViewer		fTagViewer;

	TagDropAdapter(final PrefPageTags prefPageTags, final TreeViewer tagViewer) {

		super(tagViewer);

		fPrefPageTags = prefPageTags;
		fTagViewer = tagViewer;
	}

	/**
	 * A tag item is dragged and is dropped into a target
	 * 
	 * @param draggedTagItem
	 *            tag tree item which is dragged
	 * @return Returns <code>true</code> when the tag is dropped
	 */
	private boolean dropTag(final TVITourTag draggedTagItem) {

		final Object hoveredTarget = getCurrentTarget();
//						final int location = getCurrentLocation();

		/*
		 * check if drag was startet from this tree
		 */
		if (LocalSelectionTransfer.getTransfer().getSelectionSetTime() != fPrefPageTags.getDragStartTime()) {
			return false;
		}

		if (hoveredTarget instanceof TVITourTag) {

			final TVITourTag targetTagItem = (TVITourTag) hoveredTarget;
			final TourTag targetTourTag = targetTagItem.getTourTag();

			if (targetTourTag.isRoot()) {

				/*
				 * drop tag into a root tag
				 */

				dropTagIntoRoot(draggedTagItem);

			} else {

				/*
				 * drop the dragged tag into the parent category of the hovered tag
				 */

				final TreeViewerItem tagParentItem = targetTagItem.getParentItem();
				if (tagParentItem instanceof TVITourTagCategory) {
					dropTagIntoCategory(draggedTagItem, (TVITourTagCategory) tagParentItem);
				}
			}

		} else if (hoveredTarget instanceof TVITourTagCategory) {

			/*
			 * drop the dragged tag into the hovered target category
			 */

			dropTagIntoCategory(draggedTagItem, (TVITourTagCategory) hoveredTarget);

		} else if (hoveredTarget == null) {

			/*
			 * drop tag item into the root
			 */

			dropTagIntoRoot(draggedTagItem);
		}

		fPrefPageTags.setIsModified(true);

		return true;
	}

	private void dropTagIntoCategory(final TVITourTag draggedTagItem, final TVITourTagCategory targetCatItem) {

		final TourTag draggedTag = draggedTagItem.getTourTag();
		final TreeViewerItem draggedTagParentItem = draggedTagItem.getParentItem();

		final TourTagCategory targetCategory = targetCatItem.getTourTagCategory();

		boolean isUpdateViewer = false;

		if (draggedTagParentItem instanceof TVITourTagCategory) {

			/*
			 * dragged tag is from a tag category
			 */

			final EntityManager em = TourDatabase.getInstance().getEntityManager();

			if (em != null) {

				final TVITourTagCategory draggedParentCatItem = (TVITourTagCategory) draggedTagParentItem;
				final TourTagCategory draggedCategory = draggedParentCatItem.getTourTagCategory();

				/*
				 * remove tag from old category
				 */
				draggedParentCatItem.removeChild(draggedTagItem);
				updateModelRemoveCategory(draggedCategory, draggedTag, em);

				/*
				 * add tag to the new category (target)
				 */
				targetCatItem.addChild(draggedTagItem);
				updateModelAddCategory(targetCategory, draggedTag, em);

				em.close();

				isUpdateViewer = true;
			}

		} else if (draggedTagParentItem instanceof TVIRootItem) {

			/*
			 * dragged tag is a root tag item
			 */

			final EntityManager em = TourDatabase.getInstance().getEntityManager();

			if (em != null) {

				final TVIRootItem draggedRootItem = (TVIRootItem) draggedTagParentItem;

				/*
				 * remove tag from root item
				 */
				draggedRootItem.removeChild(draggedTagItem);

				/*
				 * update tag in db
				 */
				draggedTag.setRoot(false);
				TourDatabase.saveEntity(draggedTag, draggedTag.getTagId(), TourTag.class, em);

				/*
				 * add tag to the new category (target)
				 */
				targetCatItem.addChild(draggedTagItem);
				updateModelAddCategory(targetCategory, draggedTag, em);

				em.close();

				isUpdateViewer = true;
			}
		}

		if (isUpdateViewer) {

			// update tag viewer

			fTagViewer.remove(draggedTagItem);
			fTagViewer.add(targetCatItem, draggedTagItem);
		}
	}

	private void dropTagIntoRoot(final TVITourTag draggedTagItem) {

		final TourTag draggedTag = draggedTagItem.getTourTag();
		final TreeViewerItem draggedTagParentItem = draggedTagItem.getParentItem();

		final EntityManager em = TourDatabase.getInstance().getEntityManager();

		if (em != null) {

			if (draggedTagParentItem instanceof TVITourTagCategory) {

				/*
				 * remove tag from old category
				 */

				final TVITourTagCategory draggedParentCatItem = (TVITourTagCategory) draggedTagParentItem;
				final TourTagCategory draggedParentCategory = draggedParentCatItem.getTourTagCategory();

				draggedParentCatItem.removeChild(draggedTagItem);
				updateModelRemoveCategory(draggedParentCategory, draggedTag, em);
			}

			/*
			 * update tag in db
			 */
			draggedTag.setRoot(true);
			TourDatabase.saveEntity(draggedTag, draggedTag.getTagId(), TourTag.class, em);

			/*
			 * add tag to the root item (target)
			 */
			final TVIRootItem rootItem = fPrefPageTags.getRootItem();
			rootItem.addChild(draggedTagItem);

			em.close();

			/*
			 * update tag viewer
			 */
			fTagViewer.remove(draggedTagItem);
			fTagViewer.add(fPrefPageTags, draggedTagItem);
		}
	}

	@Override
	public boolean performDrop(final Object dropData) {

		boolean returnValue = false;

		if (dropData instanceof StructuredSelection) {

			final StructuredSelection selection = (StructuredSelection) dropData;

			for (final Object element : selection.toList()) {
				if (element instanceof TVITourTag) {
					returnValue |= dropTag((TVITourTag) element);
				}
			}
		}

		return returnValue;
	}

	private void updateModelAddCategory(final TourTagCategory category, final TourTag dropTag, final EntityManager em) {

		final TourTagCategory lazyCategory = em.find(TourTagCategory.class, category.getCategoryId());

		// add new tag
		final Set<TourTag> lazyTourTags = lazyCategory.getTourTags();
		lazyTourTags.add(dropTag);

		TourDatabase.saveEntity(lazyCategory, lazyCategory.getCategoryId(), TourTagCategory.class, em);
	}

	private void updateModelRemoveCategory(final TourTagCategory category, final TourTag dropTag, final EntityManager em) {

		final TourTagCategory lazyCategory = em.find(TourTagCategory.class, //
				category.getCategoryId());

		// remove tag
		final Set<TourTag> lazyTourTags = lazyCategory.getTourTags();
		lazyTourTags.remove(dropTag);

		TourDatabase.saveEntity(lazyCategory, lazyCategory.getCategoryId(), TourTagCategory.class, em);
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

//				if (draggedItem instanceof TVITourTag) {
//					if (target instanceof TVITourTagCategory || target instanceof TVITourTag) {
//						// drag tag into category or on a tag which will use the parent as category
//						return true;
//					} else if (target == null) {
//						// drag tag into root item
//						return true;
//					}
//				}

				return true;
			}
		}

		return false;
	}
}
