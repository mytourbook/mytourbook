/*******************************************************************************
 * Copyright (C) 2005, 2013  Wolfgang Schramm and Contributors
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

import net.tourbook.common.tooltip.ToolTip3;

import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.ui.dialogs.ContainerCheckedTreeViewer;

public class PropertyViewerToolTip extends ToolTip3 {

	private ContainerCheckedTreeViewer	_propViewer;
	private Tree						_tree;

	public PropertyViewerToolTip(final ContainerCheckedTreeViewer propViewer) {

		super(propViewer.getTree());

		_propViewer = propViewer;
		_tree = propViewer.getTree();
	}

	@Override
	public Point getLocation(final Point tipSize, final Event event) {

		// try to position the tooltip at the bottom of the cell
		final ViewerCell cell = _propViewer.getCell(new Point(event.x, event.y));

		if (cell != null) {

			final Rectangle displayBounds = _tree.getDisplay().getBounds();

			final Rectangle cellBounds = cell.getBounds();
			final int cellWidth2 = cellBounds.width / 2;
			final int cellHeight = cellBounds.height;

			final int devXDefault = cellBounds.x + cellWidth2;// + cellBounds.width; //event.x;
			final int devY = cellBounds.y + cellHeight;

			/*
			 * check if the tooltip is outside of the tree, this can happen when the column is very
			 * wide and partly hidden
			 */
			final Rectangle treeBounds = _tree.getBounds();
			boolean isDevXAdjusted = false;
			int devX = devXDefault;

			if (devXDefault >= treeBounds.width) {
				devX = treeBounds.width - 40;
				isDevXAdjusted = true;
			}

			Point ttDisplayLocation = _tree.toDisplay(devX, devY);
			final int tipSizeWidth = tipSize.x;
			final int tipSizeHeight = tipSize.y;

			if (ttDisplayLocation.x + tipSizeWidth > displayBounds.width) {

				/*
				 * adjust horizontal position, it is outside of the display, prevent default
				 * repositioning
				 */

				if (isDevXAdjusted) {

					ttDisplayLocation = _tree.toDisplay(devXDefault - cellWidth2 + 20 - tipSizeWidth, devY);

				} else {
					ttDisplayLocation.x = ttDisplayLocation.x - tipSizeWidth;
				}
			}

			if (ttDisplayLocation.y + tipSizeHeight > displayBounds.height) {

				/*
				 * adjust vertical position, it is outside of the display, prevent default
				 * repositioning
				 */

				ttDisplayLocation.y = ttDisplayLocation.y - tipSizeHeight - cellHeight;
			}

			return fixupDisplayBounds(tipSize, ttDisplayLocation);
		}

		return super.getLocation(tipSize, event);
	}
}
