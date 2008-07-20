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
