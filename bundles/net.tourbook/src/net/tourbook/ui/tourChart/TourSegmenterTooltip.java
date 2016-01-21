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

import java.text.NumberFormat;
import java.util.ArrayList;

import net.tourbook.chart.ChartComponentGraph;
import net.tourbook.common.UI;
import net.tourbook.common.tooltip.AnimatedToolTipShell;
import net.tourbook.common.tooltip.IOpeningDialog;
import net.tourbook.common.util.IToolTipProvider;
import net.tourbook.data.TourData;
import net.tourbook.data.TourSegment;
import net.tourbook.tour.TourManager;
import net.tourbook.ui.ITourProvider;
import net.tourbook.ui.Messages;

import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.ToolBar;

/**
 * created: 06.09.2015
 */
public class TourSegmenterTooltip extends AnimatedToolTipShell implements ITourProvider, IToolTipProvider,
		IOpeningDialog {

	private String						_dialogId				= getClass().getCanonicalName();

	private TourChart					_tourChart;
	//
	private SegmenterSegment			_hoveredSegment;
	private Long						_hoveredTourId;
	//
	private final NumberFormat			_nf_0_0					= NumberFormat.getNumberInstance();
	private final NumberFormat			_nf_1_0					= NumberFormat.getNumberInstance();
	private final NumberFormat			_nf_1_1					= NumberFormat.getNumberInstance();
	private final NumberFormat			_nf_3_3					= NumberFormat.getNumberInstance();
	{
		_nf_0_0.setMinimumFractionDigits(0);
		_nf_0_0.setMaximumFractionDigits(0);

		_nf_1_0.setMinimumFractionDigits(1);
		_nf_1_0.setMaximumFractionDigits(0);

		_nf_1_1.setMinimumFractionDigits(1);
		_nf_1_1.setMaximumFractionDigits(1);

		_nf_3_3.setMinimumFractionDigits(3);
		_nf_3_3.setMaximumFractionDigits(3);
	}
	/**
	 * contains the controls which are displayed in the first column, these controls are used to get
	 * the maximum width and set the first column within the differenct section to the same width
	 */
	private final ArrayList<Control>	_firstColumnControls	= new ArrayList<Control>();
	private final ArrayList<Control>	_secondColumnControls	= new ArrayList<Control>();

	/*
	 * UI resources
	 */
	private Color						_bgColor;
	private Color						_fgColor;

	private Composite					_shellContainer;
	private Composite					_ttContainer;

	private Font						_boldFont;

	private Label						_lblAltitude_Diff;
	private Label						_lblAltitude_Diff_Unit;
	private Label						_lblAltitude_DownHour;
	private Label						_lblAltitude_DownHour_Unit;
	private Label						_lblAltitude_UpHour;
	private Label						_lblAltitude_UpHour_Unit;
	private Label						_lblAvg_Cadence;
	private Label						_lblAvg_CadenceUnit;
	private Label						_lblAvg_Pace;
	private Label						_lblAvg_PaceUnit;
	private Label						_lblAvg_Pulse;
	private Label						_lblAvg_PulseUnit;
	private Label						_lblAvg_Speed;
	private Label						_lblAvg_SpeedUnit;
	private Label						_lblDistance;
	private Label						_lblDistance_Unit;
	private Label						_lblTime_Break;
	private Label						_lblTime_Moving;
	private Label						_lblTime_Recording;
	private Label						_lblSegmentNo;

	private Label						_lblGradient;

//	private Label						_lblSegmentDuration;

	public TourSegmenterTooltip(final TourChart tourChart) {

		super(tourChart);

		_tourChart = tourChart;

		setFadeInSteps(10);
		setFadeOutSteps(20);
		setFadeOutDelaySteps(20);

		setToolTipCreateStyle(AnimatedToolTipShell.TOOLTIP_STYLE_KEEP_CONTENT);
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

		final Display display = shell.getDisplay();

		_bgColor = display.getSystemColor(SWT.COLOR_INFO_BACKGROUND);
		_fgColor = display.getSystemColor(SWT.COLOR_INFO_FOREGROUND);
		_boldFont = JFaceResources.getFontRegistry().getBold(JFaceResources.DIALOG_FONT);

		return createUI(shell);
	}

	private Composite createUI(final Composite shell) {

		/*
		 * shell container is necessary because the margins of the inner container will hide the
		 * tooltip when the mouse is hovered, which is not as it should be.
		 */
		_shellContainer = new Composite(shell, SWT.NONE);
		_shellContainer.setForeground(_fgColor);
		_shellContainer.setBackground(_bgColor);
		GridLayoutFactory.swtDefaults().applyTo(_shellContainer);
//		shellContainer.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_GREEN));
		{
			_ttContainer = new Composite(_shellContainer, SWT.NONE);
			_ttContainer.setForeground(_fgColor);
			_ttContainer.setBackground(_bgColor);
			GridLayoutFactory.fillDefaults() //
					.numColumns(2)
//					.equalWidth(true)
					.spacing(20, 0)
					.applyTo(_ttContainer);
//			_ttContainer.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
			{
				createUI_10_Header(_ttContainer);

				createUI_30_LeftColumn(_ttContainer);
				createUI_40_RightColumn(_ttContainer);
			}
		}

		return _shellContainer;
	}

	private void createUI_10_Header(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults()//
				.span(2, 1)
				.grab(true, false)
				.align(SWT.CENTER, SWT.FILL)
				.applyTo(container);
		container.setForeground(_fgColor);
		container.setBackground(_bgColor);
		GridLayoutFactory.fillDefaults()//
				.numColumns(2)
				.applyTo(container);
//		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));
		{

			/*
			 * title
			 */
			final Label label = createUI_Label(container, Messages.Segmenter_Tooltip_Label_Title);
			label.setFont(_boldFont);

			/*
			 * Segment No.
			 */
			_lblSegmentNo = createUI_Label(container, null);
			_lblSegmentNo.setFont(_boldFont);

			/*
			 * Duration
			 */
//			_lblSegmentDuration = createUI_Label(container, null);
		}
	}

	private void createUI_30_LeftColumn(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults()//
				.align(SWT.FILL, SWT.BEGINNING)
				.indent(0, 10)
				.applyTo(container);
		container.setForeground(_fgColor);
		container.setBackground(_bgColor);
		GridLayoutFactory.fillDefaults()//
				.numColumns(3)
				.spacing(5, 0)
				.applyTo(container);
//		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_MAGENTA));
		{
			createUI_32_Time(container);
			createUI_34_Distance(container);
		}
	}

	private void createUI_32_Time(final Composite container) {

		/*
		 * recording time
		 */
		{
			final Label label = createUI_Label(container, Messages.Segmenter_Tooltip_Label_RecordingTime);
			_firstColumnControls.add(label);

			_lblTime_Recording = createUI_LabelValue(container, SWT.TRAIL);
			_secondColumnControls.add(_lblTime_Recording);

			final Label labelHour = createUI_Label(container, Messages.Tour_Tooltip_Label_Hour);

			// force this column to take the rest of the space
			GridDataFactory.fillDefaults().grab(true, false).applyTo(labelHour);
		}

		/*
		 * moving time
		 */
		{
			final Label label = createUI_Label(container, Messages.Segmenter_Tooltip_Label_MovingTime);
			_firstColumnControls.add(label);

			_lblTime_Moving = createUI_LabelValue(container, SWT.TRAIL);
			_secondColumnControls.add(_lblTime_Moving);

			createUI_Label(container, Messages.Tour_Tooltip_Label_Hour);
		}

		/*
		 * break time
		 */
		{
			final Label label = createUI_Label(container, Messages.Segmenter_Tooltip_Label_BreakTime);
			_firstColumnControls.add(label);

			_lblTime_Break = createUI_LabelValue(container, SWT.TRAIL);
			_secondColumnControls.add(_lblTime_Break);

			createUI_Label(container, Messages.Tour_Tooltip_Label_Hour);
		}
	}

	private void createUI_34_Distance(final Composite container) {

		createUI_Spacer(container);

		/*
		 * distance
		 */
		{
			_firstColumnControls.add(createUI_Label(container, Messages.Segmenter_Tooltip_Label_Distance));

			_lblDistance = createUI_LabelValue(container, SWT.TRAIL);
			_secondColumnControls.add(_lblDistance);

			_lblDistance_Unit = createUI_LabelValue(container, SWT.LEAD);
		}

		createUI_Spacer(container);

		/*
		 * Altitude up/down
		 */
		{
			_firstColumnControls.add(createUI_Label(container, Messages.Segmenter_Tooltip_Label_AltitudeDifference));
			_secondColumnControls.add(_lblAltitude_Diff = createUI_LabelValue(container, SWT.TRAIL));

			_lblAltitude_Diff_Unit = createUI_LabelValue(container, SWT.LEAD);
		}

		/*
		 * Gradient
		 */
		{
			_firstColumnControls.add(createUI_Label(container, Messages.Segmenter_Tooltip_Label_Gradient));
			_secondColumnControls.add(_lblGradient = createUI_LabelValue(container, SWT.TRAIL));

			createUI_Label(container, UI.SYMBOL_PERCENTAGE);
		}
	}

	private void createUI_40_RightColumn(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults()//
				.align(SWT.FILL, SWT.BEGINNING)
				.indent(0, 10)
				.applyTo(container);
		container.setForeground(_fgColor);
		container.setBackground(_bgColor);
		GridLayoutFactory.fillDefaults()//
				.numColumns(3)
				.spacing(5, 0)
				.applyTo(container);
//		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));
		{
			createUI_42_Avg(container);
			createUI_44_Altimeter(container);
		}
	}

	private void createUI_42_Avg(final Composite parent) {

		Label label;

		/*
		 * avg pulse
		 */
		label = createUI_Label(parent, Messages.Tour_Tooltip_Label_AvgPulse);
		_firstColumnControls.add(label);

		_lblAvg_Pulse = createUI_LabelValue(parent, SWT.TRAIL);
		_secondColumnControls.add(_lblAvg_Pulse);

		_lblAvg_PulseUnit = createUI_LabelValue(parent, SWT.LEAD);

		/*
		 * avg speed
		 */
		label = createUI_Label(parent, Messages.Tour_Tooltip_Label_AvgSpeed);
		_firstColumnControls.add(label);

		_lblAvg_Speed = createUI_LabelValue(parent, SWT.TRAIL);
		_secondColumnControls.add(_lblAvg_Speed);

		_lblAvg_SpeedUnit = createUI_LabelValue(parent, SWT.LEAD);

		/*
		 * avg pace
		 */
		label = createUI_Label(parent, Messages.Tour_Tooltip_Label_AvgPace);
		_firstColumnControls.add(label);

		_lblAvg_Pace = createUI_LabelValue(parent, SWT.TRAIL);
		_secondColumnControls.add(_lblAvg_Pace);

		_lblAvg_PaceUnit = createUI_LabelValue(parent, SWT.LEAD);

		/*
		 * avg cadence
		 */
		label = createUI_Label(parent, Messages.Tour_Tooltip_Label_AvgCadence);
		_firstColumnControls.add(label);

		_lblAvg_Cadence = createUI_LabelValue(parent, SWT.TRAIL);
		_secondColumnControls.add(_lblAvg_Cadence);

		_lblAvg_CadenceUnit = createUI_LabelValue(parent, SWT.LEAD);
	}

	private void createUI_44_Altimeter(final Composite container) {

		createUI_Spacer(container);
		createUI_Spacer(container);

		/*
		 * Altitude up/h
		 */
		{
			_firstColumnControls.add(createUI_Label(container, Messages.Segmenter_Tooltip_Label_Altimeter
					+ UI.SPACE
					+ UI.SYMBOL_ARROW_UP));
			_secondColumnControls.add(_lblAltitude_UpHour = createUI_LabelValue(container, SWT.TRAIL));

			_lblAltitude_UpHour_Unit = createUI_LabelValue(container, SWT.LEAD);
		}

		/*
		 * Altitude down/h
		 */
		{
			_firstColumnControls.add(createUI_Label(container, Messages.Segmenter_Tooltip_Label_Altimeter
					+ UI.SPACE
					+ UI.SYMBOL_ARROW_DOWN));
			_secondColumnControls.add(_lblAltitude_DownHour = createUI_LabelValue(container, SWT.TRAIL));

			_lblAltitude_DownHour_Unit = createUI_LabelValue(container, SWT.LEAD);
		}
	}

	private Label createUI_Label(final Composite parent, final String labelText) {

		final Label label = new Label(parent, SWT.NONE);
		label.setForeground(_fgColor);
		label.setBackground(_bgColor);

		if (labelText != null) {
			label.setText(labelText);
		}

		return label;
	}

	private Label createUI_LabelValue(final Composite parent, final int style) {

		final Label label = new Label(parent, style);
		GridDataFactory.fillDefaults().applyTo(label);
		label.setForeground(_fgColor);
		label.setBackground(_bgColor);

		return label;
	}

	private void createUI_Spacer(final Composite container) {

		// spacer
		final Label label = createUI_Label(container, null);
		GridDataFactory.fillDefaults().span(3, 1).applyTo(label);
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

		final int segmentWidth = _hoveredSegment.paintedX2 - _hoveredSegment.paintedX1;

		final int devHoveredX = _hoveredSegment.paintedX1;
		int devHoveredY = Math.min(_hoveredSegment.paintedY2, _hoveredSegment.paintedY1);
		final int devHoveredWidth = segmentWidth;

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
		if (ttPosX /* + tipWidth */< 0) {

			// set tooltip to the graph left border
//			ttPosX = -tipWidth - 1;

			ttPosX = 0;

		} else if (ttPosX + tipWidth > _hoveredSegment.devGraphWidth) {

			// set tooltip to the graph right border
			ttPosX = _hoveredSegment.devGraphWidth - tipWidth;
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
		hide();
	}

	@Override
	public void hideToolTip() {
		hide();
	}

	@Override
	protected boolean isInNoHideArea(final Point displayCursorLocation) {

		if (_hoveredSegment == null) {

			return false;

		} else {

			final boolean isInNoHideArea = _hoveredSegment.isInNoHideArea(_tourChart
					.getChartComponents()
					.getChartComponentGraph(), displayCursorLocation);

			return isInNoHideArea;
		}
	}

	private void onDispose() {

	}

	@Override
	protected void onUpdateUI() {

		updateUI();

		// compute width for all controls and equalize column width for the different sections
		UI.setEqualizeColumWidthsWithReset(_firstColumnControls, 5);
		UI.setEqualizeColumWidthsWithReset(_secondColumnControls, 5);

		final Shell shell = _shellContainer.getShell();
		shell.layout(true, true);
		shell.pack(true);

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

			// another segment is hovered, show tooltip

			_hoveredSegment = hoveredSegment;
			_hoveredTourId = hoveredSegment.tourId;

			showToolTip();

		}
	}

	private void updateUI() {

		final ArrayList<TourSegment> tourSegments = _tourChart.getTourSegments();
		if (tourSegments == null || tourSegments.size() == 0) {
			return;
		}

		final int segmentIndex = _hoveredSegment.segmentIndex;

		final TourSegment tourSegment = tourSegments.get(segmentIndex - 1);

		_lblSegmentNo.setText(Integer.toString(segmentIndex));

		/*
		 * Duration
		 */
//		_lblSegmentDuration.setText(String.format(//
//				Messages.Tour_Tooltip_Format_DateWeekTime,
//				_dateFormatter.print(dtTourStart.getMillis()),
//				_timeFormatter.print(dtTourStart.getMillis()),
//				_timeFormatter.print(dtTourEnd.getMillis()),
//				dtTourStart.getWeekOfWeekyear()));

		/*
		 * Altitude
		 */
		_lblAltitude_Diff.setText(_nf_1_1.format(tourSegment.altitudeDiffSegmentBorder));
		_lblAltitude_Diff_Unit.setText(UI.UNIT_LABEL_ALTITUDE);

		final float altiDown = (tourSegment.altitudeDownHour / net.tourbook.ui.UI.UNIT_VALUE_ALTITUDE)
				/ tourSegment.drivingTime
				* 3600;
		_lblAltitude_DownHour.setText(_nf_1_0.format(altiDown));
		_lblAltitude_DownHour_Unit.setText(UI.UNIT_LABEL_ALTITUDE + Messages.ColumnFactory_hour);

		final float altiUp = (tourSegment.altitudeUpHour / net.tourbook.ui.UI.UNIT_VALUE_ALTITUDE)
				/ tourSegment.drivingTime
				* 3600;
		_lblAltitude_UpHour.setText(_nf_1_0.format(altiUp));
		_lblAltitude_UpHour_Unit.setText(UI.UNIT_LABEL_ALTITUDE + Messages.ColumnFactory_hour);

		_lblGradient.setText(_nf_1_1.format(tourSegment.gradient));

		// distance
		_lblDistance.setText(_nf_3_3.format(tourSegment.distanceDiff / 1000));
		_lblDistance_Unit.setText(UI.UNIT_LABEL_DISTANCE);

		/*
		 * Avg
		 */
		// avg speed
		_lblAvg_Speed.setText(_nf_1_1.format(tourSegment.speed));
		_lblAvg_SpeedUnit.setText(UI.UNIT_LABEL_SPEED);

		// avg pace
		final int pace = (int) tourSegment.pace;
		_lblAvg_Pace.setText(String.format(//
				Messages.Tour_Tooltip_Format_Pace,
				pace / 60,
				pace % 60)//
				);
		_lblAvg_PaceUnit.setText(UI.UNIT_LABEL_PACE);

		// avg pulse
		_lblAvg_Pulse.setText(_nf_1_1.format(tourSegment.pulse));
		_lblAvg_PulseUnit.setText(Messages.Value_Unit_Pulse);

		// avg cadence
		_lblAvg_Cadence.setText(_nf_1_1.format(tourSegment.cadence));
		_lblAvg_CadenceUnit.setText(Messages.Value_Unit_Cadence);

		/*
		 * Time
		 */
		final int breakTime = tourSegment.breakTime;
		final int movingTime = tourSegment.drivingTime;
		final int recordingTime = tourSegment.recordingTime;

		_lblTime_Break.setText(String.format(
				Messages.Tour_Tooltip_Format_Date,
				breakTime / 3600,
				(breakTime % 3600) / 60,
				(breakTime % 3600) % 60)//
				);
		_lblTime_Moving.setText(String.format(
				Messages.Tour_Tooltip_Format_Date,
				movingTime / 3600,
				(movingTime % 3600) / 60,
				(movingTime % 3600) % 60)//
				);
		_lblTime_Recording.setText(String.format(
				Messages.Tour_Tooltip_Format_Date,
				recordingTime / 3600,
				(recordingTime % 3600) / 60,
				(recordingTime % 3600) % 60)//
				);
	}
}
