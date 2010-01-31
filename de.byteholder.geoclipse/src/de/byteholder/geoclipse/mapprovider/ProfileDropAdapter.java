/*******************************************************************************
 * Copyright (C) 2005, 2009  Wolfgang Schramm and Contributors
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
package de.byteholder.geoclipse.mapprovider;

import java.util.ArrayList;

import net.tourbook.util.StatusUtil;

import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.ViewerDropAdapter;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.TransferData;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.ui.dialogs.ContainerCheckedTreeViewer;

class ProfileDropAdapter extends ViewerDropAdapter {

	private DialogMPProfile				fDialogMPProfile;
	private ContainerCheckedTreeViewer	fMpViewer;

	private Widget						fTargetTreeItem;

	ProfileDropAdapter(final DialogMPProfile dialogMPProfile, final ContainerCheckedTreeViewer treeViewer) {

		super(treeViewer);

		fDialogMPProfile = dialogMPProfile;
		fMpViewer = treeViewer;
	}

	@Override
	public void dragOver(final DropTargetEvent event) {

		fTargetTreeItem = event.item;

		super.dragOver(event);
	}

	@Override
	public boolean performDrop(final Object droppedData) {

		/*
		 * check if drag was startet only from this tree
		 */
		if (LocalSelectionTransfer.getTransfer().getSelectionSetTime() != fDialogMPProfile.getDragStartTime()) {
			return false;
		}

		if (droppedData instanceof StructuredSelection) {

			final Object firstElement = ((StructuredSelection) droppedData).getFirstElement();
			if (firstElement instanceof TVIMapProvider) {

				// reorder map provider

				return reorderMapProvider((TVIMapProvider) firstElement);

			} else if (firstElement instanceof TVIWmsLayer) {

				// reorder wms layer

				return reorderWmsLayer((TVIWmsLayer) firstElement);
			}
		}

		return false;
	}

	private boolean reorderMapProvider(final TVIMapProvider droppedMP) {

		final Tree mpTree = fMpViewer.getTree();
		final TVIMapProviderRoot rootItem = fDialogMPProfile.getRootItem();

		// remove drop item before it is inserted
		fMpViewer.remove(droppedMP);

		int itemIndex;

		if (fTargetTreeItem == null) {

			// a tree item is not hovered

			fMpViewer.add(rootItem, droppedMP);
			itemIndex = mpTree.getItemCount() - 1;

		} else {

			// get index of the target in the table
			itemIndex = mpTree.indexOf((TreeItem) fTargetTreeItem);
			if (itemIndex == -1) {
				return false;
			}

			// insert into the tree
			final int location = getCurrentLocation();
			if (location == LOCATION_BEFORE) {
				fMpViewer.insert(rootItem, droppedMP, itemIndex);
			} else if (location == LOCATION_AFTER) {
				fMpViewer.insert(rootItem, droppedMP, ++itemIndex);
			}
		}

		// reselect filter item
		fMpViewer.setSelection(new StructuredSelection(droppedMP));

		// set focus and selection
		final TreeItem droppedTreeItem = mpTree.getItem(itemIndex);

		mpTree.select(droppedTreeItem);
		mpTree.setFocus();

		fDialogMPProfile.updateLiveView();

		return true;
	}

	private boolean reorderWmsLayer(final TVIWmsLayer droppedTviWmsLayer) {

		if (fTargetTreeItem == null) {
			// a tree item is not hovered
			return false;
		}

		final TVIMapProvider tviParent = (TVIMapProvider) droppedTviWmsLayer.getParentItem();
		final ArrayList<TreeViewerItem> tviChildren = tviParent.getChildren();

		/*
		 * get index of the source+target within the parent
		 */
		final Object tviItem = fTargetTreeItem.getData();
		int toIndex = -1;
		int fromIndex = -1;
		if (tviItem instanceof TVIWmsLayer) {
			final TVIWmsLayer targetTvi = (TVIWmsLayer) tviItem;

			int childIndex = 0;

			for (final TreeViewerItem tviChild : tviChildren) {

				if (tviChild == droppedTviWmsLayer) {
					fromIndex = childIndex;
				}
				if (tviChild == targetTvi) {
					toIndex = childIndex;
				}

				childIndex++;
			}

			if (fromIndex == -1 || toIndex == -1) {
				StatusUtil.showStatus("invalid index", new Exception());
				return false;
			}
		}

		// adjust position, otherwise the target is not correct
		final int currentLocation = getCurrentLocation();
		if (toIndex < fromIndex) {
			if (currentLocation == LOCATION_AFTER) {
				toIndex++;
			}
		} else {
			if (currentLocation == LOCATION_BEFORE) {
				toIndex--;
			}
		}

		// update model
		tviChildren.remove(fromIndex);
		tviChildren.add(toIndex, droppedTviWmsLayer);

		// update viewer
		fMpViewer.remove(droppedTviWmsLayer);
		fMpViewer.insert(tviParent, droppedTviWmsLayer, toIndex);

		// reselect filter item
		fMpViewer.setSelection(new StructuredSelection(droppedTviWmsLayer));

		fDialogMPProfile.updateLiveView();

		return true;
	}

	@Override
	public boolean validateDrop(final Object target, final int operation, final TransferData transferType) {

		// disable auto expand for tree items which have children
		setExpandEnabled(false);

		final LocalSelectionTransfer localTransfer = LocalSelectionTransfer.getTransfer();

		if (localTransfer.isSupportedType(transferType) == false) {
			return false;
		}

		final ISelection selection = localTransfer.getSelection();
		if ((selection instanceof StructuredSelection) == false) {
			return false;
		}

		final Object draggedItem = ((StructuredSelection) selection).getFirstElement();

		// don't drop on itself
		if (target == draggedItem) {
			return false;
		}

		// check drop location
		final int location = getCurrentLocation();
		if ((location == LOCATION_AFTER || location == LOCATION_BEFORE) == false) {
			return false;
		}

		if (draggedItem instanceof TVIMapProvider && target instanceof TVIMapProvider) {

			// support the reorder of map providers

			return true;

		} else if (draggedItem instanceof TVIWmsLayer && target instanceof TVIWmsLayer) {

			// support the reorder of wms layer

			// check if the layers have the same parent
			final TreeViewerItem draggedParent = ((TVIWmsLayer) draggedItem).getParentItem();
			final TreeViewerItem targetParent = ((TVIWmsLayer) target).getParentItem();

			if (draggedParent == targetParent) {
				return true;
			}
		}

		return false;
	}

}
