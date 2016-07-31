/*******************************************************************************
 * Copyright (C) 2005, 2016 Wolfgang Schramm and Contributors
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
package net.tourbook.chart;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;

/**
 * Chart tooltip until version 11.7
 */
public class ToolTipV1 {

	/**
	 * maximum width in pixel for the width of the tooltip
	 */
	private static final int	MAX_TOOLTIP_WIDTH	= 500;

	private Chart				_chart;
	private Shell				_toolTipShell;
	private Composite			_toolTipContainer;
	private Label				_toolTipTitle;
	private Label				_toolTipLabel;

	private ChartToolTipInfo	_toolTipInfo;

	private Listener			_toolTipListener;
	private final int[]			_toolTipEvents		= new int[] {
			SWT.MouseExit,
			SWT.MouseHover,
			SWT.MouseMove,
			SWT.MouseDown,
			SWT.DragDetect							};

	private int					_hoveredBarSerieIndex;
	private int					_hoveredBarValueIndex;

	public ToolTipV1(final Chart chart) {

		_chart = chart;

		_toolTipListener = new Listener() {
			@Override
			public void handleEvent(final Event event) {
				switch (event.type) {
				case SWT.MouseHover:
				case SWT.MouseMove:
					if (toolTip_30_Update(event.x, event.y)) {
						break;
					}
					// FALL THROUGH
				case SWT.MouseExit:
				case SWT.MouseDown:
					toolTip_20_Hide();
					break;
				}
			}
		};
	}

	void dispose() {

		// dispose tooltip
		if (_toolTipShell != null) {
			toolTip_20_Hide();
			for (final int toolTipEvent : _toolTipEvents) {
				_chart.removeListener(toolTipEvent, _toolTipListener);
			}
			_toolTipShell.dispose();
			_toolTipShell = null;
			_toolTipContainer = null;
		}

	}

	void toolTip_10_Show(final int x, final int y, final int hoveredBarSerieIndex, final int hoveredBarValueIndex) {

		_hoveredBarSerieIndex = hoveredBarSerieIndex;
		_hoveredBarValueIndex = hoveredBarValueIndex;

		if (_toolTipShell == null) {

			_toolTipShell = new Shell(_chart.getShell(), SWT.ON_TOP | SWT.TOOL);

			final Display display = _toolTipShell.getDisplay();
			final Color infoColorBackground = display.getSystemColor(SWT.COLOR_INFO_BACKGROUND);
			final Color infoColorForeground = display.getSystemColor(SWT.COLOR_INFO_FOREGROUND);

			_toolTipContainer = new Composite(_toolTipShell, SWT.NONE);
			GridLayoutFactory.fillDefaults().extendedMargins(2, 5, 2, 3).applyTo(_toolTipContainer);
			_toolTipContainer.setBackground(infoColorBackground);
			_toolTipContainer.setForeground(infoColorForeground);
			{
				_toolTipTitle = new Label(_toolTipContainer, SWT.LEAD);
				_toolTipTitle.setBackground(infoColorBackground);
				_toolTipTitle.setForeground(infoColorForeground);
				_toolTipTitle.setFont(JFaceResources.getFontRegistry().getBold(JFaceResources.DIALOG_FONT));

				_toolTipLabel = new Label(_toolTipContainer, SWT.LEAD | SWT.WRAP);
				_toolTipLabel.setBackground(infoColorBackground);
				_toolTipLabel.setForeground(infoColorForeground);
			}

			for (final int toolTipEvent : _toolTipEvents) {
				_chart.addListener(toolTipEvent, _toolTipListener);
			}
		}

		if (toolTip_30_Update(x, y)) {
			_toolTipShell.setVisible(true);
		} else {
			toolTip_20_Hide();
		}
	}

	void toolTip_20_Hide() {

		if (_toolTipShell == null || _toolTipShell.isDisposed()) {
			return;
		}

		if (_toolTipShell.isVisible()) {

			/*
			 * when hiding the tooltip, reposition the tooltip the next time when the tool tip is
			 * displayed
			 */
			_toolTipInfo.setReposition(true);

			_toolTipShell.setVisible(false);
		}
	}

	private boolean toolTip_30_Update(final int x, final int y) {

		final ChartToolTipInfo tooltipInfo = toolTip_40_GetInfo(x, y);

		if (tooltipInfo == null) {
			return false;
		}

		final String toolTipLabel = tooltipInfo.getLabel();
		final String toolTipTitle = tooltipInfo.getTitle();

		// check if the content has changed
		if (toolTipLabel.trim().equals(_toolTipLabel.getText().trim())
				&& toolTipTitle.trim().equals(_toolTipTitle.getText().trim())) {
			return true;
		}

		// title
		if (toolTipTitle != null) {
			_toolTipTitle.setText(toolTipTitle);
			_toolTipTitle.pack(true);
			_toolTipTitle.setVisible(true);
		} else {
			_toolTipTitle.setVisible(false);
		}

		// label
		_toolTipLabel.setText(toolTipLabel);
		GridDataFactory.fillDefaults().hint(SWT.DEFAULT, SWT.DEFAULT).applyTo(_toolTipLabel);
		_toolTipLabel.pack(true);

		/*
		 * adjust width of the tooltip when it exeeds the maximum
		 */
		Point containerSize = _toolTipContainer.computeSize(SWT.DEFAULT, SWT.DEFAULT, true);
		if (containerSize.x > MAX_TOOLTIP_WIDTH) {

			GridDataFactory.fillDefaults().hint(MAX_TOOLTIP_WIDTH, SWT.DEFAULT).applyTo(_toolTipLabel);
			_toolTipLabel.pack(true);

			containerSize = _toolTipContainer.computeSize(SWT.DEFAULT, SWT.DEFAULT, true);
		}

		_toolTipContainer.setSize(containerSize);
		_toolTipShell.pack(true);

		/*
		 * On some platforms, there is a minimum size for a shell which may be greater than the
		 * label size. To avoid having the background of the tip shell showing around the label,
		 * force the label to fill the entire client area.
		 */
		final Rectangle area = _toolTipShell.getClientArea();
		_toolTipContainer.setSize(area.width, area.height);

		toolTip_50_SetPosition();

		return true;
	}

	private ChartToolTipInfo toolTip_40_GetInfo(final int x, final int y) {

		if (_hoveredBarSerieIndex != -1) {

			// get the method which computes the bar info
			final IChartInfoProvider toolTipInfoProvider = (IChartInfoProvider) _chart
					.getChartDataModel()
					.getCustomData(ChartDataModel.BAR_TOOLTIP_INFO_PROVIDER);

			if (toolTipInfoProvider != null) {

				_toolTipInfo = toolTipInfoProvider.getToolTipInfo(_hoveredBarSerieIndex, _hoveredBarValueIndex);

				return _toolTipInfo;
			}
		}

		return null;
	}

	/**
	 * Position the tooltip and ensure that it is not located off the screen.
	 */
	private void toolTip_50_SetPosition() {

		final Point cursorLocation = _chart.getDisplay().getCursorLocation();

		// Assuming cursor is 21x21 because this is the size of
		// the arrow cursor on Windows
		final int cursorHeight = 21;

		final Point tooltipSize = _toolTipShell.getSize();
		final Rectangle monitorRect = _chart.getMonitor().getBounds();
		final Point pt = new Point(cursorLocation.x, cursorLocation.y + cursorHeight + 2);

		pt.x = Math.max(pt.x, monitorRect.x);
		if (pt.x + tooltipSize.x > monitorRect.x + monitorRect.width) {
			pt.x = monitorRect.x + monitorRect.width - tooltipSize.x;
		}
		if (pt.y + tooltipSize.y > monitorRect.y + monitorRect.height) {
			pt.y = cursorLocation.y - 2 - tooltipSize.y;
		}

		_toolTipShell.setLocation(pt);
	}

	/**
	 * Check if the tooltip is too far away from the cursor position
	 * 
	 * @return Returns <code>true</code> when the cursor is too far away
	 */
	private boolean toolTip_60_IsWrongPositioned() {

		final Point cursorLocation = _chart.getDisplay().getCursorLocation();
		final Point toolTipLocation = _toolTipShell.getLocation();

		final int cursorAreaLength = 50;

		final Rectangle cursorArea = new Rectangle(cursorLocation.x - cursorAreaLength, cursorLocation.y
				- cursorAreaLength, 2 * cursorAreaLength, 2 * cursorAreaLength);

		if (cursorArea.contains(toolTipLocation)) {
			return false;
		} else {
			return true;
		}
	}
}
