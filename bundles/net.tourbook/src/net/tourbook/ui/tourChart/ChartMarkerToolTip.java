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
import net.tourbook.chart.ColorCache;
import net.tourbook.common.UI;
import net.tourbook.common.formatter.FormatManager;
import net.tourbook.common.tooltip.AnimatedToolTipShell;
import net.tourbook.data.TourData;
import net.tourbook.data.TourMarker;
import net.tourbook.tour.ActionOpenMarkerDialog;
import net.tourbook.ui.ITourProvider;
import net.tourbook.web.WEB;

import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
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
import org.eclipse.swt.widgets.ToolBar;

/**
 * created: 30.5.2014
 */
public class ChartMarkerToolTip extends AnimatedToolTipShell implements ITourProvider {

	private static final String				GRAPH_LABEL_ALTITUDE			= net.tourbook.common.Messages.Graph_Label_Altitude;
	private static final String				GRAPH_LABEL_TIME				= net.tourbook.common.Messages.Graph_Label_Time;
	private static final String				GRAPH_LABEL_DISTANCE			= net.tourbook.common.Messages.Graph_Label_Distance;

	private static final int				DEFAULT_TEXT_WIDTH				= 50;
	private static final int				DEFAULT_TEXT_HEIGHT				= 20;

	/**
	 * Visual position for marker tooltip, they must correspond to the position id
	 * TOOLTIP_POSITION_*.
	 */
	public static final String[]			TOOLTIP_POSITIONS;

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

	private static final int				TOOLTIP_POSITION_LEFT			= 0;
	private static final int				TOOLTIP_POSITION_RIGHT			= 1;
	private static final int				TOOLTIP_POSITION_ABOVE			= 2;
	private static final int				TOOLTIP_POSITION_BELOW			= 3;
	private static final int				TOOLTIP_POSITION_CHART_TOP		= 4;
	private static final int				TOOLTIP_POSITION_CHART_BOTTOM	= 5;

	public static final int					DEFAULT_TOOLTIP_POSITION		= TOOLTIP_POSITION_BELOW;

	private static final int				_textStyle						= SWT.WRAP //
																					| SWT.MULTI
																					| SWT.READ_ONLY
//																					| SWT.BORDER
																			;

	private PixelConverter					_pc;

	private int								_defaultTextWidth;
	private int								_defaultTextHeight;

	private TourChart						_tourChart;
	private TourData						_tourData;

	private ChartLabel						_hoveredLabel;
	private TourMarker						_hoveredTourMarker;

	/**
	 * When <code>true</code> the actions are displayed, e.g. to open the marker dialog.
	 */
	private boolean							_isShowActions;

	private ChartMarkerConfig				_cmc;

	private ActionOpenMarkerDialogInTooltip	_actionOpenMarkerDialog;

//	private final NumberFormat				_nf1NoGroup						= NumberFormat.getNumberInstance();
//	private final NumberFormat				_nf3NoGroup						= NumberFormat.getNumberInstance();
//	{
//		_nf1NoGroup.setMinimumFractionDigits(1);
//		_nf1NoGroup.setMaximumFractionDigits(1);
//		_nf1NoGroup.setGroupingUsed(false);
//
//		_nf3NoGroup.setMinimumFractionDigits(3);
//		_nf3NoGroup.setMaximumFractionDigits(3);
//		_nf3NoGroup.setGroupingUsed(false);
//	}

	/*
	 * UI resources
	 */
	private Font							_boldFont;
	private Color							_fgBorder;
	private Color							_titleColor;
	private ColorCache						_colorCache;

	/*
	 * UI controls
	 */
	private Composite						_shellContainer;
	private Composite						_ttContainer;

	/**
	 * Contains the controls which are displayed in the first column, these controls are used to get
	 * the maximum width and set the first column within the differenct section to the same width.
	 */
	private final ArrayList<Control>		_firstColumnControls			= new ArrayList<Control>();

	private class ActionOpenMarkerDialogInTooltip extends ActionOpenMarkerDialog {

		public ActionOpenMarkerDialogInTooltip() {
			super(ChartMarkerToolTip.this, true);
		}

		@Override
		public void run() {

			hideNow();

			super.run();
		}
	}

	public ChartMarkerToolTip(final TourChart tourChart) {

		super(tourChart);

		_tourChart = tourChart;

		setReceiveMouseExitEvent(true);
		setReceiveMouseMoveEvent(true);

		setIsShowShellTrimStyle(false);
		setIsAnimateLocation(false);
	}

	@Override
	protected void beforeHideToolTip() {

		/*
		 * This is the tricky part that the hovered marker is reset before the tooltip is closed and
		 * not when nothing is hovered. This ensures that the tooltip has a valid state.
		 */
		_hoveredLabel = null;
		_hoveredTourMarker = null;
	}

	@Override
	protected boolean canShowToolTip() {

		return _hoveredLabel != null;
	}

	private void createActions() {

		final boolean isSingleTour = _tourData.isMultipleTours == false;

		_actionOpenMarkerDialog = new ActionOpenMarkerDialogInTooltip();

		// setup action for the current tour marker
		_actionOpenMarkerDialog.setEnabled(isSingleTour);
		_actionOpenMarkerDialog.setTourMarker(_hoveredTourMarker);
	}

	@Override
	protected Composite createToolTipContentArea(final Composite shell) {

		setFadeInSteps(1);
		setFadeOutSteps(10);
		setFadeOutDelaySteps(5);

		if (_hoveredLabel == null) {
			return null;
		}

		_tourData = _tourChart.getTourData();

		shell.addDisposeListener(new DisposeListener() {
			@Override
			public void widgetDisposed(final DisposeEvent e) {
				onDispose();
			}
		});

		createActions();

		_firstColumnControls.clear();

		final Composite container = createUI(shell);

		setColors(container);

		// compute width for all controls and equalize column width for the different sections
		_ttContainer.layout(true, true);
		UI.setEqualizeColumWidths(_firstColumnControls, 10);

		_ttContainer.layout(true, true);

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
//		_shellContainer.setForeground(_fgColor);
//		_shellContainer.setBackground(_bgColor);
		_shellContainer.addPaintListener(new PaintListener() {
			@Override
			public void paintControl(final PaintEvent e) {
				onPaintShellContainer(e);
			}
		});
		{
			_ttContainer = new Composite(_shellContainer, SWT.NONE);
			GridLayoutFactory.fillDefaults()//
					.extendedMargins(2, 5, 2, 5)
					.numColumns(1)
					.applyTo(_ttContainer);
			_ttContainer.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_INFO_BACKGROUND));
			_ttContainer.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
			{
				createUI_20_TopContainer();
				createUI_40_Description(_ttContainer);
				createUI_70_Values(_ttContainer);
				createUI_80_Link();
			}
		}

		return _shellContainer;
	}

	private void createUI_20_TopContainer() {

		final String ttTitle = _hoveredTourMarker.getLabel();

		if (ttTitle.length() == 0 && _isShowActions == false) {

			// nothing is displayed
			return;
		}

		final Composite topContainer = new Composite(_ttContainer, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(topContainer);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(topContainer);
//			topContainer.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));
		{
			/*
			 * Name
			 */
			final Label lblName = new Label(topContainer, SWT.NONE);
			GridDataFactory.fillDefaults()//
					.grab(true, false)
					.align(SWT.FILL, SWT.CENTER)
					.indent(3, 0)
					.applyTo(lblName);

			lblName.setFont(_boldFont);
			lblName.setForeground(_titleColor);
			lblName.setText(ttTitle);

			/*
			 * Actions
			 */
			createUI_30_Actions(topContainer);
		}
	}

	private void createUI_30_Actions(final Composite parent) {

		if (_isShowActions) {

			/*
			 * Action toolbar
			 */
			final ToolBar toolbar = new ToolBar(parent, SWT.FLAT);
			GridDataFactory.fillDefaults().applyTo(toolbar);

			final ToolBarManager tbm = new ToolBarManager(toolbar);

			tbm.add(_actionOpenMarkerDialog);

			tbm.update(true);

		} else {

			// create dummy to keep the layout
			new Label(parent, SWT.NONE);
		}
	}

	/**
	 * Description
	 * 
	 * @param parent
	 */
	private void createUI_40_Description(final Composite parent) {

		final String markerDescription = _hoveredTourMarker.getDescription();
		if (markerDescription.length() > 0) {

			final Text txtDescription = new Text(parent, _textStyle);
			GridDataFactory.fillDefaults()//
//					.indent(-3, 0)
					.applyTo(txtDescription);
			txtDescription.setText(markerDescription);

			setTextControlSize(parent, txtDescription, markerDescription);
		}
	}

	private void createUI_70_Values(final Composite parent) {

		int valueIndex;
		if (_tourData.isMultipleTours) {
			valueIndex = _hoveredTourMarker.getMultiTourSerieIndex();
		} else {
			valueIndex = _hoveredTourMarker.getSerieIndex();
		}

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults()//
				.grab(true, false)
				.indent(3, 0)
				.applyTo(container);
		GridLayoutFactory.fillDefaults()//
				.numColumns(3)
				.spacing(5, 1)
				.applyTo(container);
		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_YELLOW));
		{
			{
				/*
				 * Altitude
				 */
				final boolean isAvailableAltitude = _tourData.getAltitudeSerie() != null;
				if (isAvailableAltitude) {

					final float value = _tourData.getAltitudeSmoothedSerie(false)[valueIndex];

					createUI_72_ValueField(
							container,
							GRAPH_LABEL_ALTITUDE,
							UI.UNIT_LABEL_ALTITUDE,
							FormatManager.formatAltitude(value));
				}
			}

			{
				/*
				 * Distance
				 */
				float distance = Float.MIN_VALUE;

				if (_cmc.isShowAbsoluteValues) {

					// absolute distance

					if (_tourData.distanceSerie != null) {
						distance = _tourData.distanceSerie[valueIndex];
					}

				} else {

					// relative distance

					final float markerDistance = _hoveredTourMarker.getDistance();
					if (markerDistance != 1) {
						distance = markerDistance;
					}
				}

				if (distance != Float.MIN_VALUE) {

					final double value = distance //
							/ 1000
							/ net.tourbook.ui.UI.UNIT_VALUE_DISTANCE;

					createUI_72_ValueField(
							container,
							GRAPH_LABEL_DISTANCE,
							UI.UNIT_LABEL_DISTANCE,
							FormatManager.formatDistance(value));
				}
			}

			{
				/*
				 * Duration
				 */
				int timeValue = Integer.MIN_VALUE;

				if (_cmc.isShowAbsoluteValues) {

					// absolute time

					if (_tourData.timeSerie != null) {
						timeValue = _tourData.timeSerie[valueIndex];
					}

				} else {

					// relative time

					timeValue = _hoveredTourMarker.getTime();
				}

				if (timeValue != Integer.MIN_VALUE) {

					createUI_72_ValueField(
							container,
							GRAPH_LABEL_TIME,
							UI.UNIT_LABEL_TIME,
							FormatManager.formatRecordingTime(timeValue));
				}
			}
		}

	}

	private void createUI_72_ValueField(final Composite parent,
										final String fieldName,
										final String unit,
										final String valueText) {

		Label label = new Label(parent, SWT.NONE);
		GridDataFactory.fillDefaults().applyTo(label);
		_firstColumnControls.add(label);

		label.setText(fieldName);

		// Value
		label = new Label(parent, SWT.TRAIL);
		GridDataFactory.fillDefaults()//
				.align(SWT.END, SWT.FILL)
				.indent(10, 0)
				.applyTo(label);

		label.setText(valueText);

		// Unit
		label = new Label(parent, SWT.NONE);
		label.setText(unit);
	}

	/**
	 * Url
	 */
	private void createUI_80_Link() {

		final String urlText = _hoveredTourMarker.getUrlText();
		final String urlAddress = _hoveredTourMarker.getUrlAddress();
		final boolean isText = urlText.length() > 0;
		final boolean isAddress = urlAddress.length() > 0;

		if (isText || isAddress) {

			final Link linkUrl = new Link(_ttContainer, SWT.NONE);
			GridDataFactory.fillDefaults()//
					.indent(3, 0)
					.applyTo(linkUrl);

			linkUrl.addListener(SWT.Selection, new Listener() {
				@Override
				public void handleEvent(final Event event) {
					onSelectUrl(event.text);
				}
			});

			String linkText;

			if (isAddress == false) {

				// only text is in the link -> this is not a internet address but create a link of it

				linkText = "<a href=\"" + urlText + "\">" + urlText + "</a>"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

			} else if (isText == false) {

				linkText = "<a href=\"" + urlAddress + "\">" + urlAddress + "</a>"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

			} else {

				linkText = "<a href=\"" + urlAddress + "\">" + urlText + "</a>"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			}

			linkUrl.setText(linkText);

			setUrlWidth(linkUrl, linkText);
		}
	}

	/**
	 * This is copied from {@link ChartLayerMarker#drawOverlay()}.
	 * <p>
	 * The region which is computed in drawOverlay() cannot be used because the overlay is painted
	 * <b>after</b> the tooltip is displayed and <b>not before.</b>
	 */
	private Rectangle getHoveredRect() {

		final int hoverSize = _hoveredLabel.devHoverSize;
		final int devMarkerPointSizeRaw = _hoveredLabel.devMarkerPointSize;

		Rectangle rectHovered = new Rectangle(_hoveredLabel.devXMarker, _hoveredLabel.devYMarker, 1, 1);

		// add marker rect
		if (_cmc.isShowMarkerPoint && devMarkerPointSizeRaw > 0) {

			int devMarkerPointSize = devMarkerPointSizeRaw;
			if (devMarkerPointSize < 1) {
				devMarkerPointSize = 1;
			}

			final int devMarkerX = _hoveredLabel.devXMarker - hoverSize;
			final int devMarkerY = _hoveredLabel.devYMarker - hoverSize;
			final int devMarkerSize = devMarkerPointSize + 2 * hoverSize;

			final Rectangle rectMarker = new Rectangle(devMarkerX, devMarkerY, devMarkerSize, devMarkerSize);
			rectHovered = rectHovered.union(rectMarker);
		}

		// add label rect
		if (_cmc.isShowMarkerLabel && _hoveredLabel.paintedLabel != null) {

			final Rectangle paintedLabel = _hoveredLabel.paintedLabel;
			final int devLabelWidthRaw = paintedLabel.width;

			if (devLabelWidthRaw > 0) {

				final int devLabelX = paintedLabel.x - hoverSize;
				final int devLabelY = paintedLabel.y - hoverSize;
				final int devLabelWidth = devLabelWidthRaw + 2 * hoverSize;
				final int devLabelHeight = paintedLabel.height + 2 * hoverSize;

				final Rectangle rectLabel = new Rectangle(devLabelX, devLabelY, devLabelWidth, devLabelHeight);

				rectHovered = rectHovered.union(rectLabel);
			}
		}

		return rectHovered;
	}

	/**
	 * @param hoveredLabel
	 * @return Returns a {@link TourMarker} when a chart label (marker) is hovered or
	 *         <code>null</code> when a marker is not hovered.
	 */
	private TourMarker getHoveredTourMarker(final ChartLabel hoveredLabel) {

		TourMarker tourMarker = null;

		if (hoveredLabel.data instanceof TourMarker) {
			tourMarker = (TourMarker) hoveredLabel.data;
		}

		return tourMarker;
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

		final Rectangle hoveredRect = getHoveredRect();

		final int devHoveredX = hoveredRect.x;
		int devHoveredY = hoveredRect.y;
		final int devHoveredWidth = hoveredRect.width;
		final int devHoveredHeight = hoveredRect.height;
		final int devHoverSize = _hoveredLabel.devHoverSize;

		final int devYTop = _hoveredLabel.devYTop;
		final int devYBottom = _hoveredLabel.devYBottom;
		final boolean isVertical = _hoveredLabel.devIsVertical;

		final int tipWidth = tipSize.x;
		final int tipHeight = tipSize.y;

		int ttPosX;
		int ttPosY;

		if (devHoveredY < devYTop) {
			// remove hovered size
			devHoveredY = devYTop;
		}

		switch (_cmc.markerTooltipPosition) {
		case TOOLTIP_POSITION_LEFT:

			ttPosX = devHoveredX - tipWidth - 1;

			if (isVertical) {
				ttPosY = devHoveredY;
			} else {
				ttPosY = devHoveredY + devHoveredHeight / 2 - tipHeight / 2;
			}

			break;

		case TOOLTIP_POSITION_RIGHT:

			ttPosX = devHoveredX + devHoveredWidth + 1;

			if (isVertical) {
				ttPosY = devHoveredY;
			} else {
				ttPosY = devHoveredY + devHoveredHeight / 2 - tipHeight / 2;
			}

			break;

		case TOOLTIP_POSITION_ABOVE:

			ttPosX = devHoveredX + devHoveredWidth / 2 - tipWidth / 2;
			ttPosY = devHoveredY - tipHeight - 1;

			break;

		case TOOLTIP_POSITION_CHART_TOP:

			ttPosX = devHoveredX + devHoveredWidth / 2 - tipWidth / 2;
			ttPosY = devYTop - tipHeight;

			break;
		case TOOLTIP_POSITION_CHART_BOTTOM:

			ttPosX = devHoveredX + devHoveredWidth / 2 - tipWidth / 2;
			ttPosY = devYBottom;

			break;

		// case TOOLTIP_POSITION_BELOW:
		default:

			ttPosX = devHoveredX + devHoveredWidth / 2 - tipWidth / 2;
			ttPosY = devHoveredY + devHoveredHeight + 1;

			if (_hoveredLabel.visualPosition == TourMarker.LABEL_POS_VERTICAL_TOP_CHART) {

				/*
				 * y position is wrong, adjust it but this do NOT cover all possible positions, but
				 * some
				 */
				ttPosY -= devHoverSize;
			}

			break;
		}

		// ckeck if tooltip is left to the chart border
		if (ttPosX + tipWidth < 0) {

			// set tooltip to the graph left border
			ttPosX = -tipWidth - 1;

		} else if (ttPosX > _hoveredLabel.devGraphWidth) {

			// set tooltip to the graph right border
			ttPosX = _hoveredLabel.devGraphWidth;
		}

		if (ttPosY + tipHeight < devYTop) {

			// tooltip is above the graph

			ttPosY = devYTop - tipHeight;

		} else if (ttPosY > devYBottom) {

			// tooltip is below the graph

			ttPosY = devYBottom;
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

				switch (_cmc.markerTooltipPosition) {

				case TOOLTIP_POSITION_BELOW:
				case TOOLTIP_POSITION_ABOVE:
				case TOOLTIP_POSITION_CHART_TOP:
				case TOOLTIP_POSITION_CHART_BOTTOM:
					ttLocation.x = displayX;
					break;

				case TOOLTIP_POSITION_LEFT:
					ttLocation.x = ttLocation.x + tipWidth + devHoveredWidth + 2;
					break;
				}
			}

			if (rightBottomBounds.x > displayX + displayWidth) {

				switch (_cmc.markerTooltipPosition) {

				case TOOLTIP_POSITION_BELOW:
				case TOOLTIP_POSITION_ABOVE:
				case TOOLTIP_POSITION_CHART_TOP:
				case TOOLTIP_POSITION_CHART_BOTTOM:
					ttLocation.x = displayWidth - tipWidth;
					break;

				case TOOLTIP_POSITION_RIGHT:
					ttLocation.x = ttLocation.x - tipWidth - devHoveredWidth - 2;
					break;
				}
			}

			if (ttLocation.y < displayY) {

				switch (_cmc.markerTooltipPosition) {

				case TOOLTIP_POSITION_ABOVE:
				case TOOLTIP_POSITION_CHART_TOP:
					ttLocation.y = graphLocation.y + devHoveredY + devHoveredHeight - devHoverSize + 2;
					break;
				}
			}

			if (rightBottomBounds.y > displayY + displayHeight) {

				switch (_cmc.markerTooltipPosition) {

				case TOOLTIP_POSITION_BELOW:
				case TOOLTIP_POSITION_CHART_BOTTOM:
					ttLocation.y = graphLocation.y + devHoveredY - tipHeight - 1;
					break;
				}
			}
		}

		return ttLocation;
	}

	private void initUI(final Composite parent) {

		final Display display = parent.getDisplay();

		_pc = new PixelConverter(parent);
		_defaultTextWidth = _pc.convertWidthInCharsToPixels(DEFAULT_TEXT_WIDTH);
		_defaultTextHeight = _pc.convertHeightInCharsToPixels(DEFAULT_TEXT_HEIGHT);

		_colorCache = new ColorCache();
		_boldFont = JFaceResources.getFontRegistry().getBold(JFaceResources.DIALOG_FONT);
		_fgBorder = display.getSystemColor(SWT.COLOR_WIDGET_NORMAL_SHADOW);
		_titleColor = _colorCache.getColor(new RGB(0x50, 0x50, 0x50));
	}

	private void onDispose() {

		_colorCache.dispose();

		_firstColumnControls.clear();
	}

	@Override
	protected void onMouseExitToolTip(final MouseEvent mouseEvent) {

		/*
		 * When exit tooltip, hide hovered label
		 */
		final ChartLayerMarker markerLayer = _tourChart.getLayerTourMarker();
		markerLayer.setTooltipLabel(null);
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

		WEB.openUrl(address);

		// close tooltip when a link is selected
		hideNow();
	}

	void open(final ChartLabel hoveredLabel) {

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

		if (hoveredLabel == null /* || hoveredLabel.paintedLabel == null */) {

			// a marker is not hovered or is hidden, hide tooltip

			hide();

		} else {

			// another marker is hovered, show tooltip

			_hoveredLabel = hoveredLabel;
			_hoveredTourMarker = getHoveredTourMarker(hoveredLabel);

			showToolTip();
		}
	}

	void setChartMarkerConfig(final ChartMarkerConfig cmc) {
		_cmc = cmc;
	}

	private void setColors(final Composite container) {

		final Display display = container.getDisplay();

		UI.setBackgroundColorForAllChildren(container, display.getSystemColor(SWT.COLOR_INFO_BACKGROUND));
	}

	void setIsShowMarkerActions(final boolean isShowMarkerActions) {

		_isShowActions = isShowMarkerActions;
	}

	private void setTextControlSize(final Composite parent, final Text txtControl, final String text) {

		Point defaultSize = txtControl.computeSize(SWT.DEFAULT, SWT.DEFAULT);

		// check default width
		if (defaultSize.x > _defaultTextWidth) {

			// check default height
			defaultSize = txtControl.computeSize(_defaultTextWidth, SWT.DEFAULT);
			if (defaultSize.y > _defaultTextHeight) {

				setTextControlSize_RecreateWithVScroll(parent, txtControl, text, _defaultTextWidth);

			} else {

				// limit width
				final GridData gd = (GridData) txtControl.getLayoutData();
				gd.widthHint = _defaultTextWidth;
			}

		} else if (defaultSize.y > _defaultTextHeight) {

			setTextControlSize_RecreateWithVScroll(parent, txtControl, text, SWT.DEFAULT);
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
	private void setTextControlSize_RecreateWithVScroll(final Composite parent,
														Text txtControl,
														final String text,
														final int widthHint) {

		txtControl.dispose();

		txtControl = new Text(parent, _textStyle | SWT.V_SCROLL);
		GridDataFactory.fillDefaults()//
				.hint(widthHint, _defaultTextHeight)
				.applyTo(txtControl);

		txtControl.setText(text);
	}

	private void setUrlWidth(final Control control, final String text) {

		final Point defaultSize = control.computeSize(SWT.DEFAULT, SWT.DEFAULT);

		// check default width
		if (defaultSize.x > _defaultTextWidth) {

			// limit width
			final GridData gd = (GridData) control.getLayoutData();
			gd.widthHint = _defaultTextWidth;
		}
	}

}
