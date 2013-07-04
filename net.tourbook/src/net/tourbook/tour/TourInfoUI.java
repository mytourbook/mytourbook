/*******************************************************************************
 * Copyright (C) 2005, 2013  Wolfgang Schramm and Contributors
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
package net.tourbook.tour;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Set;

import net.tourbook.common.UI;
import net.tourbook.common.util.ITourToolTipProvider;
import net.tourbook.common.util.Util;
import net.tourbook.common.weather.IWeather;
import net.tourbook.data.TourData;
import net.tourbook.data.TourTag;
import net.tourbook.data.TourType;
import net.tourbook.database.TourDatabase;
import net.tourbook.ui.ITourProvider;
import net.tourbook.ui.Messages;
import net.tourbook.ui.action.ActionTourToolTipEditQuick;
import net.tourbook.ui.action.ActionTourToolTipEditTour;

import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolBar;
import org.joda.time.DateTime;
import org.joda.time.Period;
import org.joda.time.PeriodType;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.PeriodFormatter;
import org.joda.time.format.PeriodFormatterBuilder;

public class TourInfoUI {

	private static final int			SHELL_MARGIN			= 5;
	private static final int			MAX_DATA_WIDTH			= 300;

	private Color						_bgColor;
	private Color						_fgColor;
	private Font						_boldFont;

	private final DateTimeFormatter		_dateFormatter			= DateTimeFormat.fullDate();
	private final DateTimeFormatter		_timeFormatter			= DateTimeFormat.mediumTime();
	private final DateTimeFormatter		_dtFormatterCreated		= DateTimeFormat.mediumDateTime();

	private final DateTimeFormatter		_dtHistoryFormatter		= DateTimeFormat.forStyle("FM");	//$NON-NLS-1$
//	private final DateTimeFormatter		_dtWeekday				= DateTimeFormat.forPattern("E");	//$NON-NLS-1$

	private final NumberFormat			_nf1					= NumberFormat.getInstance();
	private final NumberFormat			_nf3					= NumberFormat.getInstance();

	{
		_nf1.setMinimumFractionDigits(1);
		_nf1.setMaximumFractionDigits(1);
		_nf3.setMinimumFractionDigits(3);
		_nf3.setMaximumFractionDigits(3);
	}

	private static PeriodType			_tourPeriodTemplate		= PeriodType.yearMonthDayTime()
																// hide these components
																		.withMinutesRemoved()
																		.withSecondsRemoved()
																		.withMillisRemoved();

	private final PeriodFormatter		_durationFormatter		= new PeriodFormatterBuilder()
																		.appendYears()
																		.appendSuffix("y ", "y ") //$NON-NLS-1$ //$NON-NLS-2$
																		.appendMonths()
																		.appendSuffix("m ", "m ") //$NON-NLS-1$ //$NON-NLS-2$
																		.appendDays()
																		.appendSuffix("d ", "d ") //$NON-NLS-1$ //$NON-NLS-2$
																		.appendHours()
																		.appendSuffix("h ", "h ") //$NON-NLS-1$ //$NON-NLS-2$
																		.toFormatter();

	private boolean						_hasTourType;
	private boolean						_hasWeather;
	private boolean						_hasTags;
	private boolean						_hasDescription;

	private boolean						_isActionsVisible		= false;

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
	private CLabel						_lblTourType;
	private Label						_lblTourTypeText;
	private Text						_txtWeather;
	private Label						_lblTourTags;
	private Text						_txtDescription;

	/*
	 * 1. column
	 */
	private Label						_lblRecordingTime;
	private Label						_lblMovingTime;
	private Label						_lblBreakTime;
	private Label						_lblRecordingTimeHour;
	private Label						_lblMovingTimeHour;
	private Label						_lblBreakTimeHour;

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

	private Label						_lblDateTimeCreatedValue;
	private Label						_lblDateTimeModifiedValue;
	private Label						_lblDateTimeModified;

	/*
	 * actions
	 */
	private ActionTourToolTipEditTour	_actionEditTour;
	private ActionTourToolTipEditQuick	_actionEditQuick;

	/**
	 * Tour which is displayed in the tool tip
	 */
	private TourData					_tourData;

	/*
	 * fields which are optionally displayed when they are not null
	 */
	private DateTime					_uiDtCreated;
	private DateTime					_uiDtModified;
	private String						_uiTourTypeName;

	private ITourToolTipProvider		_tourInfoToolTipProvider;
	private ITourProvider				_tourProvider;

	public Composite createContentArea(	final Composite parent,
										final TourData tourData,
										final ITourToolTipProvider tourInfoToolTipProvider,
										final ITourProvider tourProvider) {

		_tourData = tourData;
		_tourInfoToolTipProvider = tourInfoToolTipProvider;
		_tourProvider = tourProvider;

		final Display display = parent.getDisplay();

		_bgColor = display.getSystemColor(SWT.COLOR_INFO_BACKGROUND);
		_fgColor = display.getSystemColor(SWT.COLOR_INFO_FOREGROUND);
		_boldFont = JFaceResources.getFontRegistry().getBold(JFaceResources.DIALOG_FONT);

		final Set<TourTag> tourTags = _tourData.getTourTags();
		final String tourDescription = _tourData.getTourDescription();

		// date/time created/modified
		_uiDtCreated = _tourData.getDateTimeCreated();
		_uiDtModified = _tourData.getDateTimeModified();

		final TourType tourType = _tourData.getTourType();
		_uiTourTypeName = tourType == null ? //
				null
				: TourDatabase.getTourTypeName(tourType.getTypeId());

		_hasTags = tourTags != null && tourTags.size() > 0;
		_hasTourType = tourType != null;
		_hasDescription = tourDescription != null && tourDescription.length() > 0;
		_hasWeather = _tourData.getWeather().length() > 0;

		final Composite container = createUI(parent);

		updateUI();
		enableControls();

		// compute width for all controls and equalize column width for the different sections
		_ttContainer.layout(true, true);
		UI.setEqualizeColumWidths(_firstColumnControls, 5);
		UI.setEqualizeColumWidths(_secondColumnControls);

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
//		shellContainer.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_GREEN));
		{
			_ttContainer = new Composite(shellContainer, SWT.NONE);
			_ttContainer.setForeground(_fgColor);
			_ttContainer.setBackground(_bgColor);
			GridLayoutFactory.fillDefaults() //
					.numColumns(2)
					.equalWidth(true)
					.margins(SHELL_MARGIN, SHELL_MARGIN)
					.applyTo(_ttContainer);
//			_ttContainer.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
			{
				createUI10UpperPart(_ttContainer);

				createUI30LeftColumn(_ttContainer);
				createUI40RightColumn(_ttContainer);
				createUI50LowerPart(_ttContainer);
				createUI60CreateModifyTime(_ttContainer);
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
			if (_uiTourTypeName != null) {

				_lblTourType = new CLabel(container, SWT.NONE);
				GridDataFactory.swtDefaults()//
						.align(SWT.BEGINNING, SWT.BEGINNING)
						.applyTo(_lblTourType);
				_lblTourType.setForeground(_fgColor);
				_lblTourType.setBackground(_bgColor);
			}

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

		if (_isActionsVisible == false) {
			// actions are not enabled
			return;
		}

		/*
		 * create actions
		 */
		_actionEditTour = new ActionTourToolTipEditTour(_tourInfoToolTipProvider, _tourProvider);
		_actionEditQuick = new ActionTourToolTipEditQuick(_tourInfoToolTipProvider, _tourProvider);

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
//		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_MAGENTA));
		{
			/*
			 * recording time
			 */
			Label label = createUILabel(container, Messages.Tour_Tooltip_Label_RecordingTime);
			_firstColumnControls.add(label);

			_lblRecordingTime = createUILabelValue(container, SWT.TRAIL);
			_secondColumnControls.add(_lblRecordingTime);

			_lblRecordingTimeHour = createUILabel(container, Messages.Tour_Tooltip_Label_Hour);

			// force this column to take the rest of the space
			GridDataFactory.fillDefaults().grab(true, false).applyTo(_lblRecordingTimeHour);

			/*
			 * moving time
			 */
			label = createUILabel(container, Messages.Tour_Tooltip_Label_MovingTime);
			_firstColumnControls.add(label);

			_lblMovingTime = createUILabelValue(container, SWT.TRAIL);
			_secondColumnControls.add(_lblMovingTime);

			_lblMovingTimeHour = createUILabel(container, Messages.Tour_Tooltip_Label_Hour);

			/*
			 * break time
			 */
			label = createUILabel(container, Messages.Tour_Tooltip_Label_BreakTime);
			_firstColumnControls.add(label);

			_lblBreakTime = createUILabelValue(container, SWT.TRAIL);
			_secondColumnControls.add(_lblBreakTime);

			_lblBreakTimeHour = createUILabel(container, Messages.Tour_Tooltip_Label_Hour);

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
		}
	}

	private void createUI50LowerPart(final Composite parent) {

		if (_hasTags == false && _hasDescription == false && _hasWeather == false && _hasTourType == false) {
			return;
		}

		Label label;
		final PixelConverter pc = new PixelConverter(parent);

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
			if (_hasTourType) {

				label = createUILabel(container, Messages.Tour_Tooltip_Label_TourType);
				GridDataFactory.fillDefaults().align(SWT.FILL, SWT.BEGINNING).applyTo(label);
				_firstColumnControls.add(label);

				_lblTourTypeText = createUILabelValue(container, SWT.LEAD | SWT.WRAP);
				GridDataFactory.fillDefaults()//
						.grab(true, false)
						.hint(MAX_DATA_WIDTH, SWT.DEFAULT)
						.applyTo(_lblTourTypeText);
			}

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
			 * weather
			 */
			if (_hasWeather) {

				label = createUILabel(container, Messages.Tour_Tooltip_Label_Weather);
				GridDataFactory.fillDefaults()//
						.span(2, 1)
						.indent(0, 5)
						.applyTo(label);

				_txtWeather = new Text(container, SWT.WRAP | SWT.MULTI | SWT.READ_ONLY | SWT.BORDER);
				GridDataFactory.fillDefaults()//
						.span(2, 1)
						.grab(true, false)
						.hint(pc.convertWidthInCharsToPixels(80), SWT.DEFAULT)
						.applyTo(_txtWeather);

				_txtWeather.setForeground(_fgColor);
				_txtWeather.setBackground(_bgColor);
			}

			/*
			 * description
			 */
			if (_hasDescription) {

				// label
				label = createUILabel(container, Messages.Tour_Tooltip_Label_Description);
				GridDataFactory.fillDefaults()//
						.span(2, 1)
						.indent(0, 5)
						.applyTo(label);

				// text field
				int style;
				final int lineCount = Util.countCharacter(_tourData.getTourDescription(), '\n');

				if (lineCount > 10) {
					style = SWT.WRAP | SWT.MULTI | SWT.READ_ONLY | SWT.BORDER | SWT.V_SCROLL;
				} else {
					style = SWT.WRAP | SWT.MULTI | SWT.READ_ONLY | SWT.BORDER;
				}

				_txtDescription = new Text(container, style);
				GridDataFactory.fillDefaults()//
						.span(2, 1)
						.grab(true, false)
						.hint(pc.convertWidthInCharsToPixels(80), SWT.DEFAULT)
						.applyTo(_txtDescription);

				if (lineCount > 15) {
					final GridData gd = (GridData) _txtDescription.getLayoutData();
					gd.heightHint = pc.convertHeightInCharsToPixels(15);
				}

				_txtDescription.setForeground(_fgColor);
				_txtDescription.setBackground(_bgColor);
			}
		}
	}

	private void createUI60CreateModifyTime(final Composite parent) {

		if (_uiDtCreated == null && _uiDtModified == null) {
			return;
		}

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).span(2, 1).applyTo(container);
		container.setForeground(_fgColor);
		container.setBackground(_bgColor);
		GridLayoutFactory.fillDefaults()//
				.numColumns(2)
				.equalWidth(true)
				.spacing(20, 5)
				.applyTo(container);
//		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));
		{
			final Composite containerCreated = new Composite(container, SWT.NONE);
			GridDataFactory.fillDefaults().grab(true, false).applyTo(containerCreated);
			containerCreated.setForeground(_fgColor);
			containerCreated.setBackground(_bgColor);
			GridLayoutFactory.fillDefaults().numColumns(2).applyTo(containerCreated);
			{
				/*
				 * date/time created
				 */
				createUILabel(containerCreated, Messages.Tour_Tooltip_Label_DateTimeCreated);

				_lblDateTimeCreatedValue = createUILabelValue(containerCreated, SWT.LEAD);
				GridDataFactory.fillDefaults().applyTo(_lblDateTimeCreatedValue);
			}

			final Composite containerModified = new Composite(container, SWT.NONE);
			GridDataFactory.fillDefaults().grab(true, false).applyTo(containerModified);
			containerModified.setForeground(_fgColor);
			containerModified.setBackground(_bgColor);
			GridLayoutFactory.fillDefaults().numColumns(2).applyTo(containerModified);
			{
				/*
				 * date/time modified
				 */
				_lblDateTimeModified = createUILabel(containerModified, Messages.Tour_Tooltip_Label_DateTimeModified);
				GridDataFactory.fillDefaults()//
						.grab(true, false)
						.align(SWT.END, SWT.FILL)
						.applyTo(_lblDateTimeModified);

				_lblDateTimeModifiedValue = createUILabelValue(containerModified, SWT.TRAIL);
				GridDataFactory.fillDefaults()//
						.align(SWT.END, SWT.FILL)
						.applyTo(_lblDateTimeModifiedValue);
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
		GridDataFactory.fillDefaults().applyTo(label);
		label.setForeground(_fgColor);
		label.setBackground(_bgColor);

		return label;
	}

	public Composite createUINoData(final Composite parent) {

		final Display display = parent.getDisplay();

		final Color bgColor = display.getSystemColor(SWT.COLOR_INFO_BACKGROUND);
		final Color fgColor = display.getSystemColor(SWT.COLOR_INFO_FOREGROUND);

		/*
		 * shell container is necessary because the margins of the inner container will hide the
		 * tooltip when the mouse is hovered, which is not as it should be.
		 */
		final Composite shellContainer = new Composite(parent, SWT.NONE);
		shellContainer.setForeground(fgColor);
		shellContainer.setBackground(bgColor);
		GridLayoutFactory.fillDefaults().applyTo(shellContainer);
		{

			final Composite container = new Composite(shellContainer, SWT.NONE);
			container.setForeground(fgColor);
			container.setBackground(bgColor);
			GridLayoutFactory.fillDefaults()//
					.margins(SHELL_MARGIN, SHELL_MARGIN)
					.applyTo(container);
			{
				final Label label = new Label(container, SWT.NONE);
				label.setText(Messages.Tour_Tooltip_Label_NoTour);
				label.setForeground(fgColor);
				label.setBackground(bgColor);
			}
		}

		return shellContainer;
	}

	public void dispose() {

		_firstColumnControls.clear();
		_secondColumnControls.clear();
	}

	private void enableControls() {

		if (_isActionsVisible == false) {
			return;
		}

		final boolean isTourSaved = _tourData.isTourSaved();

		_actionEditQuick.setEnabled(isTourSaved);
		_actionEditTour.setEnabled(true);
	}

	private int getWindDirectionTextIndex(final int degreeDirection) {

		final float degree = (degreeDirection + 22.5f) / 45.0f;

		final int directionIndex = ((int) degree) % 8;

		return directionIndex;
	}

	private int getWindSpeedTextIndex(final int speed) {

		final int[] unitValueWindSpeed = net.tourbook.ui.UI.UNIT_VALUE_DISTANCE == 1
				? IWeather.windSpeedKmh
				: IWeather.windSpeedMph;

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

	/**
	 * Enable/disable tour edit actions, actions are disabled by default
	 * 
	 * @param isEnabled
	 */
	public void setActionsEnabled(final boolean isEnabled) {
		_isActionsVisible = isEnabled;
	}

	private void updateUI() {

		/*
		 * upper/lower part
		 */
		if (_lblTourType != null && _lblTourType.isDisposed() == false) {
			_lblTourType.setToolTipText(_uiTourTypeName);
			net.tourbook.ui.UI.updateUITourType(_tourData, _lblTourType, false);
		}

		String tourTitle = _tourData.getTourTitle();
		if (tourTitle == null || tourTitle.trim().length() == 0) {
			tourTitle = Messages.Tour_Tooltip_Label_DefaultTitle;
		}
		_lblTitle.setText(tourTitle);

		if (_hasWeather) {
			_txtWeather.setText(_tourData.getWeather());
		}
		if (_hasTourType) {
			_lblTourTypeText.setText(_tourData.getTourType().getName());
		}
		if (_hasTags) {
			net.tourbook.ui.UI.updateUITags(_tourData, _lblTourTags);
		}
		if (_hasDescription) {
			_txtDescription.setText(_tourData.getTourDescription());
		}

		/*
		 * column: left
		 */
		final long recordingTime = _tourData.getTourRecordingTime();
		final long movingTime = _tourData.getTourDrivingTime();
		final long breakTime = recordingTime - movingTime;

		final DateTime dtTourStart = _tourData.getTourStartTime();
		final DateTime dtTourEnd = dtTourStart.plus(recordingTime * 1000);

		final boolean isHistory = recordingTime < net.tourbook.common.UI.DAY_IN_SECONDS;

		if (isHistory) {

			// < 1 day

			_lblDate.setText(String.format(//
					Messages.Tour_Tooltip_Format_DateWeekTime,
					_dateFormatter.print(dtTourStart.getMillis()),
					_timeFormatter.print(dtTourStart.getMillis()),
					_timeFormatter.print(dtTourEnd.getMillis()),
					dtTourStart.getWeekOfWeekyear()));

			_lblRecordingTimeHour.setVisible(true);
			_lblMovingTimeHour.setVisible(true);
			_lblBreakTimeHour.setVisible(true);

			_lblRecordingTime.setText(String.format(
					Messages.Tour_Tooltip_Format_Date,
					recordingTime / 3600,
					(recordingTime % 3600) / 60,
					(recordingTime % 3600) % 60)//
					);

			_lblMovingTime.setText(String.format(
					Messages.Tour_Tooltip_Format_Date,
					movingTime / 3600,
					(movingTime % 3600) / 60,
					(movingTime % 3600) % 60)//
					);

			_lblBreakTime.setText(String.format(
					Messages.Tour_Tooltip_Format_Date,
					breakTime / 3600,
					(breakTime % 3600) / 60,
					(breakTime % 3600) % 60)//
					);

		} else {

			// > 1 day

			_lblDate.setText(String.format(//
					Messages.Tour_Tooltip_Format_HistoryDateTime,
					_dtHistoryFormatter.print(dtTourStart.getMillis()),
					_dtHistoryFormatter.print(dtTourEnd.getMillis())
			//
					));

			_lblRecordingTimeHour.setVisible(false);
			_lblMovingTimeHour.setVisible(false);
			_lblBreakTimeHour.setVisible(false);

			final Period tourPeriod = new Period(dtTourStart, dtTourEnd, _tourPeriodTemplate);

			_lblRecordingTime.setText(tourPeriod.toString(_durationFormatter));
			_lblMovingTime.setText(UI.EMPTY_STRING);
			_lblBreakTime.setText(UI.EMPTY_STRING);
		}

		int windSpeed = _tourData.getWeatherWindSpeed();
		windSpeed = (int) (windSpeed / net.tourbook.ui.UI.UNIT_VALUE_DISTANCE);

		_lblWindSpeed.setText(Integer.toString(windSpeed));
		_lblWindSpeedUnit.setText(String.format(
				Messages.Tour_Tooltip_Format_WindSpeedUnit,
				UI.UNIT_LABEL_SPEED,
				IWeather.windSpeedTextShort[getWindSpeedTextIndex(windSpeed)]));

		// wind direction
		final int weatherWindDirDegree = _tourData.getWeatherWindDir();
		_lblWindDirection.setText(Integer.toString(weatherWindDirDegree));
		_lblWindDirectionUnit.setText(String.format(
				Messages.Tour_Tooltip_Format_WindDirectionUnit,
				IWeather.windDirectionText[getWindDirectionTextIndex(weatherWindDirDegree)]));

		// temperature
		float temperature = _tourData.getAvgTemperature();
		if (net.tourbook.ui.UI.UNIT_VALUE_TEMPERATURE != 1) {
			temperature = temperature
					* net.tourbook.ui.UI.UNIT_FAHRENHEIT_MULTI
					+ net.tourbook.ui.UI.UNIT_FAHRENHEIT_ADD;
		}
		_lblTemperature.setText(_nf1.format(temperature));

		// weather clouds
		final int weatherIndex = _tourData.getWeatherIndex();
		final String cloudText = IWeather.cloudText[weatherIndex];
		final String cloudImageName = IWeather.cloudIcon[weatherIndex];

		_lblClouds.setImage(net.tourbook.common.UI.IMAGE_REGISTRY.get(cloudImageName));
		_lblCloudsUnit.setText(cloudText.equals(IWeather.cloudIsNotDefined) ? UI.EMPTY_STRING : cloudText);

		/*
		 * column: right
		 */
		final float distance = _tourData.getTourDistance() / net.tourbook.ui.UI.UNIT_VALUE_DISTANCE;

		_lblDistance.setText(_nf3.format(distance / 1000));
		_lblDistanceUnit.setText(UI.UNIT_LABEL_DISTANCE);

		_lblAltitudeUp
				.setText(Integer.toString((int) (_tourData.getTourAltUp() / net.tourbook.ui.UI.UNIT_VALUE_ALTITUDE)));
		_lblAltitudeUpUnit.setText(UI.UNIT_LABEL_ALTITUDE);

		_lblAltitudeDown
				.setText(Integer.toString((int) (_tourData.getTourAltDown() / net.tourbook.ui.UI.UNIT_VALUE_ALTITUDE)));
		_lblAltitudeDownUnit.setText(UI.UNIT_LABEL_ALTITUDE);

		_lblAvgSpeed.setText(_nf1.format(movingTime == 0 ? 0 : distance / (movingTime / 3.6f)));
		_lblAvgSpeedUnit.setText(UI.UNIT_LABEL_SPEED);

		final int pace = (int) (distance == 0 ? 0 : (movingTime * 1000 / distance));
		_lblAvgPace.setText(String.format(//
				Messages.Tour_Tooltip_Format_Pace,
				pace / 60,
				pace % 60)//
				);
		_lblAvgPaceUnit.setText(UI.UNIT_LABEL_PACE);

		// avg pulse
		_lblAvgPulse.setText(_nf1.format(_tourData.getAvgPulse()));
		_lblAvgPulseUnit.setText(Messages.Value_Unit_Pulse);

		// avg cadence
		_lblAvgCadence.setText(_nf1.format(_tourData.getAvgCadence()));
		_lblAvgCadenceUnit.setText(Messages.Value_Unit_Cadence);

		_lblCalories.setText(Integer.toString(_tourData.getCalories()));
		_lblRestPulse.setText(Integer.toString(_tourData.getRestPulse()));

		/*
		 * date/time
		 */

		// date/time created
		if (_uiDtCreated != null) {

			_lblDateTimeCreatedValue.setText(_uiDtCreated == null ? //
					UI.EMPTY_STRING
					: _dtFormatterCreated.print(_uiDtCreated.getMillis()));
		}

		// date/time modified
		if (_uiDtModified != null) {

			_lblDateTimeModifiedValue.setText(_uiDtModified == null ? //
					UI.EMPTY_STRING
					: _dtFormatterCreated.print(_uiDtModified.getMillis()));
		}
	}
}
