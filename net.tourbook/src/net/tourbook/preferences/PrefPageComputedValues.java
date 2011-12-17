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
import net.tourbook.tour.BreakTimeMethod;
import net.tourbook.tour.BreakTimeTool;
import net.tourbook.tour.TourEventId;
import net.tourbook.tour.TourManager;
import net.tourbook.ui.UI;
import net.tourbook.ui.views.SmoothingUI;
import net.tourbook.util.Util;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
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
import org.eclipse.ui.dialogs.PreferenceLinkArea;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.part.PageBook;
import org.eclipse.ui.preferences.IWorkbenchPreferenceContainer;

public class PrefPageComputedValues extends PreferencePage implements IWorkbenchPreferencePage {

	public static final String			ID									= "net.tourbook.preferences.PrefPageComputedValues";	//$NON-NLS-1$

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
	public static final int				TAB_FOLDER_SMOOTHING				= 0;
	public static final int				TAB_FOLDER_BREAK_TIME				= 1;
	public static final int				TAB_FOLDER_ELEVATION				= 2;

	private static final float			SPEED_DIGIT_VALUE					= 10.0f;

	private static final int			DEFAULT_DESCRIPTION_WIDTH			= 450;

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

	private boolean						_isUpdateUI;
	private SelectionAdapter			_selectionListener;
	private MouseWheelListener			_spinnerMouseWheelListener;

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

	private Combo						_comboBreakMethod;
	private PageBook					_pagebookBreakTime;

	private Composite					_pageBreakByAvgSliceSpeed;
	private Composite					_pageBreakByAvgSpeed;
	private Composite					_pageBreakBySliceSpeed;
	private Composite					_pageBreakByTimeDistance;

	private Label						_lblBreakDistanceUnit;

	private Spinner						_spinnerBreakShortestTime;
	private Spinner						_spinnerBreakMaxDistance;
	private Spinner						_spinnerBreakMinSliceSpeed;
	private Spinner						_spinnerBreakMinAvgSpeed;
	private Spinner						_spinnerBreakSliceDiff;
	private Spinner						_spinnerBreakMinAvgSpeedAS;
	private Spinner						_spinnerBreakMinSliceSpeedAS;
	private Spinner						_spinnerBreakMinSliceTimeAS;

	private ScrolledComposite			_smoothingScrolledContainer;
	private Composite					_smoothingScrolledContent;
	private SmoothingUI					_smoothingUI;

	private FormToolkit					_tk;

	@Override
	public void applyData(final Object data) {

		// data contains the folder index, this is set when the pref page is opened from a link

		if (data instanceof Integer) {
			_tabFolder.setSelection((Integer) data);
		}
	}

	@Override
	protected Control createContents(final Composite parent) {

		initUI(parent);
		final Composite container = createUI(parent);

		restoreState();

		return container;
	}

	private Composite createUI(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults()//
				.grab(true, true)
				.applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(1).spacing(0, 15).applyTo(container);
		{
			/*
			 * label: info
			 */
			final Label label = new Label(container, SWT.WRAP);
			GridDataFactory.fillDefaults().hint(DEFAULT_DESCRIPTION_WIDTH, SWT.DEFAULT).applyTo(label);
			label.setText(Messages.Compute_Values_Label_Info);

			/*
			 * tab folder: computed values
			 */
			_tabFolder = new TabFolder(container, SWT.TOP);
			GridDataFactory.fillDefaults()//
					.grab(true, true)
//					.indent(40, 20)
					.applyTo(_tabFolder);
			{

				final TabItem tabSmoothing = new TabItem(_tabFolder, SWT.NONE);
				tabSmoothing.setControl(createUI10Smoothing(_tabFolder));
				tabSmoothing.setText(Messages.Compute_Values_Group_Smoothing);

				final TabItem tabBreakTime = new TabItem(_tabFolder, SWT.NONE);
				tabBreakTime.setControl(createUI50BreakTime(_tabFolder));
				tabBreakTime.setText(Messages.Compute_BreakTime_Group_BreakTime);

				final TabItem tabElevation = new TabItem(_tabFolder, SWT.NONE);
				tabElevation.setControl(createUI20ElevationGain(_tabFolder));
				tabElevation.setText(Messages.compute_tourValueElevation_group_computeTourAltitude);

				final TabItem tabHrZone = new TabItem(_tabFolder, SWT.NONE);
				tabHrZone.setControl(createUI60HrZone(_tabFolder));
				tabHrZone.setText(Messages.Compute_HrZone_Group);

				/**
				 * 4.8.2009 week no/year is currently disabled because a new field in the db is
				 * required which holds the year of the week<br>
				 * <br>
				 * all plugins must be adjusted which set's the week number of a tour
				 */
//				createUIWeek(container);
			}
		}

		return _tabFolder;
	}

	private Control createUI10Smoothing(final Composite parent) {

		_tk = new FormToolkit(parent.getDisplay());
		_smoothingUI = new SmoothingUI();

		_smoothingScrolledContainer = new ScrolledComposite(parent, SWT.V_SCROLL);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(_smoothingScrolledContainer);
		{
			_smoothingScrolledContent = _tk.createComposite(_smoothingScrolledContainer);
			GridDataFactory.fillDefaults()//
					.grab(true, true)
					.hint(DEFAULT_DESCRIPTION_WIDTH, SWT.DEFAULT)
					.applyTo(_smoothingScrolledContent);
			GridLayoutFactory.swtDefaults() //
					.extendedMargins(5, 5, 10, 5)
					.numColumns(1)
					.applyTo(_smoothingScrolledContent);
//			_smoothingScrolledContent.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_MAGENTA));
			{
				_smoothingUI.createUI(_smoothingScrolledContent, true);
			}

			// setup scrolled container
			_smoothingScrolledContainer.setExpandVertical(true);
			_smoothingScrolledContainer.setExpandHorizontal(true);
			_smoothingScrolledContainer.addControlListener(new ControlAdapter() {
				@Override
				public void controlResized(final ControlEvent e) {
					_smoothingScrolledContainer.setMinSize(//
							_smoothingScrolledContent.computeSize(SWT.DEFAULT, SWT.DEFAULT));
				}
			});

			_smoothingScrolledContainer.setContent(_smoothingScrolledContent);
		}

		return _smoothingScrolledContainer;
	}

	private Control createUI20ElevationGain(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
		GridLayoutFactory.swtDefaults().extendedMargins(5, 5, 10, 5).numColumns(3).applyTo(container);
//		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));
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
					.hint(DEFAULT_DESCRIPTION_WIDTH, SWT.DEFAULT)
					.grab(true, false)
					.applyTo(label);
			label.setText(Messages.compute_tourValueElevation_label_description);

			UI.createBullets(container, //
					Messages.compute_tourValueElevation_label_description_Hints,
					1,
					3,
					DEFAULT_DESCRIPTION_WIDTH,
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

	private Composite createUI50BreakTime(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
		GridLayoutFactory.swtDefaults().extendedMargins(5, 5, 10, 5).numColumns(2).applyTo(container);

//		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_MAGENTA));
		{
			final Composite containerTitle = new Composite(container, SWT.NONE);
			GridDataFactory.fillDefaults().grab(true, false).span(2, 1).applyTo(containerTitle);
			GridLayoutFactory.fillDefaults().extendedMargins(0, 0, 0, 10).numColumns(1).applyTo(containerTitle);
			{
				/*
				 * label: compute break time by
				 */
				final Label label = new Label(containerTitle, SWT.NONE);
				label.setText(Messages.Compute_BreakTime_Label_Title);
			}

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
					updateUIShowSelectedBreakTimeMethod();
					onModifyBreakTime();
				}
			});

			// fill combo
			for (final BreakTimeMethod breakMethod : BreakTimeTool.BREAK_TIME_METHODS) {
				_comboBreakMethod.add(breakMethod.uiText);
			}

			/*
			 * pagebook: break method
			 */
			_pagebookBreakTime = new PageBook(container, SWT.NONE);
			GridDataFactory.fillDefaults().span(2, 1).grab(true, false).applyTo(_pagebookBreakTime);
			{
				_pageBreakByAvgSliceSpeed = createUI51BreakByAvgSliceSpeed(_pagebookBreakTime);
				_pageBreakByAvgSpeed = createUI52BreakByAvgSpeed(_pagebookBreakTime);
				_pageBreakBySliceSpeed = createUI53BreakBySliceSpeed(_pagebookBreakTime);
				_pageBreakByTimeDistance = createUI54BreakByTimeDistance(_pagebookBreakTime);
			}

			/*
			 * label: description part II
			 */
			label = new Label(container, SWT.WRAP);
			GridDataFactory.fillDefaults()//
					.span(2, 1)
					.indent(0, 10)
					.hint(DEFAULT_DESCRIPTION_WIDTH, SWT.DEFAULT)
					.grab(true, false)
					.applyTo(label);
			label.setText(Messages.Compute_BreakTime_Label_Description);

			/*
			 * hints
			 */
			UI.createBullets(container, //
					Messages.Compute_BreakTime_Label_Hints,
					1,
					2,
					DEFAULT_DESCRIPTION_WIDTH,
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

	private Composite createUI51BreakByAvgSliceSpeed(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(3).applyTo(container);
//		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
		{
			/*
			 * minimum average speed
			 */
			{
				// label: minimum speed
				Label label = new Label(container, SWT.NONE);
				label.setText(Messages.Compute_BreakTime_Label_MinimumAvgSpeed);
				_firstColBreakTime.add(label);

				// spinner: minimum speed
				_spinnerBreakMinAvgSpeedAS = new Spinner(container, SWT.BORDER);
				GridDataFactory.fillDefaults()//
						.applyTo(_spinnerBreakMinAvgSpeedAS);
				_spinnerBreakMinAvgSpeedAS.setMinimum(0); // 0.0 km/h
				_spinnerBreakMinAvgSpeedAS.setMaximum(100); // 10.0 km/h
				_spinnerBreakMinAvgSpeedAS.setDigits(1);
				_spinnerBreakMinAvgSpeedAS.addSelectionListener(_selectionListener);
				_spinnerBreakMinAvgSpeedAS.addMouseWheelListener(_spinnerMouseWheelListener);

				// label: km/h
				label = new Label(container, SWT.NONE);
				label.setText(UI.UNIT_LABEL_SPEED);
			}

			/*
			 * minimum slice speed
			 */
			{
				// label: minimum speed
				Label label = new Label(container, SWT.NONE);
				label.setText(Messages.Compute_BreakTime_Label_MinimumSliceSpeed);
				_firstColBreakTime.add(label);

				// spinner: minimum speed
				_spinnerBreakMinSliceSpeedAS = new Spinner(container, SWT.BORDER);
				GridDataFactory.fillDefaults()//
						.applyTo(_spinnerBreakMinSliceSpeedAS);
				_spinnerBreakMinSliceSpeedAS.setMinimum(0); // 0.0 km/h
				_spinnerBreakMinSliceSpeedAS.setMaximum(100); // 10.0 km/h
				_spinnerBreakMinSliceSpeedAS.setDigits(1);
				_spinnerBreakMinSliceSpeedAS.addSelectionListener(_selectionListener);
				_spinnerBreakMinSliceSpeedAS.addMouseWheelListener(_spinnerMouseWheelListener);

				// label: km/h
				label = new Label(container, SWT.NONE);
				label.setText(UI.UNIT_LABEL_SPEED);
			}

			/*
			 * minimum slice time
			 */
			{
				// label: minimum slice time
				Label label = new Label(container, SWT.NONE);
				label.setText(Messages.Compute_BreakTime_Label_MinimumSliceTime);
				_firstColBreakTime.add(label);

				// spinner: minimum slice time
				_spinnerBreakMinSliceTimeAS = new Spinner(container, SWT.BORDER);
				GridDataFactory.fillDefaults()//
						.applyTo(_spinnerBreakMinSliceTimeAS);
				_spinnerBreakMinSliceTimeAS.setMinimum(0); // 0 sec
				_spinnerBreakMinSliceTimeAS.setMaximum(10); // 10 sec
				_spinnerBreakMinSliceTimeAS.addSelectionListener(_selectionListener);
				_spinnerBreakMinSliceTimeAS.addMouseWheelListener(_spinnerMouseWheelListener);

				// label: seconds
				label = new Label(container, SWT.NONE);
				label.setText(Messages.app_unit_seconds);
			}

			/*
			 * label: description
			 */
			final Label label = new Label(container, SWT.WRAP);
			GridDataFactory.fillDefaults()//
					.span(3, 1)
					.indent(0, 10)
					.hint(DEFAULT_DESCRIPTION_WIDTH, SWT.DEFAULT)
					.grab(true, false)
					.applyTo(label);
			label.setText(Messages.Compute_BreakTime_Label_Description_ComputeByAvgSliceSpeed);
		}

		return container;
	}

	private Composite createUI52BreakByAvgSpeed(final Composite parent) {

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
			GridDataFactory.fillDefaults()//
					.applyTo(_spinnerBreakMinAvgSpeed);
			_spinnerBreakMinAvgSpeed.setMinimum(0); // 0.0 km/h
			_spinnerBreakMinAvgSpeed.setMaximum(100); // 10.0 km/h
			_spinnerBreakMinAvgSpeed.setDigits(1);
			_spinnerBreakMinAvgSpeed.addSelectionListener(_selectionListener);
			_spinnerBreakMinAvgSpeed.addMouseWheelListener(_spinnerMouseWheelListener);

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
					.hint(DEFAULT_DESCRIPTION_WIDTH, SWT.DEFAULT)
					.grab(true, false)
					.applyTo(label);
			label.setText(Messages.Compute_BreakTime_Label_Description_ComputeByAvgSpeed);
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
			 * minimum slice speed
			 */

			// label: minimum speed
			Label label = new Label(container, SWT.NONE);
			label.setText(Messages.Compute_BreakTime_Label_MinimumSliceSpeed);
			_firstColBreakTime.add(label);

			// spinner: minimum speed
			_spinnerBreakMinSliceSpeed = new Spinner(container, SWT.BORDER);
			GridDataFactory.fillDefaults()//
					.applyTo(_spinnerBreakMinSliceSpeed);
			_spinnerBreakMinSliceSpeed.setMinimum(0); // 0.0 km/h
			_spinnerBreakMinSliceSpeed.setMaximum(100); // 10.0 km/h
			_spinnerBreakMinSliceSpeed.setDigits(1);
			_spinnerBreakMinSliceSpeed.addSelectionListener(_selectionListener);
			_spinnerBreakMinSliceSpeed.addMouseWheelListener(_spinnerMouseWheelListener);

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
					.hint(DEFAULT_DESCRIPTION_WIDTH, SWT.DEFAULT)
					.grab(true, false)
					.applyTo(label);
			label.setText(Messages.Compute_BreakTime_Label_Description_ComputeBySliceSpeed);
		}

		return container;
	}

	private Composite createUI54BreakByTimeDistance(final Composite parent) {

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
				GridDataFactory.fillDefaults()//
						.applyTo(_spinnerBreakShortestTime);
				_spinnerBreakShortestTime.setMinimum(1);
				_spinnerBreakShortestTime.setMaximum(120); // 120 seconds
				_spinnerBreakShortestTime.addSelectionListener(_selectionListener);
				_spinnerBreakShortestTime.addMouseWheelListener(_spinnerMouseWheelListener);

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
				GridDataFactory.fillDefaults()//
						.applyTo(_spinnerBreakMaxDistance);
				_spinnerBreakMaxDistance.setMinimum(1);
				_spinnerBreakMaxDistance.setMaximum(1000); // 1000 m/yards
				_spinnerBreakMaxDistance.addSelectionListener(_selectionListener);
				_spinnerBreakMaxDistance.addMouseWheelListener(_spinnerMouseWheelListener);

				// label: unit
				_lblBreakDistanceUnit = new Label(container, SWT.NONE);
				_lblBreakDistanceUnit.setText(UI.UNIT_LABEL_DISTANCE_SMALL);
				GridDataFactory.fillDefaults()//
//						.span(2, 1)
						.align(SWT.FILL, SWT.CENTER)
						.applyTo(_lblBreakDistanceUnit);
			}

			/*
			 * slice diff break
			 */
			{
				// label: break slice diff
				Label label = new Label(container, SWT.NONE);
				label.setText(Messages.Compute_BreakTime_Label_SliceDiffBreak);
				label.setToolTipText(Messages.Compute_BreakTime_Label_SliceDiffBreak_Tooltip);
				_firstColBreakTime.add(label);

				// spinner: slice diff break time
				_spinnerBreakSliceDiff = new Spinner(container, SWT.BORDER);
				GridDataFactory.fillDefaults()//
						.applyTo(_spinnerBreakSliceDiff);
				_spinnerBreakSliceDiff.setMinimum(0);
				_spinnerBreakSliceDiff.setMaximum(60); // minutes
				_spinnerBreakSliceDiff.addSelectionListener(_selectionListener);
				_spinnerBreakSliceDiff.addMouseWheelListener(_spinnerMouseWheelListener);

				// label: unit
				label = new Label(container, SWT.NONE);
				label.setText(Messages.App_Unit_Minute);
			}

			/*
			 * label: description
			 */
			final Label label = new Label(container, SWT.WRAP);
			GridDataFactory.fillDefaults()//
					.span(3, 1)
					.indent(0, 10)
					.hint(DEFAULT_DESCRIPTION_WIDTH, SWT.DEFAULT)
					.grab(true, false)
					.applyTo(label);
			label.setText(Messages.Compute_BreakTime_Label_Description_ComputeByTime);
		}

		return container;
	}

	private Control createUI60HrZone(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
		GridLayoutFactory.swtDefaults().extendedMargins(5, 5, 10, 5).numColumns(1).applyTo(container);
		{
			final PreferenceLinkArea prefLink = new PreferenceLinkArea(
					container,
					SWT.NONE,
					PrefPagePeople.ID,
					Messages.Compute_HrZone_Link,
					(IWorkbenchPreferenceContainer) getContainer(),
					new PrefPagePeopleData(PrefPagePeople.PREF_DATA_SELECT_HR_ZONES, null));

			GridDataFactory.fillDefaults()//
					.grab(true, false)
					.hint(DEFAULT_DESCRIPTION_WIDTH, SWT.DEFAULT)
					.applyTo(prefLink.getControl());
		}

		return container;
	}

	@Override
	public void dispose() {

		_smoothingUI.dispose();

		super.dispose();
	}

	private void fireTourModifyEvent() {

		TourManager.getInstance().removeAllToursFromCache();
		TourManager.fireEvent(TourEventId.CLEAR_DISPLAYED_TOUR);

		// fire unique event for all changes
		TourManager.fireEvent(TourEventId.ALL_TOURS_ARE_MODIFIED);
	}

	private BreakTimeMethod getSelectedBreakMethod() {

		int selectedIndex = _comboBreakMethod.getSelectionIndex();

		if (selectedIndex == -1) {
			selectedIndex = 0;
		}

		return BreakTimeTool.BREAK_TIME_METHODS[selectedIndex];
	}

	public void init(final IWorkbench workbench) {}

	private void initUI(final Composite parent) {

		_selectionListener = new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				if (_isUpdateUI) {
					return;
				}
				onModifyBreakTime();
			}
		};

		_spinnerMouseWheelListener = new MouseWheelListener() {
			@Override
			public void mouseScrolled(final MouseEvent event) {
				if (_isUpdateUI) {
					return;
				}
				Util.adjustSpinnerValueOnMouseScroll(event);
				onModifyBreakTime();
			}
		};
	}

	@Override
	public boolean okToLeave() {
		saveUIState();
		return super.okToLeave();
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
				oldTourData.setBreakTimeSerie(null);

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

	private void onModifyBreakTime() {

		saveState();

		TourManager.getInstance().removeAllToursFromCache();

		// fire unique event for all changes
		TourManager.fireEvent(TourEventId.TOUR_CHART_PROPERTY_IS_MODIFIED, null);
	}

	@Override
	public boolean performCancel() {
		saveUIState();
		return super.performCancel();
	}

	@Override
	protected void performDefaults() {

		if (_tabFolder.getSelectionIndex() == TAB_FOLDER_ELEVATION) {

			/*
			 * compute altitude
			 */
			_comboMinAltitude.select(DEFAULT_MIN_ALTITUDE_INDEX);

		} else if (_tabFolder.getSelectionIndex() == TAB_FOLDER_SMOOTHING) {

			/*
			 * compute smoothing
			 */
			_smoothingUI.performDefaults();

		} else if (_tabFolder.getSelectionIndex() == TAB_FOLDER_BREAK_TIME) {

			_isUpdateUI = true;
			{
				/*
				 * break method
				 */
				final String prefBreakTimeMethod = _prefStore.getDefaultString(ITourbookPreferences.BREAK_TIME_METHOD2);
				selectBreakMethod(prefBreakTimeMethod);

				/*
				 * break by avg+slice speed
				 */
				final float prefMinAvgSpeedAS = _prefStore.getDefaultFloat(//
						ITourbookPreferences.BREAK_TIME_MIN_AVG_SPEED_AS);
				final float prefMinSliceSpeedAS = _prefStore.getDefaultFloat(//
						ITourbookPreferences.BREAK_TIME_MIN_SLICE_SPEED_AS);
				final int prefMinSliceTimeAS = _prefStore.getDefaultInt(//
						ITourbookPreferences.BREAK_TIME_MIN_SLICE_TIME_AS);

				_spinnerBreakMinAvgSpeedAS.setSelection(//
						(int) (prefMinAvgSpeedAS * SPEED_DIGIT_VALUE * UI.UNIT_VALUE_DISTANCE));
				_spinnerBreakMinSliceSpeedAS.setSelection(//
						(int) (prefMinSliceSpeedAS * SPEED_DIGIT_VALUE * UI.UNIT_VALUE_DISTANCE));
				_spinnerBreakMinSliceTimeAS.setSelection(prefMinSliceTimeAS);

				/*
				 * break by speed
				 */
				final float prefMinSliceSpeed = _prefStore
						.getDefaultFloat(ITourbookPreferences.BREAK_TIME_MIN_SLICE_SPEED);
				final float prefMinAvgSpeed = _prefStore.getDefaultFloat(ITourbookPreferences.BREAK_TIME_MIN_AVG_SPEED);

				_spinnerBreakMinSliceSpeed.setSelection(//
						(int) (prefMinSliceSpeed * SPEED_DIGIT_VALUE * UI.UNIT_VALUE_DISTANCE));
				_spinnerBreakMinAvgSpeed.setSelection(//
						(int) (prefMinAvgSpeed * SPEED_DIGIT_VALUE * UI.UNIT_VALUE_DISTANCE));

				/*
				 * break time by time/distance
				 */
				final int prefShortestTime = _prefStore.getDefaultInt(ITourbookPreferences.BREAK_TIME_SHORTEST_TIME);
				final float prefMaxDistance = _prefStore.getDefaultFloat(ITourbookPreferences.BREAK_TIME_MAX_DISTANCE);
				final int prefSliceDiff = _prefStore.getDefaultInt(ITourbookPreferences.BREAK_TIME_SLICE_DIFF);
				final float breakDistance = prefMaxDistance / UI.UNIT_VALUE_DISTANCE_SMALL;

				_spinnerBreakShortestTime.setSelection(prefShortestTime);
				_spinnerBreakMaxDistance.setSelection((int) (breakDistance + 0.5));
				_spinnerBreakSliceDiff.setSelection(prefSliceDiff);

				updateUIShowSelectedBreakTimeMethod();
			}
			_isUpdateUI = false;

			// keep state and fire event
			onModifyBreakTime();
		}

		super.performDefaults();
	}

	@Override
	public boolean performOk() {

		saveState();

		return super.performOk();
	}

	private void restoreState() {

		_isUpdateUI = true;
		{
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
			 * break method
			 */
			final String prefBreakTimeMethod = _prefStore.getString(ITourbookPreferences.BREAK_TIME_METHOD2);
			selectBreakMethod(prefBreakTimeMethod);

			/*
			 * break by avg+slice speed
			 */
			final float prefMinAvgSpeedAS = _prefStore.getFloat(ITourbookPreferences.BREAK_TIME_MIN_AVG_SPEED_AS);
			final float prefMinSliceSpeedAS = _prefStore.getFloat(ITourbookPreferences.BREAK_TIME_MIN_SLICE_SPEED_AS);
			final int prefMinSliceTimeAS = _prefStore.getInt(ITourbookPreferences.BREAK_TIME_MIN_SLICE_TIME_AS);
			_spinnerBreakMinAvgSpeedAS.setSelection(//
					(int) (prefMinAvgSpeedAS * SPEED_DIGIT_VALUE * UI.UNIT_VALUE_DISTANCE));
			_spinnerBreakMinSliceSpeedAS.setSelection(//
					(int) (prefMinSliceSpeedAS * SPEED_DIGIT_VALUE * UI.UNIT_VALUE_DISTANCE));
			_spinnerBreakMinSliceTimeAS.setSelection(prefMinSliceTimeAS);

			/*
			 * break by speed
			 */
			final float prefMinSliceSpeed = _prefStore.getFloat(ITourbookPreferences.BREAK_TIME_MIN_SLICE_SPEED);
			final float prefMinAvgSpeed = _prefStore.getFloat(ITourbookPreferences.BREAK_TIME_MIN_AVG_SPEED);
			_spinnerBreakMinSliceSpeed.setSelection(//
					(int) (prefMinSliceSpeed * SPEED_DIGIT_VALUE * UI.UNIT_VALUE_DISTANCE));
			_spinnerBreakMinAvgSpeed.setSelection(//
					(int) (prefMinAvgSpeed * SPEED_DIGIT_VALUE * UI.UNIT_VALUE_DISTANCE));

			/*
			 * break time by time/distance
			 */
			final int prefShortestTime = _prefStore.getInt(ITourbookPreferences.BREAK_TIME_SHORTEST_TIME);
			final float prefMaxDistance = _prefStore.getFloat(ITourbookPreferences.BREAK_TIME_MAX_DISTANCE);
			final int prefSliceDiff = _prefStore.getInt(ITourbookPreferences.BREAK_TIME_SLICE_DIFF);
			final float breakDistance = prefMaxDistance / UI.UNIT_VALUE_DISTANCE_SMALL;
			_spinnerBreakShortestTime.setSelection(prefShortestTime);
			_spinnerBreakMaxDistance.setSelection((int) (breakDistance + 0.5));
			_spinnerBreakSliceDiff.setSelection(prefSliceDiff);

			/*
			 * folder
			 */
			_tabFolder.setSelection(_prefStore.getInt(STATE_COMPUTED_VALUE_SELECTED_TAB));

			updateUIShowSelectedBreakTimeMethod();
		}
		_isUpdateUI = false;
	}

	/**
	 * save values in the pref store
	 */
	private void saveState() {

		_prefStore.setValue(STATE_COMPUTED_VALUE_MIN_ALTITUDE, ALTITUDE_MINIMUM[_comboMinAltitude.getSelectionIndex()]);

		/*
		 * break time method
		 */
		_prefStore.setValue(ITourbookPreferences.BREAK_TIME_METHOD2, getSelectedBreakMethod().methodId);

		/*
		 * break by average+slice speed
		 */
		final float breakMinAvgSpeedAS = _spinnerBreakMinAvgSpeedAS.getSelection()
				/ SPEED_DIGIT_VALUE
				/ UI.UNIT_VALUE_DISTANCE;
		final float breakMinSliceSpeedAS = _spinnerBreakMinSliceSpeedAS.getSelection()
				/ SPEED_DIGIT_VALUE
				/ UI.UNIT_VALUE_DISTANCE;
		final int breakMinSliceTimeAS = _spinnerBreakMinSliceTimeAS.getSelection();

		_prefStore.setValue(ITourbookPreferences.BREAK_TIME_MIN_AVG_SPEED_AS, breakMinAvgSpeedAS);
		_prefStore.setValue(ITourbookPreferences.BREAK_TIME_MIN_SLICE_SPEED_AS, breakMinSliceSpeedAS);
		_prefStore.setValue(ITourbookPreferences.BREAK_TIME_MIN_SLICE_TIME_AS, breakMinSliceTimeAS);

		/*
		 * break by slice speed
		 */
		final float breakMinSliceSpeed = _spinnerBreakMinSliceSpeed.getSelection()
				/ SPEED_DIGIT_VALUE
				/ UI.UNIT_VALUE_DISTANCE;
		_prefStore.setValue(ITourbookPreferences.BREAK_TIME_MIN_SLICE_SPEED, breakMinSliceSpeed);

		/*
		 * break by avg speed
		 */
		final float breakMinAvgSpeed = _spinnerBreakMinAvgSpeed.getSelection()
				/ SPEED_DIGIT_VALUE
				/ UI.UNIT_VALUE_DISTANCE;
		_prefStore.setValue(ITourbookPreferences.BREAK_TIME_MIN_AVG_SPEED, breakMinAvgSpeed);

		/*
		 * break by time/distance
		 */
		_prefStore.setValue(ITourbookPreferences.BREAK_TIME_SHORTEST_TIME, _spinnerBreakShortestTime.getSelection());

		final float breakDistance = _spinnerBreakMaxDistance.getSelection() * UI.UNIT_VALUE_DISTANCE_SMALL;
		_prefStore.setValue(ITourbookPreferences.BREAK_TIME_MAX_DISTANCE, breakDistance);

		_prefStore.setValue(ITourbookPreferences.BREAK_TIME_SLICE_DIFF, _spinnerBreakSliceDiff.getSelection());

		/*
		 * notify break time listener
		 */
		_prefStore.setValue(ITourbookPreferences.BREAK_TIME_IS_MODIFIED, Math.random());
	}

	private void saveUIState() {

		if (_tabFolder == null || _tabFolder.isDisposed()) {
			return;
		}

		_prefStore.setValue(STATE_COMPUTED_VALUE_SELECTED_TAB, _tabFolder.getSelectionIndex());
	}

	private void selectBreakMethod(final String methodId) {

		final BreakTimeMethod[] breakMethods = BreakTimeTool.BREAK_TIME_METHODS;

		int selectionIndex = -1;

		for (int methodIndex = 0; methodIndex < breakMethods.length; methodIndex++) {
			if (breakMethods[methodIndex].methodId.equals(methodId)) {
				selectionIndex = methodIndex;
				break;
			}
		}

		if (selectionIndex == -1) {
			selectionIndex = 0;
		}

		_comboBreakMethod.select(selectionIndex);
	}

	private void updateUIShowSelectedBreakTimeMethod() {

		final BreakTimeMethod selectedBreakMethod = getSelectedBreakMethod();

		if (selectedBreakMethod.methodId.equals(BreakTimeTool.BREAK_TIME_METHOD_BY_AVG_SPEED)) {

			_pagebookBreakTime.showPage(_pageBreakByAvgSpeed);

		} else if (selectedBreakMethod.methodId.equals(BreakTimeTool.BREAK_TIME_METHOD_BY_SLICE_SPEED)) {

			_pagebookBreakTime.showPage(_pageBreakBySliceSpeed);

		} else if (selectedBreakMethod.methodId.equals(BreakTimeTool.BREAK_TIME_METHOD_BY_AVG_SLICE_SPEED)) {

			_pagebookBreakTime.showPage(_pageBreakByAvgSliceSpeed);

		} else if (selectedBreakMethod.methodId.equals(BreakTimeTool.BREAK_TIME_METHOD_BY_TIME_DISTANCE)) {

			_pagebookBreakTime.showPage(_pageBreakByTimeDistance);
		}

		// break method pages have different heights, enforce layout of the whole view part
		_tabFolder.layout(true, true);

		net.tourbook.util.UI.updateScrolledContent(_tabFolder);
	}

}
