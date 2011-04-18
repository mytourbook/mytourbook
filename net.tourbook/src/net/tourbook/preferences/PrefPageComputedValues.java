/*******************************************************************************
 * Copyright (C) 2005, 2011  Wolfgang Schramm and Contributors
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
package net.tourbook.preferences;

import java.text.NumberFormat;
import java.util.ArrayList;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.data.TourData;
import net.tourbook.database.IComputeTourValues;
import net.tourbook.database.TourDatabase;
import net.tourbook.tour.BreakTimeTool;
import net.tourbook.tour.TourEventId;
import net.tourbook.tour.TourManager;
import net.tourbook.ui.UI;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.part.PageBook;

public class PrefPageComputedValues extends PreferencePage implements IWorkbenchPreferencePage {

	public static final String			ID									= "net.tourbook.preferences.PrefPageComputedValues";	//$NON-NLS-1$

	private static final int			DESCRIPTION_HINT					= 300;

	public static final String			STATE_COMPUTED_VALUE_MIN_ALTITUDE	= "computedValue.minAltitude";							//$NON-NLS-1$
	private static final String			STATE_COMPUTED_VALUE_SELECTED_TAB	= "computedValue.selectedTab";							//$NON-NLS-1$

	public static final int[]			ALTITUDE_MINIMUM					= new int[] {
			1,
			2,
			3,
			4,
			5,
			6,
			7,
			8,
			9,
			10,
			12,
			14,
			16,
			18,
			20,
			25,
			30,
			35,
			40,
			45,
			50,
			60,
			70,
			80,
			90,
			100,
			120,
			140,
			160,
			180,
			200,
			250,
			300,
			350,
			400,
			450,
			500,
			600,
			700,
			800,
			900,
			1000															};

	public static final int				DEFAULT_MIN_ALTITUDE_INDEX			= 4;
	public static final int				DEFAULT_MIN_ALTITUDE				= ALTITUDE_MINIMUM[DEFAULT_MIN_ALTITUDE_INDEX];

	/*
	 * contains the tab folder index
	 */
	public static final int				TAB_FOLDER_ELEVATION				= 0;
	public static final int				TAB_FOLDER_SPEED					= 1;
	public static final int				TAB_FOLDER_BREAK_TIME				= 2;

	private static final float			SPEED_DIGIT_VALUE					= 10.0f;

	private IPreferenceStore			_prefStore							= TourbookPlugin
																					.getDefault()
																					.getPreferenceStore();
	private NumberFormat				_nf0								= NumberFormat.getNumberInstance();
	private NumberFormat				_nf1								= NumberFormat.getNumberInstance();
	{
		_nf0.setMinimumFractionDigits(0);
		_nf0.setMaximumFractionDigits(0);
		_nf1.setMinimumFractionDigits(1);
		_nf1.setMaximumFractionDigits(1);
	}
	private final boolean				_isOSX								= net.tourbook.util.UI.IS_OSX;
	private int							_spinnerWidth;

	/**
	 * contains the controls which are displayed in the first column, these controls are used to get
	 * the maximum width and set the first column within the differenct section to the same width
	 */
	private final ArrayList<Control>	_firstColBreakTime					= new ArrayList<Control>();

	/*
	 * UI controls
	 */
	private TabFolder					_tabFolder;

	private Combo						_comboMinAltitude;
	private Spinner						_spinnerSpeedMinTime;

	private PixelConverter				_pc;

	private Combo						_comboBreakMethod;

	private PageBook					_pagebookBreakTime;

	private Composite					_pageBreakByTimeDistance;
	private Composite					_pageBreakBySliceSpeed;
	private Composite					_pageBreakByAvgSpeed;

	private Label						_lblBreakDistanceUnit;

	private Spinner						_spinnerBreakShortestTime;
	private Spinner						_spinnerBreakMaxDistance;
	private Spinner						_spinnerBreakMinSliceSpeed;
	private Spinner						_spinnerBreakMinAvgSpeed;

	@Override
	public void applyData(final Object data) {

		// data contains the folder index, this is set when the pref page is opened from a link

		if (data instanceof Integer) {
			_tabFolder.setSelection((Integer) data);
		}
	}

	@Override
	protected Control createContents(final Composite parent) {

		final Composite container = createUI(parent);

		restoreState();

		return container;
	}

	private Composite createUI(final Composite parent) {

		_pc = new PixelConverter(parent);
		_spinnerWidth = _pc.convertWidthInCharsToPixels(_isOSX ? 10 : 5);

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(1).spacing(0, 15).applyTo(container);
		{
			/*
			 * label: info
			 */
			final Label label = new Label(container, SWT.NONE);
			GridDataFactory.fillDefaults().applyTo(label);
			label.setText(Messages.Compute_Values_Label_Info);

			/*
			 * tab folder: computed values
			 */
			_tabFolder = new TabFolder(container, SWT.TOP);
			GridDataFactory.fillDefaults()//
					.grab(true, false)
//					.indent(40, 20)
					.applyTo(_tabFolder);
			{
				final TabItem tabElevation = new TabItem(_tabFolder, SWT.NONE);
				tabElevation.setControl(createUI10ElevationGain(_tabFolder));
				tabElevation.setText(Messages.compute_tourValueElevation_group_computeTourAltitude);

				final TabItem tabSpeed = new TabItem(_tabFolder, SWT.NONE);
				tabSpeed.setControl(createUI20Speed(_tabFolder));
				tabSpeed.setText(Messages.compute_tourValueSpeed_group_speed);

				final TabItem tabBreakTime = new TabItem(_tabFolder, SWT.NONE);
				tabBreakTime.setControl(createUI50BreakTime(_tabFolder));
				tabBreakTime.setText(Messages.Compute_BreakTime_Group_BreakTime);

				/**
				 * 4.8.2009 week no/year is currently disabled because a new field in the db is
				 * required which holds the year of the week<br>
				 * <br>
				 * all plugins must be adjusted which set's the week number of a tour
				 */
//			createUIWeek(container);
			}
		}

		return _tabFolder;
	}

	private Control createUI10ElevationGain(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
		GridLayoutFactory.swtDefaults().extendedMargins(5, 5, 10, 5).numColumns(3).applyTo(container);
		{
			// label: min alti diff
			Label label = new Label(container, SWT.NONE);
			label.setText(Messages.compute_tourValueElevation_label_minAltiDifference);

			// combo: min altitude
			_comboMinAltitude = new Combo(container, SWT.READ_ONLY);
			_comboMinAltitude.setVisibleItemCount(20);
			for (final int minAlti : PrefPageComputedValues.ALTITUDE_MINIMUM) {
				_comboMinAltitude.add(Integer.toString((int) (minAlti / UI.UNIT_VALUE_ALTITUDE)));
			}

			// label: unit
			label = new Label(container, SWT.NONE);
			label.setText(UI.UNIT_LABEL_ALTITUDE);

			// label: description
			label = new Label(container, SWT.WRAP);
			GridDataFactory.fillDefaults()//
					.span(3, 1)
					.indent(0, 10)
					.hint(DESCRIPTION_HINT, SWT.DEFAULT)
					.grab(true, false)
					.applyTo(label);
			label.setText(Messages.compute_tourValueElevation_label_description);

			UI.createBullets(container, //
					Messages.compute_tourValueElevation_label_description_Hints,
					1,
					3,
					DESCRIPTION_HINT,
					null);

			final Composite btnContainer = new Composite(container, SWT.NONE);
			GridDataFactory.fillDefaults().span(3, 1).applyTo(btnContainer);
			GridLayoutFactory.fillDefaults().applyTo(btnContainer);
			{
				// button: compute computed values
				final Button btnComputValues = new Button(btnContainer, SWT.NONE);
				GridDataFactory.fillDefaults().indent(0, 10).applyTo(btnComputValues);
				btnComputValues.setText(Messages.compute_tourValueElevation_button_computeValues);
				btnComputValues.setToolTipText(Messages.compute_tourValueElevation_button_computeValues_tooltip);
				btnComputValues.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(final SelectionEvent e) {
						onComputeElevationGainValues();
					}
				});
			}
		}
		return container;
	}

	private Control createUI20Speed(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
		GridLayoutFactory.swtDefaults().extendedMargins(5, 5, 10, 5).numColumns(3).applyTo(container);
		{
			// label: min alti diff
			Label label = new Label(container, SWT.NONE);
			label.setText(Messages.compute_tourValueSpeed_label_speedTimeSlice);

			// combo: min altitude
			_spinnerSpeedMinTime = new Spinner(container, SWT.BORDER);
			GridDataFactory.fillDefaults().hint(_spinnerWidth, SWT.DEFAULT).applyTo(_spinnerSpeedMinTime);
			_spinnerSpeedMinTime.setMaximum(1000);
			_spinnerSpeedMinTime.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					onChangeSpeedProperties();
				}
			});
			_spinnerSpeedMinTime.addMouseWheelListener(new MouseWheelListener() {
				public void mouseScrolled(final MouseEvent event) {
					UI.adjustSpinnerValueOnMouseScroll(event);
					onChangeSpeedProperties();
				}
			});

			// label: unit
			label = new Label(container, SWT.NONE);
			label.setText(Messages.app_unit_seconds);

			// label: description
			label = new Label(container, SWT.WRAP);
			GridDataFactory.fillDefaults()//
					.span(3, 1)
					.indent(0, 10)
					.hint(DESCRIPTION_HINT, SWT.DEFAULT)
					.grab(true, false)
					.applyTo(label);
			label.setText(Messages.compute_tourValueSpeed_label_description);

			UI.createBullets(container, //
					Messages.compute_tourValueSpeed_label_description_Hints,
					1,
					3,
					DESCRIPTION_HINT,
					null);

			/*
			 * compute speed values for all tours
			 */
			final Composite btnContainer = new Composite(container, SWT.NONE);
			GridDataFactory.fillDefaults().span(3, 1).applyTo(btnContainer);
			GridLayoutFactory.fillDefaults().applyTo(btnContainer);
			{
				// button: compute computed values
				final Button btnComputValues = new Button(btnContainer, SWT.NONE);
				GridDataFactory.fillDefaults().indent(0, 10).applyTo(btnComputValues);
				btnComputValues.setText(Messages.compute_tourValueSpeed_button_computeValues);
				btnComputValues.setToolTipText(Messages.compute_tourValueSpeed_button_computeValues_tooltip);
				btnComputValues.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(final SelectionEvent e) {
						onComputeSpeedValues();
					}
				});
			}
		}

		return container;
	}

	private Composite createUI50BreakTime(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
		GridLayoutFactory.swtDefaults().extendedMargins(5, 5, 10, 5).numColumns(2).applyTo(container);

//		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_MAGENTA));
		{
			/*
			 * label: compute break time by
			 */
			Label label = new Label(container, SWT.NONE);
			GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).applyTo(label);
			label.setText(Messages.Compute_BreakTime_Label_ComputeBreakTimeBy);
			_firstColBreakTime.add(label);

			// combo: break method
			_comboBreakMethod = new Combo(container, SWT.READ_ONLY | SWT.BORDER);
			_comboBreakMethod.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					onSelectBreakTimeMethod();
				}
			});

			// fill combo
			for (final String breakMethod : BreakTimeTool.BREAK_TIME_METHODS) {
				_comboBreakMethod.add(breakMethod);
			}

			/*
			 * pagebook: break method
			 */
			_pagebookBreakTime = new PageBook(container, SWT.NONE);
			GridDataFactory.fillDefaults().span(2, 1).grab(true, false).applyTo(_pagebookBreakTime);
			{
				_pageBreakByTimeDistance = createUI52BreakByTimeDistance(_pagebookBreakTime);
				_pageBreakBySliceSpeed = createUI53BreakBySliceSpeed(_pagebookBreakTime);
				_pageBreakByAvgSpeed = createUI54BreakByAvgSpeed(_pagebookBreakTime);
			}

			/*
			 * label: description part II
			 */
			label = new Label(container, SWT.WRAP);
			GridDataFactory.fillDefaults()//
					.span(2, 1)
					.indent(0, 10)
					.hint(DESCRIPTION_HINT, SWT.DEFAULT)
					.grab(true, false)
					.applyTo(label);
			label.setText(Messages.Compute_BreakTime_Label_Description);

			/*
			 * hints
			 */
			final StyledText listHints = UI.createBullets(container, //
					Messages.Compute_BreakTime_Label_Hints,
					1,
					2,
					DESCRIPTION_HINT,
					null);
			GridDataFactory.fillDefaults()//
					.span(2, 1)
					.hint(DESCRIPTION_HINT, SWT.DEFAULT)
					.indent(0, 30)
					.applyTo(listHints);

			/*
			 * compute speed values for all tours
			 */
			final Composite btnContainer = new Composite(container, SWT.NONE);
			GridDataFactory.fillDefaults().span(3, 1).applyTo(btnContainer);
			GridLayoutFactory.fillDefaults().applyTo(btnContainer);
			{
				// button: compute computed values
				final Button btnComputValues = new Button(btnContainer, SWT.NONE);
				GridDataFactory.fillDefaults().indent(0, 10).applyTo(btnComputValues);
				btnComputValues.setText(Messages.Compute_BreakTime_Button_ComputeAllTours);
				btnComputValues.setToolTipText(Messages.Compute_BreakTime_Button_ComputeAllTours_Tooltip);
				btnComputValues.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(final SelectionEvent e) {
						onComputeBreakTimeValues();
					}
				});
			}
		}

		/*
		 * force pages to be displayed otherwise they are hidden or the hint is not computed for the
		 * first column until a resize is done
		 */
		_pagebookBreakTime.showPage(_pageBreakBySliceSpeed);

		container.layout(true, true);
		UI.setEqualizeColumWidths(_firstColBreakTime);

		return container;
	}

	private Composite createUI52BreakByTimeDistance(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(3).applyTo(container);
		{
			/*
			 * shortest break time
			 */
			{
				// label: break min time
				Label label = new Label(container, SWT.NONE);
				label.setText(Messages.Compute_BreakTime_Label_MinimumTime);
				_firstColBreakTime.add(label);

				// spinner: break minimum time
				_spinnerBreakShortestTime = new Spinner(container, SWT.BORDER);
				GridDataFactory.fillDefaults().hint(_spinnerWidth, SWT.DEFAULT).applyTo(_spinnerBreakShortestTime);
				_spinnerBreakShortestTime.setMinimum(1);
				_spinnerBreakShortestTime.setMaximum(120); // 120 seconds
				_spinnerBreakShortestTime.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(final SelectionEvent e) {
						onChangeBreakTime();
					}
				});
				_spinnerBreakShortestTime.addMouseWheelListener(new MouseWheelListener() {
					public void mouseScrolled(final MouseEvent event) {
						UI.adjustSpinnerValueOnMouseScroll(event);
						onChangeBreakTime();
					}
				});

				// label: unit
				label = new Label(container, SWT.NONE);
				label.setText(Messages.App_Unit_Seconds_Small);
			}

			/*
			 * recording distance
			 */
			{
				// label: break min distance
				final Label label = new Label(container, SWT.NONE);
				label.setText(Messages.Compute_BreakTime_Label_MinimumDistance);
				_firstColBreakTime.add(label);

				// spinner: break minimum time
				_spinnerBreakMaxDistance = new Spinner(container, SWT.BORDER);
				GridDataFactory.fillDefaults().hint(_spinnerWidth, SWT.DEFAULT).applyTo(_spinnerBreakMaxDistance);
				_spinnerBreakMaxDistance.setMinimum(1);
				_spinnerBreakMaxDistance.setMaximum(1000); // 1000 m/yards
				_spinnerBreakMaxDistance.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(final SelectionEvent e) {
						onChangeBreakTime();
					}
				});
				_spinnerBreakMaxDistance.addMouseWheelListener(new MouseWheelListener() {
					public void mouseScrolled(final MouseEvent event) {
						UI.adjustSpinnerValueOnMouseScroll(event);
						onChangeBreakTime();
					}
				});

				// label: unit
				_lblBreakDistanceUnit = new Label(container, SWT.NONE);
				_lblBreakDistanceUnit.setText(UI.UNIT_LABEL_DISTANCE_SMALL);
				GridDataFactory.fillDefaults()//
//						.span(2, 1)
						.align(SWT.FILL, SWT.CENTER)
						.applyTo(_lblBreakDistanceUnit);
			}

			/*
			 * label: description
			 */
			final Label label = new Label(container, SWT.WRAP);
			GridDataFactory.fillDefaults()//
					.span(3, 1)
					.indent(0, 10)
					.hint(DESCRIPTION_HINT, SWT.DEFAULT)
					.grab(true, false)
					.applyTo(label);
			label.setText(Messages.Compute_BreakTime_Label_Description_ComputeByTime);
		}

		return container;
	}

	private Composite createUI53BreakBySliceSpeed(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(3).applyTo(container);
//		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));
		{
			/*
			 * minimum speed
			 */

			// label: minimum speed
			Label label = new Label(container, SWT.NONE);
			label.setText(Messages.Compute_BreakTime_Label_MinimumSliceSpeed);
			_firstColBreakTime.add(label);

			// spinner: minimum speed
			_spinnerBreakMinSliceSpeed = new Spinner(container, SWT.BORDER);
			GridDataFactory.fillDefaults().hint(_spinnerWidth, SWT.DEFAULT).applyTo(_spinnerBreakMinSliceSpeed);
			_spinnerBreakMinSliceSpeed.setMinimum(0); // 0.0 km/h
			_spinnerBreakMinSliceSpeed.setMaximum(100); // 10.0 km/h
			_spinnerBreakMinSliceSpeed.setDigits(1);
			_spinnerBreakMinSliceSpeed.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					onChangeBreakTime();
				}
			});
			_spinnerBreakMinSliceSpeed.addMouseWheelListener(new MouseWheelListener() {
				public void mouseScrolled(final MouseEvent event) {
					UI.adjustSpinnerValueOnMouseScroll(event);
					onChangeBreakTime();
				}
			});

			// label: km/h
			label = new Label(container, SWT.NONE);
			label.setText(UI.UNIT_LABEL_SPEED);

			/*
			 * label: description
			 */
			label = new Label(container, SWT.WRAP);
			GridDataFactory.fillDefaults()//
					.span(3, 1)
					.indent(0, 10)
					.hint(DESCRIPTION_HINT, SWT.DEFAULT)
					.grab(true, false)
					.applyTo(label);
			label.setText(Messages.Compute_BreakTime_Label_Description_ComputeBySliceSpeed);
		}

		return container;
	}

	private Composite createUI54BreakByAvgSpeed(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(3).applyTo(container);
//		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
		{
			/*
			 * minimum average speed
			 */

			// label: minimum speed
			Label label = new Label(container, SWT.NONE);
			label.setText(Messages.Compute_BreakTime_Label_MinimumAvgSpeed);
			_firstColBreakTime.add(label);

			// spinner: minimum speed
			_spinnerBreakMinAvgSpeed = new Spinner(container, SWT.BORDER);
			GridDataFactory.fillDefaults().hint(_spinnerWidth, SWT.DEFAULT).applyTo(_spinnerBreakMinAvgSpeed);
			_spinnerBreakMinAvgSpeed.setMinimum(0); // 0.0 km/h
			_spinnerBreakMinAvgSpeed.setMaximum(100); // 10.0 km/h
			_spinnerBreakMinAvgSpeed.setDigits(1);
			_spinnerBreakMinAvgSpeed.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					onChangeBreakTime();
				}
			});
			_spinnerBreakMinAvgSpeed.addMouseWheelListener(new MouseWheelListener() {
				public void mouseScrolled(final MouseEvent event) {
					UI.adjustSpinnerValueOnMouseScroll(event);
					onChangeBreakTime();
				}
			});

			// label: km/h
			label = new Label(container, SWT.NONE);
			label.setText(UI.UNIT_LABEL_SPEED);

			/*
			 * label: description
			 */
			label = new Label(container, SWT.WRAP);
			GridDataFactory.fillDefaults()//
					.span(3, 1)
					.indent(0, 10)
					.hint(DESCRIPTION_HINT, SWT.DEFAULT)
					.grab(true, false)
					.applyTo(label);
			label.setText(Messages.Compute_BreakTime_Label_Description_ComputeByAvgSpeed);
		}

		return container;
	}

	private void fireTourModifyEvent() {

		TourManager.getInstance().removeAllToursFromCache();
		TourManager.fireEvent(TourEventId.CLEAR_DISPLAYED_TOUR);

		// fire unique event for all changes
		TourManager.fireEvent(TourEventId.ALL_TOURS_ARE_MODIFIED);
	}

	public void init(final IWorkbench workbench) {}

	@Override
	public boolean okToLeave() {
		saveUIState();
		return super.okToLeave();
	}

	private void onChangeBreakTime() {

//		final double time = _spinnerBreakMinTime.getSelection();
//		final double distance = (_spinnerBreakMaxDistance.getSelection() * UI.UNIT_VALUE_DISTANCE_SMALL);
//
//		final double speed = 3.6f * distance / time;
//
//		_lblMinSpeed.setText(_nf1.format(speed) + UI.SPACE + UI.UNIT_LABEL_SPEED);
	}

	/**
	 * Property has changed, fire a property change event
	 */
	private void onChangeSpeedProperties() {

		// set new values in the pref store

		// spinner: compute value time slice
		_prefStore.setValue(
				ITourbookPreferences.APP_DATA_SPEED_MIN_TIMESLICE_VALUE,
				_spinnerSpeedMinTime.getSelection());

		// force all tours to recompute the speed
		TourManager.getInstance().clearTourDataCache();

		// fire unique event for all changes
		TourManager.fireEvent(TourEventId.TOUR_CHART_PROPERTY_IS_MODIFIED, null);
	}

	private void onComputeBreakTimeValues() {

		if (MessageDialog.openConfirm(
				Display.getCurrent().getActiveShell(),
				Messages.Compute_BreakTime_Dialog_ComputeForAllTours_Title,
				Messages.Compute_BreakTime_Dialog_ComputeForAllTours_Message) == false) {
			return;
		}

		saveState();

		final int[] oldBreakTime = { 0 };
		final int[] newBreakTime = { 0 };

		TourDatabase.computeValuesForAllTours(new IComputeTourValues() {

			public boolean computeTourValues(final TourData oldTourData) {

				final int tourRecordingTime = oldTourData.getTourRecordingTime();

				// get old break time
				final int tourDrivingTime = oldTourData.getTourDrivingTime();
				oldBreakTime[0] += tourRecordingTime - tourDrivingTime;

				// force the break time to be recomputed with the current values which are already store in the pref store
				oldTourData.breakTimeSerie = null;

				// recompute break time
				oldTourData.computeTourDrivingTime();

				return true;
			}

			public String getResultText() {

				return NLS.bind(Messages.Compute_BreakTime_ForAllTour_Job_Result, //
						new Object[] { UI.format_hh_mm_ss(oldBreakTime[0]), UI.format_hh_mm_ss(newBreakTime[0]), });
			}

			public String getSubTaskText(final TourData savedTourData) {

				String subTaskText = null;

				if (savedTourData != null) {

					// get new value
					final int tourRecordingTime = savedTourData.getTourRecordingTime();

					// get old break time
					final int tourDrivingTime = savedTourData.getTourDrivingTime();
					newBreakTime[0] += tourRecordingTime - tourDrivingTime;

					subTaskText = NLS.bind(Messages.Compute_BreakTime_ForAllTour_Job_SubTask,//
							new Object[] { UI.format_hh_mm_ss(oldBreakTime[0]), UI.format_hh_mm_ss(newBreakTime[0]), });
				}

				return subTaskText;
			}
		});

		fireTourModifyEvent();
	}

	private void onComputeElevationGainValues() {

		final int altiMin = ALTITUDE_MINIMUM[_comboMinAltitude.getSelectionIndex()];

		if (MessageDialog.openConfirm(
				Display.getCurrent().getActiveShell(),
				Messages.compute_tourValueElevation_dlg_computeValues_title,
				NLS.bind(
						Messages.compute_tourValueElevation_dlg_computeValues_message,
						Integer.toString((int) (altiMin / UI.UNIT_VALUE_ALTITUDE)),
						UI.UNIT_LABEL_ALTITUDE)) == false) {
			return;
		}

		saveState();

		_nf0.setMinimumFractionDigits(0);
		_nf0.setMaximumFractionDigits(0);
		final int[] elevation = new int[] { 0, 0 };

		TourDatabase.computeValuesForAllTours(new IComputeTourValues() {

			public boolean computeTourValues(final TourData oldTourData) {

				// keep old value
				elevation[0] += oldTourData.getTourAltUp();

				return oldTourData.computeAltitudeUpDown();
			}

			public String getResultText() {

				final int prefMinAltitude = TourbookPlugin.getDefault()//
						.getPreferenceStore()
						.getInt(PrefPageComputedValues.STATE_COMPUTED_VALUE_MIN_ALTITUDE);

				return NLS.bind(Messages.compute_tourValueElevation_resultText, //
						new Object[] {
								prefMinAltitude,
								UI.UNIT_LABEL_ALTITUDE,
								_nf0.format((elevation[1] - elevation[0]) / UI.UNIT_VALUE_ALTITUDE),
								UI.UNIT_LABEL_ALTITUDE //
						});
			}

			public String getSubTaskText(final TourData savedTourData) {

				String subTaskText = null;

				if (savedTourData != null) {

					// get new value
					elevation[1] += savedTourData.getTourAltUp();

					subTaskText = NLS.bind(Messages.compute_tourValueElevation_subTaskText,//
							new Object[] {
									_nf0.format((elevation[1] - elevation[0]) / UI.UNIT_VALUE_ALTITUDE),
									UI.UNIT_LABEL_ALTITUDE //
							});
				}

				return subTaskText;
			}
		});

		fireTourModifyEvent();
	}

	private void onComputeSpeedValues() {

		if (MessageDialog.openConfirm(
				Display.getCurrent().getActiveShell(),
				Messages.compute_tourValueSpeed_dlg_computeValues_title,
				NLS.bind(
						Messages.compute_tourValueSpeed_dlg_computeValues_message,
						_spinnerSpeedMinTime.getSelection(),
						Messages.app_unit_seconds//
						)) == false) {
			return;
		}

		saveState();

		final int[] tourMax = new int[] { 0, 0 };

		TourDatabase.computeValuesForAllTours(new IComputeTourValues() {

			public boolean computeTourValues(final TourData oldTourData) {

				// get old value
				tourMax[0] += oldTourData.getMaxSpeed();

				oldTourData.computeSpeedSerie();

				return true;
			}

			public String getResultText() {
				return NLS.bind(Messages.compute_tourValueSpeed_resultText, //
						new Object[] {
								_spinnerSpeedMinTime.getSelection(),
								Messages.app_unit_seconds,
								_nf0.format((tourMax[1] - tourMax[0]) / UI.UNIT_VALUE_DISTANCE),
								UI.UNIT_LABEL_SPEED //
						});
			}

			public String getSubTaskText(final TourData savedTourData) {

				String subTaskText = null;

				if (savedTourData != null) {

					// get new value
					tourMax[1] += savedTourData.getMaxSpeed();

					subTaskText = NLS.bind(Messages.compute_tourValueSpeed_subTaskText,//
							new Object[] {
									_nf0.format((tourMax[1] - tourMax[0]) / UI.UNIT_VALUE_DISTANCE),
									UI.UNIT_LABEL_SPEED //
							});
				}

				return subTaskText;
			}
		});

		fireTourModifyEvent();
	}

	private void onSelectBreakTimeMethod() {

		switch (_comboBreakMethod.getSelectionIndex()) {
		case BreakTimeTool.BREAK_TIME_METHOD_BY_TIME_DISTANCE:
			_pagebookBreakTime.showPage(_pageBreakByTimeDistance);
			break;

		case BreakTimeTool.BREAK_TIME_METHOD_BY_SLICE_SPEED:
			_pagebookBreakTime.showPage(_pageBreakBySliceSpeed);
			break;

		case BreakTimeTool.BREAK_TIME_METHOD_BY_AVG_SPEED:
			_pagebookBreakTime.showPage(_pageBreakByAvgSpeed);
			break;

		default: // BREAK_TIME_METHOD_BY_TIME_DISTANCE
			_pagebookBreakTime.showPage(_pageBreakByTimeDistance);
			break;
		}

		// break method pages have different heights, enforce layout of the whole view part
		_tabFolder.layout(true, true);

		onChangeBreakTime();
	}

	@Override
	public boolean performCancel() {
		saveUIState();
		return super.performCancel();
	}

	@Override
	protected void performDefaults() {

		/*
		 * compute altitude
		 */
		_comboMinAltitude.select(DEFAULT_MIN_ALTITUDE_INDEX);

		/*
		 * compute speed
		 */
		final int defaultInt = _prefStore.getDefaultInt(ITourbookPreferences.APP_DATA_SPEED_MIN_TIMESLICE_VALUE);
		_spinnerSpeedMinTime.setSelection(defaultInt);

		/*
		 * compute break time
		 */
		final int prefBreakTimeMethod = _prefStore.getDefaultInt(ITourbookPreferences.BREAK_TIME_METHOD);

		final int prefShortestTime = _prefStore.getDefaultInt(ITourbookPreferences.BREAK_TIME_SHORTEST_TIME);
		final float prefMaxDistance = _prefStore.getDefaultFloat(ITourbookPreferences.BREAK_TIME_MAX_DISTANCE);

		final float prefMinSliceSpeed = _prefStore.getDefaultFloat(ITourbookPreferences.BREAK_TIME_MIN_SLICE_SPEED);
		final float prefMinAvgSpeed = _prefStore.getDefaultFloat(ITourbookPreferences.BREAK_TIME_MIN_AVG_SPEED);

		// break method
		_comboBreakMethod.select(prefBreakTimeMethod);

		// break time by time/distance
		_spinnerBreakShortestTime.setSelection(prefShortestTime);
		final float breakDistance = prefMaxDistance / UI.UNIT_VALUE_DISTANCE_SMALL;
		_spinnerBreakMaxDistance.setSelection((int) (breakDistance + 0.5));

		// break by speed
		_spinnerBreakMinSliceSpeed.setSelection((int) (prefMinSliceSpeed * SPEED_DIGIT_VALUE * UI.UNIT_VALUE_DISTANCE));
		_spinnerBreakMinAvgSpeed.setSelection((int) (prefMinAvgSpeed * SPEED_DIGIT_VALUE * UI.UNIT_VALUE_DISTANCE));

		onSelectBreakTimeMethod();

		onChangeSpeedProperties();

		super.performDefaults();
	}

	@Override
	public boolean performOk() {

		saveState();

		return super.performOk();
	}

	private void restoreState() {

		/*
		 * find pref minAlti value in list
		 */
		final int prefMinAltitude = _prefStore.getInt(STATE_COMPUTED_VALUE_MIN_ALTITUDE);

		int minAltiIndex = -1;
		int listIndex = 0;
		for (final int minAltiInList : ALTITUDE_MINIMUM) {
			if (minAltiInList == prefMinAltitude) {
				minAltiIndex = listIndex;
				break;
			}

			listIndex++;
		}

		if (minAltiIndex == -1) {
			minAltiIndex = DEFAULT_MIN_ALTITUDE_INDEX;
		}

		_comboMinAltitude.select(minAltiIndex);

		/*
		 * compute speed
		 */
		_spinnerSpeedMinTime.setSelection(_prefStore.getInt(ITourbookPreferences.APP_DATA_SPEED_MIN_TIMESLICE_VALUE));

		/*
		 * break time
		 */
		final int prefBreakTimeMethod = _prefStore.getInt(ITourbookPreferences.BREAK_TIME_METHOD);

		final int prefShortestTime = _prefStore.getInt(ITourbookPreferences.BREAK_TIME_SHORTEST_TIME);
		final float prefMaxDistance = _prefStore.getFloat(ITourbookPreferences.BREAK_TIME_MAX_DISTANCE);

		final float prefMinSliceSpeed = _prefStore.getFloat(ITourbookPreferences.BREAK_TIME_MIN_SLICE_SPEED);
		final float prefMinAvgSpeed = _prefStore.getFloat(ITourbookPreferences.BREAK_TIME_MIN_AVG_SPEED);

		// method
		_comboBreakMethod.select(prefBreakTimeMethod);

		// break time by time/distance
		_spinnerBreakShortestTime.setSelection(prefShortestTime);
		final float breakDistance = prefMaxDistance / UI.UNIT_VALUE_DISTANCE_SMALL;
		_spinnerBreakMaxDistance.setSelection((int) (breakDistance + 0.5));

		// break by speed
		_spinnerBreakMinSliceSpeed.setSelection((int) (prefMinSliceSpeed * SPEED_DIGIT_VALUE * UI.UNIT_VALUE_DISTANCE));
		_spinnerBreakMinAvgSpeed.setSelection((int) (prefMinAvgSpeed * SPEED_DIGIT_VALUE * UI.UNIT_VALUE_DISTANCE));

		// compute min speed
		onChangeBreakTime();

		/*
		 * folder
		 */
		_tabFolder.setSelection(_prefStore.getInt(STATE_COMPUTED_VALUE_SELECTED_TAB));

		onSelectBreakTimeMethod();
	}

	/**
	 * save values in the pref store
	 */
	private void saveState() {

		_prefStore.setValue(STATE_COMPUTED_VALUE_MIN_ALTITUDE, ALTITUDE_MINIMUM[_comboMinAltitude.getSelectionIndex()]);
		_prefStore.setValue(
				ITourbookPreferences.APP_DATA_SPEED_MIN_TIMESLICE_VALUE,
				_spinnerSpeedMinTime.getSelection());

		/*
		 * break time properties
		 */
		_prefStore.setValue(ITourbookPreferences.BREAK_TIME_METHOD, _comboBreakMethod.getSelectionIndex());

		// by time/distance
		_prefStore.setValue(ITourbookPreferences.BREAK_TIME_SHORTEST_TIME, _spinnerBreakShortestTime.getSelection());

		final float breakDistance = _spinnerBreakMaxDistance.getSelection() * UI.UNIT_VALUE_DISTANCE_SMALL;
		_prefStore.setValue(ITourbookPreferences.BREAK_TIME_MAX_DISTANCE, breakDistance);

		// by slice speed
		final float breakMinSliceSpeed = _spinnerBreakMinSliceSpeed.getSelection()
				/ SPEED_DIGIT_VALUE
				/ UI.UNIT_VALUE_DISTANCE;
		_prefStore.setValue(ITourbookPreferences.BREAK_TIME_MIN_SLICE_SPEED, breakMinSliceSpeed);

		// by avg speed
		final float breakMinAvgSpeed = _spinnerBreakMinAvgSpeed.getSelection()
				/ SPEED_DIGIT_VALUE
				/ UI.UNIT_VALUE_DISTANCE;
		_prefStore.setValue(ITourbookPreferences.BREAK_TIME_MIN_AVG_SPEED, breakMinAvgSpeed);

		/*
		 * notify break time listener
		 */
		_prefStore.setValue(ITourbookPreferences.BREAK_TIME_IS_MODIFIED, Math.random());
	}

	private void saveUIState() {
		_prefStore.setValue(STATE_COMPUTED_VALUE_SELECTED_TAB, _tabFolder.getSelectionIndex());
	}

}
