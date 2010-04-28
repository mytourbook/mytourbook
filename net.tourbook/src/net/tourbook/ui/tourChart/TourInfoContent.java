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
package net.tourbook.ui.tourChart;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Formatter;

import net.tourbook.data.IWeather;
import net.tourbook.data.TourData;
import net.tourbook.data.TourType;
import net.tourbook.database.TourDatabase;
import net.tourbook.ui.ITourProvider;
import net.tourbook.ui.Messages;
import net.tourbook.ui.UI;
import net.tourbook.ui.action.ActionTourToolTipEditQuick;
import net.tourbook.ui.action.ActionTourToolTipEditTour;

import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.window.ToolTip;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.ToolBar;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

public class TourInfoContent extends ToolTip implements ITourProvider {

	private static final int			MAX_TOOLTIP_WIDTH		= 400;
	private static final int			MAX_DATA_WIDTH			= 300;

	private TourData					_tourData;

	private Color						_bgColor;
	private Color						_fgColor;
	private Font						_boldFont;

	private final DateTimeFormatter		_dateFormatter			= DateTimeFormat.fullDate();
	private final DateTimeFormatter		_timeFormatter			= DateTimeFormat.shortTime();
	private final NumberFormat			_nf1					= NumberFormat.getInstance();
	private final NumberFormat			_nf3					= NumberFormat.getInstance();

	{
		_nf1.setMinimumFractionDigits(1);
		_nf1.setMaximumFractionDigits(1);
		_nf3.setMinimumFractionDigits(3);
		_nf3.setMaximumFractionDigits(3);
	}

	private ActionTourToolTipEditTour	_actionEditTour;
	private ActionTourToolTipEditQuick	_actionEditQuick;

	/**
	 * contains the controls which are displayed in the first column, these controls are used to get
	 * the maximum width and set the first column within the differenct section to the same width
	 */
	private final ArrayList<Control>	_firstColumnControls	= new ArrayList<Control>();

	/*
	 * UI controls
	 */
	private Composite					_ttContainer;
	private Label						_lblTitle;
	private Label						_lblDate;
	private Label						_lblTourTags;
	private CLabel						_lblTourType;
	private Label						_lblDescription;

	/*
	 * 1. column
	 */
	private Label						_lblRecordingTime;
	private Label						_lblMovingTime;
	private Label						_lblBreakTime;

	/*
	 * 2. column
	 */
	private Label						_lblDistance;
	private Label						_lblDistanceUnit;
	private Label						_lblAltitudeUp;
	private Label						_lblAltitudeUpUnit;
	private Label						_lblAltitudeDown;
	private Label						_lblAltitudeDownUnit;

	private Label						_lblAvgSpeed;
	private Label						_lblAvgSpeedUnit;
	private Label						_lblAvgPace;
	private Label						_lblAvgPaceUnit;

	private Label						_lblTemperature;
	private CLabel						_lblClouds;


	public TourInfoContent(final Control control) {
		this(control, NO_RECREATE, false);
	}

	public TourInfoContent(final Control control, final int style, final boolean manualActivation) {
		super(control, style, manualActivation);
	}

	@Override
	protected Composite createToolTipContentArea(final Event event, final Composite parent) {

		if (_tourData == null) {
			return null;
		}

		final Display display = parent.getDisplay();
		_bgColor = display.getSystemColor(SWT.COLOR_INFO_BACKGROUND);
		_fgColor = display.getSystemColor(SWT.COLOR_INFO_FOREGROUND);
		_boldFont = JFaceResources.getFontRegistry().getBold(JFaceResources.DIALOG_FONT);

		final Composite container = createUI(parent);
		updateUI();
		enableControls();

		// compute width for all controls and equalize column width for the different sections
		_ttContainer.layout(true, true);
		UI.setEqualizeColumWidths(_firstColumnControls);

		parent.addDisposeListener(new DisposeListener() {
			@Override
			public void widgetDisposed(final DisposeEvent e) {
				onDispose();
			}
		});

		// allow the actions to be selected
		setHideOnMouseDown(false);

		return container;
	}

	private Composite createUI(final Composite parent) {

		final int margin = 5;

		/*
		 * shell container is necessary because the margins of the inner container will hide the
		 * tooltip which is not as it should be.
		 */
		final Composite shellContainer = new Composite(parent, SWT.NONE);
		shellContainer.setForeground(_fgColor);
		shellContainer.setBackground(_bgColor);
//		shellContainer.setLayout(new FillLayout());
		GridLayoutFactory.fillDefaults().applyTo(shellContainer);
//		shellContainer.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_MAGENTA));
		{

			_ttContainer = new Composite(shellContainer, SWT.NONE);
			_ttContainer.setForeground(_fgColor);
			_ttContainer.setBackground(_bgColor);
			GridLayoutFactory.fillDefaults()//
					.numColumns(2)
					.equalWidth(true)
					.spacing(20, 5)
					.margins(margin, margin)
					.applyTo(_ttContainer);
//			_ttContainer.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_GREEN));
			{
				createUI10UpperPart(_ttContainer);
				createUI30LeftColumn(_ttContainer);
				createUI40RightColumn(_ttContainer);
				createUI50LowerPart(_ttContainer);
			}
		}

		return shellContainer;
	}

	private void createUI10UpperPart(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).span(2, 1).applyTo(container);
		container.setForeground(_fgColor);
		container.setBackground(_bgColor);
		GridLayoutFactory.fillDefaults()//
				.numColumns(2)
//				.spacing(5, 0)
				.applyTo(container);
//		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
		{
			/*
			 * title
			 */
			_lblTitle = new Label(container, SWT.LEAD | SWT.WRAP);
			GridDataFactory.fillDefaults()//
					.hint(MAX_DATA_WIDTH, SWT.DEFAULT)
					.grab(true, false)
					.applyTo(_lblTitle);
			_lblTitle.setFont(_boldFont);
			_lblTitle.setForeground(_fgColor);
			_lblTitle.setBackground(_bgColor);

			/*
			 * action toolbar in the top right corner
			 */
			createUI12Toolbar(container);

			/*
			 * date
			 */
			_lblDate = createUILabelValue(container, SWT.LEAD);
			GridDataFactory.fillDefaults().span(2, 1).applyTo(_lblDate);
		}
	}

	private void createUI12Toolbar(final Composite shellContainer) {

		/*
		 * create actions
		 */
		_actionEditTour = new ActionTourToolTipEditTour(this);
		_actionEditQuick = new ActionTourToolTipEditQuick(this);

		/*
		 * create toolbar
		 */
		final ToolBar toolbar = new ToolBar(shellContainer, SWT.FLAT);
		toolbar.setForeground(_fgColor);
		toolbar.setBackground(_bgColor);

		final ToolBarManager tbm = new ToolBarManager(toolbar);

		tbm.add(_actionEditTour);
		tbm.add(_actionEditQuick);

		tbm.update(true);
	}

	private void createUI30LeftColumn(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.BEGINNING).applyTo(container);
		container.setForeground(_fgColor);
		container.setBackground(_bgColor);
		GridLayoutFactory.fillDefaults().numColumns(3).spacing(5, 0).applyTo(container);
//		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
		{
			/*
			 * recording time
			 */
			Label label = createUILabel(container, Messages.Tour_Tooltip_Label_RecordingTime);
			_firstColumnControls.add(label);

			_lblRecordingTime = createUILabelValue(container, SWT.TRAIL);
			label = createUILabel(container, Messages.Tour_Tooltip_Label_Hour);

			// force this column to take the rest of the space
			GridDataFactory.fillDefaults().grab(true, false).applyTo(label);

			// moving time
			label = createUILabel(container, Messages.Tour_Tooltip_Label_MovingTime);
			_firstColumnControls.add(label);
			_lblMovingTime = createUILabelValue(container, SWT.TRAIL);
			createUILabel(container, Messages.Tour_Tooltip_Label_Hour);

			// break time
			label = createUILabel(container, Messages.Tour_Tooltip_Label_BreakTime);
			_firstColumnControls.add(label);
			_lblBreakTime = createUILabelValue(container, SWT.TRAIL);
			createUILabel(container, Messages.Tour_Tooltip_Label_Hour);

			// spacer
			label = createUILabel(container, null);
			GridDataFactory.fillDefaults().span(3, 1).applyTo(label);

			// temperature
			label = createUILabel(container, Messages.Tour_Tooltip_Label_Temperature);
			_firstColumnControls.add(label);
			_lblTemperature = createUILabelValue(container, SWT.TRAIL);
			createUILabel(container, UI.UNIT_LABEL_TEMPERATURE);

			// wind direction
			final int weatherWindDirDegree = _tourData.getWeatherWindDir();
//			_spinWindDirectionValue.setSelection(weatherWindDirDegree);
//			_comboWindDirectionText.select(getWindDirectionTextIndex(weatherWindDirDegree));

			// wind speed
			final int windSpeed = _tourData.getWeatherWindSpd();
//			final int speed = (int) (windSpeed / _unitValueDistance);
//			_spinWindSpeedValue.setSelection(speed);
//			_comboWindSpeedText.select(getWindSpeedTextIndex(speed));

			/*
			 * clouds
			 */
			label = createUILabel(container, Messages.Tour_Tooltip_Label_Clouds);
			_firstColumnControls.add(label);

			final Composite cloudContainer = new Composite(container, SWT.NONE);
			GridDataFactory.fillDefaults().span(2, 1).applyTo(cloudContainer);
			cloudContainer.setForeground(_fgColor);
			cloudContainer.setBackground(_bgColor);
			GridLayoutFactory.fillDefaults().numColumns(2).applyTo(cloudContainer);
			{
				// icon/text: clouds
				_lblClouds = new CLabel(cloudContainer, SWT.NONE);
				GridDataFactory.fillDefaults()//
//						.align(SWT.END, SWT.FILL)
						.grab(true, false)
						.applyTo(_lblClouds);

				_lblClouds.setForeground(_fgColor);
				_lblClouds.setBackground(_bgColor);
			}

		}
	}

	private void createUI40RightColumn(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.BEGINNING).applyTo(container);
		container.setForeground(_fgColor);
		container.setBackground(_bgColor);
		GridLayoutFactory.fillDefaults().numColumns(3).spacing(5, 0).applyTo(container);
//		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));
		{
			/*
			 * distance
			 */
			createUILabel(container, Messages.Tour_Tooltip_Label_Distance);
			_lblDistance = createUILabelValue(container, SWT.TRAIL);
			_lblDistanceUnit = createUILabelValue(container, SWT.LEAD);

			/*
			 * altitude up
			 */
			createUILabel(container, Messages.Tour_Tooltip_Label_AltitudeUp);
			_lblAltitudeUp = createUILabelValue(container, SWT.TRAIL);
			_lblAltitudeUpUnit = createUILabelValue(container, SWT.LEAD);

			/*
			 * altitude up
			 */
			createUILabel(container, Messages.Tour_Tooltip_Label_AltitudeDown);
			_lblAltitudeDown = createUILabelValue(container, SWT.TRAIL);
			_lblAltitudeDownUnit = createUILabelValue(container, SWT.LEAD);

			// spacer
			final Label label = createUILabel(container, null);
			GridDataFactory.fillDefaults().span(3, 1).applyTo(label);

			/*
			 * avg speed
			 */
			createUILabel(container, Messages.Tour_Tooltip_Label_AvgSpeed);
			_lblAvgSpeed = createUILabelValue(container, SWT.TRAIL);
			_lblAvgSpeedUnit = createUILabelValue(container, SWT.LEAD);

			/*
			 * avg pace
			 */
			createUILabel(container, Messages.Tour_Tooltip_Label_AvgPace);
			_lblAvgPace = createUILabelValue(container, SWT.TRAIL);
			_lblAvgPaceUnit = createUILabelValue(container, SWT.LEAD);
		}
	}

	private void createUI50LowerPart(final Composite parent) {

		Label label;

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).span(2, 1).applyTo(container);
		container.setForeground(_fgColor);
		container.setBackground(_bgColor);
		GridLayoutFactory.fillDefaults().numColumns(2).spacing(5, 0).applyTo(container);
//		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_CYAN));
		{
			/*
			 * tour type
			 */
			label = createUILabel(container, Messages.Tour_Tooltip_Label_TourType);
			_firstColumnControls.add(label);

			_lblTourType = new CLabel(container, SWT.NONE);
			_lblTourType.setForeground(_fgColor);
			_lblTourType.setBackground(_bgColor);
			GridDataFactory.swtDefaults()//
					.grab(true, false)
					.applyTo(_lblTourType);

			/*
			 * tags
			 */
			label = createUILabel(container, Messages.Tour_Tooltip_Label_Tags);
			GridDataFactory.fillDefaults().align(SWT.FILL, SWT.BEGINNING).applyTo(label);
			_firstColumnControls.add(label);

			_lblTourTags = createUILabelValue(container, SWT.LEAD | SWT.WRAP);
			GridDataFactory.fillDefaults()//
					.grab(true, false)
					.hint(MAX_DATA_WIDTH, SWT.DEFAULT)
					.applyTo(_lblTourTags);

			/*
			 * description
			 */
			_lblDescription = createUILabelValue(container, SWT.LEAD | SWT.WRAP);
			GridDataFactory.fillDefaults()//
					.span(2, 1)
					.indent(0, 20)
					.hint(MAX_TOOLTIP_WIDTH, SWT.DEFAULT)
					.applyTo(_lblDescription);
		}
	}

	private Label createUILabel(final Composite parent, final String labelText) {

		final Label label = new Label(parent, SWT.NONE);
		label.setForeground(_fgColor);
		label.setBackground(_bgColor);

		if (labelText != null) {
			label.setText(labelText);
		}

		return label;
	}

	private Label createUILabelValue(final Composite parent, final int style) {

		final Label label = new Label(parent, style);
		GridDataFactory.fillDefaults()//
//				.grab(true, false)
				.applyTo(label);
		label.setForeground(_fgColor);
		label.setBackground(_bgColor);

		return label;
	}

	private void enableControls() {

		final boolean isTourSaved = _tourData.isTourSaved();

		_actionEditQuick.setEnabled(isTourSaved);
		_actionEditTour.setEnabled(isTourSaved);
	}

//	@Override
//	public Point getLocation(final Point tipSize, final Event event) {
//
////		// try to position the tooltip at the bottom of the cell
////		ViewerCell cell = v.getCell(new Point(event.x, event.y));
////
////		if( cell != null ) {
////			return tree.toDisplay(event.x,cell.getBounds().y+cell.getBounds().height);
////		}
//
//		return super.getLocation(tipSize, event);
//	}

//	@Override
//	protected Object getToolTipArea(final Event event) {
//
////		// Ensure that the tooltip is hidden when the cell is left
////		return v.getCell(new Point(event.x, event.y));
//
//		return super.getToolTipArea(event);
//	}

	@Override
	public ArrayList<TourData> getSelectedTours() {

		final ArrayList<TourData> selectedTour = new ArrayList<TourData>();

		selectedTour.add(_tourData);

		return selectedTour;
	}

	private void onDispose() {
		_firstColumnControls.clear();
	}

	public void setTourData(final TourData tourData) {
		_tourData = tourData;
	}

	private void updateUI() {

		final DateTime dtTour = new DateTime(//
				_tourData.getStartYear(),
				_tourData.getStartMonth(),
				_tourData.getStartDay(),
				_tourData.getStartHour(),
				_tourData.getStartMinute(),
				_tourData.getStartSecond(),
				0);

		final int recordingTime = _tourData.getTourRecordingTime();
		final int movingTime = _tourData.getTourDrivingTime();
		final int breakTime = recordingTime - movingTime;

		final float altiUp = _tourData.getTourAltUp() / UI.UNIT_VALUE_ALTITUDE;
		final float altiDown = _tourData.getTourAltDown() / UI.UNIT_VALUE_ALTITUDE;
		final float distance = _tourData.getTourDistance() / UI.UNIT_VALUE_DISTANCE;

		final float speed = movingTime == 0 ? 0 : distance / (movingTime / 3.6f);
		final int pace = (int) (distance == 0 ? 0 : (movingTime * 1000 / distance));

		final TourType tourType = _tourData.getTourType();
		final String tourTypeName = tourType == null ? //
				UI.EMPTY_STRING
				: TourDatabase.getTourTypeName(tourType.getTypeId());

		String tourTitle = _tourData.getTourTitle();
		if (tourTitle == null || tourTitle.trim().length() == 0) {
			tourTitle = tourTypeName.length() > 0 ? tourTypeName : UI.EMPTY_STRING;
		}

		final String tourDescription = _tourData.getTourDescription();

		/*
		 * columns: left+right
		 */
		_lblTitle.setText(tourTitle);
		_lblDescription.setText(tourDescription);

		UI.updateUITags(_tourData, _lblTourTags);
		UI.updateUITourType(_tourData.getTourType(), _lblTourType);

		/*
		 * column: left
		 */
		_lblDate.setText(new Formatter().format(//
				Messages.Tour_Tooltip_Format_DateWeekTime,
				_dateFormatter.print(dtTour.getMillis()),
				dtTour.getWeekOfWeekyear(),
				_timeFormatter.print(dtTour.getMillis())
		//
				)
				.toString());

		_lblRecordingTime.setText(new Formatter().format(
				Messages.Tour_Tooltip_Format_Date,
				recordingTime / 3600,
				(recordingTime % 3600) / 60,
				(recordingTime % 3600) % 60)//
				.toString());

		_lblMovingTime.setText(new Formatter().format(
				Messages.Tour_Tooltip_Format_Date,
				movingTime / 3600,
				(movingTime % 3600) / 60,
				(movingTime % 3600) % 60)//
				.toString());

		_lblBreakTime.setText(new Formatter().format(
				Messages.Tour_Tooltip_Format_Date,
				breakTime / 3600,
				(breakTime % 3600) / 60,
				(breakTime % 3600) % 60)//
				.toString());

		// temperature
		int temperature = _tourData.getAvgTemperature();
		if (UI.UNIT_VALUE_TEMPERATURE != 1) {
			temperature = (int) (temperature * UI.UNIT_FAHRENHEIT_MULTI + UI.UNIT_FAHRENHEIT_ADD);
		}
		_lblTemperature.setText(Integer.toString(temperature));

		// weather
		final int weatherIndex = _tourData.getWeatherIndex();
		final String cloudImageName = IWeather.cloudIcon[weatherIndex];

		_lblClouds.setImage(UI.IMAGE_REGISTRY.get(cloudImageName));
		_lblClouds.setText(IWeather.cloudText[weatherIndex]);

		/*
		 * column: right
		 */
		_lblDistance.setText(_nf3.format(distance / 1000));
		_lblDistanceUnit.setText(UI.UNIT_LABEL_DISTANCE);

		_lblAltitudeUp.setText(Integer.toString((int) altiUp));
		_lblAltitudeUpUnit.setText(UI.UNIT_LABEL_ALTITUDE);

		_lblAltitudeDown.setText(Integer.toString((int) altiDown));
		_lblAltitudeDownUnit.setText(UI.UNIT_LABEL_ALTITUDE);

		_lblAvgSpeed.setText(_nf1.format(speed));
		_lblAvgSpeedUnit.setText(UI.UNIT_LABEL_SPEED);

		_lblAvgPace.setText(new Formatter().format(//
				Messages.Tour_Tooltip_Format_Pace,
				pace / 60,
				pace % 60)//
				.toString());
		_lblAvgPaceUnit.setText(UI.UNIT_LABEL_PACE);

	}
}
