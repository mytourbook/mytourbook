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
package net.tourbook.tour;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;

import net.tourbook.Messages;
import net.tourbook.data.IWeather;
import net.tourbook.data.TourData;
import net.tourbook.plugin.TourbookPlugin;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.ui.UI;
import net.tourbook.ui.views.tourDataEditor.TourDataEditorView;
import net.tourbook.util.Util;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.widgets.Form;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;

public class DialogQuickEdit extends TitleAreaDialog {

	private static final Calendar		_calendar						= GregorianCalendar.getInstance();
	private static final DateFormat		_dateFormatter					= DateFormat.getDateInstance(DateFormat.FULL);
	private static final DateFormat		_timeFormatter					= DateFormat.getTimeInstance(DateFormat.SHORT);

	private final TourData				_tourData;

	private final IDialogSettings		_state;
	private PixelConverter				_pc;

	private FormToolkit					_tk;
	private Form						_formContainer;

	private Text						_txtTitle;
	private Text						_txtDescription;

	private Spinner						_spinWindSpeedValue;
	private Combo						_comboWindSpeedText;
	private Combo						_comboWindDirectionText;
	private Spinner						_spinWindDirectionValue;
	private Spinner						_spinTemperature;
	private CLabel						_lblCloudIcon;
	private Combo						_comboClouds;

	/**
	 * contains the controls which are displayed in the first column, these controls are used to get
	 * the maximum width and set the first column within the differenct section to the same width
	 */
	private final ArrayList<Control>	_firstColumnControls			= new ArrayList<Control>();
	private final ArrayList<Control>	_firstColumnContainerControls	= new ArrayList<Control>();
	private final ArrayList<Control>	_secondColumnControls			= new ArrayList<Control>();

	private int							_defaultSpinnerWidth;

	private boolean						_isUpdateUI						= false;
	private boolean						_isTemperatureManuallyModified	= false;
	private boolean						_isWindSpeedManuallyModified	= false;
	private int[]						_unitValueWindSpeed;
	private float						_unitValueDistance;
	private float						_unitValueTemperature;
	private Spinner						_spinRestPuls;
	private Spinner						_spinTourCalories;

	/**
	 * When <code>true</code> the tour is created with the tour editor
	 */
	private final boolean				_isManualTour;

	private MouseWheelListener			_mouseWheelListener;
	{
		_mouseWheelListener = new MouseWheelListener() {
			public void mouseScrolled(final MouseEvent event) {
				Util.adjustSpinnerValueOnMouseScroll(event);
			}
		};
	}

	public DialogQuickEdit(final Shell parentShell, final TourData tourData) {

		super(parentShell);

		// make dialog resizable
		setShellStyle(getShellStyle() | SWT.RESIZE);

		setDefaultImage(TourbookPlugin.getImageDescriptor(Messages.Image__quick_edit).createImage());

		_tourData = tourData;
		_isManualTour = _tourData.isManualTour();

		_state = TourbookPlugin.getDefault().getDialogSettingsSection(getClass().getName());
	}

	@Override
	protected void configureShell(final Shell shell) {

		super.configureShell(shell);

		shell.setText(Messages.dialog_quick_edit_dialog_title);

		shell.addDisposeListener(new DisposeListener() {
			@Override
			public void widgetDisposed(final DisposeEvent e) {
				onDispose();
			}
		});
	}

	@Override
	public void create() {

		super.create();

		setTitle(Messages.dialog_quick_edit_dialog_area_title);

		_calendar.set(//
				_tourData.getStartYear(),
				_tourData.getStartMonth() - 1,
				_tourData.getStartDay(),
				_tourData.getStartHour(),
				_tourData.getStartMinute());

		setMessage(_dateFormatter.format(_calendar.getTime()) + "  " + _timeFormatter.format(_calendar.getTime())); //$NON-NLS-1$
	}

	@Override
	protected final void createButtonsForButtonBar(final Composite parent) {

		super.createButtonsForButtonBar(parent);

		// set text for the OK button
		String okText = null;

		final TourDataEditorView tourDataEditor = TourManager.getTourDataEditor();
		if ((tourDataEditor != null) && tourDataEditor.isDirty() && (tourDataEditor.getTourData() == _tourData)) {
			okText = Messages.app_action_update;
		} else {
			okText = Messages.app_action_save;
		}

		getButton(IDialogConstants.OK_ID).setText(okText);
	}

	@Override
	protected Control createDialogArea(final Composite parent) {

		final Composite dlgAreaContainer = (Composite) super.createDialogArea(parent);

		// create ui
		createUI(dlgAreaContainer);

		updateUIFromModel();
		enableControls();

		return dlgAreaContainer;
	}

	/**
	 * @param parent
	 * @param title
	 * @param isGrabVertical
	 * @return
	 */
	private Composite createSection(final Composite parent, final String title, final boolean isGrabVertical) {

		final Section section = _tk.createSection(parent,//
				//Section.TWISTIE |
//				Section.SHORT_TITLE_BAR
				Section.TITLE_BAR
		// | Section.DESCRIPTION
		// | Section.EXPANDED
				);

		section.setText(title);
		GridDataFactory.fillDefaults().grab(true, isGrabVertical).applyTo(section);

		final Composite sectionContainer = _tk.createComposite(section);
		section.setClient(sectionContainer);

//		section.addExpansionListener(new ExpansionAdapter() {
//			@Override
//			public void expansionStateChanged(final ExpansionEvent e) {
//				form.reflow(false);
//			}
//		});

		return sectionContainer;
	}

	private void createUI(final Composite parent) {

		_pc = new PixelConverter(parent);
		_defaultSpinnerWidth = _pc.convertWidthInCharsToPixels(5);

		_unitValueDistance = UI.UNIT_VALUE_DISTANCE;
		_unitValueTemperature = UI.UNIT_VALUE_TEMPERATURE;
		_unitValueWindSpeed = UI.UNIT_VALUE_DISTANCE == 1 ? IWeather.windSpeedKmh : IWeather.windSpeedMph;

		_tk = new FormToolkit(parent.getDisplay());

		_formContainer = _tk.createForm(parent);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(_formContainer);
		_tk.decorateFormHeading(_formContainer);
		_tk.setBorderStyle(SWT.BORDER);

		final Composite tourContainer = _formContainer.getBody();
		GridLayoutFactory.swtDefaults().applyTo(tourContainer);
		{
			createUISection110Title(tourContainer);
			createUISectionSeparator(tourContainer);

			createUISection130Personal(tourContainer);
			createUISectionSeparator(tourContainer);

			createUISection140Weather(tourContainer);
		}

		final Label label = new Label(parent, SWT.SEPARATOR | SWT.HORIZONTAL);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(label);

		// compute width for all controls and equalize column width for the different sections
		tourContainer.layout(true, true);
		UI.setEqualizeColumWidths(_firstColumnControls);
		UI.setEqualizeColumWidths(_secondColumnControls);

		tourContainer.layout(true, true);
		UI.setEqualizeColumWidths(_firstColumnContainerControls);
	}

	private void createUISection110Title(final Composite parent) {

		Label label;

		final Composite section = createSection(parent, Messages.tour_editor_section_tour, true);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(section);
		{
			/*
			 * title
			 */
			label = _tk.createLabel(section, Messages.tour_editor_label_tour_title);
			_firstColumnControls.add(label);

			_txtTitle = _tk.createText(section, UI.EMPTY_STRING);
			GridDataFactory.fillDefaults()//
					.grab(true, false)
					.applyTo(_txtTitle);

			/*
			 * description
			 */
			label = _tk.createLabel(section, Messages.tour_editor_label_description);
			GridDataFactory.swtDefaults().align(SWT.FILL, SWT.BEGINNING).applyTo(label);
			_firstColumnControls.add(label);

			_txtDescription = _tk.createText(section, //
					UI.EMPTY_STRING,
					SWT.BORDER | SWT.WRAP | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL//
			);

			final IPreferenceStore store = TourbookPlugin.getDefault().getPreferenceStore();

			int descLines = store.getInt(ITourbookPreferences.TOUR_EDITOR_DESCRIPTION_HEIGHT);
			descLines = descLines < 1 ? 5 : descLines;

			GridDataFactory.fillDefaults()//
					.grab(true, true)
					//
					// SWT.DEFAULT causes lot's of problems with the layout therefore the hint is set
					//
					.hint(_pc.convertWidthInCharsToPixels(80), _pc.convertHeightInCharsToPixels(5))
					.applyTo(_txtDescription);
		}
	}

	private void createUISection130Personal(final Composite parent) {

		final Composite section = createSection(parent, Messages.tour_editor_section_personal, false);
		GridLayoutFactory.fillDefaults()//
				.numColumns(2)
				.spacing(20, 5)
				.applyTo(section);
		{
			createUISection132PersonalCol1(section);
			createUISection134PersonalCol2(section);
		}
	}

	/**
	 * 1. column
	 */
	private void createUISection132PersonalCol1(final Composite section) {

		final Composite container = _tk.createComposite(section);
		GridDataFactory.fillDefaults().applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(3).applyTo(container);
		_firstColumnContainerControls.add(container);
		{
			/*
			 * rest pulse
			 */

			// label: Rest pulse
			final Label label = _tk.createLabel(container, Messages.tour_editor_label_rest_pulse);
			label.setToolTipText(Messages.tour_editor_label_rest_pulse_Tooltip);
			_firstColumnControls.add(label);

			// spinner
			_spinRestPuls = new Spinner(container, SWT.BORDER);
			GridDataFactory.fillDefaults()//
					.hint(_defaultSpinnerWidth, SWT.DEFAULT)
					.align(SWT.BEGINNING, SWT.CENTER)
					.applyTo(_spinRestPuls);
			_spinRestPuls.setMinimum(0);
			_spinRestPuls.setMaximum(200);
			_spinRestPuls.setToolTipText(Messages.tour_editor_label_rest_pulse_Tooltip);

			_spinRestPuls.addMouseWheelListener(_mouseWheelListener);

			// label: bpm
			_tk.createLabel(container, Messages.Graph_Label_Heartbeat_unit);
		}
	}

	/**
	 * 2. column
	 */
	private void createUISection134PersonalCol2(final Composite section) {

		final Composite container = _tk.createComposite(section);
		GridDataFactory.fillDefaults().applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(3).applyTo(container);
		{
			/*
			 * calories
			 */

			// label
			final Label label = _tk.createLabel(container, Messages.tour_editor_label_tour_calories);
			_secondColumnControls.add(label);

			// spinner
			_spinTourCalories = new Spinner(container, SWT.BORDER);
			GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.CENTER).applyTo(_spinTourCalories);
			_spinTourCalories.setMinimum(0);
			_spinTourCalories.setMaximum(1000000);
//			_spinTourCalories.setToolTipText();

			_spinTourCalories.addMouseWheelListener(_mouseWheelListener);

			// label: cal
			_tk.createLabel(container, Messages.tour_editor_label_tour_calories_unit);
		}
	}

	private void createUISection140Weather(final Composite parent) {

		final Composite section = createSection(parent, Messages.tour_editor_section_weather, false);
		GridLayoutFactory.fillDefaults()//
				.numColumns(2)
				.spacing(20, 5)
				.applyTo(section);
		{
			createUISection142Weather(section);
			createUISection144WeatherCol1(section);
		}
	}

	private void createUISection142Weather(final Composite section) {

		final Composite container = _tk.createComposite(section);
		GridDataFactory.fillDefaults().span(2, 1).applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(5).applyTo(container);
		{

			/*
			 * wind speed
			 */

			// label
			Label label = _tk.createLabel(container, Messages.tour_editor_label_wind_speed);
			label.setToolTipText(Messages.tour_editor_label_wind_speed_Tooltip);
			_firstColumnControls.add(label);

			// spinner
			_spinWindSpeedValue = new Spinner(container, SWT.BORDER);
			GridDataFactory.fillDefaults()//
					.hint(_defaultSpinnerWidth, SWT.DEFAULT)
					.align(SWT.BEGINNING, SWT.CENTER)
					.applyTo(_spinWindSpeedValue);
			_spinWindSpeedValue.setMinimum(0);
			_spinWindSpeedValue.setMaximum(120);
			_spinWindSpeedValue.setToolTipText(Messages.tour_editor_label_wind_speed_Tooltip);

			_spinWindSpeedValue.addModifyListener(new ModifyListener() {
				public void modifyText(final ModifyEvent e) {
					if (_isUpdateUI) {
						return;
					}
					onSelectWindSpeedValue();
				}
			});
			_spinWindSpeedValue.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					if (_isUpdateUI) {
						return;
					}
					onSelectWindSpeedValue();
				}
			});
			_spinWindSpeedValue.addMouseWheelListener(new MouseWheelListener() {
				public void mouseScrolled(final MouseEvent event) {
					Util.adjustSpinnerValueOnMouseScroll(event);
					if (_isUpdateUI) {
						return;
					}
					onSelectWindSpeedValue();
				}
			});

			// label: km/h, mi/h
			label = _tk.createLabel(container, UI.UNIT_LABEL_SPEED);

			// combo: wind speed with text
			_comboWindSpeedText = new Combo(container, SWT.READ_ONLY | SWT.BORDER);
			GridDataFactory.fillDefaults()//
					.align(SWT.BEGINNING, SWT.FILL)
					.indent(10, 0)
					.span(2, 1)
					.applyTo(_comboWindSpeedText);
			_tk.adapt(_comboWindSpeedText, true, false);
			_comboWindSpeedText.setToolTipText(Messages.tour_editor_label_wind_speed_Tooltip);
			_comboWindSpeedText.setVisibleItemCount(20);
			_comboWindSpeedText.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {

					if (_isUpdateUI) {
						return;
					}
					onSelectWindSpeedText();
				}
			});

			// fill combobox
			for (final String speedText : IWeather.windSpeedText) {
				_comboWindSpeedText.add(speedText);
			}

			/*
			 * wind direction
			 */

			// label
			label = _tk.createLabel(container, Messages.tour_editor_label_wind_direction);
			label.setToolTipText(Messages.tour_editor_label_wind_direction_Tooltip);
			_firstColumnControls.add(label);

			// combo: wind direction text
			_comboWindDirectionText = new Combo(container, SWT.READ_ONLY | SWT.BORDER);
			_tk.adapt(_comboWindDirectionText, true, false);
			GridDataFactory.fillDefaults()//
					.align(SWT.BEGINNING, SWT.FILL)
					.hint(_defaultSpinnerWidth, SWT.DEFAULT)
					.applyTo(_comboWindDirectionText);
			_comboWindDirectionText.setToolTipText(Messages.tour_editor_label_WindDirectionNESW_Tooltip);
			_comboWindDirectionText.setVisibleItemCount(10);
			_comboWindDirectionText.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {

					if (_isUpdateUI) {
						return;
					}
					onSelectWindDirectionText();
				}
			});

			// spacer
			new Label(container, SWT.NONE);

			// spinner: wind direction value
			_spinWindDirectionValue = new Spinner(container, SWT.BORDER);
			GridDataFactory.fillDefaults()//
					.hint(_defaultSpinnerWidth, SWT.DEFAULT)
					.indent(10, 0)
					.align(SWT.BEGINNING, SWT.CENTER)
					.applyTo(_spinWindDirectionValue);
			_spinWindDirectionValue.setMinimum(-1);
			_spinWindDirectionValue.setMaximum(360);
			_spinWindDirectionValue.setToolTipText(Messages.tour_editor_label_wind_direction_Tooltip);

			_spinWindDirectionValue.addModifyListener(new ModifyListener() {
				public void modifyText(final ModifyEvent e) {
					if (_isUpdateUI) {
						return;
					}
					onSelectWindDirectionValue();
				}
			});
			_spinWindDirectionValue.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					if (_isUpdateUI) {
						return;
					}
					onSelectWindDirectionValue();
				}
			});
			_spinWindDirectionValue.addMouseWheelListener(new MouseWheelListener() {
				public void mouseScrolled(final MouseEvent event) {
					Util.adjustSpinnerValueOnMouseScroll(event);
					if (_isUpdateUI) {
						return;
					}
					onSelectWindDirectionValue();
				}
			});

			// label: direction unit = degree
			_tk.createLabel(container, Messages.Tour_Editor_Label_WindDirection_Unit);

			// fill combobox
			for (final String fComboCloudsUIValue : IWeather.windDirectionText) {
				_comboWindDirectionText.add(fComboCloudsUIValue);
			}

		}
	}

	/**
	 * weather: 1. column
	 */
	private void createUISection144WeatherCol1(final Composite section) {

		final Composite container = _tk.createComposite(section);
		GridDataFactory.fillDefaults().applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(3).applyTo(container);
		_firstColumnContainerControls.add(container);
		{
			/*
			 * temperature
			 */

			// label
			Label label = _tk.createLabel(container, Messages.tour_editor_label_temperature);
			label.setToolTipText(Messages.tour_editor_label_temperature_Tooltip);
			_firstColumnControls.add(label);

			// spinner
			_spinTemperature = new Spinner(container, SWT.BORDER);
			GridDataFactory.fillDefaults()//
					.align(SWT.BEGINNING, SWT.CENTER)
					.hint(_defaultSpinnerWidth, SWT.DEFAULT)
					.applyTo(_spinTemperature);
			_spinTemperature.setToolTipText(Messages.tour_editor_label_temperature_Tooltip);

			// the min/max temperature has a large range because fahrenheit has bigger values than celcius
			_spinTemperature.setMinimum(-60);
			_spinTemperature.setMaximum(150);

			_spinTemperature.addModifyListener(new ModifyListener() {
				public void modifyText(final ModifyEvent e) {
					if (_isUpdateUI) {
						return;
					}
					_isTemperatureManuallyModified = true;
				}
			});
			_spinTemperature.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					if (_isUpdateUI) {
						return;
					}
					_isTemperatureManuallyModified = true;
				}
			});
			_spinTemperature.addMouseWheelListener(new MouseWheelListener() {
				public void mouseScrolled(final MouseEvent event) {
					Util.adjustSpinnerValueOnMouseScroll(event);
					if (_isUpdateUI) {
						return;
					}
					_isTemperatureManuallyModified = true;
				}
			});

			// label: celcius, fahrenheit
			label = _tk.createLabel(container, UI.UNIT_LABEL_TEMPERATURE);

			/*
			 * clouds
			 */
			final Composite cloudContainer = new Composite(container, SWT.NONE);
			GridDataFactory.fillDefaults().applyTo(cloudContainer);
			GridLayoutFactory.fillDefaults().numColumns(3).applyTo(cloudContainer);
			{
				// label: clouds
				label = _tk.createLabel(cloudContainer, Messages.tour_editor_label_clouds);
				label.setToolTipText(Messages.tour_editor_label_clouds_Tooltip);

				// icon: clouds
				_lblCloudIcon = new CLabel(cloudContainer, SWT.NONE);
				GridDataFactory.fillDefaults()//
						.align(SWT.END, SWT.FILL)
						.grab(true, false)
						.applyTo(_lblCloudIcon);
			}
			_firstColumnControls.add(cloudContainer);

			// combo: clouds
			_comboClouds = new Combo(container, SWT.READ_ONLY | SWT.BORDER);
			GridDataFactory.fillDefaults().span(2, 1).applyTo(_comboClouds);
			_tk.adapt(_comboClouds, true, false);
			_comboClouds.setToolTipText(Messages.tour_editor_label_clouds_Tooltip);
			_comboClouds.setVisibleItemCount(10);
			_comboClouds.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					displayCloudIcon();
				}
			});

			// fill combobox
			for (final String cloudText : IWeather.cloudText) {
				_comboClouds.add(cloudText);
			}

			// force the icon to be displayed to ensure the width is correctly set when the size is computed
			_isUpdateUI = true;
			{
				_comboClouds.select(0);
				displayCloudIcon();
			}
			_isUpdateUI = false;
		}
	}

	private void createUISectionSeparator(final Composite parent) {
		final Composite sep = _tk.createComposite(parent);
		GridDataFactory.fillDefaults().hint(SWT.DEFAULT, 5).applyTo(sep);
	}

	private void displayCloudIcon() {

		final int selectionIndex = _comboClouds.getSelectionIndex();

		final String cloudKey = IWeather.cloudIcon[selectionIndex];
		final Image cloundIcon = UI.IMAGE_REGISTRY.get(cloudKey);

		_lblCloudIcon.setImage(cloundIcon);
	}

	private void enableControls() {

		_spinTourCalories.setEnabled(_isManualTour);

		_spinTemperature.setEnabled(_tourData.temperatureSerie == null);
	}

	@Override
	protected IDialogSettings getDialogBoundsSettings() {

		// keep window size and position
		return _state;
//		return null;
	}

	private int getWindDirectionTextIndex(final int degreeDirection) {

		final float degree = (degreeDirection + 22.5f) / 45.0f;

		final int directionIndex = ((int) degree) % 8;

		return directionIndex;
	}

	private int getWindSpeedTextIndex(final int speed) {

		// set speed to max index value
		int speedValueIndex = _unitValueWindSpeed.length - 1;

		for (int speedIndex = 0; speedIndex < _unitValueWindSpeed.length; speedIndex++) {

			final int speedMaxValue = _unitValueWindSpeed[speedIndex];

			if (speed <= speedMaxValue) {
				speedValueIndex = speedIndex;
				break;
			}
		}

		return speedValueIndex;
	}

	@Override
	protected void okPressed() {

		updateModelFromUI();

		if (_tourData.isValidForSave() == false) {
			// data are not valid to be saved which is done in the action which opened this dialog
			return;
		}

		super.okPressed();
	}

	private void onDispose() {

		if (_tk != null) {
			_tk.dispose();
		}

		_firstColumnControls.clear();
		_secondColumnControls.clear();
		_firstColumnContainerControls.clear();
	}

	private void onSelectWindDirectionText() {

		// N=0=0  NE=1=45  E=2=90  SE=3=135  S=4=180  SW=5=225  W=6=270  NW=7=315
		final int selectedIndex = _comboWindDirectionText.getSelectionIndex();

		// get degree from selected direction

		final int degree = selectedIndex * 45;

		_spinWindDirectionValue.setSelection(degree);
	}

	private void onSelectWindDirectionValue() {

		int degree = _spinWindDirectionValue.getSelection();

		if (degree == -1) {
			degree = 359;
			_spinWindDirectionValue.setSelection(degree);
		}
		if (degree == 360) {
			degree = 0;
			_spinWindDirectionValue.setSelection(degree);
		}

		_comboWindDirectionText.select(getWindDirectionTextIndex(degree));

	}

	private void onSelectWindSpeedText() {

		_isWindSpeedManuallyModified = true;

		final int selectedIndex = _comboWindSpeedText.getSelectionIndex();
		final int speed = _unitValueWindSpeed[selectedIndex];

		final boolean isBackup = _isUpdateUI;
		_isUpdateUI = true;
		{
			_spinWindSpeedValue.setSelection(speed);
		}
		_isUpdateUI = isBackup;
	}

	private void onSelectWindSpeedValue() {

		_isWindSpeedManuallyModified = true;

		final int windSpeed = _spinWindSpeedValue.getSelection();

		final boolean isBackup = _isUpdateUI;
		_isUpdateUI = true;
		{
			_comboWindSpeedText.select(getWindSpeedTextIndex(windSpeed));
		}
		_isUpdateUI = isBackup;
	}

	/**
	 * update tourdata from the fields
	 */
	private void updateModelFromUI() {

		_tourData.setTourTitle(_txtTitle.getText().trim());
		_tourData.setTourDescription(_txtDescription.getText().trim());

		_tourData.setRestPulse(_spinRestPuls.getSelection());
		_tourData.setCalories(_spinTourCalories.getSelection());

		_tourData.setWeatherWindDir(_spinWindDirectionValue.getSelection());
		if (_isWindSpeedManuallyModified) {
			/*
			 * update the speed only when it was modified because when the measurement is
			 * changed when the tour is being modified then the computation of the speed
			 * value can cause rounding errors
			 */
			_tourData.setWeatherWindSpd((int) (_spinWindSpeedValue.getSelection() * _unitValueDistance));
		}

		final int cloudIndex = _comboClouds.getSelectionIndex();
		String cloudValue = IWeather.cloudIcon[cloudIndex];
		if (cloudValue.equals(UI.IMAGE_EMPTY_16)) {
			// replace invalid cloud key
			cloudValue = UI.EMPTY_STRING;
		}
		_tourData.setWeatherClouds(cloudValue);

		if (_isTemperatureManuallyModified) {
			int temperature = _spinTemperature.getSelection();
			if (_unitValueTemperature != 1) {
				temperature = (int) ((temperature - UI.UNIT_FAHRENHEIT_ADD) / UI.UNIT_FAHRENHEIT_MULTI);
			}
			_tourData.setAvgTemperature(temperature);
		}

	}

	private void updateUIFromModel() {

		_isUpdateUI = true;
		{
			/*
			 * tour/event
			 */
			// set field content
			_txtTitle.setText(_tourData.getTourTitle());
			_txtDescription.setText(_tourData.getTourDescription());

			/*
			 * personal details
			 */
			_spinRestPuls.setSelection(_tourData.getRestPulse());
			_spinTourCalories.setSelection(_tourData.getCalories());

			/*
			 * wind properties
			 */
			// wind direction
			final int weatherWindDirDegree = _tourData.getWeatherWindDir();
			_spinWindDirectionValue.setSelection(weatherWindDirDegree);
			_comboWindDirectionText.select(getWindDirectionTextIndex(weatherWindDirDegree));

			// wind speed
			final int windSpeed = _tourData.getWeatherWindSpeed();
			final int speed = (int) (windSpeed / _unitValueDistance);
			_spinWindSpeedValue.setSelection(speed);
			_comboWindSpeedText.select(getWindSpeedTextIndex(speed));

			// weather clouds
			_comboClouds.select(_tourData.getWeatherIndex());

			// icon must be displayed after the combobox entry is selected
			displayCloudIcon();

			// temperature
			int temperature = _tourData.getAvgTemperature();
			if (_unitValueTemperature != 1) {
				temperature = (int) (temperature * UI.UNIT_FAHRENHEIT_MULTI + UI.UNIT_FAHRENHEIT_ADD);
			}
			_spinTemperature.setSelection(temperature);

		}
		_isUpdateUI = false;
	}
}
