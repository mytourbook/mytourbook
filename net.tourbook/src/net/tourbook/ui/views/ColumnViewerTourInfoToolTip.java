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

import java.util.ArrayList;

import net.tourbook.common.util.ITourToolTipProvider;
import net.tourbook.common.util.ToolTip;
import net.tourbook.common.util.TourToolTip;
import net.tourbook.data.TourData;
import net.tourbook.tour.TourInfoUI;
import net.tourbook.tour.TourManager;
import net.tourbook.ui.ITourProvider;
import net.tourbook.ui.UI;

import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Monitor;

/**
 * Tour info tooltip, implemented custom tooltip similar like
 * {@link org.eclipse.ui.internal.dialogs.CustomizePerspectiveDialog} and
 * {@link org.eclipse.jface.viewers.ColumnViewerToolTipSupport}
 */
@SuppressWarnings("restriction")
public abstract class ColumnViewerTourInfoToolTip extends ToolTip implements ITourProvider, ITourToolTipProvider {

	private final TourInfoUI	_tourInfoUI	= new TourInfoUI();

	private Long				_tourId;
	private TourData			_tourData;

	private Control				_ttControl;

	private ColumnViewer		_columnViewer;
	private ViewerCell			_viewerCell;

	public ColumnViewerTourInfoToolTip(final Control control, final int style, final ColumnViewer columnViewer) {

		super(control, style, false);

		_ttControl = control;
		_columnViewer = columnViewer;

		setHideOnMouseDown(false);
	}

	@Override
	public void afterHideToolTip() {
		// not used
	}

	@Override
	protected void afterHideToolTip(final Event event) {

		super.afterHideToolTip(event);

		_viewerCell = null;
	}

	@Override
	public Composite createToolTipContentArea(final Event event, final Composite parent) {

		Composite container;

		if (_tourId != null && _tourId != -1) {
			// first get data from the tour id when it is set
			_tourData = TourManager.getInstance().getTourData(_tourId);
		}

		if (_tourData == null) {

			// there are no data available

			container = _tourInfoUI.createUI_NoData(parent);

			// allow the actions to be selected
			setHideOnMouseDown(true);

		} else {

			// tour data is available

			container = _tourInfoUI.createContentArea(parent, _tourData, this, this);

			_tourInfoUI.setActionsEnabled(true);

			// allow the actions to be selected
			setHideOnMouseDown(false);
		}

		parent.addDisposeListener(new DisposeListener() {
			@Override
			public void widgetDisposed(final DisposeEvent e) {
				_tourInfoUI.dispose();
			}
		});

		return container;
	}

	/**
	 * @param tipSize
	 * @param ttTopLeft
	 *            Top/left location for the hovered area relativ to the display
	 * @param devXAdjusted
	 * @return
	 */
	protected Point fixupDisplayBounds(final Point tipSize, final Point ttTopLeft) {

		Rectangle displayBounds;
		final Monitor[] allMonitors = _ttControl.getDisplay().getMonitors();

		if (allMonitors.length > 1) {

			// By default present in the monitor of the control
			displayBounds = _ttControl.getMonitor().getBounds();
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
			displayBounds = _ttControl.getDisplay().getBounds();
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
				ttTopLeft.x += displayBounds.x;
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
		final ViewerCell cell = _columnViewer.getCell(new Point(event.x, event.y));

		if (cell != null) {

			final Rectangle displayBounds = _ttControl.getDisplay().getBounds();

			final Rectangle cellBounds = cell.getBounds();
			final int cellWidth2 = cellBounds.width / 2;
			final int cellHeight = cellBounds.height;

			final int devXDefault = cellBounds.x + cellWidth2;// + cellBounds.width; //event.x;
			final int devY = cellBounds.y + cellHeight;

			/*
			 * check if the tooltip is outside of the tree, this can happen when the column is very
			 * wide and partly hidden
			 */
			final Rectangle treeBounds = _ttControl.getBounds();
			boolean isDevXAdjusted = false;
			int devX = devXDefault;

			if (devXDefault >= treeBounds.width) {
				devX = treeBounds.width - 40;
				isDevXAdjusted = true;
			}

			Point ttDisplayLocation = _ttControl.toDisplay(devX, devY);
			final int tipSizeWidth = tipSize.x;
			final int tipSizeHeight = tipSize.y;

			if (ttDisplayLocation.x + tipSizeWidth > displayBounds.width) {

				/*
				 * adjust horizontal position, it is outside of the display, prevent default
				 * repositioning
				 */

				if (isDevXAdjusted) {

					ttDisplayLocation = _ttControl.toDisplay(devXDefault - cellWidth2 + 20 - tipSizeWidth, devY);

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

	@Override
	public ArrayList<TourData> getSelectedTours() {

		if (_tourData == null) {
			return null;
		}

		final ArrayList<TourData> list = new ArrayList<TourData>();
		list.add(_tourData);

		return list;
	}

	@Override
	protected Object getToolTipArea(final Event event) {

		// Ensure that the tooltip is hidden when the cell is left
		return _viewerCell = _columnViewer.getCell(new Point(event.x, event.y));
	}

	@Override
	public void paint(final GC gc, final Rectangle clientArea) {
		// not used
	}

	@Override
	public boolean setHoveredLocation(final int x, final int y) {
		// not used
		return false;
	}

	protected void setTourId(final Long tourId) {
		_tourId = tourId;
	}

	@Override
	public void setTourToolTip(final TourToolTip tourToolTip) {
		// not used
	}

	@Override
	protected boolean shouldCreateToolTip(final Event event) {

		if (!super.shouldCreateToolTip(event)) {
			return false;
		}

		if (_viewerCell == null) {
			return false;
		}

		/*
		 * get tour id from hovered cell label provider
		 */
		Long tourId = null;
		final CellLabelProvider labelProvider = _columnViewer.getLabelProvider(_viewerCell.getColumnIndex());

		if (labelProvider instanceof IColumnViewerTourIdProvider) {
			tourId = ((IColumnViewerTourIdProvider) labelProvider).getTourId(_viewerCell);
		}

		setTourId(tourId);

		if (tourId == null) {
			// show default tooltip
			_ttControl.setToolTipText(null);
		} else {
			// hide default tooltip and display the custom tooltip
			_ttControl.setToolTipText(UI.EMPTY_STRING);
		}

		return tourId != null;
	}
}
