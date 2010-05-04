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
import java.util.Set;

import net.tourbook.data.IWeather;
import net.tourbook.data.TourData;
import net.tourbook.data.TourTag;
import net.tourbook.data.TourType;
import net.tourbook.database.TourDatabase;
import net.tourbook.tour.TourManager;
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

public class TourToolTip extends ToolTip implements ITourProvider {

	private static final int			SHELL_MARGIN			= 5;
	private static final int			MAX_TOOLTIP_WIDTH		= 400;
	private static final int			MAX_DATA_WIDTH			= 300;

	private TourData					_tourData;
	private long						_tourId					= -1;

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
	private final ArrayList<Control>	_secondColumnControls	= new ArrayList<Control>();

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

	private Label						_lblWindSpeed;
	private Label						_lblWindSpeedUnit;
	private Label						_lblWindDirection;
	private Label						_lblWindDirectionUnit;
	private Label						_lblTemperature;
	private CLabel						_lblClouds;
	private Label						_lblCloudsUnit;

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
	private Label						_lblAvgPulse;
	private Label						_lblAvgPulseUnit;
	private Label						_lblAvgCadence;
	private Label						_lblAvgCadenceUnit;

	private Label						_lblCalories;
	private Label						_lblRestPulse;

	private boolean						_hasTags;
	private boolean						_hasDescription;

	public TourToolTip(final Control control) {
		this(control, NO_RECREATE, false);
	}

	public TourToolTip(final Control control, final int style, final boolean manualActivation) {
		super(control, style, manualActivation);
	}

	@Override
	protected Composite createToolTipContentArea(final Event event, final Composite parent) {

		final Display display = parent.getDisplay();
		_bgColor = display.getSystemColor(SWT.COLOR_INFO_BACKGROUND);
		_fgColor = display.getSystemColor(SWT.COLOR_INFO_FOREGROUND);
		_boldFont = JFaceResources.getFontRegistry().getBold(JFaceResources.DIALOG_FONT);

		Composite container;

		if (_tourId != -1) {
			// tour id is set
			_tourData = TourManager.getInstance().getTourData(_tourId);
		}

		if (_tourData == null) {

			// there are no data available

			container = createUINoData(parent);

		} else {

			// tour data is available

			if (_tourData != null) {

				final Set<TourTag> tourTags = _tourData.getTourTags();
				final String tourDescription = _tourData.getTourDescription();

				_hasTags = tourTags != null && tourTags.size() > 0;
				_hasDescription = tourDescription != null && tourDescription.length() > 0;
			}

			container = createUI(parent);

			updateUI();
			enableControls();

			// compute width for all controls and equalize column width for the different sections
			_ttContainer.layout(true, true);
			UI.setEqualizeColumWidths(_firstColumnControls, 5);
			UI.setEqualizeColumWidths(_secondColumnControls);

			// allow the actions to be selected
			setHideOnMouseDown(false);
		}

		parent.addDisposeListener(new DisposeListener() {
			@Override
			public void widgetDisposed(final DisposeEvent e) {
				onDispose();
			}
		});

		return container;
	}

	private Composite createUI(final Composite parent) {

		/*
		 * shell container is necessary because the margins of the inner container will hide the
		 * tooltip when the mouse is hovered, which is not as it should be.
		 */
		final Composite shellContainer = new Composite(parent, SWT.NONE);
		shellContainer.setForeground(_fgColor);
		shellContainer.setBackground(_bgColor);
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
					.margins(SHELL_MARGIN, SHELL_MARGIN)
					.applyTo(_ttContainer);
//			_ttContainer.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_GREEN));
			{
				createUI10UpperPart(_ttContainer);

				createUI30LeftColumn(_ttContainer);
				createUI40RightColumn(_ttContainer);

				if (_hasTags || _hasDescription) {
					createUI50LowerPart(_ttContainer);
				}
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
				.numColumns(3)
				.applyTo(container);
//		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
		{
			/*
			 * tour type
			 */
			_lblTourType = new CLabel(container, SWT.NONE);
			GridDataFactory.swtDefaults()//
					.align(SWT.BEGINNING, SWT.BEGINNING)
					.applyTo(_lblTourType);
			_lblTourType.setForeground(_fgColor);
			_lblTourType.setBackground(_bgColor);

			/*
			 * title
			 */
			_lblTitle = new Label(container, SWT.LEAD | SWT.WRAP);
			GridDataFactory.fillDefaults()//
					.hint(MAX_DATA_WIDTH, SWT.DEFAULT)
					.grab(true, false)
					.align(SWT.FILL, SWT.CENTER)
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
			GridDataFactory.fillDefaults().span(3, 1).applyTo(_lblDate);
		}
	}

	private void createUI12Toolbar(final Composite container) {

		/*
		 * create actions
		 */
		_actionEditTour = new ActionTourToolTipEditTour(this);
		_actionEditQuick = new ActionTourToolTipEditQuick(this);

		/*
		 * create toolbar
		 */
		final ToolBar toolbar = new ToolBar(container, SWT.FLAT);
		GridDataFactory.fillDefaults().applyTo(toolbar);
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
			_secondColumnControls.add(_lblRecordingTime);

			label = createUILabel(container, Messages.Tour_Tooltip_Label_Hour);

			// force this column to take the rest of the space
			GridDataFactory.fillDefaults().grab(true, false).applyTo(label);

			/*
			 * moving time
			 */
			label = createUILabel(container, Messages.Tour_Tooltip_Label_MovingTime);
			_firstColumnControls.add(label);

			_lblMovingTime = createUILabelValue(container, SWT.TRAIL);
			_secondColumnControls.add(_lblMovingTime);

			createUILabel(container, Messages.Tour_Tooltip_Label_Hour);

			/*
			 * break time
			 */
			label = createUILabel(container, Messages.Tour_Tooltip_Label_BreakTime);
			_firstColumnControls.add(label);

			_lblBreakTime = createUILabelValue(container, SWT.TRAIL);
			_secondColumnControls.add(_lblBreakTime);

			createUILabel(container, Messages.Tour_Tooltip_Label_Hour);

			// ----------------- spacer ----------------
			label = createUILabel(container, null);
			GridDataFactory.fillDefaults().span(3, 1).applyTo(label);

			/*
			 * distance
			 */
			label = createUILabel(container, Messages.Tour_Tooltip_Label_Distance);
			_firstColumnControls.add(label);

			_lblDistance = createUILabelValue(container, SWT.TRAIL);
			_secondColumnControls.add(_lblDistance);

			_lblDistanceUnit = createUILabelValue(container, SWT.LEAD);

			/*
			 * altitude up
			 */
			label = createUILabel(container, Messages.Tour_Tooltip_Label_AltitudeUp);
			_firstColumnControls.add(label);

			_lblAltitudeUp = createUILabelValue(container, SWT.TRAIL);
			_secondColumnControls.add(_lblAltitudeUp);

			_lblAltitudeUpUnit = createUILabelValue(container, SWT.LEAD);

			/*
			 * altitude up
			 */
			label = createUILabel(container, Messages.Tour_Tooltip_Label_AltitudeDown);
			_firstColumnControls.add(label);

			_lblAltitudeDown = createUILabelValue(container, SWT.TRAIL);
			_secondColumnControls.add(_lblAltitudeDown);

			_lblAltitudeDownUnit = createUILabelValue(container, SWT.LEAD);

			// ----------------- spacer ----------------
			label = createUILabel(container, null);
			GridDataFactory.fillDefaults().span(3, 1).applyTo(label);

			/*
			 * calories
			 */
			label = createUILabel(container, Messages.Tour_Tooltip_Label_Calories);
			_firstColumnControls.add(label);

			_lblCalories = createUILabelValue(container, SWT.TRAIL);
			_secondColumnControls.add(_lblCalories);
			createUILabel(container, Messages.Value_Unit_Calories);

			/*
			 * rest pulse
			 */
			label = createUILabel(container, Messages.Tour_Tooltip_Label_RestPulse);
			_firstColumnControls.add(label);

			_lblRestPulse = createUILabelValue(container, SWT.TRAIL);
			_secondColumnControls.add(_lblRestPulse);

			createUILabel(container, Messages.Value_Unit_Pulse);
		}
	}

	private void createUI40RightColumn(final Composite parent) {

		Label label;

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.BEGINNING).applyTo(container);
		container.setForeground(_fgColor);
		container.setBackground(_bgColor);
		GridLayoutFactory.fillDefaults().numColumns(3).spacing(5, 0).applyTo(container);
//		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));
		{
			/*
			 * avg speed
			 */
			label = createUILabel(container, Messages.Tour_Tooltip_Label_AvgSpeed);
			_firstColumnControls.add(label);

			_lblAvgSpeed = createUILabelValue(container, SWT.TRAIL);
			_secondColumnControls.add(_lblAvgSpeed);

			_lblAvgSpeedUnit = createUILabelValue(container, SWT.LEAD);

			/*
			 * avg pace
			 */
			label = createUILabel(container, Messages.Tour_Tooltip_Label_AvgPace);
			_firstColumnControls.add(label);

			_lblAvgPace = createUILabelValue(container, SWT.TRAIL);
			_secondColumnControls.add(_lblAvgPace);

			_lblAvgPaceUnit = createUILabelValue(container, SWT.LEAD);

			/*
			 * avg pulse
			 */
			label = createUILabel(container, Messages.Tour_Tooltip_Label_AvgPulse);
			_firstColumnControls.add(label);

			_lblAvgPulse = createUILabelValue(container, SWT.TRAIL);
			_secondColumnControls.add(_lblAvgPulse);

			_lblAvgPulseUnit = createUILabelValue(container, SWT.LEAD);

			/*
			 * avg cadence
			 */
			label = createUILabel(container, Messages.Tour_Tooltip_Label_AvgCadence);
			_firstColumnControls.add(label);

			_lblAvgCadence = createUILabelValue(container, SWT.TRAIL);
			_secondColumnControls.add(_lblAvgCadence);

			_lblAvgCadenceUnit = createUILabelValue(container, SWT.LEAD);

			// ########### spacer ##########
			label = createUILabel(container, null);
			GridDataFactory.fillDefaults().span(3, 1).applyTo(label);

			/*
			 * temperature
			 */
			label = createUILabel(container, Messages.Tour_Tooltip_Label_Temperature);
			_firstColumnControls.add(label);

			_lblTemperature = createUILabelValue(container, SWT.TRAIL);
			_secondColumnControls.add(_lblTemperature);

			createUILabel(container, UI.UNIT_LABEL_TEMPERATURE);
			/*
			 * wind speed
			 */
			label = createUILabel(container, Messages.Tour_Tooltip_Label_WindSpeed);
			_firstColumnControls.add(label);

			_lblWindSpeed = createUILabelValue(container, SWT.TRAIL);
			_secondColumnControls.add(_lblWindSpeed);

			_lblWindSpeedUnit = createUILabelValue(container, SWT.LEAD);

			/*
			 * wind direction
			 */
			label = createUILabel(container, Messages.Tour_Tooltip_Label_WindDirection);
			_firstColumnControls.add(label);

			_lblWindDirection = createUILabelValue(container, SWT.TRAIL);
			_secondColumnControls.add(_lblWindDirection);

			_lblWindDirectionUnit = createUILabelValue(container, SWT.LEAD);

			/*
			 * clouds
			 */
			label = createUILabel(container, Messages.Tour_Tooltip_Label_Clouds);
			_firstColumnControls.add(label);

			// icon: clouds
			_lblClouds = new CLabel(container, SWT.TRAIL);
			GridDataFactory.fillDefaults().align(SWT.END, SWT.FILL).applyTo(_lblClouds);
			_lblClouds.setForeground(_fgColor);
			_lblClouds.setBackground(_bgColor);

			// text: clouds
			_lblCloudsUnit = createUILabelValue(container, SWT.LEAD);
			GridDataFactory.swtDefaults().applyTo(_lblCloudsUnit);
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
			 * tags
			 */
			if (_hasTags) {

				label = createUILabel(container, Messages.Tour_Tooltip_Label_Tags);
				GridDataFactory.fillDefaults().align(SWT.FILL, SWT.BEGINNING).applyTo(label);
				_firstColumnControls.add(label);

				_lblTourTags = createUILabelValue(container, SWT.LEAD | SWT.WRAP);
				GridDataFactory.fillDefaults()//
						.grab(true, false)
						.hint(MAX_DATA_WIDTH, SWT.DEFAULT)
						.applyTo(_lblTourTags);
			}

			/*
			 * description
			 */
			if (_hasDescription) {

				label = createUILabel(container, Messages.Tour_Tooltip_Label_Description);
				GridDataFactory.fillDefaults()//
						.span(2, 1)
						.indent(0, 5)
						.applyTo(label);

				_lblDescription = createUILabelValue(container, SWT.LEAD | SWT.WRAP);
				GridDataFactory.fillDefaults()//
						.span(2, 1)
						.indent(0, 5)
						.hint(MAX_TOOLTIP_WIDTH, SWT.DEFAULT)
						.applyTo(_lblDescription);
			}
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
				.applyTo(label);
		label.setForeground(_fgColor);
		label.setBackground(_bgColor);

		return label;
	}

	private Composite createUINoData(final Composite parent) {

		/*
		 * shell container is necessary because the margins of the inner container will hide the
		 * tooltip when the mouse is hovered, which is not as it should be.
		 */
		final Composite shellContainer = new Composite(parent, SWT.NONE);
		shellContainer.setForeground(_fgColor);
		shellContainer.setBackground(_bgColor);
		GridLayoutFactory.fillDefaults().applyTo(shellContainer);
		{

			final Composite container = new Composite(shellContainer, SWT.NONE);
			container.setForeground(_fgColor);
			container.setBackground(_bgColor);
			GridLayoutFactory.fillDefaults()//
					.margins(SHELL_MARGIN, SHELL_MARGIN)
					.applyTo(container);
			{
				final Label label = createUILabel(container, Messages.Tour_Tooltip_Label_Tags);
				label.setText(Messages.Tour_Tooltip_Label_NoTour);
			}
		}

		return shellContainer;
	}

	private void enableControls() {

		final boolean isTourSaved = _tourData.isTourSaved();

		_actionEditQuick.setEnabled(isTourSaved);
		_actionEditTour.setEnabled(true);
	}

	@Override
	public ArrayList<TourData> getSelectedTours() {

		final ArrayList<TourData> selectedTour = new ArrayList<TourData>();

		if (_tourData == null) {
			_tourData = TourManager.getInstance().getTourData(_tourId);
		}

		selectedTour.add(_tourData);

		return selectedTour;
	}

	private int getWindDirectionTextIndex(final int degreeDirection) {

		final float degree = (degreeDirection + 22.5f) / 45.0f;

		final int directionIndex = ((int) degree) % 8;

		return directionIndex;
	}

	private int getWindSpeedTextIndex(final int speed) {

		final int[] unitValueWindSpeed = UI.UNIT_VALUE_DISTANCE == 1 ? IWeather.windSpeedKmh : IWeather.windSpeedMph;

		// set speed to max index value
		int speedValueIndex = unitValueWindSpeed.length - 1;

		for (int speedIndex = 0; speedIndex < unitValueWindSpeed.length; speedIndex++) {

			final int speedMaxValue = unitValueWindSpeed[speedIndex];

			if (speed <= speedMaxValue) {
				speedValueIndex = speedIndex;
				break;
			}
		}

		return speedValueIndex;
	}

	private void onDispose() {
		_firstColumnControls.clear();
		_secondColumnControls.clear();
	}

	public void setTourData(final ArrayList<TourData> tourDataList) {

		if (tourDataList == null || tourDataList.size() == 0) {
			_tourData = null;
		} else {
			_tourData = tourDataList.get(0);
		}

		_tourId = -1;
	}

	public void setTourId(final long tourId) {
		_tourId = tourId;
		_tourData = null;
	}

	private void updateUI() {

		/*
		 * upper/lower part
		 */
		final TourType tourType = _tourData.getTourType();
		final String tourTypeName = tourType == null ? //
				UI.EMPTY_STRING
				: TourDatabase.getTourTypeName(tourType.getTypeId());

		String tourTitle = _tourData.getTourTitle();
		if (tourTitle == null || tourTitle.trim().length() == 0) {
			tourTitle = tourTypeName.length() > 0 ? tourTypeName : UI.EMPTY_STRING;
		}

		final String tourDescription = _tourData.getTourDescription();

		_lblTitle.setText(tourTitle);
		_lblTourType.setToolTipText(tourTypeName);

		UI.updateUITourType(tourType, _lblTourType, false);

		if (_hasTags) {
			UI.updateUITags(_tourData, _lblTourTags);
		}
		if (_hasDescription) {
			_lblDescription.setText(tourDescription);
		}

		/*
		 * column: left
		 */
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

		_lblDate.setText(new Formatter().format(//
				Messages.Tour_Tooltip_Format_DateWeekTime,
				_dateFormatter.print(dtTour.getMillis()),
				_timeFormatter.print(dtTour.getMillis()),
				dtTour.getWeekOfWeekyear())//
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

		int windSpeed = _tourData.getWeatherWindSpeed();
		windSpeed = (int) (windSpeed / UI.UNIT_VALUE_DISTANCE);

		_lblWindSpeed.setText(Integer.toString(windSpeed));
		_lblWindSpeedUnit.setText(new Formatter().format(
				Messages.Tour_Tooltip_Format_WindSpeedUnit,
				UI.UNIT_LABEL_SPEED,
				IWeather.windSpeedTextShort[getWindSpeedTextIndex(windSpeed)]).toString());

		// wind direction
		final int weatherWindDirDegree = _tourData.getWeatherWindDir();
		_lblWindDirection.setText(Integer.toString(weatherWindDirDegree));
		_lblWindDirectionUnit.setText(new Formatter().format(
				Messages.Tour_Tooltip_Format_WindDirectionUnit,
				IWeather.windDirectionText[getWindDirectionTextIndex(weatherWindDirDegree)]).toString());

		// temperature
		int temperature = _tourData.getAvgTemperature();
		if (UI.UNIT_VALUE_TEMPERATURE != 1) {
			temperature = (int) (temperature * UI.UNIT_FAHRENHEIT_MULTI + UI.UNIT_FAHRENHEIT_ADD);
		}
		_lblTemperature.setText(Integer.toString(temperature));

		// weather clouds
		final int weatherIndex = _tourData.getWeatherIndex();
		final String cloudText = IWeather.cloudText[weatherIndex];
		final String cloudImageName = IWeather.cloudIcon[weatherIndex];

		_lblClouds.setImage(UI.IMAGE_REGISTRY.get(cloudImageName));
		_lblCloudsUnit.setText(cloudText.equals(IWeather.cloudIsNotDefined) ? UI.EMPTY_STRING : cloudText);

		/*
		 * column: right
		 */
		final float distance = _tourData.getTourDistance() / UI.UNIT_VALUE_DISTANCE;

		_lblDistance.setText(_nf3.format(distance / 1000));
		_lblDistanceUnit.setText(UI.UNIT_LABEL_DISTANCE);

		_lblAltitudeUp.setText(Integer.toString((int) (_tourData.getTourAltUp() / UI.UNIT_VALUE_ALTITUDE)));
		_lblAltitudeUpUnit.setText(UI.UNIT_LABEL_ALTITUDE);

		_lblAltitudeDown.setText(Integer.toString((int) (_tourData.getTourAltDown() / UI.UNIT_VALUE_ALTITUDE)));
		_lblAltitudeDownUnit.setText(UI.UNIT_LABEL_ALTITUDE);

		_lblAvgSpeed.setText(_nf1.format(movingTime == 0 ? 0 : distance / (movingTime / 3.6f)));
		_lblAvgSpeedUnit.setText(UI.UNIT_LABEL_SPEED);

		final int pace = (int) (distance == 0 ? 0 : (movingTime * 1000 / distance));
		_lblAvgPace.setText(new Formatter().format(//
				Messages.Tour_Tooltip_Format_Pace,
				pace / 60,
				pace % 60)//
				.toString());
		_lblAvgPaceUnit.setText(UI.UNIT_LABEL_PACE);

		// avg pulse
		_lblAvgPulse.setText(Integer.toString(_tourData.getAvgPulse()));
		_lblAvgPulseUnit.setText(Messages.Value_Unit_Pulse);

		// avg cadence
		_lblAvgCadence.setText(Integer.toString(_tourData.getAvgCadence()));
		_lblAvgCadenceUnit.setText(Messages.Value_Unit_Cadence);

		_lblCalories.setText(Integer.toString(_tourData.getCalories()));
		_lblRestPulse.setText(Integer.toString(_tourData.getRestPulse()));

	}
}
