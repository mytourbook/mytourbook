/*******************************************************************************
 * Copyright (C) 2005, 2010  Wolfgang Schramm and Contributors
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
package net.tourbook.ui.views;

import net.tourbook.ui.UI;

import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Monitor;
import org.eclipse.swt.widgets.Tree;

public class TreeViewerTourInfoToolTip extends ColumnViewerTourInfoToolTip {

	private Tree		_tree;

	private TreeViewer	_treeViewer;
	private ViewerCell	_treeCell;

	/**
	 * Tour info tooltip for a tree viewer
	 * 
	 * @param treeViewer
	 */
	public TreeViewerTourInfoToolTip(final TreeViewer treeViewer) {

		super(treeViewer.getTree(), NO_RECREATE);

		_treeViewer = treeViewer;
		_tree = treeViewer.getTree();
	}

	@Override
	protected void afterHideToolTip(final Event event) {

		super.afterHideToolTip(event);

		_treeCell = null;
	}

	/**
	 * @param tipSize
	 * @param ttTopLeft
	 *            Top/left location for the hovered area relativ to the display
	 * @param devXAdjusted
	 * @return
	 */
	private Point fixupDisplayBounds(final Point tipSize, final Point ttTopLeft, final int devXAdjusted) {

		Rectangle displayBounds;
		final Monitor[] allMonitors = _tree.getDisplay().getMonitors();

		if (allMonitors.length > 1) {

			// By default present in the monitor of the control
			displayBounds = _tree.getMonitor().getBounds();
			final Point topLeft2 = new Point(ttTopLeft.x, ttTopLeft.y);

			// Search on which monitor the event occurred
			Rectangle monitorBounds;
			for (final Monitor monitor : allMonitors) {
				monitorBounds = monitor.getBounds();
				if (monitorBounds.contains(topLeft2)) {
					displayBounds = monitorBounds;
					break;
				}
			}

		} else {
			displayBounds = _tree.getDisplay().getBounds();
		}

		final Point bottomRight = new Point(//
				ttTopLeft.x + tipSize.x,
				ttTopLeft.y + tipSize.y);

		if (!(displayBounds.contains(ttTopLeft) && displayBounds.contains(bottomRight))) {

			if (bottomRight.x > displayBounds.x + displayBounds.width) {
				ttTopLeft.x -= bottomRight.x - (displayBounds.x + displayBounds.width);
			}

			if (bottomRight.y > displayBounds.y + displayBounds.height) {
				ttTopLeft.y -= bottomRight.y - (displayBounds.y + displayBounds.height);
			}

			if (ttTopLeft.x < displayBounds.x) {
				ttTopLeft.x += displayBounds.x + devXAdjusted;
			}

			if (ttTopLeft.y < displayBounds.y) {
				ttTopLeft.y = displayBounds.y;
			}
		}

		return ttTopLeft;
	}

	@Override
	public Point getLocation(final Point tipSize, final Event event) {

		// try to position the tooltip at the bottom of the cell
		final ViewerCell cell = _treeViewer.getCell(new Point(event.x, event.y));

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

			return fixupDisplayBounds(tipSize, ttDisplayLocation, devX);
		}

		return super.getLocation(tipSize, event);
	}

	@Override
	protected Object getToolTipArea(final Event event) {

		// Ensure that the tooltip is hidden when the cell is left
		return _treeCell = _treeViewer.getCell(new Point(event.x, event.y));
	}

	@Override
	protected boolean shouldCreateToolTip(final Event event) {

		if (!super.shouldCreateToolTip(event)) {
			return false;
		}

		if (_treeCell == null) {
			return false;
		}

		Long tourId = null;
		final CellLabelProvider labelProvider = _treeViewer.getLabelProvider(_treeCell.getColumnIndex());

		if (labelProvider instanceof IColumnViewerTourIdProvider) {
			tourId = ((IColumnViewerTourIdProvider) labelProvider).getTourId(_treeCell);
		}

		setTourId(tourId);

		if (tourId == null) {
			// show default tooltip
			_tree.setToolTipText(null);
		} else {
			// hide default tooltip and display the custom tooltip
			_tree.setToolTipText(UI.EMPTY_STRING);
		}

		return tourId != null;
	}

}
