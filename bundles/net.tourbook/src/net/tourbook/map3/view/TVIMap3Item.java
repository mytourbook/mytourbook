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
package net.tourbook.map3.view;

import net.tourbook.common.UI;
import net.tourbook.common.util.TreeViewerItem;

import org.eclipse.ui.dialogs.ContainerCheckedTreeViewer;

public abstract class TVIMap3Item extends TreeViewerItem {

	public String	name	= UI.EMPTY_STRING;

	public TVIMap3Item() {}

	/**
	 * @return Returns the tree viewer for the tree items, the viewer is set in the root item of the
	 *         tree viewer.
	 */
	ContainerCheckedTreeViewer getTreeItemViewer() {

		int endlessCounter = 0;

		while (true) {

			final TreeViewerItem parent = getParentItem();

			if (parent instanceof TVIMap3Root) {
				final TVIMap3Root map3Root = (TVIMap3Root) parent;

				return map3Root.getTreeViewer();
			}

			if (endlessCounter++ > 10) {
				return null;
			}
		}
	}

	@Override
	public String toString() {
		return "TVIMap3Item [name=" + name + "]"; //$NON-NLS-1$ //$NON-NLS-2$
	}

}
