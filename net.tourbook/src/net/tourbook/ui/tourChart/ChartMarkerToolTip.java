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
package net.tourbook.ui.tourChart;

import net.tourbook.chart.ColorCache;
import net.tourbook.common.tooltip.AnimatedToolTipShell;
import net.tourbook.data.TourMarker;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;

import de.byteholder.geoclipse.util.Util;

/**
 * created: 30.5.2014
 */
public class ChartMarkerToolTip extends AnimatedToolTipShell {

	private static final int	DEFAULT_TEXT_WIDTH	= 50;
	private static final int	DEFAULT_TEXT_HEIGHT	= 20;

	private PixelConverter		_pc;

	private int					_defaultTextWidth;
	private int					_defaultTextHeight;

	private TourChart			_tourChart;

	private ChartLabel			_hoveredLabel;
	private TourMarker			_hoveredTourMarker;

	/*
	 * UI resources
	 */
	private final ColorCache	_colorCache			= new ColorCache();
	private Color				_fgBorder;
	private Color				_fgColor;
	private Color				_bgColor;

	/*
	 * UI controls
	 */
	private Composite			_shellContainer;

	public ChartMarkerToolTip(final TourChart tourChart) {

		super(tourChart);

		_tourChart = tourChart;

		setReceiveMouseMoveEvent(true);
		setIsShowShellTrimStyle(false);
		setIsAnimateLocation(false);
	}

	@Override
	protected void beforeHideToolTip() {

		_hoveredLabel = null;
		_hoveredTourMarker = null;
	}

	@Override
	protected boolean canShowToolTip() {

		return _hoveredLabel != null;
	}

	private void checkTextControlSize(final Composite parent, final Text txtControl, final String text) {

		Point defaultSize = txtControl.computeSize(SWT.DEFAULT, SWT.DEFAULT);

		// check default width
		if (defaultSize.x > _defaultTextWidth) {

			// check default height
			defaultSize = txtControl.computeSize(_defaultTextWidth, SWT.DEFAULT);
			if (defaultSize.y > _defaultTextHeight) {

				checkTextControlSize_RecreateWithVScroll(parent, txtControl, text, _defaultTextWidth);

			} else {

				// limit width
				final GridData gd = (GridData) txtControl.getLayoutData();
				gd.widthHint = _defaultTextWidth;
			}

		} else if (defaultSize.y > _defaultTextHeight) {

			checkTextControlSize_RecreateWithVScroll(parent, txtControl, text, SWT.DEFAULT);
		}
	}

	/**
	 * Recreate text control with vertical scrollbar and limited height.
	 * 
	 * @param parent
	 * @param txtControl
	 * @param text
	 * @param widthHint
	 */
	private void checkTextControlSize_RecreateWithVScroll(	final Composite parent,
															Text txtControl,
															final String text,
															final int widthHint) {

		txtControl.dispose();

		txtControl = new Text(parent, SWT.WRAP | SWT.V_SCROLL);
		GridDataFactory.fillDefaults()//
				.hint(widthHint, _defaultTextHeight)
				.applyTo(txtControl);

		txtControl.setText(text);
	}

	@Override
	protected Composite createToolTipContentArea(final Composite shell) {

		setFadeInSteps(1);
		setFadeOutSteps(10);
		setFadeOutDelaySteps(20);

		if (_hoveredLabel == null) {
			return null;
		}

		_pc = new PixelConverter(shell);
		_defaultTextWidth = _pc.convertWidthInCharsToPixels(DEFAULT_TEXT_WIDTH);
		_defaultTextHeight = _pc.convertHeightInCharsToPixels(DEFAULT_TEXT_HEIGHT);

		shell.addDisposeListener(new DisposeListener() {
			@Override
			public void widgetDisposed(final DisposeEvent e) {
				onDispose();
			}
		});

		final Composite container = createUI(shell);

		return container;
	}

	private Composite createUI(final Composite shell) {

		initUI(shell);

		/*
		 * shell container is necessary because the margins of the inner container will hide the
		 * tooltip when the mouse is hovered, which is not as it should be.
		 */
		_shellContainer = new Composite(shell, SWT.NONE);
		GridLayoutFactory.fillDefaults()//
//				.spacing(0, 0)
//				.numColumns(2)
				// set margin to draw the border
				.extendedMargins(1, 1, 1, 1)
				.applyTo(_shellContainer);
		_shellContainer.setForeground(_fgColor);
		_shellContainer.setBackground(_bgColor);
		_shellContainer.addPaintListener(new PaintListener() {
			@Override
			public void paintControl(final PaintEvent e) {
				onPaintShellContainer(e);
			}
		});
		{
			createUI_10_Content(_shellContainer);
		}

		return _shellContainer;
	}

	private void createUI_10_Content(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridLayoutFactory.fillDefaults()//
				.extendedMargins(5, 5, 5, 5)
				.spacing(3, 1)
				.numColumns(1)
				.applyTo(container);
//			container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_GREEN));
		{
			/*
			 * Name
			 */
			final String markerName = _hoveredTourMarker.getLabel();
			if (markerName.length() > 0) {

				final Label lblName = new Label(container, SWT.NONE);
				GridDataFactory.fillDefaults().applyTo(lblName);

				lblName.setText(markerName);
			}

			/*
			 * Description
			 */
			final String markerDescription = _hoveredTourMarker.getDescription();
			if (markerDescription.length() > 0) {

				final Text txtDescription = new Text(container, SWT.WRAP);
				GridDataFactory.fillDefaults().applyTo(txtDescription);

				txtDescription.setText(markerDescription);

				checkTextControlSize(container, txtDescription, markerDescription);
			}

			/*
			 * Url
			 */
			final String urlText = _hoveredTourMarker.getUrlText();
			final String urlAddress = _hoveredTourMarker.getUrlAddress();
			final boolean isText = urlText.length() > 0;
			final boolean isAddress = urlAddress.length() > 0;

			if (isText || isAddress) {

				final Link linkUrl = new Link(container, SWT.NONE);
				GridDataFactory.fillDefaults().applyTo(linkUrl);

				linkUrl.addListener(SWT.Selection, new Listener() {
					public void handleEvent(final Event event) {
						onSelectUrl(event.text);
					}
				});

				String linkText;

				if (isAddress == false) {

					// only text is in the link -> this is not a internet address but create a link of it

					linkText = "<a href=\"" + urlText + "\">" + urlText + "</a>";

				} else if (isText == false) {

					linkText = "<a href=\"" + urlAddress + "\">" + urlAddress + "</a>";

				} else {

					linkText = "<a href=\"" + urlAddress + "\">" + urlText + "</a>";
				}

				linkUrl.setText(linkText);

				setTextWidth(linkUrl, linkText);
			}
		}
	}

	/**
	 * This is copied from {@link ChartLayerMarker#drawOverlay()}.
	 * <p>
	 * The region which is computed there cannot be used because the overlay is painted <b>after</b>
	 * the tooltip is displayed and <b>not before.</b>
	 */
	private Rectangle getHoveredRect() {

		final int devMarkerPointSizeRaw = _hoveredLabel.devMarkerPointSize;
		final int devLabelWidthRaw = _hoveredLabel.devLabelWidth;
		final int hoverSize = _hoveredLabel.devHoverSize;

		int devMarkerPointSize = devMarkerPointSizeRaw;
		if (devMarkerPointSize < 1) {
			devMarkerPointSize = 1;
		}

		final int devLabelX = _hoveredLabel.devXLabel - hoverSize;
		final int devLabelY = _hoveredLabel.devYLabel - hoverSize;
		final int devLabelWidth = devLabelWidthRaw + 2 * hoverSize;
		final int devLabelHeight = _hoveredLabel.devLabelHeight + 2 * hoverSize;

		final int devMarkerX = _hoveredLabel.devXMarker - hoverSize;
		final int devMarkerY = _hoveredLabel.devYMarker - hoverSize;
		final int devMarkerSize = devMarkerPointSize + 2 * hoverSize;

		Rectangle rectMarker = null;
		Rectangle rectLabel = null;
		Rectangle rectHovered = new Rectangle(_hoveredLabel.devXMarker, _hoveredLabel.devYMarker, 1, 1);

		// add marker rect
		if (devMarkerPointSizeRaw > 0) {
			rectMarker = new Rectangle(devMarkerX, devMarkerY, devMarkerSize, devMarkerSize);
			rectHovered = rectHovered.union(rectMarker);
		}

		// add label rect
		if (devLabelWidthRaw > 0) {

			rectLabel = new Rectangle(devLabelX, devLabelY, devLabelWidth, devLabelHeight);
			rectHovered = rectHovered.union(rectLabel);
		}

//		System.out.println((UI.timeStampNano() + " [" + getClass().getSimpleName() + "] ")
//				+ ("\trectLabel: " + rectLabel)
//				+ ("\trectMarker: " + rectMarker)
//				+ ("\trectHovered: " + rectHovered)
//		//
//				);
//		// TODO remove SYSTEM.OUT.PRINTLN

		return rectHovered;
	}

	/**
	 * @return Returns a {@link TourMarker} when a chart label (marker) is hovered or
	 *         <code>null</code> when a marker is not hovered.
	 */
	private TourMarker getHoveredTourMarker() {

		TourMarker tourMarker = null;

		if (_hoveredLabel.data instanceof TourMarker) {
			tourMarker = (TourMarker) _hoveredLabel.data;
		}

		return tourMarker;
	}

	@Override
	public Point getToolTipLocation(final Point tipSize) {

		final Rectangle hoveredRect = getHoveredRect();

		final int devHoveredX = hoveredRect.x;
		final int devHoveredY = hoveredRect.y;
		final int devHoveredWidth = hoveredRect.width;
		final int devHoveredHeight = hoveredRect.height;

		final boolean isVertical = _hoveredLabel.devIsVertical;

		final int margin = 0;//10;

		final int markerPosX = _hoveredLabel.devXMarker;
		final int markerPosY = _hoveredLabel.devYMarker;

		final int tipWidth = tipSize.x;
		final int tipHeight = tipSize.y;

		final int tipWidth2 = tipWidth / 2;

		int ttPosX;
		int ttPosY;

		if (isVertical) {

			// label is vertical

			ttPosX = devHoveredX - tipWidth + 1;
			ttPosY = devHoveredY;

		} else {

			// label is horizontal

			ttPosX = devHoveredX - tipWidth + 1;
			ttPosY = devHoveredY;
		}

//		// check chart bottom
//		final int chartHeight = _tourChart.getBounds().height;
//		if (ttPosY > chartHeight) {
//			// tooltip is below the chart bottom
//			ttPosY = chartHeight + margin;
//		}
//
//		// check display height
//		final Rectangle displayBounds = _tourChart.getDisplay().getBounds();
//		final Point chartDisplay = _tourChart.toDisplay(0, 0);
//
//		if (chartDisplay.y + ttPosY + tipHeight > displayBounds.height) {
//			ttPosY = markerPosY - tipHeight - margin;
//		}
//
//		// check display top
//		final int aboveChart = -tipHeight - margin;
//		if (ttPosY < aboveChart) {
//			ttPosY = aboveChart;
//		}

		final Point ttLocation = _tourChart.getChartComponents().getChartComponentGraph().toDisplay(ttPosX, ttPosY);

		return ttLocation;
	}

	public void hideMarkerTooltip() {

		hide();
	}

	private void initUI(final Composite parent) {

		final Display display = parent.getDisplay();

		_fgBorder = display.getSystemColor(SWT.COLOR_WIDGET_NORMAL_SHADOW);
		_bgColor = display.getSystemColor(SWT.COLOR_WHITE);
		_fgColor = display.getSystemColor(SWT.COLOR_DARK_GRAY);

	}

	private void onDispose() {

		_colorCache.dispose();
	}

	@Override
	protected void onMouseMoveInToolTip(final MouseEvent mouseEvent) {

		/*
		 * When in tooltip, the hovered label state is not displayed, keep it displayed
		 */
		final ChartLayerMarker markerLayer = _tourChart.getLayerTourMarker();
		markerLayer.setTooltipLabel(_hoveredLabel);
	}

	private void onPaintShellContainer(final PaintEvent event) {

		final GC gc = event.gc;
		final Point shellSize = _shellContainer.getSize();

		// draw border
		gc.setForeground(_fgBorder);
		gc.drawRectangle(0, 0, shellSize.x - 1, shellSize.y - 1);
	}

	private void onSelectUrl(final String address) {

		Util.openLink(Display.getCurrent().getActiveShell(), address);
	}

	void open(final ChartLabel hoveredLabel) {

//		System.out.println((UI.timeStampNano() + " [" + getClass().getSimpleName() + "] ")
//				+ ("\topen ")
//				+ ("\thoveredLabel " + (hoveredLabel == null ? "null" : hoveredLabel.serieIndex))
//				+ ("\t_hoveredLabel " + (_hoveredLabel == null ? "null" : _hoveredLabel.serieIndex))
//				+ ("\tisTooltipClosing() " + isTooltipClosing())
//		//
//				);
//		// TODO remove SYSTEM.OUT.PRINTLN

		boolean isKeepOpened = false;

		if (hoveredLabel != null && isTooltipClosing()) {

			/**
			 * This case occures when the tooltip is opened but is currently closing and the mouse
			 * is moved from the tooltip back to the hovered label.
			 * <p>
			 * This prevents that when the mouse is over the hovered label but not moved, that the
			 * tooltip keeps opened.
			 */
			isKeepOpened = true;
		}

		if (hoveredLabel == _hoveredLabel && isKeepOpened == false) {
			// nothing has changed
			return;
		}

		if (hoveredLabel == null) {

			// a marker is not hovered, hide tooltip

			hideMarkerTooltip();

		} else {

			// another marker is hovered, show tooltip

			_hoveredLabel = hoveredLabel;
			_hoveredTourMarker = getHoveredTourMarker();

			showToolTip();
		}
	}

	private void setTextWidth(final Control control, final String text) {

		final Point defaultSize = control.computeSize(SWT.DEFAULT, SWT.DEFAULT);

		// check default width
		if (defaultSize.x > _defaultTextWidth) {

			// limit width
			final GridData gd = (GridData) control.getLayoutData();
			gd.widthHint = _defaultTextWidth;
		}
	}

}
