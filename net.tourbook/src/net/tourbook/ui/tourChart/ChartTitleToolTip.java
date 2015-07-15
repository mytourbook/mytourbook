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

import net.tourbook.Messages;
import net.tourbook.chart.ChartComponentGraph;
import net.tourbook.chart.ChartTitle;
import net.tourbook.common.UI;
import net.tourbook.common.tooltip.AnimatedToolTipShell;
import net.tourbook.common.util.IToolTipProvider;
import net.tourbook.data.TourData;
import net.tourbook.tour.TourInfoUI;
import net.tourbook.tour.TourManager;
import net.tourbook.ui.ITourProvider;

import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;

/**
 * created: 13.07.2015
 */
public class ChartTitleToolTip extends AnimatedToolTipShell implements ITourProvider, IToolTipProvider {

	/**
	 * Visual position for marker tooltip, they must correspond to the position id
	 * TOOLTIP_POSITION_*.
	 */
	public static final String[]	TOOLTIP_POSITIONS;

	static {

		TOOLTIP_POSITIONS = new String[] { //
		//
			Messages.Tour_Marker_TooltipPosition_Left, // 				0
			Messages.Tour_Marker_TooltipPosition_Right, // 				1
			Messages.Tour_Marker_TooltipPosition_Top, // 				2
			Messages.Tour_Marker_TooltipPosition_Bottom, // 			3
			Messages.Tour_Marker_TooltipPosition_ChartTop, // 			4
			Messages.Tour_Marker_TooltipPosition_ChartBottom, // 		5
		};
	}

	private static final int		TOOLTIP_POSITION_LEFT			= 0;
	private static final int		TOOLTIP_POSITION_RIGHT			= 1;
	private static final int		TOOLTIP_POSITION_ABOVE			= 2;
	private static final int		TOOLTIP_POSITION_BELOW			= 3;
	private static final int		TOOLTIP_POSITION_CHART_TOP		= 4;
	private static final int		TOOLTIP_POSITION_CHART_BOTTOM	= 5;

	public static final int			DEFAULT_TOOLTIP_POSITION		= TOOLTIP_POSITION_BELOW;

	private TourChart				_tourChart;

	private ChartTitle				_hoveredTitle;

	/**
	 * When <code>true</code> the actions are displayed, e.g. to open the marker dialog.
	 */
	private boolean					_isShowActions;

//	private ChartMarkerConfig		_cmc;

	/*
	 * UI resources
	 */
	private final TourInfoUI		_tourInfoUI						= new TourInfoUI();

	public ChartTitleToolTip(final TourChart tourChart) {

		super(tourChart);

		_tourChart = tourChart;

		setReceiveMouseMoveEvent(true);
//		setIsAnimateLocation(false);

		setFadeInSteps(10);
		setFadeOutSteps(20);
		setFadeOutDelaySteps(10);
	}

	@Override
	protected void beforeHideToolTip() {

		/*
		 * This is the tricky part that the hovered marker is reset before the tooltip is closed and
		 * not when nothing is hovered. This ensures that the tooltip has a valid state.
		 */
		_hoveredTitle = null;
	}

	@Override
	protected boolean canShowToolTip() {

		return _hoveredTitle != null;
	}

	@Override
	protected Composite createToolTipContentArea(final Composite shell) {

		if (_hoveredTitle == null) {
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

		Composite ui;

		final TourData tourData = TourManager.getInstance().getTourData(_hoveredTitle.tourId);

		if (tourData == null) {

			// there are no data available

			ui = _tourInfoUI.createUI_NoData(parent);

		} else {

			// tour data is available

			ui = _tourInfoUI.createContentArea(parent, tourData, this, this);

			// allow the actions to be selected
//			if (_tourToolTip != null) {
//				_tourToolTip.setHideOnMouseDown(false);
//			}
		}
		return ui;
	}

	@Override
	public ArrayList<TourData> getSelectedTours() {

		final ArrayList<TourData> tours = new ArrayList<TourData>();
		tours.add(_tourChart.getTourData());

		return tours;
	}

	/**
	 * By default the tooltip is located to the left side of the tour marker point, when not visible
	 * it is displayed to the right side of the tour marker point.
	 */
	@Override
	public Point getToolTipLocation(final Point tipSize) {

		final int devHoveredX = _hoveredTitle.devX;
		int devHoveredY = _hoveredTitle.devY;
		final int devHoveredWidth = _hoveredTitle.width;
		final int devHoveredHeight = _hoveredTitle.height;
//		final int devHoverSize = _hoveredTitle.devHoverSize;

		final int devYTop = _hoveredTitle.devYTop;
		final int devYBottom = _hoveredTitle.devYBottom;

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
		ttPosY = 0 - tipHeight + 1;

		// ckeck if tooltip is left to the chart border
		if (ttPosX + tipWidth < 0) {

			// set tooltip to the graph left border
			ttPosX = -tipWidth - 1;

		} else if (ttPosX > _hoveredTitle.devGraphWidth) {

			// set tooltip to the graph right border
			ttPosX = _hoveredTitle.devGraphWidth;
		}

		// check display bounds
		final ChartComponentGraph chartComponentGraph = _tourChart.getChartComponents().getChartComponentGraph();
//		final Point dispPos = chartComponentGraph.toDisplay(ttPosX, ttPosY);

//		if (dispPos.x < 0) {
//
//			// tooltip is outside of the display, set tooltip to the right of the tour marker
//			ttPosX = devHoveredRight + 1;
//		}

		final Point graphLocation = chartComponentGraph.toDisplay(0, 0);
		final Point ttLocation = chartComponentGraph.toDisplay(ttPosX, ttPosY);

		/*
		 * Fixup display bounds
		 */
		final Rectangle displayBounds = UI.getDisplayBounds(chartComponentGraph, ttLocation);
		final Point rightBottomBounds = new Point(tipSize.x + ttLocation.x, tipSize.y + ttLocation.y);

		if (!(displayBounds.contains(ttLocation) && displayBounds.contains(rightBottomBounds))) {

			final int displayX = displayBounds.x;
			final int displayY = displayBounds.y;
			final int displayWidth = displayBounds.width;
			final int displayHeight = displayBounds.height;

			if (ttLocation.x < displayX) {
				ttLocation.x = displayX;
			}

			if (rightBottomBounds.x > displayX + displayWidth) {
				ttLocation.x = displayWidth - tipWidth;
			}

			if (ttLocation.y < displayY) {
				ttLocation.y = graphLocation.y + devHoveredY + devHoveredHeight /*- devHoverSize */+ 2;
			}
		}

		return ttLocation;
	}

	@Override
	public void hideToolTip() {
		hideNow();
	}

	private void onDispose() {
		_tourInfoUI.dispose();
	}

	@Override
	protected void onMouseMoveInToolTip(final MouseEvent mouseEvent) {

		/*
		 * When in tooltip, the hovered label state is not displayed, keep it displayed
		 */
//		final ChartLayerMarker markerLayer = _tourChart.getLayerTourMarker();
//		markerLayer.setTooltipLabel(_hoveredTitle);
	}

	void open(final ChartTitle hoveredTitle) {

		boolean isKeepOpened = false;

		if (hoveredTitle != null && isTooltipClosing()) {

			/**
			 * This case occures when the tooltip is opened but is currently closing and the mouse
			 * is moved from the tooltip back to the hovered label.
			 * <p>
			 * This prevents that when the mouse is over the hovered label but not moved, that the
			 * tooltip keeps opened.
			 */
			isKeepOpened = true;
		}

		if (hoveredTitle == _hoveredTitle && isKeepOpened == false) {
			// nothing has changed
			return;
		}

		if (hoveredTitle == null) {

			// a marker is not hovered or is hidden, hide tooltip

			hide();

		} else {

			// another marker is hovered, show tooltip

			_hoveredTitle = hoveredTitle;

			showToolTip();
		}
	}

}
