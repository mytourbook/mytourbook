/*******************************************************************************
 * Copyright (C) 2005, 2015 Wolfgang Schramm and Contributors
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
package net.tourbook.ui.tourChart;

import java.util.ArrayList;

import net.tourbook.chart.ChartComponentGraph;
import net.tourbook.common.UI;
import net.tourbook.common.tooltip.AnimatedToolTipShell2;
import net.tourbook.common.tooltip.IOpeningDialog;
import net.tourbook.common.util.IToolTipProvider;
import net.tourbook.data.TourData;
import net.tourbook.tour.TourManager;
import net.tourbook.ui.ITourProvider;

import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.ToolBar;

/**
 * created: 06.09.2015
 */
public class TourSegmenterTooltip extends AnimatedToolTipShell2 implements ITourProvider, IToolTipProvider,
		IOpeningDialog {

	private String				_dialogId	= getClass().getCanonicalName();

	private TourChart			_tourChart;
	private SegmenterSegment	_hoveredSegment;

	/*
	 * UI resources
	 */

	private Long				_hoveredTourId;

	public TourSegmenterTooltip(final TourChart tourChart) {

		super(tourChart);

		_tourChart = tourChart;

		setFadeInSteps(5);
		setFadeOutSteps(20);
		setFadeOutDelaySteps(10);

		setBehaviourOnMouseOver(MOUSE_OVER_BEHAVIOUR_IGNORE_OWNER);
	}

	@Override
	protected void beforeHideToolTip() {

		/*
		 * This is the tricky part that the hovered marker is reset before the tooltip is closed and
		 * not when nothing is hovered. This ensures that the tooltip has a valid state.
		 */
		_hoveredSegment = null;

	}

	@Override
	protected boolean canShowToolTip() {

		return _hoveredSegment != null;
	}

	@Override
	protected Composite createToolTipContentArea(final Composite shell) {

		if (_hoveredSegment == null) {
			return null;
		}

		shell.addDisposeListener(new DisposeListener() {
			@Override
			public void widgetDisposed(final DisposeEvent e) {
				onDispose();
			}
		});

		return createUI(shell);
	}

	private Composite createUI(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(1).applyTo(container);
		{

			final Label label = new Label(container, SWT.NONE);
			GridDataFactory.fillDefaults().applyTo(label);
			label.setText("TourSegmenterToolTip");

		}

		return container;
	}

	@Override
	public String getDialogId() {
		return _dialogId;
	}

	@Override
	public ArrayList<TourData> getSelectedTours() {

		final TourData tourData = TourManager.getInstance().getTourData(_hoveredTourId);

		final ArrayList<TourData> tours = new ArrayList<TourData>();
		tours.add(tourData);

		return tours;
	}

	/**
	 * By default the tooltip is located to the left side of the tour marker point, when not visible
	 * it is displayed to the right side of the tour marker point.
	 */
	@Override
	public Point getToolTipLocation(final Point tipSize) {

		final int devHoveredX = _hoveredSegment.devXTitle;
		int devHoveredY = _hoveredSegment.devYTitle;
		final int devHoveredWidth = _hoveredSegment.titleWidth;

		final int devYTop = _hoveredSegment.devYGraphTop;

		final int tipWidth = tipSize.x;
		final int tipHeight = tipSize.y;

		int ttPosX;
		int ttPosY;

		if (devHoveredY < devYTop) {
			// remove hovered size
			devHoveredY = devYTop;
		}

		// position tooltip above the chart
		ttPosX = devHoveredX + devHoveredWidth / 2 - tipWidth / 2;
		ttPosY = -tipHeight + 1;

		// ckeck if tooltip is left to the chart border
		if (ttPosX + tipWidth < 0) {

			// set tooltip to the graph left border
			ttPosX = -tipWidth - 1;

		} else if (ttPosX > _hoveredSegment.devGraphWidth) {

			// set tooltip to the graph right border
			ttPosX = _hoveredSegment.devGraphWidth;
		}

		final ChartComponentGraph graphControl = _tourChart.getChartComponents().getChartComponentGraph();
		final IToolBarManager iTbm = _tourChart.getToolBarManager();

		final ToolBarManager tbm = (ToolBarManager) iTbm;
		final ToolBar toolbarControl = tbm.getControl();

		/*
		 * Center horizontally in the middle of the tour segment and vertically above the toolbar
		 * that the tool buttons are not hidden from the tooltip.
		 */
		final Point ttLocationX = graphControl.toDisplay(ttPosX, ttPosY);
		final Point ttLocationY = toolbarControl.toDisplay(ttPosX, ttPosY);

		final Point ttLocation = new Point(ttLocationX.x, ttLocationY.y - 1);

		/*
		 * Fixup display bounds
		 */
		final Rectangle displayBounds = UI.getDisplayBounds(toolbarControl, ttLocation);
		final Point rightBottomBounds = new Point(tipSize.x + ttLocation.x, tipSize.y + ttLocation.y);

		final boolean isLocationInDisplay = displayBounds.contains(ttLocation);
		final boolean isBottomInDisplay = displayBounds.contains(rightBottomBounds);

		if (!(isLocationInDisplay && isBottomInDisplay)) {

			final int displayX = displayBounds.x;
			final int displayY = displayBounds.y;
			final int displayWidth = displayBounds.width;

			if (ttLocation.x < displayX) {
				ttLocation.x = displayX;
			}

			if (rightBottomBounds.x > displayX + displayWidth) {
				ttLocation.x = displayWidth - tipWidth;
			}

			if (ttLocation.y < displayY) {
				// position evaluated with try and error until it fits
				ttLocation.y = ttLocationX.y - ttPosY + graphControl.getSize().y;
			}
		}

		return ttLocation;
	}

	@Override
	public void hide() {

		// reset state
		_hoveredSegment = null;
		_hoveredTourId = null;

		super.hide();
	}

	@Override
	public void hideDialog() {
		hideNow();
	}

	@Override
	public void hideToolTip() {
		hideNow();
	}

	@Override
	protected boolean isInNoHideArea(final Point displayCursorLocation) {

		if (_hoveredSegment == null) {

			return false;

		} else {

			return _hoveredSegment.isInNoHideArea(
					_tourChart.getChartComponents().getChartComponentGraph(),
					displayCursorLocation);
		}
	}

	private void onDispose() {

	}

	void open(final SegmenterSegment hoveredSegment) {

		boolean isKeepOpened = false;

		if (hoveredSegment != null && isTooltipClosing()) {

			/**
			 * This case occures when the tooltip is opened but is currently closing and the mouse
			 * is moved from the tooltip back to the hovered label.
			 * <p>
			 * This prevents that when the mouse is over the hovered label but not moved, that the
			 * tooltip keeps opened.
			 */
			isKeepOpened = true;
		}

		if (hoveredSegment == _hoveredSegment && isKeepOpened == false) {

			// nothing has changed

			return;
		}

		if (hoveredSegment == null) {

			// a segment is not hovered or is hidden, hide tooltip

			hide();

		} else {

			// another marker is hovered, show tooltip

			_hoveredSegment = hoveredSegment;
			_hoveredTourId = hoveredSegment.tourId;

//			showToolTip();
		}

		System.out.println((UI.timeStampNano() + " [" + getClass().getSimpleName() + "] ")
				+ ("\t_hoveredTourId: " + _hoveredTourId)
				+ ("\thoveredSegment: " + hoveredSegment)
				+ ("\t_hoveredSegment: " + _hoveredSegment)
		//
				);
		// TODO remove SYSTEM.OUT.PRINTLN
	}

}
