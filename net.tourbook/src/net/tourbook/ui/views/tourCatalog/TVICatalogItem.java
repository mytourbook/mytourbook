package net.tourbook.ui.views.tourCatalog;

import net.tourbook.tour.TreeViewerItem;

public abstract class TVICatalogItem extends TreeViewerItem {

	public TVICatalogItem() {}

//	/**
//	 * @return Returns the viewer which is defined in the root item
//	 */
//	public ColumnViewer getTourViewer() {
//
//		TreeViewerItem parentItem = this;
//		int maxLoops = 0;
//
//		while (true) {
//
//			if (parentItem instanceof TVICatalogRootItem) {
//				final TVICatalogRootItem rootItem = (TVICatalogRootItem) parentItem;
//				return rootItem.getRootTourViewer().getViewer();
//			}
//			
//			parentItem = getParentItem();
//
//			// prevent endless loops
//			if (maxLoops++ > 10) {
//				break;
//			}
//		}
//
//		return null;
//	}
}
