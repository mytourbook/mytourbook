/*******************************************************************************
 * Copyright (C) 2005, 2018 Wolfgang Schramm and Contributors
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
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Set;

import net.tourbook.common.CommonActivator;
import net.tourbook.common.UI;
import net.tourbook.common.font.MTFont;
import net.tourbook.common.formatter.FormatManager;
import net.tourbook.common.preferences.ICommonPreferences;
import net.tourbook.common.time.TimeTools;
import net.tourbook.common.time.TourDateTime;
import net.tourbook.common.util.IToolTipProvider;
import net.tourbook.common.util.Util;
import net.tourbook.common.weather.IWeather;
import net.tourbook.data.TourData;
import net.tourbook.data.TourTag;
import net.tourbook.data.TourType;
import net.tourbook.database.TourDatabase;
import net.tourbook.preferences.PrefPageAppearanceDisplayFormat;
import net.tourbook.ui.ITourProvider;
import net.tourbook.ui.Messages;
import net.tourbook.ui.action.ActionTourToolTip_EditPreferences;
import net.tourbook.ui.action.ActionTourToolTip_EditQuick;
import net.tourbook.ui.action.ActionTourToolTip_EditTour;

import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.LayoutConstants;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolBar;
import org.joda.time.Period;
import org.joda.time.PeriodType;

public class TourInfoUI {

	private static final int				SHELL_MARGIN		= 5;
	private static final int				MAX_DATA_WIDTH		= 300;

	private static final String				REAR_SHIFT_FORMAT	= "/  ";								//$NON-NLS-1$

	private final static IPreferenceStore	_prefStoreCommon	= CommonActivator.getPrefStore();

	public static final DateTimeFormatter	_dtHistoryFormatter	= DateTimeFormatter.ofLocalizedDateTime(
			FormatStyle.FULL,
			FormatStyle.MEDIUM);

	private static PeriodType				_tourPeriodTemplate	= PeriodType.yearMonthDayTime()
	// hide these components
//																		.withMinutesRemoved()
			.withSecondsRemoved()
			.withMillisRemoved()
//
	;
	private final NumberFormat				_nf0				= NumberFormat.getNumberInstance();
	private final NumberFormat				_nf1				= NumberFormat.getInstance();
	private final NumberFormat				_nf2				= NumberFormat.getInstance();
	private final NumberFormat				_nf3				= NumberFormat.getInstance();

	{
		_nf0.setMinimumFractionDigits(0);
		_nf0.setMaximumFractionDigits(0);

		_nf1.setMinimumFractionDigits(1);
		_nf1.setMaximumFractionDigits(1);

		_nf2.setMinimumFractionDigits(2);
		_nf2.setMaximumFractionDigits(2);

		_nf3.setMinimumFractionDigits(3);
		_nf3.setMaximumFractionDigits(3);
	}

	private boolean								_hasDescription;
	private boolean								_hasGears;
	private boolean								_hasRunDyn;
	private boolean								_hasTags;
	private boolean								_hasTourType;
	private boolean								_hasWeather;

	/*
	 * actions
	 */
	private ActionTourToolTip_EditTour			_actionEditTour;
	private ActionTourToolTip_EditQuick			_actionEditQuick;
	private ActionTourToolTip_EditPreferences	_actionPrefDialog;

	private boolean								_isActionsVisible	= false;

	/**
	 * Tour which is displayed in the tool tip
	 */
	private TourData							_tourData;

	private String								_noTourTooltip		= Messages.Tour_Tooltip_Label_NoTour;

	/*
	 * fields which are optionally displayed when they are not null
	 */
	private ZonedDateTime						_uiDtCreated;
	private ZonedDateTime						_uiDtModified;
	private String								_uiTourTypeName;

	private IToolTipProvider					_tourToolTipProvider;
	private ITourProvider						_tourProvider;

	/*
	 * UI resources
	 */
	private Color								_bgColor;
	private Color								_fgColor;

	/*
	 * UI controls
	 */
	private Composite							_ttContainer;

	private ControlDecoration					_decoTimeZone;

	private Text								_txtDescription;
	private Text								_txtWeather;

	private CLabel								_lblClouds;
	private CLabel								_lblTourType;

	private Label								_lblAltitudeUp;
	private Label								_lblAltitudeUpUnit;
	private Label								_lblAltitudeDown;
	private Label								_lblAltitudeDownUnit;
	private Label								_lblAvgSpeed;
	private Label								_lblAvgSpeedUnit;
	private Label								_lblAvgPace;
	private Label								_lblAvgPaceUnit;
	private Label								_lblAvgPulse;
	private Label								_lblAvgPulseUnit;
	private Label								_lblAvgCadence;
	private Label								_lblAvgCadenceUnit;
	private Label								_lblAvg_Power;
	private Label								_lblAvg_PowerUnit;
	private Label								_lblBodyWeight;
	private Label								_lblBreakTime;
	private Label								_lblBreakTimeHour;
	private Label								_lblCalories;
	private Label								_lblCloudsUnit;
	private Label								_lblDate;
	private Label								_lblDateTimeCreatedValue;
	private Label								_lblDateTimeModifiedValue;
	private Label								_lblDateTimeModified;
	private Label								_lblDistance;
	private Label								_lblDistanceUnit;
	private Label								_lblGearFrontShifts;
	private Label								_lblGearRearShifts;
	private Label								_lblMaxAltitude;
	private Label								_lblMaxAltitudeUnit;
	private Label								_lblMaxPulse;
	private Label								_lblMaxPulseUnit;
	private Label								_lblMaxSpeed;
	private Label								_lblMaxSpeedUnit;
	private Label								_lblMovingTime;
	private Label								_lblMovingTimeHour;
	private Label								_lblRecordingTime;
	private Label								_lblRecordingTimeHour;
	private Label								_lblRestPulse;
	private Label								_lblTemperature;
	private Label								_lblTimeZone_Value;
	private Label								_lblTimeZoneDifference;
	private Label								_lblTimeZoneDifference_Value;
	private Label								_lblTitle;
	private Label								_lblTourTags;
	private Label								_lblTourTypeText;
	private Label								_lblWindSpeed;
	private Label								_lblWindSpeedUnit;
	private Label								_lblWindDirection;
	private Label								_lblWindDirectionUnit;
	private Label								_lblRunDyn_StanceTime_Min;
	private Label								_lblRunDyn_StanceTime_Min_Unit;
	private Label								_lblRunDyn_StanceTime_Max;
	private Label								_lblRunDyn_StanceTime_Max_Unit;
	private Label								_lblRunDyn_StanceTime_Avg;
	private Label								_lblRunDyn_StanceTime_Avg_Unit;
	private Label								_lblRunDyn_StanceTimeBalance_Min;
	private Label								_lblRunDyn_StanceTimeBalance_Min_Unit;
	private Label								_lblRunDyn_StanceTimeBalance_Max;
	private Label								_lblRunDyn_StanceTimeBalance_Max_Unit;
	private Label								_lblRunDyn_StanceTimeBalance_Avg;
	private Label								_lblRunDyn_StanceTimeBalance_Avg_Unit;
	private Label								_lblRunDyn_StepLength_Min;
	private Label								_lblRunDyn_StepLength_Min_Unit;
	private Label								_lblRunDyn_StepLength_Max;
	private Label								_lblRunDyn_StepLength_Max_Unit;
	private Label								_lblRunDyn_StepLength_Avg;
	private Label								_lblRunDyn_StepLength_Avg_Unit;
	private Label								_lblRunDyn_VerticalOscillation_Min;
	private Label								_lblRunDyn_VerticalOscillation_Min_Unit;
	private Label								_lblRunDyn_VerticalOscillation_Max;
	private Label								_lblRunDyn_VerticalOscillation_Max_Unit;
	private Label								_lblRunDyn_VerticalOscillation_Avg;
	private Label								_lblRunDyn_VerticalOscillation_Avg_Unit;
	private Label								_lblRunDyn_VerticalRatio_Min;
	private Label								_lblRunDyn_VerticalRatio_Min_Unit;
	private Label								_lblRunDyn_VerticalRatio_Max;
	private Label								_lblRunDyn_VerticalRatio_Max_Unit;
	private Label								_lblRunDyn_VerticalRatio_Avg;
	private Label								_lblRunDyn_VerticalRatio_Avg_Unit;

	/**
	 * Run tour action quick edit.
	 */
	public void actionQuickEditTour() {

		_actionEditQuick.run();
	}

	public Composite createContentArea(	final Composite parent,
										final TourData tourData,
										final IToolTipProvider tourToolTipProvider,
										final ITourProvider tourProvider) {

		_tourData = tourData;
		_tourToolTipProvider = tourToolTipProvider;
		_tourProvider = tourProvider;

		final Display display = parent.getDisplay();

		_bgColor = display.getSystemColor(SWT.COLOR_INFO_BACKGROUND);
		_fgColor = display.getSystemColor(SWT.COLOR_INFO_FOREGROUND);

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
		_hasGears = _tourData.getFrontShiftCount() > 0 || _tourData.getRearShiftCount() > 0;

		_hasRunDyn = _tourData.isRunDynAvailable();

		final Composite container = createUI(parent);

//		_ttContainer.setRedraw(false);

		updateUI();
		updateUI_Layout();

		enableControls();

//		_ttContainer.setRedraw(true);

		return container;
	}

	private Composite createUI(final Composite parent) {

		final Point defaultSpacing = LayoutConstants.getSpacing();
		final int columnSpacing = defaultSpacing.x + 30;

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
			GridLayoutFactory
					.fillDefaults() //
					.margins(SHELL_MARGIN, SHELL_MARGIN)
					.applyTo(_ttContainer);
//			_ttContainer.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
			{
				createUI_10_UpperPart(_ttContainer);

				final Composite container = new Composite(_ttContainer, SWT.NONE);
				container.setBackground(_bgColor);
				GridDataFactory.fillDefaults()
//						.grab(true, false)
						.applyTo(container);

				if (_hasRunDyn) {

					GridLayoutFactory.fillDefaults()
							.numColumns(3)
							.spacing(columnSpacing, defaultSpacing.y)
							.applyTo(container);
					{
						createUI_30_Column_1(container);
						createUI_40_Column_2(container);
						createUI_50_Column_3(container);
					}

				} else {

					GridLayoutFactory.fillDefaults()
							.numColumns(2)
							.spacing(columnSpacing, defaultSpacing.y)
							.applyTo(container);
					{
						createUI_30_Column_1(container);
						createUI_40_Column_2(container);
					}
				}

				createUI_90_LowerPart(_ttContainer);
				createUI_92_CreateModifyTime(_ttContainer);
			}
		}

		return shellContainer;
	}

	private void createUI_10_UpperPart(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		container.setForeground(_fgColor);
		container.setBackground(_bgColor);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
		GridLayoutFactory
				.fillDefaults()//
				.numColumns(3)
				.applyTo(container);
//		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
		{
			/*
			 * tour type
			 */
			if (_uiTourTypeName != null) {

				_lblTourType = new CLabel(container, SWT.NONE);
				GridDataFactory
						.swtDefaults()//
						.align(SWT.BEGINNING, SWT.BEGINNING)
						.applyTo(_lblTourType);
				_lblTourType.setForeground(_fgColor);
				_lblTourType.setBackground(_bgColor);
			}

			/*
			 * title
			 */
			_lblTitle = new Label(container, SWT.LEAD | SWT.WRAP);
			GridDataFactory
					.fillDefaults()//
					.hint(MAX_DATA_WIDTH, SWT.DEFAULT)
					.grab(true, false)
					.align(SWT.FILL, SWT.CENTER)
					.applyTo(_lblTitle);
			_lblTitle.setForeground(_fgColor);
			_lblTitle.setBackground(_bgColor);
			MTFont.setBannerFont(_lblTitle);

			/*
			 * action toolbar in the top right corner
			 */
			createUI_12_Toolbar(container);

			/*
			 * date
			 */
			_lblDate = createUI_LabelValue(container, SWT.LEAD);
			GridDataFactory.fillDefaults().span(3, 1).applyTo(_lblDate);
		}
	}

	private void createUI_12_Toolbar(final Composite container) {

		if (_isActionsVisible == false) {
			// actions are not enabled
			return;
		}

		/*
		 * create actions
		 */
		_actionEditTour = new ActionTourToolTip_EditTour(_tourToolTipProvider, _tourProvider);
		_actionEditQuick = new ActionTourToolTip_EditQuick(_tourToolTipProvider, _tourProvider);
		_actionPrefDialog = new ActionTourToolTip_EditPreferences(
				_tourToolTipProvider,
				Messages.Tour_Tooltip_Action_EditFormatPreferences,
				PrefPageAppearanceDisplayFormat.ID);

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
		tbm.add(_actionPrefDialog);

		tbm.update(true);
	}

	private void createUI_30_Column_1(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.BEGINNING).applyTo(container);
		container.setForeground(_fgColor);
		container.setBackground(_bgColor);
		GridLayoutFactory.fillDefaults().numColumns(3).spacing(5, 0).applyTo(container);
//		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_MAGENTA));
		{
			createUI_32_Time(container);
			createUI_34_DistanceAltitude(container);
			createUI_36_Misc(container);

			if (_hasGears) {
				createUI_Spacer(container);
				createUI_46_Gears(container);
			}
		}
	}

	private void createUI_32_Time(final Composite container) {

		{
			/*
			 * recording time
			 */
			createUI_Label(container, Messages.Tour_Tooltip_Label_RecordingTime);

			_lblRecordingTime = createUI_LabelValue(container, SWT.TRAIL);
			_lblRecordingTimeHour = createUI_Label(container, Messages.Tour_Tooltip_Label_Hour);

			// force this column to take the rest of the space
			GridDataFactory.fillDefaults().grab(true, false).applyTo(_lblRecordingTimeHour);
		}

		{
			/*
			 * moving time
			 */
			createUI_Label(container, Messages.Tour_Tooltip_Label_MovingTime);

			_lblMovingTime = createUI_LabelValue(container, SWT.TRAIL);
			_lblMovingTimeHour = createUI_Label(container, Messages.Tour_Tooltip_Label_Hour);
		}

		{
			/*
			 * break time
			 */
			createUI_Label(container, Messages.Tour_Tooltip_Label_BreakTime);

			_lblBreakTime = createUI_LabelValue(container, SWT.TRAIL);
			_lblBreakTimeHour = createUI_Label(container, Messages.Tour_Tooltip_Label_Hour);
		}

		if (isSimpleTour()) {

			createUI_Spacer(container);

			{
				/*
				 * Timezone difference
				 */

				_lblTimeZoneDifference = createUI_Label(container, Messages.Tour_Tooltip_Label_TimeZoneDifference);

				/*
				 * Add decoration
				 */
				final Image infoImage = FieldDecorationRegistry
						.getDefault()
						.getFieldDecoration(FieldDecorationRegistry.DEC_INFORMATION)
						.getImage();

				_decoTimeZone = new ControlDecoration(_lblTimeZoneDifference, SWT.TOP | SWT.RIGHT);
				_decoTimeZone.setImage(infoImage);

				_lblTimeZoneDifference_Value = createUI_LabelValue(container, SWT.TRAIL);

				// hour
				createUI_Label(container, Messages.Tour_Tooltip_Label_Hour);
			}
			{
				/*
				 * Timezone
				 */
				createUI_Label(container, Messages.Tour_Tooltip_Label_TimeZone);

				_lblTimeZone_Value = createUI_LabelValue(container, SWT.TRAIL);

				// spacer
				createUI_LabelValue(container, SWT.TRAIL);
			}
		}
	}

	private void createUI_34_DistanceAltitude(final Composite container) {

		createUI_Spacer(container);

		/*
		 * distance
		 */
		createUI_Label(container, Messages.Tour_Tooltip_Label_Distance);

		_lblDistance = createUI_LabelValue(container, SWT.TRAIL);
		_lblDistanceUnit = createUI_LabelValue(container, SWT.LEAD);

		/*
		 * altitude up
		 */
		createUI_Label(container, Messages.Tour_Tooltip_Label_AltitudeUp);

		_lblAltitudeUp = createUI_LabelValue(container, SWT.TRAIL);
		_lblAltitudeUpUnit = createUI_LabelValue(container, SWT.LEAD);

		/*
		 * altitude up
		 */
		createUI_Label(container, Messages.Tour_Tooltip_Label_AltitudeDown);

		_lblAltitudeDown = createUI_LabelValue(container, SWT.TRAIL);
		_lblAltitudeDownUnit = createUI_LabelValue(container, SWT.LEAD);

		createUI_Spacer(container);
	}

	private void createUI_36_Misc(final Composite container) {

		{
			/*
			 * calories
			 */
			createUI_Label(container, Messages.Tour_Tooltip_Label_Calories);

			_lblCalories = createUI_LabelValue(container, SWT.TRAIL);

			createUI_Label(container, Messages.Value_Unit_KCalories);
		}

		{
			/*
			 * rest pulse
			 */
			createUI_Label(container, Messages.Tour_Tooltip_Label_RestPulse);

			_lblRestPulse = createUI_LabelValue(container, SWT.TRAIL);

			createUI_Label(container, Messages.Value_Unit_Pulse);
		}
		{
			/*
			 * Body weight
			 */
			createUI_Label(container, Messages.Tour_Tooltip_Label_BodyWeight);

			_lblBodyWeight = createUI_LabelValue(container, SWT.TRAIL);

			createUI_Label(container, UI.UNIT_WEIGHT_KG);
		}
	}

	private void createUI_40_Column_2(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.BEGINNING).applyTo(container);
		container.setForeground(_fgColor);
		container.setBackground(_bgColor);
		GridLayoutFactory.fillDefaults().numColumns(3).spacing(5, 0).applyTo(container);
//		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));
		{
			createUI_42_Avg(container);

			createUI_Spacer(container);
			createUI_43_Max(container);

			createUI_Spacer(container);
			createUI_44_Weather(container);
		}
	}

	private void createUI_42_Avg(final Composite parent) {

		/*
		 * avg pulse
		 */
		createUI_Label(parent, Messages.Tour_Tooltip_Label_AvgPulse);

		_lblAvgPulse = createUI_LabelValue(parent, SWT.TRAIL);
		_lblAvgPulseUnit = createUI_LabelValue(parent, SWT.LEAD);

		/*
		 * avg speed
		 */
		createUI_Label(parent, Messages.Tour_Tooltip_Label_AvgSpeed);

		_lblAvgSpeed = createUI_LabelValue(parent, SWT.TRAIL);
		_lblAvgSpeedUnit = createUI_LabelValue(parent, SWT.LEAD);

		/*
		 * avg pace
		 */
		createUI_Label(parent, Messages.Tour_Tooltip_Label_AvgPace);

		_lblAvgPace = createUI_LabelValue(parent, SWT.TRAIL);
		_lblAvgPaceUnit = createUI_LabelValue(parent, SWT.LEAD);

		/*
		 * avg cadence
		 */
		createUI_Label(parent, Messages.Tour_Tooltip_Label_AvgCadence);

		_lblAvgCadence = createUI_LabelValue(parent, SWT.TRAIL);
		_lblAvgCadenceUnit = createUI_LabelValue(parent, SWT.LEAD);

		/*
		 * avg power
		 */
		createUI_Label(parent, Messages.Tour_Tooltip_Label_AvgPower);

		_lblAvg_Power = createUI_LabelValue(parent, SWT.TRAIL);
		_lblAvg_PowerUnit = createUI_LabelValue(parent, SWT.LEAD);
	}

	private void createUI_43_Max(final Composite container) {

		/*
		 * max pulse
		 */
		createUI_Label(container, Messages.Tour_Tooltip_Label_MaxPulse);

		_lblMaxPulse = createUI_LabelValue(container, SWT.TRAIL);
		_lblMaxPulseUnit = createUI_LabelValue(container, SWT.LEAD);

		/*
		 * max speed
		 */
		createUI_Label(container, Messages.Tour_Tooltip_Label_MaxSpeed);

		_lblMaxSpeed = createUI_LabelValue(container, SWT.TRAIL);
		_lblMaxSpeedUnit = createUI_LabelValue(container, SWT.LEAD);

		/*
		 * max altitude
		 */
		createUI_Label(container, Messages.Tour_Tooltip_Label_MaxAltitude);

		_lblMaxAltitude = createUI_LabelValue(container, SWT.TRAIL);
		_lblMaxAltitudeUnit = createUI_LabelValue(container, SWT.LEAD);
	}

	private void createUI_44_Weather(final Composite parent) {

		/*
		 * clouds
		 */
		createUI_Label(parent, Messages.Tour_Tooltip_Label_Clouds);

		// icon: clouds
		_lblClouds = new CLabel(parent, SWT.TRAIL);
		GridDataFactory.fillDefaults().align(SWT.END, SWT.FILL).applyTo(_lblClouds);
		_lblClouds.setForeground(_fgColor);
		_lblClouds.setBackground(_bgColor);

		// text: clouds
		_lblCloudsUnit = createUI_LabelValue(parent, SWT.LEAD);
		GridDataFactory.swtDefaults().applyTo(_lblCloudsUnit);

		/*
		 * temperature
		 */
		createUI_Label(parent, Messages.Tour_Tooltip_Label_Temperature);

		_lblTemperature = createUI_LabelValue(parent, SWT.TRAIL);

		createUI_Label(parent, UI.UNIT_LABEL_TEMPERATURE);

		/*
		 * wind speed
		 */
		createUI_Label(parent, Messages.Tour_Tooltip_Label_WindSpeed);

		_lblWindSpeed = createUI_LabelValue(parent, SWT.TRAIL);
		_lblWindSpeedUnit = createUI_LabelValue(parent, SWT.LEAD);

		/*
		 * wind direction
		 */
		createUI_Label(parent, Messages.Tour_Tooltip_Label_WindDirection);

		_lblWindDirection = createUI_LabelValue(parent, SWT.TRAIL);
		_lblWindDirectionUnit = createUI_LabelValue(parent, SWT.LEAD);
	}

	private void createUI_46_Gears(final Composite parent) {

		/*
		 * Front/rear gear shifts
		 */
		createUI_Label(parent, Messages.Tour_Tooltip_Label_GearShifts);

		_lblGearFrontShifts = createUI_LabelValue(parent, SWT.TRAIL);
		_lblGearRearShifts = createUI_LabelValue(parent, SWT.LEAD);
	}

	private void createUI_50_Column_3(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.BEGINNING).applyTo(container);
		container.setForeground(_fgColor);
		container.setBackground(_bgColor);
		GridLayoutFactory.fillDefaults().numColumns(3).spacing(5, 0).applyTo(container);
//		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));
		{
			createUI_52_RunDyn(container);
		}
	}

	private void createUI_52_RunDyn(final Composite parent) {

		{
			/*
			 * Stance time
			 */

			{
				/*
				 * Min
				 */
				createUI_Label(parent, Messages.Tour_Tooltip_Label_RunDyn_StanceTime_Min);

				_lblRunDyn_StanceTime_Min = createUI_LabelValue(parent, SWT.TRAIL);
				_lblRunDyn_StanceTime_Min_Unit = createUI_LabelValue(parent, SWT.LEAD);
			}
			{
				/*
				 * Max
				 */
				createUI_Label(parent, Messages.Tour_Tooltip_Label_RunDyn_StanceTime_Max);

				_lblRunDyn_StanceTime_Max = createUI_LabelValue(parent, SWT.TRAIL);
				_lblRunDyn_StanceTime_Max_Unit = createUI_LabelValue(parent, SWT.LEAD);
			}
			{
				/*
				 * Avg
				 */
				createUI_Label(parent, Messages.Tour_Tooltip_Label_RunDyn_StanceTime_Avg);

				_lblRunDyn_StanceTime_Avg = createUI_LabelValue(parent, SWT.TRAIL);
				_lblRunDyn_StanceTime_Avg_Unit = createUI_LabelValue(parent, SWT.LEAD);
			}
		}

		createUI_Spacer(parent);

		{
			/*
			 * Stance Time Balance
			 */

			{
				/*
				 * Min
				 */
				createUI_Label(parent, Messages.Tour_Tooltip_Label_RunDyn_StanceTimeBalance_Min);

				_lblRunDyn_StanceTimeBalance_Min = createUI_LabelValue(parent, SWT.TRAIL);
				_lblRunDyn_StanceTimeBalance_Min_Unit = createUI_LabelValue(parent, SWT.LEAD);
			}
			{
				/*
				 * Max
				 */
				createUI_Label(parent, Messages.Tour_Tooltip_Label_RunDyn_StanceTimeBalance_Max);

				_lblRunDyn_StanceTimeBalance_Max = createUI_LabelValue(parent, SWT.TRAIL);
				_lblRunDyn_StanceTimeBalance_Max_Unit = createUI_LabelValue(parent, SWT.LEAD);
			}
			{
				/*
				 * Avg
				 */
				createUI_Label(parent, Messages.Tour_Tooltip_Label_RunDyn_StanceTimeBalance_Avg);

				_lblRunDyn_StanceTimeBalance_Avg = createUI_LabelValue(parent, SWT.TRAIL);
				_lblRunDyn_StanceTimeBalance_Avg_Unit = createUI_LabelValue(parent, SWT.LEAD);
			}
		}

		createUI_Spacer(parent);

		{
			/*
			 * Step Length
			 */

			{
				/*
				 * Min
				 */
				createUI_Label(parent, Messages.Tour_Tooltip_Label_RunDyn_StepLength_Min);

				_lblRunDyn_StepLength_Min = createUI_LabelValue(parent, SWT.TRAIL);
				_lblRunDyn_StepLength_Min_Unit = createUI_LabelValue(parent, SWT.LEAD);
			}
			{
				/*
				 * Max
				 */
				createUI_Label(parent, Messages.Tour_Tooltip_Label_RunDyn_StepLength_Max);

				_lblRunDyn_StepLength_Max = createUI_LabelValue(parent, SWT.TRAIL);
				_lblRunDyn_StepLength_Max_Unit = createUI_LabelValue(parent, SWT.LEAD);
			}
			{
				/*
				 * Avg
				 */
				createUI_Label(parent, Messages.Tour_Tooltip_Label_RunDyn_StepLength_Avg);

				_lblRunDyn_StepLength_Avg = createUI_LabelValue(parent, SWT.TRAIL);
				_lblRunDyn_StepLength_Avg_Unit = createUI_LabelValue(parent, SWT.LEAD);
			}
		}

		createUI_Spacer(parent);

		{
			/*
			 * Vertical Oscillation
			 */

			{
				/*
				 * Min
				 */
				createUI_Label(parent, Messages.Tour_Tooltip_Label_RunDyn_VerticalOscillation_Min);

				_lblRunDyn_VerticalOscillation_Min = createUI_LabelValue(parent, SWT.TRAIL);
				_lblRunDyn_VerticalOscillation_Min_Unit = createUI_LabelValue(parent, SWT.LEAD);
			}
			{
				/*
				 * Max
				 */
				createUI_Label(parent, Messages.Tour_Tooltip_Label_RunDyn_VerticalOscillation_Max);

				_lblRunDyn_VerticalOscillation_Max = createUI_LabelValue(parent, SWT.TRAIL);
				_lblRunDyn_VerticalOscillation_Max_Unit = createUI_LabelValue(parent, SWT.LEAD);
			}
			{
				/*
				 * Avg
				 */
				createUI_Label(parent, Messages.Tour_Tooltip_Label_RunDyn_VerticalOscillation_Avg);

				_lblRunDyn_VerticalOscillation_Avg = createUI_LabelValue(parent, SWT.TRAIL);
				_lblRunDyn_VerticalOscillation_Avg_Unit = createUI_LabelValue(parent, SWT.LEAD);
			}
		}

		createUI_Spacer(parent);

		{
			/*
			 * Vertical Ratio
			 */

			{
				/*
				 * Min
				 */
				createUI_Label(parent, Messages.Tour_Tooltip_Label_RunDyn_VerticalRatio_Min);

				_lblRunDyn_VerticalRatio_Min = createUI_LabelValue(parent, SWT.TRAIL);
				_lblRunDyn_VerticalRatio_Min_Unit = createUI_LabelValue(parent, SWT.LEAD);
			}
			{
				/*
				 * Max
				 */
				createUI_Label(parent, Messages.Tour_Tooltip_Label_RunDyn_VerticalRatio_Max);

				_lblRunDyn_VerticalRatio_Max = createUI_LabelValue(parent, SWT.TRAIL);
				_lblRunDyn_VerticalRatio_Max_Unit = createUI_LabelValue(parent, SWT.LEAD);
			}
			{
				/*
				 * Avg
				 */
				createUI_Label(parent, Messages.Tour_Tooltip_Label_RunDyn_VerticalRatio_Avg);

				_lblRunDyn_VerticalRatio_Avg = createUI_LabelValue(parent, SWT.TRAIL);
				_lblRunDyn_VerticalRatio_Avg_Unit = createUI_LabelValue(parent, SWT.LEAD);
			}
		}
	}

	private void createUI_90_LowerPart(final Composite parent) {

		if (_hasTags == false && _hasDescription == false && _hasWeather == false && _hasTourType == false) {
			return;
		}

		Label label;
		final PixelConverter pc = new PixelConverter(parent);

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
		container.setForeground(_fgColor);
		container.setBackground(_bgColor);
		GridLayoutFactory.fillDefaults().numColumns(2).spacing(5, 0).applyTo(container);
//		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_CYAN));
		{
			/*
			 * tour type
			 */
			if (_hasTourType) {

				label = createUI_Label(container, Messages.Tour_Tooltip_Label_TourType);
				GridDataFactory.fillDefaults().align(SWT.FILL, SWT.BEGINNING).applyTo(label);

				_lblTourTypeText = createUI_LabelValue(container, SWT.LEAD | SWT.WRAP);
				GridDataFactory
						.fillDefaults()//
						.grab(true, false)
						.hint(MAX_DATA_WIDTH, SWT.DEFAULT)
						.applyTo(_lblTourTypeText);
			}

			/*
			 * tags
			 */
			if (_hasTags) {

				label = createUI_Label(container, Messages.Tour_Tooltip_Label_Tags);
				GridDataFactory.fillDefaults().align(SWT.FILL, SWT.BEGINNING).applyTo(label);

				_lblTourTags = createUI_LabelValue(container, SWT.LEAD | SWT.WRAP);
				GridDataFactory
						.fillDefaults()//
						.grab(true, false)
						.hint(MAX_DATA_WIDTH, SWT.DEFAULT)
						.applyTo(_lblTourTags);
			}

			/*
			 * weather
			 */
			if (_hasWeather) {

				label = createUI_Label(container, Messages.Tour_Tooltip_Label_Weather);
				GridDataFactory
						.fillDefaults()//
						.span(2, 1)
						.indent(0, 5)
						.applyTo(label);

				_txtWeather = new Text(container, SWT.WRAP | SWT.MULTI | SWT.READ_ONLY | SWT.BORDER);
				GridDataFactory
						.fillDefaults()//
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
				label = createUI_Label(container, Messages.Tour_Tooltip_Label_Description);
				GridDataFactory
						.fillDefaults()//
						.span(2, 1)
						.indent(0, 5)
						.applyTo(label);

				// text field
				int style = SWT.WRAP | SWT.MULTI | SWT.READ_ONLY | SWT.BORDER;
				final int lineCount = Util.countCharacter(_tourData.getTourDescription(), '\n');

				if (lineCount > 10) {
					style |= SWT.V_SCROLL;
				}

				_txtDescription = new Text(container, style);
				GridDataFactory
						.fillDefaults()//
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

	private void createUI_92_CreateModifyTime(final Composite parent) {

		if (_uiDtCreated == null && _uiDtModified == null) {
			return;
		}

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
		container.setForeground(_fgColor);
		container.setBackground(_bgColor);
		GridLayoutFactory
				.fillDefaults()//
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
				createUI_Label(containerCreated, Messages.Tour_Tooltip_Label_DateTimeCreated);

				_lblDateTimeCreatedValue = createUI_LabelValue(containerCreated, SWT.LEAD);
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
				_lblDateTimeModified = createUI_Label(containerModified, Messages.Tour_Tooltip_Label_DateTimeModified);
				GridDataFactory
						.fillDefaults()//
						.grab(true, false)
						.align(SWT.END, SWT.FILL)
						.applyTo(_lblDateTimeModified);

				_lblDateTimeModifiedValue = createUI_LabelValue(containerModified, SWT.TRAIL);
				GridDataFactory
						.fillDefaults()//
						.align(SWT.END, SWT.FILL)
						.applyTo(_lblDateTimeModifiedValue);
			}
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

	public Composite createUI_NoData(final Composite parent) {

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
			GridLayoutFactory
					.fillDefaults()//
					.margins(SHELL_MARGIN, SHELL_MARGIN)
					.applyTo(container);
			{
				final Label label = new Label(container, SWT.NONE);
				label.setText(_noTourTooltip);
				label.setForeground(fgColor);
				label.setBackground(bgColor);
			}
		}

		return shellContainer;
	}

	private void createUI_Spacer(final Composite container) {

		// spacer
		final Label label = createUI_Label(container, null);
		GridDataFactory.fillDefaults().span(3, 1).applyTo(label);
	}

	public void dispose() {

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

	private boolean isSimpleTour() {

		final long recordingTime = _tourData.getTourRecordingTime();

		final boolean isShortTour = recordingTime < UI.DAY_IN_SECONDS;
		final boolean isSingleTour = !_tourData.isMultipleTours();

		return isShortTour || isSingleTour;
	}

	/**
	 * Enable/disable tour edit actions, actions are disabled by default
	 * 
	 * @param isEnabled
	 */
	public void setActionsEnabled(final boolean isEnabled) {
		_isActionsVisible = isEnabled;
	}

	/**
	 * Set text for the tooltip which is displayed when a tour is not hovered.
	 * 
	 * @param noTourTooltip
	 */
	public void setNoTourTooltip(final String noTourTooltip) {
		_noTourTooltip = noTourTooltip;
	}

	private void updateUI() {

		/*
		 * upper/lower part
		 */
		if (_lblTourType != null && _lblTourType.isDisposed() == false) {
			_lblTourType.setToolTipText(_uiTourTypeName);
			net.tourbook.ui.UI.updateUI_TourType(_tourData, _lblTourType, false);
		}

		String tourTitle = _tourData.getTourTitle();
		if (tourTitle == null || tourTitle.trim().length() == 0) {

			if (_uiTourTypeName == null) {
				tourTitle = Messages.Tour_Tooltip_Label_DefaultTitle;
			} else {
				tourTitle = _uiTourTypeName;
			}
		}
		_lblTitle.setText(tourTitle);

		if (_hasWeather) {
			_txtWeather.setText(_tourData.getWeather());
		}
		if (_hasTourType) {
			_lblTourTypeText.setText(_tourData.getTourType().getName());
		}
		if (_hasTags) {
			net.tourbook.ui.UI.updateUI_Tags(_tourData, _lblTourTags);
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

		final ZonedDateTime zdtTourStart = _tourData.getTourStartTime();
		final ZonedDateTime zdtTourEnd = zdtTourStart.plusSeconds(recordingTime);

		if (isSimpleTour()) {

			// < 1 day

			_lblDate.setText(
					String.format(//
							Messages.Tour_Tooltip_Format_DateWeekTime,
							zdtTourStart.format(TimeTools.Formatter_Date_F),
							zdtTourStart.format(TimeTools.Formatter_Time_M),
							zdtTourEnd.format(TimeTools.Formatter_Time_M),
							zdtTourStart.get(TimeTools.calendarWeek.weekOfWeekBasedYear())

					));

			_lblRecordingTimeHour.setVisible(true);
			_lblMovingTimeHour.setVisible(true);
			_lblBreakTimeHour.setVisible(true);

			_lblRecordingTime.setText(FormatManager.formatRecordingTime(recordingTime));
			_lblMovingTime.setText(FormatManager.formatDrivingTime(movingTime));
			_lblBreakTime.setText(FormatManager.formatPausedTime(breakTime));

			/*
			 * Time zone
			 */
			final String tourTimeZoneId = _tourData.getTimeZoneId();
			final TourDateTime tourDateTime = _tourData.getTourDateTime();
			_lblTimeZone_Value.setText(tourTimeZoneId == null ? UI.EMPTY_STRING : tourTimeZoneId);
			_lblTimeZoneDifference_Value.setText(tourDateTime.timeZoneOffsetLabel);

			// set tooltip text
			final String defaultTimeZoneId = _prefStoreCommon.getString(ICommonPreferences.TIME_ZONE_LOCAL_ID);
			final String timeZoneTooltip = NLS.bind(
					Messages.ColumnFactory_TimeZoneDifference_Tooltip,
					defaultTimeZoneId);

			_lblTimeZoneDifference.setToolTipText(timeZoneTooltip);
			_lblTimeZoneDifference_Value.setToolTipText(timeZoneTooltip);
			_decoTimeZone.setDescriptionText(timeZoneTooltip);

		} else {

			// > 1 day

			_lblDate.setText(
					String.format(//
							Messages.Tour_Tooltip_Format_HistoryDateTime,
							zdtTourStart.format(_dtHistoryFormatter),
							zdtTourEnd.format(_dtHistoryFormatter)//
					));

			// hide labels, they are displayed with the period values
			_lblRecordingTimeHour.setVisible(false);
			_lblMovingTimeHour.setVisible(false);
			_lblBreakTimeHour.setVisible(false);

			final Period recordingPeriod = new Period(
					_tourData.getTourStartTimeMS(),
					_tourData.getTourEndTimeMS(),
					_tourPeriodTemplate);
			final Period movingPeriod = new Period(0, movingTime * 1000, _tourPeriodTemplate);
			final Period breakPeriod = new Period(0, breakTime * 1000, _tourPeriodTemplate);

			_lblRecordingTime.setText(recordingPeriod.toString(UI.DEFAULT_DURATION_FORMATTER_SHORT));
			_lblMovingTime.setText(movingPeriod.toString(UI.DEFAULT_DURATION_FORMATTER_SHORT));
			_lblBreakTime.setText(breakPeriod.toString(UI.DEFAULT_DURATION_FORMATTER_SHORT));
		}

		int windSpeed = _tourData.getWeatherWindSpeed();
		windSpeed = (int) (windSpeed / net.tourbook.ui.UI.UNIT_VALUE_DISTANCE);

		_lblWindSpeed.setText(Integer.toString(windSpeed));
		_lblWindSpeedUnit.setText(
				String.format(
						Messages.Tour_Tooltip_Format_WindSpeedUnit,
						UI.UNIT_LABEL_SPEED,
						IWeather.windSpeedTextShort[getWindSpeedTextIndex(windSpeed)]));

		// wind direction
		final int weatherWindDirDegree = _tourData.getWeatherWindDir();
		_lblWindDirection.setText(Integer.toString(weatherWindDirDegree));
		_lblWindDirectionUnit.setText(
				String.format(
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

		_lblClouds.setImage(UI.IMAGE_REGISTRY.get(cloudImageName));
		_lblCloudsUnit.setText(cloudText.equals(IWeather.cloudIsNotDefined) ? UI.EMPTY_STRING : cloudText);

		/*
		 * column: right
		 */
		final float distance = _tourData.getTourDistance() / net.tourbook.ui.UI.UNIT_VALUE_DISTANCE;

		_lblDistance.setText(FormatManager.formatDistance(distance / 1000.0));
		_lblDistanceUnit.setText(UI.UNIT_LABEL_DISTANCE);

		_lblAltitudeUp.setText(Integer.toString((int) (_tourData.getTourAltUp() / net.tourbook.ui.UI.UNIT_VALUE_ALTITUDE)));
		_lblAltitudeUpUnit.setText(UI.UNIT_LABEL_ALTITUDE);

		_lblAltitudeDown.setText(Integer.toString((int) (_tourData.getTourAltDown() / net.tourbook.ui.UI.UNIT_VALUE_ALTITUDE)));
		_lblAltitudeDownUnit.setText(UI.UNIT_LABEL_ALTITUDE);

		final float avgSpeed = movingTime == 0 ? 0 : distance / (movingTime / 3.6f);
		_lblAvgSpeed.setText(FormatManager.formatSpeed(avgSpeed));
		_lblAvgSpeedUnit.setText(UI.UNIT_LABEL_SPEED);

		final int pace = (int) (distance == 0 ? 0 : (movingTime * 1000 / distance));
		_lblAvgPace.setText(
				String.format(//
						Messages.Tour_Tooltip_Format_Pace,
						pace / 60,
						pace % 60)//
		);
		_lblAvgPaceUnit.setText(UI.UNIT_LABEL_PACE);

		// avg pulse
		final double avgPulse = _tourData.getAvgPulse();
		_lblAvgPulse.setText(FormatManager.formatPulse(avgPulse));
		_lblAvgPulseUnit.setText(Messages.Value_Unit_Pulse);

		// avg cadence
		final double avgCadence = _tourData.getAvgCadence() * _tourData.getCadenceMultiplier();
		_lblAvgCadence.setText(FormatManager.formatCadence(avgCadence));
		_lblAvgCadenceUnit.setText(
				_tourData.isCadenceSpm()
						? Messages.Value_Unit_Cadence_Spm
						: Messages.Value_Unit_Cadence);

		// avg power
		final double avgPower = _tourData.getPower_Avg();
		_lblAvg_Power.setText(FormatManager.formatPower(avgPower));
		_lblAvg_PowerUnit.setText(UI.UNIT_POWER);

		// calories
		final double calories = _tourData.getCalories();
		_lblCalories.setText(FormatManager.formatNumber_0(calories));

		// body
		_lblRestPulse.setText(Integer.toString(_tourData.getRestPulse()));
		_lblBodyWeight.setText(_nf1.format(_tourData.getBodyWeight()));

		/*
		 * Max values
		 */
		_lblMaxAltitude.setText(Integer.toString((int) (_tourData.getMaxAltitude() / net.tourbook.ui.UI.UNIT_VALUE_ALTITUDE)));
		_lblMaxAltitudeUnit.setText(UI.UNIT_LABEL_ALTITUDE);

		_lblMaxPulse.setText(FormatManager.formatPulse(_tourData.getMaxPulse()));
		_lblMaxPulseUnit.setText(Messages.Value_Unit_Pulse);

		_lblMaxSpeed.setText(FormatManager.formatSpeed(_tourData.getMaxSpeed()));
		_lblMaxSpeedUnit.setText(UI.UNIT_LABEL_SPEED);

		// gears
		if (_hasGears) {
			_lblGearFrontShifts.setText(Integer.toString(_tourData.getFrontShiftCount()));
			_lblGearRearShifts.setText(REAR_SHIFT_FORMAT + Integer.toString(_tourData.getRearShiftCount()));
		}

		/*
		 * date/time
		 */

		// date/time created
		if (_uiDtCreated != null) {

			_lblDateTimeCreatedValue.setText(_uiDtCreated == null ? //
					UI.EMPTY_STRING
					: _uiDtCreated.format(TimeTools.Formatter_DateTime_M));
		}

		// date/time modified
		if (_uiDtModified != null) {

			_lblDateTimeModifiedValue.setText(_uiDtModified == null ? //
					UI.EMPTY_STRING
					: _uiDtModified.format(TimeTools.Formatter_DateTime_M));
		}

		/*
		 * Running Dynamics
		 */
		if (_hasRunDyn) {

			final float mmOrInch = net.tourbook.ui.UI.UNIT_VALUE_DISTANCE_MM_OR_INCH;

			_lblRunDyn_StanceTime_Min.setText(Integer.toString(_tourData.getRunDyn_StanceTime_Min()));
			_lblRunDyn_StanceTime_Min_Unit.setText(UI.UNIT_MS);
			_lblRunDyn_StanceTime_Max.setText(Integer.toString(_tourData.getRunDyn_StanceTime_Max()));
			_lblRunDyn_StanceTime_Max_Unit.setText(UI.UNIT_MS);
			_lblRunDyn_StanceTime_Avg.setText(_nf0.format(_tourData.getRunDyn_StanceTime_Avg()));
			_lblRunDyn_StanceTime_Avg_Unit.setText(UI.UNIT_MS);

			_lblRunDyn_StanceTimeBalance_Min.setText(_nf1.format(_tourData.getRunDyn_StanceTimeBalance_Min()));
			_lblRunDyn_StanceTimeBalance_Min_Unit.setText(UI.SYMBOL_PERCENTAGE);
			_lblRunDyn_StanceTimeBalance_Max.setText(_nf1.format(_tourData.getRunDyn_StanceTimeBalance_Max()));
			_lblRunDyn_StanceTimeBalance_Max_Unit.setText(UI.SYMBOL_PERCENTAGE);
			_lblRunDyn_StanceTimeBalance_Avg.setText(_nf1.format(_tourData.getRunDyn_StanceTimeBalance_Avg()));
			_lblRunDyn_StanceTimeBalance_Avg_Unit.setText(UI.SYMBOL_PERCENTAGE);

			if (UI.UNIT_IS_METRIC) {

				_lblRunDyn_StepLength_Min.setText(_nf0.format(_tourData.getRunDyn_StepLength_Min() * mmOrInch));
				_lblRunDyn_StepLength_Max.setText(_nf0.format(_tourData.getRunDyn_StepLength_Max() * mmOrInch));
				_lblRunDyn_StepLength_Avg.setText(_nf0.format(_tourData.getRunDyn_StepLength_Avg() * mmOrInch));

				_lblRunDyn_VerticalOscillation_Min.setText(_nf0.format(_tourData.getRunDyn_VerticalOscillation_Min() * mmOrInch));
				_lblRunDyn_VerticalOscillation_Max.setText(_nf0.format(_tourData.getRunDyn_VerticalOscillation_Max() * mmOrInch));
				_lblRunDyn_VerticalOscillation_Avg.setText(_nf0.format(_tourData.getRunDyn_VerticalOscillation_Avg() * mmOrInch));

			} else {

				// imperial has 1 more digit

				_lblRunDyn_StepLength_Min.setText(_nf1.format(_tourData.getRunDyn_StepLength_Min() * mmOrInch));
				_lblRunDyn_StepLength_Max.setText(_nf1.format(_tourData.getRunDyn_StepLength_Max() * mmOrInch));
				_lblRunDyn_StepLength_Avg.setText(_nf1.format(_tourData.getRunDyn_StepLength_Avg() * mmOrInch));

				_lblRunDyn_VerticalOscillation_Min.setText(_nf1.format(_tourData.getRunDyn_VerticalOscillation_Min() * mmOrInch));
				_lblRunDyn_VerticalOscillation_Max.setText(_nf1.format(_tourData.getRunDyn_VerticalOscillation_Max() * mmOrInch));
				_lblRunDyn_VerticalOscillation_Avg.setText(_nf1.format(_tourData.getRunDyn_VerticalOscillation_Avg() * mmOrInch));
			}

			_lblRunDyn_StepLength_Min_Unit.setText(UI.UNIT_LABEL_DISTANCE_MM_OR_INCH);
			_lblRunDyn_StepLength_Max_Unit.setText(UI.UNIT_LABEL_DISTANCE_MM_OR_INCH);
			_lblRunDyn_StepLength_Avg_Unit.setText(UI.UNIT_LABEL_DISTANCE_MM_OR_INCH);

			_lblRunDyn_VerticalOscillation_Min_Unit.setText(UI.UNIT_LABEL_DISTANCE_MM_OR_INCH);
			_lblRunDyn_VerticalOscillation_Max_Unit.setText(UI.UNIT_LABEL_DISTANCE_MM_OR_INCH);
			_lblRunDyn_VerticalOscillation_Avg_Unit.setText(UI.UNIT_LABEL_DISTANCE_MM_OR_INCH);

			_lblRunDyn_VerticalRatio_Min.setText(_nf1.format(_tourData.getRunDyn_VerticalRatio_Min()));
			_lblRunDyn_VerticalRatio_Min_Unit.setText(UI.SYMBOL_PERCENTAGE);
			_lblRunDyn_VerticalRatio_Max.setText(_nf1.format(_tourData.getRunDyn_VerticalRatio_Max()));
			_lblRunDyn_VerticalRatio_Max_Unit.setText(UI.SYMBOL_PERCENTAGE);
			_lblRunDyn_VerticalRatio_Avg.setText(_nf1.format(_tourData.getRunDyn_VerticalRatio_Avg()));
			_lblRunDyn_VerticalRatio_Avg_Unit.setText(UI.SYMBOL_PERCENTAGE);
		}

	}

	public void updateUI_Layout() {

		// compute width for all controls and equalize column width for the different sections

		_ttContainer.layout(true, true);

		if (isSimpleTour()) {

			/*
			 * Reduce width that the decorator is not truncated
			 */
			final GridData gd = (GridData) _lblTimeZoneDifference.getLayoutData();
			gd.widthHint -= UI.DECORATOR_HORIZONTAL_INDENT;
		}
	}
}
